package com.sxdbsm.cookbook.ai

/**
 * @File : TasteProfile
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 口味画像——从用户历史吃过的菜聚合出的「菜系/做法/主料」偏好频次，给"合口味"的候选加分
 * <p>
 * 纯本地统计（不调 AI、不涉隐私上云）：统计一段历史里各菜系/做法/主料出现次数 → 归一为偏好强度 →
 * 候选菜与画像的匹配度 [0,1]。数据越多越准、随记录成长；空画像→匹配分恒 0（因子中性、向后兼容）。
 * 方案见 .ai-context/docs/feature/推荐算法深挖分析.md（算法3项·口味画像）。
 * <p>
 * [AI生成] 增长型本地推荐算法：口味画像因子（学菜系+做法+主料偏好）。
 **/
data class TasteProfile(
    val cuisineFreq: Map<String, Int> = emptyMap(), // 历史菜系 → 次数（空菜系已在取数层剔除）
    val methodFreq: Map<String, Int> = emptyMap(),  // 历史做法 → 次数
    val mainFreq: Map<String, Int> = emptyMap(),    // 历史主料名 → 次数（仅 is_main；匹配时候选侧主料已排除调料，误标调料匹配不到候选故无影响）
) {
    // 各维最高频次（归一化分母）。取自本画像自身，保证"最常吃的那个"=满偏好 1.0。
    private val maxCuisine = cuisineFreq.values.maxOrNull() ?: 0
    private val maxMethod = methodFreq.values.maxOrNull() ?: 0
    private val maxMain = mainFreq.values.maxOrNull() ?: 0

    /** 无任何历史信号 → 画像为空：matchScore 恒 0，口味因子中性（向后兼容）。[AI生成] */
    val isEmpty: Boolean get() = maxCuisine == 0 && maxMethod == 0 && maxMain == 0

    /**
     * 候选菜与口味画像的匹配度 [0,1]。[AI生成]
     *
     * 每维按「该值频次 / 该维最高频次」归一（最常吃的菜系/做法/主料 = 1.0，从没吃过 = 0）；
     * 做法/主料取候选中**匹配最强的一味**（max，不因多主料/多做法被平均稀释）。
     * 加权：菜系 0.4 + 做法 0.3 + 主料 0.3——菜系是口味最粗的整体信号（爱吃川菜/清淡），做法/主料细化。
     * 某维在画像里无数据（分母 0）则该维不贡献（自然退化，不误伤）。
     *
     * @param cuisine 候选菜系（空串=未分类，不计菜系维）
     * @param methods 候选做法名集合
     * @param mains   候选主料名集合（调料应已由调用方排除）
     */
    fun matchScore(cuisine: String, methods: List<String>, mains: List<String>): Double {
        if (isEmpty) return 0.0
        val cuisineAffinity = if (maxCuisine == 0 || cuisine.isBlank()) 0.0
            else (cuisineFreq[cuisine] ?: 0).toDouble() / maxCuisine
        val methodAffinity = if (maxMethod == 0) 0.0
            else methods.maxOfOrNull { (methodFreq[it] ?: 0).toDouble() / maxMethod } ?: 0.0
        val mainAffinity = if (maxMain == 0) 0.0
            else mains.maxOfOrNull { (mainFreq[it] ?: 0).toDouble() / maxMain } ?: 0.0
        return (W_CUISINE * cuisineAffinity + W_METHOD * methodAffinity + W_MAIN * mainAffinity)
            .coerceIn(0.0, 1.0)
    }

    companion object {
        val EMPTY = TasteProfile()
        private const val W_CUISINE = 0.4 // 菜系：口味最粗整体信号，主导
        private const val W_METHOD = 0.3  // 做法：细化（爱清蒸/爱红烧）
        private const val W_MAIN = 0.3    // 主料：细化（爱吃某类主料的菜）
    }
}
