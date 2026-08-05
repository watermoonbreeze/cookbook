package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.GlmProtocol
import com.sxdbsm.cookbook.android.util.AppLogger
import kotlinx.coroutines.CancellationException
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * @File : StreamTransport
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : AF-13/15/16: 每请求独立 call；取消标记三处检查；非取消 IO 安全包装。
 */

data class SseStreamResult(
    val finishReason: String?,
    val totalChars: Int,
)

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

internal data class StreamHttpRequest(
    val endpoint: String,
    val apiKey: String,
    val body: String,
)

internal interface StreamTransport {
    fun newCall(request: StreamHttpRequest): StreamCall
}

internal interface StreamCall {
    @Throws(StreamTransportException::class)
    suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult
    fun cancel()
}

/**
 * AF-16: 安全 transport 异常。
 *
 * - [httpStatus] nullable：非 2xx 时必有；IO 失败（超时/重置/读失败）为 null。
 * - [code] 稳定错误码。
 * - [retryable] 供 Runtime 判定是否首帧重试。
 * - message 只含安全文案，绝不含原始 body/输入/Key。
 */
class StreamTransportException(
    val httpStatus: Int?,
    val code: String,
    val retryable: Boolean,
) : IOException(code) {
    override val message: String get() = if (httpStatus != null) "HTTP $httpStatus $code" else code
}

internal class HttpUrlStreamTransport : StreamTransport {
    override fun newCall(request: StreamHttpRequest): StreamCall =
        HttpUrlStreamCall(request)
}

internal class HttpUrlStreamCall(
    private val request: StreamHttpRequest,
    // AF-21: 仅可测试性而引入的 internal 默认连接工厂；生产行为不变，不暴露到 Koin/公开 API。
    private val connectionFactory: (String) -> HttpURLConnection = {
        URL(it).openConnection() as HttpURLConnection
    },
) : StreamCall {
    @Volatile
    private var connection: HttpURLConnection? = null

    // AF-15: 每 call 私有取消标记；cancel() 先置位再 disconnect。
    @Volatile
    private var cancelled = false

    override suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult {
        checkNotCancelled("before connect")
        val started = System.currentTimeMillis()
        val conn = connectionFactory(request.endpoint).apply {
            requestMethod = "POST"
            connectTimeout = 15000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer ${request.apiKey}")
            setRequestProperty("Accept", "text/event-stream")
        }
        connection = conn
        try {
            checkNotCancelled("before write body")
            conn.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            checkNotCancelled("before read response")
            val code = conn.responseCode
            if (code !in 200..299) {
                AppLogger.debugLong("CloudAiRaw", "stream http[$code] errorLength",
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { "${it.readText().length} bytes" }.orEmpty())
                // AF-20: 仅 408/429/5xx 可重试；其余 4xx 确定性失败不重试
                throw StreamTransportException(code, "STREAM_HTTP_ERROR", retryable = isHttpRetryable(code))
            }
            val result = readSseStream(conn.inputStream, onDelta)
            AppLogger.i("CloudAi", "stream http=$code cost=${System.currentTimeMillis() - started}ms chars=${result.totalChars} finish=${result.finishReason}")
            return result
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            // AF-19: 用户取消造成的 IOException（disconnect 常见）必须优先转为取消，
            // 不得包装为可重试网络失败。
            if (cancelled) throw CancellationException("call cancelled during IO", e)
            // AF-16: 非取消网络 IO 失败统一安全包装
            if (e is StreamTransportException) throw e
            throw StreamTransportException(httpStatus = null, code = "STREAM_IO_ERROR", retryable = true)
        } finally {
            connection = null
            conn.disconnect()
        }
    }

    private fun checkNotCancelled(phase: String) {
        if (cancelled) throw CancellationException("call cancelled at $phase")
    }

    override fun cancel() {
        cancelled = true
        connection?.disconnect()
    }

    companion object {
        /** AF-20: 固定 HTTP 重试分类——仅 408/429/5xx 可重试；其余 4xx 确定性失败。 */
        internal fun isHttpRetryable(code: Int): Boolean =
            code == 408 || code == 429 || code in 500..599
    }
}
