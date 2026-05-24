package com.sxdbsm.cookbook.domain.model

data class Dish(
    val id: Long = 0,
    val name: String,
    val cookingMethodId: Long? = null,
    val cookingMethodName: String? = null,
    /** 热度 0-100，自动累加，UI 不暴露编辑 */
    val preference: Double = 0.0,
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
    val source: String = "user",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val tags: List<String> = emptyList(),
    val ingredients: List<DishIngredient> = emptyList(),
) {
    /** 热度星级（0-5） */
    val popularityStars: Float get() = (preference / 20.0).toFloat().coerceIn(0f, 5f)
}

data class DishIngredient(
    val ingredient: Ingredient,
    val quantity: Double? = null,
    val unitId: Long? = null,
    val unitName: String = "",
    val isMain: Boolean = true,
)

/** 列表/卡片用的菜品轻量信息 */
data class DishMini(
    val id: Long,
    val name: String,
    val imagePath: String = "",
    val tags: List<String> = emptyList(),
    val preference: Double = 0.0,
    val mainIngredientNames: List<String> = emptyList(),
    val cookingMethodName: String? = null,
)
