package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.GlmProtocol
import com.sxdbsm.cookbook.ai.LlmChatRequest
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import com.sxdbsm.cookbook.android.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * @File : CloudAiRuntime
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 云端 AI 运行时（OpenAI 兼容 API）
 * <p>
 * 用 HttpURLConnection 调 OpenAI 兼容的 chat/completions，零额外依赖；JSON 编解码交给 shared 的 GlmProtocol。
 * Key 从 AiRuntimeConfig 读取（只存本机、不写日志）。失败返回 Result.failure，由 Orchestrator 回退纯规则推荐。
 * <p>
 * [AI修改] B2 周期记+NDJSON流式改造：实现 stream() —— SSE 逐行解析 → LlmStreamEvent。
 * <p>
 * [AI生成] S2：接真实云端；换厂商只改 ENDPOINT/MODEL/鉴权，业务不动。
 **/
class CloudAiRuntime(private val config: AiRuntimeConfig) : AiRuntime {

    override suspend fun complete(request: LlmRequest): Result<String> = withContext(Dispatchers.IO) {
        val model = config.selectedModel()
        val key = config.currentCloudApiKey()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("${model.vendorName} API Key 未配置"))
        }
        val body = GlmProtocol.buildRequestBody(model.model, request.system, request.user, request.temperature, jsonMode = model.supportsJsonMode, maxTokens = request.maxTokens.coerceIn(256, 8192))
        AppLogger.i("CloudAi", "req[${model.id}] endpoint=${model.endpoint} payloadBytes=${body.toByteArray().size}")
        AppLogger.debugLong("CloudAiRaw", "complete[${model.id}] requestBody", body)
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { postOnce(model.endpoint, key, body) }
            result.onSuccess { return@withContext Result.success(it) }
            lastError = result.exceptionOrNull()
            AppLogger.w("CloudAi", "[${model.id}] attempt ${attempt + 1} failed: ${lastError?.message}")
        }
        Result.failure(lastError ?: IOException("unknown"))
    }

    override suspend fun chat(request: LlmChatRequest): Result<String> = withContext(Dispatchers.IO) {
        val model = config.selectedModel()
        val key = config.currentCloudApiKey()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("${model.vendorName} API Key 未配置"))
        val body = GlmProtocol.buildChatRequestBody(model.model, request.messages, request.temperature, jsonMode = model.supportsJsonMode, maxTokens = request.maxTokens.coerceIn(256, 8192))
        AppLogger.i("CloudAi", "chat[${model.id}] msgs=${request.messages.size}")
        AppLogger.debugLong("CloudAiRaw", "chat[${model.id}] requestBody", body)
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { postOnce(model.endpoint, key, body) }
            result.onSuccess { return@withContext Result.success(it) }
            lastError = result.exceptionOrNull()
            AppLogger.w("CloudAi", "chat[${model.id}] attempt ${attempt + 1} failed: ${lastError?.message}")
        }
        Result.failure(lastError ?: IOException("unknown"))
    }

    // ============================================================
    // 流式补全（B2 新增）
    // ============================================================

    /**
     * 流式补全。[AI生成] B2
     *
     * 用 callbackFlow + withContext(IO) 做 SSE 流式读取：
     * 逐行解析 data: 帧 → 累积完整文本 → 发送 Delta + Completed。
     * 网络失败且未收到任何内容时自动重试 1 次；已收到内容后失败不重试。
     */
    override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = callbackFlow {
        val model = config.selectedModel()
        val key = config.currentCloudApiKey()
        if (key.isBlank()) {
            send(LlmStreamEvent.Failed(message = "${model.vendorName} API Key 未配置", retryable = false))
            awaitClose {}
            return@callbackFlow
        }
        val body = GlmProtocol.buildStreamRequestBody(
            model = model.model, system = request.system, user = request.user,
            temperature = request.temperature, maxTokens = request.maxTokens.coerceIn(256, 8192),
        )
        AppLogger.i("CloudAi", "stream[${model.id}] payloadBytes=${body.toByteArray().size}")
        AppLogger.debugLong("CloudAiRaw", "stream[${model.id}] requestBody", body)

        var lastError: Throwable? = null
        var hasAnyContent = false

        for (attempt in 0 until MAX_ATTEMPTS) {
            try {
                val result = withContext(Dispatchers.IO) {
                    streamOnce(model.endpoint, key, body)
                }
                hasAnyContent = result.text.isNotEmpty()
                if (result.text.isNotEmpty()) {
                    send(LlmStreamEvent.Delta(result.text))
                }
                send(LlmStreamEvent.Completed(
                    finishReason = result.finishReason ?: "stop",
                    totalChars = result.text.length,
                ))
                awaitClose {}
                return@callbackFlow
            } catch (e: IOException) {
                lastError = e
                AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1} failed: ${e.message}")
                if (hasAnyContent) break
            } catch (e: Exception) {
                lastError = e
                AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1} failed: ${e.message}")
                if (hasAnyContent) break
            }
        }

        send(LlmStreamEvent.Failed(
            message = lastError?.message ?: "流式请求失败",
            retryable = !hasAnyContent,
        ))
        awaitClose {}
    }

    /** SSE 流读取结果。[AI生成] B2 */
    private data class StreamResult(
        val text: String,
        val finishReason: String?,
    )

    /**
     * 执行一次流式 HTTP 请求，逐行解析 SSE 并累积完整文本。[AI生成] B2
     *
     * 读取所有 data: 行 → 解析 delta.content → 累积 → 返回完整文本 + finish_reason。
     * 日志遵守脱敏规则：只记耗时、字符数、finish reason、HTTP 状态，不记原文。
     */
    private fun streamOnce(endpoint: String, key: String, body: String): StreamResult {
        val started = System.currentTimeMillis()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = STREAM_CONNECT_TIMEOUT
            readTimeout = STREAM_READ_TIMEOUT
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
            setRequestProperty("Accept", "text/event-stream")
        }

        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode

            if (code !in 200..299) {
                val errorBody = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw IOException("HTTP $code: ${errorBody.take(200)}")
            }

            val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
            val accumulated = StringBuilder()
            var finishReason: String? = null

            reader.use { r ->
                var line: String?
                while (r.readLine().also { line = it } != null) {
                    val currentLine = line ?: break
                    if (currentLine.isEmpty()) continue
                    if (!currentLine.startsWith("data:")) continue

                    val dataContent = currentLine.removePrefix("data:").trimStart()
                    val chunk = GlmProtocol.parseSseLine(dataContent)

                    if (chunk.isDone) {
                        // [DONE] 标记，流结束
                        break
                    }

                    if (chunk.finishReason != null) {
                        finishReason = chunk.finishReason
                    }

                    if (chunk.deltaContent.isNotEmpty()) {
                        accumulated.append(chunk.deltaContent)
                    }
                }
            }

            val text = accumulated.toString()
            AppLogger.i("CloudAi",
                "stream http=$code cost=${System.currentTimeMillis() - started}ms chars=${text.length} finish=$finishReason")

            return StreamResult(text = text, finishReason = finishReason)
        } finally {
            conn.disconnect()
        }
    }

    // ============================================================
    // 非流式 HTTP（已有）
    // ============================================================

    private fun postOnce(endpoint: String, key: String, body: String): String {
        val started = System.currentTimeMillis()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = CONNECT_TIMEOUT
            readTimeout = READ_TIMEOUT
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }
        try {
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            AppLogger.i("CloudAi", "http=$code cost=${System.currentTimeMillis() - started}ms responseBytes=${text.toByteArray().size}")
            AppLogger.debugLong("CloudAiRaw", "http[$code] responseBody", text)
            if (code !in 200..299) throw IOException("HTTP $code")
            return GlmProtocol.parseContent(text) ?: throw IOException("empty content")
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        private const val CONNECT_TIMEOUT = 15000
        private const val READ_TIMEOUT = 45000
        // [AI生成] B2 流式超时：连接 15s，单 segment 读取 60s（规范 §3.3）
        private const val STREAM_CONNECT_TIMEOUT = 15000
        private const val STREAM_READ_TIMEOUT = 60000
        private const val MAX_ATTEMPTS = 2
    }
}
