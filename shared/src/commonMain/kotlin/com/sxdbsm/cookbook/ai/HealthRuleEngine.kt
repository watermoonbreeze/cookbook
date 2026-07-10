package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.HealthConstraints
import com.sxdbsm.cookbook.ai.model.IngredientRole
import com.sxdbsm.cookbook.ai.model.RuleDish

/**
 * @File : HealthRuleEngine
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 推荐下一餐的规则引擎（纯函数，正确性由代码保证，不依赖模型）
 * <p>
 * 在把候选交给模型之前先做硬筛：可做性(主料齐) + 犯忌过滤(含 avoid 剔除) + 去重降权 + 打分。
 * 即使模型完全失效，这一层也能纯规则出一版安全推荐。忌口由此强校验，模型无权绕过。
 * <p>
 * [AI生成] S0 核心：先立规则与可测逻辑，模型层(S1)只在本引擎产出的安全集里做组合与解释。
 **/
class HealthRuleEngine {

    /**
     * 评估菜品库，产出安全可做的候选（降序）。[AI生成]
     *
     * @param dishes 菜品库（食材已按角色标好）
     * @param pantryIngredientIds 在手食材 id
     * @param constraints 健康硬约束（忌口/限量）
     * @param recentDishIds 最近吃过的菜 id（去重降权）
     */
    fun evaluate(
        dishes: List<RuleDish>,
        pantryIngredientIds: Set<Long>,
        constraints: HealthConstraints,
        recentDishIds: Set<Long> = emptySet(),
        shortageIngredientIds: Set<Long> = emptySet(), // [AI生成] 可用份数≤0 的在库食材
    ): List<DishCandidate> = dishes.mapNotNull { dish ->
        val nonSeasoning = dish.ingredients.filter { it.role != IngredientRole.SEASONING }
        val seasonings = dish.ingredients.filter { it.role == IngredientRole.SEASONING }
        // [AI修改] 方案A''(物尽其用)：只要该菜用到**至少一个在手的非调料食材**就推荐，
        // 缺的非调料(主料/辅料)全部列出让用户看到缺什么、自行选择；齐备的排前、缺得多的排后。
        // 调料常备不计入(避免"有盐就推所有菜")。满足用户"库存有的都要用上、少什么也要列出来"。
        val onHandNonSeasoning = nonSeasoning.filter { it.ingredientId in pantryIngredientIds }
        if (onHandNonSeasoning.isEmpty()) return@mapNotNull null
        // 缺的非调料食材(不在库)=还需采购的主料/辅料，全部列出。
        val missingNames = nonSeasoning.filter { it.ingredientId !in pantryIngredientIds }.map { it.name }
        val onHandNames = onHandNonSeasoning.map { it.name } // 用到你库存的食材(非调料)

        // 犯忌：含任一 avoid 食材 → 直接剔除（硬约束）。
        val hasAvoid = dish.ingredients.any { it.ingredientId in constraints.avoidIngredientIds }
        if (hasAvoid) return@mapNotNull null

        val limitHits = dish.ingredients.filter { it.ingredientId in constraints.limitIngredientIds }
        val recommendHits = dish.ingredients.filter { it.ingredientId in constraints.recommendIngredientIds }
        val seasoningsOnHand = seasonings.filter { it.ingredientId in pantryIngredientIds }
        val isRecent = dish.id in recentDishIds

        // 打分：基础 + 调养推荐加分(利健康靠前) + 在手调料丰富度 - 限量(不利靠后) - 最近吃过。
        val seasoningRichness = if (seasonings.isEmpty()) 0.0 else seasoningsOnHand.size.toDouble() / seasonings.size
        // [AI生成] 库存不足：非调料食材可用份数≤0 → 仍推荐但大幅降权排到最后，并记录短料名。
        val shortageNames = nonSeasoning.filter { it.ingredientId in shortageIngredientIds }.map { it.name }

        var score = BASE_SCORE + SEASONING_WEIGHT * seasoningRichness
        score += RECOMMEND_BONUS * recommendHits.size
        score -= LIMIT_PENALTY * limitHits.size
        if (isRecent) score -= RECENT_PENALTY
        if (shortageNames.isNotEmpty()) score -= SHORTAGE_PENALTY // 短料菜排到充足菜之后
        if (missingNames.isNotEmpty()) score -= MISSING_PENALTY * missingNames.size // 缺辅料的菜排到齐备菜之后

        DishCandidate(
            id = dish.id,
            name = dish.name,
            mainNames = dish.ingredients.filter { it.role == IngredientRole.MAIN }.map { it.name },
            secondaryNames = dish.ingredients.filter { it.role == IngredientRole.SECONDARY }.map { it.name },
            seasoningsOnHand = seasoningsOnHand.map { it.name },
            limitHits = limitHits.map { it.name },
            recommendHits = recommendHits.map { it.name },
            isRecent = isRecent,
            score = score,
            shortageNames = shortageNames,
            missingNames = missingNames,
            onHandNames = onHandNames,
        )
    }.sortedByDescending { it.score }

    companion object {
        private const val BASE_SCORE = 1.0
        private const val SEASONING_WEIGHT = 0.5 // 在手调料越全，可做的做法越丰富，略加分。
        private const val RECOMMEND_BONUS = 0.6 // [AI生成] 每个调养推荐食材的加分(利健康的菜靠前)。
        private const val RECENT_PENALTY = 0.5 // 最近吃过降权，鼓励多样性。
        private const val LIMIT_PENALTY = 0.4 // [AI修改] 每个限量食材的降权(不利健康的菜靠后)。
        private const val SHORTAGE_PENALTY = 100.0 // [AI生成] 库存不足菜大幅降权，排到所有充足菜之后(仍保留推荐)。
        private const val MISSING_PENALTY = 50.0 // [AI生成] 缺辅料(需采购)每味降权，让食材齐备的菜排在前面(仍保留推荐)。
    }
}
