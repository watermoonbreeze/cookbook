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

    /**
     * 会话端口 spy：记录调用次数与入参（R2-04 记录 commit 实例）。
     * [AI新增] `ruleParseYields`：第 N 次 `parseRule` 调用是否产出合法餐食的序列（0-indexed）；
     * 超出序列长度的调用沿用最后一项，序列为空则恒成功（保持既有测试默认行为不变）。
     */
    private open class SpySessionPort(
        private val hasExisting: Boolean = false,
        private val ruleParseYields: List<Boolean> = emptyList(),
    ) : AiMealSessionPort {
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
            val succeed = if (ruleParseYields.isEmpty()) true else {
                ruleParseYields.getOrElse(parseRuleCount) { ruleParseYields.last() }
            }
            parseRuleCount++
            if (!succeed) return RuleFallbackResult(days = emptyList(), warning = null)
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
        // [AI修改] Google质量复核：parseWarnings 现在过 humanizeWarning，不再是原始 "STREAM_ENDED_WITHOUT_TERMINAL"
        // 代号，而是人读文案"AI 响应异常中断"——断言改用人读文案，原始代号不该出现在用户可见文本里。
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertTrue(
            "应有人读的 AI 响应异常中断诊断",
            vm.state.value.parseWarnings.any { it.contains("AI 响应异常中断") } || vm.state.value.diagnostic != null,
        )
        assertTrue(
            "不得把内部代号原样吐给用户",
            vm.state.value.parseWarnings.none { it.contains("STREAM_ENDED_WITHOUT_TERMINAL") },
        )
    }

    @Test
    fun `T-B3-05 全段Failed但规则解析成功 自动回退到PREVIEW_READY`() = runVmTest {
        // [AI修改] AI 未配置报错重设计：AI 该段失败后自动尝试规则解析兜底，成功则直接进入 PREVIEW_READY，
        // 不再进 ERROR（规则解析是兜底而非阻断态）；确认页 parseSourceMessage 须诚实说明"AI 失败→规则解析"。
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort() // 默认 ruleParseYields 空 = parseRule 恒成功
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertEquals(1, spy.parseRuleCount)
        // 该段进入 FAILED（terminal）触发一次中间 preview（PARTIAL_READY），最终 isFinal=true 再无条件重算一次
        // preview——这是既有既定行为（dedup 只对非 final 调用生效，见 handleSessionSnapshot 注释），非本次改动引入。
        assertEquals(2, spy.previewCount)
        assertEquals(0, spy.commitCount)
        assertTrue(
            "确认页须说明 AI 失败已回退规则",
            vm.state.value.parseSourceMessage.contains("规则解析") &&
                vm.state.value.parseSourceMessage.contains("HTTP 500 STREAM_HTTP_ERROR"),
        )
    }

    @Test
    fun `T-B3-05b 全段Failed且规则解析也失败 才真进ERROR`() = runVmTest {
        // 两个引擎都解析不出内容时，"没能识别出菜品"才是诚实文案；此时 useRuleFallback 已被自动尝试过一次
        // （parseRuleCount==1），仍应允许用户手动再试（T-B3-07 覆盖该场景）。
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort(ruleParseYields = listOf(false))
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.ERROR, vm.state.value.phase)
        assertEquals(0, spy.previewCount)
        assertEquals(1, spy.parseRuleCount)
        assertEquals(0, spy.commitCount)
        assertTrue(
            "两引擎皆失败时仍是诚实的没识别出菜品文案",
            vm.state.value.errorMessage?.contains("没能识别出菜品") == true,
        )
    }

    @Test
    fun `T-B3-07 自动兜底也失败后 useRuleFallback手动再试可成功`() = runVmTest {
        // 自动兜底（第 1 次 parseRule）失败→真 ERROR；用户手动 useRuleFallback（第 2 次 parseRule）成功。
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort(ruleParseYields = listOf(false, true))
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Failed("HTTP 500 STREAM_HTTP_ERROR", retryable = false))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.ERROR }
        assertEquals(1, spy.parseRuleCount)

        vm.useRuleFallback()
        vm.state.first { it.parseSourceMessage.contains("规则解析") || it.errorMessage?.contains("规则解析") == true }
        assertEquals(2, spy.parseRuleCount)
        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertTrue(vm.state.value.parseSourceMessage.contains("规则解析"))
        assertEquals(1, spy.previewCount)
    }

    @Test
    fun `T-CFG-01 AI未配置直接走规则解析 不发起任何AI请求`() = runVmTest {
        // configReady 默认 true（测试从不调用 refreshEngineStatus），本测试显式驱动到"未配置"分支需要
        // 走 refreshEngineStatus 的真实判据：AiRuntimeConfig 默认 CLOUD 且未填 Key，isNotBlank()==false。
        val runtime = object : AiRuntime {
            var streamCalls = 0
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
                streamCalls++
                return kotlinx.coroutines.flow.emptyFlow()
            }
        }
        val spy = SpySessionPort()
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)
        vm.refreshEngineStatus() // 未填 Key → configReady=false，engineLabel="规则解析"

        assertEquals("规则解析", vm.state.value.engineLabel)

        vm.submit()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertEquals(1, spy.parseRuleCount)
        assertEquals("本次结果：规则解析", vm.state.value.parseSourceMessage)
        assertEquals("未配置时不得发起任何 AI 网络请求", 0, runtime.streamCalls)
    }

    @Test
    fun `T-CFG-02 AI声明与targetDate不同的绝对日期 段结果不被按日期字符串误判丢弃`() = runVmTest {
        // Google质量复核阻断项1的回归锁定：AI 解析"昨天晚饭"这类输入会给出与 seg.targetDate 不同的绝对
        // 日期（此处 2026-08-04≠targetDate 2026-08-05），mergeDays 必须按 segmentId 归属找到该结果，
        // 不能按 `day.date == seg.targetDate.toString()` 字符串匹配（会把这天判定成"未解析"丢弃）。
        val yesterdayMeal = """{"type":"meal","segment_id":"$quickSegId","meal_id":"2026-08-04|dinner","date":"2026-08-04","slot":"dinner"}"""
        val yesterdayDish = """{"type":"dish","segment_id":"$quickSegId","meal_id":"2026-08-04|dinner","dish_id":"2026-08-04|dinner|d1","name":"清蒸鲈鱼"}"""
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val vm = createVm(channelRuntime(channel), text = "昨天晚饭吃了清蒸鲈鱼")
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Delta("$yesterdayMeal\n$yesterdayDish\n"))
        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertEquals("AI 结果不该被判定为解析失败", 0, spy.parseRuleCount)
        assertEquals(
            "昨天的绝对日期必须原样送入 preview，不能因日期不等于 targetDate 而丢弃",
            LocalDate(2026, 8, 4),
            vm.state.value.autoGenPreview?.days?.firstOrNull()?.date,
        )
    }

    @Test
    fun `T-CFG-03 段AI已COMPLETED成功后 不再误触发规则兜底`() = runVmTest {
        // Google质量复核阻断项2的回归锁定：currentSegmentId() 只看下标不看状态，段已 COMPLETED 时它仍等于
        // seg.segmentId；必须靠 session.isStreaming(...) 挡住，不能对已成功的段调用 parseRule。
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort()
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        channel.send(LlmStreamEvent.Delta("$mealJson\n$dishJson\n"))
        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertEquals("成功段不得触发规则兜底", 0, spy.parseRuleCount)
    }

    @Test
    fun `T-CFG-04 未配置AI时诊断信息不泄露内部哨兵代号`() = runVmTest {
        // Google质量复核阻断项3的回归锁定：ENGINE_NOT_CONFIGURED_REASON 是内部哨兵，规则解析也失败走到
        // ERROR 时，parseWarnings 里不能出现这个原始代号字符串。
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = kotlinx.coroutines.flow.emptyFlow()
        }
        val spy = SpySessionPort(ruleParseYields = listOf(false))
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(spy)
        vm.refreshEngineStatus()
        vm.submit()
        vm.state.first { it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.ERROR, vm.state.value.phase)
        assertTrue(
            "parseWarnings 不得包含内部哨兵代号",
            vm.state.value.parseWarnings.none { it.contains("AI_NOT_CONFIGURED") },
        )
    }

    @Test
    fun `T-CFG-05 周期记非首段规则兜底 相对日期按该段自身targetDate而非首段锚点解析`() = runVmTest {
        // Google质量复核阻断项5的回归锁定：MealDateAnchorPolicy 对"有星期无绝对日期"的输入产出
        // date=null + date_offset(相对传入的 targetDate 计算)。attemptRuleFallback 必须用该段自身的
        // seg.targetDate（周五 2026-08-07）解析绝对日期，不能等流到 handleSessionSnapshot 后被首段
        // （周一 2026-08-03）锚点的 frozenDate 重新解析——否则同样 offset 在错误锚点下会指向错误日期。
        val relativeDayPort = object : SpySessionPort() {
            override suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult {
                parseRuleCount++
                // date_offset=2：若按本段自身 targetDate(2026-08-07 周五) 解析 → 2026-08-09；
                // 若被错误套用首段锚点(2026-08-03 周一) → 会变成 2026-08-05，两者不同，测试才有辨别力。
                return RuleFallbackResult(
                    days = listOf(
                        DayMealJson(
                            date = null,
                            date_offset = 2,
                            meals = listOf(
                                MealJson(meal_type = "lunch", dishes = listOf(MealDishRefJson(name = "炒饭", dish = DishJson(name = "炒饭")))),
                            ),
                        ),
                    ),
                    warning = null,
                )
            }
        }
        val runtime = object : AiRuntime {
            override suspend fun complete(request: LlmRequest): Result<String> = Result.success("")
            override fun stream(request: LlmRequest): Flow<LlmStreamEvent> = kotlinx.coroutines.flow.emptyFlow()
        }
        val vm = createVm(runtime)
        vm.replaceSessionPortForTest(relativeDayPort)
        vm.refreshEngineStatus() // 未配置 Key → 全走规则，触发 attemptRuleFallback 的日期解析逻辑
        vm.setInputMode(InputMode.WEEK)
        vm.setPeriodInput(4, "周五吃了炒饭") // index 4 = 周五（首段是周一 2026-08-03），seg.targetDate=2026-08-07
        vm.submit()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        val day = vm.state.value.autoGenPreview?.days?.firstOrNull()
        assertEquals(
            "date_offset 必须按该段自身 targetDate(周五)解析，不能套用首段(周一)锚点",
            LocalDate(2026, 8, 9),
            day?.date,
        )
    }

    @Test
    fun `T-CFG-06 AI正常Completed但没解析出任何合法菜 仍自动兜底到规则`() = runVmTest {
        // Google质量二次复核发现的覆盖缺口回归锁定：段 Completed 正常终态但没有一行合法 NDJSON（模型只回了
        // 文字/道歉，没有任何 meal/dish 事件），不经过任何 onFailed 调用点——必须在段全部终态后的收尾阶段
        // 补触发规则兜底，不能让"AI 正常结束但啥也没解析出"这条路径永远拿不到自动兜底。
        val channel = Channel<LlmStreamEvent>(Channel.UNLIMITED)
        val spy = SpySessionPort() // 默认 ruleParseYields 空 = parseRule 恒成功
        val vm = createVm(channelRuntime(channel))
        vm.replaceSessionPortForTest(spy)
        vm.submit()

        // 模型只回一段无法解析出菜品的文字，然后正常 Completed（不是 Failed，不是异常，不是流意外结束）。
        channel.send(LlmStreamEvent.Delta("抱歉，我没有理解您的意思，能再描述具体一些吗？\n"))
        channel.send(LlmStreamEvent.Completed("stop", 0))
        channel.close()
        vm.state.first { it.phase == AiMealPhase.PREVIEW_READY || it.phase == AiMealPhase.ERROR }

        assertEquals(AiMealPhase.PREVIEW_READY, vm.state.value.phase)
        assertEquals("Completed但空解析必须仍触发一次规则兜底", 1, spy.parseRuleCount)
        assertTrue(vm.state.value.parseSourceMessage.contains("规则解析"))
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
