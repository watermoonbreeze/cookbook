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
    fun `P4痛风_命中高嘌呤定性食物_下调并提示_仅登记痛风生效`() {
        val hits = NutritionLevelEvaluator.matchHighPurineFoods(listOf("猪肝", "青菜", "浓肉汤"))
        assertEquals(listOf("猪肝", "浓肉汤"), hits, "主料名匹配内脏/浓肉汤")
        // 登记痛风 + 命中 → 封到 2 + 提示
        val gout = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, setOf(HealthCondition.GOUT), highPurineHits = hits,
        )
        assertEquals(2, gout.level)
        assertTrue(gout.concerns.any { it.contains("嘌呤") && it.contains("痛风") }, "应给痛风提示: ${gout.concerns}")
        // 没登记痛风 → 命中也不下调
        val noGout = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, emptySet(), highPurineHits = hits,
        )
        assertEquals(4, noGout.level)
        assertTrue(noGout.concerns.isEmpty())
        // 登记痛风但未命中 → 不下调
        val goutNoHit = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 200.0), CalorieStatus.ON, setOf(HealthCondition.GOUT), highPurineHits = emptyList(),
        )
        assertEquals(4, goutNoHit.level)
    }

    @Test
    fun `主料空_不误触发痛风`() {
        // 红线:mainIngredientNames 曾恒空——空输入应返空、不误触发。
        assertTrue(NutritionLevelEvaluator.matchHighPurineFoods(emptyList()).isEmpty())
        // "凤尾"泛词已删:凤尾菇不应命中(凤尾鱼仍命中)。
        assertTrue(NutritionLevelEvaluator.matchHighPurineFoods(listOf("凤尾菇", "青菜")).isEmpty())
        assertEquals(listOf("凤尾鱼"), NutritionLevelEvaluator.matchHighPurineFoods(listOf("凤尾鱼")))
    }

    @Test
    fun `高血压钠加痛风嘌呤同时命中_取最严且两提示`() {
        // 多慢病叠加:level 各分支 minOf 取最严、concerns 各自追加不覆盖。
        val r = NutritionLevelEvaluator.evaluate(
            4, totals(800.0, 3000.0), CalorieStatus.ON,
            setOf(HealthCondition.HYPERTENSION, HealthCondition.GOUT), highPurineHits = listOf("猪肝"),
        )
        assertEquals(2, r.level)
        assertTrue(r.concerns.any { it.contains("偏咸") } && r.concerns.any { it.contains("嘌呤") }, "两提示都在: ${r.concerns}")
    }

    @Test
    fun `病种名映射`() {
        assertTrue(HealthCondition.HYPERTENSION in HealthCondition.fromCareName("高血压"))
        assertTrue(HealthCondition.GOUT in HealthCondition.fromCareName("痛风/高尿酸"))
        val sanGao = HealthCondition.fromCareName("三高")
        assertTrue(HealthCondition.HYPERTENSION in sanGao && HealthCondition.DIABETES in sanGao)
    }
}
