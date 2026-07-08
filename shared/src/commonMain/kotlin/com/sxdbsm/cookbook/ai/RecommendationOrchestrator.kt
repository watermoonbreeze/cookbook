package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.MealSuggestion
import com.sxdbsm.cookbook.ai.model.RecommendationInput
import com.sxdbsm.cookbook.ai.model.RecommendationResult
import com.sxdbsm.cookbook.ai.model.RecommendationSource

/**
 * @File : RecommendationOrchestrator
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 推荐下一餐编排：规则筛 → 模型组合 → 校验 → 兜底
 * <p>
 * 先用 HealthRuleEngine 筛出安全候选，再让模型只在候选里搭 2~3 菜的组合；模型输出经解析+校验
 * (dishId 必须来自候选)才采用，任何环节失败都回退到纯规则兜底。忌口由规则层强校验，模型无权绕过。
 * <p>
 * [AI生成] S1：面向 AiRuntime 接口编排，Mock/云端/端侧可换。取数由调用方用 RecommendationDataSource 提供。
 **/
class RecommendationOrchestrator(
    private val runtime: AiRuntime,
    private val engine: HealthRuleEngine = HealthRuleEngine(),
) {
    /**
     * 生成推荐。[AI生成]
     *
     * @param input 取数层聚合的输入（RecommendationDataSource.gather()）
     * @param mealCount 推荐几个不同的餐
     */
    suspend fun recommend(input: RecommendationInput, mealCount: Int = DEFAULT_MEAL_COUNT): RecommendationResult {
        val candidates = engine.evaluate(input.dishes, input.pantryIngredientIds, input.constraints, input.recentDishIds)
        if (candidates.isEmpty()) {
            return RecommendationResult(emptyList(), candidates, RecommendationSource.EMPTY)
        }

        val prompt = RecommendationPrompt.build(candidates, input.constraints, mealCount)
        val raw = runCatching { runtime.complete(prompt) }.getOrNull()?.getOrNull()
        val modelSuggestions = raw
            ?.let { RecommendationParser.parse(it) }
            ?.let { validate(it, candidates, mealCount) }
            ?.takeIf { it.isNotEmpty() }

        return if (modelSuggestions != null) {
            RecommendationResult(modelSuggestions, candidates, RecommendationSource.MODEL)
        } else {
            RecommendationResult(fallback(candidates, mealCount), candidates, RecommendationSource.RULE_FALLBACK)
        }
    }

    /** 校验模型输出：dishId 必须来自候选集、每餐 1~3 菜、截到 mealCount。[AI生成] */
    private fun validate(
        suggestions: List<MealSuggestion>,
        candidates: List<DishCandidate>,
        mealCount: Int,
    ): List<MealSuggestion> {
        val validIds = candidates.map { it.id }.toSet()
        return suggestions
            .map { it.copy(dishIds = it.dishIds.filter { id -> id in validIds }.distinct()) }
            .filter { it.dishIds.size in 1..MAX_DISHES_PER_MEAL }
            .take(mealCount)
    }

    /** 纯规则兜底：把规则 top 候选按每餐 2 菜切成 mealCount 餐。[AI生成] */
    private fun fallback(candidates: List<DishCandidate>, mealCount: Int): List<MealSuggestion> =
        candidates.take(mealCount * FALLBACK_DISHES_PER_MEAL)
            .chunked(FALLBACK_DISHES_PER_MEAL)
            .take(mealCount)
            .map { chunk ->
                MealSuggestion(
                    dishIds = chunk.map { it.id },
                    reason = "用你现有食材可做：" + chunk.joinToString("、") { it.name },
                    cookingHint = chunk.firstOrNull()?.seasoningsOnHand
                        ?.takeIf { it.isNotEmpty() }
                        ?.let { "在手调料：" + it.joinToString("、") },
                )
            }

    companion object {
        private const val DEFAULT_MEAL_COUNT = 3
        private const val MAX_DISHES_PER_MEAL = 3
        private const val FALLBACK_DISHES_PER_MEAL = 2
    }
}
