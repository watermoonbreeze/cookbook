package com.sxdbsm.cookbook.android.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

/**
 * T-BF03-01~03: StreamTransport 流式超时与回归测试
 *
 * T-BF03-01: 新增——请求总时长超时兜底（真实阻塞+看门狗强制断连）
 * T-BF03-02/03: 回归——既有 CloudAiRuntimeStreamTest 全部通过
 */
class StreamTransportTimeoutTest {

    /**
     * T-BF03-01: 整体请求超时（纯心跳行阻塞，看门狗超时后强制 disconnect）应抛 STREAM_TIMEOUT_ERROR
     *
     * 夹具模式同既有 AF-21（`BlockingSseInputStream` + `BlockingHttpURLConnection` + `async().await()`）：
     * - fake InputStream 先吐一行 ": heartbeat\n"（非 data: 前缀），随后在 CountDownLatch 上真实阻塞（不忙等）
     * - fake HttpURLConnection.disconnect() 记录调用次数并 countDown() 释放 latch，释放后 read() 抛 IOException
     * - totalTimeoutMs 设置为 200L，看门狗在 200ms 后置 timedOut=true 并强制 disconnect()
     * - 整个刺激用 `withTimeout(5000)` 包裹（防止修复再次失效时测试挂起拖垮整个构建）
     *
     * 断言：抛出 StreamTransportException；code="STREAM_TIMEOUT_ERROR"；retryable=true；httpStatus=null；disconnect >= 1
     */
    @Test
    fun test_BF03_01_streamTimeoutError() {
        val disconnectCount = AtomicInteger(0)
        val blockingLatch = CountDownLatch(1)

        // 先吐一行心跳，然后真实阻塞在 latch 上（非忙等），disconnect 时 latch 释放
        val blockingStream = object : InputStream() {
            private val heartbeatLine = ": heartbeat\n".toByteArray(Charsets.UTF_8)
            private var heartbeatSent = false

            override fun read(): Int {
                if (!heartbeatSent) {
                    heartbeatSent = true
                    return heartbeatLine[0].toInt()
                }
                // 真实阻塞在 latch 上
                blockingLatch.await()
                throw IOException("socket closed")
            }

            override fun read(b: ByteArray?, off: Int, len: Int): Int {
                if (!heartbeatSent && b != null && len > 0) {
                    heartbeatSent = true
                    val copy = minOf(len, heartbeatLine.size)
                    System.arraycopy(heartbeatLine, 0, b, off, copy)
                    return copy
                }
                // 真实阻塞在 latch 上
                blockingLatch.await()
                throw IOException("socket closed")
            }
        }

        // fake HttpURLConnection：disconnect() 释放 latch 并计数
        val fakeConn = object : HttpURLConnection(null) {
            private val outputBuffer = ByteArrayOutputStream()

            override fun connect() {
                // no-op
            }

            override fun disconnect() {
                disconnectCount.incrementAndGet()
                blockingLatch.countDown()
            }

            override fun usingProxy(): Boolean = false

            override fun getOutputStream() = outputBuffer

            override fun getResponseCode(): Int = 200

            override fun getInputStream() = blockingStream

            override fun getErrorStream(): InputStream? = null
        }

        val request = StreamHttpRequest(
            endpoint = "https://fake.endpoint",
            apiKey = "fake-key",
            body = """{"model":"gpt-4"}"""
        )

        val call = HttpUrlStreamCall(
            request,
            connectionFactory = { fakeConn },
            totalTimeoutMs = 200L
        )

        // 用 withTimeout(5000) 包裹防止挂起拖垮构建（如果修复再次失效）；
        // 用 async(Dispatchers.Default).await() 模式同既有 AF-21 测试
        var caughtException: StreamTransportException? = null
        try {
            runBlocking {
                withTimeout(5000) {
                    async(Dispatchers.Default) {
                        call.execute { }
                    }.await()
                }
            }
        } catch (e: StreamTransportException) {
            caughtException = e
        }

        // 断言
        assertNotNull("应该抛出 StreamTransportException", caughtException)
        assertEquals("code 应为 STREAM_TIMEOUT_ERROR", "STREAM_TIMEOUT_ERROR", caughtException?.code)
        assertEquals("retryable 应为 true", true, caughtException?.retryable)
        assertEquals("httpStatus 应为 null", null, caughtException?.httpStatus)
        assertTrue("disconnect() 应被调用至少一次，实际：${disconnectCount.get()}", disconnectCount.get() >= 1)
    }
}
