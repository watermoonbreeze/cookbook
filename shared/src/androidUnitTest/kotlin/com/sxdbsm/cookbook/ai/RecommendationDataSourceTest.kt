package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.data.seed.PresetDataSeeder
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.ai.model.RecommendMode
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

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
    fun `B2_日期窗口内吃过标记recent并带天数_窗口外不标`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val pantry = PantryRepository(db)
        val mealRepo = MealRecordRepository(db)
        val ds = RecommendationDataSource(db, pantry, dishRepo, HealthProfileRepository(db), ingredientRepo)

        val pork = ingredientRepo.search("五花肉").first { it.name == "五花肉" }.id
        val dishId = dishRepo.saveDish(
            id = 0, name = "窗口测试菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = pork, name = "五花肉"), isMain = true)),
            steps = emptyList(),
        )
        pantry.addToPantry(pork)
        val bf = db.cookbookQueries.selectMealTypeIdByCode("BREAKFAST").executeAsOne()
        // 2026-07-01 吃过这道菜
        mealRepo.saveDayMeals(LocalDate(2026, 7, 1), listOf(DayMealDraft(bf, LocalTime(8, 0), "", listOf(dishId))))

        // today=7-04(3天前)、窗口一周 → 命中 recent，daysAgo=3
        val within = ds.gather(RecommendMode.PANTRY, today = LocalDate(2026, 7, 4), recentWindowDays = 7)
        assertEquals(3, within.recentDishDaysAgo[dishId], "3天前吃过应标 daysAgo=3")
        assertTrue(dishId in within.recentDishIds)
        // today=7-15(14天前)、窗口一周 → 不在窗口，不标
        val outside = ds.gather(RecommendMode.PANTRY, today = LocalDate(2026, 7, 15), recentWindowDays = 7)
        assertFalse(outside.recentDishDaysAgo.containsKey(dishId), "超出一周窗口不再标记")
        // 窗口放大到四周 → 又命中
        val wide = ds.gather(RecommendMode.PANTRY, today = LocalDate(2026, 7, 15), recentWindowDays = 28)
        assertEquals(14, wide.recentDishDaysAgo[dishId], "四周窗口内 14 天前应标 daysAgo=14")
    }

    @Test
    fun `两份红烧肉_预设与自建共用同一个五花肉_库存有五花肉_两份都应推荐`() = runBlocking {
        // ingredient.name 有 UNIQUE 约束(全新库)：五花肉只有一个 id，两份红烧肉必然共用它。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val q = db.cookbookQueries
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val pantry = PantryRepository(db)
        val ds = RecommendationDataSource(db, pantry, dishRepo, HealthProfileRepository(db), ingredientRepo)

        val pork = ingredientRepo.search("五花肉").first { it.name == "五花肉" }.id
        val presetHongshao = q.selectAllDishes().executeAsList().first { it.name == "红烧肉" }.id

        // 用户自建另一份「红烧肉」也用同一个五花肉(createUserIngredient 去重后本就复用同 id)。
        val userHongshao = dishRepo.saveDish(
            id = 0, name = "红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                com.sxdbsm.cookbook.domain.model.DishIngredient(
                    ingredient = com.sxdbsm.cookbook.domain.model.Ingredient(id = pork, name = "五花肉"),
                    isMain = true,
                ),
            ),
            steps = emptyList(),
        )
        assertTrue(userHongshao != presetHongshao, "两份不同的红烧肉")

        pantry.addToPantry(pork) // 库存加入五花肉

        val input = ds.gather(com.sxdbsm.cookbook.ai.model.RecommendMode.PANTRY)
        assertTrue(input.dishes.any { it.id == presetHongshao }, "预设红烧肉应进候选")
        assertTrue(input.dishes.any { it.id == userHongshao }, "自建红烧肉应进候选")
        val cands = HealthRuleEngine().evaluate(input.dishes, input.pantryIngredientIds, input.constraints)
        assertTrue(cands.any { it.id == presetHongshao }, "预设红烧肉应被推荐")
        assertTrue(cands.any { it.id == userHongshao }, "自建红烧肉应被推荐")
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
