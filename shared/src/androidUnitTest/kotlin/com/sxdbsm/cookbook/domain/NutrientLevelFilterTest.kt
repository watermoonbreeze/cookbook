package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.IngredientNutritionRow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : NutrientLevelFilterTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : GI/钠/嘌呤 三级分级 + 命中筛选 单测（边界 + 无数据 + 多选）
 * <p>
 * [AI生成] 守阈值口径:GI 55/70 FAO/WHO、钠 120/600 惯例、嘌呤 25/150 惯例·非国标。
 **/
class NutrientLevelFilterTest {

    private fun row(gi: Double? = null, sodium: Double? = null, purine: Double? = null) =
        IngredientNutritionRow(
            name = "x", foodGroup = "g", kcal = null, protein = null, fat = null, carb = null,
            fiber = null, sodium = sodium, potassium = null, calcium = null, gi = gi, purine = purine,
        )

    @Test
    fun giBands() {
        assertEquals(NutrientLevel.LOW, NutrientBands.levelOf(FilterMetric.GI, 55.0)) // ≤55 低
        assertEquals(NutrientLevel.MID, NutrientBands.levelOf(FilterMetric.GI, 56.0))
        assertEquals(NutrientLevel.MID, NutrientBands.levelOf(FilterMetric.GI, 69.0))
        assertEquals(NutrientLevel.HIGH, NutrientBands.levelOf(FilterMetric.GI, 70.0)) // ≥70 高
    }

    @Test
    fun sodiumBands() {
        assertEquals(NutrientLevel.LOW, NutrientBands.levelOf(FilterMetric.SODIUM, 120.0))
        assertEquals(NutrientLevel.MID, NutrientBands.levelOf(FilterMetric.SODIUM, 300.0))
        assertEquals(NutrientLevel.HIGH, NutrientBands.levelOf(FilterMetric.SODIUM, 600.0))
    }

    @Test
    fun purineBands() {
        assertEquals(NutrientLevel.LOW, NutrientBands.levelOf(FilterMetric.PURINE, 25.0))
        assertEquals(NutrientLevel.MID, NutrientBands.levelOf(FilterMetric.PURINE, 100.0))
        assertEquals(NutrientLevel.HIGH, NutrientBands.levelOf(FilterMetric.PURINE, 150.0))
    }

    @Test
    fun nullValueNoLevel() {
        assertNull(NutrientBands.levelOf(FilterMetric.GI, null))
    }

    @Test
    fun matchesEmptyLevelsPassesAll() {
        assertTrue(NutrientBands.matches(FilterMetric.GI, emptySet(), row(gi = 90.0)))
    }

    @Test
    fun matchesRespectsLevelSet() {
        val lowGi = row(gi = 40.0)
        assertTrue(NutrientBands.matches(FilterMetric.GI, setOf(NutrientLevel.LOW), lowGi))
        assertFalse(NutrientBands.matches(FilterMetric.GI, setOf(NutrientLevel.HIGH), lowGi))
        // 多选低+中
        assertTrue(NutrientBands.matches(FilterMetric.GI, setOf(NutrientLevel.LOW, NutrientLevel.MID), lowGi))
    }

    @Test
    fun matchesExcludesNoData() {
        // 有级别筛选 + 该指标无数据 → 不通过(被排除)
        assertFalse(NutrientBands.matches(FilterMetric.SODIUM, setOf(NutrientLevel.LOW), row(gi = 40.0)))
    }
}
