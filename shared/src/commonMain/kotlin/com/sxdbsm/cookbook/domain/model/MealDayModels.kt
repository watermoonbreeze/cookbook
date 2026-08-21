package com.sxdbsm.cookbook.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * MDC2 stable read contract: 中性的一天餐食读取结果，不携带今天/未来等时间语义。
 *
 * Repository 的新读取路径应返回此模型；时间角色只能在 [MealDayCardProjector] 边界计算。
 */
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

/**
 * Shared meal read projection，供 Home、Timeline、Search 等 Feature 共用。
 *
 * 该模型只承载投影结果，不承载页面加载、编辑草稿或其他 Feature 状态。
 */
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
