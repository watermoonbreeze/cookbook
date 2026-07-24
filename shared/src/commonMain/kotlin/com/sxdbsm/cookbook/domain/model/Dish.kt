package com.sxdbsm.cookbook.domain.model

import com.sxdbsm.cookbook.ai.MealSlot

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
    val cuisine: String = "", // [AI生成] 菜系(家常菜/川菜等)，空=未分类。
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
    val tags: List<String> = emptyList(), // [AI修改] 只读 List；修改时用 copy(tags = 新列表)。
    val ingredients: List<DishIngredient> = emptyList(), // [AI修改] 菜品关联的食材明细。
    val steps: List<DishStep> = emptyList(), // [AI生成] 菜品操作步骤，支持每一步记录文字和多张过程图。
    /** [AI生成] v28：适合餐次(早/上午/中/下午/晚/宵夜，可多值)。编辑回显存储值(空=老库未打标)。 */
    val mealSlots: List<MealSlot> = emptyList(),
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
    /**
     * [AI生成] J15：本菜**全部食材名**(含非主料)。仅供营养大类判定"主料空则回退全食材"用——
     * 修 is_main 全0/缺标的问题菜(简单菜/自建菜/盖浇饭·mainIngredientNames 恒空)漏判主食/蛋白。
     * 与 mainIngredientNames 一样**仅在餐次上下文**(buildDishesByMealRecord)真填充，其余场景恒空(勿裸用)。
     */
    val allIngredientNames: List<String> = emptyList(),
    val cookingMethodName: String? = null,
    val cookingMethodNames: List<String> = emptyList(), // [AI生成] 列表/搜索中展示多个烹饪方式。
    val source: String = "user", // [AI生成] 'preset'=预设(不可直接编辑,复制后编辑)/'user'=自建。
    val cuisine: String = "", // [AI生成] 菜系(家常菜/川菜等)，空=未分类；用于列表按菜系筛选。
    val shortageIngredients: List<String> = emptyList(), // [AI生成] 该菜在此餐次缺料的库存食材名(份数用尽)；非空则灰显"缺"
    val purchaseIngredients: List<String> = emptyList(), // [AI生成] 该菜主料不在库存的食材名(需采购)；非空则灰显"采购"
    /** [AI生成] v28：适合餐次(列表/推荐/记一餐按餐次筛)。buildDishMinis 已填(存储值优先、未打标回退 MealSlotMatcher 兜底，恒非空)。 */
    val mealSlots: List<MealSlot> = emptyList(),
    /**
     * [AI生成] 食用比例(是否吃完)：这道菜这一餐实际吃掉的比例[0,1]，默认 1.0=吃完。
     * **仅在"餐次上下文"(buildDishesByMealRecord)才真赋值**；库列表/推荐/搜索等非餐次场景恒为默认 1.0(不消费·无意义)。
     * 个人摄入折算 = 整份 × eatenRatio × share(见 IntakeCalculator)。用它前先确认来自餐次查询(踩坑红线:DishMini 默认字段勿裸用)。
     */
    val eatenRatio: Double = 1.0,
)

/**
 * 按食材匹配出来的菜品。[AI生成]
 *
 * `matchCount` 表示用户选择的食材命中了多少个，`totalIngredientCount` 表示菜品全部食材数。
 */
data class DishIngredientMatch(
    val dish: DishMini,
    val matchCount: Int,
    val totalIngredientCount: Int,
) {
    val missingCount: Int get() = (totalIngredientCount - matchCount).coerceAtLeast(0)
}
