package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.CalorieStatus
import com.sxdbsm.cookbook.domain.model.NutritionTotals
import kotlin.math.roundToInt

/**
 * @File : NutritionLevel
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 营养级别综合评估——在结构多样性级别之上，叠加「热量达标 + 关键慢病指标」下调
 * <p>
 * 修「营养级别只看多样性→偏咸/超量也显绿」的误导。口径与阈值见 `feature/营养级别评级方案.md`、
 * 「膳食参考依据」页(钠每日上限、热量±15% 达标带)。**缺营养数据→退回多样性级别、不下调**(向后兼容)。
 * 全部提示挂「仅供参考·非医嘱」由 UI 承接。P1 只上热量+钠(数据齐)；GI/嘌呤/脂肪待逐项数据齐再扩展。
 * <p>
 * [AI生成] 营养级别评级方案落地：纯函数、可单测；慢病只对已登记病种触发。
 **/

/** 慢病类型（决定触发哪些指标）。[AI生成] */
enum class HealthCondition {
    HYPERTENSION, // 高血压 → 钠
    DIABETES, // 糖尿病 → GI/添加糖(待数据)
    GOUT, // 痛风/高尿酸 → 嘌呤(待数据)
    HYPERLIPIDEMIA, // 高血脂 → 饱和脂肪/胆固醇(待数据)
    ;

    companion object {
        /** 病种分类名 → 慢病类型(按关键词，兼容"三高/高血压/糖尿病/痛风/高尿酸/高血脂/高胆固醇")。[AI生成] */
        fun fromCareName(name: String): Set<HealthCondition> {
            val s = mutableSetOf<HealthCondition>()
            if (name.contains("血压")) s += HYPERTENSION
            if (name.contains("血糖") || name.contains("糖尿")) s += DIABETES
            if (name.contains("痛风") || name.contains("尿酸")) s += GOUT
            if (name.contains("血脂") || name.contains("胆固醇")) s += HYPERLIPIDEMIA
            if (name.contains("三高")) { s += HYPERTENSION; s += DIABETES; s += HYPERLIPIDEMIA }
            return s
        }
    }
}

/** 评估结果：级别 + 逐条提示(如"偏咸·高血压留意")。[AI生成] */
data class NutritionAssessment(val level: Int, val concerns: List<String>)

object NutritionLevelEvaluator {

    // 每日钠限量基准(mg)，见「膳食参考依据」：一般 2000(膳食指南/NRV)、高血压 2400(高血压指南2018)。
    const val SODIUM_DAILY_MG = 2000.0
    const val SODIUM_HYPERTENSION_MG = 2400.0

    // 占比阈值：≥WARN→注意(下调更多)、≥MID→中(略下调)。见方案，可拍板调整。
    const val WARN_RATIO = 1.0
    const val MID_RATIO = 0.7

    /**
     * 综合评级：以结构多样性 [baseLevel] 为底，按热量达标 + 慢病指标下调，产出级别+提示。[AI生成]
     *
     * @param baseLevel 结构多样性级别 0~4（[FoodGroup.nutritionLevel]）
     * @param totals 该食用者份额后的摄入营养合计；null 或热量≤0 视为"无营养数据"→原样返回 baseLevel、无提示
     * @param calorieStatus 热量达标状态；null=无目标(不判热量)
     * @param conditions 该食用者/家庭已登记的慢病(空=不触发任何慢病指标)
     * @param dayCount 累计天数(单日=1；多日窗口按 dayCount×每日限量)
     */
    fun evaluate(
        baseLevel: Int,
        totals: NutritionTotals?,
        calorieStatus: CalorieStatus?,
        conditions: Set<HealthCondition>,
        dayCount: Int = 1,
    ): NutritionAssessment {
        if (totals == null || totals.energyKcal <= 0.0) return NutritionAssessment(baseLevel, emptyList())
        var level = baseLevel
        val concerns = mutableListOf<String>()
        val days = dayCount.coerceAtLeast(1)

        // 钠(高血压)：整份/整日钠对比每日上限占比。
        if (HealthCondition.HYPERTENSION in conditions && totals.sodiumMg > 0.0) {
            val ratio = totals.sodiumMg / (SODIUM_HYPERTENSION_MG * days)
            when {
                ratio >= WARN_RATIO -> {
                    level = minOf(level, 2)
                    concerns += "偏咸（钠约${(ratio * 100).roundToInt()}%上限）· 高血压留意"
                }
                ratio >= MID_RATIO -> {
                    level = minOf(level, 3)
                    concerns += "钠偏高 · 高血压注意少盐"
                }
            }
        }

        // 热量：明显超标下调(达标带 ±15% 见 CalorieTarget)。偏低不算"不健康"，不下调。
        if (calorieStatus == CalorieStatus.ABOVE) {
            level = minOf(level, 2)
            concerns += "热量超标"
        }

        return NutritionAssessment(level, concerns)
    }
}
