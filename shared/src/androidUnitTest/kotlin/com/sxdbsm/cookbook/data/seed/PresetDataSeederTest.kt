package com.sxdbsm.cookbook.data.seed

import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
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

        // [AI修改] 单位克当量已回填：g=1、两=50(常用单位统一英文符号)。
        val units = db.cookbookQueries.selectAllMeasurementUnits().executeAsList().associateBy { it.name }
        assertEquals(1.0, units["g"]?.grams)
        assertEquals(50.0, units["两"]?.grams)
    }

    @Test
    fun `健康安全_动物内脏配了痛风避免规则_seed后对痛风显慎选红`() = runBlocking {
        // [AI生成] F#附2 回归守卫：忌口补漏(肥肠→痛风avoid)+引擎压制修(录低值反判绿)整链——
        //   肥肠即便录了偏低嘌呤(数值可能判黄)，因 care 规则痛风avoid，IngredientCrowdCare 应压成慎选(红)。
        //   守住"低嘌呤内脏误显可食"的健康安全缺口不回归。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val repo = IngredientRepository(db)
        val nutritionRepo = com.sxdbsm.cookbook.data.repository.NutritionRepository(db)
        val feichang = repo.search("肥肠").firstOrNull { it.name == "肥肠" }
        assertNotNull(feichang, "seed 应含食材：肥肠")
        val care = repo.listCareRules(feichang.id)
        assertTrue(
            care.any { r ->
                com.sxdbsm.cookbook.domain.HealthCondition.GOUT in com.sxdbsm.cookbook.domain.HealthCondition.fromCareName(r.categoryName) &&
                    r.adviceLevel == AdviceLevel.AVOID
            },
            "肥肠应已 seed 痛风 avoid 忌口规则，实得：${care.map { it.categoryName to it.adviceLevel }}",
        )
        val verdicts = com.sxdbsm.cookbook.domain.IngredientCrowdCare.evaluate(
            feichang.name, nutritionRepo.ingredientNutrition(feichang.id), care,
        )
        val gout = verdicts.first { it.condition == com.sxdbsm.cookbook.domain.HealthCondition.GOUT }
        assertEquals(com.sxdbsm.cookbook.domain.CrowdFit.CAUTION, gout.fit, "动物内脏(肥肠)对痛风应显慎选(红)")
    }

    @Test
    fun `F#8 更新记录游标_首装基线对齐不弹_落后告知则有待告知_markNotified清空`() = runBlocking {
        // [AI生成] F#8 透明准则:reseed 告知游标逻辑守卫。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded() // 首装→APPLIED=NOTIFIED=latest(基线对齐)
        val center = SeedUpdateCenter(db)
        val latest = center.latestVersion()
        assertTrue(latest >= 1, "内置 changelog 应至少有 1 版，实得 $latest")
        assertTrue(center.pendingChangelog().isEmpty(), "首装应基线对齐、无待告知(不追溯弹历史变更)")
        assertFalse(center.hasUnnotified(), "首装不应亮红点")

        // 模拟老用户：已告知停在更早版本(latest-1)，已应用=latest → 应有待告知
        db.cookbookQueries.upsertPreference(PreferenceKeys.SEED_NOTIFIED_CHANGELOG_VERSION, (latest - 1).toString(), 0L)
        assertTrue(center.pendingChangelog().isNotEmpty(), "已告知落后于已应用→应有待告知条目")
        assertTrue(center.hasUnnotified(), "应亮红点")

        // 标记已告知→幂等清空(消红点)
        center.markNotified(0L)
        assertTrue(center.pendingChangelog().isEmpty(), "markNotified 后应无待告知")
        assertFalse(center.hasUnnotified(), "红点应消")
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
    fun `脂肪肝care分类与食材规则_seed后完整且唯一`() = runBlocking {
        // [AI生成] L2：守护既有 category-id seed 链路，不引入 HealthCondition 或专属算法。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val fattyLiver = db.cookbookQueries.selectAllFoodCategories().executeAsList()
            .filter { it.name == "非酒精性脂肪肝" && it.dimension == "crowd" }
        assertEquals(1, fattyLiver.size, "应存在唯一的 crowd 分类：非酒精性脂肪肝")

        val careRules = IngredientRepository(db).listByCareCategories(listOf(fattyLiver.single().id))
        assertEquals(18, careRules.size, "脂肪肝应写入既有的 18 条食材级规则")
        val rules = careRules.associate { it.name to it.adviceLevel }
        assertEquals(AdviceLevel.RECOMMEND, rules["燕麦"], "燕麦应为推荐")
        assertEquals(AdviceLevel.LIMIT, rules["大米"], "大米应为限量")
        assertEquals(AdviceLevel.AVOID, rules["白糖"], "白糖应为避免")
        assertEquals(AdviceLevel.AVOID, rules["啤酒"], "啤酒应为避免")
    }

    @Test
    fun `脂肪肝care规则_重复seed不重复写入`() = runBlocking {
        // [AI生成] L2：同一数据库重跑 seed 必须保持 category-id 和规则集合幂等。
        val db = RepositoryTestDatabase.create()
        val seeder = PresetDataSeeder(db)
        seeder.seedIfNeeded()
        val category = db.cookbookQueries.selectAllFoodCategories().executeAsList()
            .single { it.name == "非酒精性脂肪肝" && it.dimension == "crowd" }
        val rulesBefore = IngredientRepository(db).listByCareCategories(listOf(category.id)).size
        assertEquals(18, rulesBefore, "首次 seed 应有 18 条脂肪肝食材级规则")

        seeder.seedIfNeeded()

        val categoriesAfter = db.cookbookQueries.selectAllFoodCategories().executeAsList()
            .filter { it.name == "非酒精性脂肪肝" && it.dimension == "crowd" }
        val rulesAfter = IngredientRepository(db).listByCareCategories(listOf(categoriesAfter.single().id)).size
        assertEquals(1, categoriesAfter.size, "重复 seed 后脂肪肝分类不应重复")
        assertEquals(18, rulesAfter, "重复 seed 后脂肪肝规则仍应完整")
        assertEquals(rulesBefore, rulesAfter, "重复 seed 后脂肪肝规则不应重复")
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
    fun `A1_自建菜空用量配料_seed后按分类补默认克数与克单位`() = runBlocking {
        // [AI生成] A1 回归：模拟老 bug——用户加食材到自建菜没设量(quantity=NULL)→营养算 0。
        // seed 应按食材名分类补智能默认克数(蛋类=50)+克单位；且**只修 source='user'**(预设菜空量走 seed 循环填正确值)。
        val db = RepositoryTestDatabase.create()
        val seeder = PresetDataSeeder(db)
        seeder.seedIfNeeded()
        val q = db.cookbookQueries
        val eggId = q.selectIngredientIdByNameIncludingInactive("鸡蛋").executeAsOneOrNull()?.id
        assertNotNull(eggId, "seed 应含鸡蛋")
        val gramId = q.selectAllMeasurementUnits().executeAsList().first { it.name == "g" || it.name == "克" }.id
        q.insertDish(
            name = "测试蒸蛋A1", cooking_method_id = null, special_note = "", description = "",
            image_path = "", thumbnail_path = "", source = "user", created_at = 0L, updated_at = 0L, cuisine = "",
        )
        val dishId = q.lastInsertId().executeAsOne()
        q.insertDishIngredient(dish_id = dishId, ingredient_id = eggId!!, quantity = null, unit_id = null, is_main = 1L)
        seeder.forceReseedBaseData() // 重跑 seed 触发 A1 修复
        val egg = q.selectIngredientsOfDish(dishId).executeAsList().first { it.ingredient_name == "鸡蛋" }
        assertEquals(50.0, egg.quantity, "鸡蛋空用量应补为蛋类默认 50g(非 0/非 NULL)")
        assertEquals(gramId, egg.unit_id, "空用量修复应同时配克单位")
    }

    @Test
    fun dishSeedJsonHasNoDanglingReferences() {
        // [AI生成] D5 扩充红线守卫：每道预设菜的食材名/烹饪方式/单位都必须能解析，否则 seeder 静默跳过导致少关联。
        val db = RepositoryTestDatabase.create()
        val problems = PresetDataSeeder(db).validateDishSeedForTest()
        assertTrue(problems.isEmpty(), "预设菜 JSON 存在无法解析的引用：$problems")
    }

    @Test
    fun seed回填营养大类food_group() = runBlocking {
        // [AI生成] A1：seed 后预设食材应按名回填 food_group(空的才填,不覆盖)；供色系/均衡按显式大类判定。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val map = IngredientRepository(db).foodGroupByName()
        assertEquals("EGG", map["鸡蛋"], "鸡蛋应回填为蛋类")
        assertEquals("RED_MEAT", map["猪瘦肉"], "猪瘦肉应回填为红肉")
        // 显式大类覆盖关键词
        val explicit = com.sxdbsm.cookbook.domain.FoodGroup.explicitFrom(mapOf("某自定义无关键词菜" to "VEGETABLE"))
        val groups = com.sxdbsm.cookbook.domain.FoodGroup.groupsOf(listOf("某自定义无关键词菜"), explicit)
        assertTrue(groups.contains(com.sxdbsm.cookbook.domain.FoodGroup.Group.VEGETABLE), "显式大类应覆盖关键词判定")
    }

    @Test
    fun detailAndCategorySeedHasNoDanglingOrDuplicateReferences() {
        // [AI生成] 补数据回归守卫：食材详情引用、食材/调养规则挂载的分类 code、以及各 JSON 内部 name/code 重复都要干净，
        // 否则 seeder 静默丢关联（详情/分类不生效）而非报红。
        val db = RepositoryTestDatabase.create()
        val problems = PresetDataSeeder(db).validateDetailAndCategorySeedForTest()
        assertTrue(problems.isEmpty(), "食材详情/分类 code/重复项存在问题：$problems")
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

    @Test
    fun `v29个人忌口白名单分类都能展开出食材_防seed漂移致忌口静默失效`() = runBlocking {
        // [AI生成] v29 引用完整性守卫(Google审查建议)：个人忌口 chip 白名单的 15 个 general 分类，
        //   每个都必须能 listByCategories 展开出≥1食材——否则用户选它=静默无效(seed 漂移/漏绑该子分类 code)。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val idByName = db.cookbookQueries.selectAllFoodCategories().executeAsList()
            .filter { it.dimension == "general" }.associate { it.name to it.id }
        val repo = IngredientRepository(db)
        // 与 FamilyRepository.AVOID_CATEGORY_WHITELIST 分类名一致。
        val whitelist = listOf(
            "猪肉类", "牛肉类", "羊肉类", "禽肉类", "动物内脏类", "鱼类", "虾蟹类", "贝类", "蛋类",
            "奶类", "大豆及坚果", "食用菌类", "藻类", "葱蒜类", "香辛料类",
        )
        whitelist.forEach { name ->
            val id = idByName[name]
            assertNotNull(id, "个人忌口白名单分类「$name」在 seed 中缺失(chip 会消失)")
            assertTrue(repo.listByCategories(listOf(id)).isNotEmpty(), "分类「$name」展开为空→选它忌口将静默无效(seed 漂移?)")
        }
        Unit
    }
}
