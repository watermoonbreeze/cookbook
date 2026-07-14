package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : IngredientPantryBugTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 食材/库存深挖修复回归（H1 名称归一防重复 / H2 幽灵库存 / H3 加份数原子）
 * <p>
 * [AI生成] 守护本轮三个真 bug 修复。
 **/
class IngredientPantryBugTest {

    // ===== H1：新建食材按归一名复用，不同书写(空格/大小写)不产生重复 =====
    @Test
    fun `H1_同名不同空格大小写复用同一id`() = runBlocking {
        val repo = IngredientRepository(RepositoryTestDatabase.create())
        val base = repo.createUserIngredient("五花肉")
        assertEquals(base, repo.createUserIngredient("五花肉"), "完全同名复用")
        assertEquals(base, repo.createUserIngredient("五 花 肉"), "内部空格归一后复用")
        assertEquals(base, repo.createUserIngredient("  五花肉 "), "首尾空格复用")
        assertEquals(base, repo.createUserIngredient("五　花　肉"), "全角空格归一后复用")

        val egg = repo.createUserIngredient("Egg")
        assertEquals(egg, repo.createUserIngredient("egg"), "大小写归一后复用")
        assertEquals(egg, repo.createUserIngredient("EGG"), "全大写复用")
        // 不同食材仍各自独立
        assertFalse(base == egg)
    }

    // ===== H2：软删食材后其库存不再被当"家里有" =====
    @Test
    fun `H2_软删食材后库存不残留幽灵在手`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val pantryRepo = PantryRepository(db)
        val id = ingRepo.createUserIngredient("临时菜")
        pantryRepo.addServings(id, 3)
        assertTrue(id in pantryRepo.pantryIngredientIds(), "入库后在手")
        assertEquals(3, pantryRepo.servingCounts()[id])

        ingRepo.deleteUserIngredient(id)
        assertFalse(id in pantryRepo.pantryIngredientIds(), "软删食材后不应仍算在手")
        assertFalse(pantryRepo.servingCounts().containsKey(id), "份数映射也不应含失效食材")
        assertFalse(pantryRepo.remaining().containsKey(id), "剩余份数不应含失效食材")
    }

    // ===== H3：加份数读写在同一事务，顺序加份数正确累加 =====
    @Test
    fun `H3_多次加份数正确累加`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val ingRepo = IngredientRepository(db)
        val pantryRepo = PantryRepository(db)
        val id = ingRepo.createUserIngredient("土豆")
        pantryRepo.addServings(id, 1)
        pantryRepo.addServings(id, 1)
        pantryRepo.addServings(id, 3)
        assertEquals(5, pantryRepo.servingCounts()[id], "1+1+3 应累加为 5")
        assertEquals(5, pantryRepo.remaining()[id], "无餐食占用时剩余=份数")
    }
}
