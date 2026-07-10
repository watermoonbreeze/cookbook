package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.ShoppingItem
import com.sxdbsm.cookbook.domain.model.ShoppingReason
import com.sxdbsm.cookbook.pantry.PantryAllocation
import com.sxdbsm.cookbook.pantry.PantryUsage
import com.sxdbsm.cookbook.platform.ioDispatcher
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

/**
 * @File : ShoppingListRepository
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 采购清单聚合仓库
 * <p>
 * 聚合"今天及未来"所有餐食里需采购(主料未入库)/缺料(库存份数不足)的食材成一张去重购物清单。
 * 采购/缺料判定复用与食历卡片相同的口径(selectMainIngredientUsageFromDate / PantryAllocation.shortages)，
 * 但 today 由参数传入，保证可控可测且与卡片显示一致。
 * <p>
 * [AI生成] 待办"采购清单聚合"。
 **/
class ShoppingListRepository(
    private val db: CookbookDatabase,
) {
    private val q = db.cookbookQueries

    /**
     * 聚合今天及未来餐食的采购/缺料食材成购物清单。[AI生成]
     *
     * @param today 当天日期（由平台传入，避免 shared 直接取系统时钟；也便于测试）。
     * @return 去重后的购物项；采购优先在前、涉及餐次多的在前、再按名排序。
     */
    suspend fun aggregate(today: LocalDate): List<ShoppingItem> = withContext(ioDispatcher) {
        val todayStr = DateTime.formatDate(today)
        val acc = LinkedHashMap<String, Acc>()

        val stock = q.selectPantryStock().executeAsList()
        val servings = stock.associate { it.ingredient_id to it.serving_count.toInt() }
        val addedDate = stock.associate { it.ingredient_id to DateTime.epochSecondsToDate(it.added_at) }
        val pantryIds = servings.keys

        // 采购：今天及未来餐食里主料不在库存。
        q.selectMainIngredientUsageFromDate(todayStr).executeAsList().forEach { r ->
            if (r.ingredient_id !in pantryIds) {
                acc.getOrPut(r.ingredient_name) { Acc(r.ingredient_name) }.mark(purchase = true, date = r.meal_date)
            }
        }

        // 缺料：在库但从入库日起、今天及未来的餐食份数不足。
        if (pantryIds.isNotEmpty()) {
            val usages = q.selectPantryUsageChrono().executeAsList()
                .map { PantryUsage(it.ingredient_id, it.ingredient_name, it.meal_record_id, it.dish_id, it.meal_date) }
                .filter { u -> addedDate[u.ingredientId]?.let { u.date >= it } == true }
            val shortage = PantryAllocation.shortages(servings, usages, onlyFromDate = todayStr)
            shortage.forEach { (key, names) ->
                names.forEach { name ->
                    val date = usages.firstOrNull {
                        it.mealRecordId == key.mealRecordId && it.dishId == key.dishId && it.ingredientName == name
                    }?.date ?: todayStr
                    acc.getOrPut(name) { Acc(name) }.mark(purchase = false, date = date)
                }
            }
        }

        acc.values
            .map { it.toItem(resolveId(it.name)) }
            .sortedWith(
                compareByDescending<ShoppingItem> { it.reason == ShoppingReason.PURCHASE }
                    .thenByDescending { it.mealCount }
                    .thenBy { it.ingredientName },
            )
    }

    private fun resolveId(name: String): Long? = q.selectActiveIngredientIdByName(name).executeAsOneOrNull()

    /** 单个食材的聚合累积。[AI生成] */
    private class Acc(val name: String) {
        private var anyPurchase = false // 只要有一处主料未入库就算采购（食材是否在库是全局的）
        private var mealCount = 0
        private val dateSet = LinkedHashSet<String>()

        fun mark(purchase: Boolean, date: String) {
            if (purchase) anyPurchase = true
            mealCount++
            if (date.isNotBlank()) dateSet.add(date)
        }

        fun toItem(id: Long?): ShoppingItem = ShoppingItem(
            ingredientId = id,
            ingredientName = name,
            reason = if (anyPurchase) ShoppingReason.PURCHASE else ShoppingReason.SHORTAGE,
            mealCount = mealCount,
            dates = dateSet.sorted(), // yyyy-MM-dd 字符串排序即时间序
        )
    }
}
