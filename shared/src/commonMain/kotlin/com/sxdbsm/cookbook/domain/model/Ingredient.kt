package com.sxdbsm.cookbook.domain.model

data class Ingredient(
    val id: Long,
    val name: String,
    val alias: String = "",
    val pinyin: String = "",
    val imagePath: String = "",
    val defaultUnitId: Long? = null,
    val defaultUnitName: String = "",
    val source: String = "preset",
    /** 仅在按人群分类查询时附带 */
    val adviceLevel: AdviceLevel? = null,
    val adviceReason: String = "",
)

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

data class MeasurementUnit(
    val id: Long,
    val name: String,
)

data class CookingMethod(
    val id: Long,
    val name: String,
)
