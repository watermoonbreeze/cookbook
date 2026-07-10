package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.ShoppingReason
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * @File : ShoppingListRepositoryTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 采购清单聚合单元测试
 * <p>
 * 验证：只聚合今天及未来的餐食；同一食材跨多餐去重并累计涉及餐数；未入库主料标为采购。
 * <p>
 * [AI生成] 待办"采购清单聚合"回归测试。
 **/
class ShoppingListRepositoryTest {

    @Test
    fun aggregatesFuturePurchaseIngredientsDedupedByName() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val shopping = ShoppingListRepository(db)

        // 餐次字典
        q.insertMealType("LUNCH", "午餐", "12:00", 2, "preset")
        val lunchId = q.lastInsertId().executeAsOne()

        // 三个食材，均不入库 → 作为主料时都需采购
        val porkId = ingredientRepo.createUserIngredient(name = "猪肉")
        val potatoId = ingredientRepo.createUserIngredient(name = "土豆")
        val eggId = ingredientRepo.createUserIngredient(name = "鸡蛋")

        // 菜A: 主料 猪肉+土豆；菜B: 主料 鸡蛋+土豆(土豆两菜共用)
        val dishA = dishRepo.saveDish(
            id = 0, name = "红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = porkId, name = "猪肉"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = potatoId, name = "土豆"), isMain = true),
            ),
        )
        val dishB = dishRepo.saveDish(
            id = 0, name = "土豆鸡蛋", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = eggId, name = "鸡蛋"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = potatoId, name = "土豆"), isMain = true),
            ),
        )

        val today = LocalDate(2026, 6, 5)
        // 未来两餐 + 一顿过去的(不应计入)
        mealRepo.saveDayMeals(LocalDate(2026, 6, 6), listOf(DayMealDraft(lunchId, LocalTime(12, 0), "", listOf(dishA))))
        mealRepo.saveDayMeals(LocalDate(2026, 6, 7), listOf(DayMealDraft(lunchId, LocalTime(12, 0), "", listOf(dishB))))
        mealRepo.saveDayMeals(LocalDate(2026, 6, 1), listOf(DayMealDraft(lunchId, LocalTime(12, 0), "", listOf(dishA))))

        val list = shopping.aggregate(today)

        // 未来涉及主料：猪肉(1) 土豆(2) 鸡蛋(1)，共 3 项去重
        assertEquals(3, list.size, "应聚合出 3 个去重食材")
        assertTrue(list.all { it.reason == ShoppingReason.PURCHASE }, "均未入库应标为采购")

        val potato = list.first { it.ingredientName == "土豆" }
        assertEquals(2, potato.mealCount, "土豆跨两餐应累计 mealCount=2")
        assertEquals(2, potato.dates.size, "土豆涉及两个日期")
        assertEquals(potatoId, potato.ingredientId, "应解析出食材 id")

        // 过去(6-1)那顿不计入：猪肉只在 6-6 出现 1 次
        assertEquals(1, list.first { it.ingredientName == "猪肉" }.mealCount, "过去餐食不计入")
    }

    @Test
    fun emptyWhenNoFutureMeals() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val shopping = ShoppingListRepository(db)
        assertTrue(shopping.aggregate(LocalDate(2026, 6, 5)).isEmpty(), "无未来餐食应返回空清单")
    }
}
