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

    fun build(candidates: List<DishCandidate>, constraints: HealthConstraints, mealCount: Int): LlmRequest {
        val system = buildString {
            append("你是家庭饮食助手，帮慢性病用户用现有食材决定下一餐吃什么。")
            append("只能从给定候选菜里挑选，不能编造候选之外的菜。")
            append("每一餐搭配 2~3 个菜，共给 $mealCount 个不同的餐。")
            append("根据每个菜在手的调料，给一句简短做法建议（有葱姜蒜酱→红烧/爆炒，只有盐油→清蒸/白灼）。")
            append("优先选择标注「利调养」的菜、尽量少选标注「注意限量」的菜（候选已按利于健康排序，靠前更优）。")
            if (constraints.labels.isNotEmpty()) {
                append("用户有健康档案，请优先遵循《中国居民膳食指南》等权威膳食建议（如三高少盐少油、痛风低嘌呤、糖尿病低GI），")
                append("并严格遵守候选上标注的「利调养/注意限量」——这些是硬约束，不得违背。")
            }
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
                if (c.seasoningsOnHand.isNotEmpty()) append("｜在手调料:").append(c.seasoningsOnHand.joinToString("、"))
                if (c.recommendHits.isNotEmpty()) append("｜利调养:").append(c.recommendHits.joinToString("、"))
                if (c.limitHits.isNotEmpty()) append("｜注意限量:").append(c.limitHits.joinToString("、"))
                append("\n")
            }
            append("\n只输出如下 JSON：\n")
            append("""{"suggestions":[{"dishIds":[菜id,...],"reason":"一句理由","cookingHint":"做法建议"}]}""")
        }
        return LlmRequest(system = system, user = user)
    }
}
