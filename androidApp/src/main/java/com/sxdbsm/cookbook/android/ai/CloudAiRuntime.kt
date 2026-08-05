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
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * SSE 读取结果。[AI修改] AF-06: 抽取为可测试函数。
 */
data class SseStreamResult(
    val finishReason: String?,
    val totalChars: Int,
)

/**
 * 从 InputStream 逐行读取 SSE data: 帧并回调。[AI修改] AF-06
 *
 * @param inputStream SSE 数据源
 * @param onDelta 每个非空 delta.content 立即回调
 * @return 读取结果（finish_reason + 总字符数）
 */
suspend fun readSseStream(
    inputStream: InputStream,
    onDelta: suspend (deltaContent: String) -> Unit,
): SseStreamResult {
    val reader = BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8))
    var finishReason: String? = null
    var totalChars = 0

    reader.use { r ->
        var line: String?
        while (r.readLine().also { line = it } != null) {
            val currentLine = line ?: break
            if (currentLine.isEmpty()) continue
            if (!currentLine.startsWith("data:")) continue

            val dataContent = currentLine.removePrefix("data:").trimStart()
            val chunk = GlmProtocol.parseSseLine(dataContent)

            if (chunk.isDone) break

            if (chunk.finishReason != null) finishReason = chunk.finishReason

            if (chunk.deltaContent.isNotEmpty()) {
                totalChars += chunk.deltaContent.length
                onDelta(chunk.deltaContent)
            }
        }
    }

    return SseStreamResult(finishReason = finishReason, totalChars = totalChars)
}

/**
 * HTTP transport 抽象——便于 Runtime 单测注入。[AI修改] AF-06
 */
interface StreamTransport {
    /** 执行一次流式请求，流式期间阻塞在 IO；每帧回调 onDelta，成功返回响应码/耗时。 */
    @Throws(IOException::class)
    suspend fun execute(
        endpoint: String, key: String, body: String,
        onDelta: suspend (deltaContent: String) -> Unit,
    ): SseStreamResult
}

/** 默认实现：HttpURLConnection。[AI修改] AF-06 */
class HttpUrlStreamTransport : StreamTransport {
    override suspend fun execute(
        endpoint: String, key: String, body: String,
        onDelta: suspend (deltaContent: String) -> Unit,
    ): SseStreamResult {
        val started = System.currentTimeMillis()
        val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
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
            val result = readSseStream(conn.inputStream, onDelta)
            AppLogger.i("CloudAi", "stream http=$code cost=${System.currentTimeMillis() - started}ms chars=${result.totalChars} finish=${result.finishReason}")
            return result
        } finally {
            conn.disconnect()
        }
    }
}

/**
 * @File : CloudAiRuntime
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 云端 AI 运行时（OpenAI 兼容 API）
 * <p>
 * [AI修改] AF-06: 终态四选一 + awaitClose 取消 + 可测试 transport。
 * <p>
 * [AI生成] S2：接真实云端；换厂商只改 ENDPOINT/MODEL/鉴权，业务不动。
 **/
class CloudAiRuntime(
    private val config: AiRuntimeConfig,
    private val streamTransport: StreamTransport = HttpUrlStreamTransport(),
) : AiRuntime {

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
    // 流式补全（AF-06 重写：四终态 + 无阻塞join + 可测试 transport）
    // ============================================================

    /**
     * AF-06: 终态严格四选一。[AI修改]
     *
     * | 场景 | 事件序列 | 重试 | 终态 |
     * |---|---|---|---|
     * | [DONE]/EOF | Delta* → Completed | 否 | close() |
     * | 首 Delta 前 IO 失败 | (重试)→Failed(retryable=true) | 最多1次 | close() |
     * | 首 Delta 后 IO 失败 | Delta* → Failed(retryable=false) | 否 | close() |
     * | 收集者取消 | 不再发终态 | 否 | awaitClose 取消 job |
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

        val channel = this

        // 启动 IO job（不 join，让 awaitClose 接管清理）
        val job = launch(Dispatchers.IO) {
            var totalChars = 0
            var finishReason: String? = null
            var hasAnyContent = false

            for (attempt in 0 until MAX_ATTEMPTS) {
                try {
                    val result = streamTransport.execute(model.endpoint, key, body) { delta ->
                        if (delta.isNotEmpty()) {
                            hasAnyContent = true
                            totalChars += delta.length
                            channel.send(LlmStreamEvent.Delta(delta))
                        }
                    }
                    finishReason = result.finishReason
                    break // 成功
                } catch (e: IOException) {
                    AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1} io: ${e.message}")
                    if (hasAnyContent || attempt == MAX_ATTEMPTS - 1) {
                        // 首帧后失败 或 重试耗尽 → Failed，不发 Completed
                        channel.send(LlmStreamEvent.Failed(
                            message = e.message ?: "流式请求失败",
                            retryable = !hasAnyContent,
                        ))
                        channel.close()
                        return@launch
                    }
                    continue // 首帧前 → 重试
                } catch (e: Exception) {
                    AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1}: ${e.message}")
                    channel.send(LlmStreamEvent.Failed(message = e.message ?: "未知错误", retryable = !hasAnyContent))
                    channel.close()
                    return@launch
                }
            }

            // 正常结束
            channel.send(LlmStreamEvent.Completed(
                finishReason = finishReason ?: "unknown",
                totalChars = totalChars,
            ))
            channel.close()
        }

        // 不 join — 让 collect 在 job 活跃期间消费 Delta
        // awaitClose 在收集者取消时立即取消 job
        awaitClose {
            job.cancel()
        }
    }

    // ============================================================
    // 非流式 HTTP
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
        private const val MAX_ATTEMPTS = 2
    }
}
