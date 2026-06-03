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
    val defaultUnitId: Long? = null,
    val defaultUnitName: String = "",
    val source: String = "preset",
    /** 仅在按人群分类查询时附带。[AI修改] */
    val adviceLevel: AdviceLevel? = null,
    val adviceReason: String = "",
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
)

/**
 * 烹饪方式字典项，例如“炒”“蒸”“炖”。[AI修改]
 */
data class CookingMethod(
    val id: Long,
    val name: String,
)
