package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * @File : RecommendationDataSourceTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 取数层（自由搭配分类）守护测试
 * <p>
 * 端到端跑真实 seed 数据：在手食材经 classifyPairRole 分类后应能产出搭配，
 * 防止 seed 食材-分类关联演进后角色判定静默退化为全 OTHER（搭配恒空）。
 * <p>
 * [AI生成] 自由搭配一期回归守护。
 **/
class RecommendationDataSourceTest {

    @Test
    fun `自建菜_主料预设食材在库_应被库存推荐_与source和步骤无关`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val pantry = PantryRepository(db)
        val ds = RecommendationDataSource(db, pantry, dishRepo, HealthProfileRepository(db), ingredientRepo)

        val porkId = ingredientRepo.search("五花肉").first { it.name == "五花肉" }.id
        // 用户自建"我的红烧肉"：主料=预设五花肉，无步骤、无图片、source=user
        val dishId = dishRepo.saveDish(
            id = 0, name = "我的红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                com.sxdbsm.cookbook.domain.model.DishIngredient(
                    ingredient = com.sxdbsm.cookbook.domain.model.Ingredient(id = porkId, name = "五花肉"),
                    isMain = true,
                ),
            ),
            steps = emptyList(),
        )
        pantry.addToPantry(porkId) // 五花肉入库

        val input = ds.gather(com.sxdbsm.cookbook.ai.model.RecommendMode.PANTRY)
        assertTrue(input.dishes.any { it.id == dishId }, "自建菜(source=user、无步骤)主料在库应进候选——与 source/步骤无关")
        val cands = HealthRuleEngine().evaluate(input.dishes, input.pantryIngredientIds, input.constraints)
        assertTrue(cands.any { it.id == dishId }, "应被库存推荐(物尽其用: 用到在手非调料五花肉)")
        Unit
    }

    @Test
    fun freePairingWorksOnRealSeedData() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded() // 灌入食材 + general 大类关联
        val pantry = PantryRepository(db)
        val ingredientRepo = IngredientRepository(db)
        val dataSource = RecommendationDataSource(
            db, pantry, DishRepository(db), HealthProfileRepository(db), ingredientRepo,
        )

        // 在手：一荤(猪瘦肉)一素(青椒) → 应能搭出荤×素
        val pork = ingredientRepo.search("猪瘦肉").first { it.name == "猪瘦肉" }.id
        val pepper = ingredientRepo.search("青椒").first { it.name == "青椒" }.id
        pantry.addToPantry(pork)
        pantry.addToPantry(pepper)

        val out = dataSource.freePairing()
        assertTrue(out.isNotEmpty(), "有荤有素应产出搭配——守护 classifyPairRole 不因 seed 演进而全判 OTHER")
        assertTrue(
            out.any { it.items.contains("猪瘦肉") || it.items.contains("青椒") },
            "搭配项应包含在手食材",
        )
        Unit
    }
}
