package com.sxdbsm.cookbook.usecase.mealplanning

import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.mealplanning.MealPlanLifecycle
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordDraft
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MealPlanSaveUseCaseTest {
    @Test
    fun `T-P4-01 conflict does not write until exact current set is confirmed`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        db.cookbookQueries.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val repository = MealRecordRepository(db)
        val useCase = MealPlanSaveUseCase(MealRecordUseCase(repository))
        val date = LocalDate(2026, 8, 25)
        val draft = MealPlanDayDraft(date, listOf(MealPlanMealDraft(1, "午餐", LocalTime(12, 0))))

        val initial = assertIs<MealPlanSaveResult.Saved>(useCase.save(listOf(draft)))
        assertEquals(MealPlanLifecycle.PLANNED, initial.plans.single().lifecycle)
        assertEquals("legacy-meal-record:1", initial.plans.single().id.value)

        val conflict = assertIs<MealPlanSaveResult.Conflict>(useCase.save(listOf(draft)))
        assertEquals(setOf(date), conflict.dates)
        assertEquals(1, repository.loadDayMealsForEdit(date).size)

        val saved = assertIs<MealPlanSaveResult.Saved>(useCase.save(listOf(draft), conflict.dates))
        assertEquals(1, saved.plans.size)
        assertEquals(1, repository.loadDayMealsForEdit(date).size)
    }

    @Test
    fun `T-P4-10 changed conflict set invalidates prior confirmation without replacing either day`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        db.cookbookQueries.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val repository = MealRecordRepository(db)
        val records = MealRecordUseCase(repository)
        val useCase = MealPlanSaveUseCase(records)
        val firstDate = LocalDate(2026, 8, 25)
        val secondDate = LocalDate(2026, 8, 26)
        val proposed = listOf(
            MealPlanDayDraft(firstDate, listOf(MealPlanMealDraft(1, "计划午餐", LocalTime(12, 0), note = "planned"))),
            MealPlanDayDraft(secondDate, listOf(MealPlanMealDraft(1, "计划午餐", LocalTime(12, 0), note = "planned"))),
        )

        records.saveDay(firstDate, listOf(MealRecordDraft(1, firstDate, LocalTime(12, 0), "original-first")))
        val initialConflict = assertIs<MealPlanSaveResult.Conflict>(useCase.save(proposed))
        assertEquals(setOf(firstDate), initialConflict.dates)

        records.saveDay(secondDate, listOf(MealRecordDraft(1, secondDate, LocalTime(12, 0), "original-second")))
        val beforeFirst = repository.loadDayMealsForEdit(firstDate).single()
        val beforeSecond = repository.loadDayMealsForEdit(secondDate).single()

        val changedConflict = assertIs<MealPlanSaveResult.Conflict>(useCase.save(proposed, initialConflict.dates))
        assertEquals(setOf(firstDate, secondDate), changedConflict.dates)
        assertEquals(beforeFirst.mealRecordId, repository.loadDayMealsForEdit(firstDate).single().mealRecordId)
        assertEquals("original-first", repository.loadDayMealsForEdit(firstDate).single().note)
        assertEquals(beforeSecond.mealRecordId, repository.loadDayMealsForEdit(secondDate).single().mealRecordId)
        assertEquals("original-second", repository.loadDayMealsForEdit(secondDate).single().note)
    }
}
