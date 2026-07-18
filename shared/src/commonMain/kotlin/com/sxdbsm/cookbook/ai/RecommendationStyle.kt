package com.sxdbsm.cookbook.ai

/**
 * @File : RecommendationStyle
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 增长型推荐的打分权重表 + 推荐风格预设（用户轻干预）
 * <p>
 * 打分因子权重集中在 [RecommendationWeights]；[RecommendationStyle] 是一组权重预设，
 * 底层因子不变，只切各因子系数（类音乐 App 的“综合/偏熟悉/偏新鲜”）。默认“综合”=现有行为。
 * 方案见 .ai-context/docs/feature/增长型本地推荐算法.md。
 * <p>
 * [AI生成] 增长型本地推荐算法 P1：权重框架 + 风格预设。
 **/

/**
 * 推荐打分各因子权重。默认值 = HealthRuleEngine 现有硬编码常量（保证默认行为不变）。[AI生成]
 *
 * 加分项为正、罚分项为正值（引擎里做减法）。营养搭配/偏好为 P1 新增因子，无数据时贡献 0。
 */
data class RecommendationWeights(
    val base: Double = 1.0,
    val onHandMain: Double = 1.0,       // 在手主料/味（物尽其用核心）
    val seasoning: Double = 0.5,        // 在手调料丰富度占比
    val recommend: Double = 0.6,        // 调养推荐食材/个
    val limit: Double = 0.4,            // 限量食材/个（罚）
    val recent: Double = 0.5,           // 最近吃过（罚）
    val shortage: Double = 0.3,         // 库存不足（罚）
    val missing: Double = 0.2,          // 缺辅料/味（罚）
    val avoid: Double = 5.0,            // [AI修改] 忌口(罚)。排末由 sortedWith 分层保证,此值不再需-50巨量;降到象征值让 score 回到可比可解释区间(算法评审P0)。
    // ↓ P1 新增因子（无画像数据时为 0，不影响现有行为）
    val nutritionBalance: Double = 0.8, // 与当日/本餐已选的营养互补度（[-1,1]）
    val preference: Double = 0.6,       // 偏好画像分（[0,1]：爱吃/常做/收藏）
    val mainRepeat: Double = 0.3,       // 近期同主料重复/次（罚）
    // [AI生成] 慢病数值软约束(GI/嘌呤)：登记糖尿病/痛风家庭，高GI/高嘌呤主料的菜在正常层内轻度靠后(不剔除、不进忌口分层)。
    //   **默认 0=不生效**(多角色验证:软降弱可感知,静默默认开有操纵感→仅"偏营养"风格才开=用户主动知情同意)。钠不做(每道菜都放盐会误伤全部)。
    val chronicDiseaseNutrition: Double = 0.0, // 高GI/高嘌呤梯度罚(仅登记病种+营养风格生效)
    // [AI生成] 口味画像因子(菜系/做法/主料偏好匹配[0,1])：默认 0，各风格按需开(见 weights())。无历史→匹配分0→中性。
    val tasteProfile: Double = 0.0,
    // [AI生成] 时间衰减开关(仅"偏新鲜"开)：为 true 时 preference(常做)加分随"距上次做天数"指数衰减，久没做的老菜常做加分递减，推荐不固化在老菜。
    val decayPreferenceByStaleness: Boolean = false,
) {
    companion object {
        val DEFAULT = RecommendationWeights()
    }
}

/**
 * 推荐风格（用户轻干预）。[AI生成]
 *
 * 每个风格映射一组权重系数；默认 [BALANCED] 与现有行为一致。存 PreferenceKeys.RECOMMEND_STYLE。
 */
enum class RecommendationStyle {
    /** 综合：各因子均衡（默认，行为同现有）。 */
    BALANCED,

    /** 偏熟悉：多推爱吃/常做——↑偏好、↓最近惩罚。 */
    FAMILIAR,

    /** 偏新鲜：多推久未吃/尝新——↑最近与主料重复惩罚、↓偏好。 */
    FRESH,

    /** 偏营养：更冲着均衡达标——↑营养搭配与调养推荐。 */
    NUTRITION,
    ;

    /** 该风格对应的权重（在默认权重上按风格调系数）。[AI生成] */
    fun weights(): RecommendationWeights = when (this) {
        // [AI修改] 综合：新增中等口味画像加成(合口味的菜靠前)，其余因子同现有；无历史时 taste 分为0→行为不变。
        BALANCED -> RecommendationWeights.DEFAULT.copy(
            tasteProfile = 0.4, // 中等：合口味加分与 recommend/preference 同量级，不碾压物尽其用
        )
        FAMILIAR -> RecommendationWeights.DEFAULT.copy(
            preference = 1.2,   // ×2 更看重爱吃/常做
            recent = 0.2,       // ×0.4 弱化去重
            mainRepeat = 0.1,
            tasteProfile = 0.6, // [AI生成] 偏熟悉=最看重合口味
        )
        FRESH -> RecommendationWeights.DEFAULT.copy(
            recent = 1.0,       // ×2 更强去重
            mainRepeat = 0.6,   // ×2 抑制同主料
            preference = 0.18,  // ×0.3 弱化熟悉偏好
            tasteProfile = 0.2, // [AI生成] 偏新鲜=口味画像最弱(仍保菜系倾向但不过度锚定熟悉口味)
            decayPreferenceByStaleness = true, // [AI生成] 时间衰减:久没做的常做菜preference递减→不固化在老菜(仅此风格开)
        )
        NUTRITION -> RecommendationWeights.DEFAULT.copy(
            nutritionBalance = 1.28, // ×1.6 更看重营养互补
            recommend = 0.9,         // ×1.5 更看重调养推荐
            chronicDiseaseNutrition = 0.6, // [AI生成] 偏营养=用户主动求健康→开启慢病数值软降(高GI/嘌呤菜轻度靠后);其余风格默认0不动
            tasteProfile = 0.3, // [AI生成] 偏营养=口味让位营养,画像加成偏低
        )
    }

    /**
     * 批内多样性重排强度 λ（MMR）。[AI生成] 算法评审#3.1：只有"跨天硬去重"缺"同一批列表内多样性"，
     * 易整批同簇（库存五花肉→满屏五花肉菜）。λ=1.0 关（纯分数序），越小越打散主料。
     * **四风格全开**（用户诉求：不论熟不熟悉都不想同主料霸屏、都要多样性），仅强度不同：
     * 偏熟悉最温和（只破"同主料连排"、仍保熟悉高分）→ 综合 → 偏营养 → 偏新鲜最强打散。
     */
    fun diversityLambda(): Double = when (this) {
        FRESH -> 0.6      // 尝新：强打散
        NUTRITION -> 0.75 // 偏营养：主料多样也利均衡
        BALANCED -> 0.8   // 综合：温和
        FAMILIAR -> 0.85  // 偏熟悉：最温和，只破同主料连排
    }

    companion object {
        val DEFAULT = BALANCED

        /** 从偏好存储的字符串解析(容错回默认)。[AI生成] */
        fun fromKey(key: String?): RecommendationStyle =
            entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
