package com.sxdbsm.cookbook.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : NutritionCalculatorTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养计算器测试（克重折算/汇总/覆盖率/估算标记）
 * <p>
 * [AI生成] 纯逻辑测试，不依赖 DB。
 **/
class NutritionCalculatorTest {

    private val egg = IngredientNutrition(ingredientId = 1, energyKcal = 144.0, proteinG = 13.3, pieceGram = 50.0)
    private val rice = IngredientNutrition(ingredientId = 2, energyKcal = 346.0, carbG = 77.9)

    @Test
    fun `计件单位用piece_gram折算_1个鸡蛋72kcal`() {
        val input = NutritionInput(quantity = 1.0, unitGrams = null, nutrition = egg)
        val (grams, est) = NutritionCalculator.resolveGrams(input)
        assertEquals(50.0, grams)
        assertFalse(est, "有 pieceGram 不算估算")
        val d = NutritionCalculator.dishNutrition(listOf(input))
        assertEquals(72.0, d.totals.energyKcal, 0.001)
        assertEquals(1, d.coveredCount)
        assertTrue(d.complete)
    }

    @Test
    fun `重量单位用克当量折算_100克大米346kcal`() {
        val input = NutritionInput(quantity = 100.0, unitGrams = 1.0, nutrition = rice)
        val d = NutritionCalculator.dishNutrition(listOf(input))
        assertEquals(346.0, d.totals.energyKcal, 0.001)
    }

    @Test
    fun `一道菜多料汇总_鸡蛋加大米`() {
        val d = NutritionCalculator.dishNutrition(
            listOf(
                NutritionInput(1.0, null, egg),      // 50g → 72kcal
                NutritionInput(50.0, 1.0, rice),     // 50g → 173kcal
            ),
        )
        assertEquals(72.0 + 173.0, d.totals.energyKcal, 0.001)
        assertEquals(2, d.ingredientCount)
        assertEquals(2, d.coveredCount)
    }

    @Test
    fun `缺营养数据的料只降覆盖率不报错`() {
        val d = NutritionCalculator.dishNutrition(
            listOf(
                NutritionInput(1.0, null, egg),
                NutritionInput(1.0, null, null), // 无营养数据
            ),
        )
        assertEquals(72.0, d.totals.energyKcal, 0.001)
        assertEquals(2, d.ingredientCount)
        assertEquals(1, d.coveredCount)
        assertFalse(d.complete, "有料缺数据即不完整")
        assertTrue(d.hasData)
    }

    @Test
    fun `营养互补度_近期碳水多则高蛋白菜加分_高碳水菜降分`() {
        val recentCarbHeavy = NutritionTotals(proteinG = 5.0, fatG = 2.0, carbG = 100.0) // 碳水为主
        val proteinDish = NutritionTotals(proteinG = 30.0, fatG = 5.0, carbG = 5.0) // 高蛋白
        val carbDish = NutritionTotals(proteinG = 2.0, fatG = 1.0, carbG = 60.0) // 又是高碳水
        val protScore = NutritionBalance.score(recentCarbHeavy, proteinDish)
        val carbScore = NutritionBalance.score(recentCarbHeavy, carbDish)
        assertTrue(protScore > 0, "近期缺蛋白→高蛋白菜加分")
        assertTrue(carbScore < 0, "近期已多碳水→再高碳水降分")
        assertTrue(protScore > carbScore)
    }

    @Test
    fun `营养互补度_任一侧无数据返回0`() {
        assertEquals(0.0, NutritionBalance.score(NutritionTotals.EMPTY, NutritionTotals(proteinG = 10.0)))
        assertEquals(0.0, NutritionBalance.score(NutritionTotals(carbG = 10.0), NutritionTotals.EMPTY))
    }

    @Test
    fun `计件单位且无piece_gram走兜底并标估算`() {
        val noPiece = IngredientNutrition(ingredientId = 3, energyKcal = 100.0) // 无 pieceGram
        val input = NutritionInput(quantity = 2.0, unitGrams = null, nutrition = noPiece)
        val (grams, est) = NutritionCalculator.resolveGrams(input)
        assertEquals(2.0 * NutritionCalculator.DEFAULT_PIECE_GRAM, grams)
        assertTrue(est)
        val d = NutritionCalculator.dishNutrition(listOf(input))
        assertTrue(d.estimated)
        assertFalse(d.complete)
    }
}
