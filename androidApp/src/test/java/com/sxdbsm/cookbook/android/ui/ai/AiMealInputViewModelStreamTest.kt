package com.sxdbsm.cookbook.android.ui.ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * B3.1: AiMealInputViewModel 会话链测试（AF-B3-01~07 修复版）。
 * - 用 AiMealSessionPort spy 精确记录 preview/commit/parseRule 调用。
 * - 用 Channel/CompletableDeferred 受控驱动，无 delay/轮询/sleep。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiMealInputViewModelStreamTest {

    private val targetDate = LocalDate(2026, 8, 5)
    private val quickSegId = "quick-$targetDate"

    private val mealJson = """{"type":"meal","segment_id":"$quickSegId","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}"""
    private val dishJson = """{"type":"dish","segment_id":"$quickSegId","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋"}"""

    private fun runVmTest(block: suspend TestScope.() -> Unit) = runTest {
        // Unconfined：send 后 receive 同步处理，避免 Standard 队列推进时序。
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            block()
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** 会话端口 spy：记录调用次数与入参。 */
    private class SpySessionPort(private val hasExisting: Boolean = false) : AiMealSessionPort {
        var previewCount = 0
        var commitCount = 0
        var parseRuleCount = 0
        val previewDates = mutableListOf<LocalDate>()

        override suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview {
            previewCount++
            previewDates.add(targetDate)
            return AutoGenPreview(
                days = days.map { day ->
                    DayPreview(
                        date = runCatching { LocalDate.parse(day.date!!) }.getOrElse { targetDate },
                        meals = emptyList(),
                        hasExisting = hasExisting,
                    )
                },
                warnings = emptyList(),
            )
        }

        override suspend fun commit(preview: AutoGenPreview): AutoGenResult {
            commitCount++
            return AutoGenResult(1, 1, 0, 0, 0, 0, emptyList(), emptyList())
        }

        override suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult {
            parseRuleCount++
            return RuleFallbackResult(
                days = listOf(
                    DayMealJson(
                        date = "2026-08-05",
                        meals = listOf(
                            MealJson(
                                meal_type = "lunch",
                                dishes = listOf(MealDishRefJson(name = "米饭", dish = DishJson(name = "米饭"))),
                            ),
                        ),
                    ),
                ),
                warning = null,
            )
        }

        override suspend fun healthReport(preview: AutoGenPreview): HealthSafetyReport =
            HealthSafetyReport()
    }

    private fun channelRuntime(channel: Channel<LlmStreamEvent>): AiRuntime = object : AiRuntime {
        override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
        override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = channel.receiveAsFlow()
    }

    private fun inMemoryDb(): CookbookDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        return CookbookDatabase(driver)
    }

    private fun createVm(aiRuntime: AiRuntime, text: String = "中午吃了番茄炒蛋"): AiMealInputViewModel {
        val db = inMemoryDb()
        val prefs = PreferenceRepository(db)
        val ingredientRepo = IngredientRepository(db)
        val recorder = MultiDayRecorder(
            ingredientRepo, DishRepository(db), MealRecordRepository(db), NutritionRepository(db),
            IngredientAliasResolver(emptyMap()), db,
        )
        return AiMealInputViewModel(
            initialText = text, targetDate = targetDate,
            aiRuntime = aiRuntime, config = AiRuntimeConfig(prefs), recorder = recorder,
            ingredientRepo = ingredientRepo,
            healthRepo = HealthProfileRepository(db), familyRepo = FamilyRepository(db, prefs),
        )
    }

    @Test
    fun `T-B3-03 meal后不preview dish后PARTIAL_READY preview一次`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        // 仅 meal（无合法 dish）→ 不 preview
        channel.send(LlmStreamEvent.Delta("$mealJson\n"))
        assertEquals(0, spy.previewCount)
        assertTrue(vm.state.value.isGenerating)

        // 再送 dish → PARTIAL_READY + preview 一次 + 无 commit
        channel.send(LlmStreamEvent.Delta("$dishJson\n"))
        assertEquals(1, spy.previewCount)
        assertEquals(AiMealPhase.PARTIAL_READY, vm.state.value.phase)
        assertTrue("未 Completed 不得 PREVIEW_READY", vm.state.value.phase != AiMealPhase.PREVIEW_READY)
        assertEquals(0, spy.commitCount)

        // Completed → 流结束（close）→ 最终 PREVIEW_READY
        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
    }

    @Test
    fun `T-B3-02 无terminal的flow记录STREAM_ENDED诊断且不提前终态`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        // 有合法 dish 但 flow 直接结束（无 Completed/Failed）
        channel.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        // 单段无下一段 → 最终 isFinal → 有合法餐食 → PREVIEW_READY + STREAM_ENDED 诊断
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertTrue(
            "应有 STREAM_ENDED 诊断",
            vm.state.value.parseWarnings.any { it.contains("STREAM_ENDED") } || vm.state.value.diagnostic != null,
        )
    }

    @Test
    fun `T-B3-05 全流Failed进入ERROR 不自动规则解析`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.ERROR, vm.state.value.phase)
        assertEquals(0, spy.previewCount)
        assertEquals("不自动调用规则解析", 0, spy.parseRuleCount)
    }

    @Test
    fun `T-B3-07 ERROR后useRuleFallback显式触发 标记规则来源`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.ERROR }

        vm.useRuleFallback()
        vm.state.first { it.parseSourceMessage.contains("规则解析") || it.errorMessage?.contains("规则解析") == true }
        assertEquals(1, spy.parseRuleCount)
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertTrue(vm.state.value.parseSourceMessage.contains("规则解析"))
        assertEquals(1, spy.previewCount)
    }

    @Test
    fun `T-B3-01 编辑文本取消generation 后到事件不污染`() = runVmTest {
        val gate = CompletableDeferred<Unit>()
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                gate.await()
                emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
                emit(LlmStreamEvent.Completed("stop", 0))
            }
        }
        val spy = SpySessionPort()
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        // 编辑 → INPUT + 取消 generation
        vm.setInputText("改成别的")
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)

        // 释放 A 事件 → job 已取消，不更新 state
        gate.complete(Unit)
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        assertNull(vm.state.value.autoGenPreview)
        assertEquals(0, spy.previewCount)
    }

    @Test
    fun `T-B3-06 新submit取消旧generation 旧事件不污染新会话`() = runVmTest {
        val gateA = CompletableDeferred<Unit>()
        val callCount = AtomicInteger(0)
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
                val idx = callCount.incrementAndGet()
                return if (idx == 1) {
                    flow { gateA.await(); emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n")); emit(LlmStreamEvent.Completed("stop", 0)) }
                } else {
                    flow { emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n")); emit(LlmStreamEvent.Completed("stop", 0)) }
                }
            }
        }
        val spy = SpySessionPort()
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)
        vm.submit() // gen A 阻塞
        vm.submit() // gen B 立即完成
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals("meal-2", vm.state.value.generationId)
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)

        // 释放 A → 不污染 B
        gateA.complete(Unit)
        vm.state.first { it.generationId == "meal-2" && (it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR) }
        assertEquals("meal-2", vm.state.value.generationId)
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
    }

    @Test
    fun `T-B3-08 两次MERGE确认 commit恰一次 保存后旧事件不干扰`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort(hasExisting = true)
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()
        channel.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertTrue(vm.state.value.mergeConfirmationRequired)

        // 第一次：仅置 mergeConfirmed
        vm.confirmSave()
        assertTrue(vm.state.value.mergeConfirmed)
        assertEquals(0, spy.commitCount)

        // 第二次：SAVING + commit 一次
        vm.confirmSave()
        assertEquals(1, spy.commitCount)
        assertEquals(AiMealPhase.DONE, vm.state.value.phase)

        // 第三次：重复调用 0 次（DONE 直接 return）
        vm.confirmSave()
        assertEquals(1, spy.commitCount)
    }
}
