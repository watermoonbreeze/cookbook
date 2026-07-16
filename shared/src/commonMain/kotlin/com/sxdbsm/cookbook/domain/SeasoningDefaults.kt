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

    /** 普通食材默认克数(主料/辅料)。[AI生成] */
    const val DEFAULT_INGREDIENT_GRAMS = 100

    /** 调料但未命中具体名时的通用默认克数。[AI生成] */
    const val GENERAL_SEASONING_GRAMS = 8

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
     * 某食材加进菜品时的默认克数。[AI生成]
     *
     * 只有被判定为调料(按分类「调味品/油脂类」，见 selectSeasoningIngredientIds)时才用小默认值，
     * 普通食材(含名字带「油」的油菜/油麦菜等)一律 100g，避免误缩。
     */
    fun defaultGramFor(name: String, isSeasoning: Boolean): Int {
        if (!isSeasoning) return DEFAULT_INGREDIENT_GRAMS
        val hit = KEYWORD_GRAMS.firstOrNull { entry -> entry.first.any { name.contains(it) } }
        return hit?.second ?: GENERAL_SEASONING_GRAMS
    }
}
