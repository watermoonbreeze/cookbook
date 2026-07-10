package com.sxdbsm.cookbook.pantry

import com.sxdbsm.cookbook.ai.model.PeriodPlan

/**
 * @File : PantryPlanAnnotator
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 周期规划的库存「采购/缺料」标注（纯逻辑，可单测）
 * <p>
 * 按当前库存快照，对规划的每道菜**主料**判定：不在库→采购、在库但份数不够→缺料。
 * 按天序对在库主料分配剩余份数预算(份数-今天及过去占用)；只判主料(盐/油等调料不标)。
 * 规划本就「涉及后续采购」，故用当前剩余作预算、跨规划自身多天分配，超出即缺料。
 * <p>
 * [AI生成] Req: 周期规划-库存不足标缺料、库存没有标采购、样式同缺料。
 **/

/** 规划判定用的主料。[AI生成] */
data class PlanMainIngredient(val id: Long, val name: String)

object PantryPlanAnnotator {
    /**
     * 标注规划每道菜的采购/缺料主料。[AI生成]
     *
     * @param plan 规划
     * @param mainIngredientsByDish 菜 id -> 主料列表
     * @param pantryIds 在库食材 id(status=1)
     * @param remaining 剩余份数预算(份数-今天及过去占用)
     */
    fun annotate(
        plan: PeriodPlan,
        mainIngredientsByDish: Map<Long, List<PlanMainIngredient>>,
        pantryIds: Set<Long>,
        remaining: Map<Long, Int>,
    ): PeriodPlan {
        val budget = remaining.toMutableMap() // 跨规划多天按顺序占用
        val days = plan.days.map { day ->
            val meals = day.meals.map { meal ->
                val dishes = meal.dishes.map { dish ->
                    val mains = mainIngredientsByDish[dish.id].orEmpty()
                    val purchase = mutableListOf<String>()
                    val shortage = mutableListOf<String>()
                    for (m in mains) {
                        if (m.id !in pantryIds) {
                            purchase += m.name // 库存里没有 → 采购
                        } else {
                            val left = budget[m.id] ?: 0
                            if (left > 0) budget[m.id] = left - 1 // 够 → 占用一份
                            else shortage += m.name // 在库但份数用尽 → 缺料
                        }
                    }
                    dish.copy(purchaseNames = purchase.distinct(), shortageNames = shortage.distinct())
                }
                meal.copy(dishes = dishes)
            }
            day.copy(meals = meals)
        }
        return plan.copy(days = days)
    }
}
