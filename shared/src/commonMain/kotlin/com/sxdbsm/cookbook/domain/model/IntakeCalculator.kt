package com.sxdbsm.cookbook.domain.model

/**
 * @File : IntakeCalculator
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 个人摄入折算的单一真相源(整份 × 食用比例 × 成员份额)
 * <p>
 * 三个正交维度(会商拍板·见 `食用比例吃完度_摄入会商方案.md`)：
 *  - dishTotals  = 这道菜"整份"(全家做出来的总量)的营养(绝对值)。
 *  - eatenRatio  = 这道菜这一餐**实际吃掉**的比例[0,1]，默认 1.0=吃完(meal_record_dish.eaten_ratio)。
 *  - share       = 吃掉的部分里该成员**分到**多少[0,1]，成员饭量系数归一(family_member.portion_coefficient)。
 * 公式：个人某餐摄入 = Σ_菜[ dishTotals × eatenRatio ] × share。三维乘法可交换、share 归一不受 eatenRatio 影响
 *   (全家个人摄入之和 = Σ dishTotals×eatenRatio，守恒·见单测)。
 * <p>
 * 今日卡 / 饮食报告 / 趋势折线 / 达标天 **全部复用本对象**，禁各处各乘(抽共享防调参/口径漂移·同 MealCompositionScorer)。
 * <p>
 * [AI生成] 食用比例(是否吃完)维度落地·摄入折算单一真相源。
 **/
object IntakeCalculator {

    /** 单道菜的"实吃"营养 = 整份 × 食用比例。ratio 强制 coerceIn(0,1) 防脏值放大(踩坑红线:>1 天价/负值负营养)。[AI生成] */
    fun eatenPortion(dishTotals: NutritionTotals, eatenRatio: Double): NutritionTotals =
        dishTotals * eatenRatio.coerceIn(0.0, 1.0)

    /**
     * 一餐/一天多道菜的个人摄入 = Σ(整份×食用比例) × 成员份额。[AI生成]
     *
     * @param dishes (菜整份营养, 该菜食用比例) 列表；缺营养的菜传 EMPTY。
     * @param share  成员份额[0,1]。
     */
    fun personalIntake(dishes: List<Pair<NutritionTotals, Double>>, share: Double): NutritionTotals {
        val eaten = dishes.fold(NutritionTotals.EMPTY) { acc, (totals, ratio) -> acc + eatenPortion(totals, ratio) }
        return eaten * share
    }
}
