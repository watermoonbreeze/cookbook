package com.sxdbsm.cookbook.android.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.ai.meallog.AiMealHealthAdvice
import com.sxdbsm.cookbook.ai.meallog.DayMealJson
import com.sxdbsm.cookbook.ai.meallog.DiagnosticCode
import com.sxdbsm.cookbook.ai.meallog.DiagnosticLevel
import com.sxdbsm.cookbook.ai.meallog.StreamDiagnostic
import com.sxdbsm.cookbook.ai.meallog.InputSegment
import com.sxdbsm.cookbook.ai.meallog.InputSegmentFactory
import com.sxdbsm.cookbook.ai.meallog.MultiDayRecorder
import com.sxdbsm.cookbook.ai.meallog.MealDateAnchorPolicy
import com.sxdbsm.cookbook.ai.meallog.RuleMealParser
import com.sxdbsm.cookbook.ai.meallog.StreamSegmentState
import com.sxdbsm.cookbook.ai.meallog.StreamingMealRequest
import com.sxdbsm.cookbook.ai.meallog.StreamingMealSession
import com.sxdbsm.cookbook.ai.meallog.StreamingSessionSnapshot
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.domain.autogen.AutoGenPreview
import com.sxdbsm.cookbook.domain.autogen.AutoGenResult
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * @File : AiMealInputViewModel
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐 ViewModel
 * <p>
 * 状态机：输入 → 解析中 → 预览(含营养估算) → 保存中 → 完成/错误。
 * P2-1 K1a：接入 MultiDayRecorder.previewAll/commitPreview 两阶段，预览阶段可展示热量。
 * <p>
 * [AI修改] P2-1 K1a AI快捷输入记餐：两阶段 preview/commit 接入。
 **/

/** [AI修改] B4: 输入模式——快速记(单段) vs 周期记(周多段)。原 TEXT/VOICE 由 voiceState 独立表达。 */
enum class InputMode { QUICK, WEEK }

/** 处理阶段。[AI修改] B3: 唯一状态机 INPUT→GENERATING→PARTIAL_READY→PREVIEW_READY→SAVING→DONE；删除 PARSING/PREVIEW。 */
enum class AiMealPhase { INPUT, GENERATING, PARTIAL_READY, PREVIEW_READY, SAVING, DONE, ERROR }

/** [AI新增] 段级规则兜底原因哨兵值：AI 未配置（区别于"AI 配置了但该段解析失败"，两者文案框架不同）。 */
private const val ENGINE_NOT_CONFIGURED_REASON = "AI_NOT_CONFIGURED"

/** 语音识别状态。[AI生成] */
enum class VoiceState { IDLE, LISTENING, PROCESSING, ERROR }

/** [AI生成] B5: ViewModel → UI Snackbar 事件（含撤销回调）。 */
data class SnackbarAction(
    val message: String,
    val actionLabel: String,
    val onAction: () -> Unit,
)

/** AI 本次尝试的仅会话诊断；原始响应不写日志、不入库，重输/退出即清除。 [AI修改] */
data class AiMealAttemptDiagnostic(
    val stage: String,
    val summary: String,
    val responseLength: Int? = null,
    val rawResponse: String? = null,
)

/** 本机事实与 AI 建议均仅随当前确认会话存在。 [AI生成] */
data class HealthSafetyReport(val facts: List<String> = emptyList())

/** B3.1-PORT-01: 规则降级结果。[AI修改] */
internal data class RuleFallbackResult(
    val days: List<DayMealJson>,
    val warning: String?,
)

/**
 * B3.1-PORT-01: 会话对外端口——preview/commit/规则解析。
 *
 * 默认实现逐字复用现有 previewAll/commitPreview/RuleMealParser 链路；
 * 测试通过 [AiMealInputViewModel.replaceSessionPortForTest] 注入 spy。
 */
internal interface AiMealSessionPort {
    suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview
    suspend fun commit(preview: AutoGenPreview): AutoGenResult
    suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult
}

/** UI 状态。[AI修改] P2-1 K1a：autoGenPreview 作两阶段主键，PreviewPhase 直接渲染能力层产出。 */
data class AiMealInputUiState(
    val inputMode: InputMode = InputMode.QUICK,
    val phase: AiMealPhase = AiMealPhase.INPUT,
    /** [AI修改] B3: 当前 generation 标识；会话只读。 */
    val generationId: String? = null,
    /** [AI修改] B3: 各分段状态；会话只读。 */
    val segmentStates: Map<String, StreamSegmentState> = emptyMap(),
    /** [AI生成] B5: 生成进度（UI 友好的段进度封装）。GENERATING/PARTIAL_READY 时非 null。 */
    val generationProgress: GenerationProgress? = null,
    /** [AI生成] B3: 生成中标记（GENERATING/PARTIAL_READY 为 true）。 */
    val isGenerating: Boolean = false,
    /** [AI修改] P2-1 K1a：preview 阶段存能力层产出（含营养估算）。 */
    val autoGenPreview: AutoGenPreview? = null,
    /** [AI修改] P2-1 K1a：commit 结果（替代旧 AiMealRecorder.RecordResult）。 */
    val autoGenResult: AutoGenResult? = null,
    val errorMessage: String? = null,
    val voiceActive: Boolean = false,
    /** [AI修改] K2 语音识别精确状态：IDLE/录音中/处理中/出错 */
    val voiceState: VoiceState = VoiceState.IDLE,
    /** [AI修改] K2 语音识别错误消息（仅 ERROR 态非空） */
    val voiceError: String? = null,
    /** [AI修改] K2 语音音量 dB 值（-10~10+，用于波形动画） */
    val voiceRmsdB: Float = 0f,
    // ── B4: 周期记字段 ──
    /** [AI生成] B4: 快速记草稿（切换模式保留）。 */
    val quickDraftText: String = "",
    /** [AI修改] B6-fix2: 最近一次写入 `quickDraftText` 是否发生了 200 字截断，驱动 UI 内联提示（google_quality_engineer 复审：Sheet 内 Snackbar 大概率被 ModalBottomSheet 浮层窗口遮挡，改用内联反馈+此状态单一真相源，避免 UI 侧重算 appendText 的拼接逻辑）。 */
    val quickInputTruncated: Boolean = false,
    /** [AI生成] B4: 周期记周锚点（所选周周一）。 */
    val periodWeekMonday: LocalDate? = null,
    /** [AI生成] B4: 周期记选中日期范围（0..6 表示周一至周日），默认全选。[AI修改] B6-fix: 本范围仅控制输入区可见性；submit() 提交的是 periodInputs 中全部非空白天（不受本范围限制），收窄范围不会撤回已输入内容（AF-B456-07·Google质量 B4-S1）。 */
    val periodSelectedRange: IntRange = 0..6,
    /** [AI生成] B4: 周期记各天草稿（key=0..6）。 */
    val periodInputs: Map<Int, String> = emptyMap(),
    /** [AI生成] B6: 本周已有餐食的日期集合，周期记 WeekStrip 据此灰显 + "已有餐食"标记。 */
    val existingMealDates: Set<LocalDate> = emptySet(),
    /** 新建的菜品名列表（供预览提示"已自动创建"）。[AI生成] */
    val newDishNames: List<String> = emptyList(),
    /** 目标日期（预览可调）。[AI生成] */
    val targetDate: LocalDate = DateTime.today(),
    val parseSourceMessage: String = "",
    /** AI 结构化校验及预览产生的可见提示。 [AI修改] */
    val parseWarnings: List<String> = emptyList(),
    /** 有历史餐食时，必须先由用户显式确认才允许 MERGE 写入。 [AI修改] */
    val mergeConfirmationRequired: Boolean = false,
    val mergeConfirmed: Boolean = false,
    val diagnostic: AiMealAttemptDiagnostic? = null,
    val healthSafetyReport: HealthSafetyReport? = null,
    val healthAdviceConsentPending: Boolean = false,
    val healthAdviceLoading: Boolean = false,
    val healthAdvice: String? = null,
    val healthAdviceError: String? = null,
    /** [AI新增] 当前引擎标签，标题旁始终可见："AI · {模型名}" 或 "规则解析"；未刷新前为空（UI 隐藏）。 */
    val engineLabel: String = "",
) {
    /** [AI修改] B4→B6-fix: inputText 改为计算属性，统一真相源为 quickDraftText（AF-B456-01 修复·单一真相源准则A）。 */
    val inputText: String get() = quickDraftText
}

