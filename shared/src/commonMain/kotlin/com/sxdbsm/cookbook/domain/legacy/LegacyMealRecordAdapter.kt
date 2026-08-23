package com.sxdbsm.cookbook.domain.legacy

import com.sxdbsm.cookbook.domain.mealrecording.MealRecord
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordId
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordLifecycle
import com.sxdbsm.cookbook.domain.model.MealRecord as LegacyMealRecord

/** Maps the existing meal_record-backed model without changing its storage contract. */
object LegacyMealRecordAdapter {
    fun toDomain(legacy: LegacyMealRecord): MealRecord = MealRecord(
        id = MealRecordId(legacy.id.toString()),
        date = legacy.date,
        mealTypeId = legacy.mealTypeId,
        mealName = legacy.mealName,
        mealTime = legacy.mealTime,
        note = legacy.note,
        createdAt = legacy.createdAt,
        dishIds = legacy.dishes.map { it.id },
        lifecycle = MealRecordLifecycle.RECORDED,
    )
}
