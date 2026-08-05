package com.sxdbsm.cookbook.android.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.LlmStreamEvent
import com.sxdbsm.cookbook.ai.meallog.AiMealParseResult
import com.sxdbsm.cookbook.ai.meallog.AiMealParser
import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.ai.meallog.AiMealHealthAdvice
import com.sxdbsm.cookbook.ai.meallog.DayMealJson
import com.sxdbsm.cookbook.ai.meallog.InputSegment
import com.sxdbsm.cookbook.ai.meallog.MultiDayRecorder
import com.sxdbsm.cookbook.ai.meallog.MealDateAnchorPolicy
import com.sxdbsm.cookbook.ai.meallog.RuleMealParser
import com.sxdbsm.cookbook.ai.meallog.SchemaMigration
import com.sxdbsm.cookbook.ai.meallog.StreamSegmentState
import com.sxdbsm.cookbook.ai.meallog.StreamingMealRequest
import com.sxdbsm.cookbook.ai.meallog.StreamingMealSession
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.domain.autogen.AutoGenPreview
import com.sxdbsm.cookbook.domain.autogen.AutoGenResult
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

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

/** 输入模式。[AI生成] */
enum class InputMode { TEXT, VOICE }

/** 处理阶段。[AI修改] B3: 唯一状态机 INPUT→GENERATING→PARTIAL_READY→PREVIEW_READY→SAVING→DONE；删除 PARSING/PREVIEW。 */
enum class AiMealPhase { INPUT, GENERATING, PARTIAL_READY, PREVIEW_READY, SAVING, DONE, ERROR }

