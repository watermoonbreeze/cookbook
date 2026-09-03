package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.CookingMethodInferrer
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : NdjsonFoodGroupMapTest
 * @Time : 2026/09/03
 * @Author : SXD-AI
 * @Desc : AUTOGEN-UNIFY 透传链与推断组件单测——T-AU-03/04/10/11/13
 * <p>
 * - T-AU-03：词表映射全量（7 精确词直用；meat/seasoning/未知→null）
 * - T-AU-04：NDJSON 主路端到端（DayMealJson.food_group → previewAll → IngredientPreview.group·判别性夹具豆腐）
 * - T-AU-13：FLAT 回退路端到端（FlatToDayMealConverter → previewAll 同口径）
 * - T-AU-10/11：RuleMealParser 委托后行为等价 + CookingMethodInferrer 字表
 * <p>
 * [AI生成] AUTOGEN-UNIFY。
 **/
class NdjsonFoodGroupMapTest {

    private lateinit var db: CookbookDatabase
    private lateinit var recorder: MultiDayRecorder

    @BeforeTest
    fun setUp() {
        db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        q.insertMeasurementUnit("g", "preset", 1.0)
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        q.insertMealType("DINNER", "晚餐", "18:00", 1, "preset")
        listOf(
            "谷薯主食类", "蔬菜类", "菌藻类", "水果类", "水产类",
            "畜禽肉类", "蛋类", "奶类", "大豆及坚果",
        ).forEachIndexed { i, cn ->
            q.insertFoodCategory(cn, "general", null, null, (i + 1).toLong(), "", "preset", 0)
        }
        // 烹饪方式字典（预设 10 项——预选交集判定的字典面）
        listOf("炒", "蒸", "煮", "炖", "烤", "凉拌", "煎", "炸", "焖", "卤").forEach {
            q.insertCookingMethod(it, "preset", 0L)
        }
        recorder = MultiDayRecorder(
            ingredientRepo = IngredientRepository(db),
            dishRepo = DishRepository(db),
            mealRepo = MealRecordRepository(db),
            nutritionRepo = NutritionRepository(db),
            aliasResolver = IngredientAliasResolver.fromJson("{}"),
            db = db,
        )
    }

