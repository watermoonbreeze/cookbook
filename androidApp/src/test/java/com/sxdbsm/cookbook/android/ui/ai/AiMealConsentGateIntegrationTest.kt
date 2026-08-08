package com.sxdbsm.cookbook.android.ui.ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.CloudAiConsent
import com.sxdbsm.cookbook.ai.SwitchableAiRuntime
import com.sxdbsm.cookbook.ai.ConsentStatus
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import com.sxdbsm.cookbook.ai.meallog.DayMealJson
import com.sxdbsm.cookbook.ai.meallog.DishJson
import com.sxdbsm.cookbook.ai.meallog.MealDishRefJson
import com.sxdbsm.cookbook.ai.meallog.MealJson
import com.sxdbsm.cookbook.ai.meallog.MultiDayRecorder
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.autogen.AutoGenPreview
import com.sxdbsm.cookbook.domain.autogen.AutoGenResult
import com.sxdbsm.cookbook.domain.autogen.DayPreview
import com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * L1 蓝图 §8.2 INV-L1-03：AI 记一餐消费点在"同意未满足"时沿既有失败路径自动降级（不改任何消费点代码）。
 *
 * - T-L1-03a：`submit()` 走 `SwitchableAiRuntime` 闸门 → `LlmStreamEvent.Failed` → `attemptRuleFallback()`
 *   → 最终 `parseSourceMessage` **精确等于**完整字面量（非仅"含规则解析"字样，防文案重复/病句类缺陷被掩盖）。
 * - T-L1-03b：`confirmHealthAdvice()`（无规则兜底的消费点）→ `healthAdviceError` 为该异常 message，`healthAdvice==null`。
 *
 * 集成测试真实构造 `AiMealInputViewModel`（DECLINED + 非空 Key），不改既有 `AiMealInputViewModelStreamTest.kt` 任何一行。
 * [AI生成] L1。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AiMealConsentGateIntegrationTest {

    private val targetDate = LocalDate(2026, 8, 5)

    private fun inMemoryDb(): CookbookDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        return CookbookDatabase(driver)
    }

    /** 规则兜底恒产出合法午餐（AI 侧零产出）。 */
    private class RuleOnlySessionPort : AiMealSessionPort {
        override suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview =
            AutoGenPreview(
                days = days.map { day ->
                    DayPreview(
                        date = runCatching { LocalDate.parse(day.date!!) }.getOrElse { targetDate },
                        meals = emptyList(),
                        hasExisting = false,
                    )
                },
                warnings = emptyList(),
            )

        override suspend fun commit(preview: AutoGenPreview): AutoGenResult =
            AutoGenResult(1, 1, 0, 0, 0, 0, emptyList(), emptyList())

        override suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult =
            RuleFallbackResult(
                days = listOf(
                    DayMealJson(
                        date = "2026-08-05",
                        meals = listOf(
                            MealJson(
                                meal_type = "lunch",
                                dishes = listOf(MealDishRefJson(name = "红烧肉", dish = DishJson(name = "红烧肉"))),
                            ),
                        ),
                    ),
                ),
                warning = null,
            )
    }

    /** 记录 CLOUD runtime 是否被调用（闸门拦截断言"零网络调用"）。stream() 意外触达时发 Failed 立即可辨识（Google 质量终审 🟡#3）。 */
    private class RecordingCloudRuntime : AiRuntime {
        var calls = 0
        override suspend fun complete(request: LlmRequest): Result<String> {
            calls++
            return Result.success("{}")
        }
        override fun stream(request: LlmRequest): Flow<LlmStreamEvent> =
            flowOf(LlmStreamEvent.Failed("TEST_ERROR: stream() should not be called in consent gate tests"))
    }

    /** 构造"云端已选中 + 非空 Key + DECLINED 同意"的 VM（闸门由 SwitchableAiRuntime 承载）。 */
    private suspend fun createDeclinedVm(cloud: RecordingCloudRuntime): Pair<AiMealInputViewModel, AiRuntimeConfig> {
        val db = inMemoryDb()
        val prefs = PreferenceRepository(db)
        val config = AiRuntimeConfig(prefs)
        config.setVendorApiKey("zhipu", "sk-abc")
        config.setCloudAiConsent(CloudAiConsent(status = ConsentStatus.DECLINED))
        val switchable = SwitchableAiRuntime(config, mapOf(AiRuntimeType.CLOUD to cloud))
        val ingredientRepo = IngredientRepository(db)
        val recorder = MultiDayRecorder(
            ingredientRepo, DishRepository(db), MealRecordRepository(db), NutritionRepository(db),
            IngredientAliasResolver(emptyMap()), db,
        )
        val vm = AiMealInputViewModel(
            initialText = "中午吃了红烧肉和米饭", targetDate = targetDate,
            aiRuntime = switchable, config = config, recorder = recorder,
            ingredientRepo = ingredientRepo,
            healthRepo = HealthProfileRepository(db), familyRepo = FamilyRepository(db, prefs),
        )
        vm.replaceSessionPortForTest(RuleOnlySessionPort())
        return vm to config
    }

    private fun runVmTest(block: suspend () -> Unit) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            runBlocking { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `T-L1-03a 未同意时submit走规则兜底且文案精确等于完整字面量`() = runVmTest {
        val cloud = RecordingCloudRuntime()
        val (vm, _) = createDeclinedVm(cloud)
        vm.submit()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        // 完整字面量断言（非仅"含规则解析"字样）
        assertEquals(
            "本次结果：规则解析（AI 解析失败：还没有同意把数据发给云端 AI）",
            vm.state.value.parseSourceMessage,
        )
        assertEquals(0, cloud.calls) // 数据未离开设备：CLOUD runtime 零调用
    }

    @Test
    fun `T-L1-03b 未同意时confirmHealthAdvice失败且无替代内容`() = runVmTest {
        val cloud = RecordingCloudRuntime()
        val (vm, _) = createDeclinedVm(cloud)
        vm.submit()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        vm.requestHealthAdvice()
        vm.confirmHealthAdvice()
        vm.state.first { it.healthAdviceError != null || it.healthAdvice != null }
        // 该消费点无规则兜底：用户看到的是闸门本身的文案（healthAdviceError 展示区），不生成替代内容
        // 精确断言完整串：当前 message 14 汉字 < 生产 `confirmHealthAdvice()` 的 `.take(120)` 截断线，故不被截断；
        //   若未来 message 加长超 120，此处会如实失败提醒同步（Google 质量终审 🟡#2 以注释替代弱化断言）。
        assertEquals("还没有同意把数据发给云端 AI", vm.state.value.healthAdviceError)
        assertNull(vm.state.value.healthAdvice)
        assertEquals(0, cloud.calls)
    }
}
