package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.HealthConstraints

/**
 * @File : RecommendationPrompt
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 推荐下一餐的中文 Prompt 构造（纯函数，可测）
 * <p>
 * 只把规则筛过的安全候选喂给模型；模型只能从候选 id 里挑搭 2~3 菜一餐、按在手调料给做法。
 * 强约束输出严格 JSON，便于 schema 校验。敏感健康数据不进 prompt，只给粗约束标签。
 * <p>
 * [AI生成] S1：把候选与约束翻译成模型能稳定产出 JSON 的提示。
 **/
object RecommendationPrompt {

    fun build(
        candidates: List<DishCandidate>,
        constraints: HealthConstraints,
        mealCount: Int,
        // [AI生成] 3a：把用户"推荐风格"与画像信号(常做/补营养)带给模型，让云端也对齐个性化。
        style: RecommendationStyle = RecommendationStyle.DEFAULT,
        preferenceScores: Map<Long, Double> = emptyMap(),
        nutritionBalanceScores: Map<Long, Double> = emptyMap(),
    ): LlmRequest {
        val system = buildString {
            append("你是家庭饮食助手，帮慢性病用户用现有食材决定下一餐吃什么。")
            append("只能从给定候选菜里挑选，不能编造候选之外的菜。")
            append("每一餐搭配 2~3 个菜，共给 $mealCount 个不同的餐。")
            append("根据每个菜在手的调料，给一句简短做法建议（有葱姜蒜酱→红烧/爆炒，只有盐油→清蒸/白灼）。")
            append("优先选择标注「利调养」的菜、尽量少选标注「注意限量」的菜（候选已按利于健康排序，靠前更优）。")
            // [AI生成] 3a：推荐风格(用户轻干预)。
            append(
                when (style) {
                    RecommendationStyle.FAMILIAR -> "用户偏好『熟悉』：多选标「常做」的家常菜。"
                    RecommendationStyle.FRESH -> "用户偏好『新鲜』：多换口味、少重复，优先近期没吃的。"
                    RecommendationStyle.NUTRITION -> "用户偏好『营养』：优先标「补营养」的菜，让这餐更均衡、利健康。"
                    RecommendationStyle.BALANCED -> "综合权衡口味、营养与多样性。"
                },
            )
            if (constraints.labels.isNotEmpty()) {
                append("用户有健康档案，请优先遵循《中国居民膳食指南》等权威膳食建议（如三高少盐少油、痛风低嘌呤、糖尿病低GI），")
                append("并严格遵守候选上标注的「利调养/注意限量」——这些是硬约束，不得违背。")
            }
            // [AI生成] R5(免责红线)：约束模型理由/做法建议不得产生医疗断言、不承诺疗效。
            append("理由只谈食材搭配、口味、是否用到现有食材，不得出现『降压/降糖/降脂/治疗/根治/达标/包治』等医疗断言，也不承诺疗效；涉及健康只说『较清淡/少油少盐』这类做法层面的话。")
            append("严格输出 JSON，不要多余文字。")
        }
        val user = buildString {
            if (constraints.labels.isNotEmpty()) {
                append("健康约束：").append(constraints.labels.joinToString("、")).append("。\n")
            }
            append("候选菜（只能用这些 id）：\n")
            candidates.forEach { c ->
                append("- id=").append(c.id).append(" ").append(c.name)
                append("｜主料:").append(c.mainNames.joinToString("、").ifEmpty { "-" })
                if (c.onHandNames.isNotEmpty()) append("｜在手:").append(c.onHandNames.joinToString("、")) // [AI生成] 用到你库存的食材(物尽其用)
                if (c.seasoningsOnHand.isNotEmpty()) append("｜在手调料:").append(c.seasoningsOnHand.joinToString("、"))
                if (c.recommendHits.isNotEmpty()) append("｜利调养:").append(c.recommendHits.joinToString("、"))
                if ((preferenceScores[c.id] ?: 0.0) >= 0.5) append("｜常做") // [AI生成] 3a：偏好画像高=常做/爱吃
                if ((nutritionBalanceScores[c.id] ?: 0.0) > 0.0) append("｜补营养") // [AI生成] 3a：能补近期缺的宏量
                c.recentDaysAgo?.let { d -> append(if (d <= 0) "｜今天吃过" else "｜${d}天前吃过") } // [AI生成] R2：近吃标签，帮模型主动避开近期吃过的(换口味)；d≤0 归"今天"(防御非正数)
                if (c.missingNames.isNotEmpty()) append("｜还差:").append(c.missingNames.joinToString("、")) // [AI生成] 缺的主料/辅料，供模型措辞提示
                if (c.limitHits.isNotEmpty()) append("｜注意限量:").append(c.limitHits.joinToString("、"))
                append("\n")
            }
            append("\n只输出如下 JSON：\n")
            append("""{"suggestions":[{"dishIds":[菜id,...],"reason":"一句理由","cookingHint":"做法建议"}]}""")
        }
        return LlmRequest(system = system, user = user)
    }
}
