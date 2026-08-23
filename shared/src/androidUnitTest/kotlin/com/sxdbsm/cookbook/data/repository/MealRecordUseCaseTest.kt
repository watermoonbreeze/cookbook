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
        val useCase = MealRecordUseCase(MealRecordRepository(RepositoryTestDatabase.create()))
        val date = LocalDate(2026, 8, 23)
        val draft = MealRecordDraft(1L, date, LocalTime(8, 0), note = "早餐")

        val created = useCase.create(draft)

        assertEquals(MealRecordLifecycle.RECORDED, created.lifecycle)
        assertEquals("1", created.id.value)
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
}
