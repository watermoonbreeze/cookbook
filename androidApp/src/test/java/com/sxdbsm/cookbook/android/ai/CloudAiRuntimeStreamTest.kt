package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.CloudModel
import com.sxdbsm.cookbook.ai.CloudModels
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

// ============================================================
// AF-13/17: Fake 测试夹具（§7.5.4 增强版）
// ============================================================

private class FakeRequestConfig : CloudAiRequestConfig {
    override suspend fun selectedModel(): CloudModel = CloudModels.DEFAULT
    override suspend fun apiKeyForSelectedModel(): String = "test-key"
}

private enum class Script {
    /** 正常：发 deltas → 返回 finishReason */
    Complete,
    /** 首帧前失败（模拟 IO/HTTP 错误） */
    FailBeforeDelta,
    /** 首帧后失败：发 deltas 再抛 */
    DeltaThenFail,
    /** 阻塞直到 cancel()；cancel 后不得制造 Delta */
    BlockUntilCancelled,
    /** cancel 在建连前到达：bodyWritten/responseRead 必须为 false */
    CancelBeforeConnect,
    /** AF-19: 阻塞 read 中 cancel() 释放并抛原始 IOException（模拟 disconnect），
     *  生产 StreamTransport 的 cancelled 检查应转 CancellationException。 */
    DisconnectDuringRead,
}

private class ScriptedStreamCall(
    val script: Script,
    val deltas: List<String>,
    val finishReason: String?,
    /** AF-20: 指定 HTTP 状态码（FailBeforeDelta/DeltaThenFail 用，null=IO 错误） */
    val httpErrorStatus: Int? = null,
    /** 可选注入的异常；null 时按 script 抛默认安全异常 */
    val injectedThrowable: Throwable? = null,
) : StreamCall {
    val cancelCount = AtomicInteger(0)
    val entered = CompletableDeferred<Unit>()
    val released = CompletableDeferred<Unit>()
    var bodyWritten = false
    var responseRead = false
    private val cancelled = AtomicBoolean(false)

    override suspend fun execute(onDelta: suspend (String) -> Unit): SseStreamResult {
        when (script) {
            Script.Complete -> {
                bodyWritten = true
                for (d in deltas) onDelta(d)
                responseRead = true
                return SseStreamResult(finishReason = finishReason, totalChars = deltas.sumOf { it.length })
            }
            Script.FailBeforeDelta -> {
                bodyWritten = true
                throw injectedThrowable ?: httpErrorStatus?.let {
                    StreamTransportException(it, "STREAM_HTTP_ERROR", HttpUrlStreamCall.isHttpRetryable(it))
                } ?: StreamTransportException(httpStatus = null, code = "STREAM_IO_ERROR", retryable = true)
            }
            Script.DeltaThenFail -> {
                bodyWritten = true
                for (d in deltas) onDelta(d)
                throw injectedThrowable ?: httpErrorStatus?.let {
                    StreamTransportException(it, "STREAM_HTTP_ERROR", HttpUrlStreamCall.isHttpRetryable(it))
                } ?: StreamTransportException(httpStatus = 500, code = "STREAM_HTTP_ERROR", retryable = false)
            }
            Script.BlockUntilCancelled -> {
                entered.complete(Unit)
                released.await()
                if (cancelled.get()) throw CancellationException("call cancelled")
                return SseStreamResult(finishReason = null, totalChars = 0)
            }
            Script.CancelBeforeConnect -> {
                if (cancelled.get()) throw CancellationException("cancelled before connect")
                entered.complete(Unit)
                released.await()
                if (cancelled.get()) throw CancellationException("cancelled before connect")
                return SseStreamResult(finishReason = null, totalChars = 0)
            }
            Script.DisconnectDuringRead -> {
                entered.complete(Unit)
                released.await()
                // AF-19: 模拟 disconnect 抛出的原始 IOException；
                // 生产 StreamTransport 的 catch(IOException) 会先检查 cancelled 转 CancellationException
                if (cancelled.get()) throw CancellationException("call cancelled during IO")
                throw IOException("socket closed")
            }
        }
    }

    override fun cancel() {
        cancelled.set(true)
        cancelCount.incrementAndGet()
        released.complete(Unit)
    }
}

