package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : DishRepositoryDeepTest
 * @Time : 2026/07/13
 * @Author : SXD-AI
 * @Desc : 菜品模块深入测试（补齐食材往返/编辑替换/搜索多路/按食材匹配/菜系/多烹饪方式/删除引用等缺口）
 * <p>
 * [AI生成] 菜品模块深挖：把之前零覆盖的关键路径固化为回归，并守护本轮 N+1/字段一致性重构。
 **/
class DishRepositoryDeepTest {

    private fun mealType(db: com.sxdbsm.cookbook.db.CookbookDatabase, code: String, name: String, time: String): Long {
        db.cookbookQueries.insertMealType(code, name, time, 1, "preset")
        return db.cookbookQueries.lastInsertId().executeAsOne()
    }

    @Test
    fun `食材往返_含主辅料与克数`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val pork = ingRepo.createUserIngredient("五花肉")
        val ginger = ingRepo.createUserIngredient("姜")
        val dishId = dishRepo.saveDish(
            id = 0, name = "红烧肉", cookingMethodId = null, cookingMethodNames = listOf("焖"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = pork, name = "五花肉"), isMain = true, quantity = 300.0),
                DishIngredient(ingredient = Ingredient(id = ginger, name = "姜"), isMain = false, quantity = 10.0),
            ),
        )
        val dish = dishRepo.getDishById(dishId)!!
        assertEquals(2, dish.ingredients.size)
        val main = dish.ingredients.first { it.isMain }
        assertEquals("五花肉", main.ingredient.name)
        assertEquals(300.0, main.quantity)
        assertTrue(dish.ingredients.any { !it.isMain && it.ingredient.name == "姜" })
        // is_main DESC 排序：主料在前
        assertTrue(dish.ingredients.first().isMain)
    }

    @Test
    fun `编辑菜品_替换食材标签烹饪方式全生效`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val a = ingRepo.createUserIngredient("青椒")
        val b = ingRepo.createUserIngredient("土豆")
        val id = dishRepo.saveDish(
            id = 0, name = "菜", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = listOf("旧标签"),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = a, name = "青椒"), isMain = true)),
        )
        dishRepo.saveDish(
            id = id, name = "菜", cookingMethodId = null, cookingMethodNames = listOf("焖", "炖"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = listOf("新标签"),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = b, name = "土豆"), isMain = true)),
        )
        val dish = dishRepo.getDishById(id)!!
        assertEquals(listOf("土豆"), dish.ingredients.map { it.ingredient.name }, "旧食材被替换")
        assertEquals(listOf("新标签"), dish.tags, "旧标签被替换")
        assertEquals(setOf("焖", "炖"), dish.cookingMethods.map { it.name }.toSet(), "烹饪方式被替换为多方式")
    }

    @Test
    fun `多烹饪方式_有序去重往返`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val id = dishRepo.saveDish(
            id = 0, name = "多法菜", cookingMethodId = null, cookingMethodNames = listOf("炒", "炸", "炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
        )
        val dish = dishRepo.getDishById(id)!!
        assertEquals(setOf("炒", "炸"), dish.cookingMethods.map { it.name }.toSet(), "重复方式去重")
    }

    @Test
    fun `菜系往返_getDishById与getDishMiniById都读回`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val id = dishRepo.saveDish(
            id = 0, name = "宫保鸡丁", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
            cuisine = "川菜",
        )
        assertEquals("川菜", dishRepo.getDishById(id)!!.cuisine)
        assertEquals("川菜", dishRepo.getDishMiniById(id)!!.cuisine, "getDishMiniById 应补齐 cuisine")
    }

    @Test
    fun `搜索_命中菜名标签食材_空关键词返回全部`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val peanut = ingRepo.createUserIngredient("花生")
        dishRepo.saveDish(
            id = 0, name = "宫保鸡丁", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = listOf("川菜"),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = peanut, name = "花生"), isMain = false)),
        )
        assertTrue(dishRepo.searchDishes("宫保").any { it.name == "宫保鸡丁" }, "按菜名命中")
        assertTrue(dishRepo.searchDishes("川菜").any { it.name == "宫保鸡丁" }, "按标签命中")
        assertTrue(dishRepo.searchDishes("花生").any { it.name == "宫保鸡丁" }, "按食材命中")
        assertTrue(dishRepo.searchDishes("").any { it.name == "宫保鸡丁" }, "空关键词返回全部")
    }

    @Test
    fun `按食材匹配_matchCount与HAVING_空输入返回空`() = runBlocking {
        // 守护 findDishesByIngredients 的 N+1 批量重构：计数正确、字段完整、顺序对应。
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val potato = ingRepo.createUserIngredient("土豆")
        val pepper = ingRepo.createUserIngredient("青椒")
        val onion = ingRepo.createUserIngredient("洋葱")
        val dishId = dishRepo.saveDish(
            id = 0, name = "青椒土豆丝", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = potato, name = "土豆"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = pepper, name = "青椒"), isMain = true),
            ),
            cuisine = "家常菜",
        )
        // 只有土豆在手 → 命中1，总食材2
        val m = dishRepo.findDishesByIngredients(listOf(potato))
        val match = m.first { it.dish.id == dishId }
        assertEquals(1, match.matchCount)
        assertEquals(2, match.totalIngredientCount)
        assertEquals("家常菜", match.dish.cuisine, "N+1 重构后 cuisine 应保留")
        assertEquals(listOf("土豆", "青椒"), match.dish.mainIngredientNames)
        // 只有不相关的洋葱 → 该菜不命中(HAVING match_count>0)
        assertTrue(dishRepo.findDishesByIngredients(listOf(onion)).none { it.dish.id == dishId })
        // 空输入 → 空
        assertTrue(dishRepo.findDishesByIngredients(emptyList()).isEmpty())
    }

    @Test
    fun `批量buildDishMinis_多菜标签主料不串`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val potato = ingRepo.createUserIngredient("土豆")
        val fish = ingRepo.createUserIngredient("鲈鱼")
        dishRepo.saveDish(id = 0, name = "土豆菜", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = listOf("素"), ingredients = listOf(DishIngredient(ingredient = Ingredient(id = potato, name = "土豆"), isMain = true)))
        dishRepo.saveDish(id = 0, name = "鲈鱼菜", cookingMethodId = null, cookingMethodNames = listOf("蒸"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = listOf("荤"), ingredients = listOf(DishIngredient(ingredient = Ingredient(id = fish, name = "鲈鱼"), isMain = true)))
        val all = dishRepo.searchDishes("")
        val potatoDish = all.first { it.name == "土豆菜" }
        val fishDish = all.first { it.name == "鲈鱼菜" }
        assertEquals(listOf("土豆"), potatoDish.mainIngredientNames)
        assertEquals(listOf("鲈鱼"), fishDish.mainIngredientNames)
        assertEquals(listOf("素"), potatoDish.tags)
        assertEquals(listOf("荤"), fishDish.tags)
    }

    @Test
    fun `删除菜品_软删后列表不见但餐食历史保留`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val id = dishRepo.saveDish(id = 0, name = "历史菜", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())
        val bf = mealType(db, "BREAKFAST", "早餐", "07:30")
        val date = LocalDate(2026, 7, 13)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(id))))
        dishRepo.deleteDish(id)
        assertTrue(dishRepo.searchDishes("历史菜").none { it.id == id }, "软删后菜品列表不再展示")
        assertTrue(mealRepo.loadDayMealsForEdit(date).first().dishes.any { it.id == id }, "餐食历史仍保留该菜(契约)")
    }
}
