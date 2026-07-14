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
        recentDishDaysAgo: Map<Long, Int> = emptyMap(), // [AI生成] B2：去重窗口内吃过的菜→距今天数(用于标注"N天前吃过")
        // [AI生成] 增长型推荐 P1：权重(风格切换)与画像信号；默认均为空/中性→行为同现有。
        weights: RecommendationWeights = RecommendationWeights.DEFAULT,
        preferenceScores: Map<Long, Double> = emptyMap(), // 每菜偏好画像分[0,1]：爱吃/常做/收藏
        nutritionBalanceScores: Map<Long, Double> = emptyMap(), // 每菜与当日/本餐已选的营养互补度[-1,1]
        mainRepeatCounts: Map<Long, Int> = emptyMap(), // 每菜主料近期重复次数
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

        // [AI修改] 忌口(avoid)：不再直接剔除——改为「照样列出、排到最后、标红警示」。
        // 家庭 app：有慢病的成员应避免，但库存有的菜仍要让用户看到(家人也能做)，由用户自行判断，不替他隐藏。
        // [AI修改] 忌口/限量只算**非调料**食材：否则"盐对高血压忌口/生抽限量"会让几乎所有菜都命中(每道菜都放盐)，忌口失去意义。
        val avoidNames = nonSeasoning.filter { it.ingredientId in constraints.avoidIngredientIds }.map { it.name }.distinct()

        val limitHits = nonSeasoning.filter { it.ingredientId in constraints.limitIngredientIds }
        val recommendHits = nonSeasoning.filter { it.ingredientId in constraints.recommendIngredientIds }
        // [AI生成] 调料若命中忌口/限量 → 不判菜品忌口，而是转成做法提示(少盐/少糖/少油)，让用户少放而非不做这道菜。
        val cookingCautions = seasonings
            .filter { it.ingredientId in constraints.avoidIngredientIds || it.ingredientId in constraints.limitIngredientIds }
            .map { seasoningCaution(it.name) }
            .distinct()
        val seasoningsOnHand = seasonings.filter { it.ingredientId in pantryIngredientIds }
        // [AI修改] B2：优先用日期窗口的 recentDishDaysAgo 判定"最近吃过"；兼容旧调用(只传 recentDishIds)。
        val recentDaysAgo = recentDishDaysAgo[dish.id]
        val isRecent = recentDaysAgo != null || dish.id in recentDishIds

        // 打分：物尽其用为核心——用到在手主料强力提权 + 调养推荐 + 在手调料丰富度 - 限量 - 最近。
        val seasoningRichness = if (seasonings.isEmpty()) 0.0 else seasoningsOnHand.size.toDouble() / seasonings.size
        // [AI生成] 库存不足：非调料食材可用份数≤0 → 仍推荐，只轻微靠后并标短料名(份数不影响是否推荐)。
        val shortageNames = nonSeasoning.filter { it.ingredientId in shortageIngredientIds }.map { it.name }
        // [AI生成] 物尽其用核心信号：用到几味在手「主料」。用到在手主料的菜(如库存有五花肉→各种五花肉菜)强力提权，
        // 保证它们排到前面、进第一批，不被"缺辅料/份数不足"埋到看不见。
        val onHandMainCount = onHandNonSeasoning.count { it.role == IngredientRole.MAIN }

        // [AI修改] 因子化打分：权重来自 RecommendationWeights(默认=原常量、可由推荐风格切换)。
        var score = weights.base + weights.seasoning * seasoningRichness
        score += weights.onHandMain * onHandMainCount // 用到在手主料 → 强力靠前(物尽其用)
        score += weights.recommend * recommendHits.size
        score -= weights.limit * limitHits.size
        if (isRecent) score -= weights.recent
        // [AI修改] 份数不足/缺辅料只做「轻微靠后」的排序微调，不再是断崖式重罚——
        // 用户要求「不管库存有几份、只要在库存中，用到它的菜都要推出来」，故份数/缺料不得把菜挤出推荐。
        if (shortageNames.isNotEmpty()) score -= weights.shortage
        if (missingNames.isNotEmpty()) score -= weights.missing * missingNames.size
        // [AI生成] 增长型 P1 新因子(无画像数据时为 0，行为同现有)：
        //   营养搭配互补度[-1,1]、偏好画像[0,1]加分；近期同主料重复罚分。
        score += weights.nutritionBalance * (nutritionBalanceScores[dish.id] ?: 0.0)
        score += weights.preference * (preferenceScores[dish.id] ?: 0.0)
        score -= weights.mainRepeat * (mainRepeatCounts[dish.id] ?: 0)
        // [AI修改] 忌口菜大幅降权排到所有正常菜之后(仍保留、带 avoidNames 让 UI 标红警示)，让用户看得到但明确知道该避免。
        if (avoidNames.isNotEmpty()) score -= weights.avoid

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
            avoidNames = avoidNames,
            cookingCautions = cookingCautions,
            recentDaysAgo = recentDaysAgo,
            frequent = (preferenceScores[dish.id] ?: 0.0) >= 0.5, // [AI生成] 3b：常做/收藏
            complementary = (nutritionBalanceScores[dish.id] ?: 0.0) > 0.0, // [AI生成] 3b：补营养搭配
        )
    }
        // [AI修改] B2：分层排序保证"最近吃过的排到最后(但在忌口之前)、忌口最末"，不依赖罚分量级、稳健：
        //   非忌口非最近(正常) → 非忌口最近 → 忌口。层内按 score 降序。
        //   即：先看是否忌口(false 在前)，再看是否最近(false 在前)，最后 score 高者靠前。
        .sortedWith(compareBy({ it.avoidNames.isNotEmpty() }, { it.isRecent }, { -it.score }))

    /** 调料忌口/限量 → 做法提示。[AI生成] 盐→少盐、白糖→少糖、生抽/老抽/豉油→少酱油、各种油→少油、各种酱→少酱。 */
    private fun seasoningCaution(name: String): String = "少" + when {
        name.contains("糖") -> "糖"
        name.contains("盐") -> "盐"
        name == "生抽" || name == "老抽" || name.contains("酱油") || name.contains("豉油") -> "酱油"
        name.contains("油") -> "油"
        name.contains("酱") -> "酱"
        else -> name
    }

    // [AI修改] 打分权重(含默认值)已迁到 RecommendationWeights（支持推荐风格切换）；
    // 默认权重与原常量一致，行为不变。见 RecommendationStyle.kt / 增长型本地推荐算法.md。
}