class AiMealInputViewModel(
    private val initialText: String,
    targetDate: LocalDate,  // [AI修改] 从食历当前日期传入，替代硬编码 _state.value.targetDate
    private val aiRuntime: AiRuntime,
    private val config: AiRuntimeConfig,
    private val recorder: MultiDayRecorder,
    private val ingredientRepo: IngredientRepository,
    private val healthRepo: HealthProfileRepository,
    private val familyRepo: FamilyRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiMealInputUiState(quickDraftText = initialText, targetDate = targetDate)
    )
    val state: StateFlow<AiMealInputUiState> = _state.asStateFlow()

    // [AI修改] B3: 当前 generation 的流收集 Job；新 generation 前 cancel 旧 Job。
    private var generationJob: Job? = null
    // 健康建议属于确认页的独立异步工作；复用 generation 身份，但绝不能占用主生成 Job。
    private var healthAdviceJob: Job? = null
    // [AI修改] B3: generation 单调递增序号。
    private var generationCounter = 0
    // [AI修改] B3: 最近一次 preview 的 days，避免相同内容重复调用 previewAll。
    private var lastPreviewDays: List<DayMealJson>? = null
    // [AI生成] B5: 最近一次 preview 时的终态段数，用于判定段边界。
    private var lastPreviewTerminalCount: Int = -1
    // [AI生成] B5: Snackbar 事件通道（ViewModel → UI）。
    private val _snackbarEvent = MutableSharedFlow<SnackbarAction>()
    val snackbarEvent: SharedFlow<SnackbarAction> = _snackbarEvent

    // [AI新增] AI→规则自动兜底：是否有真实可用的云端 AI（乐观默认 true，避免影响未调用 refreshEngineStatus() 的既有测试）。
    private var configReady: Boolean = true
    // [AI新增] segmentId -> 该段规则解析兜底产出的 DayMealJson（AI 未配置或 AI 该段失败时填充）。
    private val ruleFallbackDays = mutableMapOf<String, DayMealJson>()
    // [AI新增] 已尝试过规则兜底的 segmentId 集合，防止同一段被重复触发（session 内多个 onFailed 调用点可能对同一段各触发一次）。
    private val fallbackAttempted = mutableSetOf<String>()
    // [AI新增] segmentId -> 触发规则兜底的原因（AI 失败消息 或 ENGINE_NOT_CONFIGURED_REASON），用于确认页说明文案。
    private val segmentFallbackReasons = mutableMapOf<String, String>()
    // [AI新增] segmentId -> 规则解析自身的告知性 warning（如"当前餐食以选择的餐食日期为参照"），并入 parseWarnings。
    private val ruleWarnings = mutableMapOf<String, String>()

    // [AI修改] B3.1-PORT-01: 会话端口（测试可替换）；B3.2 R2-02 回退为严格三方法。
    private var sessionPort: AiMealSessionPort = DefaultSessionPort(recorder, ingredientRepo)

    /** 仅测试使用：替换会话端口为 spy。[AI修改] */
    internal fun replaceSessionPortForTest(port: AiMealSessionPort) {
        sessionPort = port
    }

    private class DefaultSessionPort(
        private val recorder: MultiDayRecorder,
        private val ingredientRepo: IngredientRepository,
    ) : AiMealSessionPort {
        override suspend fun preview(days: List<DayMealJson>, targetDate: LocalDate): AutoGenPreview =
            recorder.previewAll(days, targetDate)

        override suspend fun commit(preview: AutoGenPreview): AutoGenResult =
            recorder.commitPreview(preview)

        override suspend fun parseRule(input: String, targetDate: LocalDate): RuleFallbackResult {
            val names = ingredientRepo.allActiveNames()
            val ruleDays = RuleMealParser.parse(input, names, today = targetDate)
            val anchorResult = MealDateAnchorPolicy.apply(input, targetDate, ruleDays)
            return RuleFallbackResult(anchorResult.days, anchorResult.warning)
        }
    }

    /**
     * [AI新增] 刷新引擎状态：是否有真实可用云端 AI + 标题旁引擎标签。
     *
     * 由 UI 侧在 Sheet 每次进入可交互态时调用（`AiMealInputSheet` 的 `LaunchedEffect(Unit)`），覆盖两个场景：
     * ①Sheet 首次打开前完成一次判定；②用户从 AI 设置页配置 Key 后返回本 Sheet（Compose Navigation 标准行为下，
     * 宿主 composable 会随导航离开/返回而整体离开/重新进入组合，`LaunchedEffect(Unit)` 因此重新触发）。
     *
     * 判据：仅当"选中云端类型且已填 Key"才算真实可用；MOCK/ON_DEVICE 与"CLOUD 但未填 Key"统一视为"用规则"，
     * 不额外调用 `AiRuntimeConfig.isModelReady()`（其值域覆盖 MOCK/ON_DEVICE，语义是"能否发起任意调用"而非
     * "本次记一餐要不要走 AI"，两者不等价）。
     */
    suspend fun refreshEngineStatus() {
        // [AI修改] Google质量复核：prefs 读取异常（如库损坏）不应从 Compose LaunchedEffect 逃逸崩溃组合树，
        // 失败时保守回退到"规则解析"标签（configReady 也回退 false——宁可少用一次 AI，不让本次刷新直接崩溃）。
        runCatching {
            val cloudReady = config.activeType() == AiRuntimeType.CLOUD && config.currentCloudApiKey().isNotBlank()
            configReady = cloudReady
            val label = if (cloudReady) {
                // CloudModel.displayName 里的说明用全角或半角括号都有（如"（免费·推荐）" vs "(moonshot-v1-8k)"），
                // 标题旁小标签只取模型标识本体，两种括号都要截。
                val model = config.selectedModel()
                "AI · ${model.displayName.substringBefore("（").substringBefore(" (")}"
            } else {
                "规则解析"
            }
            _state.update { it.copy(engineLabel = label) }
        }.onFailure {
            configReady = false
            _state.update { it.copy(engineLabel = "规则解析") }
        }
    }

    /** [AI修改] B4: 快速记模式下设置草稿；周期记模式下无操作（周期记走 setPeriodInput）。 */
    fun setInputText(text: String) {
        if (_state.value.inputMode == InputMode.QUICK) {
            setQuickDraft(text)
        }
        // WEEK 模式：setInputText 是 no-op，文本由 setPeriodInput(dayIndex, text) 管理
    }

    /** [AI生成] B4: 设置快速记草稿（含 200 字截断，收口进 invalidateGenerationToInput）。[AI修改] B6-fix: 确保"编辑即新会话"语义（AF-B456-01 W5·GC-27）。 */
    fun setQuickDraft(text: String) {
        invalidateGenerationToInput(text, _state.value.targetDate)
    }

    /** [AI生成] B4: 设置周期记某天草稿（含 200 字截断）。 */
    fun setPeriodInput(dayIndex: Int, text: String) {
        val trimmed = text.take(AiMealPrompt.MAX_INPUT_CHARS)
        _state.update {
            it.copy(periodInputs = it.periodInputs.toMutableMap().apply { put(dayIndex, trimmed) })
        }
    }

    /** [AI生成] B4: 调整周期记选中日期范围。倒置区间静默拒绝（start > end 无操作）。 */
    fun setWeekRange(start: Int, end: Int) {
        if (start > end) return
        _state.update { it.copy(periodSelectedRange = start..end) }
    }

    /** [AI生成] B4: 切换到下一周（清空草稿，重置范围）。 */
    /** [AI修改] B5: 切换到下一周。保存当前草稿以支持撤销。 */

    /** [AI生成] B6: 设置本周已有餐食日期（由 AddDayFoodScreen 在打开 Sheet 时注入）。 */
    fun setExistingMealDates(dates: Set<LocalDate>) {
        _state.update { it.copy(existingMealDates = dates) }
    }

    fun advanceWeek() {
        val current = _state.value.periodWeekMonday ?: return
        shiftWeek(DateTime.plusDays(current, 7), "已切换到下一周")
    }

    /** [AI修改] B5: 切换到上一周。保存当前草稿以支持撤销。 */
    fun retreatWeek() {
        val current = _state.value.periodWeekMonday ?: return
        shiftWeek(DateTime.plusDays(current, -7), "已切换到上一周")
    }

    /** [AI生成] B5: 切周实现，保存旧状态供撤销。 */
    private fun shiftWeek(newMonday: LocalDate, message: String) {
        val cur = _state.value
        // [AI修改] B5-review: 闭包捕获快照值而非可变字段引用，防连续切周时撤销状态被覆盖。
        val savedMonday = cur.periodWeekMonday
        val savedInputs = cur.periodInputs
        _state.update { it.copy(periodWeekMonday = newMonday, periodInputs = emptyMap(), periodSelectedRange = 0..6) }
        viewModelScope.launch {
            _snackbarEvent.emit(SnackbarAction(message, "撤销") {
                if (savedMonday != null) {
                    _state.update { it.copy(periodWeekMonday = savedMonday, periodInputs = savedInputs, periodSelectedRange = 0..6) }
                }
            })
        }
    }

    /** 原子失效进行中 generation 并回到 INPUT 态。[AI修改] B3.4-R4-06: 改用 .copy() 保留粘性字段。[AI修改] B6-fix2: 统一在此收口截断（AF-B456-04 复检·appendText/onVoiceResult 此前绕过了 setQuickDraft 的截断，粘贴按钮追加超长剪贴板内容后一直显示未截断，直到下次 onValueChange 才补截断）。 */
    private fun invalidateGenerationToInput(nextInput: String, nextDate: LocalDate) {
        val truncated = nextInput.take(AiMealPrompt.MAX_INPUT_CHARS)
        val wasTruncated = nextInput.length > AiMealPrompt.MAX_INPUT_CHARS
        generationJob?.cancel()
        generationJob = null
        clearHealthAdviceForInvalidatedSession()
        lastPreviewDays = null
        lastPreviewTerminalCount = -1
        ruleFallbackDays.clear()
        fallbackAttempted.clear()
        segmentFallbackReasons.clear()
        ruleWarnings.clear()
        _state.update { prev ->
            prev.copy(
                quickDraftText = truncated,
                quickInputTruncated = wasTruncated,
                targetDate = nextDate,
                phase = AiMealPhase.INPUT,
                generationId = null,
                isGenerating = false,
                autoGenPreview = null,
                autoGenResult = null,
                errorMessage = null,
                segmentStates = emptyMap(),
                parseSourceMessage = "",
                parseWarnings = emptyList(),
                mergeConfirmationRequired = false,
                mergeConfirmed = false,
                diagnostic = null,
                healthSafetyReport = null,
                // inputMode / voiceState / voiceActive / voiceError / voiceRmsdB / newDishNames —
                // B4: quickDraftText / periodWeekMonday / periodSelectedRange / periodInputs —
                // 粘性字段从 prev 自动继承，无需显式声明。
            )
        }
    }

    /** [AI修改] B4: 切换输入模式——保留对方草稿。周期记首次进入时初始化 weekAnchor。[AI修改] B6-fix: inputText 已改为计算属性，copy 不再写它（AF-B456-01 W6）。 */
    fun setInputMode(mode: InputMode) {
        _state.update {
            val newWeekMonday = when {
                mode == InputMode.WEEK && it.periodWeekMonday == null ->
                    InputSegmentFactory.mondayOfWeek(it.targetDate)
                else -> it.periodWeekMonday
            }
            it.copy(inputMode = mode, periodWeekMonday = newWeekMonday)
        }
    }

    /** 设置语音激活态（录音中/停止）。[AI生成] */
    fun setVoiceActive(active: Boolean) {
        _state.update { it.copy(voiceActive = active) }
    }

    /** [AI修改] K2 语音识别开始：长按按下时调用 */
    fun onVoiceStart() {
        _state.update { it.copy(voiceState = VoiceState.LISTENING, voiceActive = true, voiceError = null, voiceRmsdB = 0f) }
    }

    /** [AI修改] K2 语音音量变化：SpeechRecognizer onRmsChanged 回调 */
    fun onVoiceRmsChanged(rmsdB: Float) {
        _state.update { it.copy(voiceRmsdB = rmsdB) }
    }

    /** [AI修改] K2 语音识别处理中：松手后等待结果时调用 */
    fun onVoiceProcessing() {
        _state.update { it.copy(voiceState = VoiceState.PROCESSING, voiceActive = false) }
    }

    /** [AI修改] K2 语音识别完成：将识别结果追加到输入框。B3.1: 结果即新会话。 */
    fun onVoiceResult(text: String) {
        val current = _state.value.quickDraftText.trimEnd()
        val appended = if (current.isBlank()) text else "$current $text"
        invalidateGenerationToInput(appended, _state.value.targetDate)
    }

    /** [AI修改] K2 语音识别出错 */
    fun onVoiceError(error: String) {
        _state.update {
            it.copy(
                voiceState = VoiceState.ERROR,
                voiceActive = false,
                voiceError = error,
            )
        }
    }

    /** [AI修改] K2 清除语音错误状态（用户点 Snackbar 后或再次长按时） */
    fun clearVoiceError() {
        _state.update { it.copy(voiceState = VoiceState.IDLE, voiceError = null) }
    }

    /** [AI修改] K2 追加文本到输入框（粘贴用）。B3.1: 追加即新会话。 */
    fun appendText(text: String) {
        val current = _state.value.quickDraftText.trimEnd()
        val appended = if (current.isBlank()) text else "$current\n$text"
        invalidateGenerationToInput(appended, _state.value.targetDate)
    }

    /**
     * 发送输入进行流式解析并产出预览。[AI修改] B4: 按 inputMode 构造 segments → 其余与 B3 完全一致。
     */
    fun submit() {
        val state = _state.value
        val segments = when (state.inputMode) {
            InputMode.QUICK -> {
                val text = state.quickDraftText.trim()
                if (text.isBlank()) return
                InputSegmentFactory.forQuickRecord(text, state.targetDate)
            }
            InputMode.WEEK -> {
                val anchor = state.periodWeekMonday ?: return
                val dayTexts = (0..6).map { state.periodInputs[it] ?: "" }
                InputSegmentFactory.forPeriodicRecord(dayTexts, anchor)
            }
        }
        // 全空白不发送
        if (segments.all { it.isBlank }) return

        val generationId = "meal-${++generationCounter}"
        val request = StreamingMealRequest(
            segments = segments,
            generationId = generationId,
            weekAnchor = InputSegmentFactory.mondayOfWeek(
                segments.firstOrNull { !it.isBlank }?.targetDate ?: state.targetDate
            ),
        )
        val session = StreamingMealSession(request)

        // 新 generation 前取消旧 Job（旧会话后到事件不再喂入）
        generationJob?.cancel()
        lastPreviewDays = null
        lastPreviewTerminalCount = -1
        // [AI新增] segmentId 在不同 generation 间可能重复（同日期→同 segmentId），必须清空防止跨 generation 串数据。
        ruleFallbackDays.clear()
        fallbackAttempted.clear()
        segmentFallbackReasons.clear()
        ruleWarnings.clear()
        // [AI生成] B5: 初始化段进度
        val nonBlankSegments = segments.filter { !it.isBlank }.sortedBy { it.ordinal }
        val initialProgress = GenerationProgress(
            totalSegments = nonBlankSegments.size,
            completedSegments = 0,
            failedSegments = 0,
            currentSegmentIndex = 0,  // [AI修改] B6-fix: 显示序下标，初始段即 index 0（AF-B456-05）。
            currentSegmentLabel = nonBlankSegments.firstOrNull()?.targetDate?.let { shortWeekday(it) } ?: "",
            segmentStatuses = nonBlankSegments.map { null },  // [AI修改] B6-fix2: 初始全部"未开始"，不预判为 STREAMING（AF-B456-05 第二轮·§3.5.1）。
        )
        _state.update {
            it.copy(
                phase = AiMealPhase.GENERATING,
                generationId = generationId,
                generationProgress = initialProgress,
                isGenerating = true,
                segmentStates = session.snapshot().segmentStates,
                errorMessage = null,
                diagnostic = null,
                autoGenPreview = null,
                autoGenResult = null,
                parseSourceMessage = "",
                mergeConfirmationRequired = false,
                mergeConfirmed = false,
            )
        }

        generationJob = viewModelScope.launch {
            var segment = session.nextSegment()
            while (segment != null && _state.value.generationId == generationId) {
                val seg = segment
                if (!configReady) {
                    // [AI新增] 未配置真实云端 AI：不发起任何网络请求，该段直接判定为"未尝试"并走规则解析
                    // （不是失败，是本来就该用的引擎——不产出"AI 解析失败"框架的文案）。
                    session.onFailed(seg.segmentId, ENGINE_NOT_CONFIGURED_REASON)
                    attemptRuleFallback(seg, ENGINE_NOT_CONFIGURED_REASON, generationId, session)
                    handleSessionSnapshot(session, generationId, isFinal = false)
                    segment = session.nextSegment()
                    continue
                }
                val llmRequest = AiMealPrompt.buildStreamingRequest(seg)
                // 顺序收集当前段流；流结束（Completed/Failed 后 close）返回。
                try {
                    aiRuntime.stream(llmRequest).collect { event ->
                        when (event) {
                            is LlmStreamEvent.Delta -> {
                                // AF-B3-01: 每 Delta 后立即 snapshot/preview（lastPreviewDays 去重）。
                                session.onDelta(seg.segmentId, event.text)
                                handleSessionSnapshot(session, generationId, isFinal = false)
                            }
                            is LlmStreamEvent.Completed -> {
                                session.onCompleted(seg.segmentId, event.finishReason)
                                handleSessionSnapshot(session, generationId, isFinal = false)
                            }
                            is LlmStreamEvent.Failed -> {
                                session.onFailed(seg.segmentId, event.message)
                                // [AI新增] AI 该段失败：自动、立即（同协程内）尝试规则解析兜底——不是静默切换，
                                // 原因通过 parseWarnings（既有 snap.diagnostics 通道）与确认页 parseSourceMessage
                                // 双重可见（GC-30/透明准则 T1：能自动做好又无害的就"做+告知"）。
                                attemptRuleFallback(seg, event.message, generationId, session)
                                handleSessionSnapshot(session, generationId, isFinal = false)
                            }
                        }
                    }
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // [AI修改] B3.4-R4-07: stream 实现抛未捕获异常时，记录为段失败而非静默丢失。
                    if (isCurrentGeneration(generationId)) {
                        session.onFailed(seg.segmentId, "STREAM_COLLECT_ERROR: ${e.message}")
                        attemptRuleFallback(seg, "STREAM_COLLECT_ERROR: ${e.message}", generationId, session)
                        handleSessionSnapshot(session, generationId, isFinal = false)
                    }
                }
                if (_state.value.generationId != generationId) return@launch
                // AF-B3-02: 流返回但当前段仍未终态（STREAMING）→ 记为异常结束。
                // [AI修改] Google质量复核：必须核对 session.isStreaming(...)（真实状态），不能只比较
                // currentSegmentId()（只看下标顺序，段已 Completed 时下标未推进、这里仍会误判为"未终态"，
                // 对已成功的段重复调用 onFailed/规则兜底——虽然 onFailed 内部对非 STREAMING 段是 no-op，
                // 但这里的目的就是精确表达"仅当真的还没终态才处理"，不依赖下游兜底吸收错误触发）。
                if (session.currentSegmentId() == seg.segmentId &&
                    session.isStreaming(seg.segmentId) &&
                    _state.value.generationId == generationId) {
                    session.onFailed(seg.segmentId, "STREAM_ENDED_WITHOUT_TERMINAL")
                    attemptRuleFallback(seg, "STREAM_ENDED_WITHOUT_TERMINAL", generationId, session)
                    handleSessionSnapshot(session, generationId, isFinal = false)
                }
                segment = session.nextSegment()
            }
            // [AI新增] Google质量二次复核：补上"AI 正常 Completed 但没解析出任何合法菜"（模型只回了大段文字/
            // 道歉、没有一行合法 NDJSON）这条路径的兜底——它不会经过任何 onFailed 调用点，之前完全没有
            // attemptRuleFallback 的机会，直接落到"没能识别出菜品"且没有任何自动兜底，与本功能承诺的
            // "AI 失败就自动转规则"矛盾。收尾时对每个仍无合法结果的段补触发一次。
            if (_state.value.generationId == generationId) {
                for (seg in session.request.nonBlankSegments) {
                    val hasAiDay = session.daysForSegment(seg.segmentId).any { it.meals.any { m -> m.dishes.isNotEmpty() } }
                    if (!hasAiDay) {
                        attemptRuleFallback(seg, "AI 未产出可用结果", generationId, session)
                    }
                }
            }
            // 全部段终态：最终重算 preview 或进入 ERROR
            if (_state.value.generationId == generationId) {
                handleSessionSnapshot(session, generationId, isFinal = true)
            }
        }
    }

    /**
     * [AI新增] 对失败的段自动尝试规则解析兜底（AI 未配置或该段 AI 失败时触发）。
     *
     * 幂等：同一 segmentId 只尝试一次（`fallbackAttempted` 守卫），避免同一段的多个 onFailed 调用点
     * （Failed 事件 / 异常捕获 / 流结束未终态）重复触发。仅在规则解析真的产出合法餐食时写入
     * [ruleFallbackDays]；解析不出时不写入，交由 [mergeDays] 后的 `hasValidMeals` 判断进入真正的 ERROR。
     *
     * 若该段其实已通过 AI 拿到合法餐食（如"流结束但未收到干净的终态事件"发生在有效 Delta 之后，
     * T-B3-02 场景），跳过兜底——不浪费一次规则解析，也不用规则结果覆盖已解析出的 AI 内容。
     * [AI修改] Google质量复核：`aiAlreadyValid` 改用 [StreamingMealSession.daysForSegment]（按 segmentId 取该段
     * 自身草稿），不再按日期字符串匹配——AI 可能为该段声明与 `seg.targetDate` 不同的绝对日期（用户说"昨天"）
     * 或用 `/` 分隔符，字符串匹配会误判"未解析"、错误覆盖已成功的 AI 结果。
     * [AI修改] 规则解析产出的相对日期（`date==null, date_offset` 相对 `seg.targetDate` 计算，见
     * `MealDateAnchorPolicy.apply`）必须在此处立即解析成绝对日期——后续 `handleSessionSnapshot` 统一用
     * 首段日期做 `frozenDate` 传给 `preview()`，周期记非首段若仍带相对 offset 会被按错误锚点重新解析。
     * [AI修改] `RuleFallbackResult.warning`（如"当前餐食以选择的餐食日期为参照"）不再丢弃，写入 [ruleWarnings]
     * 供确认页展示，对齐手动 [useRuleFallback] 路径的既有行为。
     */
    private suspend fun attemptRuleFallback(seg: InputSegment, reason: String, generationId: String, session: StreamingMealSession) {
        if (!isCurrentGeneration(generationId)) return
        if (!fallbackAttempted.add(seg.segmentId)) return
        val aiAlreadyValid = session.daysForSegment(seg.segmentId).any { day -> day.meals.any { it.dishes.isNotEmpty() } }
        if (aiAlreadyValid) return
        val result = runCatching { sessionPort.parseRule(seg.inputText, seg.targetDate) }
            .getOrElse { if (it is kotlinx.coroutines.CancellationException) throw it else return }
        if (!isCurrentGeneration(generationId)) return
        val day = result.days.firstOrNull { day -> day.meals.any { it.dishes.isNotEmpty() } } ?: return
        val resolvedDay = if (day.date == null) {
            day.copy(date = DateTime.plusDays(seg.targetDate, day.date_offset).toString(), date_offset = 0)
        } else day
        ruleFallbackDays[seg.segmentId] = resolvedDay
        segmentFallbackReasons[seg.segmentId] = reason
        result.warning?.let { ruleWarnings[seg.segmentId] = it }
    }

    /**
     * [AI新增] 合并 AI 解析结果与规则兜底结果：按显示序遍历各段，AI 该段有合法餐食则用 AI 结果，
     * 否则取 [ruleFallbackDays] 中该段的规则兜底结果（可能仍缺失，即两个引擎都没解析出内容）。
     * [AI修改] Google质量复核：改用 [StreamingMealSession.daysForSegment] 按 segmentId 取段自身结果，
     * 不再按日期字符串跨段匹配（同一原因见 [attemptRuleFallback] 顶部说明）。
     */
    private fun mergeDays(session: StreamingMealSession): List<DayMealJson> {
        return session.request.nonBlankSegments.sortedBy { it.ordinal }.mapNotNull { seg ->
            val aiDay = session.daysForSegment(seg.segmentId).firstOrNull { day -> day.meals.any { it.dishes.isNotEmpty() } }
            aiDay ?: ruleFallbackDays[seg.segmentId]
        }
    }

    /**
     * [AI新增] 确认页"本次结果来自哪个引擎"说明文案。无兜底发生时返回空串（不显示，维持现状：默认即 AI）。
     */
    private fun buildParseSourceMessage(session: StreamingMealSession): String {
        val segs = session.request.nonBlankSegments
        val fallbackSegs = segs.filter { it.segmentId in ruleFallbackDays }
        if (fallbackSegs.isEmpty()) return ""
        if (!configReady) return "本次结果：规则解析"
        val allFallback = fallbackSegs.size == segs.size
        val reason = fallbackSegs.mapNotNull { segmentFallbackReasons[it.segmentId] }
            .firstOrNull { it != ENGINE_NOT_CONFIGURED_REASON }
            ?.let(::humanizeWarning)
        return if (allFallback) {
            "本次结果：规则解析（AI 解析失败：${reason ?: "未知原因"}）"
        } else {
            "部分内容由规则解析补充（AI 解析失败：${reason ?: "未知原因"}）"
        }
    }

    /**
     * [AI新增/修改] 把内部诊断代号转成用户可读文案；已是人读文案（如 CloudAiRuntime 的 Key 未配置提示）时原样透传。
     * `ENGINE_NOT_CONFIGURED_REASON` 返回 null——它代表"本就该用规则"的正常路径，不是需要告知的异常，
     * 不该出现在诊断/警告类展示位（Google质量复核：原实现遗漏此过滤，未配置用户会在"诊断信息"里看到内部代号原文）。
     */
    private fun humanizeWarning(raw: String): String? = when {
        raw == ENGINE_NOT_CONFIGURED_REASON -> null
        raw.startsWith("STREAM_COLLECT_ERROR") -> "网络请求异常"
        raw == "STREAM_ENDED_WITHOUT_TERMINAL" -> "AI 响应异常中断"
        else -> raw
    }

    /**
     * [AI新增 10-a] 把 StreamingMealParser 的协议级诊断（如 "dish_id「xxx」格式无效，已拒绝" 这类
     * 开发者措辞）按分类代号合并计数，转成用户能看懂、能行动的文案，取代此前逐条原样透传。
     * 非阻断的自愈类代号（MEAL_ID_NORMALIZED/DISH_ID_REUSED）不展示——没有内容丢失，克制去噪。
     *
     * [AI修改 Google质量复核🔴-2/🟡-1修复] 只处理了 8 个分类代号会导致"漏一个就永久静默"：
     * 段级失败原因（如 STREAM_ENDED_WITHOUT_TERMINAL）由 StreamingMealSession.snapshot() 包装时
     * 未带 code（默认 OTHER），若不兜底会连同其余未分类 ERROR（缺字段/整体JSON拒绝等协议原文）
     * 一起被静默丢弃——用户"AI 中断且规则兜底也没产出"时会看不到任何解释。末尾补一个通用兜底：
     * 未被上面分类覆盖的 ERROR 级诊断统一走 [humanizeWarning]（转人话 + 过滤 ENGINE_NOT_CONFIGURED_REASON
     * 等内部哨兵），保证"总有话可说"而不是无声无息。
     */
    private fun summarizeDiagnostics(diagnostics: List<StreamDiagnostic>): List<String> {
        val byCode = diagnostics.groupBy { it.code }
        val handledCodes = setOf(
            DiagnosticCode.INVALID_SLOT, DiagnosticCode.MEAL_ID_MISMATCH,
            DiagnosticCode.DISH_ID_FORMAT, DiagnosticCode.ORPHAN_DISH, DiagnosticCode.DISH_CONFLICT,
            DiagnosticCode.ORPHAN_INGREDIENT, DiagnosticCode.TRUNCATED, DiagnosticCode.PARSE_ERROR,
        )
        return buildList {
            val mealIssues = (byCode[DiagnosticCode.INVALID_SLOT].orEmpty() + byCode[DiagnosticCode.MEAL_ID_MISMATCH].orEmpty())
                .distinctBy { it.mealId ?: it.message }
            if (mealIssues.isNotEmpty()) add("有 ${mealIssues.size} 餐没能对上餐次，这部分没有记录")

            val dishIssues = (
                byCode[DiagnosticCode.DISH_ID_FORMAT].orEmpty() +
                    byCode[DiagnosticCode.ORPHAN_DISH].orEmpty() +
                    byCode[DiagnosticCode.DISH_CONFLICT].orEmpty()
                ).distinctBy { it.dishId ?: it.message }
            if (dishIssues.isNotEmpty()) add("有 ${dishIssues.size} 道菜的信息不完整，已跳过——可以重说一遍，或改用规则解析")

            val ingredientIssues = byCode[DiagnosticCode.ORPHAN_INGREDIENT].orEmpty()
            if (ingredientIssues.isNotEmpty()) add("有 ${ingredientIssues.size} 种食材没能对应到具体菜品，已跳过（不影响已识别的菜）")

            if (byCode[DiagnosticCode.TRUNCATED].orEmpty().isNotEmpty()) add("AI 的回复被截断了，已保留能识别的部分")
            if (byCode[DiagnosticCode.PARSE_ERROR].orEmpty().isNotEmpty()) add("AI 的部分回复格式有问题，已跳过")

            diagnostics.asSequence()
                .filter { it.level == DiagnosticLevel.ERROR && it.code !in handledCodes }
                .mapNotNull { humanizeWarning(it.message) }
                .distinct()
                .forEach { add(it) }
        }
    }

    /**
     * B3: 将 session 快照落为 UI 状态。[AI修改]
     *
     * - 无合法餐食且非 final：仅更新进度/诊断，不调 preview。
     * - 有合法餐食：PARTIAL_READY（生成中）或 PREVIEW_READY（final）；相同 days 不重复 preview。
     * - 无合法餐食且 final：ERROR。
     * - [B5] preview 触发优化：段终态或 final 才调 previewAll，Delta 中途仅更新 progress。
     * - [AI新增] "有效"判定改用 AI 结果 + 规则兜底结果的合并集 [mergeDays]，而非 `snap.hasValidMeals`（后者只看 AI）；
     *   段失败时已在调用方自动尝试规则兜底（见 [attemptRuleFallback]），此处只负责合并与展示，不重复触发解析。
     *   走到"没能识别出菜品"时，意味着两个引擎都没有为这些段解析出任何菜——此时该文案才是诚实的。
     */
    private suspend fun handleSessionSnapshot(session: StreamingMealSession, generationId: String, isFinal: Boolean) {
        // AF-B3-R2-01: 唯一 generation 谓词。
        if (!isCurrentGeneration(generationId)) return
        val snap = session.snapshot()
        val progress = computeProgress(session, snap)
        val mergedDays = mergeDays(session)
        val hasValidMeals = mergedDays.any { day -> day.meals.any { it.dishes.isNotEmpty() } }
        // [AI修改 10-a] 诊断消息改走 summarizeDiagnostics 按分类代号合并计数出人话文案（此前逐条原样
        // 透传开发者协议措辞，如"dish_id「xxx」格式无效，已拒绝"，用户看不懂也不知道少了哪道菜）+ 并入规则解析自身的 warning。
        val displayWarnings = (summarizeDiagnostics(snap.diagnostics) + ruleWarnings.values).distinct()

        // [B5] 快速路径：无合法餐食 + 非 final → 仅更新进度，不调 preview
        if (!hasValidMeals && !isFinal) {
            _state.update {
                it.copy(segmentStates = snap.segmentStates, isGenerating = true, generationProgress = progress)
            }
            return
        }
        if (!hasValidMeals) {
            _state.update {
                it.copy(
                    phase = AiMealPhase.ERROR,
                    errorMessage = "没能识别出菜品，试试更具体的描述？\n如「中午吃了红烧肉和米饭」",
                    segmentStates = snap.segmentStates,
                    generationProgress = progress,
                    isGenerating = false,
                    // [AI修改] B3.4-R4-05: 将 session 诊断传入 parseWarnings，帮助用户理解失败原因。
                    parseWarnings = displayWarnings,
                )
            }
            return
        }

        // [B5] 非段边界且非 final → 仅更新进度（跳过 preview，优化频繁 Delta）
        val isBoundary = progress.terminalSegments != lastPreviewTerminalCount
        if (!isFinal && !isBoundary && _state.value.autoGenPreview != null) {
            _state.update {
                it.copy(phase = AiMealPhase.PARTIAL_READY, segmentStates = snap.segmentStates, generationProgress = progress, isGenerating = true)
            }
            return
        }

        // 相同 days 不重复调用 previewAll
        if (!isFinal && lastPreviewDays == mergedDays && _state.value.autoGenPreview != null) {
            _state.update {
                it.copy(phase = AiMealPhase.PARTIAL_READY, segmentStates = snap.segmentStates, generationProgress = progress, isGenerating = true)
            }
            return
        }
        // AF-B3-03: preview date 取 session/request 冻结日期，不读可变 UI date。
        val frozenDate = session.request.segments.firstOrNull()?.targetDate ?: _state.value.targetDate
        try {
            val preview = sessionPort.preview(mergedDays, frozenDate)
            // AF-B3-R3-01: preview 挂起后先比对 generation，旧 A 直接返回（不跑健康摘要）。
            if (!isCurrentGeneration(generationId)) return
            // [AI修改] B3.4-R4-02: 健康摘要独立容错——即使 buildHealthSafetyReport 抛异常，
            // preview 也不应被丢弃；异常时降级为默认提示。NonCancellable 防取消打断 DB 读。
            val safetyReport = runCatching {
                withContext(kotlinx.coroutines.NonCancellable) {
                    buildHealthSafetyReport(preview)
                }
            }.getOrDefault(HealthSafetyReport(listOf("健康档案暂不可用")))
            if (!isCurrentGeneration(generationId)) return
            lastPreviewDays = mergedDays
            lastPreviewTerminalCount = progress.terminalSegments
            _state.update {
                it.copy(
                    phase = if (isFinal) AiMealPhase.PREVIEW_READY else AiMealPhase.PARTIAL_READY,
                    autoGenPreview = preview,
                    segmentStates = snap.segmentStates,
                    generationProgress = progress,
                    isGenerating = !isFinal,
                    parseWarnings = displayWarnings,
                    parseSourceMessage = buildParseSourceMessage(session),
                    mergeConfirmationRequired = preview.days.any { it.hasExisting },
                    mergeConfirmed = false,
                    healthSafetyReport = safetyReport,
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isCurrentGeneration(generationId)) return
            com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "preview failed", e)
            _state.update {
                it.copy(
                    phase = AiMealPhase.ERROR,
                    // [AI修改] 文案准则：不把原始异常 message 直接展示给用户，诊断信息走 parseWarnings 通道。
                    errorMessage = "预览生成失败，请重新试试",
                    isGenerating = false,
                )
            }
        }
    }

    /** AF-B3-R2-01: 当前 generation 谓词。 */
    private fun isCurrentGeneration(generationId: String): Boolean =
        _state.value.generationId == generationId

    /** [AI生成] B5: 简短星期标签（ViewModel 内联，不依赖 Sheet 私有函数）。 */
    private fun shortWeekday(date: LocalDate): String = when (date.dayOfWeek) {
        kotlinx.datetime.DayOfWeek.MONDAY -> "周一"
        kotlinx.datetime.DayOfWeek.TUESDAY -> "周二"
        kotlinx.datetime.DayOfWeek.WEDNESDAY -> "周三"
        kotlinx.datetime.DayOfWeek.THURSDAY -> "周四"
        kotlinx.datetime.DayOfWeek.FRIDAY -> "周五"
        kotlinx.datetime.DayOfWeek.SATURDAY -> "周六"
        kotlinx.datetime.DayOfWeek.SUNDAY -> "周日"
    }

    /** [AI生成] B5: 从 snapshot 和 session 推算当前段进度。[AI修改] B6-fix: 产出逐段状态列表 segmentStatuses，UI 只做 1:1 映射（AF-B456-05·GC-17·INV-B456-R05a/b/c）。 */
    private fun computeProgress(session: StreamingMealSession, snap: StreamingSessionSnapshot): GenerationProgress {
        val nonBlank = session.request.nonBlankSegments.sortedBy { it.ordinal }
        val states = snap.segmentStates
        val completed = nonBlank.count { states[it.segmentId] == StreamSegmentState.COMPLETED }
        val failed = nonBlank.count { states[it.segmentId] == StreamSegmentState.FAILED }
        val terminalCount = completed + failed
        // [AI修改] B6-fix2: 逐段状态列表——由 VM 按显示序直接产出。null=尚未开始，不兜底为 STREAMING（AF-B456-05 第二轮·INV-B456-R05d·GC-36·§3.5.1）。
        val segmentStatuses = nonBlank.map { seg ->
            states[seg.segmentId]
        }
        // [AI生成] B6-fix: currentSegmentIndex = 显示序下标，非业务 ordinal（AF-B456-05·INV-B456-R05b·§3.5 索引空间对照表）。
        val currentSeg = nonBlank.firstOrNull { states[it.segmentId] == StreamSegmentState.STREAMING }
        val fallback = nonBlank.getOrNull(terminalCount) ?: nonBlank.lastOrNull()
        val currentIdx = if (currentSeg != null) nonBlank.indexOf(currentSeg) else {
            val fbIdx = fallback?.let { nonBlank.indexOf(it) } ?: -1
            if (fbIdx >= 0) fbIdx else 0
        }
        val label = currentSeg?.targetDate?.let { shortWeekday(it) }
            ?: fallback?.targetDate?.let { shortWeekday(it) } ?: ""
        return GenerationProgress(
            totalSegments = nonBlank.size,
            completedSegments = completed,
            failedSegments = failed,
            currentSegmentIndex = currentIdx,
            currentSegmentLabel = label,
            segmentStatuses = segmentStatuses,
        )
    }

    /**
     * 用户手动触发的规则解析重试。[AI修改] 自动兜底（见 [attemptRuleFallback]）已覆盖"AI 失败自动转规则"的
     * 主路径；本方法保留给"两个引擎都没解析出内容、用户改了描述想再试一次规则解析"的残余场景。
     *
     * 仅当 phase=ERROR 且当前 generation 无合法餐食时执行；产物标记"规则解析"。当前无 UI 调用点（孤儿方法，
     * 沿用 B5 前的既有状态，非本次改动引入）。
     */
    fun useRuleFallback() {
        val state = _state.value
        // AF-B3-R3-01: 仅 ERROR && 无合法 preview && generationId 非空时启动；赋给既有 generationJob。
        if (state.phase != AiMealPhase.ERROR || state.autoGenPreview != null || state.generationId == null) return
        val fallbackGenerationId = state.generationId
        val fallbackText = state.inputText
        val fallbackDate = state.targetDate

        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                val result = sessionPort.parseRule(fallbackText, fallbackDate)
                if (!isCurrentGeneration(fallbackGenerationId)) return@launch
                if (result.days.isEmpty() || result.days.all { it.meals.isEmpty() || it.meals.all { m -> m.dishes.isEmpty() } }) {
                    if (!isCurrentGeneration(fallbackGenerationId)) return@launch
                    _state.update {
                        it.copy(phase = AiMealPhase.ERROR, errorMessage = "规则解析也未能识别出菜品，请重新描述")
                    }
                    return@launch
                }
                val preview = sessionPort.preview(result.days, fallbackDate)
                if (!isCurrentGeneration(fallbackGenerationId)) return@launch
                _state.update {
                    it.copy(
                        phase = AiMealPhase.PREVIEW_READY,
                        autoGenPreview = preview,
                        parseSourceMessage = "本次结果：规则解析",
                        parseWarnings = listOfNotNull(result.warning),
                        mergeConfirmationRequired = preview.days.any { it.hasExisting },
                        mergeConfirmed = false,
                        isGenerating = false,
                    )
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isCurrentGeneration(fallbackGenerationId)) return@launch
                com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "rule fallback failed", e)
                _state.update {
                    it.copy(phase = AiMealPhase.ERROR, errorMessage = "规则解析失败，请重新试试")
                }
            }
        }
    }

    /** 只使用本地档案和预览事实；不调用云端、不阻断真实记录。 [AI生成] */
    private suspend fun buildHealthSafetyReport(preview: AutoGenPreview): HealthSafetyReport {
        val enabled = healthSummaryLabels()
        val allDishes = preview.days.flatMap { it.meals }.flatMap { it.dishes }
        val newDishCount = allDishes.count { it.resolution == com.sxdbsm.cookbook.domain.autogen.ResolveKind.CREATE }
        val pendingIngredients = allDishes.flatMap { it.ingredients }
            .count { it.careFlag == com.sxdbsm.cookbook.domain.autogen.CareFlag.PENDING_REVIEW }
        return HealthSafetyReport(buildList {
            // [AI修改] 透明准则交叉项：确认页此前只报"新食材数"，没告知会新建几道菜——
            // 补上"改动清单"最小充分形态，是否要写库仍由用户点确认决定。
            if (newDishCount > 0 || pendingIngredients > 0) {
                add(
                    "本次将新建 $newDishCount 道菜" +
                        if (pendingIngredients > 0) "、$pendingIngredients 种食材（营养为自动估算，仅供参考）" else "",
                )
            }
            if (enabled.isNotEmpty()) add("已结合健康档案：${enabled.joinToString("、")}")
            if (isEmpty()) add("未设置健康档案；可按个人情况核对本餐")
        })
    }

    fun requestHealthAdvice() {
        if (_state.value.autoGenPreview == null || _state.value.healthAdviceLoading) return
        _state.update { it.copy(healthAdviceConsentPending = true, healthAdviceError = null) }
    }

    fun declineHealthAdvice() = _state.update { it.copy(healthAdviceConsentPending = false) }

    fun confirmHealthAdvice() {
        val current = _state.value
        val preview = current.autoGenPreview ?: return
        val generationId = current.generationId ?: return
        if (!isPreviewPhase(current.phase)) return
        _state.update { it.copy(healthAdviceConsentPending = false, healthAdviceLoading = true, healthAdviceError = null) }
        healthAdviceJob?.cancel()
        healthAdviceJob = viewModelScope.launch {
            try {
                val healthSummary = healthSummaryLabels().ifEmpty { listOf("未设置具体健康档案") }.joinToString("、")
                val mealSummary = preview.days.flatMap { it.meals }.flatMap { it.dishes }
                    .joinToString("、") { it.inputName }.take(500)
                val result = aiRuntime.complete(AiMealHealthAdvice.request(healthSummary, mealSummary))
                if (!isCurrentHealthAdviceRequest(generationId, preview)) return@launch
                _state.update {
                    it.copy(
                        healthAdviceLoading = false,
                        healthAdvice = result.getOrNull()?.trim()?.takeIf(String::isNotBlank),
                        healthAdviceError = result.exceptionOrNull()?.message?.take(120)
                            ?: if (result.getOrNull().isNullOrBlank()) "暂时无法生成建议" else null,
                    )
                }
                healthAdviceJob = null
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                if (!isCurrentHealthAdviceRequest(generationId, preview)) return@launch
                _state.update {
                    it.copy(
                        healthAdviceLoading = false,
                        healthAdvice = null,
                        healthAdviceError = e.message?.take(120) ?: "暂时无法生成建议",
                    )
                }
                healthAdviceJob = null
            }
        }
    }

    private fun clearHealthAdviceForInvalidatedSession() {
        healthAdviceJob?.cancel()
        healthAdviceJob = null
        _state.update {
            it.copy(
                healthAdviceConsentPending = false,
                healthAdviceLoading = false,
                healthAdvice = null,
                healthAdviceError = null,
            )
        }
    }

    private fun isPreviewPhase(phase: AiMealPhase): Boolean =
        phase == AiMealPhase.PARTIAL_READY || phase == AiMealPhase.PREVIEW_READY

    private fun isCurrentHealthAdviceRequest(generationId: String, preview: AutoGenPreview): Boolean {
        val current = _state.value
        return current.generationId == generationId &&
            current.autoGenPreview === preview &&
            isPreviewPhase(current.phase)
    }

    /** 仅输出匿名成员序号与病种/生命阶段标签；绝不发送姓名或 ID。 [AI修改] */
    private suspend fun healthSummaryLabels(): List<String> {
        val namesById = healthRepo.listAllCrowdTypes().associate { it.id to it.name }
        val members = familyRepo.listMembers()
        val memberLabels = members.mapIndexedNotNull { index, member ->
            member.careCategoryIds.mapNotNull(namesById::get).distinct().takeIf { it.isNotEmpty() }
                ?.let { "成员${index + 1}:${it.joinToString("·")}" }
        }
        val legacy = healthRepo.listAll().filter { it.enabled }.map { it.crowdName }
        return (memberLabels + legacy).distinct()
    }


    /**
     * 确认保存。[AI修改] B3.1-PORT/AF-B3-05：仅 PARTIAL_READY/PREVIEW_READY 可保存；
     * 第二次 MERGE 确认前把当前 AutoGenPreview 存入局部不可变值，再取消 generation 并原子清会话。
     */
    fun confirmSave() {
        val cur = _state.value
        // AF-B3-05: 仅可预览阶段可保存；SAVING/DONE/ERROR 重复调用直接 return。
        if (cur.phase != AiMealPhase.PARTIAL_READY && cur.phase != AiMealPhase.PREVIEW_READY) return
        val preview = cur.autoGenPreview ?: return

        if (cur.mergeConfirmationRequired && !cur.mergeConfirmed) {
            _state.update { it.copy(mergeConfirmed = true) }
            return
        }

        // 局部不可变 preview；先冻结，再取消 generation 并原子清会话状态。
        val frozenPreview = preview
        generationJob?.cancel()
        clearHealthAdviceForInvalidatedSession()
        _state.update {
            it.copy(
                phase = AiMealPhase.SAVING,
                generationId = null,
                isGenerating = false,
            )
        }
        lastPreviewDays = null
        lastPreviewTerminalCount = -1  // [AI修改] B5-review: 补齐配对重置

        // [AI修改] B3.4-R4-01: save 协程复用 generationJob slot，invalidateGenerationToInput 会 cancel 它；
        // _state.update 内加 phase==SAVING 守卫防止竞态覆盖。
        generationJob = viewModelScope.launch {
            try {
                val result = sessionPort.commit(frozenPreview)
                _state.update {
                    if (it.phase == AiMealPhase.SAVING)
                        it.copy(phase = AiMealPhase.DONE, autoGenResult = result)
                    else it
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    if (it.phase == AiMealPhase.SAVING)
                        it.copy(phase = AiMealPhase.ERROR, errorMessage = "保存失败，请重试")
                    else it
                }
            }
        }
    }

    /** 重新输入。[AI修改] B3.1: 取消进行中 generation，保留添加页日期。 */
    fun reset() {
        invalidateGenerationToInput(_state.value.inputText, _state.value.targetDate)
    }

    /** 从错误恢复。[AI修改] R3-01: 走 invalidate，取消进行中 generation。 */
    fun dismissError() {
        invalidateGenerationToInput(_state.value.inputText, _state.value.targetDate)
    }

    /** [AI修改] B3.4-R4-03: 取消进行中 generation（Sheet 关闭守卫用），不重置输入文本。 */
    fun cancelGeneration() {
        generationJob?.cancel()
        generationJob = null
        clearHealthAdviceForInvalidatedSession()
        lastPreviewDays = null
        lastPreviewTerminalCount = -1  // [AI修改] B5-review: 补齐配对重置
        ruleFallbackDays.clear()
        fallbackAttempted.clear()
        segmentFallbackReasons.clear()
        ruleWarnings.clear()
    }

    /** [AI修改] B3.4-R4-04: 重试保存——复用当前 autoGenPreview；仅 ERROR 态可触发，防连点并发。 */
    fun retrySave() {
        val cur = _state.value
        if (cur.phase != AiMealPhase.ERROR) return
        val preview = cur.autoGenPreview ?: return

        _state.update {
            it.copy(phase = AiMealPhase.SAVING, errorMessage = null)
        }

        // 复用 generationJob slot；先 cancel 旧 Job 防连点并发。
        generationJob?.cancel()
        generationJob = viewModelScope.launch {
            try {
                val result = sessionPort.commit(preview)
                _state.update {
                    if (it.phase == AiMealPhase.SAVING)
                        it.copy(phase = AiMealPhase.DONE, autoGenResult = result)
                    else it
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    if (it.phase == AiMealPhase.SAVING)
                        it.copy(phase = AiMealPhase.ERROR, errorMessage = "保存失败，请重试")
                    else it
                }
            }
        }
    }

    /** 设置目标日期（预览页调整）。[AI修改] B3.1 AF-B3-03: 日期变更取消进行中 generation。 */
    fun setTargetDate(date: LocalDate) {
        invalidateGenerationToInput(_state.value.inputText, date)
    }
}
