package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * @File : PantryRepositoryTest
 * @Time : 2026/07/06
 * @Author : SXD-AI
 * @Desc : 库存（我家食材）仓库单元测试
 * <p>
 * 覆盖加入/移出库存、在手列表与 id 集合、重复加入刷新、移出后可再次加入等核心行为。
 * <p>
 * [AI生成] 食材层阶段1：pantry 库存数据层回归测试。
 **/
class PantryRepositoryTest {

    @Test
    fun addAndListPantryIngredients() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val pantry = PantryRepository(db)

        val eggId = ingredientRepo.createUserIngredient(name = "鸡蛋")
        val riceId = ingredientRepo.createUserIngredient(name = "大米")
        pantry.addToPantry(eggId, quantity = 6.0)
        pantry.addToPantry(riceId)

        val inPantry = pantry.listPantryIngredients()
        assertEquals(2, inPantry.size, "库存应有 2 个在手食材")
        assertTrue(inPantry.any { it.name == "鸡蛋" } && inPantry.any { it.name == "大米" })
        assertEquals(setOf(eggId, riceId), pantry.pantryIngredientIds())
        assertEquals(2L, pantry.count())
    }

    @Test
    fun reAddSameIngredientDoesNotDuplicate() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val pantry = PantryRepository(db)

        val id = ingredientRepo.createUserIngredient(name = "牛奶")
        pantry.addToPantry(id, quantity = 1.0)
        pantry.addToPantry(id, quantity = 2.0) // 重复加入=刷新，不新增行

        assertEquals(1L, pantry.count(), "重复加入同一食材不应产生两条库存")
    }

    @Test
    fun removedItemCanBeReAdded() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val pantry = PantryRepository(db)

        val id = ingredientRepo.createUserIngredient(name = "西红柿")
        pantry.addToPantry(id)
        pantry.removeFromPantry(id)
        assertEquals(0L, pantry.count(), "移出后不在在手列表")
        assertFalse(pantry.pantryIngredientIds().contains(id))

        pantry.addToPantry(id)
        assertEquals(1L, pantry.count(), "移出后应能再次加入库存")
        assertTrue(pantry.pantryIngredientIds().contains(id))
    }
}
