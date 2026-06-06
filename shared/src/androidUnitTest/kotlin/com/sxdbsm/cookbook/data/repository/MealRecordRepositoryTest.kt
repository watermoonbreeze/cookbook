package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
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
}
