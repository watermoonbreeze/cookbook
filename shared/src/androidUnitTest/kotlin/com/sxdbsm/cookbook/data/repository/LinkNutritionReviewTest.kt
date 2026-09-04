package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : LinkNutritionReviewTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : link 食材营养复核语义单测（L4 分享链接·T-L4-18·真库）
 * <p>
 * isLinkNutritionPending 的 shared 查询级语义 = `nutritionRepo.ingredientNutrition(id)?.review`
 * （ARCH-01 配套）：review=0（待复核）→ false；review=1（已复核）→ true。
 * 手工造 link 来源食材 + 各一份 review=0 / review=1 营养行验证。
 * <p>
 * [AI生成] 实施蓝图 `feature/分享链接解析_实施蓝图.md` §8 测试矩阵 shared 部分。
 **/
class LinkNutritionReviewTest {

    @Test
    fun `T-L4-18 ingredientNutrition_review语义_review0为false_review1为true`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val nutritionRepo = NutritionRepository(db)

        // 手工造 link 来源食材 + review=0 营养行
        q.insertIngredient("冬瓜", "", "donggua", "", "", "🥗", null, "link", 0)
        val review0Id = q.lastInsertId().executeAsOne()
        q.upsertIngredientNutrition(
            ingredient_id = review0Id,
            energy_kcal = 12.0, protein_g = 0.6, fat_g = 0.2, carb_g = 2.6,
            fiber_g = 0.7, sodium_mg = 30.0, potassium_mg = 120.0, calcium_mg = 30.0, gi = null,
            purine_mg = 5.0, saturated_fat_g = null, cholesterol_mg = null, piece_gram = null,
            ref = "链接导入·待复核", review = 0L, updated_at = 0L,
        )

        // 手工造 link 来源食材 + review=1 营养行
        q.insertIngredient("猪肉馅", "", "zhurouxian", "", "", "🥩", null, "link", 0)
        val review1Id = q.lastInsertId().executeAsOne()
        q.upsertIngredientNutrition(
            ingredient_id = review1Id,
            energy_kcal = 300.0, protein_g = 18.0, fat_g = 25.0, carb_g = 0.0,
            fiber_g = null, sodium_mg = 60.0, potassium_mg = 200.0, calcium_mg = 5.0, gi = null,
            purine_mg = 120.0, saturated_fat_g = null, cholesterol_mg = null, piece_gram = null,
            ref = "链接导入·已复核", review = 1L, updated_at = 0L,
        )

        val n0 = nutritionRepo.ingredientNutrition(review0Id)
        assertNotNull(n0, "review=0 的营养行应可读")
        assertFalse(n0.review, "review=0 → ingredientNutrition().review==false（链接导入待复核语义）")

        val n1 = nutritionRepo.ingredientNutrition(review1Id)
        assertNotNull(n1, "review=1 的营养行应可读")
        assertTrue(n1.review, "review=1 → ingredientNutrition().review==true")
        Unit
    }
}
