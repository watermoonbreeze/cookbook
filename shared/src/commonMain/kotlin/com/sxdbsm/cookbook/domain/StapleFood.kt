package com.sxdbsm.cookbook.domain

/**
 * @File : StapleFood
 * @Time : 2026/07/13
 * @Author : SXD-AI
 * @Desc : 主食判定(谷薯主食类)——按菜名/主料名关键词启发式识别
 * <p>
 * 供餐次展示"主食置顶+角标"与周期规划"体现主食"复用。判定依据谷薯主食类食材/常见主食菜名，
 * 与 food_categories 的 staple(谷薯主食类) 语义对齐；启发式、非精确，可后续细化。
 * <p>
 * [AI生成] 餐次主食置顶 + 周期规划主食搭配。
 **/
object StapleFood {

    // 主食关键词：米饭/粥/面点/薯芋/玉米/年糕/米粉等谷薯主食。
    // 避免用裸"米"(会误伤虾米/花生米)，用具体名(大米/小米/糯米…)。
    private val KEYWORDS = listOf(
        "米饭", "饭", "粥", "面条", "面", "馒头", "包子", "饺子", "馄饨", "花卷",
        "玉米", "红薯", "紫薯", "土豆", "山药", "芋头", "年糕", "米线", "河粉",
        "粉条", "粉丝", "面包", "饼", "糯米", "黑米", "藜麦", "燕麦", "荞麦",
        "小米", "大米", "薏米", "大麦",
        // [AI修改] 补漏：馍(炕馍)、五色米/糙米/香米/粳米等杂粮米、糁(玉米糁)、米糕/汤圆/青团/粽等米面成品。裸"米"仍不用(避免虾米/花生米误伤)。
        "馍", "五色米", "糙米", "香米", "粳米", "糁", "米糕", "汤圆", "青团", "粽",
    )

    /** 该菜是否含主食(按菜名或任一主料名命中主食关键词)。[AI生成] */
    fun isStaple(dishName: String, mainIngredientNames: List<String>): Boolean {
        if (KEYWORDS.any { dishName.contains(it) }) return true
        return mainIngredientNames.any { ing -> KEYWORDS.any { ing.contains(it) } }
    }
}
