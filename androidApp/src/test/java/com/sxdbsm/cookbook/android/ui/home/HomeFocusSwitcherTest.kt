package com.sxdbsm.cookbook.android.ui.home

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealProjectionRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.PresentFocusSelection
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.FamilyMember
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeFocusSwitcherTest {

    @Test
    fun presentFocusSelectionOnlyPublishesPresentTabsAndEffectiveViewer() {
        val dad = FamilyMember(id = 2L, name = "爸", isFocus = true)
        val switcher = PresentFocusSelection(
            members = listOf(dad),
            viewing = dad,
            share = 1.0,
            requiresViewingFallback = true,
        ).toFocusSwitcher()

        assertEquals(listOf("爸"), switcher.members.map { it.name })
        assertEquals(dad.id, switcher.viewingId)
    }

    @Test
    fun absentViewingMemberFallsBackThroughRealHomeViewModelFlow() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val db = CookbookDatabase(driver)
            val q = db.cookbookQueries
            val prefs = PreferenceRepository(db)
            val dishRepo = DishRepository(db)
            val ingredientRepo = IngredientRepository(db)
            val nutritionRepo = NutritionRepository(db)
            val family = FamilyRepository(db, prefs)
            val mealRepo = MealRecordRepository(db)
            family.ensureInitialized()
            val me = family.listMembers().single()
            val dadId = family.createMember(FamilyMember(id = 0, name = "爸"))
            family.toggleFocus(dadId)
            prefs.setFocusViewingMemberId(me.id)

            q.insertMeasurementUnit("克", "preset", 1.0)
            val gram = q.lastInsertId().executeAsOne()
            q.insertIngredient("测试米", "", "test_rice", "", "", "🍚", gram, "preset", 0L)
            val ingredientId = q.lastInsertId().executeAsOne()
            q.upsertIngredientNutrition(ingredientId, 350.0, 7.0, 1.0, 78.0, 1.0, 1.0, 1.0, 1.0, 50.0, 1.0, 0.0, 0.0, null, "test", 1L, 0L)
            q.insertDish("测试饭", null, "", "", "", "", "user", 0L, 0L, "")
            val dishId = q.lastInsertId().executeAsOne()
            q.insertDishIngredient(dishId, ingredientId, 100.0, gram, 1L)
            q.insertMealType("LUNCH", "午餐", "12:00", 1L, "preset")
            val lunchId = q.lastInsertId().executeAsOne()
            val today = DateTime.today()
            mealRepo.saveDayMeals(today, listOf(DayMealDraft(lunchId, LocalTime(12, 0), "", listOf(dishId))))
            family.setAbsent(DateTime.formatDate(today), me.id, true)

            val vm = HomeViewModel(
                dishRepo, MealRecordUseCase(mealRepo), MealProjectionRepository(mealRepo), prefs, nutritionRepo,
                family, ingredientRepo, HealthProfileRepository(db),
                RecommendationDataSource(db, PantryRepository(db), dishRepo, family, ingredientRepo, nutritionRepo),
            )
            val nutrition = withTimeout(5_000) { vm.todayNutrition.first { it != null } }
            val switcher = withTimeout(5_000) { vm.focusSwitcher.first { it.members.isNotEmpty() } }

            assertEquals(dadId, switcher.viewingId)
            assertEquals(listOf(dadId), switcher.members.map { it.id })
            assertEquals(true, (nutrition?.kcal ?: 0) > 0)
            assertEquals(dadId, family.observeViewingMember().first()?.id)
        } finally {
            Dispatchers.resetMain()
            driver.close()
        }
    }
}
