package com.sxdbsm.cookbook.pantry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : PantryAllocationTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 库存份数派生占用/不足计算单测
 * <p>
 * [AI生成] 覆盖用户 7.4/7.5 例子：份数用尽后按时间序判不足、加份数早的先恢复。
 **/
class PantryAllocationTest {

    private val PORK = 1L
    private fun use(mealId: Long, dishId: Long, name: String = "猪肉", ing: Long = PORK) =
        PantryUsage(ingredientId = ing, ingredientName = name, mealRecordId = mealId, dishId = dishId)

    @Test
    fun `份数为0时全部不足`() {
        // 7.4(mr1) 与 7.5(mr2) 都用猪肉，库存 0 份
        val usages = listOf(use(1, 10), use(2, 20))
        val short = PantryAllocation.shortages(servingCounts = mapOf(PORK to 0), usagesChrono = usages)
        assertEquals(listOf("猪肉"), short[MealDishKey(1, 10)])
        assertEquals(listOf("猪肉"), short[MealDishKey(2, 20)])
    }

    @Test
    fun `加一份后只有更早的恢复`() {
        val usages = listOf(use(1, 10), use(2, 20)) // 已按时间序：mr1 早于 mr2
        val short = PantryAllocation.shortages(servingCounts = mapOf(PORK to 1), usagesChrono = usages)
        // 7.4 够(rank1<=1)，不在 map；7.5 不足(rank2>1)
        assertTrue(short[MealDishKey(1, 10)] == null)
        assertEquals(listOf("猪肉"), short[MealDishKey(2, 20)])
    }

    @Test
    fun `份数充足时无不足`() {
        val usages = listOf(use(1, 10), use(2, 20))
        val short = PantryAllocation.shortages(servingCounts = mapOf(PORK to 2), usagesChrono = usages)
        assertTrue(short.isEmpty())
    }

    @Test
    fun `一道菜多种食材缺料都列出`() {
        val egg = 2L
        // mr1 的菜同时用猪肉(库存0)和鸡蛋(库存0)
        val usages = listOf(
            PantryUsage(PORK, "猪肉", 1, 10),
            PantryUsage(egg, "鸡蛋", 1, 10),
        )
        val short = PantryAllocation.shortages(mapOf(PORK to 0, egg to 0), usages)
        assertEquals(listOf("猪肉", "鸡蛋"), short[MealDishKey(1, 10)])
    }

    @Test
    fun `未在库存的食材不判不足`() {
        // usagesChrono 只包含在库食材(查询已 JOIN pantry)，故此处不出现的食材天然不判
        val usages = listOf(use(1, 10))
        val short = PantryAllocation.shortages(mapOf(PORK to 1), usages)
        assertTrue(short.isEmpty())
    }
}
