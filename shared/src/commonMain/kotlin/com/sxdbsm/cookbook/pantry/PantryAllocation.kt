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
    val date: String = "", // [AI生成] 餐食日期(yyyy-MM-dd)，用于「只今天及未来标缺料、历史不标」
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
    /**
     * 计算缺料。[AI修改]
     *
     * 调用方需先把 `usagesChrono` 过滤为**入库日起**的餐(date >= 该食材 added_at)，按时间序排好。
     * 入库日到昨天的餐**占用份数(占 rank)但不显示缺料**(已吃过)；只对 `date >= onlyFromDate`(今天)的餐发出缺料标记。
     *
     * @param onlyFromDate 仅对 date>=此值的餐发出缺料标记(通常=今天)；空串=全部都标。
     */
    fun shortages(
        servingCounts: Map<Long, Int>,
        usagesChrono: List<PantryUsage>,
        onlyFromDate: String = "",
    ): Map<MealDishKey, List<String>> {
        val result = LinkedHashMap<MealDishKey, MutableList<String>>()
        var currentIngredient = Long.MIN_VALUE
        var rank = 0
        for (u in usagesChrono) {
            if (u.ingredientId != currentIngredient) {
                currentIngredient = u.ingredientId
                rank = 0
            }
            rank++ // 该食材第 rank 次使用(1起)；入库日起的历史餐也占 rank(消耗份数)
            val stock = servingCounts[u.ingredientId] ?: 0
            if (rank > stock && (onlyFromDate.isEmpty() || u.date >= onlyFromDate)) {
                result.getOrPut(MealDishKey(u.mealRecordId, u.dishId)) { mutableListOf() }.add(u.ingredientName)
            }
        }
        return result
    }

    /**
     * 计算每个在库食材的"剩余份数" = 份数 - 入库日起到今天已用掉的份数。[AI生成]
     *
     * @param addedDate 食材 id -> 入库日(yyyy-MM-dd)；@param usagesChrono 全部使用记录；@param today 今天日期
     */
    fun remaining(
        servingCounts: Map<Long, Int>,
        addedDate: Map<Long, String>,
        usagesChrono: List<PantryUsage>,
        today: String,
    ): Map<Long, Int> {
        val consumed = HashMap<Long, Int>()
        for (u in usagesChrono) {
            val added = addedDate[u.ingredientId] ?: continue
            // 入库日起、且不晚于今天(已发生)的餐才算已用掉
            if (u.date in added..today) consumed[u.ingredientId] = (consumed[u.ingredientId] ?: 0) + 1
        }
        return servingCounts.mapValues { (id, c) -> (c - (consumed[id] ?: 0)).coerceAtLeast(0) }
    }
}
