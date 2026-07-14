package com.sxdbsm.cookbook.domain.model

/**
 * @File : Nutrition
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养素领域模型 + 计算层（每100g值 → 菜品/餐/日汇总）
 * <p>
 * 数据口径：每 100g 可食部（国标《中国食物成分表》整理，AI 参考、非权威核对，见 ref/免责）。
 * 汇总只累加“可加性”营养素(热量/宏量/纤维/矿物质/嘌呤总量)；GI 是浓度值不可加，仅在食材/单料层展示。
 * <p>
 * [AI生成] 营养素 L2 数值层的领域模型与计算器。
 **/

/** 单个食材每 100g 可食部营养（全部可空=未知）。[AI生成] */
data class IngredientNutrition(
    val ingredientId: Long,
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val fatG: Double? = null,
    val carbG: Double? = null,
    val fiberG: Double? = null,
    val sodiumMg: Double? = null,
    val potassiumMg: Double? = null,
    val calciumMg: Double? = null,
    val gi: Double? = null,
    val purineMg: Double? = null,
    /** 计件单位默认克重（如 1 个鸡蛋≈50g），用于把“个/勺/片”折算成克。 */
    val pieceGram: Double? = null,
    val ref: String = "",
    val review: Boolean = false,
) {
    /** 是否含有任何量化营养数据（用于判断“有无数据”）。 */
    val hasAny: Boolean
        get() = listOf(energyKcal, proteinG, fatG, carbG, fiberG, sodiumMg, potassiumMg, calciumMg, purineMg).any { it != null }
}

/**
 * 可加性营养汇总（菜品/餐/日通用）。单位：kcal / g / mg。[AI生成]
 */
data class NutritionTotals(
    val energyKcal: Double = 0.0,
    val proteinG: Double = 0.0,
    val fatG: Double = 0.0,
    val carbG: Double = 0.0,
    val fiberG: Double = 0.0,
    val sodiumMg: Double = 0.0,
    val potassiumMg: Double = 0.0,
    val calciumMg: Double = 0.0,
    val purineMg: Double = 0.0,
) {
    operator fun plus(o: NutritionTotals) = NutritionTotals(
        energyKcal + o.energyKcal,
        proteinG + o.proteinG,
        fatG + o.fatG,
        carbG + o.carbG,
        fiberG + o.fiberG,
        sodiumMg + o.sodiumMg,
        potassiumMg + o.potassiumMg,
        calciumMg + o.calciumMg,
        purineMg + o.purineMg,
    )

    companion object {
        val EMPTY = NutritionTotals()
    }
}

/**
 * 菜品营养估算结果。[AI生成]
 *
 * @param totals 汇总营养
 * @param ingredientCount 参与计算的配料总数
 * @param coveredCount 其中有营养数据的配料数（coveredCount < ingredientCount 表示部分缺数据）
 * @param estimated 是否用到克重兜底估算（用量缺失/单位无克当量且无 piece_gram）
 */
data class DishNutrition(
    val totals: NutritionTotals,
    val ingredientCount: Int,
    val coveredCount: Int,
    val estimated: Boolean,
) {
    /** 营养数据是否完整（每个配料都有数据且无克重兜底）。 */
    val complete: Boolean get() = ingredientCount > 0 && coveredCount == ingredientCount && !estimated
    /** 至少有部分数据可展示。 */
    val hasData: Boolean get() = coveredCount > 0
}

/**
 * 计算某配料折算克数所需的原始输入（对应 SQL selectNutritionInputsByDishIds 一行）。[AI生成]
 */
data class NutritionInput(
    val quantity: Double?,
    /** 该配料所用单位的克当量（克=1、两=50…；计件单位为 null）。 */
    val unitGrams: Double?,
    val nutrition: IngredientNutrition?,
)

/**
 * 营养计算器：把“配料用量 + 每100g营养”折算成菜品/餐/日汇总。[AI生成]
 *
 * 折算克数优先级：quantity×unitGrams（重量/体积单位）→ quantity×pieceGram（计件单位默认克重）
 * → quantity×DEFAULT_PIECE_GRAM（兜底估算，置 estimated）。用量或营养缺失的配料贡献 0 但计入覆盖率。
 */
object NutritionCalculator {
    /** 计件单位且食材无 piece_gram 时的兜底单件克重（粗估，会标“估算”）。 */
    const val DEFAULT_PIECE_GRAM = 60.0

    /** 解析单条配料应计的克数；返回 (克数, 是否兜底估算)。克数为 null 表示无法计算(用量缺失)。 */
    fun resolveGrams(input: NutritionInput): Pair<Double?, Boolean> {
        val qty = input.quantity ?: return null to false
        input.unitGrams?.let { return (qty * it) to false }
        input.nutrition?.pieceGram?.let { return (qty * it) to false }
        return (qty * DEFAULT_PIECE_GRAM) to true // 兜底估算
    }

    /** 汇总一道菜的营养。inputs 为该菜所有配料。 */
    fun dishNutrition(inputs: List<NutritionInput>): DishNutrition {
        var totals = NutritionTotals.EMPTY
        var covered = 0
        var estimated = false
        inputs.forEach { input ->
            val n = input.nutrition
            if (n == null || !n.hasAny) return@forEach
            covered++
            val (grams, est) = resolveGrams(input)
            if (grams == null) return@forEach // 有营养但用量缺失：无法折算，跳过累加(仍计入 covered，表示“知道这个料但算不出量”)
            if (est) estimated = true
            val f = grams / 100.0
            totals = totals + NutritionTotals(
                energyKcal = (n.energyKcal ?: 0.0) * f,
                proteinG = (n.proteinG ?: 0.0) * f,
                fatG = (n.fatG ?: 0.0) * f,
                carbG = (n.carbG ?: 0.0) * f,
                fiberG = (n.fiberG ?: 0.0) * f,
                sodiumMg = (n.sodiumMg ?: 0.0) * f,
                potassiumMg = (n.potassiumMg ?: 0.0) * f,
                calciumMg = (n.calciumMg ?: 0.0) * f,
                purineMg = (n.purineMg ?: 0.0) * f,
            )
        }
        return DishNutrition(
            totals = totals,
            ingredientCount = inputs.size,
            coveredCount = covered,
            estimated = estimated,
        )
    }

    /** 汇总多道菜(一餐/一天)的营养总量。 */
    fun sumTotals(list: List<NutritionTotals>): NutritionTotals =
        list.fold(NutritionTotals.EMPTY) { acc, t -> acc + t }
}