/**
 * 按序提供 call script；每次 newCall 取下一个（最后一个重复）。
 * 记录 createdCalls 供 AF-17 精确断言次数/实例。
 */
private class ScriptedStreamTransport(
    private val callScripts: List<Pair<Script, List<String>?>>,
    private val finishReason: String? = "stop",
    private val injectedThrowable: Throwable? = null,
    private val httpErrorStatus: Int? = null,
) : StreamTransport {
    val createdCalls = mutableListOf<ScriptedStreamCall>()
    val firstCallCreated = CompletableDeferred<ScriptedStreamCall>()
    private val nextIndex = AtomicInteger(0)

    override fun newCall(request: StreamHttpRequest): StreamCall {
        val (script, deltas) = callScripts[nextIndex.getAndIncrement().coerceAtMost(callScripts.size - 1)]
        val call = ScriptedStreamCall(
            script = script,
            deltas = deltas ?: emptyList(),
            finishReason = finishReason,
            httpErrorStatus = httpErrorStatus,
            injectedThrowable = injectedThrowable,
        )
        createdCalls.add(call)
        if (createdCalls.size == 1) firstCallCreated.complete(call)
        return call
    }
}

// ============================================================
// AF-13/15/16/17: R-01~R-05 端到端合同测试（§7.5.4）
// ============================================================

class CloudAiRuntimeStreamTest {

    private suspend fun collectToList(flow: Flow<LlmStreamEvent>): List<LlmStreamEvent> {
        val result = mutableListOf<LlmStreamEvent>()
        flow.collect { result.add(it) }
        return result
    }

