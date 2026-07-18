package com.sxdbsm.cookbook.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : NutritionBalanceTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 营养互补度打分单测（含 A2 无基线绝对均衡分兜底）
 * <p>
 * [AI生成] A2：今日还没吃(无缺口基线)时，营养互补因子不再恒 0——改用"绝对均衡分"(仅奖励接近目标比例的菜)。
 **/
class NutritionBalanceTest {

    // 三大宏量供能占比目标 P20/F28/C52：构造对应克数(蛋白/碳水 4kcal/g、脂肪 9kcal/g)
    private val balanced = NutritionTotals(proteinG = 5.0, fatG = 3.111, carbG = 13.0) // ≈ 20/28/52
    private val pureCarb = NutritionTotals(carbG = 25.0) // 纯碳水
    private val highProtein = NutritionTotals(proteinG = 25.0) // 高蛋白(如清蒸鱼)

    @Test
    fun `无基线_均衡候选得正分`() {
        val s = NutritionBalance.score(NutritionTotals.EMPTY, balanced)
        assertTrue(s > 0.0, "今日未吃时，接近膳食指南比例的均衡菜应得正分: $s")
    }

    @Test
    fun `无基线_偏斜单菜不扣分只中性`() {
        // 高蛋白单菜(清蒸鱼)是合理选择，无基线时应=0(中性)而非负分，避免误伤。
        assertEquals(0.0, NutritionBalance.score(NutritionTotals.EMPTY, highProtein), 1e-9)
        assertEquals(0.0, NutritionBalance.score(NutritionTotals.EMPTY, pureCarb), 1e-9)
    }

    @Test
    fun `无基线_均衡菜优于偏斜菜`() {
        assertTrue(
            NutritionBalance.score(NutritionTotals.EMPTY, balanced) >
                NutritionBalance.score(NutritionTotals.EMPTY, pureCarb),
            "无基线时均衡菜应排在纯碳水菜前",
        )
    }

    @Test
    fun `候选无营养数据_恒中性`() {
        assertEquals(0.0, NutritionBalance.score(balanced, NutritionTotals.EMPTY), 1e-9)
    }

    @Test
    fun `有基线_补近期所缺宏量得正分_向后兼容`() {
        // 近期偏碳水(缺蛋白) → 高蛋白候选补缺口 → 正分(原有逻辑不变)。
        val recentCarbHeavy = NutritionTotals(proteinG = 2.0, fatG = 2.0, carbG = 30.0)
        val s = NutritionBalance.score(recentCarbHeavy, highProtein)
        assertTrue(s > 0.0, "补近期缺的蛋白应得正分: $s")
    }
}
