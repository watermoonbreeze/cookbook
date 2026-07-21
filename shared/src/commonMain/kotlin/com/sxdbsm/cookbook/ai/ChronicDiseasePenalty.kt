package com.sxdbsm.cookbook.ai

/**
 * @File : ChronicDiseasePenalty
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 慢病数值软降罚分公式（高GI/高嘌呤主料命中→轻度靠后）·纯函数·单一真相源
 * <p>
 * 把单餐 [HealthRuleEngine.evaluate] 与周计划 [PeriodPlanner.score] 里语义/常量完全相同的
 * "高GI+高嘌呤命中→梯度软降"罚分基数抽成唯一真相源，杜绝两处各改各的调参漂移（同 [MealCompositionScorer] 思路）。
 * 只出**未乘风格权重**的罚分基数[0,PENALTY_CAP]，各调用方再乘各自的 chronicDiseaseNutrition 权重。
 * 门禁（仅偏营养风格 + 登记糖尿病/痛风）与命中计算（[com.sxdbsm.cookbook.domain.NutritionLevelEvaluator.dishQualitativeHits]）
 * 仍在各调用方，本对象只统一"命中数→罚多少"这一段公式。
 * <p>
 * [AI生成] D4 周计划接入慢病软降·技术债收敛：与单餐同口径，防漂移。
 **/
object ChronicDiseasePenalty {
    /** 每命中一味高GI/高嘌呤主料的罚分步长。[AI生成] */
    const val HIT_STEP = 0.25

    /** 每个维度(GI / 嘌呤)最多计入的命中味数（防一道多料的菜被线性重罚）。[AI生成] */
    const val HITS_PER_DIM_CAP = 2

    /** 整因子罚分基数封顶（弱可感知、不反超核心信号）。[AI生成] */
    const val PENALTY_CAP = 0.7

    /**
     * 高GI/高嘌呤命中数 → 罚分基数[0,PENALTY_CAP]（**未乘风格权重**）。[AI生成]
     * 每维度封顶 [HITS_PER_DIM_CAP] 味 × [HIT_STEP]，两维相加后整体封顶 [PENALTY_CAP]。
     *
     * @param highGiCount 高GI主料命中数（调用方已按病种 gate + 去重后传入）
     * @param highPurineCount 高嘌呤主料命中数（同上）
     */
    fun penaltyBase(highGiCount: Int, highPurineCount: Int): Double =
        ((minOf(highGiCount, HITS_PER_DIM_CAP) + minOf(highPurineCount, HITS_PER_DIM_CAP)) * HIT_STEP)
            .coerceAtMost(PENALTY_CAP)
}