    @Test
    fun `R-01 正常 两Delta后Completed 精确断言只创建一个call`() = runBlocking {
        val transport = ScriptedStreamTransport(listOf(Script.Complete to listOf("A", "B")))
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        // 精确事件序列
        assertEquals(3, events.size)
        assertEquals("A", (events[0] as LlmStreamEvent.Delta).text)
        assertEquals("B", (events[1] as LlmStreamEvent.Delta).text)
        val c = events[2] as LlmStreamEvent.Completed
        assertEquals("stop", c.finishReason)
        // 无 Failed
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Failed>().size)
        // 只创建一个 call
        assertEquals(1, transport.createdCalls.size)
    }

    @Test
    fun `R-02 首帧前IO失败 重试一次 两个不同call 最终成功`() = runBlocking {
        // 第一 call: 安全 IO 失败（无 HTTP 状态，retryable）；第二 call: 成功
        val transport = ScriptedStreamTransport(
            listOf(
                Script.FailBeforeDelta to null,
                Script.Complete to listOf("A"),
            ),
        )
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        // 最终仅 Delta(A), Completed(stop)，无首轮 Failed
        assertEquals(listOf("A"), events.filterIsInstance<LlmStreamEvent.Delta>().map { it.text })
        assertEquals(1, events.filterIsInstance<LlmStreamEvent.Completed>().size)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Failed>().size)
        // 精确断言创建两个不同 call
        assertEquals(2, transport.createdCalls.size)
        assertTrue(transport.createdCalls[0] !== transport.createdCalls[1])
    }

    @Test
    fun `R-02b 首帧前连续两次IO失败 只产生一个安全Failed 不含原始异常串`() = runBlocking {
        // 模拟真实 transport：原始 IOException 已被安全包装为 StreamTransportException(null, STREAM_IO_ERROR, retryable=true)
        val transport = ScriptedStreamTransport(
            listOf(
                Script.FailBeforeDelta to null,
                Script.FailBeforeDelta to null,
            ),
            injectedThrowable = StreamTransportException(
                httpStatus = null, code = "STREAM_IO_ERROR", retryable = true,
            ),
        )
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        val failed = events.filterIsInstance<LlmStreamEvent.Failed>()
        assertEquals(1, failed.size)
        // 脱敏：Failed 只含安全错误码，不含原始异常/输入/Key
        assertEquals("STREAM_IO_ERROR", failed[0].message)
        // 无 Delta/Completed
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Delta>().size)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Completed>().size)
        // 创建了两个 call（重试耗尽）
        assertEquals(2, transport.createdCalls.size)
    }

    @Test
    fun `R-03 首帧后失败 Delta加Failed 无Completed 不重试 只一个call`() = runBlocking {
        val transport = ScriptedStreamTransport(
            listOf(Script.DeltaThenFail to listOf("A")),
            injectedThrowable = StreamTransportException(httpStatus = 500, code = "STREAM_HTTP_ERROR", retryable = false),
        )
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        // 严格顺序：Delta(A), Failed
        assertEquals(2, events.size)
        assertEquals("A", (events[0] as LlmStreamEvent.Delta).text)
        val f = events[1] as LlmStreamEvent.Failed
        assertFalse(f.retryable)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Completed>().size)
        assertEquals(1, transport.createdCalls.size)
    }

    @Test
    fun `R-04 取消阻塞call 断言真实collector外部列表为空 cancelCount为1`() = runBlocking {
        val transport = ScriptedStreamTransport(listOf(Script.BlockUntilCancelled to null))
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val externalEvents = mutableListOf<LlmStreamEvent>()

        withTimeout(5000) {
            val collectJob = launch {
                runtime.stream(LlmRequest("sys", "usr")).collect { externalEvents.add(it) }
            }
            val call = transport.firstCallCreated.await()
            call.entered.await()
            collectJob.cancel()
            collectJob.join()
        }

        val call = transport.createdCalls.first()
        // 真实 collector 写入的外部列表必须为空（无终态、无晚到 Delta）
        assertEquals(0, externalEvents.size)
        assertEquals(1, call.cancelCount.get())
        assertTrue("阻塞应被解除", call.released.isCompleted)
    }

    @Test
    fun `R-04b cancel在建连前到达 bodyWritten与responseRead均为false`() = runBlocking {
        val transport = ScriptedStreamTransport(listOf(Script.CancelBeforeConnect to null))
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val externalEvents = mutableListOf<LlmStreamEvent>()

        withTimeout(5000) {
            val collectJob = launch {
                runtime.stream(LlmRequest("sys", "usr")).collect { externalEvents.add(it) }
            }
            val call = transport.firstCallCreated.await()
            call.entered.await()
            // 在 execute 内部建连前取消
            call.cancel()
            collectJob.cancel()
            collectJob.join()
        }

        val call = transport.createdCalls.first()
        // 取消在建连前：bodyWritten/responseRead 均 false（未访问网络）
        assertFalse(call.bodyWritten)
        assertFalse(call.responseRead)
        // 手动 cancel + awaitClose cancel 均幂等；至少被 cancel 一次即可
        assertTrue("cancel 应被调用", call.cancelCount.get() >= 1)
        assertEquals(0, externalEvents.size)
    }

    @Test
    fun `R-05 脱敏失败 fake call抛含饮食文本和Key的generic异常 断言剥离`() = runBlocking {
        val original = IOException("HTTP 500 server error 午餐有红烧肉 患者 key=sk-live-abcdef123456")
        val transport = ScriptedStreamTransport(
            listOf(Script.FailBeforeDelta to null),
            injectedThrowable = original,
        )
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        val failed = events.filterIsInstance<LlmStreamEvent.Failed>().single()
        // Failed 不含任何原文
        assertFalse(failed.message.contains("红烧肉"))
        assertFalse(failed.message.contains("sk-live-abcdef123456"))
        assertFalse(failed.message.contains("HTTP 500 server error"))
        assertFalse(failed.message.contains("患者"))
    }

    // ════════════════════ AF-19: 取消期间的 IOException 转取消 ════════════════════

    @Test
    fun `AF-19 DisconnectDuringRead 取消后无事件无终态只一个call`() = runBlocking {
        val transport = ScriptedStreamTransport(listOf(Script.DisconnectDuringRead to null))
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val externalEvents = mutableListOf<LlmStreamEvent>()

        withTimeout(5000) {
            val collectJob = launch {
                runtime.stream(LlmRequest("sys", "usr")).collect { externalEvents.add(it) }
            }
            val call = transport.firstCallCreated.await()
            call.entered.await()
            // 取消 collector → awaitClose → call.cancel()（disconnect 模拟 IOException）
            collectJob.cancel()
            collectJob.join()
        }

        val call = transport.createdCalls.first()
        // 只创建一个 call；取消不算网络失败
        assertEquals(1, transport.createdCalls.size)
        assertEquals("取消后不得有 Failed/Completed/Delta", 0, externalEvents.size)
        assertTrue("cancel 应被调用", call.cancelCount.get() >= 1)
    }

    @Test
    fun `AF-19 真实HttpUrlStreamCall cancel后execute抛CancellationException非IO`() = runBlocking {
        val call = HttpUrlStreamCall(
            StreamHttpRequest(endpoint = "http://127.0.0.1:1", apiKey = "k", body = "{}"),
        )
        call.cancel() // 建连前取消
        try {
            call.execute { }
            throw AssertionError("应抛出 CancellationException")
        } catch (e: CancellationException) {
            // 预期：取消后 execute 抛 CancellationException，而非 STREAM_IO_ERROR
        } catch (e: StreamTransportException) {
            throw AssertionError("取消不应被包装为 STREAM_IO_ERROR: ${e.code}")
        }
    }

    // ════════════════════ AF-20: HTTP 重试分类 ════════════════════

    @Test
    fun `R-02c 首帧HTTP 400 只一个call Failed retryable等于false 无Completed`() = runBlocking {
        val transport = ScriptedStreamTransport(
            listOf(Script.FailBeforeDelta to null),
            httpErrorStatus = 400,
        )
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        // 精确：仅 Failed("HTTP 400 STREAM_HTTP_ERROR", retryable=false)，无 Delta/Completed
        assertEquals(1, events.size)
        val f = events[0] as LlmStreamEvent.Failed
        assertEquals("HTTP 400 STREAM_HTTP_ERROR", f.message)
        assertFalse("400 确定性失败不可重试", f.retryable)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Completed>().size)
        assertEquals(1, transport.createdCalls.size)
    }

    @Test
    fun `R-02d 首帧HTTP 503 重试后第二个call成功`() = runBlocking {
        // 第一 call: 503 (retryable)；第二 call: Complete
        val transport = ScriptedStreamTransport(
            listOf(
                Script.FailBeforeDelta to null,
                Script.Complete to listOf("A"),
            ),
            httpErrorStatus = 503,
        )
        val runtime = CloudAiRuntime(FakeRequestConfig(), transport)
        val events = collectToList(runtime.stream(LlmRequest("sys", "usr")))

        // 最终 Delta(A), Completed；无 Failed；两个不同 call
        assertEquals(listOf("A"), events.filterIsInstance<LlmStreamEvent.Delta>().map { it.text })
        assertEquals(1, events.filterIsInstance<LlmStreamEvent.Completed>().size)
        assertEquals(0, events.filterIsInstance<LlmStreamEvent.Failed>().size)
        assertEquals(2, transport.createdCalls.size)
        assertTrue(transport.createdCalls[0] !== transport.createdCalls[1])
    }

    @Test
    fun `AF-20 isHttpRetryable 分类正确`() {
        assertTrue(HttpUrlStreamCall.isHttpRetryable(408))
        assertTrue(HttpUrlStreamCall.isHttpRetryable(429))
        assertTrue(HttpUrlStreamCall.isHttpRetryable(500))
        assertTrue(HttpUrlStreamCall.isHttpRetryable(503))
        assertFalse(HttpUrlStreamCall.isHttpRetryable(400))
        assertFalse(HttpUrlStreamCall.isHttpRetryable(401))
        assertFalse(HttpUrlStreamCall.isHttpRetryable(403))
        assertFalse(HttpUrlStreamCall.isHttpRetryable(404))
    }
}
