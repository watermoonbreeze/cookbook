package com.sxdbsm.cookbook.domain

/**
 * @File : DietaryGuideline
 * @Time : 2026/07/25
 * @Author : SXD-AI
 * @Desc : 平衡膳食结构的**权威真相源**——膳食宝塔五层份量 + 三餐能量分配 + 食物多样性
 * <p>
 * 值据《中国居民平衡膳食宝塔/餐盘(2022)》与《中国居民膳食指南(2022)》(成年人 1600~2400 kcal 水平)，
 * 与「我的·膳食参考依据」页(androidApp `DietaryReference`)同源、口径一致。
 * 「权威方法论优先准则」落地：色系墙均衡评级、餐次差异化、推荐搭配份量等算法一律**引用本对象**，
 * 不再各自自创简化口径(同"营养阈值走国标"，膳食结构走膳食宝塔)。
 * <p>
 * [AI生成] 全功能权威化重审 P0：把膳食宝塔/餐盘/三餐分配下沉为 shared 域层单一真相源，供算法引用。
 **/
object DietaryGuideline {

    /**
     * 膳食宝塔的五层。前四层是"要吃够"的正向层(评均衡看覆盖度)，
     * 第五层油盐是"要限量"层(不属九大类食物、不计入覆盖度)。[AI生成]
     */
    enum class PagodaLayer(val displayName: String, val order: Int) {
        GRAINS("谷薯类", 1),
        VEGETABLES_FRUITS("蔬菜水果", 2),
        ANIMAL_FOODS("鱼禽肉蛋", 3),
        DAIRY_BEANS_NUTS("奶豆坚果", 4),
        OILS_SALT("油盐（限量）", 5),
    }

