package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.FoodGroup
import com.sxdbsm.cookbook.domain.NutritionGuessSource
import com.sxdbsm.cookbook.domain.NutritionGuessValues
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : AutoGenUnifyTest
 * @Time : 2026/09/03
 * @Author : SXD-AI
 * @Desc : AUTOGEN-UNIFY 批次单测——字段级「AI 优先、本地兜底」+ 仅空列守卫 + source 语义
 * <p>
 * 覆盖蓝图 §8：T-AU-01/02/05/06/07/08/09/14/15（夹具口径=真库真 repo；
 * 判别性硬约束：凡断言 AI 优先的用例，classify(name) 必须 ≠ groupHint——防"实现没用 hint 断言也绿"）。
 * <p>
 * [AI生成] AUTOGEN-UNIFY（四角色审核后冻结蓝图 BLUEPRINT_READY）。
 **/
class AutoGenUnifyTest {

    private lateinit var db: CookbookDatabase
    private lateinit var ctx: AutoGenContext
    private lateinit var generator: IngredientAutoGenerator

    @BeforeTest
    fun setUp() = runBlocking {
        db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries

        q.insertMeasurementUnit("g", "preset", 1.0)
        q.insertMeasurementUnit("个", "preset", null)
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        q.insertMealType("DINNER", "晚餐", "18:00", 1, "preset")
        listOf(
            "谷薯主食类", "蔬菜类", "菌藻类", "水果类", "水产类",
            "畜禽肉类", "蛋类", "奶类", "大豆及坚果",
        ).forEachIndexed { i, cn ->
            q.insertFoodCategory(cn, "general", null, null, (i + 1).toLong(), "", "preset", 0)
        }

        ctx = AutoGenContext.load(db, IngredientAliasResolver.fromJson("{}"))
        generator = IngredientAutoGenerator(IngredientRepository(db), NutritionRepository(db))
    }

