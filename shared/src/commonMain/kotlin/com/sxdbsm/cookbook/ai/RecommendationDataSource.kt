package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.HealthConstraints
import com.sxdbsm.cookbook.ai.model.IngredientRole
import com.sxdbsm.cookbook.ai.model.PeriodPlan
import com.sxdbsm.cookbook.ai.model.PlanContext
import com.sxdbsm.cookbook.ai.model.PlanDish
import com.sxdbsm.cookbook.pantry.PantryPlanAnnotator
import com.sxdbsm.cookbook.pantry.PlanMainIngredient
import com.sxdbsm.cookbook.ai.model.RecommendationInput
import com.sxdbsm.cookbook.ai.model.RecommendMode
import com.sxdbsm.cookbook.ai.model.RuleDish
import com.sxdbsm.cookbook.ai.model.RuleDishIngredient
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.util.DateTime
import com.sxdbsm.cookbook.platform.ioDispatcher
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
    suspend fun gather(mode: RecommendMode = RecommendMode.PANTRY, recentLimit: Long = RECENT_LIMIT): RecommendationInput = withContext(ioDispatcher) {
        // [AI修改] 取材范围：库存=在手食材；随机=整个食材库(相当于都可做)。
        val pantryIds = when (mode) {
            RecommendMode.PANTRY -> pantryRepo.pantryIngredientIds()
            RecommendMode.RANDOM -> q.selectAllIngredientIds().executeAsList().toSet()
        }
        val seasoningIds = q.selectSeasoningIngredientIds().executeAsList().toSet()

        // 候选菜：与可用食材有交集的菜(预筛)，再取全食材做角色标注。
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
        val recommendIds = careIngredients.filter { it.adviceLevel == AdviceLevel.RECOMMEND }.map { it.id }.toSet()
        val labels = enabledProfiles.map { "关注:${it.crowdName}" } // 粗标签给模型(不含敏感明细)

        val recentDishIds = q.selectRecentEatenDishIds(recentLimit).executeAsList().toSet()

        // [AI生成] 库存不足食材：可用份数(入库日起窗口的剩余)≤0；含它的菜仍推荐但排后+标不足。仅库存模式。
        val shortageIds = if (mode == RecommendMode.PANTRY) {
            pantryRepo.remaining().filterValues { it <= 0 }.keys
        } else {
            emptySet()
        }

        RecommendationInput(
            dishes = dishes,
            pantryIngredientIds = pantryIds,
            constraints = HealthConstraints(
                avoidIngredientIds = avoidIds,
                limitIngredientIds = limitIds,
                recommendIngredientIds = recommendIds,
                labels = labels,
            ),
            recentDishIds = recentDishIds,
            shortageIngredientIds = shortageIds,
        )
    }

    /**
     * 食材自由搭配（离线规则轻搭配）。[AI生成]
     *
     * 取在手食材，按大类(荤/素/蛋/豆/主食/调味)分类后交给 FreePairingEngine 拼搭配建议。
     * 不依赖已有菜品、不调 AI，完全离线。
     */
    suspend fun freePairing(maxSuggestions: Int = 8): List<PairingSuggestion> = withContext(ioDispatcher) {
        val pantry = pantryRepo.listPantryIngredients()
        if (pantry.isEmpty()) return@withContext emptyList()
        val seasoningIds = q.selectSeasoningIngredientIds().executeAsList().toSet()
        val pairs = mutableListOf<PairIngredient>()
        for (ing in pantry) { // suspend classifyPairRole 不能放进非挂起的 .map lambda
            pairs += PairIngredient(ing.name, classifyPairRole(ing.id, seasoningIds))
        }
        FreePairingEngine.suggest(pairs, maxSuggestions)
    }

    /** 按食材 general 大类判定搭配角色。[AI生成] */
    private suspend fun classifyPairRole(ingredientId: Long, seasoningIds: Set<Long>): PairRole {
        if (ingredientId in seasoningIds) return PairRole.SEASONING
        val cats = ingredientRepo.listCategories(ingredientId).filter { it.dimension == "general" }.map { it.name }
        return when {
            cats.any { it.contains("蛋") } -> PairRole.EGG
            cats.any { it.contains("肉") || it.contains("禽") || it.contains("水产") || it.contains("鱼") || it.contains("虾") || it.contains("海") } -> PairRole.PROTEIN
            // [AI修改] 蔬菜优先于豆：鲜豆类蔬菜(毛豆/四季豆/豆芽/荷兰豆)也绑了顶层"蔬菜类"code，
            // 用"蔬/菌/藻"匹配(不用泛"菜"以免命中"预制菜类"把料理包误判为蔬菜)。
            // 不变量：seed 里每个蔬菜/菌藻食材须绑顶层 vegetable/fungi_algae(蔬菜类/菌藻类) code，否则会漏判为 OTHER。
            cats.any { it.contains("蔬") || it.contains("菌") || it.contains("藻") } -> PairRole.VEGETABLE
            // BEAN 只认"大豆/豆制品/坚果"，不用泛"豆"(否则鲜豆类蔬菜被误判为植物蛋白)。
            cats.any { it.contains("大豆") || it.contains("豆制品") || it.contains("坚果") } -> PairRole.BEAN
            cats.any { it.contains("主食") || it.contains("谷") || it.contains("薯") || it.contains("稻") || it.contains("麦") || it.contains("米") } -> PairRole.STAPLE
            else -> PairRole.OTHER
        }
    }

    /**
     * 给规划标注库存「采购/缺料」。[AI生成]
     *
     * 按当前库存快照对每道菜主料判定：不在库→采购、在库但份数不够→缺料(跨规划多天按天序分配剩余份数)。
     */
    suspend fun annotatePlanWithPantry(plan: PeriodPlan): PeriodPlan = withContext(ioDispatcher) {
        if (plan.days.isEmpty()) return@withContext plan
        val servings0 = pantryRepo.servingCounts()
        // [AI生成] 库存完全为空(未用库存功能)时不标注，避免整份规划全标"采购"灰显打扰。
        if (servings0.isEmpty()) return@withContext plan
        val dishIds = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }.distinct()
        val mainByDish = dishIds.associateWith { id ->
            dishRepo.getDishById(id)?.ingredients?.filter { it.isMain }
                ?.map { PlanMainIngredient(it.ingredient.id, it.ingredient.name) }.orEmpty()
        }
        val remaining = pantryRepo.remaining() // [AI修改] "入库日起"窗口剩余份数作预算
        PantryPlanAnnotator.annotate(plan, mainByDish, servings0.keys, remaining)
    }

    /** 周期规划取数：全库菜品 + 营养/应季标签 + 健康标记 + 当前季节。[AI生成] */
    suspend fun gatherForPlan(): PlanContext = withContext(ioDispatcher) {
        val seasoningIds = q.selectSeasoningIngredientIds().executeAsList().toSet()
        // 健康约束
        val enabledProfiles = healthRepo.listAll().filter { it.enabled }
        val careCategoryIds = enabledProfiles.map { it.crowdTypeId }
        val careIngredients = if (careCategoryIds.isEmpty()) emptyList() else ingredientRepo.listByCareCategories(careCategoryIds)
        val avoidIds = careIngredients.filter { it.adviceLevel == AdviceLevel.AVOID }.map { it.id }.toSet()
        val limitIds = careIngredients.filter { it.adviceLevel == AdviceLevel.LIMIT }.map { it.id }.toSet()
        val recommendIds = careIngredients.filter { it.adviceLevel == AdviceLevel.RECOMMEND }.map { it.id }.toSet()
        val healthAware = enabledProfiles.isNotEmpty()
        // 营养/应季标签(按食材)
        val tagRows = q.selectNutritionSeasonTags().executeAsList()
        val nutritionByIng = tagRows.filter { it.dim == "nutrition" }.groupBy({ it.ingredient_id }, { it.tag_name }).mapValues { it.value.toSet() }
        val seasonByIng = tagRows.filter { it.dim == "season" }.groupBy({ it.ingredient_id }, { it.tag_name }).mapValues { it.value.toSet() }
        // 全库菜品 → PlanDish
        val allDishIds = q.selectAllDishes().executeAsList().map { it.id }
        val dishes = allDishIds.mapNotNull { dishRepo.getDishById(it) }.map { d ->
            val ings = d.ingredients
            val ingIds = ings.map { it.ingredient.id }
            val avoidHits = ings.filter { it.ingredient.id in avoidIds }
            val limitHits = ings.filter { it.ingredient.id in limitIds }
            val recommendHits = ings.filter { it.ingredient.id in recommendIds }
            PlanDish(
                id = d.id,
                name = d.name,
                mainNames = ings.filter { it.isMain && it.ingredient.id !in seasoningIds }.map { it.ingredient.name },
                nutritionTags = ingIds.flatMap { nutritionByIng[it].orEmpty() }.toSet(),
                seasonTags = ingIds.flatMap { seasonByIng[it].orEmpty() }.toSet(),
                isHealthy = healthAware && recommendHits.isNotEmpty() && limitHits.isEmpty() && avoidHits.isEmpty(),
                hasAvoid = avoidHits.isNotEmpty(),
                isBreakfast = BREAKFAST_KEYWORDS.any { d.name.contains(it) }, // [AI生成] 按菜名判早餐菜(符合中式饮食)。
                breakfastSoft = BREAKFAST_SOFT_KEYWORDS.any { d.name.contains(it) }, // [AI生成] 软/饮 vs 硬/主食(软硬搭配)。
                recommendHits = recommendHits.map { it.ingredient.name }.distinct(),
                limitHits = limitHits.map { it.ingredient.name }.distinct(),
            )
        }
        PlanContext(dishes = dishes, season = currentSeason(), healthAware = healthAware)
    }

    /** 当前季节（按月份）。[AI生成] */
    private fun currentSeason(): String = when (DateTime.today().monthNumber) {
        in 3..5 -> "春季"
        in 6..8 -> "夏季"
        in 9..11 -> "秋季"
        else -> "冬季"
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
        // [AI生成] 早餐菜关键词(菜名含则视为早餐菜)。
        private val BREAKFAST_KEYWORDS = listOf("粥", "蛋羹", "豆浆", "豆奶", "牛奶", "燕麦", "面", "馒头", "包子", "水煮蛋", "薯", "南瓜", "玉米")
        // [AI生成] 早餐软/饮类关键词(其余早餐菜视为硬/主食，用于软硬搭配)。
        private val BREAKFAST_SOFT_KEYWORDS = listOf("粥", "豆浆", "豆奶", "牛奶", "燕麦", "蛋羹", "面")
        private const val RECENT_LIMIT = 15L // 去重参考的最近菜品数。
        private const val DISH_PREFILTER_LIMIT = 100L // 与在手食材有交集的候选菜上限。
    }
}
