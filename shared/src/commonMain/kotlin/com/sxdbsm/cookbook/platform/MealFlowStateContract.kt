package com.sxdbsm.cookbook.platform

/** 五类餐食子流程共用的状态生命周期合同。[AI生成] */
enum class MealFlow(val code: String) {
    AI_RECOMMEND("ai_recommend"),
    FOOD_SEARCH("food_search"),
    INVENTORY_SELECT("inventory_select"),
    NEW_DISH("new_dish"),
    EDIT_MEAL("edit_meal"),
}

enum class MealFlowContractEvent {
    SAVE_STATE,
    RESTORE_STATE,
    MERGE_RESULT,
}

data class MealFlowContractResult(
    val flow: MealFlow,
    val observed: Set<MealFlowContractEvent>,
) {
    val missing: Set<MealFlowContractEvent> =
        MealFlowContractEvent.entries.toSet() - observed
    val complete: Boolean get() = missing.isEmpty()
}

/**
 * 将现有结构化生命周期事件映射为产品层合同：进入 SAVE，返回 RESTORE，结果 MERGE。[AI生成]
 */
object MealFlowStateContract {
    val flows: Set<MealFlow> = MealFlow.entries.toSet()

    fun validate(flow: MealFlow, events: List<StructuredLogEvent>): MealFlowContractResult {
        val observed = events.mapNotNull { event ->
            when (event) {
                is StructuredLogEvent.StateLifecycle -> when (event.event) {
                    "state.snapshot.before_navigation" -> MealFlowContractEvent.SAVE_STATE
                    "state.restore" -> MealFlowContractEvent.RESTORE_STATE
                    "state.merge.result" -> MealFlowContractEvent.MERGE_RESULT
                    else -> null
                }
                else -> null
            }
        }.toSet()
        return MealFlowContractResult(flow, observed)
    }
}