    /** 参与"均衡覆盖度"评估的正向层(前四层)，油盐限量层不计。[AI生成] */
    val POSITIVE_LAYERS: List<PagodaLayer> =
        listOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS, PagodaLayer.ANIMAL_FOODS, PagodaLayer.DAIRY_BEANS_NUTS)

    /**
     * 食物大类(色系墙九大类) → 所属宝塔层。[AI生成]
     *
     * 菌藻(FUNGI)归蔬菜水果层(宝塔中与蔬菜同层)；油盐不在 `FoodGroup.Group` 里(不属九大类)，故本表无油盐层成员。
     */
    val LAYER_OF_GROUP: Map<FoodGroup.Group, PagodaLayer> = mapOf(
        FoodGroup.Group.STAPLE to PagodaLayer.GRAINS,
        FoodGroup.Group.VEGETABLE to PagodaLayer.VEGETABLES_FRUITS,
        FoodGroup.Group.FUNGI to PagodaLayer.VEGETABLES_FRUITS,
        FoodGroup.Group.FRUIT to PagodaLayer.VEGETABLES_FRUITS,
        FoodGroup.Group.FISH to PagodaLayer.ANIMAL_FOODS,
        FoodGroup.Group.RED_MEAT to PagodaLayer.ANIMAL_FOODS,
        FoodGroup.Group.WHITE_MEAT to PagodaLayer.ANIMAL_FOODS,
        FoodGroup.Group.EGG to PagodaLayer.ANIMAL_FOODS,
        FoodGroup.Group.DAIRY to PagodaLayer.DAIRY_BEANS_NUTS,
        FoodGroup.Group.BEAN to PagodaLayer.DAIRY_BEANS_NUTS,
    )

    /** 一组食物大类覆盖到的宝塔正向层(去重、保持层顺序)。[AI生成] */
    fun coveredLayers(groups: Collection<FoodGroup.Group>): List<PagodaLayer> {
        val set = groups.mapNotNull { LAYER_OF_GROUP[it] }.filter { it in POSITIVE_LAYERS }.toSet()
        return POSITIVE_LAYERS.filter { it in set }
    }

    /**
     * 每类每日推荐份量(克)。范围为成年人 1600~2400 kcal 水平，据膳食宝塔(2022)。[AI生成]
     *
     * min/max 为克/日；油盐层为"每日上限"(max 即上限，min 无意义置 0)。note 补充权威细则。
     */
    data class DailyAmount(
        val layer: PagodaLayer,
        val label: String,
        val minGram: Int,
        val maxGram: Int,
        val note: String = "",
    )

    /** 膳食宝塔各类每日推荐量(权威·成人 1600~2400 kcal)。[AI生成] 与 DietaryReference「平衡膳食结构」条目同源。 */
    val DAILY_AMOUNTS: List<DailyAmount> = listOf(
        DailyAmount(PagodaLayer.GRAINS, "谷类", 200, 300, "其中全谷物和杂豆 50~150 g"),
        DailyAmount(PagodaLayer.GRAINS, "薯类", 50, 100),
        DailyAmount(PagodaLayer.VEGETABLES_FRUITS, "蔬菜", 300, 500, "深色蔬菜占一半以上"),
        DailyAmount(PagodaLayer.VEGETABLES_FRUITS, "水果", 200, 350),
        DailyAmount(PagodaLayer.ANIMAL_FOODS, "鱼、禽、肉、蛋合计", 120, 200, "优先鱼禽、蛋和瘦肉适量"),
        DailyAmount(PagodaLayer.DAIRY_BEANS_NUTS, "奶及奶制品（鲜奶当量）", 300, 500),
        DailyAmount(PagodaLayer.DAIRY_BEANS_NUTS, "大豆及坚果", 25, 35),
        DailyAmount(PagodaLayer.OILS_SALT, "烹调油", 0, 30, "25~30 g/日"),
        DailyAmount(PagodaLayer.OILS_SALT, "食盐", 0, 5, "< 5 g/日"),
    )

    /**
     * 三餐能量分配(占全天能量百分比)。据《中国居民膳食指南(2022)》。[AI生成]
     *
     * `structureHint` = 该餐次膳食结构的合理期待(权威："早餐要吃好"→主食+蛋白+蔬果/奶；晚餐宜清淡)。
     * 供"餐次差异化"评估：不同餐次对各宝塔层的合理期待不同，不再"全天三大类"一刀切判缺。
     */
    data class MealEnergyShare(
        val meal: MealKind,
        val minPercent: Int,
        val maxPercent: Int,
        /** 该餐次"应有"的宝塔层(合理期待)——用于餐次差异化评估，非强制。 */
        val expectedLayers: Set<PagodaLayer>,
        val hint: String,
    )

    /** 餐次种类。[AI生成] 加餐/宵夜等非正餐归 SNACK。 */
    enum class MealKind { BREAKFAST, LUNCH, DINNER, SNACK }

    /**
     * 按餐次名(中文)归类。[AI生成]
     * 早/早餐→BREAKFAST；午/中/中午→LUNCH；晚→DINNER；加餐/上午/下午/宵夜/夜宵→SNACK；其余按 null(调用方回退正餐口径)。
     */
    fun mealKindOf(mealName: String): MealKind? = when {
        mealName.contains("加") || mealName.contains("宵") || mealName.contains("上午") || mealName.contains("下午") -> MealKind.SNACK
        mealName.contains("早") -> MealKind.BREAKFAST
        mealName.contains("午") || mealName.contains("中") -> MealKind.LUNCH
        mealName.contains("晚") -> MealKind.DINNER
        else -> null
    }

    /** 三餐能量分配 + 各餐结构合理期待(权威)。[AI生成] */
    val MEAL_ENERGY_SHARES: List<MealEnergyShare> = listOf(
        MealEnergyShare(
            MealKind.BREAKFAST, 25, 30,
            // 早餐要吃好：谷薯+蛋白(蛋奶/鱼禽肉/豆)+蔬果，不苛求正餐式齐全。
            expectedLayers = setOf(PagodaLayer.GRAINS, PagodaLayer.ANIMAL_FOODS, PagodaLayer.DAIRY_BEANS_NUTS),
            hint = "早餐要吃好：主食 + 优质蛋白（蛋奶/豆）+ 蔬果",
        ),
        MealEnergyShare(
            MealKind.LUNCH, 30, 40,
            expectedLayers = setOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS, PagodaLayer.ANIMAL_FOODS),
            hint = "午餐吃饱：主食 + 蔬菜 + 鱼禽肉蛋",
        ),
        MealEnergyShare(
            MealKind.DINNER, 30, 35,
            expectedLayers = setOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS),
            hint = "晚餐宜清淡：主食 + 蔬菜为主，肉类适量",
        ),
        MealEnergyShare(
            MealKind.SNACK, 0, 0,
            expectedLayers = emptySet(),
            hint = "加餐轻量：水果、奶、坚果等",
        ),
    )

    /** 取某餐次的能量分配/结构期待(未知餐次回退午餐正餐口径)。[AI生成] */
    fun mealShareOf(mealName: String): MealEnergyShare {
        val kind = mealKindOf(mealName) ?: MealKind.LUNCH
        return MEAL_ENERGY_SHARES.first { it.meal == kind }
    }

    /** 食物多样性目标(权威·膳食指南2022)。[AI生成] */
    const val DIVERSITY_PER_DAY = 12
    const val DIVERSITY_PER_WEEK = 25

    /** 成人每日饮水推荐(ml，白开水/淡茶为主)。[AI生成] */
    val WATER_ML_RANGE: IntRange = 1500..1700
}
