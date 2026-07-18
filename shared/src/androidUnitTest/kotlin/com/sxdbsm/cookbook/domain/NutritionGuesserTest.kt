package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : NutritionGuesserTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 食材按名营养推演——归一/近似命中/大类兜底/不确定不填 四级行为
 * <p>
 * [AI生成] 守：命中优先精确>后缀、兜底按大类均值、都不确定不编造(None)。
 **/
class NutritionGuesserTest {

    private val egg = NutritionGuessValues(energyKcal = 144.0, proteinG = 13.3, fatG = 8.8)
    private val pork = NutritionGuessValues(energyKcal = 508.0, proteinG = 7.7, fatG = 53.0)
    private val candidates = listOf("鸡蛋" to egg, "五花肉" to pork)

    @Test
    fun `归一剥离修饰词`() {
        assertEquals("五花肉", NutritionGuesser.coreName("冷冻五花肉"))
        assertEquals("西红柿", NutritionGuesser.coreName("有机 西红柿"))
        assertEquals("鸡蛋", NutritionGuesser.coreName("土鸡蛋"))
    }

    @Test
    fun `近似命中_剥修饰词后精确匹配`() {
        val g = NutritionGuesser.guess("土鸡蛋", candidates, FoodGroup.Group.EGG)
        assertTrue(g.source is NutritionGuessSource.Match, "应近似命中: ${g.source}")
        assertEquals("鸡蛋", (g.source as NutritionGuessSource.Match).refName)
        assertEquals(egg, g.values)
    }

    @Test
    fun `近似命中_输入为候选后缀`() {
        // "五花"→候选"五花肉"(候选 endsWith 输入核心)。
        val g = NutritionGuesser.guess("五花", candidates, FoodGroup.Group.RED_MEAT)
        assertTrue(g.source is NutritionGuessSource.Match)
        assertEquals("五花肉", (g.source as NutritionGuessSource.Match).refName)
    }

    @Test
    fun `无同名_按大类均值兜底`() {
        val g = NutritionGuesser.guess("某新奇绿叶菜", candidates, FoodGroup.Group.VEGETABLE)
        assertTrue(g.source is NutritionGuessSource.Group, "应大类兜底: ${g.source}")
        assertEquals("蔬菜类", (g.source as NutritionGuessSource.Group).groupLabel)
        // 蔬菜均值:低热量、有纤维。
        assertEquals(25.0, g.values?.energyKcal)
        assertTrue((g.values?.fiberG ?: 0.0) > 0.0)
    }

    @Test
    fun `都不确定_不预填不编造`() {
        val g = NutritionGuesser.guess("xyz奇怪东西", candidates, null)
        assertEquals(NutritionGuessSource.None, g.source)
        assertNull(g.values, "无同名无大类→不预填")
    }
}
