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
        // [AI修改] 高血脂维度加列后改命名参数(防位置错位)：新增 saturated_fat_g/cholesterol_mg。
        q.upsertIngredientNutrition(
            ingredient_id = riceId, energy_kcal = 346.0, protein_g = 7.4, fat_g = 0.8, carb_g = 77.9,
            fiber_g = 0.7, sodium_mg = 3.8, potassium_mg = 103.0, calcium_mg = 13.0, gi = 83.0, purine_mg = 18.0,
            saturated_fat_g = 0.2, cholesterol_mg = 0.0, piece_gram = null, ref = "ref", review = 1L, updated_at = now,
        )
        q.upsertIngredientNutrition(
            ingredient_id = eggId, energy_kcal = 144.0, protein_g = 13.3, fat_g = 8.8, carb_g = 2.8,
            fiber_g = 0.0, sodium_mg = 131.0, potassium_mg = 154.0, calcium_mg = 56.0, gi = 30.0, purine_mg = 3.0,
            saturated_fat_g = 3.1, cholesterol_mg = 585.0, piece_gram = 50.0, ref = "ref", review = 1L, updated_at = now,
        )

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
        // [AI生成] 高血脂维度折算直接断言(本次改动核心)：胆固醇=鸡蛋585×0.5=292.5，饱和脂肪=米0.2×1+蛋3.1×0.5=1.75。
        assertEquals(292.5, d.totals.cholesterolMg, 0.001)
        assertEquals(1.75, d.totals.saturatedFatG, 0.001)
        assertEquals(2, d.coveredCount)
        assertEquals(2, d.ingredientCount)
        assertTrue(d.complete, "两料都有数据且无兜底")

        // 单食材营养读取
        val eggN = repo.ingredientNutrition(eggId)
        assertNotNull(eggN)
        assertEquals(50.0, eggN.pieceGram)
        assertEquals(585.0, eggN.cholesterolMg) // [AI生成] 新列读取映射校验
        assertEquals(3.1, eggN.saturatedFatG)
        assertTrue(eggN.review)
        Unit
    }
}
