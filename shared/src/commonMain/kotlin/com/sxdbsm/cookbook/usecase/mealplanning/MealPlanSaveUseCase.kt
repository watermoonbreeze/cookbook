package com.sxdbsm.cookbook.usecase.mealplanning

import com.sxdbsm.cookbook.domain.mealplanning.MealPlan
import com.sxdbsm.cookbook.domain.mealplanning.MealPlanId
import com.sxdbsm.cookbook.domain.mealplanning.MealPlanLifecycle
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordDraft
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class MealPlanDayDraft(val date: LocalDate, val meals: List<MealPlanMealDraft>)

data class MealPlanMealDraft(
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val note: String = "",
    val dishIds: List<Long> = emptyList(),
)

sealed interface MealPlanSaveResult {
    data class Conflict(val dates: Set<LocalDate>) : MealPlanSaveResult
    data class Saved(val plans: List<MealPlan>) : MealPlanSaveResult
}

/**
 * Saves period plans through the legacy meal-record storage during the migration.
 * The legacy storage has no multi-day transaction, so every day is saved independently.
 */
open class MealPlanSaveUseCase(private val mealRecordUseCase: MealRecordUseCase) {
    open suspend fun save(
        days: List<MealPlanDayDraft>,
        confirmedConflicts: Set<LocalDate>? = null,
    ): MealPlanSaveResult {
        val saveableDays = days.filter { it.meals.isNotEmpty() }
        val currentConflicts = saveableDays.map { it.date }.distinct()
            .filterTo(linkedSetOf()) { mealRecordUseCase.queryDayForEdit(it).isNotEmpty() }
        if (currentConflicts.isNotEmpty() && currentConflicts != confirmedConflicts) {
            return MealPlanSaveResult.Conflict(currentConflicts)
        }

        val savedPlans = buildList {
            saveableDays.forEach { day ->
                val records = mealRecordUseCase.saveDay(
                    date = day.date,
                    drafts = day.meals.map { meal ->
                        MealRecordDraft(meal.mealTypeId, day.date, meal.mealTime, meal.note, meal.dishIds)
                    },
                )
                records.forEachIndexed { index, record ->
                    val meal = day.meals[index]
                    add(MealPlan(
                        id = MealPlanId("legacy-meal-record:${record.id.value}"),
                        date = record.date,
                        mealTypeId = record.mealTypeId,
                        mealName = meal.mealName,
                        mealTime = record.mealTime,
                        note = record.note,
                        dishIds = record.dishIds,
                        lifecycle = MealPlanLifecycle.PLANNED,
                    ))
                }
            }
        }
        return MealPlanSaveResult.Saved(savedPlans)
    }
}
