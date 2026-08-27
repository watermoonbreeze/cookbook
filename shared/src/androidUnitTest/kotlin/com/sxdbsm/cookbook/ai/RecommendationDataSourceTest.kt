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
        val ds = RecommendationDataSource(db, pantry, dishRepo, com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)), ingredientRepo, com.sxdbsm.cookbook.data.repository.NutritionRepository(db))

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
    fun `负反馈踩_被标不再推荐的菜不再进候选_恢复后又回来`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val pantry = PantryRepository(db)
        val ds = RecommendationDataSource(db, pantry, dishRepo, com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)), ingredientRepo, com.sxdbsm.cookbook.data.repository.NutritionRepository(db))

        val porkId = ingredientRepo.search("五花肉").first { it.name == "五花肉" }.id
        val dishId = dishRepo.saveDish(
            id = 0, name = "会被踩的红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = porkId, name = "五花肉"), isMain = true)),
            steps = emptyList(),
        )
        pantry.addToPantry(porkId)

        assertTrue(ds.gather(RecommendMode.PANTRY).dishes.any { it.id == dishId }, "踩前应在候选")
        dishRepo.setDishDisliked(dishId, true)
        assertFalse(ds.gather(RecommendMode.PANTRY).dishes.any { it.id == dishId }, "踩后应从候选剔除(不再推荐)")
        dishRepo.setDishDisliked(dishId, false)
        assertTrue(ds.gather(RecommendMode.PANTRY).dishes.any { it.id == dishId }, "恢复后又进候选")
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
        val ds = RecommendationDataSource(db, pantry, dishRepo, com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)), ingredientRepo, com.sxdbsm.cookbook.data.repository.NutritionRepository(db))

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
        val ds = RecommendationDataSource(db, pantry, dishRepo, com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)), ingredientRepo, com.sxdbsm.cookbook.data.repository.NutritionRepository(db))

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
    fun `gatherForPlan_批量取数_覆盖全库有效菜且主料分组正确`() = runBlocking {
        // [AI生成] 性能优化回归：gatherForPlan 由"逐菜 getDishById(N+1)"改为"两条批量查询+内存分组"，
        //   守护：①不漏菜(覆盖 selectAllDishes 全量) ②每道菜配料按 dish_id 正确分组(主料不串行)。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val q = db.cookbookQueries
        val ds = RecommendationDataSource(
            db, PantryRepository(db), DishRepository(db),
            com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)),
            IngredientRepository(db), com.sxdbsm.cookbook.data.repository.NutritionRepository(db),
        )

        val ctx = ds.gatherForPlan()
        val activeCount = q.selectAllDishes().executeAsList().size
        assertEquals(activeCount, ctx.dishes.size, "PlanContext 应覆盖全库有效菜(批量取数不漏菜)")

        // 已知预设菜主料正确(批量 join 分组无误、未把别的菜配料串到红烧肉)。
        val hongshao = ctx.dishes.first { it.name == "红烧肉" }
        assertTrue(hongshao.mainNames.contains("五花肉"), "红烧肉主料应含五花肉(批量配料按 dish_id 分组正确)")
        assertFalse(hongshao.mainNames.isEmpty(), "有配料的菜 mainNames 不应为空")
        Unit
    }

    @Test
    fun `gather_批量组装RuleDish_角色判定正确_主料MAIN调料SEASONING`() = runBlocking {
        // [AI生成] 性能优化回归：gather 由"逐菜 getDishById + toRuleDish"改为"DishMini + 批量配料内联组角色"。
        //   守护：主料→MAIN、调料→SEASONING、辅料→SECONDARY(替代 toRuleDish 的角色判定不退化)。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val ingredientRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val pantry = PantryRepository(db)
        val ds = RecommendationDataSource(
            db, pantry, dishRepo,
            com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)),
            ingredientRepo, com.sxdbsm.cookbook.data.repository.NutritionRepository(db),
        )

        val porkId = ingredientRepo.search("五花肉").first { it.name == "五花肉" }.id
        val saltId = ingredientRepo.search("盐").first { it.name == "盐" }.id
        val dishId = dishRepo.saveDish(
            id = 0, name = "角色测试红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = porkId, name = "五花肉"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = saltId, name = "盐"), isMain = false),
            ),
            steps = emptyList(),
        )
        pantry.addToPantry(porkId)

        val input = ds.gather(RecommendMode.PANTRY)
        val rule = input.dishes.first { it.id == dishId }
        val pork = rule.ingredients.first { it.ingredientId == porkId }
        val salt = rule.ingredients.first { it.ingredientId == saltId }
        assertEquals(com.sxdbsm.cookbook.ai.model.IngredientRole.MAIN, pork.role, "主料五花肉应判 MAIN")
        assertEquals(com.sxdbsm.cookbook.ai.model.IngredientRole.SEASONING, salt.role, "盐应判 SEASONING(调料默认常备)")
        Unit
    }

    @Test
    fun freePairingWorksOnRealSeedData() = runBlocking {
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded() // 灌入食材 + general 大类关联
        val pantry = PantryRepository(db)
        val ingredientRepo = IngredientRepository(db)
        val dataSource = RecommendationDataSource(
            db, pantry, DishRepository(db), com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)), ingredientRepo,
            com.sxdbsm.cookbook.data.repository.NutritionRepository(db),
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

    @Test
    fun `换一换缓存红线_gatherConstraints实时反映健康档案忌口_且与gather一致`() = runBlocking {
        // [AI生成] 换一换缓存(阶段1)红线守护：换一换复用缓存候选时,忌口约束改由 gatherConstraints() **每次重取覆盖**。
        //   本测证明 gatherConstraints() **实时反映别页(健康档案)新设的忌口**——否则缓存的旧忌口会让新设的忌口菜被推进一餐(红线)。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val ds = RecommendationDataSource(
            db, PantryRepository(db), DishRepository(db),
            com.sxdbsm.cookbook.data.repository.FamilyRepository(db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db)),
            IngredientRepository(db), com.sxdbsm.cookbook.data.repository.NutritionRepository(db),
        )
        val healthRepo = HealthProfileRepository(db)

        // 未设健康档案：无关注标签；gather 与 gatherConstraints 忌口一致(抽取行为等价)。
        val before = ds.gatherConstraints()
        assertTrue(before.constraints.labels.isEmpty(), "未设健康档案时无关注标签")
        assertEquals(
            before.constraints.avoidIngredientIds,
            ds.gather(RecommendMode.PANTRY).constraints.avoidIngredientIds,
            "gather 与 gatherConstraints 忌口一致(未设档案)",
        )

        // 启用一个健康档案(痛风/高尿酸·含忌口食材)。
        val crowd = healthRepo.listAllCrowdTypes().first { it.name.contains("痛风") || it.name.contains("尿酸") }
        healthRepo.add(crowd.id)

        // 红线:再取 constraints 立即反映新设忌口(=换一换缓存复用候选时重取忌口能拦住新设的忌口菜)。
        val after = ds.gatherConstraints()
        assertTrue(after.constraints.labels.isNotEmpty(), "启用健康档案后关注标签应非空(gatherConstraints 实时反映别页改的健康档案·红线)")
        assertTrue(
            after.constraints.avoidIngredientIds.isNotEmpty() || after.constraints.limitIngredientIds.isNotEmpty(),
            "痛风档案应带忌口/限量食材(care 规则实时生效)",
        )
        // 缓存复用时"覆盖 constraints"等价于全量 gather 的 constraints。
        assertEquals(
            after.constraints.avoidIngredientIds,
            ds.gather(RecommendMode.PANTRY).constraints.avoidIngredientIds,
            "gather 与 gatherConstraints 忌口一致(启用档案后)——缓存复用覆盖等价全量",
        )
        Unit
    }

    @Test
    fun `脂肪肝care经真实gatherConstraints消费既有三档食材规则`() = runBlocking {
        // [AI生成] L2：从全家 member_care 的真实 category-id 输入验证 gatherConstraints 三档消费；不新增 HealthCondition。
        val db = RepositoryTestDatabase.create()
        PresetDataSeeder(db).seedIfNeeded()
        val family = com.sxdbsm.cookbook.data.repository.FamilyRepository(
            db, com.sxdbsm.cookbook.data.repository.PreferenceRepository(db),
        )
        family.ensureInitialized()
        val fattyLiver = HealthProfileRepository(db).listAllCrowdTypes().single { it.name == "非酒精性脂肪肝" }
        val self = family.listMembers().single { it.isSelf }
        family.updateMember(self.copy(careCategoryIds = listOf(fattyLiver.id)))
        val ingredientRepo = IngredientRepository(db)
        val dataSource = RecommendationDataSource(
            db, PantryRepository(db), DishRepository(db), family, ingredientRepo,
            com.sxdbsm.cookbook.data.repository.NutritionRepository(db),
        )

        val constraints = dataSource.gatherConstraints().constraints
        suspend fun ingredientId(name: String) = ingredientRepo.search(name).single { it.name == name }.id
        assertTrue(ingredientId("燕麦") in constraints.recommendIngredientIds, "燕麦应消费为推荐")
        assertTrue(ingredientId("大米") in constraints.limitIngredientIds, "大米应消费为限量")
        assertTrue(ingredientId("白糖") in constraints.avoidIngredientIds, "白糖应消费为避免")
        assertTrue(ingredientId("啤酒") in constraints.avoidIngredientIds, "啤酒应消费为避免")
    }
}