    // ═══════════════════════════════════════════════════
    // T-AU-04a：NDJSON 主路 mapper 段（终审 AF-1 补——断链修复本体零覆盖）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-04a MealStreamDraftMapper主料分支透传food_group_调料分支不填`() {
        val draft = MealStreamDraft(
            segments = mapOf(
                "quick-2026-09-03" to SegmentDraft(
                    segmentId = "quick-2026-09-03",
                    meals = mapOf(
                        "2026-09-03|lunch" to MealDraftNode(
                            mealId = "2026-09-03|lunch",
                            date = "2026-09-03",
                            slot = "lunch",
                            dishes = mapOf(
                                "2026-09-03|lunch|d1" to DishDraftNode(
                                    dishId = "2026-09-03|lunch|d1",
                                    name = "豆腐汤",
                                    ingredients = listOf(
                                        DraftIngredient(name = "豆腐", foodGroup = "dairy", quantity = 100.0, isMain = true),
                                    ),
                                    seasonings = listOf(
                                        DraftSeasoning(name = "盐", quantity = 3.0),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val segments = listOf(
            InputSegment(
                segmentId = "quick-2026-09-03",
                targetDate = LocalDate(2026, 9, 3),
                inputText = "中午喝了豆腐汤",
                ordinal = 0,
            ),
        )

        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)
        val ings = days.single().meals.single().dishes.single().dish?.ingredients.orEmpty()
        // 主料分支：DraftIngredient.foodGroup → FoodJson.food_group（断链修复本体——此前此处被丢弃）
        assertEquals("dairy", ings.first { it.food?.name == "豆腐" }.food?.food_group,
            "mapper 主料分支必须把 AI 判定的 food_group 填进 FoodJson（AF-1：本改动点零覆盖即假绿）")
        // 调料分支：DraftSeasoning 无该字段，保持 null（不填·协议现状）
        assertEquals(null, ings.first { it.food?.name == "盐" }.food?.food_group,
            "调料分支不得填 food_group（DraftSeasoning 无该字段）")
    }

    // ═══════════════════════════════════════════════════
    // T-AU-03：词表映射（INV-AU-03/04）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-03 七个精确词一一映射`() {
        assertEquals(FoodGroup.Group.STAPLE, NdjsonFoodGroupMap.map("staple"))
        assertEquals(FoodGroup.Group.VEGETABLE, NdjsonFoodGroupMap.map("vegetable"))
        assertEquals(FoodGroup.Group.FRUIT, NdjsonFoodGroupMap.map("fruit"))
        assertEquals(FoodGroup.Group.DAIRY, NdjsonFoodGroupMap.map("dairy"))
        assertEquals(FoodGroup.Group.EGG, NdjsonFoodGroupMap.map("egg"))
        assertEquals(FoodGroup.Group.BEAN, NdjsonFoodGroupMap.map("bean"))
        assertEquals(FoodGroup.Group.FISH, NdjsonFoodGroupMap.map("seafood"), "seafood→FISH（词表词≠枚举名）")
        // 大小写归一
        assertEquals(FoodGroup.Group.DAIRY, NdjsonFoodGroupMap.map(" Dairy "))
    }

    @Test
    fun `T-AU-03 meat与seasoning及未知词一律null走本地`() {
        assertEquals(null, NdjsonFoodGroupMap.map("meat"), "meat 粗粒度（不分红/白）→null 交 classify 细分")
        assertEquals(null, NdjsonFoodGroupMap.map("seasoning"), "FoodGroup 无调料枚举→null")
        assertEquals(null, NdjsonFoodGroupMap.map("banana"), "未知词→null")
        assertEquals(null, NdjsonFoodGroupMap.map(""), "空→null")
        assertEquals(null, NdjsonFoodGroupMap.map(null), "null→null")
    }

    // ═══════════════════════════════════════════════════
    // T-AU-04：NDJSON 主路端到端透传（INV-AU-01/03·判别性夹具：豆腐 BEAN + hint dairy）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-04 DayMealJson的food_group端到端透传到preview的group`() = runBlocking {
        val day = DayMealJson(
            date = "2026-09-03",
            meals = listOf(
                MealJson(
                    meal_type = "lunch",
                    meal_time = "12:00",
                    dishes = listOf(
                        MealDishRefJson(
                            name = "豆腐汤",
                            dish = DishJson(
                                name = "豆腐汤",
                                ingredients = listOf(
                                    // classify("豆腐")=BEAN；AI 给 dairy——断言 DAIRY 才能证明 hint 生效（判别性）
                                    DishIngredientJson(
                                        food = FoodJson(name = "豆腐", food_group = "dairy"),
                                        quantity = 100.0,
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        )
        val preview = recorder.previewAll(listOf(day), LocalDate(2026, 9, 3))
        val ing = preview.days.single().meals.single().dishes.single().ingredients.single()
        assertEquals(FoodGroup.Group.DAIRY, ing.group, "NDJSON 主路：food_group 应经 map 透传到 groupHint→group（INV-AU-03）")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // T-AU-13：FLAT 回退路端到端（INV-AU-13·AF-01）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-13 FLAT整体JSON回退路同享food_group透传`() = runBlocking {
        val flat = FlatMealJson(
            items = listOf(
                FlatMealItem(
                    dish_name = "豆腐煲",
                    date = "2026-09-03",
                    meal_type = "lunch",
                    ingredients = listOf(
                        FlatIngredientItem(name = "豆腐", quantity = 200.0, food_group = "egg"),
                    ),
                ),
            ),
        )
        val converted = FlatToDayMealConverter.convert(flat, LocalDate(2026, 9, 3))
        // 第一跳：converter 产出 FoodJson.food_group（此前该路径连 FoodJson 都不建）
        val foodJson = converted.days.single().meals.single().dishes.single().dish?.ingredients?.single()?.food
        assertNotNull(foodJson, "FLAT 路径必须构造 FoodJson（AF-01 修复）")
        assertEquals("egg", foodJson.food_group)

        // 第二跳：与主路同享 MultiDayRecorder 透传（hint=EGG 压过 classify 的 BEAN）
        val preview = recorder.previewAll(converted.days, LocalDate(2026, 9, 3))
        val ing = preview.days.single().meals.single().dishes.single().ingredients.single()
        assertEquals(FoodGroup.Group.EGG, ing.group, "FLAT 回退路应与 NDJSON 主路同口径（INV-AU-13）")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // T-AU-10/11：烹饪方式推断（INV-AU-10·字表冻结值）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-11 inferFromName字表逐字对齐`() {
        assertEquals(listOf("蒸"), CookingMethodInferrer.inferFromName("清蒸鲈鱼"))
        assertEquals(listOf("烧"), CookingMethodInferrer.inferFromName("红烧肉"))
        assertEquals(emptyList(), CookingMethodInferrer.inferFromName("白切鸡"), "白切鸡不含字表字")
        // 多命中保字表序
        assertEquals(listOf("炒", "煮"), CookingMethodInferrer.inferFromName("小炒黄牛肉煮锅"))
    }

    @Test
    fun `T-AU-10 RuleMealParser委托后烹饪方式提取行为等价`() = runBlocking {
        val today = LocalDate(2026, 9, 3)
        val days = RuleMealParser.parse("中午吃了清蒸鲈鱼", today = today)
        val dishRef = days.first().meals.first().dishes.first()
        assertEquals(listOf("蒸"), dishRef.dish?.cooking_methods, "parser 级断言：委托共享组件后行为等价（S-05 补）")
        Unit
    }
}
