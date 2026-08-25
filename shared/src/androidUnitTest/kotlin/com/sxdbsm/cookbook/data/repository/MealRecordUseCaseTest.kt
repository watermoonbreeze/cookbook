package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.mealrecording.MealRecordLifecycle
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordDraft
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/** Phase 2：create/save/query 入口必须经 UseCase，并返回 Domain 生命周期结果。 */
class MealRecordUseCaseTest {
    @Test
    fun createAndQueryUseDomainBoundaryWhileKeepingLegacyStorage() = runBlocking {
        val database = RepositoryTestDatabase.create()
        database.cookbookQueries.insertMealType("BREAKFAST", "存储早餐", "07:30", 1L, "preset")
        val useCase = MealRecordUseCase(MealRecordRepository(database))
        val date = LocalDate(2026, 8, 23)
        val draft = MealRecordDraft(1L, date, LocalTime(8, 0), note = "用户备注")

        val created = useCase.create(draft)

        assertEquals(MealRecordLifecycle.RECORDED, created.lifecycle)
        assertEquals("1", created.id.value)
        assertEquals("存储早餐", created.mealName)
        assertEquals("用户备注", created.note)
        assertEquals(date, created.date)
        assertEquals(1L, created.mealTypeId)
        assertEquals(LocalTime(8, 0), created.mealTime)
        kotlin.test.assertTrue(created.createdAt > 0L)
        assertEquals(emptyList(), created.dishIds)
        assertEquals(date to date, useCase.dateRange())
        assertEquals(1, useCase.queryDayForEdit(date).size)
    }

    @Test
    fun saveDayRejectsMixedDatesBeforeCallingLegacyAdapter() = runBlocking {
        val useCase = MealRecordUseCase(MealRecordRepository(RepositoryTestDatabase.create()))
        val first = MealRecordDraft(1L, LocalDate(2026, 8, 23), LocalTime(8, 0))
        val second = first.copy(date = LocalDate(2026, 8, 24))

        assertFailsWith<IllegalArgumentException> {
            useCase.saveDay(first.date, listOf(first, second))
        }
        Unit
    }

    @Test
    fun saveDayReturnsRecordsReadBackFromStorage() = runBlocking {
        val database = RepositoryTestDatabase.create()
        database.cookbookQueries.insertMealType("BREAKFAST", "早餐", "07:30", 1L, "preset")
        database.cookbookQueries.insertMealType("LUNCH", "午餐", "12:00", 1L, "preset")
        val useCase = MealRecordUseCase(MealRecordRepository(database))
        val date = LocalDate(2026, 8, 23)

        val records = useCase.saveDay(
            date,
            listOf(
                MealRecordDraft(1L, date, LocalTime(8, 0), note = "早餐备注"),
                MealRecordDraft(2L, date, LocalTime(12, 0), note = "午餐备注"),
            ),
        )

        assertEquals(listOf("早餐", "午餐"), records.map { it.mealName })
        assertEquals(listOf("早餐备注", "午餐备注"), records.map { it.note })
        kotlin.test.assertTrue(records.all { it.createdAt > 0L })
        assertEquals(listOf("1", "2"), records.map { it.id.value })
    }
}
