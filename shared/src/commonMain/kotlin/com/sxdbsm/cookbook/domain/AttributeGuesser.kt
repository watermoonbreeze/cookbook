package com.sxdbsm.cookbook.domain

/**
 * @File : AttributeGuesser
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 自建食材「属性标签」按名智能推断（L2）——用户填名→推断可能的 {@link FoodAttribute}→UI 提示用户确认→生成 user care。
 * <p>
 * 照 {@link NutritionGuesser} 范式（按名·保守·可撤）：**关键词明确命中才推、带排除词防误判**（如"酒酿/苹果醋"非酒、"鱼肝油"非内脏），
 * 宁漏不误（过度忌口=误导，见果糖-水果教训）。推断结果仅作预填候选，**最终由用户在编辑页确认/取消**（提示确认·透明 T2）。
 * 与 L3 通俗勾选（{@code FoodAttribute.display}）互补：推断自动带出、勾选让用户主动补。
 * <p>
 * [AI生成] 自建食材双层判定 L2（用户 2026-07-22 定 L3+提示确认）。纯函数·可单测。
 **/
object AttributeGuesser {

    private data class Rule(
        val attr: FoodAttribute,
        val keywords: List<String>,
        val exclude: List<String> = emptyList(),
    )

    // 关键词表：明确命中才推、排除易混词。保守（宁漏不误）。改动须联网核实指南口径（脚本方案三-B）。
    private val RULES = listOf(
        Rule(FoodAttribute.CONTAINS_ALCOHOL,
            listOf("白酒", "啤酒", "黄酒", "米酒", "料酒", "红酒", "葡萄酒", "清酒", "洋酒", "伏特加", "威士忌", "白兰地", "鸡尾酒"),
            exclude = listOf("酒酿", "醋", "酒精棉")),
        Rule(FoodAttribute.PROCESSED_FRUCTOSE,
            listOf("可乐", "雪碧", "芬达", "汽水", "奶茶", "果汁饮料", "含糖饮料", "糖浆", "运动饮料", "能量饮料")),
        Rule(FoodAttribute.TRANS_FAT,
            listOf("植脂末", "奶精", "起酥", "氢化", "人造奶油", "代可可脂", "咖啡伴侣", "麦淇淋")),
        Rule(FoodAttribute.ORGAN_HIGH_CHOLESTEROL,
            listOf("肝", "腰花", "腰子", "脑花", "内脏", "肥肠", "大肠"),
            exclude = listOf("鱼肝油", "豆", "肝糖")),
        Rule(FoodAttribute.CURED_PROCESSED_MEAT,
            listOf("培根", "香肠", "腊肠", "火腿", "腊肉", "午餐肉", "热狗", "腊味", "咸肉", "风干肠")),
        Rule(FoodAttribute.RICH_BROTH,
            listOf("浓汤", "肉汤", "骨汤", "老火汤", "高汤", "火锅汤", "浓肉汤")),
        Rule(FoodAttribute.PICKLED_HIGH_SALT,
            listOf("咸菜", "腐乳", "咸鸭蛋", "咸蛋", "酱菜", "皮蛋", "榨菜", "泡菜", "咸鱼", "梅干菜", "酸菜", "腌菜")),
        Rule(FoodAttribute.DEEP_FRIED,
            listOf("油炸", "炸鸡", "薯条", "油条", "油饼", "炸串", "天妇罗", "炸鸡排", "炸鸡块")),
    )

    /**
     * 按食材名推断属性标签（去重·保守）。空=未命中/无属性。[AI生成]
     * @param name 食材名
     */
    fun guess(name: String): List<FoodAttribute> {
        val n = name.trim()
        if (n.isEmpty()) return emptyList()
        return RULES
            .filter { r -> r.keywords.any { n.contains(it) } && r.exclude.none { n.contains(it) } }
            .map { it.attr }
            .distinct()
    }
}
