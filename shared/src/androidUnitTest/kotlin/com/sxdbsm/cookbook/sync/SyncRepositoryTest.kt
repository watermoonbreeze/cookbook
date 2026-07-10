package com.sxdbsm.cookbook.sync

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : SyncRepositoryTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 选择性同步 导出/合并导入 单测(P1 菜品+食材)
 * <p>
 * [AI生成] 覆盖：导出→导入到另一库(按名合并+ID重映射)、同名不重复、重复导入更新不新增。
 **/
class SyncRepositoryTest {

    private fun seedSource(db: com.sxdbsm.cookbook.db.CookbookDatabase) = runBlocking {
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val porkId = ingRepo.createUserIngredient(name = "猪肉", alias = "瘦肉")
        val potatoId = ingRepo.createUserIngredient(name = "土豆")
        dishRepo.saveDish(
            id = 0, name = "土豆炖肉", cookingMethodId = null, cookingMethodNames = listOf("炖"),
            specialNote = "少盐", description = "家常炖菜", imagePath = "d.jpg", thumbnailPath = "d_t.jpg",
            tagNames = listOf("家常"),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = porkId, name = "猪肉"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = potatoId, name = "土豆"), isMain = true),
            ),
            steps = emptyList(),
        )
    }

    @Test
    fun `导出后导入到另一库-按名合并且食材重映射`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        seedSource(src)
        val bundle = SyncRepository(src, DishRepository(src), IngredientRepository(src)).export(includeIngredients = true, includeDishes = true)
        // 导出内容
        assertTrue(bundle.dishes.any { it.name == "土豆炖肉" })
        assertTrue(bundle.ingredients.any { it.name == "猪肉" } && bundle.ingredients.any { it.name == "土豆" })

        // 导入到空库(目标 id 与源不同也应正确重映射)
        val dst = RepositoryTestDatabase.create()
        val dstDishRepo = DishRepository(dst)
        val result = SyncRepository(dst, dstDishRepo, IngredientRepository(dst)).import(bundle)
        assertEquals(1, result.dishesAdded)
        assertTrue(result.ingredientsAdded >= 2)

        // 目标库能查到菜, 且其食材(重映射后)名字正确
        val dishId = dst.cookbookQueries.selectUserDishIdByName("土豆炖肉").executeAsOneOrNull()
        assertNotNull(dishId)
        val dish = dstDishRepo.getDishById(dishId)
        assertNotNull(dish)
        val names = dish.ingredients.map { it.ingredient.name }.toSet()
        assertEquals(setOf("猪肉", "土豆"), names)
    }

    @Test
    fun `重复导入更新不新增`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        seedSource(src)
        val bundle = SyncRepository(src, DishRepository(src), IngredientRepository(src)).export(true, true)

        val dst = RepositoryTestDatabase.create()
        val sync = SyncRepository(dst, DishRepository(dst), IngredientRepository(dst))
        sync.import(bundle)
        val second = sync.import(bundle) // 再导一次
        assertEquals(0, second.dishesAdded) // 同名不新增
        assertEquals(1, second.dishesUpdated) // 而是更新
        assertEquals(0, second.ingredientsAdded) // 食材同名复用
    }
}
