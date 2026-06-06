package com.sxdbsm.cookbook.domain.model

/**
 * @File : FavoriteCombo
 * @Time : 2026/06/06
 * @Author : SXD-AI
 * @Desc : 收藏菜品组合领域模型
 * <p>
 * 用于添加餐食时一键加入一组常吃菜品，也支持把当前餐食模块内的菜品保存成组合。
 * <p>
 * [AI生成] MVP 剩余项要求补齐收藏组合入口，因此新增 shared 层领域模型。
 **/
data class FavoriteCombo(
    val id: Long,
    val name: String,
    val dishes: List<DishMini> = emptyList(),
)
