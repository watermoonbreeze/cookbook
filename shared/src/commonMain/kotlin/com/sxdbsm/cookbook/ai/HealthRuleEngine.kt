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
    ): List<DishCandidate> = dishes.mapNotNull { dish ->
        val mains = dish.ingredients.filter { it.role == IngredientRole.MAIN }
        // 可做性：主料齐即可做；若菜没标主料，退化为“所有非调料齐”，避免空主料被判恒可做。
        val required = mains.ifEmpty { dish.ingredients.filter { it.role != IngredientRole.SEASONING } }
        val makeable = required.all { it.ingredientId in pantryIngredientIds }
        if (!makeable) return@mapNotNull null

        // 犯忌：含任一 avoid 食材 → 直接剔除（硬约束）。
        val hasAvoid = dish.ingredients.any { it.ingredientId in constraints.avoidIngredientIds }
        if (hasAvoid) return@mapNotNull null

        val limitHits = dish.ingredients.filter { it.ingredientId in constraints.limitIngredientIds }
        val secondary = dish.ingredients.filter { it.role == IngredientRole.SECONDARY }
        val secOnHand = secondary.filter { it.ingredientId in pantryIngredientIds }
        val secMissing = secondary.filter { it.ingredientId !in pantryIngredientIds }
        val isRecent = dish.id in recentDishIds

        // 打分：基础分 + 辅料齐度加分 - 最近吃过 - 限量项。
        val secondaryCoverage = if (secondary.isEmpty()) 1.0 else secOnHand.size.toDouble() / secondary.size
        var score = 1.0 + secondaryCoverage
        if (isRecent) score -= RECENT_PENALTY
        score -= LIMIT_PENALTY * limitHits.size

        DishCandidate(
            id = dish.id,
            name = dish.name,
            mainOnHand = required.map { it.name }, // 已保证在手
            secondaryOnHand = secOnHand.map { it.name },
            secondaryMissing = secMissing.map { it.name },
            limitHits = limitHits.map { it.name },
            isRecent = isRecent,
            score = score,
        )
    }.sortedByDescending { it.score }

    companion object {
        private const val RECENT_PENALTY = 0.5 // 最近吃过降权，鼓励多样性。
        private const val LIMIT_PENALTY = 0.3 // 每个限量食材的降权。
    }
}
