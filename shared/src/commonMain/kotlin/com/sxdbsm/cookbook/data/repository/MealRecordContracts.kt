package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.datetime.LocalTime

/** 非持久化写命令；Manual / AI / undo restore 均通过 saveDayMeals() 落库。[AI修改] */
data class DayMealDraft(
    val mealTypeId: Long,
    val mealTime: LocalTime,
    val note: String,
    val dishIds: List<Long>,
)

/** 编辑页读取投影，不是新的持久化实体。 */
data class MealRecordEditData(
    val mealRecordId: Long,
    val mealTypeId: Long,
    val mealTime: LocalTime,
    val note: String,
    val dishes: List<DishMini>,
)
