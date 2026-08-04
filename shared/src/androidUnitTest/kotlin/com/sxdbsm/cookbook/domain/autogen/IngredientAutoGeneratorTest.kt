package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.db.CookbookDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : IngredientAutoGeneratorTest
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 食材自动生成器单测——T-01(营养估算)/T-03(单位)/T-04(careFlag)/T-05(别名归一)/T-07(preview零写库)/T-08(commit落库)
 * <p>
 * [AI修改] A2 修复：createContext/createGenerator 改为共享同一 DB 实例，保证 preview 与 commit 操作
 * 在同一库上执行，commit 测试能真实验证 source="ai"（INV-07）和 ref="自动估算"（INV-05）。
 **/
class IngredientAutoGeneratorTest {

    // A2 修复：所有测试共享同一 DB，context 与 generator 基于同一库
    private lateinit var db: CookbookDatabase
    private lateinit var ctx: AutoGenContext
    private lateinit var generator: IngredientAutoGenerator

    @BeforeTest
    fun setUp() = runBlocking {
        db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries

        // 种入测量单位
        q.insertMeasurementUnit("g", "preset", 1.0)
        q.insertMeasurementUnit("个", "preset", null)
        q.insertMeasurementUnit("ml", "preset", 1.0)

        // 种入餐次
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        q.insertMealType("LUNCH",     "午餐", "12:00", 1, "preset")
        q.insertMealType("DINNER",    "晚餐", "18:00", 1, "preset")

        // 种入食物分类
        val catNames = listOf(
            "谷薯主食类", "蔬菜类", "菌藻类", "水果类", "水产类",
            "畜禽肉类", "蛋类", "奶类", "大豆及坚果",
        )
        catNames.forEachIndexed { i, cn ->
            q.insertFoodCategory(cn, "general", null, null, (i + 1).toLong(), "", "preset", 0)
        }
        q.insertFoodCategory("调味品", "general", null, null, 99, "", "preset", 0)

        // 种入五花肉（含营养，用于 T01 近似命中）
        q.insertIngredient("五花肉", "", "wuhuarou", "", "", "🥩", 1, "preset", 0)
        val porkId = q.lastInsertId().executeAsOne()
        val meatCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "畜禽肉类" }.id
        q.linkIngredientCategory(porkId, meatCatId)
        q.upsertIngredientNutrition(
            porkId, 508.0, 7.7, 53.0, 0.0, null, 79.0, null, null, null, 100.0,
            null, null, null, "中国食物成分表", 1, 0,
        )

