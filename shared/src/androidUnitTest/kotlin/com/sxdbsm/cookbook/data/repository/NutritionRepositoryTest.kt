package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : NutritionRepositoryTest
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 营养仓库集成测试（DB 查询→折算→菜品营养 端到端通路）
 * <p>
 * 手插最小数据(单位/食材/营养/菜品/配料)，验证 SQL selectNutritionInputsByDishIds 与映射正确。
 * 测试库走 Schema.create、无 seed。
 * <p>
 * [AI生成] P1 营养框架通路校验。
 **/
class NutritionRepositoryTest {

    @Test
    fun `菜品营养_克与计件混合折算`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val now = 0L

        // 单位：克(克当量1)、个(计件，克当量null)
        q.insertMeasurementUnit("克", "preset", 1.0)
        val gramUnit = q.lastInsertId().executeAsOne()
        q.insertMeasurementUnit("个", "preset", null)
        val pieceUnit = q.lastInsertId().executeAsOne()

        // 食材：大米(克)、鸡蛋(个，piece_gram 50)
        q.insertIngredient("大米", "", "dami", "", "", "🍚", gramUnit, "preset", now)
        val riceId = q.lastInsertId().executeAsOne()
        q.insertIngredient("鸡蛋", "", "jidan", "", "", "🥚", pieceUnit, "preset", now)
        val eggId = q.lastInsertId().executeAsOne()

        // 营养(每100g)
        q.upsertIngredientNutrition(riceId, 346.0, 7.4, 0.8, 77.9, 0.7, 3.8, 103.0, 13.0, 83.0, 18.0, null, "ref", 1L, now)
        q.upsertIngredientNutrition(eggId, 144.0, 13.3, 8.8, 2.8, 0.0, 131.0, 154.0, 56.0, 30.0, 3.0, 50.0, "ref", 1L, now)

        // 菜品：蛋炒饭 = 大米100克 + 鸡蛋1个
        q.insertDish("蛋炒饭", null, "", "", "", "", "user", now, now, "")
        val dishId = q.lastInsertId().executeAsOne()
        q.insertDishIngredient(dishId, riceId, 100.0, gramUnit, 1L)
        q.insertDishIngredient(dishId, eggId, 1.0, pieceUnit, 1L)

        val repo = NutritionRepository(db)
        val result = repo.dishNutrition(listOf(dishId))
        val d = result[dishId]
        assertNotNull(d)
        // 大米100g=346 + 鸡蛋50g=72 → 418 kcal
        assertEquals(418.0, d.totals.energyKcal, 0.001)
        assertEquals(2, d.coveredCount)
        assertEquals(2, d.ingredientCount)
        assertTrue(d.complete, "两料都有数据且无兜底")

        // 单食材营养读取
        val eggN = repo.ingredientNutrition(eggId)
        assertNotNull(eggN)
        assertEquals(50.0, eggN.pieceGram)
        assertTrue(eggN.review)
        Unit
    }
}
