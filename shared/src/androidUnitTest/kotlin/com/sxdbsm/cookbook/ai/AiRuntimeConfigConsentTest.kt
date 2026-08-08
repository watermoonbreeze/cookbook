package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L1 蓝图 §8.2：云端 AI 同意状态推导（INV-L1-01）+ 运行时闸门（INV-L1-02）。
 *
 * 用真实 `PreferenceRepository(db)` + `AiRuntimeConfig(prefs)`（db 走 RepositoryTestDatabase 内存 SQLite），
 * 测试真实持久化往返——两类型均 final，无接口可 mock（蓝图 §8.1 GC-07）。
 * [AI生成] L1。
 */
class AiRuntimeConfigConsentTest {

    private fun newConfig(): AiRuntimeConfig {
        val db = RepositoryTestDatabase.create()
        return AiRuntimeConfig(PreferenceRepository(db))
    }

    /** 记录 CLOUD runtime 是否被调用（T-L1-02a 断言"零调用"）。stream() 意外触达时发 Failed 立即可辨识（Google 质量终审 🟡#3）。 */
    private class RecordingCloudRuntime : AiRuntime {
        var calls = 0
        override suspend fun complete(request: LlmRequest): Result<String> {
            calls++
            return Result.success("{}")
        }
        override fun stream(request: LlmRequest): Flow<LlmStreamEvent> =
            flowOf(LlmStreamEvent.Failed("TEST_ERROR: stream() should not be called in consent gate tests"))
    }

    // ── INV-L1-01：cloudAiConsent() 推导 ──

    @Test
    fun `T-L1-01a 无consent记录且全部厂商Key空 返回 NOT_ASKED`() = runBlocking {
        val config = newConfig()
        assertEquals(ConsentStatus.NOT_ASKED, config.cloudAiConsent().status)
    }

    @Test
    fun `T-L1-01b 无consent记录但另一厂商Key非空 返回 GRANDFATHER_PENDING`() = runBlocking {
        val config = newConfig()
        // 默认选中厂商是 zhipu（CloudModels.DEFAULT）；给 deepseek 配 Key（非当前选中厂商）仍须推导 grandfather
        config.setVendorApiKey("deepseek", "sk-abc")
        assertEquals(ConsentStatus.GRANDFATHER_PENDING, config.cloudAiConsent().status)
    }

    @Test
    fun `T-L1-01c consent记录存在但解码失败 返回 fail-closed NOT_ASKED 即使有Key`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val prefs = PreferenceRepository(db)
        val config = AiRuntimeConfig(prefs)
        config.setVendorApiKey("zhipu", "sk-abc")
        prefs.set(AiRuntimeConfig.KEY_CLOUD_AI_CONSENT, "{ 这不是合法的 JSON ") // 脏字符串（损坏的 DECLINED 模拟）
        assertEquals(ConsentStatus.NOT_ASKED, config.cloudAiConsent().status) // 不得落入 grandfather 推导
    }

    // ── INV-L1-02：SwitchableAiRuntime.complete() 闸门 ──

    @Test
    fun `T-L1-02a DECLINED+非空Key 返回 complete failure 且 CLOUD runtime 零调用`() = runBlocking {
        val config = newConfig()
        config.setVendorApiKey("zhipu", "sk-abc")
        config.setCloudAiConsent(CloudAiConsent(status = ConsentStatus.DECLINED))
        val cloud = RecordingCloudRuntime()
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to cloud))
        val result = switchable.complete(LlmRequest(system = "", user = ""))
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is CloudAiConsentRequiredException)
        assertEquals(0, cloud.calls) // 未路由到 CloudAiRuntime
    }

    @Test
    fun `T-L1-02b GRANTED+非空Key 返回 正常路由 CLOUD runtime 调用一次`() = runBlocking {
        val config = newConfig()
        config.setVendorApiKey("zhipu", "sk-abc")
        config.setCloudAiConsent(
            CloudAiConsent(status = ConsentStatus.GRANTED, scopeVersion = 1, acknowledgedVendors = setOf("zhipu")),
        )
        val cloud = RecordingCloudRuntime()
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to cloud))
        val result = switchable.complete(LlmRequest(system = "", user = ""))
        assertTrue(result.isSuccess)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun `T-L1-02c GRANDFATHER_PENDING+非空Key 返回 正常路由 CLOUD runtime 调用一次`() = runBlocking {
        val config = newConfig()
        config.setVendorApiKey("zhipu", "sk-abc") // 无 consent 记录 → 推导 GRANDFATHER_PENDING
        assertEquals(ConsentStatus.GRANDFATHER_PENDING, config.cloudAiConsent().status)
        val cloud = RecordingCloudRuntime()
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to cloud))
        val result = switchable.complete(LlmRequest(system = "", user = ""))
        assertTrue(result.isSuccess)
        assertEquals(1, cloud.calls)
    }

    @Test
    fun `T-L1-02d NOT_ASKED+非空Key 返回 complete failure`() = runBlocking {
        val config = newConfig()
        config.setVendorApiKey("zhipu", "sk-abc")
        // 该初态（有 Key 但 consent 显式为 NOT_ASKED）在推导规则下不可自然产生，此处直接预写偏好构造（防御性覆盖）。
        config.setCloudAiConsent(CloudAiConsent())
        val cloud = RecordingCloudRuntime()
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to cloud))
        val result = switchable.complete(LlmRequest(system = "", user = ""))
        assertTrue(result.isFailure)
        assertEquals(0, cloud.calls)
    }
}