        // 种入番茄（别名归一目标）
        q.insertIngredient("番茄", "", "fanqie", "", "", "🍅", 1, "preset", 0)
        val tomatoId = q.lastInsertId().executeAsOne()
        val vegCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "蔬菜类" }.id
        q.linkIngredientCategory(tomatoId, vegCatId)

        // 种入盐（调味品类·供 T03 验调料小克数；不在调味品类则 isSeasoning=false 默认 100g 不符预期）
        q.insertIngredient("盐", "", "yan", "", "", "🧂", 1, "preset", 0)
        val saltId = q.lastInsertId().executeAsOne()
        val seasoningCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "调味品" }.id
        q.linkIngredientCategory(saltId, seasoningCatId)

        val aliasJson = """{"蕃茄":"番茄","西红柿":"番茄"}"""
        val aliasResolver = IngredientAliasResolver.fromJson(aliasJson)

        // 关键：ctx 与 generator 使用同一个 db
        ctx = AutoGenContext.load(db, aliasResolver)
        generator = IngredientAutoGenerator(
            IngredientRepository(db),
            NutritionRepository(db),
        )
    }

    // ═══════════════════════════════════════════════════
    // T-01: 营养估算
    // ═══════════════════════════════════════════════════

    @Test
    fun `T01 营养估算_已知食材近似命中`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "黑毛猪五花"), ctx)
        assertEquals("黑毛猪五花", preview.inputName)
        assertNotNull(preview.nutrition.values, "应有营养估算值")
        assertTrue(preview.nutrition.values!!.energyKcal != null, "应有热量估算")
        assertEquals(CareFlag.PENDING_REVIEW, preview.careFlag, "新食材care应为PENDING")
    }

    @Test
    fun `T01 营养估算_有分类归属`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "大白菜"), ctx)
        assertEquals("蔬菜", preview.groupLabel, "大白菜应归蔬菜类")
        assertNotNull(preview.categoryId, "应有分类 id")
        assertNotNull(preview.nutrition.values, "大类兜底也应有预估值")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // T-03: unitId 非 null + 调料小剂量
    // ═══════════════════════════════════════════════════

    @Test
    fun `T03 单位默认克_unitId为gramUnit`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "盐"), ctx)
        assertEquals(ctx.gramUnitId, preview.unitId, "unitId 应为 gramUnit(g)，非 null")
        assertTrue(preview.quantity <= 10.0, "调料默认克数应≤10g，实际: ${preview.quantity}")
    }

    // ═══════════════════════════════════════════════════
    // T-04: careFlag
    // ═══════════════════════════════════════════════════

    @Test
    fun `T04 新建食材careFlag_PENDING`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "未知稀有食材XYZ"), ctx)
        assertEquals(ResolveKind.CREATE, preview.resolution)
        assertEquals(CareFlag.PENDING_REVIEW, preview.careFlag,
            "新建食材care必为PENDING_REVIEW·不自动断言忌口")
    }

    @Test
    fun `T04 归一命中库内_继承INHERITED`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "五花肉"), ctx)
        assertEquals(ResolveKind.REUSE, preview.resolution)
        assertEquals(CareFlag.INHERITED, preview.careFlag,
            "归一命中库内食材应继承care=INHERITED")
        assertNotNull(preview.existingId)
        Unit
    }

    // ═══════════════════════════════════════════════════
    // T-05: 别名归一
    // ═══════════════════════════════════════════════════

    @Test
    fun `T05 蕃茄归一为番茄_REUSE`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "蕃茄"), ctx)
        assertEquals("番茄", preview.normalizedName, "蕃茄应归一为番茄")
        assertEquals(ResolveKind.REUSE, preview.resolution, "应复用已有番茄")
    }

    @Test
    fun `T05 西红柿归一为番茄_REUSE`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "西红柿"), ctx)
        assertEquals("番茄", preview.normalizedName, "西红柿应归一为番茄")
        assertEquals(ResolveKind.REUSE, preview.resolution)
    }

    // ═══════════════════════════════════════════════════
    // T-07: preview 零写库
    // ═══════════════════════════════════════════════════

    @Test
    fun `T07 preview不写入DB`() = runBlocking {
        // A2 修复：用同一个 db 验 count，不再建第三个 DB-C
        val q = db.cookbookQueries
        val countBefore = q.selectAllIngredientNames().executeAsList().size

        repeat(3) {
            generator.preview(SemanticIngredient(name = "新奇食材${it}"), ctx)
        }

        val countAfter = q.selectAllIngredientNames().executeAsList().size
        assertEquals(countBefore, countAfter, "preview 不应写入任何食材到 DB")
    }

    // ═══════════════════════════════════════════════════
    // T-08: commit 真实落库验证（INV-05/06/07）
    // ═══════════════════════════════════════════════════

    @Test
    fun `T08 commit CREATE 建食材并验证source和营养ref`() = runBlocking {
        val preview = generator.preview(SemanticIngredient(name = "牛腩"), ctx)
        assertEquals(ResolveKind.CREATE, preview.resolution)

        val id = generator.commit(preview)
        assertTrue(id > 0, "commit 应返回有效食材 id")

        // INV-07：自动生成实体统一标记为 AI。
        val q = db.cookbookQueries
        val row = q.selectIngredientById(id).executeAsOneOrNull()
        assertNotNull(row, "食材应已写入 DB")
        assertEquals("ai", row.source, "自动生成食材 source 必须为 ai (INV-07)")

        // INV-05：ref="自动估算"（selectIngredientNutrition 按 ingredient_id 查）
        val nutrition = q.selectIngredientNutrition(id).executeAsOneOrNull()
        assertNotNull(nutrition, "营养应已写入 DB")
        assertEquals("自动估算", nutrition.ref, "营养 ref 必须为[自动估算] (INV-05)")
    }

    @Test
    fun `T08 commit REUSE 返回已有id不写新行`() = runBlocking {
        val q = db.cookbookQueries
        val countBefore = q.selectAllIngredientNames().executeAsList().size

        val preview = generator.preview(SemanticIngredient(name = "五花肉"), ctx)
        assertEquals(ResolveKind.REUSE, preview.resolution)

        val id = generator.commit(preview)
        assertEquals(preview.existingId, id, "REUSE 应返回已有食材 id")

        // 不应写入新行
        val countAfter = q.selectAllIngredientNames().executeAsList().size
        assertEquals(countBefore, countAfter, "REUSE commit 不应新增食材行")
    }
}
