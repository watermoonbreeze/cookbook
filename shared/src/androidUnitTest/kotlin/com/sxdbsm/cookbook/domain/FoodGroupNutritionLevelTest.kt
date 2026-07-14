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
 * [AI生成] 守护三大支柱(蛋白/主食/蔬果)覆盖度分级口径。
 **/
class FoodGroupNutritionLevelTest {

    private fun level(vararg mains: String): Int =
        FoodGroup.nutritionLevel(FoodGroup.groupsOf(mains.toList()))

    @Test
    fun `空为0`() {
        assertEquals(0, FoodGroup.nutritionLevel(emptyList()))
    }

    @Test
    fun `仅主食或仅肉为较单一1`() {
        assertEquals(1, level("米饭"))
        assertEquals(1, level("五花肉"))
    }

    @Test
    fun `两大类为尚可2`() {
        // 肉(蛋白) + 米饭(主食)
        assertEquals(2, level("五花肉", "米饭"))
        // 蔬菜 + 主食
        assertEquals(2, level("青菜", "米饭"))
    }

    @Test
    fun `三大类齐为均衡3`() {
        // 蛋白(鸡) + 主食(米饭) + 蔬菜
        assertEquals(3, level("鸡", "米饭", "青菜"))
    }

    @Test
    fun `三类齐且多样为优4`() {
        // 鱼(蛋白) 米饭(主食) 青菜(蔬) 木耳(菌) 苹果(果) → ≥5 大类
        assertEquals(4, level("鱼", "米饭", "青菜", "木耳", "苹果"))
    }

    @Test
    fun `级别文字`() {
        assertEquals("营养优", FoodGroup.nutritionLevelLabel(4))
        assertEquals("均衡", FoodGroup.nutritionLevelLabel(3))
        assertTrue(FoodGroup.nutritionLevelLabel(0).isEmpty())
    }
}
