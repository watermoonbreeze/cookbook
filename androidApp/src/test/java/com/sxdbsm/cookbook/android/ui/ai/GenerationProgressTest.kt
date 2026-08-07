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
import com.sxdbsm.cookbook.ai.meallog.StreamSegmentState
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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * B6-fix2: GenerationProgress 段进度测试（AF-B456-05 第二轮 — GC-36·§3.5.1）。
 * 验证"未开始"段不被兜底为 STREAMING，null 正确表达 PENDING 语义。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class GenerationProgressTest {

    private val targetDate = LocalDate(2026, 8, 5)  // Wednesday; week monday = 2026-08-03

    private fun runVmTest(block: suspend () -> Unit) {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            runBlocking { block() }
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class SpySessionPort : AiMealSessionPort {
        var previewCount = 0
        val previewDates = mutableListOf<LocalDate>()

        override suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview {
            previewCount++
            previewDates.add(targetDate)
            return AutoGenPreview(
                days = days.map { day ->
                    DayPreview(
                        date = runCatching { LocalDate.parse(day.date!!) }.getOrElse { targetDate },
                        meals = emptyList(),
                        hasExisting = false,
                    )
                },
                warnings = emptyList(),
            )
        }

        override suspend fun commit(preview: AutoGenPreview) =
            AutoGenResult(1, 1, 0, 0, 0, 0, emptyList(), emptyList())

        override suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult =
            RuleFallbackResult(
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

    private fun inMemoryDb(): CookbookDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        return CookbookDatabase(driver)
    }

    private fun createVm(aiRuntime: AiRuntime): AiMealInputViewModel {
        val db = inMemoryDb()
        val prefs = PreferenceRepository(db)
        val ingredientRepo = IngredientRepository(db)
        val recorder = MultiDayRecorder(
            ingredientRepo, DishRepository(db), MealRecordRepository(db), NutritionRepository(db),
            IngredientAliasResolver(emptyMap()), db,
        )
        return AiMealInputViewModel(
            initialText = "", targetDate = targetDate,
            aiRuntime = aiRuntime, config = AiRuntimeConfig(prefs), recorder = recorder,
            ingredientRepo = ingredientRepo,
            healthRepo = HealthProfileRepository(db), familyRepo = FamilyRepository(db, prefs),
        )
    }

    // ═══════════════════════════════════════════════════════════
    // T-B5-01：{2:"周三",4:"周五"} → totalSegments==2，currentSegmentIndex in 0..1
    // ═══════════════════════════════════════════════════════════
    @Test
    fun `T-B5-01 two non-blank days totalSegments 2 currentSegmentIndex in range`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = channel.receiveAsFlow()
        }
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)

        vm.setInputMode(InputMode.WEEK)
        vm.setPeriodInput(2, "周三吃了面")  // Wednesday
        vm.setPeriodInput(4, "周五吃了饭")  // Friday
        vm.submit()

        val progress = vm.state.value.generationProgress!!
        assertEquals("totalSegments", 2, progress.totalSegments)
        assertTrue("currentSegmentIndex in 0..1", progress.currentSegmentIndex in 0..1)
        // 初始 submit 后，所有段尚未开始 → 全 null
        assertEquals(listOf<StreamSegmentState?>(null, null), progress.segmentStatuses)

        channel.close()
    }

    // ═══════════════════════════════════════════════════════════
    // T-B5-02：段1 FAILED、段2 COMPLETED（均已终态）
    //         → segmentStatuses == listOf(FAILED, COMPLETED)
    // ═══════════════════════════════════════════════════════════
    @Test
    fun `T-B5-02 first segment FAILED second COMPLETED segmentStatuses matches`() = runVmTest {
        val channelA = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val channelB = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val callCount = AtomicInteger(0)
        val spy = SpySessionPort()

        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
                val idx = callCount.incrementAndGet()
                return if (idx == 1) channelA.receiveAsFlow() else channelB.receiveAsFlow()
            }
        }
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)

        vm.setInputMode(InputMode.WEEK)
        vm.setPeriodInput(0, "周一餐食")  // Monday
        vm.setPeriodInput(1, "周二餐食")  // Tuesday
        vm.submit()

        // Seg 0: Fail it
        channelA.send(LlmStreamEvent.Failed("error", retryable = false))
        channelA.close()

        // Seg 1: Send valid meal+dish → Completed
        channelB.send(LlmStreamEvent.Completed("stop", 0))
        channelB.close()

        // Wait for terminal state
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR || !it.isGenerating }

        val finalProgress = vm.state.value.generationProgress
        assertNotNull("generationProgress should not be null", finalProgress)
        assertEquals(2, finalProgress!!.totalSegments)

        // §3.5.1 精确要求：seg 0 FAILED、seg 1 COMPLETED，逐位精确匹配（非"任一终态即可"，
        // 否则漏判"FAILED 信号被静默吞掉、两段都读成 COMPLETED"这类回归）。
        assertEquals(
            "seg 0 must be FAILED, seg 1 must be COMPLETED — exact positional match per §3.5.1",
            listOf(StreamSegmentState.FAILED, StreamSegmentState.COMPLETED),
            finalProgress.segmentStatuses,
        )
    }

    // ═══════════════════════════════════════════════════════════
    // T-B5-03：会话构造后未调用任何 nextSegment()
    //         → segmentStatuses == List(totalSegments){ null }，currentSegmentIndex==0
    // ═══════════════════════════════════════════════════════════
    @Test
    fun `T-B5-03 after submit all segmentStatuses null currentSegmentIndex 0`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = channel.receiveAsFlow()
        }
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)

        vm.setInputMode(InputMode.WEEK)
        vm.setPeriodInput(1, "周二")
        vm.setPeriodInput(3, "周四")
        vm.setPeriodInput(5, "周六")
        vm.submit()

        val progress = vm.state.value.generationProgress!!
        assertEquals(3, progress.totalSegments)
        assertEquals(0, progress.currentSegmentIndex)
        // submit() 初始构造即为全 null
        assertEquals(listOf<StreamSegmentState?>(null, null, null), progress.segmentStatuses)

        channel.close()
    }

    // ═══════════════════════════════════════════════════════════
    // T-B5-04（锁定 AF-B456-05 回归）：
    // 3 段中仅第 1 段已 nextSegment()（STREAMING），第 2/3 段未触达
    // → segmentStatuses == listOf(StreamSegmentState.STREAMING, null, null)
    // ═══════════════════════════════════════════════════════════
    @Test
    fun `T-B5-04 only first segment streaming others remain null`() = runVmTest {
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = channel.receiveAsFlow()
        }
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)

        vm.setInputMode(InputMode.WEEK)
        vm.setPeriodInput(0, "周一餐食")  // Monday
        vm.setPeriodInput(2, "周三餐食")  // Wednesday
        vm.setPeriodInput(4, "周五餐食")  // Friday
        vm.submit()

        // 发送一段 Delta 触发 handleSessionSnapshot，computeProgress 将读 session segmentStates
        // seg 0 已被 nextSegment() 标记为 STREAMING，seg 1/2 尚未触达 → null
        channel.send(LlmStreamEvent.Delta("""{"type":"meal","segment_id":"week-2026-08-03-day1","meal_id":"lunch1","date":"2026-08-03","slot":"lunch"}"""))
        // 等待 snapshot 落状态（fast-path: hasValidMeals=false 但 generationProgress 仍更新）
        vm.state.first { (it.generationProgress?.segmentStatuses?.getOrNull(0)) != null }

        val progress = vm.state.value.generationProgress!!
        assertEquals(3, progress.totalSegments)

        // 关键断言（AF-B456-05 回归）：
        // 只有 seg 0（已 nextSegment）是 STREAMING，seg 1/2 是 null (PENDING)
        // 旧 bug 会把 seg 1/2 也兜底成 STREAMING
        assertEquals(
            "Only the first segment should be STREAMING; unstarted segments must be null, not STREAMING",
            listOf<StreamSegmentState?>(StreamSegmentState.STREAMING, null, null),
            progress.segmentStatuses,
        )

        channel.close()
    }
}
