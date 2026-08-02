package com.sxdbsm.cookbook.android.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.meallog.AiMealParseResult
import com.sxdbsm.cookbook.ai.meallog.AiMealParser
import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.ai.meallog.DayMealJson
import com.sxdbsm.cookbook.ai.meallog.MultiDayRecorder
import com.sxdbsm.cookbook.ai.meallog.RuleMealParser
import com.sxdbsm.cookbook.ai.meallog.SchemaMigration
import com.sxdbsm.cookbook.data.repository.IngredientRepository
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
)

class AiMealInputViewModel(
    private val initialText: String,
    targetDate: LocalDate,  // [AI修改] 从食历当前日期传入，替代硬编码 _state.value.targetDate
    private val aiRuntime: AiRuntime,
    private val config: AiRuntimeConfig,
    private val recorder: MultiDayRecorder,
    private val ingredientRepo: IngredientRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiMealInputUiState(inputText = initialText, targetDate = targetDate)
    )
    val state: StateFlow<AiMealInputUiState> = _state.asStateFlow()

    /** 设置输入文本。[AI生成] */
    fun setInputText(text: String) {
        _state.update { it.copy(inputText = text, phase = AiMealPhase.INPUT, errorMessage = null) }
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

        _state.update { it.copy(phase = AiMealPhase.PARSING, errorMessage = null) }

        viewModelScope.launch {
            // Step 1: 解析 → DayMealJson 列表
            val days = try {
                parseToDayMealJsonList(text)
            } catch (e: Exception) {
                com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "parse failed: ${e.message}", e)
                emptyList()
            }

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

    /** 解析文本 → DayMealJson 列表（AI 优先·扁平格式→规则兜底）。[AI修改] 接入 FlatToDayMealConverter + 诊断日志 */
    private suspend fun parseToDayMealJsonList(text: String): List<com.sxdbsm.cookbook.ai.meallog.DayMealJson> {
        // 先尝试 AI 解析
        val aiDays: List<DayMealJson>? = try {
            if (config.isModelReady()) parseWithAi(text) else {
                com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI model not ready, fallback to rule")
                null
            }
        } catch (e: Exception) {
            com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "AI parse failed: ${e.message}", e)
            null
        }

        if (aiDays != null && aiDays.isNotEmpty()) {
            com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput",
                "AI parsed ${aiDays.size} day(s), ${aiDays.sumOf { it.meals.size }} meal(s), ${aiDays.sumOf { it.meals.sumOf { m -> m.dishes.size } }} dish(es)")
            return aiDays
        }

        // 规则兜底
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI parse returned null/empty, fallback to RuleMealParser")
        val names = ingredientRepo.allActiveNames()
        val ruleDays = RuleMealParser.parse(text, names, today = _state.value.targetDate)
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput",
            "RuleMealParser: ${ruleDays.size} day(s), ${ruleDays.sumOf { it.meals.size }} meal(s), ${ruleDays.sumOf { it.meals.sumOf { m -> m.dishes.size } }} dish(es)")
        return ruleDays
    }

    /** AI 解析·返回 DayMealJson 列表。[AI修改] 改用 parseToDayMealJsonList() 直接产出统一格式 */
    private suspend fun parseWithAi(text: String): List<DayMealJson>? {
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
            com.sxdbsm.cookbook.android.util.AppLogger.w("AiMealInput", "AI complete returned error")
            return null
        }
        // 截断日志（防止超长 JSON 撑爆 logcat）
        val preview = if (rawText.length > 300) rawText.take(300) + "…" else rawText
        com.sxdbsm.cookbook.android.util.AppLogger.d("AiMealInput", "AI raw response: $preview")

        val days = AiMealParser.parseToDayMealJsonList(rawText)
        if (days == null) {
            com.sxdbsm.cookbook.android.util.AppLogger.w("AiMealInput", "AiMealParser.parseToDayMealJsonList returned null (flat & legacy both failed)")
        }
        return days
    }

    /**
     * 确认保存。[AI修改] P2-1 K1a：读 autoGenPreview 调 commitPreview()。
     */
    fun confirmSave() {
        val preview = _state.value.autoGenPreview ?: return
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
