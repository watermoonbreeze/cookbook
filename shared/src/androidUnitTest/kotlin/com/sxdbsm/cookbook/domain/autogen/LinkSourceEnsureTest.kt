package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.data.seed.SeedResourceLoader
import com.sxdbsm.cookbook.db.CookbookDatabase
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : LinkSourceEnsureTest
 * @Time : 2026/09/04
 * @Author : SXD-AI
 * @Desc : ensureCreated(source="link") 单测（L4 分享链接·T-L4-12·真库）——链接导入食材的来源标记
 * <p>
 * ⚠️ 合同缺口如实说明（未自行改生产代码·已上报）：
 * 蓝图 T-L4-12 还要求"food_group 非空+营养行存在"，但 `FoodGroup.classify("冰草")` 返回 null
 * （无 NAME_OVERRIDE 特例、无 菜/苗 尾词、无关键词命中）→ preview.group=null → commit 里
 * food_group 写入与营养大类兜底两处均被"仅非空才写"守卫跳过，这两条子断言对"冰草"**不可达**。
 * 本文件按"不猜测/不改生产代码"红线只断言可达部分（id>0 + source=='link'），另补一条可归类名
 * （冰菜·即冰草的别名，尾词"菜"→蔬菜类）锚住管线的完整行为（分类挂载/food_group/营养行）。
 * <p>
 * [AI生成] 实施蓝图 `feature/分享链接解析_实施蓝图.md` §8 测试矩阵 shared 部分。
 **/
class LinkSourceEnsureTest {

    private lateinit var db: CookbookDatabase
    private lateinit var ctx: AutoGenContext
    private lateinit var autoGen: IngredientAutoGenerator

    @BeforeTest
    fun setUp() = runBlocking {
        db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries

        // —— 以下种库完全模仿 IngredientAutoGeneratorTest.setUp ——
        // 种入测量单位
        q.insertMeasurementUnit("g", "preset", 1.0)
        q.insertMeasurementUnit("个", "preset", null)
        q.insertMeasurementUnit("ml", "preset", 1.0)

        // 种入餐次
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        q.insertMealType("DINNER", "晚餐", "18:00", 1, "preset")

        // 种入食物分类
        val catNames = listOf(
            "谷薯主食类", "蔬菜类", "菌藻类", "水果类", "水产类",
            "畜禽肉类", "蛋类", "奶类", "大豆及坚果",
        )
        catNames.forEachIndexed { i, cn ->
            q.insertFoodCategory(cn, "general", null, null, (i + 1).toLong(), "", "preset", 0)
        }
        q.insertFoodCategory("调味品", "general", null, null, 99, "", "preset", 0)

        // 种入五花肉（含营养，供近似命中候选）
        q.insertIngredient("五花肉", "", "wuhuarou", "", "", "🥩", 1, "preset", 0)
        val porkId = q.lastInsertId().executeAsOne()
        val meatCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "畜禽肉类" }.id
        q.linkIngredientCategory(porkId, meatCatId)
        q.upsertIngredientNutrition(
            porkId, 508.0, 7.7, 53.0, 0.0, null, 79.0, null, null, null, 100.0,
            null, null, null, "中国食物成分表", 1, 0,
        )

        // 别名表走生产同路径（seed/ingredient_aliases.json）
        val aliasJson = SeedResourceLoader.readText("seed/ingredient_aliases.json") ?: "{}"
        ctx = AutoGenContext.load(db, IngredientAliasResolver.fromJson(aliasJson))
        autoGen = IngredientAutoGenerator(
            IngredientRepository(db),
            NutritionRepository(db),
        )
    }

    @Test
    fun `T-L4-12 ensureCreated_link源入库_source标记`() = runBlocking {
        val id = autoGen.ensureCreated("冰草", ctx, source = "link")
        assertTrue(id > 0, "ensureCreated 应返回有效食材 id")
        val row = db.cookbookQueries.selectIngredientById(id).executeAsOneOrNull()
        assertNotNull(row, "食材应已写入 DB")
        assertEquals("link", row.source, "链接导入的食材 source 必须为 link")
        Unit
    }

    @Test
    fun `T-L4-12补 可归类名走完整autogen管线_分类挂载_food_group_营养行`() = runBlocking {
        // 合同冻结名"冰草"的 food_group/营养子断言不可达（见类头说明）；
        // 此处用同类可归类名"冰菜"（冰草别名·尾词"菜"→蔬菜）锚住管线完整行为。
        val id = autoGen.ensureCreated("冰菜", ctx, source = "link")
        assertTrue(id > 0, "ensureCreated 应返回有效食材 id")

        val row = db.cookbookQueries.selectIngredientById(id).executeAsOneOrNull()
        assertNotNull(row)
        assertEquals("link", row.source, "source 仍应标记 link")

        // 分类挂载非空（蓝图 food_category 断言在可归类名下成立）
        val categoryIds = db.cookbookQueries.selectCategoryIdsByIngredient(id).executeAsList()
        assertTrue(categoryIds.isNotEmpty(), "可归类名应挂载 food_category（蔬菜类）")

        // food_group 列非空
        assertEquals(
            "VEGETABLE",
            IngredientRepository(db).foodGroupByName()["冰菜"],
            "food_group 应写入 VEGETABLE",
        )

        // ingredient_nutrition 有该 id 的行
        val nutrition = NutritionRepository(db).ingredientNutrition(id)
        assertNotNull(nutrition, "应写入营养行（蔬菜大类均值兜底）")
        Unit
    }
}
