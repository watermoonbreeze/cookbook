package com.sxdbsm.cookbook.data.seed

import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
        assertTrue(topCategories.any { it.name == "水果类" }, "日常分类应包含水果类")
        assertTrue(nutritionChildren.any { it.name == "低 GI" }, "营养维度应包含低 GI")
        assertTrue(nutritionChildren.any { it.name == "高嘌呤" }, "营养维度应包含高嘌呤")
    }

    @Test
    fun `营养seed引用完整_且能被读取和折算`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val seeder = PresetDataSeeder(db)
        // 引用完整性：营养数据的食材名都能对上食材(否则静默跳过)。
        assertTrue(seeder.validateNutritionSeedForTest().isEmpty(), "营养 seed 存在对不上的食材名: ${seeder.validateNutritionSeedForTest()}")
        assertTrue(seeder.nutritionCountForTest() >= 60, "营养数据应已覆盖常见食材(≥60)")

        seeder.seedIfNeeded()
        val nutritionRepo = com.sxdbsm.cookbook.data.repository.NutritionRepository(db)
        val eggId = IngredientRepository(db).search("鸡蛋").first { it.name == "鸡蛋" }.id
        val n = nutritionRepo.ingredientNutrition(eggId)
        assertTrue(n != null && n.energyKcal == 144.0 && n.pieceGram == 50.0, "鸡蛋营养应入库(每100g 144kcal, 单件50g)")

        // 单位克当量已回填：克=1、两=50。
        val units = db.cookbookQueries.selectAllMeasurementUnits().executeAsList().associateBy { it.name }
        assertEquals(1.0, units["克"]?.grams)
        assertEquals(50.0, units["两"]?.grams)
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

    @Test
    fun seedWritesContentFingerprintAndUserDefaults() = runBlocking {
        // [AI生成] 验证内容指纹守卫：seed 后指纹与用户默认偏好都写入（元数据 key 不会挤掉默认偏好）。
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        PresetDataSeeder(db).seedIfNeeded()

        val fingerprint = q.selectPreference(PreferenceKeys.SEED_CONTENT_FINGERPRINT).executeAsOneOrNull()?.value_
        assertTrue(!fingerprint.isNullOrBlank(), "seed 后应写入内容指纹")
        assertTrue(q.selectPreference(PreferenceKeys.SEED_LEGACY_SANITIZED).executeAsOneOrNull()?.value_ == "1", "旧库清洗标记应写入")
        assertTrue(
            q.selectPreference(PreferenceKeys.THEME_MODE).executeAsOneOrNull()?.value_ == "system",
            "用户默认主题偏好不应因 seed 元数据 key 而被漏写",
        )
    }

    @Test
    fun seedCreatesPresetDishesWithMainIngredientAndSteps() = runBlocking {
        // [AI生成] 验证预设经典菜：写入 dish（source=preset）+ 主料(is_main) + 步骤，且重复 seed 不重复插入。
        val db = RepositoryTestDatabase.create()
        val seeder = PresetDataSeeder(db)
        seeder.seedIfNeeded()
        val q = db.cookbookQueries

        val dish = q.selectAllDishes().executeAsList().firstOrNull { it.name == "青椒炒肉丝" }
        assertTrue(dish != null, "应生成预设菜：青椒炒肉丝")
        assertEquals("preset", dish!!.source, "预设菜来源应为 preset")
        val dishIngredients = q.selectIngredientsOfDish(dish.id).executeAsList()
        assertTrue(dishIngredients.any { it.ingredient_name == "青椒" && it.is_main == 1L }, "青椒应作为主料(is_main)")
        assertTrue(q.selectStepsOfDish(dish.id).executeAsList().isNotEmpty(), "预设菜应有做法步骤")
        assertEquals("家常菜", dish.cuisine, "预设菜应写入菜系(青椒炒肉丝=家常菜)") // [AI生成] 菜系独立字段回归
        val mapo = q.selectAllDishes().executeAsList().first { it.name == "麻婆豆腐" }
        assertEquals("川菜", mapo.cuisine, "地域招牌应标对应菜系(麻婆豆腐=川菜)")

        val dishCountBefore = q.selectAllDishes().executeAsList().size
        seeder.forceReseedBaseData()
        assertEquals(dishCountBefore, q.selectAllDishes().executeAsList().size, "重复 seed 不应重复插入预设菜")
    }

    @Test
    fun dishSeedJsonHasNoDanglingReferences() {
        // [AI生成] D5 扩充红线守卫：每道预设菜的食材名/烹饪方式/单位都必须能解析，否则 seeder 静默跳过导致少关联。
        val db = RepositoryTestDatabase.create()
        val problems = PresetDataSeeder(db).validateDishSeedForTest()
        assertTrue(problems.isEmpty(), "预设菜 JSON 存在无法解析的引用：$problems")
    }

    @Test
    fun forceReseedIsIdempotentAndKeepsUserData() = runBlocking {
        // [AI生成] “更新基础数据”强制重跑：内容不变时数据条数不变，且不删除/覆盖用户自建食材。
        val db = RepositoryTestDatabase.create()
        val seeder = PresetDataSeeder(db)
        seeder.seedIfNeeded()
        val ingredientRepo = IngredientRepository(db)
        val userIngredientId = ingredientRepo.createUserIngredient(name = "我的自建食材", alias = "custom")
        val presetCountBefore = db.cookbookQueries.selectAllIngredients().executeAsList().size

        val changed = seeder.forceReseedBaseData()

        assertTrue(changed, "强制更新应执行写入并返回 true")
        assertEquals(
            presetCountBefore,
            db.cookbookQueries.selectAllIngredients().executeAsList().size,
            "强制更新不应改变食材总数（幂等）",
        )
        assertTrue(
            ingredientRepo.search("我的自建食材").any { it.id == userIngredientId },
            "强制更新基础数据不应删除用户自建食材",
        )
        assertFalse(
            ingredientRepo.search("我的自建食材").isEmpty(),
            "用户自建食材应仍然可查",
        )
    }
}
