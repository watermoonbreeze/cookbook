package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.GlmProtocol
import com.sxdbsm.cookbook.ai.LlmChatRequest
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import com.sxdbsm.cookbook.android.util.AppLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicReference

/**
 * @File : CloudAiRuntime
 * @Time : 2026/07/08 · AF-13 重写 2026-08-05
 * @Desc : 云端 AI 运行时（OpenAI 兼容 API）
 */
class CloudAiRuntime internal constructor(
    private val requestConfig: CloudAiRequestConfig,
    private val transport: StreamTransport = HttpUrlStreamTransport(),
) : AiRuntime {

    override suspend fun complete(request: LlmRequest): Result<String> = withContext(Dispatchers.IO) {
        val model = requestConfig.selectedModel()
        val key = requestConfig.apiKeyForSelectedModel()
        if (key.isBlank()) {
            return@withContext Result.failure(IllegalStateException("${model.vendorName} API Key 未配置"))
        }
        val body = GlmProtocol.buildRequestBody(model.model, request.system, request.user, request.temperature, jsonMode = model.supportsJsonMode, maxTokens = request.maxTokens.coerceIn(256, 8192))
        AppLogger.i("CloudAi", "req[${model.id}] payloadBytes=${body.toByteArray().size}")
        AppLogger.debugLong("CloudAiRaw", "complete[${model.id}] requestBody", body)
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { postOnce(model.endpoint, key, body) }
            result.onSuccess { return@withContext Result.success(it) }
            lastError = result.exceptionOrNull()
            if (lastError !is StreamTransportException) {
                AppLogger.w("CloudAi", "[${model.id}] attempt ${attempt + 1} failed: ${lastError?.javaClass?.simpleName}")
            }
        }
        Result.failure(lastError ?: IOException("unknown"))
    }

    override suspend fun chat(request: LlmChatRequest): Result<String> = withContext(Dispatchers.IO) {
        val model = requestConfig.selectedModel()
        val key = requestConfig.apiKeyForSelectedModel()
        if (key.isBlank()) return@withContext Result.failure(IllegalStateException("${model.vendorName} API Key 未配置"))
        val body = GlmProtocol.buildChatRequestBody(model.model, request.messages, request.temperature, jsonMode = model.supportsJsonMode, maxTokens = request.maxTokens.coerceIn(256, 8192))
        AppLogger.i("CloudAi", "chat[${model.id}] msgs=${request.messages.size}")
        AppLogger.debugLong("CloudAiRaw", "chat[${model.id}] requestBody", body)
        var lastError: Throwable? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            val result = runCatching { postOnce(model.endpoint, key, body) }
            result.onSuccess { return@withContext Result.success(it) }
            lastError = result.exceptionOrNull()
            if (lastError !is StreamTransportException) {
                AppLogger.w("CloudAi", "chat[${model.id}] attempt ${attempt + 1} failed: ${lastError?.javaClass?.simpleName}")
            }
        }
        Result.failure(lastError ?: IOException("unknown"))
    }

    // ============================================================
    // AF-13: 固定终态顺序（§7.5.3）
    // ============================================================

    override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = callbackFlow {
        val model = requestConfig.selectedModel()
        val key = requestConfig.apiKeyForSelectedModel()
        // 步骤1: 空key立即Failed
        if (key.isBlank()) {
            send(LlmStreamEvent.Failed(message = "${model.vendorName} API Key 未配置", retryable = false))
            close()
            return@callbackFlow
        }
        val httpRequest = StreamHttpRequest(
            endpoint = model.endpoint,
            apiKey = key,
            body = GlmProtocol.buildStreamRequestBody(
                model = model.model, system = request.system, user = request.user,
                temperature = request.temperature, maxTokens = request.maxTokens.coerceIn(256, 8192),
            ),
        )
        AppLogger.i("CloudAi", "stream[${model.id}] payloadBytes=${httpRequest.body.toByteArray().size}")
        AppLogger.debugLong("CloudAiRaw", "stream[${model.id}] requestBody", httpRequest.body)

        // 步骤2: AtomicReference<StreamCall?>
        val activeCall = AtomicReference<StreamCall?>(null)
        val channel = this
        var hasDelta = false
        var totalChars = 0
        var finishReason: String? = null

        val job = launch(Dispatchers.IO) {
            for (attempt in 0 until MAX_ATTEMPTS) {
                val call = transport.newCall(httpRequest)
                activeCall.set(call)
                try {
                    val result = call.execute { delta ->
                        if (delta.isNotEmpty()) {
                            hasDelta = true
                            totalChars += delta.length
                            channel.send(LlmStreamEvent.Delta(delta)) // 步骤3
                        }
                    }
                    finishReason = result.finishReason
                    break
                } catch (e: StreamTransportException) {
                    AppLogger.w("CloudAi", "stream[${model.id}] attempt ${attempt + 1}: ${e.httpStatus}")
                    // 步骤5: 首帧后失败 -> Failed(false), 不重试
                    if (hasDelta || attempt == MAX_ATTEMPTS - 1) {
                        channel.send(LlmStreamEvent.Failed(message = "HTTP ${e.httpStatus} STREAM_ERROR", retryable = !hasDelta))
                        channel.close()
                        return@launch
                    }
                    continue
                } catch (e: Exception) {
                    // 步骤5: 非transport异常只记类型，不泄漏message
                    if (!channel.isClosedForSend) {
                        AppLogger.w("CloudAi", "stream[${model.id}] non-transport: ${e.javaClass.simpleName}")
                        channel.send(LlmStreamEvent.Failed(message = "STREAM_ERROR", retryable = false))
                        channel.close()
                    }
                    return@launch
                } finally {
                    activeCall.compareAndSet(call, null)
                }
            }
            // 步骤4: 正常结束
            channel.send(LlmStreamEvent.Completed(finishReason = finishReason ?: "unknown", totalChars = totalChars))
            channel.close()
        }

        // 步骤7: awaitClose 取消活跃 call
        awaitClose {
            activeCall.getAndSet(null)?.cancel()
            job.cancel()
        }
    }

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
