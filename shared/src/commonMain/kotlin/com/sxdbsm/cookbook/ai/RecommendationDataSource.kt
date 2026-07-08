package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.HealthConstraints
import com.sxdbsm.cookbook.ai.model.IngredientRole
import com.sxdbsm.cookbook.ai.model.RecommendationInput
import com.sxdbsm.cookbook.ai.model.RuleDish
import com.sxdbsm.cookbook.ai.model.RuleDishIngredient
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.Dish
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * @File : RecommendationDataSource
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 推荐下一餐的取数层（repo → 规则引擎输入）
 * <p>
 * 聚合在手食材、候选菜品(含食材角色)、健康忌口约束、最近吃过的菜，组装成 RecommendationInput。
 * 方案A：调料按「调味品/油脂类」分类识别为 SEASONING（默认常备），主料/辅料仅用于展示与打样。
 * <p>
 * [AI生成] S0：把真实数据接到纯规则引擎，产出"纯规则推荐"（不依赖模型）。
 **/
class RecommendationDataSource(
    private val db: CookbookDatabase,
    private val pantryRepo: PantryRepository,
    private val dishRepo: DishRepository,
    private val healthRepo: HealthProfileRepository,
    private val ingredientRepo: IngredientRepository,
) {
    private val q = db.cookbookQueries

    /** 聚合规则引擎输入。[AI生成] */
    suspend fun gather(recentLimit: Long = RECENT_LIMIT): RecommendationInput = withContext(Dispatchers.Default) {
        val pantryIds = pantryRepo.pantryIngredientIds()
        val seasoningIds = q.selectSeasoningIngredientIds().executeAsList().toSet()

        // 候选菜：与在手食材有交集的菜(预筛)，再取全食材做角色标注。
        val candidateDishIds = if (pantryIds.isEmpty()) {
            emptyList()
        } else {
            dishRepo.findDishesByIngredients(pantryIds.toList(), limit = DISH_PREFILTER_LIMIT).map { it.dish.id }
        }
        val dishes = candidateDishIds.mapNotNull { dishRepo.getDishById(it) }.map { it.toRuleDish(seasoningIds) }

        // 忌口约束：启用的健康档案 → care 分类 → 调养规则。
        val enabledProfiles = healthRepo.listAll().filter { it.enabled }
        val careCategoryIds = enabledProfiles.map { it.crowdTypeId }
        val careIngredients = if (careCategoryIds.isEmpty()) emptyList() else ingredientRepo.listByCareCategories(careCategoryIds)
        val avoidIds = careIngredients.filter { it.adviceLevel == AdviceLevel.AVOID }.map { it.id }.toSet()
        val limitIds = careIngredients.filter { it.adviceLevel == AdviceLevel.LIMIT }.map { it.id }.toSet()
        val labels = enabledProfiles.map { "关注:${it.crowdName}" } // 粗标签给模型(不含敏感明细)

        val recentDishIds = q.selectRecentEatenDishIds(recentLimit).executeAsList().toSet()

        RecommendationInput(
            dishes = dishes,
            pantryIngredientIds = pantryIds,
            constraints = HealthConstraints(avoidIngredientIds = avoidIds, limitIngredientIds = limitIds, labels = labels),
            recentDishIds = recentDishIds,
        )
    }

    /** 纯规则推荐（S0 端到端产物，不依赖模型）。[AI生成] */
    suspend fun ruleCandidates(engine: HealthRuleEngine = HealthRuleEngine()) = gather().let { input ->
        engine.evaluate(input.dishes, input.pantryIngredientIds, input.constraints, input.recentDishIds)
    }

    /** Dish → 规则引擎输入：调料=SEASONING，其余按 is_main 分主料/辅料。[AI生成] */
    private fun Dish.toRuleDish(seasoningIds: Set<Long>): RuleDish = RuleDish(
        id = id,
        name = name,
        ingredients = ingredients.map { di ->
            val ingId = di.ingredient.id
            val role = when {
                ingId in seasoningIds -> IngredientRole.SEASONING
                di.isMain -> IngredientRole.MAIN
                else -> IngredientRole.SECONDARY
            }
            RuleDishIngredient(ingId, di.ingredient.name, role)
        },
    )

    companion object {
        private const val RECENT_LIMIT = 15L // 去重参考的最近菜品数。
        private const val DISH_PREFILTER_LIMIT = 100L // 与在手食材有交集的候选菜上限。
    }
}
