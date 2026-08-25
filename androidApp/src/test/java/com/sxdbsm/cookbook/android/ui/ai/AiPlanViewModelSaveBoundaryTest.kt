package com.sxdbsm.cookbook.android.ui.ai

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.MockAiRuntime
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.model.DayPlan
import com.sxdbsm.cookbook.ai.model.PeriodPlan
import com.sxdbsm.cookbook.ai.model.PlannedMeal
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanDayDraft
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanSaveResult
import com.sxdbsm.cookbook.usecase.mealplanning.MealPlanSaveUseCase
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AiPlanViewModelSaveBoundaryTest {
    @Test
    fun `T-P4-11 duplicate save and overwrite confirmations start one request each`() {
        val dispatcher = StandardTestDispatcher()
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
        try {
            val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
            CookbookDatabase.Schema.create(driver)
            val db = CookbookDatabase(driver)
            val prefs = PreferenceRepository(db)
            val mealRepo = MealRecordRepository(db)
            val fake = BlockingPlanSaveUseCase(MealRecordUseCase(mealRepo))
            val vm = AiPlanViewModel(
                RecommendationDataSource(db, PantryRepository(db), DishRepository(db), FamilyRepository(db, prefs), IngredientRepository(db), NutritionRepository(db)),
                mealRepo, MockAiRuntime(), AiRuntimeConfig(prefs), prefs, NutritionRepository(db), fake,
            )
            val plan = PeriodPlan(listOf(DayPlan(0, listOf(PlannedMeal("午餐", emptyList())))), false, 0.0)
            vm.state = AiPlanUiState(plan = plan, planStartDate = LocalDate(2026, 8, 25))

            vm.save()
            vm.save()
            dispatcher.scheduler.runCurrent()
            assertEquals(1, fake.calls)

            vm.state = vm.state.copy(saving = false, pendingConflictDates = setOf(LocalDate(2026, 8, 25)))
            vm.confirmOverwrite()
            vm.confirmOverwrite()
            dispatcher.scheduler.runCurrent()
            assertEquals(2, fake.calls)
        } finally {
            kotlinx.coroutines.Dispatchers.resetMain()
        }
    }

    private class BlockingPlanSaveUseCase(records: MealRecordUseCase) : MealPlanSaveUseCase(records) {
        var calls = 0
        override suspend fun save(days: List<MealPlanDayDraft>, confirmedConflicts: Set<LocalDate>?): MealPlanSaveResult {
            calls++
            return MealPlanSaveResult.Saved(emptyList())
        }
    }
}