    // ═══════════════════════════════════════════════════
    // INV-AU-01/02：字段级 AI 优先（判别性夹具：豆腐 classify=BEAN ≠ hint）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-01 groupHint非空时优先于本地classify`() = runBlocking {
        // classify("豆腐")=BEAN（豆关键词），hint=DAIRY——若实现仍走 classify 会得 BEAN，断言即失败（判别性）
        val preview = generator.preview(SemanticIngredient(name = "豆腐", groupHint = FoodGroup.Group.DAIRY), ctx)
        assertEquals(FoodGroup.Group.DAIRY, preview.group, "groupHint 非空必须直用（INV-AU-01·决策点1A）")
        assertEquals("奶", preview.groupLabel, "groupLabel 应为 group 的派生（计算属性）")
        Unit
    }

    @Test
    fun `T-AU-02 groupHint为null时本地classify兜底`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "豆腐"), ctx)
        assertEquals(FoodGroup.Group.BEAN, preview.group, "hint=null 必须 classify 兜底（INV-AU-02）")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // INV-AU-05/07：CREATE 落库写列 + ref 区分
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-05 CREATE落库写food_group列且ref为自动估算`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "豆腐", groupHint = FoodGroup.Group.EGG), ctx)
        assertEquals(ResolveKind.CREATE, preview.resolution)
        val id = generator.commit(preview)

        val groupByName = IngredientRepository(db).foodGroupByName()
        assertEquals("EGG", groupByName["豆腐"], "food_group 列必须写入 preview.group 枚举名（INV-AU-05）")

        val nutrition = db.cookbookQueries.selectIngredientNutrition(id).executeAsOneOrNull()
        assertNotNull(nutrition, "营养应已写入")
        assertEquals("自动估算", nutrition.ref, "本地 NutritionGuesser 路径 ref=自动估算（INV-AU-07）")
        Unit
    }

    @Test
    fun `T-AU-08 nutritionHint走AI管线位且ref区分`() = runBlocking {
        val preview = generator.preview(
            SemanticIngredient(
                name = "豆腐",
                groupHint = FoodGroup.Group.DAIRY,
                nutritionHint = NutritionGuessValues(energyKcal = 88.0, proteinG = 6.0),
            ),
            ctx,
        )
        assertTrue(preview.nutrition.source is NutritionGuessSource.AI, "nutritionHint 非空→source=AI（管线位）")
        assertEquals(88.0, preview.nutrition.values?.energyKcal)

        val id = generator.commit(preview)
        val nutrition = db.cookbookQueries.selectIngredientNutrition(id).executeAsOneOrNull()
        assertNotNull(nutrition)
        assertEquals("AI 估算", nutrition.ref, "AI 估值 ref 必须区别于自动估算（INV-AU-07·T1 留痕）")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // INV-AU-06/14：REUSE 与陈旧碰撞都不覆盖既有值
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-06 REUSE不写food_group列`() = runBlocking {
        val q = db.cookbookQueries
        q.insertIngredient("鸡蛋", "", "jidan", "", "", "🥚", 1, "preset", 0)
        val eggId = q.lastInsertId().executeAsOne()
        q.updateIngredientFoodGroup("EGG", eggId)
        val freshCtx = AutoGenContext.load(db, IngredientAliasResolver.fromJson("{}"))

        val preview = generator.preview(SemanticIngredient(name = "鸡蛋", groupHint = FoodGroup.Group.DAIRY), freshCtx)
        assertEquals(ResolveKind.REUSE, preview.resolution)
        generator.commit(preview)

        assertEquals("EGG", IngredientRepository(db).foodGroupByName()["鸡蛋"], "REUSE 不得写 food_group 列（INV-AU-06·库内 EGG 保持）")
        Unit
    }

    @Test
    fun `T-AU-14 陈旧ctx误判CREATE_仅空列守卫不覆盖用户手填值`() = runBlocking {
        val q = db.cookbookQueries
        // 库已有"木耳"：用户手选大类 FUNGI + 手填营养（非空）
        q.insertIngredient("木耳", "", "muer", "", "", "🍄", 1, "user", 0)
        val woodId = q.lastInsertId().executeAsOne()
        q.updateIngredientFoodGroup("FUNGI", woodId)
        q.upsertIngredientNutrition(
            woodId, 300.0, 8.0, 1.0, 60.0, 10.0, 50.0, null, null, null, null,
            null, null, null, "用户手填", 1, 0,
        )
        // ctx 在 seed 木耳**之前**加载（陈旧：不含木耳 → preview 误判 CREATE）

        val id = generator.ensureCreated("木耳", ctx, source = "user")
        assertEquals(woodId, id, "createUserIngredient 内部去重应返回既有行 id")

        assertEquals("FUNGI", IngredientRepository(db).foodGroupByName()["木耳"], "陈旧 CREATE 碰撞不得覆盖用户手选大类（INV-AU-14·仅空列守卫）")
        val nutrition = q.selectIngredientNutrition(woodId).executeAsOneOrNull()
        assertEquals("用户手填", nutrition?.ref, "已有营养行(手填)不得被估算覆盖（INV-AU-14）")
        assertEquals(300.0, nutrition?.energy_kcal)
        Unit
    }

    // ═══════════════════════════════════════════════════
    // INV-AU-08/15：ensureCreated dedup + source 语义
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-07 ensureCreated同名两次同id且一次做齐`() = runBlocking {
        val id1 = generator.ensureCreated("青菜", ctx)
        val id2 = generator.ensureCreated("青菜", ctx)
        assertEquals(id1, id2, "同名二次调用必须返同一 id（INV-AU-08）")

        assertEquals("VEGETABLE", IngredientRepository(db).foodGroupByName()["青菜"], "ensureCreated 落库行应有大类（classify 兜底·判别性：青菜=VEGETABLE）")
        val nutrition = db.cookbookQueries.selectIngredientNutrition(id1).executeAsOneOrNull()
        assertNotNull(nutrition, "ensureCreated 落库行应有营养")
        Unit
    }

    @Test
    fun `T-AU-15 source参数化_快速自建user与AI记餐ai`() = runBlocking {
        val userId = generator.ensureCreated("茼蒿", ctx, source = "user")
        val aiId = generator.ensureCreated("油麦菜", ctx) // 默认 "ai"
        assertEquals("user", db.cookbookQueries.selectIngredientById(userId).executeAsOne().source,
            "快速自建传 user——删除/回收站/备份导出三处门控不回归（INV-AU-15·AF-AU-01）")
        assertEquals("ai", db.cookbookQueries.selectIngredientById(aiId).executeAsOne().source,
            "AI 记餐默认 ai 维持现状")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // INV-AU-09：回填查询扩源（user/ai/link·只查空列）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T-AU-09 回填查询覆盖user与ai_link且只查空列`() = runBlocking {
        val q = db.cookbookQueries
        fun seed(name: String, source: String): Long {
            q.insertIngredient(name, "", "", "", "", "🥗", 1, source, 0)
            return q.lastInsertId().executeAsOne()
        }
        val userId = seed("莙荙菜", "user")
        val aiId = seed("冰草", "ai")
        val linkId = seed("罗马生菜", "link")
        val filledId = seed("奶油生菜", "ai").also { q.updateIngredientFoodGroup("VEGETABLE", it) }

        val ids = q.selectAutoIngredientsWithoutFoodGroup().executeAsList().map { it.id }.toSet()
        assertTrue(userId in ids, "user 源空列应被回填查询覆盖")
        assertTrue(aiId in ids, "ai 源空列应被覆盖（扩源生效·INV-AU-09）")
        assertTrue(linkId in ids, "link 源空列应被覆盖（L4 预留）")
        assertTrue(filledId !in ids, "非空列不得进入回填（只填空纪律）")
        Unit
    }
}
