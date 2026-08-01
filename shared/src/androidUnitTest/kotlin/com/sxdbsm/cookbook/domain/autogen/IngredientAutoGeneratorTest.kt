package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : IngredientAutoGeneratorTest
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 食材自动生成器单测——T-01(营养估算)/T-04(careFlag)/T-05(别名归一)/T-07(preview零写库)
 * <p>
 * [AI生成] 自动化基础能力层 Phase 1。
 **/
class IngredientAutoGeneratorTest {

    /**
     * 构建 AutoGenContext：种子测量单位/餐次/分类/预置食材(五花肉+营养)/番茄(别名目标)。
     */
    private fun createContext(): AutoGenContext = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries

        // insertMeasurementUnit(name, source, grams)
        q.insertMeasurementUnit("g", "preset", 1.0)
        q.insertMeasurementUnit("个", "preset", null)
        q.insertMeasurementUnit("ml", "preset", 1.0)

        // insertMealType(code, name, default_time, is_fixed, source)
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        q.insertMealType("DINNER", "晚餐", "18:00", 1, "preset")

        // insertFoodCategory(name, dimension, parent_id, crowd_type_id, sort_order, icon, source, created_at)
        val catNames = listOf(
            "谷薯主食类", "蔬菜类", "菌藻类", "水果类", "水产类",
            "畜禽肉类", "蛋类", "奶类", "大豆及坚果",
        )
        catNames.forEachIndexed { i, cn ->
            q.insertFoodCategory(cn, "general", null, null, (i + 1).toLong(), "", "preset", 0)
        }
        q.insertFoodCategory("调味品", "general", null, null, 99, "", "preset", 0)

        // insertIngredient(name, alias, pinyin, image_path, thumbnail_path, emoji, default_unit_id, source, created_at)
        q.insertIngredient("五花肉", "", "wuhuarou", "", "", "🥩", 1, "preset", 0)
        val porkId = q.lastInsertId().executeAsOne()
        val meatCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "畜禽肉类" }.id
        q.linkIngredientCategory(porkId, meatCatId)

        // upsertIngredientNutrition(ingredient_id, energy_kcal, protein_g, fat_g, carb_g, fiber_g, sodium_mg, potassium_mg, calcium_mg, gi, purine_mg, saturated_fat_g, cholesterol_mg, piece_gram, ref, review, updated_at)
        q.upsertIngredientNutrition(
            porkId, 508.0, 7.7, 53.0, 0.0, null, 79.0, null, null, null, 100.0,
            null, null, null, "中国食物成分表", 1, 0,
        )

        // 番茄（别名归一目标）
        q.insertIngredient("番茄", "", "fanqie", "", "", "🍅", 1, "preset", 0)
        val tomatoId = q.lastInsertId().executeAsOne()
        val vegCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "蔬菜类" }.id
        q.linkIngredientCategory(tomatoId, vegCatId)

        val aliasJson = """{"蕃茄":"番茄","西红柿":"番茄"}"""
        val aliasResolver = IngredientAliasResolver.fromJson(aliasJson)

        AutoGenContext.load(db, aliasResolver)
    }

    private fun createGenerator(): IngredientAutoGenerator {
        val db = RepositoryTestDatabase.create()
        return IngredientAutoGenerator(
            IngredientRepository(db),
            NutritionRepository(db),
        )
    }

    // ═══════════════════════════════════════════════════
    // T-01: 营养估算
    // ═══════════════════════════════════════════════════

    @Test
    fun `T01 营养估算_已知食材近似命中`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "黑毛猪五花"), ctx)
        assertEquals("黑毛猪五花", preview.inputName)
        assertNotNull(preview.nutrition.values, "应有营养估算值")
        assertTrue(preview.nutrition.values!!.energyKcal != null, "应有热量估算")
        assertEquals(CareFlag.PENDING_REVIEW, preview.careFlag, "新食材care应为PENDING")
    }

    @Test
    fun `T01 营养估算_有分类归属`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "大白菜"), ctx)
        assertEquals("蔬菜", preview.groupLabel, "大白菜应归蔬菜类")
        assertNotNull(preview.categoryId, "应有分类 id")
        assertNotNull(preview.nutrition.values, "大类兜底也应有预估值")
        Unit
    }

    // ═══════════════════════════════════════════════════
    // T-04: careFlag
    // ═══════════════════════════════════════════════════

    @Test
    fun `T04 新建食材careFlag_PENDING`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "未知稀有食材XYZ"), ctx)
        assertEquals(ResolveKind.CREATE, preview.resolution)
        assertEquals(CareFlag.PENDING_REVIEW, preview.careFlag,
            "新建食材care必为PENDING_REVIEW·不自动断言忌口")
    }

    @Test
    fun `T04 归一命中库内_继承INHERITED`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

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
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "蕃茄"), ctx)
        assertEquals("番茄", preview.normalizedName, "蕃茄应归一为番茄")
        assertEquals(ResolveKind.REUSE, preview.resolution, "应复用已有番茄")
    }

    @Test
    fun `T05 西红柿归一为番茄_REUSE`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "西红柿"), ctx)
        assertEquals("番茄", preview.normalizedName, "西红柿应归一为番茄")
        assertEquals(ResolveKind.REUSE, preview.resolution)
    }

    // ═══════════════════════════════════════════════════
    // T-07: preview零写库
    // ═══════════════════════════════════════════════════

    @Test
    fun `T07 preview不写入DB`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries

        val countBefore = q.selectAllIngredientNames().executeAsList().size

        repeat(3) {
            generator.preview(SemanticIngredient(name = "新奇食材${it}"), ctx)
        }

        val countAfter = q.selectAllIngredientNames().executeAsList().size
        assertEquals(countBefore, countAfter, "preview 不应写入任何食材到 DB")
    }

    // ═══════════════════════════════════════════════════
    // T-03: unitId非null + 小剂量不放大
    // ═══════════════════════════════════════════════════

    @Test
    fun `T03 单位默认克_unitId为gramUnit`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "盐"), ctx)
        assertEquals(ctx.gramUnitId, preview.unitId, "unitId 应为 gramUnit(g)，非 null")
        assertTrue(preview.quantity <= 10.0, "调料默认克数应≤10g，实际: ${preview.quantity}")
    }

    // ═══════════════════════════════════════════════════
    // commit 验证
    // ═══════════════════════════════════════════════════

    @Test
    fun `commit CREATE 建食材+营养`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "牛腩"), ctx)
        assertEquals(ResolveKind.CREATE, preview.resolution)

        val id = generator.commit(preview)
        assertTrue(id > 0, "commit 应返回有效食材 id")
    }

    @Test
    fun `commit REUSE 返回已有id`() = runBlocking {
        val ctx = createContext()
        val generator = createGenerator()

        val preview = generator.preview(SemanticIngredient(name = "五花肉"), ctx)
        assertEquals(ResolveKind.REUSE, preview.resolution)
        val id = generator.commit(preview)
        assertEquals(preview.existingId, id, "REUSE 应返回已有食材 id")
    }
}
