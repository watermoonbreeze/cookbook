package com.sxdbsm.cookbook.android.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.meallog.AiMealParseResult
import com.sxdbsm.cookbook.ai.meallog.AiMealParser
import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.ai.meallog.AiMealHealthAdvice
import com.sxdbsm.cookbook.ai.meallog.DayMealJson
import com.sxdbsm.cookbook.ai.meallog.MultiDayRecorder
import com.sxdbsm.cookbook.ai.meallog.RuleMealParser
import com.sxdbsm.cookbook.ai.meallog.SchemaMigration
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.domain.autogen.AutoGenPreview
import com.sxdbsm.cookbook.domain.autogen.AutoGenResult
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
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

/** 输入模式。[AI生成] */
enum class InputMode { TEXT, VOICE }

/** 处理阶段。[AI生成] */
enum class AiMealPhase { INPUT, PARSING, PREVIEW, SAVING, DONE, ERROR }

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

/** UI 状态。[AI修改] P2-1 K1a：autoGenPreview 作两阶段主键，PreviewPhase 直接渲染能力层产出。 */
data class AiMealInputUiState(
    val inputText: String = "",
    val inputMode: InputMode = InputMode.TEXT,
    val phase: AiMealPhase = AiMealPhase.INPUT,
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

    /** 设置输入文本。[AI生成] */
    fun setInputText(text: String) {
        _state.update { it.copy(inputText = text, phase = AiMealPhase.INPUT, errorMessage = null, diagnostic = null) }
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

    /** [AI修改] K2 语音识别完成：将识别结果追加到输入框 */
    fun onVoiceResult(text: String) {
        _state.update {
            val current = it.inputText.trimEnd()
            val appended = if (current.isBlank()) text else "$current $text"
            it.copy(
                inputText = appended,
                voiceState = VoiceState.IDLE,
                voiceActive = false,
                voiceError = null,
            )
        }
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

    /** [AI修改] K2 追加文本到输入框（粘贴用） */
    fun appendText(text: String) {
        _state.update {
            val current = it.inputText.trimEnd()
            val appended = if (current.isBlank()) text else "$current\n$text"
            it.copy(inputText = appended, phase = AiMealPhase.INPUT, errorMessage = null)
        }
    }

    /**
     * 发送输入进行解析并产出预览。[AI修改] P2-1 K1a：解析后调 previewAll()，存 autoGenPreview。
     *
     * 流程：AI/规则解析 → DayMealJson 列表 → previewAll(含营养估算) → PREVIEW 态。
     */
    fun submit() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        _state.update { it.copy(phase = AiMealPhase.PARSING, errorMessage = null, diagnostic = null) }

        viewModelScope.launch {
            // Step 1: 解析 → DayMealJson 列表
            val parsed = try {
                parseToDayMealJsonList(text)
            } catch (e: Exception) {
                com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "parse failed: ${e.message}", e)
                ParsedDays(emptyList())
            }
            val days = parsed.days

            if (days.isEmpty() || days.all { day -> day.meals.isEmpty() || day.meals.all { it.dishes.isEmpty() } }) {
                _state.update {
                    it.copy(
                        phase = AiMealPhase.ERROR,
                        errorMessage = "没能识别出菜品，试试更具体的描述？\n如「中午吃了红烧肉和米饭」",
                    )
                }
                return@launch
            }

            // Step 2: preview（含营养估算）
            val today = _state.value.targetDate
            try {
                val preview = recorder.previewAll(days, today)
                if (preview.days.isEmpty()) {
                    _state.update {
                        it.copy(
                            phase = AiMealPhase.ERROR,
                            errorMessage = "没能识别出可记录的菜品，请重新描述",
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            phase = AiMealPhase.PREVIEW,
                            autoGenPreview = preview,
                            targetDate = preview.days.firstOrNull()?.date ?: today,
                            parseWarnings = (parsed.warnings + preview.warnings).distinct(),
                            mergeConfirmationRequired = preview.days.any { it.hasExisting },
                            mergeConfirmed = false,
                            healthSafetyReport = buildHealthSafetyReport(preview),
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        phase = AiMealPhase.ERROR,
                        errorMessage = "预览生成失败：${e.message ?: "未知错误"}",
                    )
                }
            }
        }
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
            val aiDays = aiResult.days
            _state.update { it.copy(parseSourceMessage = "本次结果：AI 解析") }
            com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput",
                "AI parsed ${aiDays.size} day(s), ${aiDays.sumOf { it.meals.size }} meal(s), ${aiDays.sumOf { it.meals.sumOf { m -> m.dishes.size } }} dish(es)")
            // [AI修改] AI 的完整日期已在共享解析层锚定，不能再被 weekday 规则覆盖。
            return ParsedDays(aiDays, aiResult.warnings)
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
        return ParsedDays(ruleDays)
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
        val response = aiRuntime.complete(request)
        val rawText = response.getOrNull() ?: run {
            val message = response.exceptionOrNull()?.message ?: "请求未返回结果"
            _state.update { it.copy(diagnostic = AiMealAttemptDiagnostic("请求", message)) }
            com.sxdbsm.cookbook.android.util.AppLogger.w("AiMealInput", "AI complete returned error: $message")
            return null
        }
        // [AI修改] 饮食语义与模型响应均属敏感内容，日志仅保留结构化长度诊断。
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI response received, length=${rawText.length}")

        val outcome = AiMealParser.parseOutcome(rawText, today)
        if (!outcome.isValid) {
            val summary = outcome.errors.joinToString("；").ifBlank { "AI 返回不符合餐食结构" }
            _state.update {
                it.copy(diagnostic = AiMealAttemptDiagnostic("结构化校验", summary, rawText.length, rawText))
            }
            com.sxdbsm.cookbook.android.util.AppLogger.w("AiMealInput", "AI 结构化结果无效：$summary")
            return null
        }
        return outcome
    }

    /**
     * 确认保存。[AI修改] P2-1 K1a：读 autoGenPreview 调 commitPreview()。
     */
    fun confirmSave() {
        val preview = _state.value.autoGenPreview ?: return
        if (_state.value.mergeConfirmationRequired && !_state.value.mergeConfirmed) {
            _state.update { it.copy(mergeConfirmed = true) }
            return
        }
        _state.update { it.copy(phase = AiMealPhase.SAVING) }

        viewModelScope.launch {
            try {
                val result = recorder.commitPreview(preview)
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

    /** 重新输入。[AI生成] */
    fun reset() {
        _state.update {
            AiMealInputUiState(inputText = it.inputText) // 保留用户文字
        }
    }

    /** 从错误恢复。[AI生成] */
    fun dismissError() {
        _state.update {
            it.copy(phase = AiMealPhase.INPUT, errorMessage = null)
        }
    }

    /** 设置目标日期（预览页调整）。[AI生成] */
    fun setTargetDate(date: LocalDate) {
        _state.update { it.copy(targetDate = date) }
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
