package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.CalorieStatus
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : NutritionLevelEvaluatorTest
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 营养级别综合评估——缺数据退回多样性、钠/热量下调、慢病只触发已登记病种
 * <p>
 * [AI生成] 守方案：缺营养数据不下调(向后兼容)、慢病只对登记病种触发、纯结构高分被高钠拉回。
 **/
class NutritionLevelEvaluatorTest {

    private fun totals(kcal: Double, sodium: Double) =
        NutritionTotals(energyKcal = kcal, sodiumMg = sodium)

    @Test
    fun `缺营养数据退回多样性级别不下调`() {
        assertEquals(4, NutritionLevelEvaluator.evaluate(4, null, null, setOf(HealthCondition.HYPERTENSION)).level)
        val zero = NutritionLevelEvaluator.evaluate(4, totals(0.0, 5000.0), CalorieStatus.ABOVE, setOf(HealthCondition.HYPERTENSION))
        assertEquals(4, zero.level, "热量0视为无数据，不下调")
        assertTrue(zero.concerns.isEmpty())
    }

    @Test
    fun `未登记高血压则钠不触发`() {
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 5000.0), CalorieStatus.ON, emptySet())
        assertEquals(4, r.level, "没登记病种→高钠不下调")
        assertTrue(r.concerns.isEmpty())
    }

    @Test
    fun `高血压且钠超上限_结构优也拉回尚可并给提示`() {
        // 钠 3000mg > 2400 上限 → 注意，级别封到 2。
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 3000.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION))
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("偏咸") && it.contains("高血压") }, "应给偏咸提示: ${r.concerns}")
    }

    @Test
    fun `高血压且钠七成到上限_略下调到3`() {
        // 钠 2000mg / 2400 ≈ 83% ∈[0.7,1.0) → 中，级别封到 3。
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 2000.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION))
        assertEquals(3, r.level)
        assertTrue(r.concerns.any { it.contains("钠偏高") })
    }

    @Test
    fun `热量超标下调并提示`() {
        val r = NutritionLevelEvaluator.evaluate(3, totals(3000.0, 100.0), CalorieStatus.ABOVE, emptySet())
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("热量") })
    }

    @Test
    fun `钠正常热量达标_不下调`() {
        val r = NutritionLevelEvaluator.evaluate(4, totals(800.0, 500.0), CalorieStatus.ON, setOf(HealthCondition.HYPERTENSION))
        assertEquals(4, r.level)
        assertTrue(r.concerns.isEmpty())
    }

    @Test
    fun `病种名映射`() {
        assertTrue(HealthCondition.HYPERTENSION in HealthCondition.fromCareName("高血压"))
        assertTrue(HealthCondition.GOUT in HealthCondition.fromCareName("痛风/高尿酸"))
        val sanGao = HealthCondition.fromCareName("三高")
        assertTrue(HealthCondition.HYPERTENSION in sanGao && HealthCondition.DIABETES in sanGao)
    }
}
