package com.sxdbsm.cookbook.domain.model

/**
 * @File : IngredientGroup
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 常用配料组（可复用的一组食材，如"基础调料包"）
 * <p>
 * 家常菜"葱姜蒜盐生抽油"每道菜重加很累；把常用配料存成组，编辑菜品"配料组"一键加入食材清单。
 * 存食材名 + 是否主料；应用时按名解析(缺则兜底建)。source=preset(内置)/user(自建)。
 * <p>
 * [AI生成] B5：延续步骤模板复用思路到食材清单，降低录入成本。
 **/
data class IngredientGroup(
    val id: Long = 0,
    val name: String,
    val source: String = "user",
    val items: List<IngredientGroupItem> = emptyList(),
) {
    val isPreset: Boolean get() = source == "preset"
}

data class IngredientGroupItem(
    val name: String,
    val isMain: Boolean = false,
    val quantity: Double? = null, // [AI生成] 克数，应用到菜品时带过来
)
