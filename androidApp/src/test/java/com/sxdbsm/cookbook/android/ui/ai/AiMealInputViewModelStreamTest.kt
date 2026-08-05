package com.sxdbsm.cookbook.android.ui.ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.LlmRequest
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.ai.meallog.InputSegment
import com.sxdbsm.cookbook.ai.meallog.MultiDayRecorder
import com.sxdbsm.cookbook.ai.meallog.StreamingMealRequest
import com.sxdbsm.cookbook.ai.meallog.StreamingMealSession
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

/**
 * B3: AiMealInputViewModel 会话链测试（蓝图 T-B3-01/03/04/05/06/07）。
 * T-B3-02 多段串行由 StreamingMealSessionTest 覆盖（B3 ViewModel 仅构造 quick 单段）。
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AiMealInputViewModelStreamTest {

    private val targetDate = LocalDate(2026, 8, 5)
    private val quickSegId = "quick-$targetDate"

    private val mealJson = """{"type":"meal","segment_id":"$quickSegId","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}"""
    private val dishJson = """{"type":"dish","segment_id":"$quickSegId","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋"}"""

    @Before fun setUp() { Dispatchers.setMain(kotlinx.coroutines.test.UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun inMemoryDb(): CookbookDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        return CookbookDatabase(driver)
    }

    private fun createVm(
        aiRuntime: AiRuntime,
        text: String = "中午吃了番茄炒蛋",
    ): AiMealInputViewModel {
        val db = inMemoryDb()
        val prefs = PreferenceRepository(db)
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val nutritionRepo = NutritionRepository(db)
        val healthRepo = HealthProfileRepository(db)
        val familyRepo = FamilyRepository(db, prefs)
        val recorder = MultiDayRecorder(
            ingredientRepo, dishRepo, mealRepo, nutritionRepo,
            IngredientAliasResolver(emptyMap()), db,
        )
        val config = AiRuntimeConfig(prefs)
        return AiMealInputViewModel(
            initialText = text, targetDate = targetDate,
            aiRuntime = aiRuntime, config = config, recorder = recorder,
            ingredientRepo = ingredientRepo, healthRepo = healthRepo, familyRepo = familyRepo,
        )
    }

    private suspend fun awaitState(vm: AiMealInputViewModel, pred: (AiMealInputUiState) -> Boolean) {
        withTimeout(5000) {
            while (!pred(vm.state.value)) delay(10)
        }
    }

    /** 成功流：发 meal+dish Delta 后 Completed。 */
    private fun successRuntime(): AiRuntime = object : AiRuntime {
        override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
        override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
            emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
            emit(LlmStreamEvent.Completed("stop", 0))
        }
    }

    @Test
    fun `T-B3-03 Delta到合法dish后最终PREVIEW_READY且preview非空`() = runBlocking {
        val vm = createVm(successRuntime())
        vm.submit()
        awaitState(vm) { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertNotNull(vm.state.value.autoGenPreview)
        assertEquals("meal-1", vm.state.value.generationId)
        assertTrue(vm.state.value.segmentStates.isNotEmpty())
    }

    @Test
    fun `T-B3-04 合法dish后Failed 保留preview且非ERROR`() = runBlocking {
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
                emit(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
            }
        }
        val vm = createVm(runtime)
        vm.submit()
        awaitState(vm) { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        // 合法前缀保留：不是 ERROR，有 preview
        assertTrue("应保留合法 preview", vm.state.value.phase != AiMealPhase.ERROR)
        assertNotNull(vm.state.value.autoGenPreview)
        assertTrue(vm.state.value.parseWarnings.isNotEmpty() || vm.state.value.diagnostic != null)
    }

    @Test
    fun `T-B3-05 全流Failed进入ERROR 不自动调用规则解析`() = runBlocking {
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                emit(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
            }
        }
        val vm = createVm(runtime)
        vm.submit()
        awaitState(vm) { it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.ERROR, vm.state.value.phase)
        assertNull(vm.state.value.autoGenPreview)
    }

    @Test
    fun `T-B3-07 ERROR后useRuleFallback显式触发规则解析标记来源`() = runBlocking {
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                emit(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
            }
        }
        val vm = createVm(runtime)
        vm.submit()
        awaitState(vm) { it.phase == AiMealPhase.ERROR }

        // 显式触发规则降级；等待 parseSourceMessage 变为规则解析（useRuleFallback 完成）
        vm.useRuleFallback()
        awaitState(vm) { it.parseSourceMessage.contains("规则解析") || it.errorMessage?.contains("规则解析") == true }
        val st = vm.state.value
        assertTrue("phase=${st.phase} msg=${st.errorMessage}", st.phase == AiMealPhase.PREVIEW_READY)
        assertTrue("source=${st.parseSourceMessage}", st.parseSourceMessage.contains("规则解析"))
        assertNotNull(st.autoGenPreview)
    }

    @Test
    fun `T-B3-01 submit后编辑文本 phase回INPUT且取消generation`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                gate.await() // 阻塞，模拟进行中 generation
                emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
                emit(LlmStreamEvent.Completed("stop", 0))
            }
        }
        val vm = createVm(runtime)
        vm.submit()
        // 生成中
        assertTrue(vm.state.value.isGenerating || vm.state.value.phase == AiMealPhase.GENERATING)

        // 编辑文本 → 新会话
        vm.setInputText("改成别的")
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        // 释放 gate：旧 generation 已取消，不应更新 state
        gate.complete(Unit)
        delay(50)
        assertEquals(AiMealPhase.INPUT, vm.state.value.phase)
        assertNull(vm.state.value.autoGenPreview)
    }

    @Test
    fun `T-B3-06 新submit取消旧generation 旧事件不污染`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val callCount = AtomicInteger(0)
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
                val idx = callCount.incrementAndGet()
                return if (idx == 1) {
                    // 第一 generation：阻塞
                    flow { gate.await(); emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n")); emit(LlmStreamEvent.Completed("stop", 0)) }
                } else {
                    // 第二 generation：立即成功
                    flow { emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n")); emit(LlmStreamEvent.Completed("stop", 0)) }
                }
            }
        }
        val vm = createVm(runtime)
        vm.submit() // gen A 阻塞
        vm.submit() // gen B 立即完成

        awaitState(vm) { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals("meal-2", vm.state.value.generationId)

        // 释放 A 的 gate → A 事件不应改变 B 的 state
        gate.complete(Unit)
        delay(50)
        assertEquals("meal-2", vm.state.value.generationId)
        assertNotNull(vm.state.value.autoGenPreview)
    }

    @Test
    fun `T-B3-02 单段流不结束不提前终态`() = runBlocking {
        val gate = CompletableDeferred<Unit>()
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
                emit(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
                gate.await() // 流未结束
                emit(LlmStreamEvent.Completed("stop", 0))
            }
        }
        val vm = createVm(runtime)
        vm.submit()
        delay(50)
        // 流未结束：不应 PREVIEW_READY（无 Completed）
        assertTrue(vm.state.value.phase == AiMealPhase.PARTIAL_READY || vm.state.value.phase == AiMealPhase.GENERATING)
        assertTrue(vm.state.value.isGenerating)

        gate.complete(Unit)
        awaitState(vm) { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
    }
}
