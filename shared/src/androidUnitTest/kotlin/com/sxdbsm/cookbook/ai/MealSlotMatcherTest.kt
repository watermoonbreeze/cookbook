package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : MealSlotMatcherTest
 * @Time : 2026/07/12
 * @Author : SXD-AI
 * @Desc : 餐次适配规则单测
 * <p>
 * [AI生成] 推荐加餐次选择回归。
 **/
class MealSlotMatcherTest {

    @Test
    fun `全部餐次都适配`() {
        assertTrue(MealSlotMatcher.matches(MealSlot.ALL, "红烧肉"))
        assertTrue(MealSlotMatcher.matches(MealSlot.ALL, "小米粥"))
    }

    @Test
    fun `早餐匹配粥蛋豆浆面等`() {
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "小米粥"))
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "蒸蛋羹"))
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "阳春面"))
        assertFalse(MealSlotMatcher.matches(MealSlot.BREAKFAST, "红烧肉"), "红烧肉不是早餐菜")
    }

    @Test
    fun `正餐排除纯饮品其余都算`() {
        assertTrue(MealSlotMatcher.matches(MealSlot.DINNER, "红烧肉"))
        assertTrue(MealSlotMatcher.matches(MealSlot.LUNCH, "青椒炒肉丝"))
        assertFalse(MealSlotMatcher.matches(MealSlot.DINNER, "豆浆"), "纯豆浆不作正餐")
    }

    @Test
    fun `加餐宵夜偏轻`() {
        assertTrue(MealSlotMatcher.matches(MealSlot.NIGHT_SNACK, "小米粥"))
        assertTrue(MealSlotMatcher.matches(MealSlot.AFTERNOON_SNACK, "牛奶"))
        assertFalse(MealSlotMatcher.matches(MealSlot.MORNING_SNACK, "红烧肉"), "红烧肉不算加餐轻食")
    }
}
