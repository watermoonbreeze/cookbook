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
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate
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
    private val familyRepo: com.sxdbsm.cookbook.data.repository.FamilyRepository, // [AI修改] 忌口取全家成员病种并集(含旧个人档案)。
    private val ingredientRepo: IngredientRepository,
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository, // [AI生成] P2：营养互补度画像
) {
    private val q = db.cookbookQueries

    /** 聚合规则引擎输入。[AI生成] */
    suspend fun gather(
        mode: RecommendMode = RecommendMode.PANTRY,
        recentLimit: Long = RECENT_LIMIT,
        mealSlot: MealSlot = MealSlot.ALL, // [AI生成] 按餐次筛选候选菜(全部=不筛)
        recentWindowDays: Int = RECENT_WINDOW_DAYS_DEFAULT, // [AI生成] B2：去重窗口(天)，一周/二周/三周/四周
        today: LocalDate = DateTime.today(), // [AI生成] 提为参数便于固定日期单测(红线：派生别依赖内部 today)
    ): RecommendationInput = withContext(ioDispatcher) {
        // [AI修改] 取材范围：库存=在手食材；随机=整个食材库(相当于都可做)。
        val pantryIds = when (mode) {
            RecommendMode.PANTRY -> {
                // [AI修改] 按名扩展：把与在手食材同名的所有食材 id 都算作在手，
                // 兼容"自建菜引用了同名但不同 id 的食材"(历史重复数据)，否则按 id 精确匹配会漏推该菜。
                val pantryIngredients = pantryRepo.listPantryIngredients()
                val base = pantryIngredients.map { it.id }.toSet()
                val names = pantryIngredients.map { it.name }.distinct()
                if (names.isEmpty()) base else base + q.selectIngredientIdsByNames(names).executeAsList().toSet()
            }
            RecommendMode.RANDOM -> q.selectAllIngredientIds().executeAsList().toSet()
        }
        val seasoningIds = q.selectSeasoningIngredientIds().executeAsList().toSet()

        // 候选菜：与可用食材有交集的菜(预筛)，再取全食材做角色标注；按餐次适配筛选(全部不筛)。
        val candidateDishIds = if (pantryIds.isEmpty()) {
            emptyList()
        } else {
            dishRepo.findDishesByIngredients(pantryIds.toList(), limit = DISH_PREFILTER_LIMIT).map { it.dish.id }
        }
        // [AI修改] 保留完整 Dish(用于偏好/主料重复/营养画像)，再转 RuleDish。
        val fullDishes = candidateDishIds.mapNotNull { dishRepo.getDishById(it) }
            .filter { MealSlotMatcher.matches(mealSlot, it.name) }
        val dishes = fullDishes.map { it.toRuleDish(seasoningIds) }

        // 忌口约束：启用的健康档案 → care 分类 → 调养规则。
        val careCategoryIds = familyRepo.allEnabledCareIds()
        val careIngredients = if (careCategoryIds.isEmpty()) emptyList() else ingredientRepo.listByCareCategories(careCategoryIds)
        val avoidIds = careIngredients.filter { it.adviceLevel == AdviceLevel.AVOID }.map { it.id }.toSet()
        val limitIds = careIngredients.filter { it.adviceLevel == AdviceLevel.LIMIT }.map { it.id }.toSet()
        val recommendIds = careIngredients.filter { it.adviceLevel == AdviceLevel.RECOMMEND }.map { it.id }.toSet()
        val careCategoryNames = if (careCategoryIds.isEmpty()) emptyList() else q.selectFoodCategoryNamesByIds(careCategoryIds).executeAsList()
        val labels = careCategoryNames.map { "关注:$it" } // 粗标签给模型(不含敏感明细)
        // [AI生成] 慢病数值软约束:care 分类名→病种集(与今日卡/详情页同口径 fromCareName)；giByName 仅登记糖尿病才查(GI 只对糖尿病;gate 省无谓全表查)。
        val conditions = careCategoryNames.flatMap { com.sxdbsm.cookbook.domain.HealthCondition.fromCareName(it) }.toSet()
        val giByName = if (com.sxdbsm.cookbook.domain.HealthCondition.DIABETES in conditions) nutritionRepo.giByName() else emptyMap()

        // [AI修改] B2：改为**日期窗口**去重(默认一周)——取窗口内吃过的菜及其距今天数，用于排最后+标注"N天前吃过"。
        val sinceDate = DateTime.formatDate(DateTime.plusDays(today, -recentWindowDays.coerceAtLeast(1)))
        val recentDishDaysAgo = q.selectEatenDishDatesSince(sinceDate).executeAsList()
            .mapNotNull { row -> row.last_date?.let { d -> row.dish_id to DateTime.daysBetween(DateTime.parseDate(d), today).coerceAtLeast(0) } }
            .toMap()
        val recentDishIds = recentDishDaysAgo.keys

        // [AI生成] 库存不足食材：可用份数(入库日起窗口的剩余)≤0；含它的菜仍推荐但排后+标不足。仅库存模式。
        val shortageIds = if (mode == RecommendMode.PANTRY) {
            val depleted = pantryRepo.remaining().filterValues { it <= 0 }.keys
            if (depleted.isEmpty()) {
                emptySet()
            } else {
                // [AI修改] 按名扩展与 pantryIds 一致：耗尽食材的同名副本也算不足，仍标"库存不足"并排后，避免同名副本被当充足推前无警示。
                val names = pantryRepo.listPantryIngredients().filter { it.id in depleted }.map { it.name }.distinct()
                if (names.isEmpty()) depleted.toSet()
                else depleted + q.selectIngredientIdsByNames(names).executeAsList().toSet()
            }
        } else {
            emptySet()
        }

        // [AI生成] 增长型 P2：从用户历史/收藏/营养派生画像信号（数据越多越准，会成长）。
        val candidateIds = fullDishes.map { it.id }
        // 偏好画像[0,1]：常做(preference，8 次饱和) + 收藏加成。
        val favIds = q.selectFavoriteDishIds().executeAsList().toSet()
        val preferenceScores = fullDishes.associate { d ->
            val familiar = (d.preference.toDouble() / PREF_SATURATION).coerceIn(0.0, 1.0)
            val fav = if (d.id in favIds) FAVORITE_BONUS else 0.0
            d.id to (familiar + fav).coerceAtMost(1.0)
        }.filterValues { it > 0.0 }
        // 主料近期重复：近窗口吃过菜的主料频次 → 候选取其主料最大频次。
        val recentIds = recentDishDaysAgo.keys.toList()
        val recentMainFreq = if (recentIds.isEmpty()) emptyMap() else
            q.selectMainIngredientNamesByDishIds(recentIds).executeAsList()
                .map { it.ingredient_name }.groupingBy { it }.eachCount()
        val mainRepeatCounts = fullDishes.associate { d ->
            val mains = d.ingredients.filter { it.isMain && it.ingredient.id !in seasoningIds }.map { it.ingredient.name }
            d.id to (mains.maxOfOrNull { recentMainFreq[it] ?: 0 } ?: 0)
        }.filterValues { it > 0 }
        // [AI修改] 营养互补度[-1,1] 改「今日缺口」基线(用户 2026-07-16)：候选补足**今天已吃(daysAgo==0)**还缺的宏量→加分。
        //   原用近 recentWindowDays(默认7天)窗口总量作基线——一周食物平均化后三大宏量占比接近目标、缺口趋零→因子近乎失效(算法评审)；
        //   改取今天已吃作基线(缺口更 sharp，对"下一餐补今天所缺"更有意义)。今天没吃(基线空)→NutritionBalance.score 返 0，因子中性、行为向后兼容。
        val todayDishIds = recentDishDaysAgo.filterValues { it == 0 }.keys.toList()
        val todayTotals = if (todayDishIds.isEmpty()) com.sxdbsm.cookbook.domain.model.NutritionTotals.EMPTY
            else nutritionRepo.totalOf(todayDishIds)
        val nutritionBalanceScores = if (candidateIds.isEmpty()) emptyMap() else
            nutritionRepo.dishNutrition(candidateIds)
                .mapValues { (_, dn) -> com.sxdbsm.cookbook.domain.model.NutritionBalance.score(todayTotals, dn.totals) }
                .filterValues { it != 0.0 }
        // 推荐风格(用户轻干预)：从偏好读取，默认综合。
        val style = RecommendationStyle.fromKey(
            q.selectPreference(com.sxdbsm.cookbook.domain.model.PreferenceKeys.RECOMMEND_STYLE).executeAsOneOrNull()?.value_,
        )

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
            recentDishDaysAgo = recentDishDaysAgo,
            preferenceScores = preferenceScores,
            nutritionBalanceScores = nutritionBalanceScores,
            mainRepeatCounts = mainRepeatCounts,
            style = style,
            conditions = conditions, // [AI生成] 慢病数值软约束(仅营养风格生效)
            giByName = giByName,
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
        val careCategoryIds = familyRepo.allEnabledCareIds()
        val careIngredients = if (careCategoryIds.isEmpty()) emptyList() else ingredientRepo.listByCareCategories(careCategoryIds)
        val avoidIds = careIngredients.filter { it.adviceLevel == AdviceLevel.AVOID }.map { it.id }.toSet()
        val limitIds = careIngredients.filter { it.adviceLevel == AdviceLevel.LIMIT }.map { it.id }.toSet()
        val recommendIds = careIngredients.filter { it.adviceLevel == AdviceLevel.RECOMMEND }.map { it.id }.toSet()
        val healthAware = careCategoryIds.isNotEmpty()
        // 营养/应季标签(按食材)
        val tagRows = q.selectNutritionSeasonTags().executeAsList()
        val nutritionByIng = tagRows.filter { it.dim == "nutrition" }.groupBy({ it.ingredient_id }, { it.tag_name }).mapValues { it.value.toSet() }
        val seasonByIng = tagRows.filter { it.dim == "season" }.groupBy({ it.ingredient_id }, { it.tag_name }).mapValues { it.value.toSet() }
        // 全库菜品 → PlanDish
        val allDishIds = q.selectAllDishes().executeAsList().map { it.id }
        val dishes = allDishIds.mapNotNull { dishRepo.getDishById(it) }.map { d ->
            val ings = d.ingredients
            val ingIds = ings.map { it.ingredient.id }
            // [AI修改] 忌口/限量/推荐只算非调料食材：否则盐/生抽等调料几乎每道菜都有，会让所有菜都判忌口。
            // [AI修改] 剂量占比门槛(用户 2026-07-16)：进一步只按**主料(isMain)**判定，与 HealthRuleEngine 一致——
            //   克数极少的辅料/点缀不改变菜的健康定性(如木耳50g配料不该让菜显"忌木耳")。
            val avoidHits = ings.filter { it.isMain && it.ingredient.id in avoidIds && it.ingredient.id !in seasoningIds }
            val limitHits = ings.filter { it.isMain && it.ingredient.id in limitIds && it.ingredient.id !in seasoningIds }
            val recommendHits = ings.filter { it.isMain && it.ingredient.id in recommendIds && it.ingredient.id !in seasoningIds }
            val planMainNames = ings.filter { it.isMain && it.ingredient.id !in seasoningIds }.map { it.ingredient.name }
            PlanDish(
                id = d.id,
                name = d.name,
                mainNames = planMainNames,
                isMeat = MealSlotMatcher.isMeatByMains(planMainNames), // [AI生成] 荤/素(同餐荤素搭配用)
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
        // [AI修改] 与主链路 orchestrator.recommend 一致，透传全部信号(库存不足/画像/风格权重)。
        engine.evaluate(
            input.dishes, input.pantryIngredientIds, input.constraints, input.recentDishIds,
            input.shortageIngredientIds, input.recentDishDaysAgo,
            weights = input.style.weights(),
            preferenceScores = input.preferenceScores,
            nutritionBalanceScores = input.nutritionBalanceScores,
            mainRepeatCounts = input.mainRepeatCounts,
            conditions = input.conditions, // [AI生成] 慢病数值软约束透传
            giByName = input.giByName,
        )
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
        private const val RECENT_LIMIT = 15L // 去重参考的最近菜品数(旧按条数，保留兼容)。
        const val RECENT_WINDOW_DAYS_DEFAULT = 7 // [AI生成] B2：去重窗口默认一周；UI 可切一周/二周/三周/四周。
        private const val DISH_PREFILTER_LIMIT = 100L // 与在手食材有交集的候选菜上限。
        private const val PREF_SATURATION = 8.0 // [AI生成] P2：菜被记录 8 次即视为"很熟悉"(偏好画像饱和点)。
        private const val FAVORITE_BONUS = 0.3 // [AI生成] P2：收藏菜的偏好加成。
    }
}
