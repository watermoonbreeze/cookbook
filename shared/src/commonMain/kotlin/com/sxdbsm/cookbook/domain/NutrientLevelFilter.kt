package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.IngredientNutritionRow

/**
 * @File : NutrientLevelFilter
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 食材按指标分级（低/中/高）——GI/钠/嘌呤三级筛选导航（商业#7）
 * <p>
 * 让慢病家庭按"低GI/低钠/低嘌呤"筛出能吃的食材。每 100g 可食部值判级。
 * <p>
 * 健康红线（口径区分国标 vs 惯例·用则必标）：
 * · **GI**：低≤55 / 中 56–69 / 高≥70 —— **FAO/WHO 口径**（有据；`GI_HIGH` 复用 [NutritionLevelEvaluator]）。
 * · **钠**（mg/100g）：低≤120（GB 28050 低钠声称）；中/高**无清晰国标**→惯例参考。
 * · **嘌呤**（mg/100g）：低≤25 / 中 / 高≥150 —— **无国标**（WS/T 560 只定性），三级为**惯例参考·非国标**。
 * UI 必在钠/嘌呤处标"非国标·惯例参考"、GI 标"FAO/WHO 口径"，措辞守"仅供参考·便于筛选·非医嘱"。
 * <p>
 * [AI生成] 纯逻辑、可单测；缺该指标数据(null)→判级返回 null（UI 按"无数据"排除+透明计数）。
 **/

/** 可筛选的指标。[AI生成] */
enum class FilterMetric { GI, SODIUM, PURINE }

/** 指标级别。[AI生成] */
enum class NutrientLevel { LOW, MID, HIGH }

object NutrientBands {
    // GI（FAO/WHO）：低≤55、高≥70、中夹在其间。GI_HIGH 转发 NutritionLevelEvaluator 单一真相源。
    const val GI_LOW = 55.0
    const val GI_HIGH = NutritionLevelEvaluator.GI_HIGH

    // 钠 mg/100g：低≤120(GB28050 低钠声称)、高≥600(惯例·约 NRV 30%)、中间。中/高非国标。
    const val SODIUM_LOW = 120.0
    const val SODIUM_HIGH = 600.0

    // 嘌呤 mg/100g：低≤25、高≥150(惯例·非国标，WS/T560 只定性)、中间。
    const val PURINE_LOW = 25.0
    const val PURINE_HIGH = 150.0

    /** 取某指标在该食材行的每100g值（缺则 null）。[AI生成] */
    fun valueOf(metric: FilterMetric, row: IngredientNutritionRow): Double? = when (metric) {
        FilterMetric.GI -> row.gi
        FilterMetric.SODIUM -> row.sodium
        FilterMetric.PURINE -> row.purine
    }

    /** 判级：null 值→null（无数据）。[AI生成] */
    fun levelOf(metric: FilterMetric, value: Double?): NutrientLevel? {
        if (value == null) return null
        return when (metric) {
            FilterMetric.GI -> band(value, GI_LOW, GI_HIGH)
            FilterMetric.SODIUM -> band(value, SODIUM_LOW, SODIUM_HIGH)
            FilterMetric.PURINE -> band(value, PURINE_LOW, PURINE_HIGH)
        }
    }

    private fun band(v: Double, low: Double, high: Double): NutrientLevel = when {
        v <= low -> NutrientLevel.LOW
        v >= high -> NutrientLevel.HIGH
        else -> NutrientLevel.MID
    }

    /**
     * 一行是否命中"某指标 ∈ 选中级别集"。[AI生成]
     * @param levels 空集=不筛（全通过）；非空=该指标值判级须落在集合内，缺数据(null 级)不通过（UI 另计"无数据未列出"）。
     */
    fun matches(metric: FilterMetric, levels: Set<NutrientLevel>, row: IngredientNutritionRow): Boolean {
        if (levels.isEmpty()) return true
        val lv = levelOf(metric, valueOf(metric, row)) ?: return false
        return lv in levels
    }
}
