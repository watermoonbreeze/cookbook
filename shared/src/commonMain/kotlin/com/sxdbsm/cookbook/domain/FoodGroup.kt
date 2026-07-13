package com.sxdbsm.cookbook.domain

/**
 * @File : FoodGroup
 * @Time : 2026/07/13
 * @Author : SXD-AI
 * @Desc : 食物常规分类(启发式，按主料名关键词)——供餐次"分类图标+营养搭配"展示
 * <p>
 * 按菜品主料名把一餐涵盖的食物大类归纳出来(主食/蔬菜/菌菇/水产/红肉/禽肉/蛋/奶/豆/水果)，
 * 用于餐次名后的分类 emoji 与下方"营养搭配一目了然"。启发式、非精确，与 food_categories 语义对齐。
 * <p>
 * [AI生成] N8 餐次营养搭配说明+分类图标。
 **/
object FoodGroup {

    /** 食物大类。emoji 用于餐次头图标，label 用于营养搭配文字。 */
    enum class Group(val emoji: String, val label: String) {
        STAPLE("🍚", "主食"),
        VEGETABLE("🥬", "蔬菜"),
        FUNGI("🍄", "菌菇"),
        FISH("🐟", "水产"),
        RED_MEAT("🥩", "红肉"),
        WHITE_MEAT("🍗", "禽肉"),
        EGG("🥚", "蛋"),
        DAIRY("🥛", "奶"),
        BEAN("🫘", "豆/坚果"),
        FRUIT("🍎", "水果"),
    }

    private val FISH_KW = listOf("鱼", "虾", "蟹", "贝", "蛤", "蛏", "鱿", "墨鱼", "海参", "生蚝", "扇贝", "花甲", "青口", "鳝", "鳅", "章鱼", "海蜇", "鲍", "虾皮")
    private val WHITE_MEAT_KW = listOf("鸡", "鸭", "鹅")
    private val RED_MEAT_KW = listOf("猪", "牛", "羊", "五花", "排骨", "腊肉", "香肠", "火腿", "培根", "瘦肉", "里脊", "肉末", "牛腩", "牛腱", "羊排", "猪蹄", "午餐肉")
    private val BEAN_KW = listOf("豆腐", "豆干", "腐竹", "千张", "豆浆", "黄豆", "黑豆", "绿豆", "红豆", "腐乳", "豆皮", "核桃", "花生", "杏仁", "腰果", "莲子", "松子", "板栗", "芝麻", "鹰嘴豆")
    private val DAIRY_KW = listOf("牛奶", "酸奶", "奶酪", "奶粉", "淡奶油", "黄油", "炼乳")
    private val FUNGI_KW = listOf("菇", "木耳", "银耳", "猴头", "菌", "海带", "紫菜")
    private val EGG_KW = listOf("蛋")
    private val VEG_KW = listOf(
        "菜", "瓜", "椒", "茄", "萝卜", "笋", "藕", "芹", "菠", "生菜", "包菜", "甘蓝", "蒜苗", "韭", "豆角", "豇豆",
        "毛豆", "四季豆", "荷兰豆", "秋葵", "洋葱", "胡萝卜", "莴笋", "茭白", "荸荠", "蒜薹", "葱", "姜", "苗",
    )
    private val FRUIT_KW = listOf("苹果", "梨", "香蕉", "橙", "柚", "猕猴桃", "草莓", "蓝莓", "葡萄", "西瓜", "桃", "樱桃", "枣", "橘", "芒果", "菠萝", "荔枝", "火龙果", "哈密瓜", "石榴", "柿子", "无花果", "桑葚", "山竹", "榴莲", "椰", "枇杷", "杨梅", "李子", "木瓜", "甘蔗")

    /** 单个主料名 → 食物大类(命中优先级：蛋→水产→禽→红肉→主食→豆→奶→菌→蔬菜→水果)。[AI生成] */
    fun classify(name: String): Group? = when {
        EGG_KW.any { name.contains(it) } -> Group.EGG
        FISH_KW.any { name.contains(it) } -> Group.FISH
        WHITE_MEAT_KW.any { name.contains(it) } -> Group.WHITE_MEAT
        RED_MEAT_KW.any { name.contains(it) } -> Group.RED_MEAT
        StapleFood.isStaple(name, listOf(name)) -> Group.STAPLE
        BEAN_KW.any { name.contains(it) } -> Group.BEAN
        DAIRY_KW.any { name.contains(it) } -> Group.DAIRY
        FUNGI_KW.any { name.contains(it) } -> Group.FUNGI
        VEG_KW.any { name.contains(it) } -> Group.VEGETABLE
        FRUIT_KW.any { name.contains(it) } -> Group.FRUIT
        else -> null
    }

    /** 一餐涵盖的食物大类(按主料名去重，保持枚举顺序)。[AI生成] */
    fun groupsOf(mainNamesOfMeal: List<String>): List<Group> {
        val set = mainNamesOfMeal.mapNotNull { classify(it) }.toSet()
        return Group.entries.filter { it in set }
    }

    /** 营养搭配摘要(由涵盖的大类归纳)。[AI生成] */
    fun nutritionSummary(groups: List<Group>): List<String> {
        val out = mutableListOf<String>()
        if (groups.any { it in setOf(Group.FISH, Group.RED_MEAT, Group.WHITE_MEAT, Group.EGG, Group.DAIRY, Group.BEAN) }) out += "优质蛋白"
        if (Group.STAPLE in groups) out += "主食·碳水"
        if (groups.any { it == Group.VEGETABLE || it == Group.FUNGI }) out += "蔬菜·膳食纤维"
        if (Group.FRUIT in groups) out += "水果·维生素"
        if (Group.FISH in groups) out += "含Omega-3(部分)"
        return out
    }

    /** 搭配注意(缺哪类给一句建议)。[AI生成] */
    fun balanceNote(groups: List<Group>): String? {
        val hasProtein = groups.any { it in setOf(Group.FISH, Group.RED_MEAT, Group.WHITE_MEAT, Group.EGG, Group.DAIRY, Group.BEAN) }
        val hasVeg = groups.any { it == Group.VEGETABLE || it == Group.FUNGI }
        val hasStaple = Group.STAPLE in groups
        val missing = buildList {
            if (!hasStaple) add("主食")
            if (!hasProtein) add("优质蛋白")
            if (!hasVeg) add("蔬菜")
        }
        return if (missing.isEmpty()) null else "建议再加：${missing.joinToString("、")}"
    }
}
