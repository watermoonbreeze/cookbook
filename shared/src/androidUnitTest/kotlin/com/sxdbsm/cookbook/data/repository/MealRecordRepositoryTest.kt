package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * @File : MealRecordRepositoryTest
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 餐食记录仓库单元测试
 * <p>
 * 覆盖整日替换、清空某天餐食、软删除后时间线不可见等核心行为。
 * <p>
 * [AI生成] 为添加/编辑餐食流程建立基础回归测试。
 **/
class MealRecordRepositoryTest {

    @Test
    fun saveDayMealsWithEmptyListClearsThatDay() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0,
            name = "早餐菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)

        mealRepo.saveDayMeals(
            date = date,
            meals = listOf(
                DayMealDraft(
                    mealTypeId = mealTypeId,
                    mealTime = LocalTime(7, 30),
                    note = "",
                    dishIds = listOf(dishId),
                ),
            ),
        )
        mealRepo.saveDayMeals(date = date, meals = emptyList())

        assertEquals(emptyList(), mealRepo.loadDayMealsForEdit(date))
        assertEquals(emptyList(), mealRepo.listDistinctDates(limit = 10, offset = 0))
    }

    @Test
    fun editingSameDayDoesNotIncrementExistingDishPreferenceAgain() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("DINNER", "晚餐", "18:30", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0,
            name = "晚餐菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        val draft = DayMealDraft(mealTypeId, LocalTime(18, 30), "", listOf(dishId))

        mealRepo.saveDayMeals(date, listOf(draft))
        mealRepo.saveDayMeals(date, listOf(draft.copy(note = "换个备注")))

        assertEquals(1, dishRepo.getDishById(dishId)?.preference)
    }

    // [AI生成] 食用比例(是否吃完)·数据保全(Google审🟡-1)：编辑当天餐食(整日删重插)不能静默重置用户调好的吃完度。
    @Test
    fun editingSameDayPreservesEatenRatio() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        val dishId = dishRepo.saveDish(
            id = 0, name = "午餐菜品", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val date = LocalDate(2026, 6, 5)
        val draft = DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))

        val recordIds = mealRepo.saveDayMeals(date, listOf(draft))
        mealRepo.setEatenRatio(recordIds.first(), dishId, 0.5) // 调"吃了一半"
        mealRepo.saveDayMeals(date, listOf(draft.copy(note = "换个备注"))) // 编辑当天→整日删重插

        // 之前调的 0.5 应被快照回填保全，不被重置回 1.0。
        val dishes = mealRepo.loadDayMealsForEdit(date).first().dishes
        assertEquals(0.5, dishes.first().eatenRatio, 1e-9)
    }

    // [AI生成] 食用比例：整餐一次设置(setEatenRatioForMeal)把该餐所有菜设同值，且 loadDayMealsForEdit 能读回。
    @Test
    fun setEatenRatioForMealSetsAllDishesAndRoundTrips() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val q = db.cookbookQueries
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = q.lastInsertId().executeAsOne()
        suspend fun mkDish(name: String) = dishRepo.saveDish(
            id = 0, name = name, cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val d1 = mkDish("菜一")
        val d2 = mkDish("菜二")
        val date = LocalDate(2026, 6, 5)
        val recordIds = mealRepo.saveDayMeals(date, listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(d1, d2))))

        mealRepo.setEatenRatioForMeal(recordIds.first(), 0.25)
        val allQuarter = mealRepo.loadDayMealsForEdit(date).first().dishes
        assertEquals(2, allQuarter.size)
        assertTrue(allQuarter.all { it.eatenRatio == 0.25 }, "整餐设置后两道菜都应=0.25")

        // 分菜再调 + coerce 越界防护。
        mealRepo.setEatenRatio(recordIds.first(), d1, 2.0) // 越界→夹到 1.0
        val mixed = mealRepo.loadDayMealsForEdit(date).first().dishes.associate { it.id to it.eatenRatio }
        assertEquals(1.0, mixed[d1], "越界 2.0 应 coerce 到 1.0")
        assertEquals(0.25, mixed[d2], "另一道菜不受影响仍 0.25")
    }
}
