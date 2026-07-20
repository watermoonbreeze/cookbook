package com.sxdbsm.cookbook.ai

/**
 * @File : MealSlot
 * @Time : 2026/07/12
 * @Author : SXD-AI
 * @Desc : 推荐餐次(与 meal_type 对应)及按菜名的适配规则
 * <p>
 * 库存/随机推荐可按餐次筛选：全部=不分餐次；早餐=轻食主食类；中/晚餐=正餐(排除纯饮品)；
 * 上午餐/下午餐/宵夜=偏轻的加餐。按菜名关键词启发式判定(菜品无餐次属性)，非精确、可后续细化。
 * <p>
 * [AI生成] 推荐加餐次选择。
 **/
enum class MealSlot(val code: String, val label: String) {
    ALL("", "全部"),
    BREAKFAST("BREAKFAST", "早餐"),
    MORNING_SNACK("MORNING_SNACK", "上午餐"),
    LUNCH("LUNCH", "中餐"),
    AFTERNOON_SNACK("AFTERNOON_SNACK", "下午餐"),
    DINNER("DINNER", "晚餐"),
    NIGHT_SNACK("NIGHT_SNACK", "宵夜"),
    ;

    companion object {
        fun fromCode(code: String?): MealSlot = values().firstOrNull { it.code == code } ?: ALL
    }
}

object MealSlotMatcher {
    // 早餐：粥/蒸蛋/豆浆奶酸奶/燕麦麦片/面点(面·米粉·馒头包子花卷油条煎饼饼)/吐司面包三明治 等常见早餐。
    // [AI修改] QW-3(用户2026-07-20#3):去裸"玉米/南瓜/薯"——它们把"松仁玉米/玉米排骨/南瓜排骨/拔丝红薯"等炒菜/正餐/甜点误判早餐;
    //   南瓜粥/红薯粥/南瓜饼仍由"粥/饼"覆盖。补"酸奶/麦片/米粉/花卷/油条/煎饼/吐司/面包/三明治"提升早餐覆盖。
    private val BREAKFAST = listOf(
        "粥", "蛋羹", "蒸蛋", "水煮蛋", "豆浆", "豆奶", "牛奶", "酸奶", "燕麦", "麦片",
        "面", "米粉", "馒头", "包子", "花卷", "油条", "煎饼", "饼", "吐司", "面包", "三明治",
    )
    // 加餐/宵夜：偏轻(粥/奶/蛋羹/薯玉米/汤面) 等好克化的。
    private val LIGHT = listOf(
        "粥", "豆浆", "豆奶", "牛奶", "燕麦", "蛋羹", "蒸蛋", "薯", "玉米", "南瓜", "汤", "面",
    )
    // 正餐(中/晚)排除的"纯饮品"整道菜。
    private val DRINK_ONLY = listOf("豆浆", "豆奶", "牛奶", "燕麦牛奶")

    // [AI生成] 荤菜主料关键词(判荤/素用于周期规划荤素搭配)。
    // [AI修改] 用具体词避免裸字误伤素菜：不用裸"蟹"(否则"蟹味菇"被误判)、不用裸"参/血/肚"，改具体名。
    private val MEAT = listOf(
        "肉", "鱼", "虾", "鸡", "鸭", "鹅", "牛", "羊", "猪", "排骨", "五花", "蛋",
        "鱿", "鳝", "鳅", "蚝", "海参", "鲈", "鲫", "鲳", "鳕", "带鱼", "黄鱼", "扇贝", "蛤", "蛏", "生蚝", "田螺",
        "腊", "香肠", "火腿", "培根", "午餐肉", "猪肚", "牛肚", "猪肝", "鸭血", "猪血", "猪蹄",
    )

    /** 主料是否含荤(任一主料名命中荤关键词即视为荤菜)。[AI生成] */
    fun isMeatByMains(mainNames: List<String>): Boolean = mainNames.any { m -> MEAT.any { m.contains(it) } }

    /** 该菜是否适合此餐次。[AI生成] 全部=都适合。 */
    fun matches(slot: MealSlot, dishName: String): Boolean = when (slot) {
        MealSlot.ALL -> true
        MealSlot.BREAKFAST -> BREAKFAST.any { dishName.contains(it) }
        MealSlot.MORNING_SNACK, MealSlot.AFTERNOON_SNACK, MealSlot.NIGHT_SNACK -> LIGHT.any { dishName.contains(it) }
        MealSlot.LUNCH, MealSlot.DINNER -> DRINK_ONLY.none { dishName == it } // 正餐排除纯饮品，其余(炒/炖/红烧等)都算
    }

    // [AI生成] v28：所有可打标的具体餐次(不含 ALL)，供"默认推断器"遍历。
    private val REAL_SLOTS = listOf(
        MealSlot.BREAKFAST, MealSlot.MORNING_SNACK, MealSlot.LUNCH,
        MealSlot.AFTERNOON_SNACK, MealSlot.DINNER, MealSlot.NIGHT_SNACK,
    )

    /**
     * 按菜名推断"默认适合餐次"(生成初始值/兜底用，恒非空)。[AI生成]
     *
     * v28 起 Matcher 从"实时筛选器"降级为"生成默认值的推断器"：新建/回填菜品时推断初始餐次，
     * 之后以存储的 dish_meal_slot 为准。推不出任何餐次时兜底正餐(中/晚)，保证"永不出现无餐次菜"。
     */
    fun defaultSlotsFor(dishName: String): List<MealSlot> {
        val hit = REAL_SLOTS.filter { matches(it, dishName) }
        return hit.ifEmpty { listOf(MealSlot.LUNCH, MealSlot.DINNER) }
    }
}
