package com.sxdbsm.cookbook.android.ui.addmeal

/** 统一添加餐食页面的范围维度。[AI生成] UEN */
enum class MealRange { SINGLE_DAY, PERIOD }

/** 统一添加餐食页面的输入方式。[AI生成] UEN */
enum class MealInputMethod { AI, MANUAL }

/** 页面壳状态；业务草稿继续由各既有 ViewModel 持有。[AI生成] UEN */
data class UnifiedAddMealUiState(
    val range: MealRange = MealRange.SINGLE_DAY,
    val method: MealInputMethod = MealInputMethod.AI,
)

/** 页面选择器事件；仅修改对应维度，供 UI 与单测共享。[AI生成] UEN */
sealed interface UnifiedAddMealEvent {
    data class SelectRange(val range: MealRange) : UnifiedAddMealEvent
    data class SelectMethod(val method: MealInputMethod) : UnifiedAddMealEvent
}

/** 统一入口状态纯函数迁移，保证切换一个维度不会覆盖另一个维度。[AI生成] UEN */
fun UnifiedAddMealUiState.reduce(event: UnifiedAddMealEvent): UnifiedAddMealUiState = when (event) {
    is UnifiedAddMealEvent.SelectRange -> copy(range = event.range)
    is UnifiedAddMealEvent.SelectMethod -> copy(method = event.method)
}

/** AI 保存完成后必须离开统一入口，避免 DONE 态被误判为未保存草稿。[AI修复] AF-UEN-02 */
fun shouldLeaveAfterAiSave(phase: com.sxdbsm.cookbook.android.ui.ai.AiMealPhase): Boolean =
    phase == com.sxdbsm.cookbook.android.ui.ai.AiMealPhase.DONE

/** [AI修改] AF-UEN-03：AI 会话未完成前锁定范围，避免预览与范围语义错配。 */
fun canChangeMealRange(phase: com.sxdbsm.cookbook.android.ui.ai.AiMealPhase): Boolean =
    phase == com.sxdbsm.cookbook.android.ui.ai.AiMealPhase.INPUT

/** [AI修改] AF-UEN-03：稳定预览/错误态允许仅切换 AI 与手动，保留会话。 */
fun canChangeMealMethod(phase: com.sxdbsm.cookbook.android.ui.ai.AiMealPhase): Boolean =
    phase == com.sxdbsm.cookbook.android.ui.ai.AiMealPhase.INPUT ||
        phase == com.sxdbsm.cookbook.android.ui.ai.AiMealPhase.PREVIEW_READY ||
        phase == com.sxdbsm.cookbook.android.ui.ai.AiMealPhase.ERROR
