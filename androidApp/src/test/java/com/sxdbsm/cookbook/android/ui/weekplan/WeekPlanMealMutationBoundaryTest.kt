package com.sxdbsm.cookbook.android.ui.weekplan

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.android.ui.meal.MealDayMutationPort
import com.sxdbsm.cookbook.android.ui.meal.MealDayUndoToken
import com.sxdbsm.cookbook.data.repository.MealProjectionRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WeekPlanMealMutationBoundaryTest {
    @Test fun publicUndoFlowDoesNotExposeUndoForEmptyDay() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver); Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repository = MealRecordRepository(CookbookDatabase(driver))
            val port = CountingPort()
            val vm = WeekPlanViewModel(MealProjectionRepository(repository), MealRecordUseCase(repository), port)
            var shown = 0
            vm.deleteDayUndoable(LocalDate(2026, 8, 26)) { shown++ }
            assertEquals(1, port.undoCalls); assertEquals(0, shown)
        } finally { Dispatchers.resetMain(); driver.close() }
    }
    @Test fun publicUndoFlowRestoresSuccessTokenOnce() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY); CookbookDatabase.Schema.create(driver); Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repository = MealRecordRepository(CookbookDatabase(driver)); val port = CountingPort(MealDayUndoToken(Any()))
            val vm = WeekPlanViewModel(MealProjectionRepository(repository), MealRecordUseCase(repository), port)
            var shown = 0; var undo: (() -> Unit)? = null
            vm.deleteDayUndoable(LocalDate(2026, 8, 26)) { shown++; undo = it }
            assertEquals(1, port.undoCalls); assertEquals(1, shown)
            undo!!.invoke(); assertEquals(1, port.restoreCalls)
        } finally { Dispatchers.resetMain(); driver.close() }
    }

    @Test fun publicMutationFailuresDoNotExposeUndoOrRepeatItAfterRestoreFailure() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY); CookbookDatabase.Schema.create(driver); Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val repository = MealRecordRepository(CookbookDatabase(driver))
            val failingDelete = CountingPort(deleteError = IllegalStateException("delete failed"))
            val vm = WeekPlanViewModel(MealProjectionRepository(repository), MealRecordUseCase(repository), failingDelete)
            var shown = 0
            vm.deleteDayUndoable(LocalDate(2026, 8, 26)) { shown++ }
            assertEquals(1, failingDelete.undoCalls); assertEquals(0, shown)
            vm.deleteDay(LocalDate(2026, 8, 27)); assertEquals(1, failingDelete.deleteCalls)

            val restoreFailing = CountingPort(MealDayUndoToken(Any()), restoreError = IllegalStateException("restore failed"))
            val restoreVm = WeekPlanViewModel(MealProjectionRepository(repository), MealRecordUseCase(repository), restoreFailing)
            var restoreShown = 0; var undo: (() -> Unit)? = null
            restoreVm.deleteDayUndoable(LocalDate(2026, 8, 26)) { restoreShown++; undo = it }
            undo!!.invoke()
            assertEquals(1, restoreShown); assertEquals(1, restoreFailing.restoreCalls)
        } finally { Dispatchers.resetMain(); driver.close() }
    }
    private class CountingPort(
        private val token: MealDayUndoToken? = null,
        private val deleteError: Throwable? = null,
        private val restoreError: Throwable? = null,
    ) : MealDayMutationPort {
        var deleteCalls = 0
        var undoCalls = 0
        var restoreCalls = 0
        override suspend fun deleteDay(date: LocalDate) { deleteCalls++; deleteError?.let { throw it } }
        override suspend fun deleteDayWithUndo(date: LocalDate): MealDayUndoToken? { undoCalls++; deleteError?.let { throw it }; return token }
        override suspend fun restoreDeletedDay(token: MealDayUndoToken) { restoreCalls++; restoreError?.let { throw it } }
    }
}
