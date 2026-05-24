package com.sxdbsm.cookbook.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class MealType(
    val id: Long,
    val code: String,
    val name: String,
    val defaultTime: LocalTime,
    val isFixed: Boolean,
)

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

/** 一整天的餐食卡片（HomeScreen 计划 / FoodTimelineScreen 列表共用） */
data class DayMealCardData(
    val date: LocalDate,
    val isToday: Boolean,
    val isPlanState: Boolean,
    val meals: List<MealSection>,
)

data class MealSection(
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val dishes: List<DishMini>,
    val mealRecordId: Long? = null,
)