/** 语音识别状态。[AI生成] */
enum class VoiceState { IDLE, LISTENING, PROCESSING, ERROR }

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
    val inputText: String = "",
    val inputMode: InputMode = InputMode.TEXT,
    val phase: AiMealPhase = AiMealPhase.INPUT,
    /** [AI修改] B3: 当前 generation 标识；会话只读。 */
    val generationId: String? = null,
    /** [AI修改] B3: 各分段状态；会话只读。 */
    val segmentStates: Map<String, StreamSegmentState> = emptyMap(),
    /** [AI修改] B3: 生成中标记（GENERATING/PARTIAL_READY 为 true）。 */
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
)

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
        AiMealInputUiState(inputText = initialText, targetDate = targetDate)
    )
    val state: StateFlow<AiMealInputUiState> = _state.asStateFlow()

    // [AI修改] B3: 当前 generation 的流收集 Job；新 generation 前 cancel 旧 Job。
    private var generationJob: Job? = null
    // [AI修改] B3: generation 单调递增序号。
    private var generationCounter = 0
    // [AI修改] B3: 最近一次 preview 的 days，避免相同内容重复调用 previewAll。
    private var lastPreviewDays: List<DayMealJson>? = null

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

    /** 设置输入文本。[AI修改] B3.1 AF-B3-03: 编辑即新会话，取消进行中 generation。 */
    fun setInputText(text: String) {
        invalidateGenerationToInput(text, _state.value.targetDate)
    }

    /** 原子失效进行中 generation 并回到 INPUT 态。[AI修改] */
    private fun invalidateGenerationToInput(nextInput: String, nextDate: LocalDate) {
        generationJob?.cancel()
        generationJob = null
        lastPreviewDays = null
        _state.update {
            AiMealInputUiState(
                inputText = nextInput,
                targetDate = nextDate,
                inputMode = it.inputMode,
            )
        }
    }

    /** 切换输入模式。[AI生成] */
    fun setInputMode(mode: InputMode) {
        _state.update { it.copy(inputMode = mode) }
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
        val current = _state.value.inputText.trimEnd()
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
        val current = _state.value.inputText.trimEnd()
        val appended = if (current.isBlank()) text else "$current\n$text"
        invalidateGenerationToInput(appended, _state.value.targetDate)
    }

    /**
     * 发送输入进行流式解析并产出预览。[AI修改] B3: 冻结 session → 顺序收集 stream → session → previewAll。
     *
     * 只构造 quick segment（B4 将替换为周期 segments）；不改 session/mapper/Runtime 合同。
     */
    fun submit() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        val targetDate = _state.value.targetDate
        val generationId = "meal-${++generationCounter}"
        val request = StreamingMealRequest(
            segments = listOf(
                InputSegment(segmentId = "quick-$targetDate", targetDate = targetDate, inputText = text, ordinal = 0),
            ),
            generationId = generationId,
            weekAnchor = startOfWeek(targetDate),
        )
        val session = StreamingMealSession(request)

        // 新 generation 前取消旧 Job（旧会话后到事件不再喂入）
        generationJob?.cancel()
        lastPreviewDays = null
        _state.update {
            it.copy(
                phase = AiMealPhase.GENERATING,
                generationId = generationId,
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
                val llmRequest = AiMealPrompt.buildStreamingRequest(listOf(seg))
                // 顺序收集当前段流；流结束（Completed/Failed 后 close）返回。
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
                            handleSessionSnapshot(session, generationId, isFinal = false)
                        }
                    }
                }
                if (_state.value.generationId != generationId) return@launch
                // AF-B3-02: 流返回但当前段仍未终态（STREAMING）→ 记为异常结束。
                if (session.currentSegmentId() == seg.segmentId &&
                    _state.value.generationId == generationId) {
                    session.onFailed(seg.segmentId, "STREAM_ENDED_WITHOUT_TERMINAL")
                    handleSessionSnapshot(session, generationId, isFinal = false)
                }
                segment = session.nextSegment()
            }
            // 全部段终态：最终重算 preview 或进入 ERROR
            if (_state.value.generationId == generationId) {
                handleSessionSnapshot(session, generationId, isFinal = true)
            }
        }
    }

    /**
     * B3: 将 session 快照落为 UI 状态。[AI修改]
     *
     * - 无合法餐食且非 final：仅更新进度/诊断，不调 preview。
     * - 有合法餐食：PARTIAL_READY（生成中）或 PREVIEW_READY（final）；相同 days 不重复 preview。
     * - 无合法餐食且 final：ERROR，不自动调用规则 parser。
     */
    private suspend fun handleSessionSnapshot(session: StreamingMealSession, generationId: String, isFinal: Boolean) {
        // AF-B3-R2-01: 唯一 generation 谓词。
        if (!isCurrentGeneration(generationId)) return
        val snap = session.snapshot()
        if (!snap.hasValidMeals && !isFinal) {
            _state.update {
                it.copy(segmentStates = snap.segmentStates, isGenerating = true)
            }
            return
        }
        if (!snap.hasValidMeals) {
            _state.update {
                it.copy(
                    phase = AiMealPhase.ERROR,
                    errorMessage = "没能识别出菜品，试试更具体的描述？\n如「中午吃了红烧肉和米饭」",
                    segmentStates = snap.segmentStates,
                    isGenerating = false,
                )
            }
            return
        }
        // 相同 days 不重复调用 previewAll
        if (!isFinal && lastPreviewDays == snap.days && _state.value.autoGenPreview != null) {
            _state.update {
                it.copy(phase = AiMealPhase.PARTIAL_READY, segmentStates = snap.segmentStates, isGenerating = true)
            }
            return
        }
        // AF-B3-03: preview date 取 session/request 冻结日期，不读可变 UI date。
        val frozenDate = session.request.segments.firstOrNull()?.targetDate ?: _state.value.targetDate
        try {
            val preview = sessionPort.preview(snap.days, frozenDate)
            // AF-B3-R3-01: preview 挂起后先比对 generation，旧 A 直接返回（不跑健康摘要）。
            if (!isCurrentGeneration(generationId)) return
            // AF-B3-R3-02: 健康摘要 NonCancellable 避免保存取消 job 时 IO fatal；恢复后再次比对。
            val safetyReport = withContext(kotlinx.coroutines.NonCancellable) {
                buildHealthSafetyReport(preview)
            }
            if (!isCurrentGeneration(generationId)) return
            lastPreviewDays = snap.days
            _state.update {
                it.copy(
                    phase = if (isFinal) AiMealPhase.PREVIEW_READY else AiMealPhase.PARTIAL_READY,
                    autoGenPreview = preview,
                    segmentStates = snap.segmentStates,
                    isGenerating = !isFinal,
                    parseWarnings = snap.diagnostics.map { it.message },
                    mergeConfirmationRequired = preview.days.any { it.hasExisting },
                    mergeConfirmed = false,
                    healthSafetyReport = safetyReport,
                )
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            if (!isCurrentGeneration(generationId)) return
            com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "preview failed: ${e.message}", e)
            _state.update {
                it.copy(
                    phase = AiMealPhase.ERROR,
                    errorMessage = "预览生成失败：${e.message ?: "未知错误"}",
                    isGenerating = false,
                )
            }
        }
    }

    /** AF-B3-R2-01: 当前 generation 谓词。 */
    private fun isCurrentGeneration(generationId: String): Boolean =
        _state.value.generationId == generationId

    /**
     * B3: 唯一允许调用 RuleMealParser 的显式动作。[AI修改]
     *
     * 仅当 phase=ERROR 且当前 generation 无合法餐食时执行；产物标记"规则解析"。
     * B5 再接可见按钮，B3 不新增 UI。
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
                com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "rule fallback failed: ${e.message}", e)
                _state.update {
                    it.copy(phase = AiMealPhase.ERROR, errorMessage = "规则解析失败：${e.message ?: "未知错误"}")
                }
            }
        }
    }

    /** 目标日期所在周的周一。 */
    private fun startOfWeek(date: LocalDate): LocalDate {
        val mondayOffset = date.dayOfWeek.ordinal // Mon=0
        return if (mondayOffset == 0) date else date.minus(DatePeriod(days = mondayOffset))
    }

    /** 只使用本地档案和预览事实；不调用云端、不阻断真实记录。 [AI生成] */
    private suspend fun buildHealthSafetyReport(preview: AutoGenPreview): HealthSafetyReport {
        val enabled = healthSummaryLabels()
        val pendingIngredients = preview.days.flatMap { it.meals }.flatMap { it.dishes }
            .flatMap { it.ingredients }.count { it.careFlag == com.sxdbsm.cookbook.domain.autogen.CareFlag.PENDING_REVIEW }
        return HealthSafetyReport(buildList {
            if (enabled.isNotEmpty()) add("已结合健康档案：${enabled.joinToString("、")}")
            if (pendingIngredients > 0) add("本餐有 $pendingIngredients 种新食材，营养和适宜性待复核")
            if (isEmpty()) add("未设置健康档案；可按个人情况核对本餐")
        })
    }

    fun requestHealthAdvice() {
        if (_state.value.autoGenPreview == null || _state.value.healthAdviceLoading) return
        _state.update { it.copy(healthAdviceConsentPending = true, healthAdviceError = null) }
    }

    fun declineHealthAdvice() = _state.update { it.copy(healthAdviceConsentPending = false) }

    fun confirmHealthAdvice() {
        val preview = _state.value.autoGenPreview ?: return
        _state.update { it.copy(healthAdviceConsentPending = false, healthAdviceLoading = true, healthAdviceError = null) }
        viewModelScope.launch {
            val healthSummary = healthSummaryLabels().ifEmpty { listOf("未设置具体健康档案") }.joinToString("、")
            val mealSummary = preview.days.flatMap { it.meals }.flatMap { it.dishes }
                .joinToString("、") { it.inputName }.take(500)
            val result = aiRuntime.complete(AiMealHealthAdvice.request(healthSummary, mealSummary))
            _state.update {
                it.copy(
                    healthAdviceLoading = false,
                    healthAdvice = result.getOrNull()?.trim()?.takeIf(String::isNotBlank),
                    healthAdviceError = result.exceptionOrNull()?.message?.take(120)
                        ?: if (result.getOrNull().isNullOrBlank()) "暂时无法生成建议" else null,
                )
            }
        }
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

    /** 解析文本 → DayMealJson 列表（AI 优先·扁平格式→规则兜底）。[AI修改] 接入 FlatToDayMealConverter + 诊断日志 */
    private data class ParsedDays(val days: List<DayMealJson>, val warnings: List<String> = emptyList())

    private suspend fun parseToDayMealJsonList(text: String): ParsedDays {
        // 先尝试 AI 解析
        val aiResult: AiMealParser.ParseOutcome? = try {
            if (config.isModelReady()) parseWithAi(text) else {
                com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI model not ready, fallback to rule")
                null
            }
        } catch (e: Exception) {
            com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "AI parse failed: ${e.message}", e)
            null
        }

        if (aiResult != null && aiResult.isValid) {
            val anchorResult = MealDateAnchorPolicy.apply(text, _state.value.targetDate, aiResult.days)
            val aiDays = anchorResult.days
            _state.update { it.copy(parseSourceMessage = "本次结果：AI 解析") }
            com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput",
                "AI parsed ${aiDays.size} day(s), ${aiDays.sumOf { it.meals.size }} meal(s), ${aiDays.sumOf { it.meals.sumOf { m -> m.dishes.size } }} dish(es)")
            // [AI修改] AI 的完整日期已在共享解析层锚定，不能再被 weekday 规则覆盖。
            return ParsedDays(
                aiDays,
                (aiResult.warnings.filterNot { it.contains("未给出日期") } + listOfNotNull(anchorResult.warning)).distinct(),
            )
        }

        // 规则兜底
        val fallbackReason = if (config.isModelReady()) "AI 响应无有效结果" else "AI 未配置或不可用"
        // [AI修改] AI 降级不能静默，否则用户无法判断预览的可靠性与修正方向。
        _state.update { it.copy(parseSourceMessage = "本次结果：规则解析（$fallbackReason）") }
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI parse returned null/empty, fallback to RuleMealParser")
        val names = ingredientRepo.allActiveNames()
        val ruleDays = RuleMealParser.parse(text, names, today = _state.value.targetDate)
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput",
            "RuleMealParser: ${ruleDays.size} day(s), ${ruleDays.sumOf { it.meals.size }} meal(s), ${ruleDays.sumOf { it.meals.sumOf { m -> m.dishes.size } }} dish(es)")
        val anchorResult = MealDateAnchorPolicy.apply(text, _state.value.targetDate, ruleDays)
        return ParsedDays(anchorResult.days, listOfNotNull(anchorResult.warning))
    }

    /** AI 解析·返回 DayMealJson 列表。[AI修改] 改用 parseToDayMealJsonList() 直接产出统一格式 */
    private suspend fun parseWithAi(text: String): AiMealParser.ParseOutcome? {
        val today = _state.value.targetDate
        val now = DateTime.nowTime()
        val weekday = weekdayChinese(today.dayOfWeek)
        val request = AiMealPrompt.buildRequest(
            userInput = text,
            today = DateTime.formatDate(today),
            weekday = weekday,
            nowTime = DateTime.formatTime(now),
        )
        com.sxdbsm.cookbook.android.util.AppLogger.i(
            "AiMealInput",
            "AI meal request: targetDate=$today weekday=$weekday inputLength=${text.length} maxTokens=${request.maxTokens}",
        )
        com.sxdbsm.cookbook.android.util.AppLogger.debugLong("AiMealRaw", "mealInput", text)
        com.sxdbsm.cookbook.android.util.AppLogger.debugLong("AiMealRaw", "systemPrompt", request.system)
        com.sxdbsm.cookbook.android.util.AppLogger.debugLong("AiMealRaw", "userPrompt", request.user)
        val response = aiRuntime.complete(request)
        val rawText = response.getOrNull() ?: run {
            val message = response.exceptionOrNull()?.message ?: "请求未返回结果"
            _state.update { it.copy(diagnostic = AiMealAttemptDiagnostic("请求", message)) }
            com.sxdbsm.cookbook.android.util.AppLogger.w("AiMealInput", "AI complete returned error: $message")
            return null
        }
        // [AI修改] 饮食语义与模型响应均属敏感内容，日志仅保留结构化长度诊断。
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI response received, length=${rawText.length}")

        com.sxdbsm.cookbook.android.util.AppLogger.debugLong("AiMealRaw", "modelContent", rawText)
        val outcome = AiMealParser.parseOutcome(rawText, today)
        if (!outcome.isValid) {
            com.sxdbsm.cookbook.android.util.AppLogger.debugLong("AiMealRaw", "parseErrors", outcome.errors.joinToString("\n").ifBlank { "<none>" })
            com.sxdbsm.cookbook.android.util.AppLogger.debugLong("AiMealRaw", "parseWarnings", outcome.warnings.joinToString("\n").ifBlank { "<none>" })
            val summary = outcome.errors.joinToString("；").ifBlank { "AI 返回不符合餐食结构" }
            _state.update {
                it.copy(diagnostic = AiMealAttemptDiagnostic("结构化校验", summary, rawText.length, rawText))
            }
            com.sxdbsm.cookbook.android.util.AppLogger.w("AiMealInput", "AI 结构化结果无效：$summary")
            return null
        }
        com.sxdbsm.cookbook.android.util.AppLogger.debugLong(
            "AiMealRaw",
            "normalizedDays",
            outcome.days.joinToString("\n") { day ->
                "${day.date} ${day.meals.joinToString { meal -> "${meal.meal_type}:${meal.dishes.joinToString { dish -> dish.name }}" }}"
            },
        )
        return outcome
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
        generationJob = null
        _state.update {
            it.copy(
                phase = AiMealPhase.SAVING,
                generationId = null,
                isGenerating = false,
            )
        }
        lastPreviewDays = null

        viewModelScope.launch {
            try {
                val result = sessionPort.commit(frozenPreview)
                _state.update {
                    it.copy(
                        phase = AiMealPhase.DONE,
                        autoGenResult = result,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        phase = AiMealPhase.ERROR,
                        errorMessage = "保存失败：${e.message ?: "未知错误"}",
                    )
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

    /** 设置目标日期（预览页调整）。[AI修改] B3.1 AF-B3-03: 日期变更取消进行中 generation。 */
    fun setTargetDate(date: LocalDate) {
        invalidateGenerationToInput(_state.value.inputText, date)
    }

    /** 星期几→中文。[AI生成] */
    private fun weekdayChinese(day: DayOfWeek): String = when (day) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        DayOfWeek.SUNDAY -> "周日"
        else -> ""
    }
}
