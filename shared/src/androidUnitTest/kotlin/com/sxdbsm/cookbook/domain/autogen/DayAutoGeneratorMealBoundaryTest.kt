package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class DayAutoGeneratorMealBoundaryTest {
    @Test
    fun `T-P4-06 preview is zero-write and commit creates one day meal`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        q.insertDish("测试菜", null, "", "", "", "", "preset", 0, 0, "")
        val dishId = q.lastInsertId().executeAsOne()
        val mealType = MealType(1, "LUNCH", "午餐", LocalTime(12, 0), true)
        val context = AutoGenContext(emptyMap(), emptyList(), 1, emptySet(), listOf(mealType), mapOf("LUNCH" to mealType), emptyMap(), IngredientAliasResolver(emptyMap()))
        val dishGen = DishAutoGenerator(DishRepository(db), IngredientAutoGenerator(IngredientRepository(db), NutritionRepository(db)))
        val generator = DayAutoGenerator(dishGen, MealRecordUseCase(MealRecordRepository(db)))
        val date = LocalDate(2026, 8, 25)
        val preview = generator.preview(
            listOf(SemanticDay(date.toString(), 0, listOf(SemanticMeal("LUNCH", "12:00", dishes = listOf(SemanticDish("测试菜")))))) ,
            date,
            context,
        )
        assertEquals(0, MealRecordRepository(db).loadDayMealsForEdit(date).size)
        generator.commit(preview)
        assertEquals(listOf(dishId), MealRecordRepository(db).loadDayMealsForEdit(date).single().dishes.map { it.id })
    }

    @Test
    fun `commit merges same meal and persists semantic eaten ratio`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        q.insertDish("已有菜", null, "", "", "", "", "preset", 0, 0, "")
        val existingDish = q.lastInsertId().executeAsOne()
        q.insertDish("新菜", null, "", "", "", "", "preset", 0, 0, "")
        val newDish = q.lastInsertId().executeAsOne()
        val mealType = MealType(mealTypeId, "LUNCH", "午餐", LocalTime(12, 0), true)
        val context = AutoGenContext(emptyMap(), emptyList(), 1, emptySet(), listOf(mealType), mapOf("LUNCH" to mealType), emptyMap(), IngredientAliasResolver(emptyMap()))
        val useCase = MealRecordUseCase(MealRecordRepository(db))
        val date = LocalDate(2026, 8, 26)
        useCase.saveDay(date, listOf(com.sxdbsm.cookbook.usecase.mealrecording.MealRecordDraft(mealTypeId, date, LocalTime(12, 0), dishIds = listOf(existingDish))))
        val generator = DayAutoGenerator(
            DishAutoGenerator(DishRepository(db), IngredientAutoGenerator(IngredientRepository(db), NutritionRepository(db))),
            useCase,
        )

        val preview = generator.preview(
            listOf(SemanticDay(date.toString(), 0, listOf(SemanticMeal("LUNCH", "12:00", dishes = listOf(SemanticDish("新菜", eatenRatio = 0.5)))))) ,
            date,
            context,
        )
        generator.commit(preview, MergeMode.MERGE)

        val restoredMeal = useCase.queryDayForEdit(date).single()
        assertEquals(listOf(existingDish, newDish), restoredMeal.dishes.map { it.id })
        assertEquals(0.5, restoredMeal.dishes.single { it.id == newDish }.eatenRatio, 1e-9)
    }
}
