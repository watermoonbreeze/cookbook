package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.GlmProtocol
import com.sxdbsm.cookbook.android.util.AppLogger
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
 * @Desc : AF-13: 每请求独立 call，无全局连接状态。
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

/** transport 可抛出的安全异常：只含状态码和安全文案，不含 body。 */
class StreamTransportException(
    val httpStatus: Int,
    code: String,
) : IOException("HTTP $httpStatus $code")

internal class HttpUrlStreamTransport : StreamTransport {
    override fun newCall(request: StreamHttpRequest): StreamCall =
        HttpUrlStreamCall(request)
}

internal class HttpUrlStreamCall(
    private val request: StreamHttpRequest,
) : StreamCall {
    @Volatile
    private var connection: HttpURLConnection? = null

    override suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult {
        val started = System.currentTimeMillis()
        val conn = (URL(request.endpoint).openConnection() as HttpURLConnection).apply {
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
            conn.outputStream.use { it.write(request.body.toByteArray(Charsets.UTF_8)) }
            val code = conn.responseCode
            if (code !in 200..299) {
                AppLogger.debugLong("CloudAiRaw", "stream http[$code] errorLength",
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { "${it.readText().length} bytes" }.orEmpty())
                throw StreamTransportException(code, "STREAM_HTTP_ERROR")
            }
            val result = readSseStream(conn.inputStream, onDelta)
            AppLogger.i("CloudAi", "stream http=$code cost=${System.currentTimeMillis() - started}ms chars=${result.totalChars} finish=${result.finishReason}")
            return result
        } finally {
            connection = null
            conn.disconnect()
        }
    }

    override fun cancel() {
        connection?.disconnect()
    }
}
