package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * K1i 蓝图 §8.2：`SwitchableAiRuntime.stream()` 真实委托（INV-K1I-01~04）。
 *
 * 用真实 `AiRuntimeConfig(prefs)` + 内存 SQLite + fake `AiRuntime`（构造 `runtimes` map 传入，既有可测试性设计）。
 * 测试策略按蓝图 §8.1 v1b 修订：`:shared` androidUnitTest 测试 classpath 无 `kotlinx-coroutines-test`，
 * 全部用 `runBlocking`（不做 TestScope/runTest）。
 * [AI生成] K1i。
 */
class SwitchableAiRuntimeStreamTest {

    private fun newConfig(): AiRuntimeConfig {
        val db = RepositoryTestDatabase.create()
        return AiRuntimeConfig(PreferenceRepository(db))
    }

    /** 让同意闸门放行（CLOUD + GRANTED + 非空 Key），隔离非闸门场景的测试。 */
    private suspend fun grantConsent(config: AiRuntimeConfig) {
        config.setActiveType(AiRuntimeType.CLOUD) // [AI修改] Google 质量终审 K1i S1：显式设 activeType，不依赖 from(null) 兜底默认值
        config.setVendorApiKey("zhipu", "sk-abc")
        config.setCloudAiConsent(
            CloudAiConsent(status = ConsentStatus.GRANTED, scopeVersion = 1, acknowledgedVendors = setOf("zhipu")),
        )
    }

    /** fake runtime：stream() 返回多个 Delta（模拟真实 SSE 分片），记录 stream() 调用次数。 */
    private class MultiDeltaRuntime(private val deltas: List<String>) : AiRuntime {
        var streamCalls = 0
        override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
        override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
            streamCalls++
            return flowOf(*deltas.map { LlmStreamEvent.Delta(it) }.toTypedArray())
        }
    }

    @Test
    fun `T-K1I-01 真实委托-收集到fake的多个Delta而非默认包装的单Delta`() = runBlocking {
        val config = newConfig()
        grantConsent(config)
        val fake = MultiDeltaRuntime(listOf("段1", "段2", "段3"))
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to fake))

        val events = switchable.stream(LlmRequest(system = "", user = "")).toList()
        val deltas = events.filterIsInstance<LlmStreamEvent.Delta>()
        // 恰好 3 个 Delta（默认实现只可能产出 1 个整段 Delta）——直接证伪"委托"与"包装"的行为差异
        assertEquals(3, deltas.size)
        assertEquals(listOf("段1", "段2", "段3"), deltas.map { it.text })
        assertEquals(1, fake.streamCalls)
    }

    @Test
    fun `T-K1I-02 未同意时stream不委托且Failed同源文案`() = runBlocking {
        val config = newConfig()
        config.setVendorApiKey("zhipu", "sk-abc")
        config.setCloudAiConsent(CloudAiConsent(status = ConsentStatus.DECLINED))
        val fake = MultiDeltaRuntime(listOf("不该出现"))
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to fake))

        val events = switchable.stream(LlmRequest(system = "", user = "")).toList()
        assertEquals(1, events.size)
        val failed = events[0] as LlmStreamEvent.Failed
        // 与 complete() 路径的 CloudAiConsentRequiredException 同源同文案（GC-13/INV-K1I-02）
        assertEquals(CloudAiConsentRequiredException().message, failed.message)
        assertFalse(failed.retryable)
        assertEquals(0, fake.streamCalls) // 数据未离开设备：CLOUD runtime 零调用
    }

    @Test
    fun `T-K1I-03 runtimes缺activeType时回退MOCK-再缺则Failed`() = runBlocking {
        // 场景1：CLOUD 未注册，map 只有 MOCK → 回退 MOCK（同意闸门放行以隔离本测试关注的回退逻辑）
        val config1 = newConfig()
        grantConsent(config1)
        val mock = MultiDeltaRuntime(listOf("mock-ok"))
        val sw1 = SwitchableAiRuntime(config1, mapOf(AiRuntimeType.MOCK to mock))
        val events1 = sw1.stream(LlmRequest(system = "", user = "")).toList()
        assertEquals(listOf("mock-ok"), events1.filterIsInstance<LlmStreamEvent.Delta>().map { it.text })
        assertEquals(1, mock.streamCalls)

        // 场景2：两者都没有 → Failed（与 complete() 回退语义对齐，不新造一套）
        val config2 = newConfig()
        grantConsent(config2)
        val sw2 = SwitchableAiRuntime(config2, emptyMap())
        val events2 = sw2.stream(LlmRequest(system = "", user = "")).toList()
        assertEquals(1, events2.size)
        val failed2 = events2[0] as LlmStreamEvent.Failed
        assertTrue(failed2.message?.contains("no AiRuntime registered for CLOUD") == true) // [AI修改] Google 质量终审 K1i O2：补 message 精确断言
    }

    @Test
    fun `T-K1I-04 取消信号透传到底层stream`() = runBlocking {
        val config = newConfig()
        grantConsent(config)
        var cancelled = false
        val firstDeltaReceived = CompletableDeferred<Unit>()
        val blockingRuntime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                emit(LlmStreamEvent.Delta("x")) // 确定性同步点：外层收到后再 cancel
                firstDeltaReceived.complete(Unit)
                try {
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }
        }
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to blockingRuntime))

        val job = launch {
            switchable.stream(LlmRequest(system = "", user = "")).collect { }
        }
        firstDeltaReceived.await()
        job.cancelAndJoin()
        assertTrue("取消信号未透传到底层 stream（emitAll 吞掉了取消）", cancelled)
    }
}
