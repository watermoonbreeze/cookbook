package com.sxdbsm.cookbook.domain.model

/**
 * 食材领域模型。[AI修改]
 *
 * 这是 shared 层给 Android/iOS 共用的数据结构。字段上的默认值让调用方可以只传必要参数，
 * 类似 Java 中写多个构造方法或 Builder。
 */
data class Ingredient(
    val id: Long,
    val name: String,
    val alias: String = "",
    val pinyin: String = "",
    val imagePath: String = "",
    val thumbnailPath: String = "", // [AI生成] 食材缩略图路径，选择列表优先展示。
    val emoji: String = "🥗", // [AI生成] 无图片时展示的默认食物图标，预设食材由 JSON 维护。
    val defaultUnitId: Long? = null,
    val defaultUnitName: String = "",
    val source: String = "preset",
    /** 仅在按人群分类查询时附带。[AI修改] */
    val adviceLevel: AdviceLevel? = null,
    val adviceReason: String = "",
    /** 有效性：1 有效，0 失效。默认 1；仅在需要展示失效态（菜品引用、回收站）的查询里带真实值。[AI生成] */
    val status: Int = 1,
    /** 失效原因：status=0 时说明后台下架/用户删除等原因。[AI生成] */
    val reason: String = "",
)

/**
 * 食材详情扩展信息。[AI生成]
 *
 * 后续食材详情页、新增/编辑食材页共用这组字段，避免 UI 临时拼接文本。
 */
data class IngredientDetail(
    val ingredientId: Long,
    val commonMethods: String = "",
    val prepTips: String = "",
    val eatingNotes: String = "",
    val storageTips: String = "",
    val healthNote: String = "",
    val updatedAt: Long = 0L,
)

/**
 * 食材调养规则。[AI生成]
 *
 * 用于病种、人群、身体状态等“推荐/限量/避免”规则，关联到调养分类节点。
 */
data class IngredientCareRule(
    val id: Long = 0L,
    val ingredientId: Long,
    val categoryId: Long,
    val categoryName: String = "",
    val adviceLevel: AdviceLevel,
    val reason: String = "",
    val source: String = "user",
)

/**
 * 食材建议等级。[AI修改]
 *
 * enum class 等价于 Java enum；companion object 类似 Java 里的 static 工具区。
 */
enum class AdviceLevel { RECOMMEND, LIMIT, AVOID;
    companion object {
        fun fromCode(code: String?): AdviceLevel? = when (code?.lowercase()) {
            "recommend" -> RECOMMEND
            "limit" -> LIMIT
            "avoid" -> AVOID
            else -> null
        }
    }
}

/**
 * 计量单位字典项，例如“克”“个”“勺”。[AI修改]
 */
data class MeasurementUnit(
    val id: Long,
    val name: String,
    val preset: Boolean = true, // [AI生成] 预设单位不可删；用户自建(source=user)可删。
)

/**
 * 烹饪方式字典项，例如“炒”“蒸”“炖”。[AI修改]
 */
data class CookingMethod(
    val id: Long,
    val name: String,
    val preset: Boolean = false, // [AI生成] 预设(内置)方式不可删；用户自建可在库里删除
)

/** 菜品标签(标签库)。[AI生成] preset 预设不可删，用户自建可删。 */
data class DishTag(
    val id: Long,
    val name: String,
    val preset: Boolean = false,
)
