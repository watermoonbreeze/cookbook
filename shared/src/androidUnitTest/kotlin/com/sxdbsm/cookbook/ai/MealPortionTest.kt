package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : MealPortionTest
 * @Time : 2026/07/12
 * @Author : SXD-AI
 * @Desc : 按人数定菜数单测
 * <p>
 * [AI生成] 周期规划按人数定正餐菜数回归。
 **/
class MealPortionTest {

    @Test
    fun `正餐菜数随人数增多`() {
        assertEquals(2..2, MealPortion.mainRange(1))
        assertEquals(2..3, MealPortion.mainRange(2))
        assertEquals(3..4, MealPortion.mainRange(3))
        assertEquals(3..4, MealPortion.mainRange(4))
        assertEquals(4..5, MealPortion.mainRange(5))
        assertEquals(4..5, MealPortion.mainRange(6))
        assertEquals(5..6, MealPortion.mainRange(7))
        assertEquals(5..6, MealPortion.mainRange(8))
    }

    @Test
    fun `人数越界收敛`() {
        assertEquals(MealPortion.mainRange(1), MealPortion.mainRange(0))
        assertEquals(MealPortion.mainRange(8), MealPortion.mainRange(99))
    }

    @Test
    fun `早餐轻量不随人数放大`() {
        assertEquals(1..1, MealPortion.rangeFor("早餐", 1))
        assertEquals(2..2, MealPortion.rangeFor("早餐", 4))
        // 8 人早餐最多 2~3，仍远小于正餐 5~6
        val bf = MealPortion.rangeFor("早餐", 8)
        assertTrue(bf.last <= 3, "早餐不随人数放大到正餐量")
    }

    @Test
    fun `中晚餐走正餐区间`() {
        assertEquals(MealPortion.mainRange(4), MealPortion.rangeFor("中餐", 4))
        assertEquals(MealPortion.mainRange(6), MealPortion.rangeFor("晚餐", 6))
    }
}
