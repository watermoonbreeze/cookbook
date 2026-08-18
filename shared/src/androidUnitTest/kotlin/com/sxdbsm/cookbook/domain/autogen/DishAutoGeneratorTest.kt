package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.model.IngredientNutrition
import com.sxdbsm.cookbook.domain.model.NutritionCalculator
import com.sxdbsm.cookbook.domain.model.NutritionInput
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : DishAutoGeneratorTest
 * @Time : 2026/08/08
 * @Author : SXD-AI
 * @Desc : 菜品级自动生成单测（K1a 营养统一化批次）——T-K1A-01a/b/c/d
 * <p>
 * 覆盖 INV-K1A-01/04/05：
 * - a：CREATE 菜含 2 个有营养 Match 食材 → dishNutrition() 计算与手工一致、estimated=false
 * - b：CREATE 菜全部食材无营养数据 → nutrition!=null && hasData==false（不是 null，INV-K1A-04 ②类）
 * - c：菜名为空 → nutrition==null（早退分支，INV-K1A-04 ①类）
 * - d：CREATE 菜含 1 个 Group 均值食材 → estimated==true（INV-K1A-05，GC-37 挑战 #6）
 * <p>
 * [AI生成] K1a 营养展示统一化批次。
 **/
class DishAutoGeneratorTest {

    private lateinit var db: CookbookDatabase
    private lateinit var ctx: AutoGenContext
    private lateinit var generator: DishAutoGenerator

    @BeforeTest
    fun setUp() = runBlocking {
        db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries

        // 种入测量单位（克当量：g=1.0）
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

        // 种入五花肉（带营养·近似命中 Match）
        q.insertIngredient("五花肉", "", "wuhuarou", "", "", "🥩", 1, "preset", 0)
        val porkId = q.lastInsertId().executeAsOne()
        val meatCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "畜禽肉类" }.id
        q.linkIngredientCategory(porkId, meatCatId)
        q.upsertIngredientNutrition(
            porkId, 508.0, 7.7, 53.0, 0.0, null, 79.0, null, null, null, 100.0,
            null, null, null, "中国食物成分表", 1, 0,
        )

        // 种入番茄（带营养·近似命中 Match）
        q.insertIngredient("番茄", "", "fanqie", "", "", "🍅", 1, "preset", 0)
        val tomatoId = q.lastInsertId().executeAsOne()
        val vegCatId = q.selectAllFoodCategories().executeAsList().first { it.name == "蔬菜类" }.id
        q.linkIngredientCategory(tomatoId, vegCatId)
        q.upsertIngredientNutrition(
            tomatoId, 20.0, 1.0, 0.2, 4.0, null, null, null, null, null, null,
            null, null, null, "中国食物成分表", 1, 0,
        )

