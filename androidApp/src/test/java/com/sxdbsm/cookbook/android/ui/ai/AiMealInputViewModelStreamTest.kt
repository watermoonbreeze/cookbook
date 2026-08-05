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
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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

    private fun runVmTest(block: suspend () -> Unit) {
        // runBlocking 真实时间：buildHealthSafetyReport 的真实 DB IO 能完成，StateFlow.first 真实等待。
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            runBlocking { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    /** 会话端口 spy：记录调用次数与入参（R2-04 记录 commit 实例）。 */
    private open class SpySessionPort(private val hasExisting: Boolean = false) : AiMealSessionPort {
        var previewCount = 0
        var commitCount = 0
        var parseRuleCount = 0
        val previewDates = mutableListOf<LocalDate>()
        var lastCommittedPreview: AutoGenPreview? = null

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
            lastCommittedPreview = preview
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
    }

    /**
     * R3-03: 可配置 preview 挂起/抛异常的 port——单次计数，不调 super.preview。
     * gateOnPreview：第 N 次 preview 进入 gate（0=从不）；failOnPreview：第 N 次 preview 抛异常（0=不抛）。
     */
    private class GatedPreviewPort(
        private val existingFlag: Boolean = false,
        private val gateOnPreview: Int = 0,
        private val failOnPreview: Int = 0,
    ) : SpySessionPort() {
        val previewEntered = CompletableDeferred<Unit>()
        val previewRelease = CompletableDeferred<Unit>()

        override suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview {
            previewCount++
            if (previewCount == failOnPreview) {
                throw IllegalStateException("final preview failed")
            }
            if (previewCount == gateOnPreview) {
                previewEntered.complete(Unit)
                withContext(kotlinx.coroutines.NonCancellable) { previewRelease.await() }
            }
            return AutoGenPreview(
                days = days.map { day ->
                    DayPreview(
                        date = runCatching { LocalDate.parse(day.date!!) }.getOrElse { targetDate },
                        meals = emptyList(),
                        hasExisting = existingFlag,
                    )
                },
                warnings = emptyList(),
            )
        }
    }

    /** R2-04: 记录每个 LlmRequest 的 runtime。 */
    private class RecordingRuntime(
        private val streamFactory: () -> Flow<LlmStreamEvent>,
    ) : AiRuntime {
        val requests = mutableListOf<LlmRequest>()
        override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
        override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
            requests.add(request)
            return streamFactory()
        }
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

        // 再送 dish → PARTIAL_READY + preview 一次 + 无 commit（first 等 buildHealthSafetyReport 完成）
        channel.send(LlmStreamEvent.Delta("$dishJson\n"))
        vm.state.first { it.phase == AiMealPhase.PARTIAL_READY || it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
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
        assertEquals(0, spy.commitCount)
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
    fun `T-B3-01 request冻结字段 编辑与改日期均失效generation`() = runVmTest {
        val gate = CompletableDeferred<Unit>()
        val runtime = RecordingRuntime(
            streamFactory = { flow { gate.await(); emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n")); emit(LlmStreamEvent.Completed("stop", 0)) } },
        )
        val spy = SpySessionPort()
        val vm = createVm(runtime, text = "中午吃了番茄炒蛋")
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        // R2-04: 第一请求的 user 含原始 input 与冻结 targetDate
        val firstRequest = runtime.requests.first()
        assertTrue(firstRequest.user.contains("中午吃了番茄炒蛋"))
        assertTrue(firstRequest.user.contains("2026-08-05"))
        assertEquals("meal-1", vm.state.value.generationId)

        // 编辑 → INPUT + 取消 generation
        vm.setInputText("改成别的")
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        assertNull(vm.state.value.generationId)

        // 释放 A 事件 → job 已取消，不更新 state
        gate.complete(Unit)
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        assertNull(vm.state.value.autoGenPreview)
        assertEquals(0, spy.previewCount)

        // R2-04: 改日期同样失效
        vm.submit()
        vm.setTargetDate(LocalDate(2026, 8, 6))
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        assertNull(vm.state.value.generationId)
    }

    @Test
    fun `T-B3-06 A进preview gate时submit启动B 释放A后B不变无ERROR`() = runVmTest {
        val channelA = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val callCount = AtomicInteger(0)
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
                val idx = callCount.incrementAndGet()
                return if (idx == 1) {
                    channelA.receiveAsFlow() // gen A 由 channel 控制
                } else {
                    flow { emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n")); emit(LlmStreamEvent.Completed("stop", 0)) }
                }
            }
        }
        val port = GatedPreviewPort(gateOnPreview = 1)
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(port)
        vm.submit() // gen A

        // A 送合法 meal/dish → 进入 preview gate
        channelA.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        port.previewEntered.await()

        // A 挂起时启动 B → B 立即完成
        vm.submit()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals("meal-2", vm.state.value.generationId)
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)

        // 释放 A → A generation 失效，不污染 B
        port.previewRelease.complete(Unit)
        vm.state.first { it.generationId == "meal-2" && (it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR) }
        assertEquals("meal-2", vm.state.value.generationId)
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
    }

    @Test
    fun `T-B3-06a preview挂起时编辑 旧A不写新会话且无ERROR`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val port = GatedPreviewPort(gateOnPreview = 1)
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(port)
        vm.submit() // gen A 开始

        // 送 meal+dish → A 进入 preview gate
        channel.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        port.previewEntered.await()

        // 编辑 → 新会话（invalidate 取消 A job + INPUT）
        vm.setInputText("改成别的")
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        assertNull(vm.state.value.generationId)

        // 释放 A 的 preview → handleSessionSnapshot 检查 generation 失败 → 不写 A 状态/ERROR
        port.previewRelease.complete(Unit)
        vm.state.first { it.phase == AiMealPhase.INPUT }
        assertNull(vm.state.value.autoGenPreview)
    }

    @Test
    fun `T-B3-08 两轮preview 保存时提交第一轮P 释放final preview后仍DONE`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val port = GatedPreviewPort(existingFlag = true, gateOnPreview = 2)
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(port)
        vm.submit()

        // 第一轮 preview（不 gate）返回 P → PARTIAL_READY + hasExisting
        channel.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        vm.state.first { it.phase == AiMealPhase.PARTIAL_READY || it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PARTIAL_READY, vm.state.value.phase)
        assertTrue(vm.state.value.mergeConfirmationRequired)
        val firstPreviewP = vm.state.value.autoGenPreview

        // 第一次 confirmSave：仅确认 merge，不 commit
        vm.confirmSave()
        assertTrue(vm.state.value.mergeConfirmed)
        assertEquals(0, port.commitCount)

        // 发送 Completed → 流结束（close）→ 第二轮 preview（final）进入 gate 挂起
        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        port.previewEntered.await()
        // 第二轮 gate 挂起：phase 仍 PARTIAL_READY

        // 第二次 confirmSave：在 gate 挂起时提交第一轮 P → SAVING/DONE
        vm.confirmSave()
        vm.state.first { it.phase == AiMealPhase.DONE || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.DONE, vm.state.value.phase)
        assertEquals(1, port.commitCount)
        assertTrue("commit 必须收到第一轮 preview 同一实例", port.lastCommittedPreview === firstPreviewP)

        // 释放 final preview → A 已失效，不离开 DONE
        port.previewRelease.complete(Unit)
        vm.state.first { it.phase == AiMealPhase.DONE || it.phase == AiMealPhase.SAVING || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.DONE, vm.state.value.phase)
        assertEquals(1, port.commitCount)
    }

    @Test
    fun `T-B3-08b ERROR但保留preview时 useRuleFallback拒绝`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        // 第一轮 preview 成功，第二轮（final）抛异常 → ERROR 但保留第一轮 preview
        val port = GatedPreviewPort(existingFlag = false, failOnPreview = 2)
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(port)
        vm.submit()

        channel.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        vm.state.first { it.phase == AiMealPhase.PARTIAL_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PARTIAL_READY, vm.state.value.phase)

        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.ERROR, vm.state.value.phase)
        assertTrue("ERROR 仍保留 preview", vm.state.value.autoGenPreview != null)

        // 带 preview 的 ERROR：useRuleFallback 必须拒绝（0 次）
        vm.useRuleFallback()
        assertEquals(0, port.parseRuleCount)
        assertEquals(AiMealPhase.ERROR, vm.state.value.phase)
    }
}
