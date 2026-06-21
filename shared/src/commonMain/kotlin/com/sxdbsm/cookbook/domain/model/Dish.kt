package com.sxdbsm.cookbook.domain.model

/**
 * 菜品的完整领域模型。[AI修改]
 *
 * Kotlin 的 `data class` 类似 Java 里的 POJO + 自动生成的 `equals/hashCode/toString/copy`。
 * 这里用于菜品详情页和编辑页，包含基础字段、标签、食材明细等完整信息。
 */
data class Dish(
    val id: Long = 0, // [AI修改] 主键；默认 0 表示还没有入库的新菜品。
    val name: String, // [AI修改] 菜名，Kotlin 非空 String 等价于 Java 中不允许为 null 的 String。
    val cookingMethodId: Long? = null, // [AI修改] `Long?` 表示可空 Long，对应 Java 的 Long nullable。
    val cookingMethodName: String? = null, // [AI修改] 展示用烹饪方式名称，来自字典表查询。
    val cookingMethods: List<CookingMethod> = emptyList(), // [AI生成] 支持一个菜品拥有多个烹饪方式。
    /** 喜爱度 0-1000，自动累加，UI 不暴露编辑。[AI修改] */
    val preference: Int = 0,
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
    val thumbnailPath: String = "", // [AI生成] 缩略图路径，列表优先展示；为空时回退 imagePath。
    val source: String = "user",
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val tags: List<String> = emptyList(), // [AI修改] 只读 List；修改时用 copy(tags = 新列表)。
    val ingredients: List<DishIngredient> = emptyList(), // [AI修改] 菜品关联的食材明细。
    val steps: List<DishStep> = emptyList(), // [AI生成] 菜品操作步骤，支持每一步记录文字和多张过程图。
) {
    /** 喜爱度星级（0-5） */
    val popularityStars: Float get() = (preference / 200.0).toFloat().coerceIn(0f, 5f)
}

/**
 * 菜品操作步骤模型。[AI生成]
 *
 * 一道菜可以有多步做法；每一步保存一段说明和若干本地图片路径。
 * 图片路径沿用菜品图片的 `|` 编码规则，避免 shared 层依赖平台媒体 API。
 */
data class DishStep(
    val id: Long = 0,
    val sortOrder: Int = 0,
    val text: String = "",
    val imagePath: String = "",
    val thumbnailPath: String = "",
)

/**
 * 菜品与食材的关联模型。[AI修改]
 *
 * 在 Java 里通常会写成 DishIngredient 类并持有 Ingredient 字段；
 * 这里用 data class 直接表达“某道菜用了某个食材、用量、单位、是否主料”。
 */
data class DishIngredient(
    val ingredient: Ingredient,
    val quantity: Double? = null,
    val unitId: Long? = null,
    val unitName: String = "",
    val isMain: Boolean = true,
)

/**
 * 列表/卡片用的菜品轻量信息。[AI修改]
 *
 * 详情页不使用它，因为它只保留渲染列表需要的字段，避免每行都查完整食材和标签。
 */
data class DishMini(
    val id: Long,
    val name: String,
    val imagePath: String = "",
    val thumbnailPath: String = "", // [AI生成] 列表缩略图路径，降低滚动解码成本。
    val tags: List<String> = emptyList(),
    val preference: Int = 0,
    val mainIngredientNames: List<String> = emptyList(),
    val cookingMethodName: String? = null,
    val cookingMethodNames: List<String> = emptyList(), // [AI生成] 列表/搜索中展示多个烹饪方式。
)
