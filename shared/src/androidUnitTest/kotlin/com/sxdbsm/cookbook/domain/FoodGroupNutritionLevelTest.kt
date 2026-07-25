package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : FoodGroupNutritionLevelTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养均衡级别(供餐食卡片配色/营养色系墙)测试
 * <p>
 * [AI修改] 权威化重审 P1：口径改为**膳食宝塔四正向层覆盖度**(谷薯/蔬果/鱼禽肉蛋/奶豆坚果)，守护该分级。
 **/
class FoodGroupNutritionLevelTest {

    private fun level(vararg mains: String): Int =
        FoodGroup.nutritionLevel(FoodGroup.groupsOf(mains.toList()))

    @Test
    fun `空为0`() {
        assertEquals(0, FoodGroup.nutritionLevel(emptyList()))
    }

    @Test
    fun `仅一层为较单一1`() {
        assertEquals(1, level("米饭"))   // 谷薯层
        assertEquals(1, level("五花肉")) // 鱼禽肉蛋层
    }

    @Test
    fun `两层为尚可2`() {
        // 鱼禽肉蛋 + 谷薯
        assertEquals(2, level("五花肉", "米饭"))
        // 蔬果 + 谷薯
        assertEquals(2, level("青菜", "米饭"))
    }

    @Test
    fun `三层为均衡3`() {
        // 鱼禽肉蛋(鸡) + 谷薯(米饭) + 蔬果(青菜)
        assertEquals(3, level("鸡", "米饭", "青菜"))
    }

    @Test
    fun `同层多样不额外加级`() {
        // 鱼+米饭+青菜+木耳+苹果 = 鱼禽肉蛋/谷薯/蔬果 三层(木耳苹果都在蔬果层) → 仍 3，不因大类多而升4。
        assertEquals(3, level("鱼", "米饭", "青菜", "木耳", "苹果"))
    }

    @Test
    fun `四层齐含奶豆坚果层为营养优4`() {
        // 鱼禽肉蛋(鱼) + 谷薯(米饭) + 蔬果(青菜) + 奶豆坚果(牛奶) → 四层齐。
        assertEquals(4, level("鱼", "米饭", "青菜", "牛奶"))
        // 豆(奶豆坚果层)同样可凑第四层。
        assertEquals(4, level("鸡", "米饭", "青菜", "豆腐"))
    }

    @Test
    fun `豆饭菜三层_缺鱼禽肉蛋层仍为均衡3`() {
        // 豆腐(奶豆坚果) + 米饭(谷薯) + 青菜(蔬果) = 3 层(无鱼禽肉蛋层) → 3。
        assertEquals(3, level("豆腐", "米饭", "青菜"))
    }

    @Test
    fun `级别文字`() {
        assertEquals("营养优", FoodGroup.nutritionLevelLabel(4))
        assertEquals("均衡", FoodGroup.nutritionLevelLabel(3))
        assertTrue(FoodGroup.nutritionLevelLabel(0).isEmpty())
    }
}
