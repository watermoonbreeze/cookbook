package com.sxdbsm.cookbook.android.ui.family

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.FamilyRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.FamilyMember
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
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyStatsViewModelTest {

    @Test
    fun absentMemberTabGetsZeroWhilePresentMemberGetsTodayMeal() = runBlocking {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        CookbookDatabase.Schema.create(driver)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        try {
            val db = CookbookDatabase(driver)
            val q = db.cookbookQueries
            val family = FamilyRepository(db, PreferenceRepository(db))
            val mealRepo = MealRecordRepository(db)
            family.ensureInitialized()
            val me = family.listMembers().single()
            val dadId = family.createMember(FamilyMember(id = 0, name = "Dad", portionCoefficient = me.portionCoefficient))

            q.insertMeasurementUnit("g", "preset", 1.0)
            val gram = q.lastInsertId().executeAsOne()
            q.insertIngredient("Rice", "", "rice", "", "", "", gram, "preset", 0L)
            val ingredientId = q.lastInsertId().executeAsOne()
            q.upsertIngredientNutrition(ingredientId, 350.0, 7.0, 1.0, 78.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, null, "test", 1L, 0L)
            q.insertDish("Rice dish", null, "", "", "", "", "user", 0L, 0L, "")
            val dishId = q.lastInsertId().executeAsOne()
            q.insertDishIngredient(dishId, ingredientId, 100.0, gram, 1L)
            q.insertMealType("LUNCH", "Lunch", "12:00", 1L, "preset")
            val lunchId = q.lastInsertId().executeAsOne()
            val today = DateTime.today()
            mealRepo.saveDayMeals(today, listOf(DayMealDraft(lunchId, LocalTime(12, 0), "", listOf(dishId))))
            mealRepo.saveDayMeals(DateTime.plusDays(today, -1), listOf(DayMealDraft(lunchId, LocalTime(12, 0), "", listOf(dishId))))
            val vm = FamilyStatsViewModel(family, mealRepo, NutritionRepository(db))
            vm.select(me.id)
            val bothPresentStats = withTimeout(5_000) { vm.stats.first { !it.isFamily && it.memberName == me.name && it.todayKcal > 0 } }
            assertEquals(175, bothPresentStats.todayKcal)

            family.setAbsent(DateTime.formatDate(today), me.id, true)
            val absentStats = withTimeout(5_000) { vm.stats.first { !it.isFamily && it.memberName == me.name } }
            assertEquals(0, absentStats.todayKcal)
            assertEquals(0, absentStats.proteinG)
            assertEquals(0, absentStats.fatG)
            assertEquals(0, absentStats.carbG)
            assertTrue(absentStats.dailyKcal.contains(175))

            vm.select(dadId)
            val presentStats = withTimeout(5_000) { vm.stats.first { !it.isFamily && it.memberName == "Dad" && it.todayKcal > 0 } }
            assertEquals(350, presentStats.todayKcal)
            assertEquals(7, presentStats.proteinG)
            assertEquals(1, presentStats.fatG)
            assertEquals(78, presentStats.carbG)
            assertTrue(presentStats.dailyKcal.contains(175))

            vm.select(null)
            val familyStats = withTimeout(5_000) { vm.stats.first { it.isFamily && it.breakdown.size == 2 } }
            assertEquals(350, familyStats.todayKcal)
            assertEquals(0, familyStats.breakdown.first { it.id == me.id }.kcal)
            assertEquals(false, familyStats.breakdown.first { it.id == me.id }.present)
            assertEquals(350, familyStats.breakdown.first { it.id == dadId }.kcal)
            assertEquals(true, familyStats.breakdown.first { it.id == dadId }.present)
        } finally {
            Dispatchers.resetMain()
            driver.close()
        }
    }
}
