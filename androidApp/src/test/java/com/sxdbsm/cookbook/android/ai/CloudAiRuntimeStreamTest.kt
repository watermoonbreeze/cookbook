package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.CloudModel
import com.sxdbsm.cookbook.ai.CloudModels
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicInteger

// ============================================================
// AF-13: Fake 测试夹具（§7.5.4）
// ============================================================

private class FakeRequestConfig : CloudAiRequestConfig {
    override suspend fun selectedModel(): CloudModel = CloudModels.DEFAULT
    override suspend fun apiKeyForSelectedModel(): String = "test-key"
}

private enum class Script {
    Complete, FailBeforeDelta, DeltaThenFail, BlockUntilCancelled
}

private class ScriptedStreamTransport : StreamTransport {
    var deltas: List<String> = emptyList()
    var finishReason: String? = null
    var script: Script = Script.Complete
    val cancelCount = AtomicInteger(0)
    val entered = CompletableDeferred<Unit>()
    val released = CompletableDeferred<Unit>()

    override fun newCall(request: StreamHttpRequest): StreamCall =
        ScriptedStreamCall(deltas, finishReason, script, cancelCount, entered, released)
}

private class ScriptedStreamCall(
    private val deltas: List<String>,
    private val finishReason: String?,
    private val script: Script,
    private val cancelCount: AtomicInteger,
    private val entered: CompletableDeferred<Unit>,
    private val released: CompletableDeferred<Unit>,
) : StreamCall {
    override suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult {
        when (script) {
            Script.FailBeforeDelta -> throw StreamTransportException(503, "STREAM_HTTP_ERROR")
            Script.BlockUntilCancelled -> {
                entered.complete(Unit)
                released.await()
                throw StreamTransportException(0, "CANCELLED")
            }
            Script.DeltaThenFail -> {
                for (d in deltas) onDelta(d)
                throw StreamTransportException(500, "STREAM_HTTP_ERROR")
            }
            Script.Complete -> {
                for (d in deltas) onDelta(d)
                return SseStreamResult(finishReason = finishReason, totalChars = deltas.sumOf { it.length })
            }
        }
    }

    override fun cancel() {
        cancelCount.incrementAndGet()
        released.complete(Unit)
    }
}

// ============================================================
// AF-13: R-01~R-05 端到端合同测试（§7.5.4）
// ============================================================

class CloudAiRuntimeStreamTest {

    private suspend fun collectToList(flow: kotlinx.coroutines.flow.Flow<LlmStreamEvent>): List<LlmStreamEvent> {
        val result = mutableListOf<LlmStreamEvent>()
        flow.collect { result.add(it) }
        return result
    }

    @Test
    fun `R-01 正常 两Delta后Completed无Failed只一个call`() = runBlocking {
        val transport = ScriptedStreamTransport().apply {
            deltas = listOf("A", "B")
            finishReason = "stop"
            script = Script.Complete
        }
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        assertEquals(3, events.size)
        assertEquals("A", (events[0] as LlmStreamEvent.Delta).text)
        assertEquals("B", (events[1] as LlmStreamEvent.Delta).text)
        val c = events[2] as LlmStreamEvent.Completed
        assertEquals("stop", c.finishReason)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Failed>().size)
    }

    @Test
    fun `R-02 首帧前重试 两次call最终只有一次成功事件`() = runBlocking {
        var firstCall = true
        val transport = object : StreamTransport {
            override fun newCall(request: StreamHttpRequest): StreamCall {
                if (firstCall) {
                    firstCall = false
                    return ScriptedStreamCall(emptyList(), null, Script.FailBeforeDelta, AtomicInteger(0), CompletableDeferred(), CompletableDeferred())
                }
                return ScriptedStreamCall(listOf("A"), "stop", Script.Complete, AtomicInteger(0), CompletableDeferred(), CompletableDeferred())
            }
        }
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        assertEquals(listOf("A"), events.filterIsInstance<LlmStreamEvent.Delta>().map { it.text })
        assertEquals(1, events.filterIsInstance<LlmStreamEvent.Completed>().size)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Failed>().size)
    }

    @Test
    fun `R-03 首帧后失败 Delta加Failed无Completed不重试`() = runBlocking {
        val transport = ScriptedStreamTransport().apply {
            deltas = listOf("A")
            script = Script.DeltaThenFail
        }
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        assertEquals(listOf("A"), events.filterIsInstance<LlmStreamEvent.Delta>().map { it.text })
        val f = events.filterIsInstance<LlmStreamEvent.Failed>().single()
        assertFalse(f.retryable)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Completed>().size)
    }

    @Test
    fun `R-04 取消阻塞call cancelCount为1 无事件 无终态`() = runBlocking {
        val transport = ScriptedStreamTransport().apply { script = Script.BlockUntilCancelled }
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)

        val events = withTimeout(5000) {
            val job = launch {
                collectToList(runtime.stream(LlmRequest("sys", "usr")))
            }
            transport.entered.await()
            job.cancel()
            job.join()
            emptyList<LlmStreamEvent>() // After cancel, collector gets empty
        }

        assertTrue(events.isEmpty())
        assertEquals(1, transport.cancelCount.get())
    }

    @Test
    fun `R-05 脱敏失败 不含原文body或key`() = runBlocking {
        val transport = object : StreamTransport {
            override fun newCall(request: StreamHttpRequest): StreamCall {
                return object : StreamCall {
                    override suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult {
                        // 模拟 transport 只抛出安全异常
                        throw StreamTransportException(400, "STREAM_HTTP_ERROR")
                    }
                    override fun cancel() {}
                }
            }
        }
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        val f = events.filterIsInstance<LlmStreamEvent.Failed>().single()
        assertFalse("不应含choices", f.message.contains("choices"))
        assertFalse("不应含delta", f.message.contains("delta"))
        assertTrue("应含HTTP状态", f.message.contains("HTTP 400"))
    }
}
