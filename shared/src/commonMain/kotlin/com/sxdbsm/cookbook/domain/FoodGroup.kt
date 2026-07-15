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

    /**
     * 营养大类 → food_categories 顶层分类**名**。[AI生成] 自定义食材归类：编辑器按此把选中大类挂到分类树。
     * (food_category 表无 code 列，只能按名匹配。)红肉/禽肉都归畜禽肉类；调料/油脂不属营养大类，不在此表。
     */
    val CATEGORY_NAME: Map<Group, String> = mapOf(
        Group.STAPLE to "谷薯主食类",
        Group.VEGETABLE to "蔬菜类",
        Group.FUNGI to "菌藻类",
        Group.FRUIT to "水果类",
        Group.FISH to "水产类",
        Group.RED_MEAT to "畜禽肉类",
        Group.WHITE_MEAT to "畜禽肉类",
        Group.EGG to "蛋类",
        Group.DAIRY to "奶类",
        Group.BEAN to "大豆及坚果",
    )

    /** 可归类的顶层分类名集合(编辑保存时"切换大类"去旧留新)。[AI生成] */
    val CATEGORY_NAMES: Set<String> = CATEGORY_NAME.values.toSet()

    /**
     * 单个主料名 → 食物大类。[AI修改]
     *
     * **尾词优先**：中文食材"中心词在末尾"(鸡毛菜=菜、脱脂牛奶=奶、兔肉=肉)，先按尾词判定，
     * 避免前缀修饰词误判(鸡毛菜被"鸡"判成禽肉、脱脂牛奶被"牛"判成红肉)；尾词判不了再按关键词子串。
     */
    fun classify(name: String): Group? {
        // 1) 强尾词(中心词)——最可靠，优先。
        when {
            name.endsWith("蛋") -> return Group.EGG
            name.endsWith("奶") -> return Group.DAIRY
            // xxx肉：含 鸡/鸭/鹅 归禽肉，否则红肉(兔肉/驴肉/腊肉等)。
            name.endsWith("肉") -> return if (WHITE_MEAT_KW.any { name.contains(it) }) Group.WHITE_MEAT else Group.RED_MEAT
        }
        // 2) 血制品归红肉(鸭血别被"鸭"判禽)。
        if (name.contains("血")) return Group.RED_MEAT
        // 3) 藻菌先于"菜"尾字(紫菜/海带也以菜/带结尾但属菌藻)。
        if (FUNGI_KW.any { name.contains(it) } || name.endsWith("蘑")) return Group.FUNGI
        // 4) xxx菜/xxx苗 = 蔬菜(鸡毛菜/上海青菜/蒜苗)——尾词优先于前缀修饰词。
        if (name.endsWith("菜") || name.endsWith("苗")) return Group.VEGETABLE
        // 5) 其余按关键词子串(含优先级)。
        return when {
            EGG_KW.any { name.contains(it) } -> Group.EGG
            DAIRY_KW.any { name.contains(it) } -> Group.DAIRY
            FISH_KW.any { name.contains(it) } -> Group.FISH
            WHITE_MEAT_KW.any { name.contains(it) } -> Group.WHITE_MEAT
            RED_MEAT_KW.any { name.contains(it) } -> Group.RED_MEAT
            StapleFood.isStaple(name, listOf(name)) -> Group.STAPLE
            BEAN_KW.any { name.contains(it) } -> Group.BEAN
            // 水果先于蔬菜(西瓜/哈密瓜/木瓜/菠萝的"瓜/菠"在蔬菜里会先命中)。
            FRUIT_KW.any { name.contains(it) } -> Group.FRUIT
            VEG_KW.any { name.contains(it) } -> Group.VEGETABLE
            else -> null
        }
    }

    /**
     * 一餐涵盖的食物大类(按主料名去重，保持枚举顺序)。[AI修改]
     *
     * explicit=名→大类覆盖(食材的显式营养大类 food_group)：优先于关键词，覆盖名字无关键词的自定义食材。
     */
    fun groupsOf(mainNamesOfMeal: List<String>, explicit: Map<String, Group> = emptyMap()): List<Group> {
        val set = mainNamesOfMeal.mapNotNull { name -> explicit[name] ?: classify(name) }.toSet()
        return Group.entries.filter { it in set }
    }

    /** 把"名→大类名(food_group)"字符串映射转成"名→Group"(忽略非法值)。[AI生成] */
    fun explicitFrom(nameToGroupName: Map<String, String>): Map<String, Group> =
        nameToGroupName.mapNotNull { (n, g) -> runCatching { Group.valueOf(g) }.getOrNull()?.let { n to it } }.toMap()

    /**
     * 营养构成摘要(只反映当前菜品实际包含的营养素/分类，不做推荐)。[AI修改]
     *
     * 仅由本餐已涵盖的食物大类如实归纳：有蛋白源→优质蛋白，有主食→主食·碳水，有蔬菜/菌菇→膳食纤维等。
     * 不含推测性/推荐性内容(如"建议再加""含Omega-3")，避免不准确。
     */
    fun nutritionSummary(groups: List<Group>): List<String> {
        val out = mutableListOf<String>()
        if (groups.any { it in setOf(Group.FISH, Group.RED_MEAT, Group.WHITE_MEAT, Group.EGG, Group.DAIRY, Group.BEAN) }) out += "优质蛋白"
        if (Group.STAPLE in groups) out += "主食·碳水"
        if (groups.any { it == Group.VEGETABLE || it == Group.FUNGI }) out += "蔬菜·膳食纤维"
        if (Group.FRUIT in groups) out += "水果·维生素"
        return out
    }

    /** 优质蛋白来源大类。[AI生成] */
    val PROTEIN_GROUPS = setOf(Group.FISH, Group.RED_MEAT, Group.WHITE_MEAT, Group.EGG, Group.DAIRY, Group.BEAN)

    /**
     * 营养均衡级别 0~4。[AI生成]
     *
     * 按三大支柱覆盖度评级：优质蛋白 / 主食·碳水 / 蔬果·膳食纤维。
     * 0=无(空)，1=单一(仅1类)，2=尚可(2类)，3=均衡(三大类齐)，4=优(三类齐且食材多样≥5大类)。
     * 供餐食卡片背景配色与首页"每天营养色系墙"用同一级别口径。
     */
    fun nutritionLevel(groups: List<Group>): Int {
        if (groups.isEmpty()) return 0
        val hasProtein = groups.any { it in PROTEIN_GROUPS }
        val hasStaple = Group.STAPLE in groups
        val hasVeg = groups.any { it == Group.VEGETABLE || it == Group.FUNGI || it == Group.FRUIT }
        val pillars = listOf(hasProtein, hasStaple, hasVeg).count { it }
        return when {
            pillars >= 3 && groups.size >= 5 -> 4
            pillars >= 3 -> 3
            pillars == 2 -> 2
            else -> 1
        }
    }

    /** 级别文字。[AI生成] */
    fun nutritionLevelLabel(level: Int): String = when (level) {
        4 -> "营养优"
        3 -> "均衡"
        2 -> "尚可"
        1 -> "较单一"
        else -> ""
    }
}
