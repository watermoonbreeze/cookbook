package com.sxdbsm.cookbook.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 餐次类型字典，例如早餐、中餐、晚餐。[AI修改]
 *
 * `LocalTime` 来自 kotlinx-datetime，是 KMP 可用的时间类型，不依赖 Java 8 `java.time`。
 */
data class MealType(
    val id: Long,
    val code: String,
    val name: String,
    val defaultTime: LocalTime,
    val isFixed: Boolean,
)

/**
 * 一次餐食记录。[AI修改]
 *
 * 表示某一天某个餐次实际或计划吃了哪些菜。`dishes` 使用轻量 `DishMini`，
 * 因为餐食列表只需要展示菜名、图片和热度。
 */
data class MealRecord(
    val id: Long = 0,
    val date: LocalDate,
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val note: String = "",
    val createdAt: Long = 0,
    val dishes: List<DishMini> = emptyList(),
)

/**
 * 一整天的餐食卡片（HomeScreen 计划 / FoodTimelineScreen 列表共用）。[AI修改]
 */
data class DayMealCardData(
    val date: LocalDate,
    val isToday: Boolean,
    val isPlanState: Boolean,
    val meals: List<MealSection>,
)

/**
 * 一天卡片里的一个餐次分组。[AI修改]
 *
 * 例如“早餐 07:30”下面挂若干道菜。
 */
data class MealSection(
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val dishes: List<DishMini>,
    val mealRecordId: Long? = null,
    val note: String = "", // [AI修改] 编辑餐食时需要回填该餐次备注。
)
