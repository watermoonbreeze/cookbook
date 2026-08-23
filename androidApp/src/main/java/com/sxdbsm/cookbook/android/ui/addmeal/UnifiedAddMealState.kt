package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/** 统一添加餐食页面的范围维度。[AI生成] UEN */
enum class MealRange { SINGLE_DAY, PERIOD }

/** 统一添加餐食页面的输入方式。[AI生成] UEN */
enum class MealInputMethod { AI, MANUAL }

/** 页面壳状态；业务草稿继续由各既有 ViewModel 持有。[AI生成] UEN */
data class UnifiedAddMealUiState(
    val range: MealRange = MealRange.SINGLE_DAY,
    val method: MealInputMethod = MealInputMethod.AI,
)

/**
 * 统一入口离开到子流程时保存的最小页面快照。[AI修改]
 *
 * 草稿内容仍由 AddMealViewModel/AiMealInputViewModel 持有；这里专门冻结入口选择器，
 * 避免子页面返回后重新创建组合导致 manual/AI 或单天/周期回到默认值。
 */
val UnifiedAddMealUiStateSaver: Saver<UnifiedAddMealUiState, Any> = listSaver<UnifiedAddMealUiState, String>(
    save = { state -> listOf(state.range.name, state.method.name) },
    restore = { saved ->
        UnifiedAddMealUiState(
            range = saved.getOrNull(0)?.let { runCatching { MealRange.valueOf(it) }.getOrNull() }
                ?: MealRange.SINGLE_DAY,
            method = saved.getOrNull(1)?.let { runCatching { MealInputMethod.valueOf(it) }.getOrNull() }
                ?: MealInputMethod.AI,
        )
    },
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
