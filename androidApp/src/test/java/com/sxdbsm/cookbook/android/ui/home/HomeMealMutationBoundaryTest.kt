package com.sxdbsm.cookbook.android.ui.home

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.android.ui.meal.MealDayMutationPort
import com.sxdbsm.cookbook.android.ui.meal.MealDayUndoToken
import com.sxdbsm.cookbook.data.repository.*
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
class HomeMealMutationBoundaryTest {
    @Test fun publicUndoFlowHonorsNullAndRestoresOnlyOnce() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver); Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val db = CookbookDatabase(driver)
            val preferences = PreferenceRepository(db)
            val dish = DishRepository(db); val ingredient = IngredientRepository(db); val nutrition = NutritionRepository(db)
            val family = FamilyRepository(db, preferences); val health = HealthProfileRepository(db)
            val recommendation = RecommendationDataSource(db, PantryRepository(db), dish, family, ingredient, nutrition)
            val repo = MealRecordRepository(db)
            val port = CountingPort()
            val vm = HomeViewModel(dish, MealRecordUseCase(repo), MealProjectionRepository(repo), preferences, nutrition, family, ingredient, health, recommendation, port)
            var shown = 0
            vm.deleteDayUndoable(LocalDate(2026, 8, 26)) { shown++ }
            assertEquals(1, port.undoCalls); assertEquals(0, shown)
        } finally { Dispatchers.resetMain(); driver.close() }
    }
    @Test fun publicUndoFlowRestoresCapturedTokenOnce() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver); Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val db = CookbookDatabase(driver); val preferences = PreferenceRepository(db)
            val dish = DishRepository(db); val ingredient = IngredientRepository(db); val nutrition = NutritionRepository(db)
            val family = FamilyRepository(db, preferences); val repo = MealRecordRepository(db)
            val port = CountingPort(MealDayUndoToken(Any()))
            val vm = HomeViewModel(dish, MealRecordUseCase(repo), MealProjectionRepository(repo), preferences, nutrition, family, ingredient, HealthProfileRepository(db), RecommendationDataSource(db, PantryRepository(db), dish, family, ingredient, nutrition), port)
            var shown = 0; var undo: (() -> Unit)? = null
            vm.deleteDayUndoable(LocalDate(2026, 8, 26)) { shown++; undo = it }
            assertEquals(1, shown); undo!!.invoke(); assertEquals(1, port.restoreCalls)
        } finally { Dispatchers.resetMain(); driver.close() }
    }

    @Test fun publicUndoFlowDoesNotShowForDeleteFailureAndDeleteRunsOnce() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver); Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val db = CookbookDatabase(driver); val preferences = PreferenceRepository(db)
            val dish = DishRepository(db); val ingredient = IngredientRepository(db); val nutrition = NutritionRepository(db)
            val family = FamilyRepository(db, preferences); val repo = MealRecordRepository(db)
            val port = CountingPort(deleteError = IllegalStateException("delete failed"))
            val vm = HomeViewModel(dish, MealRecordUseCase(repo), MealProjectionRepository(repo), preferences, nutrition, family, ingredient, HealthProfileRepository(db), RecommendationDataSource(db, PantryRepository(db), dish, family, ingredient, nutrition), port)
            var shown = 0
            vm.deleteDayUndoable(LocalDate(2026, 8, 26)) { shown++ }
            assertEquals(1, port.undoCalls); assertEquals(0, shown)
            vm.deleteDay(LocalDate(2026, 8, 27)); assertEquals(1, port.deleteCalls)
        } finally { Dispatchers.resetMain(); driver.close() }
    }
    private class CountingPort(
        private val token: MealDayUndoToken? = null,
        private val deleteError: Throwable? = null,
    ) : MealDayMutationPort {
        var deleteCalls = 0
        var undoCalls = 0
        var restoreCalls = 0
        override suspend fun deleteDay(date: LocalDate) { deleteCalls++; deleteError?.let { throw it } }
        override suspend fun deleteDayWithUndo(date: LocalDate): MealDayUndoToken? { undoCalls++; deleteError?.let { throw it }; return token }
        override suspend fun restoreDeletedDay(token: MealDayUndoToken) { restoreCalls++ }
    }
}
