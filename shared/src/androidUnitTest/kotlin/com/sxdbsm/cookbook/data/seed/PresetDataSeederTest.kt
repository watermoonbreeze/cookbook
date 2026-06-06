package com.sxdbsm.cookbook.data.seed

import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * @File : PresetDataSeederTest
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 预置基础数据单元测试
 * <p>
 * 验证基础食材和多维分类 seed 后能被 Repository 正常读取。
 * <p>
 * [AI生成] 为基础数据扩充建立回归测试，防止后续 seed 调整导致食材库为空或分类缺失。
 **/
class PresetDataSeederTest {

    @Test
    fun seedCreatesFoundationIngredientsAndCategories() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val ingredientRepo = IngredientRepository(db)
        val categoryRepo = FoodCategoryRepository(db)

        val allIngredients = ingredientRepo.search("")
        val topCategories = categoryRepo.listTopLevel()
        val nutritionCategory = topCategories.first { it.name == "健康饮食" }
        val nutritionChildren = categoryRepo.listChildren(nutritionCategory.id)

        assertTrue(allIngredients.size >= 80, "基础食材数量应至少达到第一阶段 80 个")
        assertTrue(allIngredients.any { it.name == "燕麦" }, "应包含慢病饮食常用食材：燕麦")
        assertTrue(allIngredients.first { it.name == "燕麦" }.emoji.isNotBlank(), "JSON 预设食材应写入默认 emoji")
        assertTrue(allIngredients.any { it.name == "低脂牛奶" }, "应包含高血压/DASH 常用低脂奶")
        assertTrue(topCategories.any { it.name == "水果" }, "日常分类应包含水果")
        assertTrue(nutritionChildren.any { it.name == "低 GI" }, "营养维度应包含低 GI")
        assertTrue(nutritionChildren.any { it.name == "高嘌呤" }, "营养维度应包含高嘌呤")
    }

    @Test
    fun seedCreatesCrowdIngredientRules() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val crowd = db.cookbookQueries.selectAllCrowdTypes().executeAsList().first { it.name == "高血压" }
        val ingredients = IngredientRepository(db).listByCrowd(crowd.id)

        assertTrue(
            ingredients.any { it.name == "盐" && it.adviceLevel == AdviceLevel.AVOID },
            "高血压人群应能看到盐的避免建议",
        )
        assertTrue(
            ingredients.any { it.name == "低脂牛奶" && it.adviceLevel == AdviceLevel.RECOMMEND },
            "高血压人群应能看到低脂牛奶的推荐建议",
        )
    }

    @Test
    fun seedJsonRequiresEmojiForEveryIngredient() {
        val db = RepositoryTestDatabase.create()
        val jsonIngredients = PresetDataSeeder(db).loadIngredientsForTest()

        assertTrue(jsonIngredients.size >= 80, "JSON 基础食材数量应至少达到第一阶段 80 个")
        assertTrue(jsonIngredients.all { it.code.isNotBlank() }, "每个 JSON 食材必须配置稳定 code")
        assertTrue(jsonIngredients.all { it.name.isNotBlank() }, "每个 JSON 食材必须配置名称")
        assertTrue(jsonIngredients.all { it.emoji.isNotBlank() }, "每个 JSON 食材必须配置默认 emoji")
    }

    @Test
    fun seedIsIdempotentAndCanCompleteExistingIngredients() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val seeder = PresetDataSeeder(db)
        seeder.seedIfNeeded()
        val q = db.cookbookQueries
        val firstIngredientCount = q.selectAllIngredients().executeAsList().size
        val firstCategoryCount = q.countFoodCategories().executeAsOne()

        seeder.seedIfNeeded()

        val allIngredients = IngredientRepository(db).search("")
        val oat = allIngredients.first { it.name == "燕麦" }
        val nutrition = FoodCategoryRepository(db).listTopLevel().first { it.name == "健康饮食" }
        val lowGi = FoodCategoryRepository(db).listChildren(nutrition.id).first { it.name == "低 GI" }
        val lowGiIngredients = IngredientRepository(db).listByCategory(lowGi.id)

        assertEquals(firstIngredientCount, allIngredients.size, "重复 seed 不应重复插入食材")
        assertEquals(firstCategoryCount, q.countFoodCategories().executeAsOne(), "重复 seed 不应重复插入分类")
        assertTrue(oat.emoji == "🌾", "已有食材也应补齐 JSON 中维护的 emoji")
        assertTrue(lowGiIngredients.any { it.name == "燕麦" }, "已有食材也应补齐 JSON 中维护的分类关系")
    }
}
