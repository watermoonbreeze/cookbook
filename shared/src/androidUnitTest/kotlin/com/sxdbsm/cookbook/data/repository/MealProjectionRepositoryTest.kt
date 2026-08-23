package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * @File : MealProjectionRepositoryTest
 * @Author : Codex-AI
 * [AI生成] Phase 3：验证 Home/Timeline 读取门面只返回 Projection/Content，
 * 且读取结果与现有 MealRecordRepository 兼容读取保持一致。
 */
class MealProjectionRepositoryTest {
    @Test
    fun projectionReadSeamPreservesMealContentAndDates() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepository = DishRepository(db)
        val mealRepository = MealRecordRepository(db)
        val projectionRepository = MealProjectionRepository(mealRepository)
        val query = db.cookbookQueries
        query.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        val mealTypeId = query.lastInsertId().executeAsOne()
        val dishId = dishRepository.saveDish(
            id = 0,
            name = "Projection Test Dish",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )
        val date = LocalDate(2026, 8, 20)
        mealRepository.saveDayMeals(
            date,
            listOf(DayMealDraft(mealTypeId, LocalTime(12, 0), "", listOf(dishId))),
        )

        val contents = projectionRepository.loadMealDayContentsByDates(listOf(date))
        val dates = projectionRepository.observeTimelineDates().first()

        assertEquals(date, contents.single().date)
        assertEquals(1, contents.single().meals.size)
        assertEquals("Projection Test Dish", contents.single().meals.single().dishes.single().name)
        assertTrue(date in dates)
    }
}
