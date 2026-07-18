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

    @Test
    fun addServingsVisiblyIncreasesRemainingEvenWhenTodayMealConsumesIt() = runBlocking {
        // [AI生成] 修复"当天有餐时加1份没反应"：剩0共1 + 今天一餐用到 → 加1份后剩余应=1、共2。
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val pantry = PantryRepository(db)

        q.insertMealType("LUNCH", "午餐", "12:00", 2, "preset")
        val lunchId = q.lastInsertId().executeAsOne()
        val porkId = ingredientRepo.createUserIngredient(name = "五花肉")
        val dishId = dishRepo.saveDish(
            id = 0, name = "红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                com.sxdbsm.cookbook.domain.model.DishIngredient(
                    ingredient = com.sxdbsm.cookbook.domain.model.Ingredient(id = porkId, name = "五花肉"),
                    isMain = true,
                ),
            ),
        )
        val today = com.sxdbsm.cookbook.util.DateTime.today()
        mealRepo.saveDayMeals(
            today,
            listOf(DayMealDraft(lunchId, kotlinx.datetime.LocalTime(12, 0), "", listOf(dishId))),
        )

        // 构造"剩0 共1"：入库并置 1 份，今天这一餐把它占光。
        pantry.addToPantry(porkId)
        pantry.setServings(porkId, 1)
        assertEquals(0, pantry.remaining()[porkId], "前置：今天餐占光后剩余应为 0")
        assertEquals(1, pantry.servingCounts()[porkId], "前置：共 1 份")

        // 加 1 份：剩余应可见 +1，而非旧 bug 的仍 0。
        pantry.addServings(porkId, 1)
        assertEquals(1, pantry.remaining()[porkId], "加1份后剩余应=1(修复前会仍为0)")
        assertEquals(2, pantry.servingCounts()[porkId], "加1份后共份数应=2")
    }

    @Test
    fun `出库撤销_还原份数与在库态`() = runBlocking {
        // [AI生成] UX深挖#2：快照(在库3份)→出库(清零失效)→restore→还原为在库3份。
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val pantry = PantryRepository(db)

        val id = ingredientRepo.createUserIngredient(name = "土豆")
        pantry.addToPantry(id)
        pantry.setServings(id, 3)

        val snap = pantry.snapshotItem(id)
        assertTrue(snap.wasActive && snap.serving == 3, "快照应记录在库3份: $snap")

        pantry.removeFromPantry(id)
        assertEquals(0L, pantry.count(), "出库后不在在手列表")

        pantry.restoreItem(snap)
        assertEquals(1L, pantry.count(), "撤销后回到在手")
        assertTrue(pantry.pantryIngredientIds().contains(id))
        assertEquals(3, pantry.servingCounts()[id], "撤销后份数还原为3")
    }

    @Test
    fun `入库撤销_还原为未入库`() = runBlocking {
        // [AI生成] UX深挖#13：从未入库→快照(wasActive=false)→入库1份→restore→回到未入库。
        val db = RepositoryTestDatabase.create()
        val ingredientRepo = IngredientRepository(db)
        val pantry = PantryRepository(db)

        val id = ingredientRepo.createUserIngredient(name = "洋葱")
        val snap = pantry.snapshotItem(id)
        assertFalse(snap.wasActive, "入库前快照应为未入库")

        pantry.addServings(id, 1)
        assertEquals(1L, pantry.count(), "入库后在在手列表")

        pantry.restoreItem(snap)
        assertEquals(0L, pantry.count(), "撤销后回到未入库")
        assertFalse(pantry.pantryIngredientIds().contains(id))
    }
}
