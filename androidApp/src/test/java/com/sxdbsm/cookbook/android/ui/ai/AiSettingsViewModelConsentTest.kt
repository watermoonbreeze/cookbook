package com.sxdbsm.cookbook.android.ui.ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.ConsentSource
import com.sxdbsm.cookbook.ai.ConsentStatus
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * L1 蓝图 §8.2：AiSettingsViewModel 同意动作函数（INV-L1-06/07/09/12）。
 *
 * 用真实 `AiRuntimeConfig(prefs)` + 内存 SQLite（两类型 final，无接口可 mock，蓝图 §8.1 GC-07/C-17）。
 * viewModelScope 需要 Main 调度器，沿用 AiMealInputViewModelStreamTest 的 setMain(UnconfinedTestDispatcher) 惯例；
 * VM 动作是 fire-and-forget launch、内部经 `withContext(Dispatchers.IO)` 写偏好——故断言前用 [awaitUntil] 轮询等写落库，
 * 避免时序竞态（初版曾 5/8 因写未完成而断言失败）。
 * [AI生成] L1。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiSettingsViewModelConsentTest {

    private fun inMemoryDb(): CookbookDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        return CookbookDatabase(driver)
    }

    private fun runVmTest(block: suspend (vm: AiSettingsViewModel, config: AiRuntimeConfig) -> Unit) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            runBlocking {
                val db = inMemoryDb()
                val config = AiRuntimeConfig(PreferenceRepository(db))
                val vm = AiSettingsViewModel(config)
                // 等 init{reload()} 完成（loaded=true）：其最终的 state.copy 会整体覆盖 keyByVendor/cloudAiConsent，
                // 若不先等它落地，后续 VM 动作写入的 state 镜像可能被 reload 的旧快照覆盖导致断言时序竞态。
                awaitUntil { vm.state.loaded }
                block(vm, config)
            }
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** 轮询等待 VM 的 launch 协程（含 withContext(IO) 异步写）到达期望终态，超时抛错。 */
    private suspend fun awaitUntil(timeoutMs: Long = 5_000, predicate: suspend () -> Boolean) {
        withTimeout(timeoutMs) {
            while (!predicate()) delay(5)
        }
    }

    // ── INV-L1-06：grantConsent ──

    @Test
    fun `T-L1-06a grantConsent先写consent后写Key 且内存态与持久化态一致`() = runVmTest { vm, config ->
        config.setVendorApiKey("deepseek", "sk-ds") // 另一厂商已有 grandfather 遗留 Key
        vm.grantConsent("zhipu", "sk-zhipu", ConsentSource.EXPLICIT_FIRST_ENABLE)
        awaitUntil {
            config.cloudAiConsent().status == ConsentStatus.GRANTED &&
                config.vendorApiKey("zhipu") == "sk-zhipu" &&
                vm.state.cloudAiConsent.status == ConsentStatus.GRANTED && // 内存态镜像也落地
                vm.state.keyByVendor["zhipu"] == "sk-zhipu"
        }
        val consent = config.cloudAiConsent()
        assertEquals(ConsentStatus.GRANTED, consent.status)
        // Key 已保存
        assertEquals("sk-zhipu", config.vendorApiKey("zhipu"))
        // acknowledgedVendors = 此刻扫描到的全部已配置Key厂商 ∪ {当前vendor}（非仅当前vendor）
        // ——若"扫描到另一厂商"漏了 zhipu（忘记显式并入），这里会缺 "zhipu"；若只扫当前厂商，会缺 "deepseek"
        assertEquals(setOf("deepseek", "zhipu"), consent.acknowledgedVendors)
        // 同一对象镜像进内存态（GC-29）
        assertEquals(consent, vm.state.cloudAiConsent)
        assertEquals(ConsentStatus.GRANTED, vm.state.cloudAiConsent.status)
        assertEquals("sk-zhipu", vm.state.keyByVendor["zhipu"])
    }

    @Test
    fun `T-L1-06b grantConsent对已确认vendor追加不丢失`() = runVmTest { vm, config ->
        vm.grantConsent("deepseek", "sk-ds", ConsentSource.GRANDFATHER_CONFIRMED) // 首次：仅 deepseek
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.GRANTED && config.vendorApiKey("deepseek") == "sk-ds" }
        vm.grantConsent("zhipu", "sk-z", ConsentSource.EXPLICIT_FIRST_ENABLE) // 再启 zhipu
        awaitUntil { config.vendorApiKey("zhipu") == "sk-z" }
        val consent = config.cloudAiConsent()
        assertEquals(ConsentStatus.GRANTED, consent.status)
        assertTrue(consent.acknowledgedVendors.containsAll(setOf("deepseek", "zhipu")))
    }

    // ── INV-L1-07：declineConsent / closeCloudAi ──

    @Test
    fun `T-L1-07a 面板暂不启用 返回 DECLINED且activeType回退MOCK且Key不保存`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-abc") // 拒绝场景：Key 本就不该被保存/改动
        vm.declineConsent()
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.DECLINED }
        assertEquals(ConsentStatus.DECLINED, config.cloudAiConsent().status)
        assertEquals(AiRuntimeType.MOCK, config.activeType())
        assertEquals("sk-abc", config.vendorApiKey("zhipu")) // Key 未被动过
    }

    @Test
    fun `T-L1-07b 关闭保留密钥 返回 DECLINED且activeType MOCK且Key不清空`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-abc")
        vm.closeCloudAi("zhipu", deleteKey = false)
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.DECLINED }
        assertEquals(ConsentStatus.DECLINED, config.cloudAiConsent().status)
        assertEquals(AiRuntimeType.MOCK, config.activeType())
        assertEquals("sk-abc", config.vendorApiKey("zhipu"))
    }

    @Test
    fun `T-L1-07c 关闭并删除密钥 返回 Key被清空`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-abc")
        vm.closeCloudAi("zhipu", deleteKey = true)
        awaitUntil { config.vendorApiKey("zhipu").isEmpty() }
        assertEquals("", config.vendorApiKey("zhipu"))
        assertEquals(ConsentStatus.DECLINED, config.cloudAiConsent().status)
        assertEquals(AiRuntimeType.MOCK, config.activeType())
    }

    // ── INV-L1-09：resolveGrandfather ──

    @Test
    fun `T-L1-09a 继续使用 返回 GRANDFATHER_CONFIRMED且acknowledgedVendors含全部已配置厂商且Key不动`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-z")
        config.setVendorApiKey("deepseek", "sk-d")
        vm.resolveGrandfather(confirm = true)
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.GRANTED }
        val consent = config.cloudAiConsent()
        assertEquals(ConsentStatus.GRANTED, consent.status)
        assertEquals(ConsentSource.GRANDFATHER_CONFIRMED, consent.source)
        assertEquals(setOf("zhipu", "deepseek"), consent.acknowledgedVendors)
        assertEquals("sk-z", config.vendorApiKey("zhipu")) // Key 未改动（resolveGrandfather 不接收 key）
        assertEquals("sk-d", config.vendorApiKey("deepseek"))
    }

    @Test
    fun `T-L1-09b 关闭云端AI 返回 DECLINED且activeType MOCK且Key不动`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-z")
        vm.resolveGrandfather(confirm = false)
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.DECLINED }
        assertEquals(ConsentStatus.DECLINED, config.cloudAiConsent().status)
        assertEquals(AiRuntimeType.MOCK, config.activeType())
        assertEquals("sk-z", config.vendorApiKey("zhipu")) // Key 未改动
    }

    // ── INV-L1-12：DECLINED 后重选 CLOUD（onTypeChange 无校验·本批不改它） ──

    @Test
    fun `T-L1-12 DECLINED后重选CLOUD - 状态块隐藏但重新启用行判据成立`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-abc")
        vm.declineConsent() // → DECLINED + activeType MOCK
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.DECLINED && config.activeType() == AiRuntimeType.MOCK }
        vm.onTypeChange(AiRuntimeType.CLOUD) // 用户手动把单选切回"云端大模型"
        awaitUntil { vm.state.type == AiRuntimeType.CLOUD }
        assertEquals(AiRuntimeType.CLOUD, vm.state.type)
        assertEquals(ConsentStatus.DECLINED, vm.state.cloudAiConsent.status)
        // 常驻状态块：需 GRANTED 且 Key 非空 → DECLINED 下隐藏
        assertFalse(shouldShowCloudStatusBlock(vm.state.cloudAiConsent, vm.state.keyByVendor, "zhipu"))
        // "云端 AI 已被你关闭 · 重新启用 ›"行渲染判据：type==CLOUD && status==DECLINED（与 CloudSection 内联条件一致）
        assertTrue(vm.state.type == AiRuntimeType.CLOUD && vm.state.cloudAiConsent.status == ConsentStatus.DECLINED)
    }

    @Test
    fun `T-L1-03 grantConsent以空Key调用不得清空已保存Key`() = runVmTest { vm, config ->
        config.setVendorApiKey("zhipu", "sk-keep")
        vm.grantConsent("zhipu", "", ConsentSource.EXPLICIT_FIRST_ENABLE)
        awaitUntil { config.cloudAiConsent().status == ConsentStatus.GRANTED }
        assertEquals("sk-keep", config.vendorApiKey("zhipu"))
    }
}
