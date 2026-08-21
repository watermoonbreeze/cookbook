package com.sxdbsm.cookbook.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** 中性的一天餐食读取结果，不携带今天/未来等时间语义。[AI修改] */
data class MealDayContent(
    val date: LocalDate,
    val meals: List<MealSection>,
)

/** 一天餐卡的唯一时间角色真相。[AI修改] */
enum class MealDayTemporalRole {
    PAST,
    TODAY,
    FUTURE,
}

/** 一整天的餐食 UI/read projection。[AI修改] */
data class DayMealCardData(
    val date: LocalDate,
    val temporalRole: MealDayTemporalRole,
    val meals: List<MealSection>,
) {
    val isToday: Boolean
        get() = temporalRole == MealDayTemporalRole.TODAY

    val isPlanState: Boolean
        get() = temporalRole == MealDayTemporalRole.FUTURE
}

/** 一天卡片里的一个餐次分组。 */
data class MealSection(
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val dishes: List<DishMini>,
    val mealRecordId: Long? = null,
    val note: String = "",
)
