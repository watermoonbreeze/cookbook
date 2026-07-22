package com.sxdbsm.cookbook.domain

/**
 * @File : SeasoningDefaults
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 配料默认克数——调味料按正常每菜用量给默认值，而非普通食材的 100g
 * <p>
 * 调料(盐/酱油/油/糖…)加进菜品时默认 100g 会让钠/糖/油脂等营养算爆、影响营养级别评级；
 * 改为按常见每菜用量给小默认值。参考国标：食盐每日≤5g(膳食指南2022)→一道菜约 3g；其余按日常烹饪常识。
 * <p>
 * [AI生成] 用户反馈：调料加进菜品默认克数应按正常饮食添加、有国标参考国标，不要默认 100g。
 **/
object SeasoningDefaults {

    /** 普通食材默认克数(分类判不出时的兜底)。[AI生成] */
    const val DEFAULT_INGREDIENT_GRAMS = 100

    /** 调料但未命中具体名时的通用默认克数。[AI生成] */
    const val GENERAL_SEASONING_GRAMS = 8

    // [AI生成] #31 非调料食材按食物大类给"一道菜常见用量"经验默认克数(整份/家庭量·惯例非权威·预填后可用 −N+ 改)。
    // 目的:减少"都默认100g再手动调"的操作(蛋约1个50g、菜量大、奶一杯200g…)，更贴近真实录入。
    // 量级参考现有预设菜 seed(青椒200/牛肉250/豆腐200…)取稳妥中间值；分类判不出退 DEFAULT_INGREDIENT_GRAMS。
    private val GROUP_GRAMS: Map<FoodGroup.Group, Int> = mapOf(
        FoodGroup.Group.STAPLE to 100,      // 主食(米/面):一道菜的主食量
        FoodGroup.Group.VEGETABLE to 150,   // 蔬菜:量较大
        FoodGroup.Group.FUNGI to 100,       // 菌菇/海带/紫菜:多为配角
        FoodGroup.Group.FISH to 150,        // 水产:主料蛋白量
        FoodGroup.Group.RED_MEAT to 150,    // 红肉:主料蛋白量
        FoodGroup.Group.WHITE_MEAT to 150,  // 禽肉:主料蛋白量
        FoodGroup.Group.EGG to 50,          // 蛋:约1个
        FoodGroup.Group.DAIRY to 200,       // 奶:约一杯
        FoodGroup.Group.BEAN to 150,        // 豆制品(豆腐等):一块;坚果偏小但本组以豆制品为主
        FoodGroup.Group.FRUIT to 150,       // 水果:一份
    )

    // [AI生成] 调料名关键词 → 一道菜常见用量(g)。命中即用；顺序敏感(具体在前，如「酱油/蚝油」先于「油」)。
    // 参考：盐日限5g(膳食指南2022)→每菜约3g；酱油/油/料酒按常识 10g；粉末类 2g。
    private val KEYWORD_GRAMS: List<Pair<List<String>, Int>> = listOf(
        listOf("盐") to 3,
        listOf("酱油", "生抽", "老抽", "豉油") to 10,
        listOf("蚝油") to 10,
        listOf("料酒", "黄酒", "米酒") to 10,
        listOf("醋") to 8,
        listOf("糖", "冰糖", "蜂蜜") to 5,
        listOf("淀粉", "生粉", "芡") to 5,
        listOf("味精", "鸡精") to 2,
        listOf("胡椒", "花椒", "孜然", "五香", "十三香", "辣椒粉", "辣椒面", "咖喱粉") to 2,
        listOf("豆瓣", "甜面酱", "番茄酱", "沙拉酱", "酱") to 15,
        listOf("蒜", "姜", "葱") to 10,
        listOf("油") to 10, // 食用油/花生油/菜籽油/香油…(酱油/蚝油已在前命中)
    )

    /**
     * 某食材加进菜品时的默认克数。[AI修改]
     *
     * - 调料(按分类「调味品/油脂类」，见 selectSeasoningIngredientIds)：按名给小用量(盐3/酱油油10…)，未命中退通用 8g。
     * - 普通食材：[AI生成] #31 按食物大类(FoodGroup.classify 尾词判定)给经验默认克数(蛋50/菜150/奶200…)，
     *   减少手动调整；分类判不出(启发式未命中,如生僻自建名)退 DEFAULT_INGREDIENT_GRAMS(100)。
     *   名字带「油」的油菜/油麦菜等经 classify 仍归蔬菜、不误判(endsWith("油")才排除,「油菜」结尾是「菜」)。
     */
    fun defaultGramFor(name: String, isSeasoning: Boolean): Int {
        if (isSeasoning) {
            val hit = KEYWORD_GRAMS.firstOrNull { entry -> entry.first.any { name.contains(it) } }
            return hit?.second ?: GENERAL_SEASONING_GRAMS
        }
        val group = FoodGroup.classify(name)
        return group?.let { GROUP_GRAMS[it] } ?: DEFAULT_INGREDIENT_GRAMS
    }
}
