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

    @Test
    fun `精确优先于后缀`() {
        // 同时存在"五花肉"与"五花肉丸子"，输入"五花肉"应精确命中"五花肉"(而非后缀匹配丸子)。
        val cands = listOf("五花肉丸子" to NutritionGuessValues(energyKcal = 200.0), "五花肉" to pork)
        val g = NutritionGuesser.guess("五花肉", cands, FoodGroup.Group.RED_MEAT)
        assertEquals("五花肉", (g.source as NutritionGuessSource.Match).refName)
        assertEquals(pork, g.values)
    }

    @Test
    fun `短名跳过近似命中走大类`() {
        // 核心名 <2 字("蛋")不做近似匹配(防泛匹配)，退大类兜底。
        val g = NutritionGuesser.guess("蛋", candidates, FoodGroup.Group.EGG)
        assertTrue(g.source is NutritionGuessSource.Group, "短名应走大类: ${g.source}")
    }

    @Test
    fun `同名多候选取匹配名最长`() {
        // 输入"红烧五花肉"→"五花肉"(后缀6分+长度)胜过更短的泛候选"肉"。
        val cands = listOf("肉" to NutritionGuessValues(energyKcal = 100.0), "五花肉" to pork)
        val g = NutritionGuesser.guess("红烧五花肉", cands, FoodGroup.Group.RED_MEAT)
        assertEquals("五花肉", (g.source as NutritionGuessSource.Match).refName, "应取匹配名最长者")
    }
}
