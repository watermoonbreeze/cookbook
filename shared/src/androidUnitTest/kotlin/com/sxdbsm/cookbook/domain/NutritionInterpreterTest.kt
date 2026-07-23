package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.DishNutrition
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : NutritionInterpreterTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 病种切换解读视角纯函数单测（数值/占比/分级 + 嘌呤GI定性红线 + 缺数据留白 + 顺序）
 * <p>
 * [AI生成] 守红线:嘌呤/GI 不给占比数值、缺数据显"暂无数据"不显 0、只显登记病种。
 **/
class NutritionInterpreterTest {

    private fun nutri(totals: NutritionTotals, covered: Int = 3, total: Int = 3, estimated: Boolean = false) =
        DishNutrition(totals = totals, ingredientCount = total, coveredCount = covered, estimated = estimated)

    @Test
    fun emptyWhenNoConditions() {
        val r = NutritionInterpreter.forConditions(emptySet(), null, emptyList(), emptyList())
        assertTrue(r.isEmpty())
    }

    @Test
    fun hypertensionSodiumWithPct() {
        val n = nutri(NutritionTotals(sodiumMg = 2400.0, potassiumMg = 3200.0))
        val r = NutritionInterpreter.forConditions(setOf(HealthCondition.HYPERTENSION), n, emptyList(), emptyList())
        assertEquals(1, r.size)
        val block = r[0]
        assertEquals("高血压 · 钠", block.title)
        val sodium = block.metrics.first { it.label == "钠" }
        assertTrue(sodium.text.contains("2400 mg"))
        assertTrue(sodium.text.contains("占每日上限 120%")) // 2024版上限收紧至2000 → 2400/2000=120%
        assertEquals(MetricTone.HIGH, sodium.tone) // 2400/2000=1.2 ≥ WARN → 偏高
        // 钾达 3200/3600≈89% ≥80% → 正向"较充足"
        val k = block.metrics.first { it.label == "钾" }
        assertEquals(MetricTone.POSITIVE, k.tone)
        assertTrue(k.text.contains("较充足"))
    }

    @Test
    fun sodiumNoDataShowsPlaceholderNotZero() {
        val n = nutri(NutritionTotals(sodiumMg = 0.0))
        val r = NutritionInterpreter.forConditions(setOf(HealthCondition.HYPERTENSION), n, emptyList(), emptyList())
        val sodium = r[0].metrics.first { it.label == "钠" }
        assertEquals("暂无数据", sodium.text) // 缺数据留白·不显 0
        assertTrue(r[0].metrics.none { it.label == "钾" }) // 钾无数据不显
    }

    @Test
    fun diabetesGiQualitativeNoNumericPct() {
        val n = nutri(NutritionTotals(fiberG = 22.0))
        val r = NutritionInterpreter.forConditions(setOf(HealthCondition.DIABETES), n, listOf("白米饭"), emptyList())
        val block = r[0]
        assertTrue(block.hitNote.contains("白米饭"))
        assertTrue(block.caveat.contains("FAO/WHO"))
        // GI 不给占比数值(红线):metrics 里不该出现"占每日"的 GI 行
        assertTrue(block.metrics.none { it.text.contains("GI") && it.text.contains("占每日") })
        // 纤维 22/25=88% ≥80% → 正向
        assertTrue(block.metrics.any { it.label == "纤维" && it.tone == MetricTone.POSITIVE })
    }

    @Test
    fun goutPurineQualitativeWithCaveat() {
        val r = NutritionInterpreter.forConditions(setOf(HealthCondition.GOUT), null, emptyList(), listOf("猪肝"))
        val block = r[0]
        assertEquals("痛风 · 嘌呤", block.title)
        assertTrue(block.metrics.isEmpty()) // 嘌呤纯定性·无数值行
        assertTrue(block.hitNote.contains("猪肝"))
        assertTrue(block.caveat.contains("非国标"))
    }

    @Test
    fun hyperlipidemiaSatAndChol() {
        val n = nutri(NutritionTotals(saturatedFatG = 22.0, cholesterolMg = 150.0))
        val r = NutritionInterpreter.forConditions(setOf(HealthCondition.HYPERLIPIDEMIA), n, emptyList(), emptyList())
        val block = r[0]
        val sat = block.metrics.first { it.label == "饱和脂肪" }
        assertEquals(MetricTone.HIGH, sat.tone) // 22/20>1 → 偏高
        val chol = block.metrics.first { it.label == "胆固醇" }
        assertTrue(chol.text.contains("占每日上限 50%")) // 150/300
        assertTrue(block.caveat.contains("建议值"))
    }

    @Test
    fun preservesFixedOrder() {
        val conditions = setOf(HealthCondition.HYPERLIPIDEMIA, HealthCondition.HYPERTENSION, HealthCondition.GOUT)
        val r = NutritionInterpreter.forConditions(conditions, nutri(NutritionTotals(sodiumMg = 100.0)), emptyList(), emptyList())
        // 顺序固定:高血压→痛风→高血脂
        assertEquals(listOf("高血压 · 钠", "痛风 · 嘌呤", "高血脂 · 血脂"), r.map { it.title })
    }
}
