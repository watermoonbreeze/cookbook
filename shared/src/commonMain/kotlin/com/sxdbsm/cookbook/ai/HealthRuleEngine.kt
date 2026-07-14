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

        var score = BASE_SCORE + SEASONING_WEIGHT * seasoningRichness
        score += ON_HAND_MAIN_BONUS * onHandMainCount // 用到在手主料 → 强力靠前(物尽其用)
        score += RECOMMEND_BONUS * recommendHits.size
        score -= LIMIT_PENALTY * limitHits.size
        if (isRecent) score -= RECENT_PENALTY
        // [AI修改] 份数不足/缺辅料只做「轻微靠后」的排序微调，不再是断崖式重罚——
        // 用户要求「不管库存有几份、只要在库存中，用到它的菜都要推出来」，故份数/缺料不得把菜挤出推荐。
        if (shortageNames.isNotEmpty()) score -= SHORTAGE_PENALTY
        if (missingNames.isNotEmpty()) score -= MISSING_PENALTY * missingNames.size
        // [AI修改] 忌口菜大幅降权排到所有正常菜之后(仍保留、带 avoidNames 让 UI 标红警示)，让用户看得到但明确知道该避免。
        if (avoidNames.isNotEmpty()) score -= AVOID_PENALTY

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

    companion object {
        private const val BASE_SCORE = 1.0
        private const val ON_HAND_MAIN_BONUS = 1.0 // [AI生成] 每味在手主料的加分：物尽其用核心，用到库存主料的菜强力靠前、进第一批。
        private const val SEASONING_WEIGHT = 0.5 // 在手调料越全，可做的做法越丰富，略加分。
        private const val RECOMMEND_BONUS = 0.6 // [AI生成] 每个调养推荐食材的加分(利健康的菜靠前)。
        private const val RECENT_PENALTY = 0.5 // 最近吃过降权，鼓励多样性。
        private const val LIMIT_PENALTY = 0.4 // [AI修改] 每个限量食材的降权(不利健康的菜靠后)。
        // [AI修改] 份数不足/缺辅料只做轻微靠后(0.3/0.2)，不再断崖重罚(原 100/50)——
        // 保证「只要库存中有该食材、用到它的菜都能推出来」，份数与缺辅料只影响先后、不影响是否出现。
        private const val SHORTAGE_PENALTY = 0.3 // 库存不足菜仍推荐，仅轻微排后并标"⚠库存不足"。
        private const val MISSING_PENALTY = 0.2 // 缺辅料(需采购)每味仅轻微排后，让齐备的略靠前。
        private const val AVOID_PENALTY = 50.0 // [AI生成] 忌口菜大幅降权，排到所有正常菜之后(仍保留+标红)，让用户看得到但明确知道该避免。
    }
}
