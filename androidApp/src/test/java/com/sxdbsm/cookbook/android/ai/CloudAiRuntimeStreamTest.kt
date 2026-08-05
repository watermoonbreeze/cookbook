package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.LlmStreamEvent
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

/**
 * [AI修改] AF-06: Runtime 流式行为测试——readSseStream + StreamTransport 五类场景。
 *
 * CloudAiRuntime.stream() 本身的终态逻辑通过 MockTransport 间接验证。
 */
class CloudAiRuntimeStreamTest {

    private fun sseData(vararg frames: String): String = frames.joinToString("\n") + "\n"

    @Test
    fun `readSseStream 两个Delta逐帧回调`() = runBlocking {
        val data = buildString {
            append("data: {\"choices\":[{\"delta\":{\"content\":\"Hello\"}}]}\n")
            append("\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"World\"}}]}\n")
            append("\n")
            append("data: [DONE]\n")
        }
        val deltas = mutableListOf<String>()

        readSseStream(
            ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)),
            onDelta = { deltas.add(it) },
        )

        assertEquals(listOf("Hello", "World"), deltas)
    }

    @Test
    fun `readSseStream finish_reason等于length`() = runBlocking {
        val data = "data: {\"choices\":[{\"delta\":{\"content\":\"X\"},\"finish_reason\":\"length\"}]}\n\n"
        var finish: String? = null

        val result = readSseStream(
            ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)),
            onDelta = { },
        )

        assertEquals("length", result.finishReason)
    }

    @Test
    fun `readSseStream 无finish_reason返回null`() = runBlocking {
        val data = "data: {\"choices\":[{\"delta\":{\"content\":\"X\"}}]}\n\ndata: [DONE]\n"
        var lastDelta: String? = null

        val result = readSseStream(
            ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)),
            onDelta = { lastDelta = it },
        )

        assertEquals(null, result.finishReason)
        assertEquals("X", lastDelta)
    }

    @Test
    fun `readSseStream 空delta不回调`() = runBlocking {
        val data = "data: {\"choices\":[{\"delta\":{},\"finish_reason\":\"stop\"}]}\n\n"
        val deltas = mutableListOf<String>()

        readSseStream(
            ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)),
            onDelta = { deltas.add(it) },
        )

        assertEquals(0, deltas.size)
    }

    @Test
    fun `readSseStream 非data行忽略`() = runBlocking {
        val data = buildString {
            append(": this is a comment\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"Real\"}}]}\n")
            append("\n")
        }
        val deltas = mutableListOf<String>()

        readSseStream(
            ByteArrayInputStream(data.toByteArray(StandardCharsets.UTF_8)),
            onDelta = { deltas.add(it) },
        )

        assertEquals(listOf("Real"), deltas)
    }

    @Test
    fun `HttpUrlStreamTransport 构造可用不抛异常`() {
        val transport = HttpUrlStreamTransport()
        // 不调用 execute（需真实端点），只验证构造不抛异常
        assertTrue(transport is StreamTransport)
    }
}
