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
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "牛奶燕麦"))
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "煎饼果子"))
        assertFalse(MealSlotMatcher.matches(MealSlot.BREAKFAST, "红烧肉"), "红烧肉不是早餐菜")
    }

    @Test
    fun `QW3去玉米南瓜薯广词_炒菜正餐不再误入早餐`() {
        // [AI生成] QW-3(2026-07-20#3):裸"玉米/南瓜/薯"曾把炒菜/正餐误判早餐,导致推荐早餐出现松仁玉米等"怪菜"。
        assertFalse(MealSlotMatcher.matches(MealSlot.BREAKFAST, "松仁玉米"), "松仁玉米是炒菜非早餐")
        assertFalse(MealSlotMatcher.matches(MealSlot.BREAKFAST, "玉米排骨汤"), "玉米排骨汤是正餐非早餐")
        assertFalse(MealSlotMatcher.matches(MealSlot.BREAKFAST, "南瓜排骨"), "南瓜排骨是正餐非早餐")
        assertFalse(MealSlotMatcher.matches(MealSlot.BREAKFAST, "拔丝红薯"), "拔丝红薯是甜点非早餐")
        // 南瓜粥/红薯粥/南瓜饼仍应命中(经粥/饼)。
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "南瓜粥"))
        assertTrue(MealSlotMatcher.matches(MealSlot.BREAKFAST, "南瓜饼"))
    }

    @Test
    fun `正餐排除纯饮品其余都算`() {
        assertTrue(MealSlotMatcher.matches(MealSlot.DINNER, "红烧肉"))
        assertTrue(MealSlotMatcher.matches(MealSlot.LUNCH, "青椒炒肉丝"))
        assertFalse(MealSlotMatcher.matches(MealSlot.DINNER, "豆浆"), "纯豆浆不作正餐")
    }

    @Test
    fun `判荤用具体词不误伤同字素菜`() {
        assertTrue(MealSlotMatcher.isMeatByMains(listOf("五花肉")))
        assertTrue(MealSlotMatcher.isMeatByMains(listOf("鸡蛋")), "蛋算荤(优质蛋白)")
        assertTrue(MealSlotMatcher.isMeatByMains(listOf("海参")))
        assertFalse(MealSlotMatcher.isMeatByMains(listOf("蟹味菇")), "蟹味菇是食用菌不是荤")
        assertFalse(MealSlotMatcher.isMeatByMains(listOf("茄子")), "鱼香茄子主料茄子非荤")
        assertFalse(MealSlotMatcher.isMeatByMains(listOf("青椒", "土豆")))
    }

    @Test
    fun `加餐宵夜偏轻`() {
        assertTrue(MealSlotMatcher.matches(MealSlot.NIGHT_SNACK, "小米粥"))
        assertTrue(MealSlotMatcher.matches(MealSlot.AFTERNOON_SNACK, "牛奶"))
        assertFalse(MealSlotMatcher.matches(MealSlot.MORNING_SNACK, "红烧肉"), "红烧肉不算加餐轻食")
    }
}
