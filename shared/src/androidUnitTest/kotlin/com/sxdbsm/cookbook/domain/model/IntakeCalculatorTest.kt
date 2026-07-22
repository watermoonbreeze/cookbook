package com.sxdbsm.cookbook.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : IntakeCalculatorTest
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 个人摄入折算(整份×食用比例×份额)单一真相源单测
 * <p>
 * 覆盖：比例缩放、clamp[0,1]防脏值、默认1.0与旧口径恒等(零回归)、逐菜比例、share 归一守恒。
 * <p>
 * [AI生成] 食用比例(是否吃完)维度落地。
 **/
class IntakeCalculatorTest {

    private fun t(kcal: Double, na: Double = 0.0) = NutritionTotals(energyKcal = kcal, sodiumMg = na)

    @Test
    fun eatenPortion_scalesByRatio() {
        val r = IntakeCalculator.eatenPortion(t(100.0, 20.0), 0.5)
        assertEquals(50.0, r.energyKcal, 1e-9)
        assertEquals(10.0, r.sodiumMg, 1e-9)
    }

    @Test
    fun eatenPortion_clampsRatioToUnitInterval() {
        // >1 夹到 1(防"天价"放大)、<0 夹到 0(防负营养)。
        assertEquals(100.0, IntakeCalculator.eatenPortion(t(100.0), 2.0).energyKcal, 1e-9)
        assertEquals(0.0, IntakeCalculator.eatenPortion(t(100.0), -0.5).energyKcal, 1e-9)
    }

    @Test
    fun personalIntake_defaultRatioOne_equalsOldFormula() {
        // 默认吃完(1.0)时 = Σ整份 × share（与旧"整份×share"口径恒等·零回归）。
        val dishes = listOf(t(200.0) to 1.0, t(300.0) to 1.0)
        assertEquals((200.0 + 300.0) * 0.4, IntakeCalculator.personalIntake(dishes, 0.4).energyKcal, 1e-9)
    }

    @Test
    fun personalIntake_appliesPerDishRatio() {
        // 一菜吃完(200)+一菜吃一半(300×0.5=150)=350，share=1。
        val dishes = listOf(t(200.0) to 1.0, t(300.0) to 0.5)
        assertEquals(350.0, IntakeCalculator.personalIntake(dishes, 1.0).energyKcal, 1e-9)
    }

    @Test
    fun personalIntake_shareNormalizationConserves() {
        // 全家个人摄入之和 = Σ(整份×eatenRatio)：eatenRatio 不破坏 share 归一守恒。
        val dishes = listOf(t(200.0) to 0.5, t(300.0) to 1.0) // 实吃合计 = 100 + 300 = 400
        val shares = listOf(0.5, 0.3, 0.2) // 归一(和=1)
        val sum = shares.sumOf { IntakeCalculator.personalIntake(dishes, it).energyKcal }
        assertEquals(400.0, sum, 1e-9)
    }
}
