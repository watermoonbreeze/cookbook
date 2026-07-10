package com.sxdbsm.cookbook.pantry

/**
 * @File : PantryAllocation
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 库存份数「派生占用/不足」计算（纯逻辑，可单测）
 * <p>
 * 不落库、实时计算：把所有餐食(含计划)里用到某在库食材的菜按时间顺序排队，
 * 前 `份数` 个够、其余「不足」。加份数后自动重算，早的餐先恢复(输入按时间序即可)。
 * 每道菜按 1 份算。
 * <p>
 * [AI生成] Req: 库存份数体系——不足派生显示、加份数按时间序恢复。
 **/

/** 一次「菜品用到某在库食材」的记录（已按时间顺序排列）。[AI生成] */
data class PantryUsage(
    val ingredientId: Long,
    val ingredientName: String,
    val mealRecordId: Long,
    val dishId: Long,
)

/** 一道菜实例（餐次+菜）的标识。[AI生成] */
data class MealDishKey(val mealRecordId: Long, val dishId: Long)

object PantryAllocation {
    /**
     * 计算每道菜的缺料食材名。[AI生成]
     *
     * @param servingCounts 在手食材份数(ingredient_id -> 份数)
     * @param usagesChrono 所有在库食材使用记录，**须按(食材, 时间顺序)排好序**
     * @return (餐次,菜) -> 缺料食材名列表；不缺料的菜不出现在 map 中
     */
    fun shortages(
        servingCounts: Map<Long, Int>,
        usagesChrono: List<PantryUsage>,
    ): Map<MealDishKey, List<String>> {
        val result = LinkedHashMap<MealDishKey, MutableList<String>>()
        var currentIngredient = Long.MIN_VALUE
        var rank = 0
        for (u in usagesChrono) {
            if (u.ingredientId != currentIngredient) {
                currentIngredient = u.ingredientId
                rank = 0
            }
            rank++ // 该食材的第 rank 次使用（1 起）
            val stock = servingCounts[u.ingredientId] ?: 0
            if (rank > stock) {
                // 超出份数 → 这道菜在该食材上不足
                result.getOrPut(MealDishKey(u.mealRecordId, u.dishId)) { mutableListOf() }.add(u.ingredientName)
            }
        }
        return result
    }
}