        val aliasResolver = IngredientAliasResolver.fromJson("{}")
        ctx = AutoGenContext.load(db, aliasResolver)
        val ingredientGen = IngredientAutoGenerator(IngredientRepository(db), NutritionRepository(db))
        generator = DishAutoGenerator(DishRepository(db), ingredientGen)
    }

    @Test
    fun `T-K1A-01a CREATE菜含2个有营养Match食材_nutrition与手工一致且estimated=false`() = runBlocking {
        val preview = generator.preview(
            SemanticDish(
                name = "红烧肉",
                ingredients = listOf(
                    SemanticIngredient(name = "五花肉", quantity = 100.0),
                    SemanticIngredient(name = "番茄", quantity = 50.0),
                ),
            ),
            ctx,
        )
        assertEquals(ResolveKind.CREATE, preview.resolution)
        val n = assertNotNull(preview.nutrition, "CREATE 菜应产出非空 DishNutrition")
        assertTrue(n.hasData, "两个 Match 食材都该有营养数据")

        // 手工独立期望：与 preview 相同的 quantity × 每100g 营养（unitGrams=1.0 即按克直取）
        val expected = NutritionCalculator.dishNutrition(
            listOf(
                NutritionInput(quantity = 100.0, unitGrams = 1.0, nutrition = ingredient(508.0, 7.7, 53.0, 0.0)),
                NutritionInput(quantity = 50.0, unitGrams = 1.0, nutrition = ingredient(20.0, 1.0, 0.2, 4.0)),
            ),
        )
        assertEquals(expected.totals.energyKcal, n.totals.energyKcal, 0.01, "热量应与独立计算一致")
        assertEquals(expected.totals.proteinG, n.totals.proteinG, 0.01, "蛋白应与独立计算一致")
        assertEquals(expected.totals.fatG, n.totals.fatG, 0.01, "脂肪应与独立计算一致")
        assertEquals(expected.totals.carbG, n.totals.carbG, 0.01, "碳水应与独立计算一致")
        assertTrue(n.totals.proteinG > 0 && n.totals.fatG > 0, "蛋白/脂肪/碳水都应>0")
        assertFalse(n.estimated, "全 Match 食材不应标估算")
        Unit
    }

    @Test
    fun `T-K1A-01b CREATE菜全部食材无营养_nutrition非空hasData=false`() = runBlocking {
        // 花生油 endsWith("油")→classify null→无候选命中→source=None→values=null
        val preview = generator.preview(
            SemanticDish(
                name = "神秘汤",
                ingredients = listOf(SemanticIngredient(name = "花生油", quantity = 5.0)),
            ),
            ctx,
        )
        val n = assertNotNull(preview.nutrition, "尝试计算但无数据：nutrition 不应为 null（INV-K1A-04 ②类）")
        assertFalse(n.hasData, "全部食材无营养数据 → hasData 应 false")
        assertNull(
            preview.ingredients.firstOrNull()?.nutrition?.values,
            "花生油无候选/无大类 → values 应 null（source=None）",
        )
        Unit
    }

    @Test
    fun `T-K1A-01c 菜名为空_nutrition为null`() = runBlocking {
        val preview = generator.preview(SemanticDish(name = "   "), ctx)
        assertNull(preview.nutrition, "空菜名早退分支：从未尝试计算 → nutrition 应为 null（INV-K1A-04 ①类）")
        Unit
    }

    @Test
    fun `T-B7-01 CREATE菜commit后做法烹饪方式标签描述特别说明全部落库`() = runBlocking {
        val preview = generator.preview(
            SemanticDish(
                name = "红烧排骨",
                ingredients = listOf(SemanticIngredient(name = "五花肉", quantity = 100.0)),
                cookingMethods = listOf("炖", "焖"),
                tags = listOf("下饭菜"),
                description = "经典家常做法",
                specialNote = "小火慢炖",
                steps = listOf("焯水去腥", "炒糖色", "加水炖煮40分钟"),
            ),
            ctx,
        )
        // preview 阶段必须原样透传（此前 bug：preview() 就已丢弃这些字段）
        assertEquals(listOf("炖", "焖"), preview.cookingMethods)
        assertEquals(listOf("下饭菜"), preview.tags)
        assertEquals("经典家常做法", preview.description)
        assertEquals("小火慢炖", preview.specialNote)
        assertEquals(listOf("焯水去腥", "炒糖色", "加水炖煮40分钟"), preview.steps)

        val dishId = generator.commit(preview)
        val saved = assertNotNull(DishRepository(db).getDishById(dishId), "commit 后应能查到刚建的菜")
        assertEquals(setOf("炖", "焖"), saved.cookingMethods.map { it.name }.toSet(), "烹饪方式应落库（此前 bug：commit() 写死 emptyList）")
        assertEquals(listOf("下饭菜"), saved.tags)
        assertEquals("经典家常做法", saved.description)
        assertEquals("小火慢炖", saved.specialNote)
        assertEquals(listOf("焯水去腥", "炒糖色", "加水炖煮40分钟"), saved.steps.map { it.text })
        Unit
    }

    @Test
    fun `T-K1A-01d CREATE菜含Group均值食材_estimated=true`() = runBlocking {
        // 大白菜不在 nutritionCandidates（未种营养）→ classify 蔬菜 → GROUP_AVG 均值 → source=Group
        val preview = generator.preview(
            SemanticDish(
                name = "清炒大白菜",
                ingredients = listOf(SemanticIngredient(name = "大白菜", quantity = 100.0)),
            ),
            ctx,
        )
        assertEquals(ResolveKind.CREATE, preview.resolution)
        val n = assertNotNull(preview.nutrition)
        assertTrue(n.hasData, "Group 均值有值 → 应有营养数据")
        assertTrue(n.estimated, "含 Group 均值食材 → 必须标估算（INV-K1A-05）")
        Unit
    }

    private fun ingredient(
        energyKcal: Double, proteinG: Double, fatG: Double, carbG: Double,
    ): IngredientNutrition = IngredientNutrition(
        ingredientId = 0L, // 占位；NutritionCalculator.dishNutrition() 不读取
        energyKcal = energyKcal, proteinG = proteinG, fatG = fatG, carbG = carbG,
    )
}
