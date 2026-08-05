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
import kotlinx.coroutines.launch
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
 * [AI修改] AF-01 AF-02: 重写 stream()——逐帧流式、Flow 自然结束、取消连接、首帧后不重试。
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
    // 流式补全（AF-01 AF-02 重写）
    // ============================================================

    /**
     * AF-01 AF-02: 逐帧流式。[AI修改]
     *
     * - 每个非空 delta.content 立即发一个 Delta（不累积到结束后）
     * - [DONE] 或正常 EOF 后发 Completed 并 close()
     * - 无 finish_reason 时传 "unknown"
     * - 首帧后网络失败不重试，保留已收 Delta
     * - awaitClose 取消 IO job 并断开 HTTP 连接
     */
    override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = callbackFlow {
        val model = config.selectedModel()
        val key = config.currentCloudApiKey()
        if (key.isBlank()) {
            send(LlmStreamEvent.Failed(message = "${model.vendorName} API Key 未配置", retryable = false))
            close()
            return@callbackFlow
        }
        val body = GlmProtocol.buildStreamRequestBody(
            model = model.model, system = request.system, user = request.user,
            temperature = request.temperature, maxTokens = request.maxTokens.coerceIn(256, 8192),
        )
        AppLogger.i("CloudAi", "stream[${model.id}] payloadBytes=${body.toByteArray().size}")
        AppLogger.debugLong("CloudAiRaw", "stream[${model.id}] requestBody", body)

        val channel = this // ProducerScope<LlmStreamEvent>
        var httpConn: HttpURLConnection? = null

        val job = launch(Dispatchers.IO) {
            var totalChars = 0
            var hasAnyContent = false // AF-02: 即时跟踪
            var lastFinishReason: String? = null

            for (attempt in 0 until MAX_ATTEMPTS) {
                try {
                    val conn = (URL(model.endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = STREAM_CONNECT_TIMEOUT
                        readTimeout = STREAM_READ_TIMEOUT
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json")
                        setRequestProperty("Authorization", "Bearer $key")
                        setRequestProperty("Accept", "text/event-stream")
                    }
                    httpConn = conn

                    conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val code = conn.responseCode

                    if (code !in 200..299) {
                        val errorBody = conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                        throw IOException("HTTP $code: ${errorBody.take(200)}")
                    }

                    val reader = BufferedReader(InputStreamReader(conn.inputStream, Charsets.UTF_8))
                    val started = System.currentTimeMillis()

                    reader.use { r ->
                        var line: String?
                        while (r.readLine().also { line = it } != null) {
                            val currentLine = line ?: break
                            if (currentLine.isEmpty()) continue
                            if (!currentLine.startsWith("data:")) continue

                            val dataContent = currentLine.removePrefix("data:").trimStart()
                            val chunk = GlmProtocol.parseSseLine(dataContent)

                            if (chunk.isDone) break

                            if (chunk.finishReason != null) lastFinishReason = chunk.finishReason

                            if (chunk.deltaContent.isNotEmpty()) {
                                hasAnyContent = true // AF-02: 首帧即标记
                                totalChars += chunk.deltaContent.length
                                channel.send(LlmStreamEvent.Delta(chunk.deltaContent)) // AF-01: 逐帧发送
                            }
                        }
                    }

                    AppLogger.i("CloudAi",
                        "stream http=$code cost=${System.currentTimeMillis() - started}ms chars=$totalChars finish=$lastFinishReason")
                    break // 成功，退出重试循环
                } catch (e: IOException) {
                    AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1} failed: ${e.message}")
                    if (hasAnyContent) break // AF-02: 首帧后不重试
                    if (attempt == MAX_ATTEMPTS - 1) {
                        channel.send(LlmStreamEvent.Failed(message = e.message ?: "流式请求失败", retryable = true))
                        channel.close()
                        return@launch
                    }
                } catch (e: Exception) {
                    AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1} failed: ${e.message}")
                    if (hasAnyContent) break
                    channel.send(LlmStreamEvent.Failed(message = e.message ?: "未知错误", retryable = !hasAnyContent))
                    channel.close()
                    return@launch
                } finally {
                    httpConn?.disconnect()
                    httpConn = null
                }
            }

            // AF-01: 流结束，发 Completed + close()
            channel.send(LlmStreamEvent.Completed(
                finishReason = lastFinishReason ?: "unknown", // AF-01: 无 finish_reason → unknown
                totalChars = totalChars,
            ))
            channel.close()
        }

        job.join()

        awaitClose {
            job.cancel()
            httpConn?.disconnect()
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
        private const val STREAM_CONNECT_TIMEOUT = 15000
        private const val STREAM_READ_TIMEOUT = 60000
        private const val MAX_ATTEMPTS = 2
    }
}
