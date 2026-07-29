package com.sxdbsm.cookbook.android.ui.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.meallog.AiMealParseResult
import com.sxdbsm.cookbook.ai.meallog.AiMealParser
import com.sxdbsm.cookbook.ai.meallog.AiMealPrompt
import com.sxdbsm.cookbook.ai.meallog.AiMealRecorder
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/**
 * @File : AiMealInputViewModel
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AI 快捷输入记餐 ViewModel
 * <p>
 * 状态机：输入 → 解析中 → 预览 → 保存中 → 完成/错误。
 * 复用 SwitchableAiRuntime 调 AI；失败走本地规则兜底。
 * <p>
 * [AI生成] K1 AI快捷输入记餐：ViewModel 层。
 **/

/** 输入模式。[AI生成] */
enum class InputMode { TEXT, VOICE }

/** 处理阶段。[AI生成] */
enum class AiMealPhase { INPUT, PARSING, PREVIEW, SAVING, DONE, ERROR }

/** 语音识别状态。[AI生成] */
enum class VoiceState { IDLE, LISTENING, PROCESSING, ERROR }

/** UI 状态。[AI生成] */
data class AiMealInputUiState(
    val inputText: String = "",
    val inputMode: InputMode = InputMode.TEXT,
    val phase: AiMealPhase = AiMealPhase.INPUT,
    val parsedResult: AiMealParseResult? = null,
    val recordResult: AiMealRecorder.RecordResult? = null,
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
    private val aiRuntime: AiRuntime,
    private val config: AiRuntimeConfig,
    private val recorder: AiMealRecorder,
    private val ingredientRepo: IngredientRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AiMealInputUiState(inputText = initialText)
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

    /** 发送输入进行解析。[AI修改] K2：AI优先→新RuleMealParser兜底 */
    fun submit() {
        val text = _state.value.inputText.trim()
        if (text.isBlank()) return

        _state.update { it.copy(phase = AiMealPhase.PARSING, errorMessage = null) }

        viewModelScope.launch {
            // 先尝试 AI 解析
            val aiResult: AiMealParseResult? = try {
                if (config.isModelReady()) {
                    parseWithAi(text)
                } else null
            } catch (e: Exception) {
                com.sxdbsm.cookbook.android.util.AppLogger.e("AiMealInput", "AI parse failed: ${e.message}", e)
                null
            }

            // [AI修改] K2：AI失败→新RuleMealParser兜底（替代旧localFallback）
            val parsed: AiMealParseResult = if (aiResult != null && aiResult.meals.isNotEmpty() && aiResult.meals.any { it.dishes.isNotEmpty() }) {
                aiResult
            } else {
                // 用 RuleMealParser 解析，再转为旧 Schema（保持预览 UI 兼容）
                val names = ingredientRepo.allActiveNames()
                val dayMeals = com.sxdbsm.cookbook.ai.meallog.RuleMealParser.parse(text, names)
                val firstDay = dayMeals.firstOrNull()
                if (firstDay != null) {
                    com.sxdbsm.cookbook.ai.meallog.SchemaMigration.toAiMealParseResult(firstDay)
                } else {
                    AiMealParseResult()
                }
            }

            if (parsed.meals.isEmpty() || parsed.meals.all { it.dishes.isEmpty() }) {
                _state.update {
                    it.copy(
                        phase = AiMealPhase.ERROR,
                        errorMessage = "没能识别出菜品，试试更具体的描述？\n如「中午吃了红烧肉和米饭」",
                    )
                }
            } else {
                _state.update {
                    it.copy(
                        phase = AiMealPhase.PREVIEW,
                        parsedResult = parsed,
                        targetDate = resolveTargetDate(parsed.date_offset),
                    )
                }
            }
        }
    }

    /** AI 解析。[AI生成] */
    private suspend fun parseWithAi(text: String): AiMealParseResult? {
        val today = DateTime.today()
        val now = DateTime.nowTime()
        val weekday = weekdayChinese(today.dayOfWeek)
        val request = AiMealPrompt.buildRequest(
            userInput = text,
            today = DateTime.formatDate(today),
            weekday = weekday,
            nowTime = DateTime.formatTime(now),
        )
        val response = aiRuntime.complete(request)
        return response.getOrNull()?.let { AiMealParser.parse(it) }
    }

    /** 确认保存。[AI生成] */
    fun confirmSave() {
        val parsed = _state.value.parsedResult ?: return
        _state.update { it.copy(phase = AiMealPhase.SAVING) }

        viewModelScope.launch {
            try {
                val names = ingredientRepo.allActiveNames()
                val result = recorder.record(
                    parsed = parsed,
                    today = DateTime.today(),
                    ingredientNames = names,
                )
                _state.update {
                    it.copy(
                        phase = AiMealPhase.DONE,
                        recordResult = result,
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

    /** 偏移 → 日期。[AI生成] */
    private fun resolveTargetDate(offset: Int): LocalDate {
        val today = DateTime.today()
        return when (offset) {
            -2 -> today.plus(DatePeriod(days = -2))
            -1 -> today.plus(DatePeriod(days = -1))
            1 -> today.plus(DatePeriod(days = 1))
            else -> today
        }
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
