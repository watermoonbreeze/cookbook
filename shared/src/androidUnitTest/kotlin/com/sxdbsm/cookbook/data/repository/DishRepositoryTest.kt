package com.sxdbsm.cookbook.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishStep
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.runBlocking

/**
 * @File : DishRepositoryTest
 * @Time : 2026/06/05
 * @Author : SXD-AI
 * @Desc : 菜品仓库单元测试
 * <p>
 * 覆盖菜品编辑读取、标签/烹饪方式回填、软删除后常规查询不可见等关键行为。
 * <p>
 * [AI生成] 补齐菜品核心 Repository 的基础回归测试。
 **/
class DishRepositoryTest {

    @Test
    fun saveDishThenLoadForEditReturnsFullDish() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)

        val dishId = repo.saveDish(
            id = 0,
            name = "酸辣土豆丝家常菜",
            cookingMethodId = null,
            cookingMethodNames = listOf("炒"),
            specialNote = "少油",
            description = "切丝后冲洗淀粉",
            imagePath = "/sdcard/cookbook/img/a.jpg",
            thumbnailPath = "/sdcard/cookbook/img/a_thum.jpg",
            tagNames = listOf("家常"),
            ingredients = emptyList(),
            steps = listOf(
                DishStep(text = "材料准备，土豆切丝", imagePath = "/sdcard/cookbook/img/step1.jpg", thumbnailPath = "/sdcard/cookbook/img/step1_t.jpg"),
                DishStep(text = "热锅冷油，下锅翻炒"),
            ),
        )

        val dish = repo.getDishById(dishId)

        assertNotNull(dish)
        assertEquals("酸辣土豆丝家常菜", dish.name)
        assertEquals(listOf("家常"), dish.tags)
        assertEquals(listOf("炒"), dish.cookingMethods.map { it.name })
        assertEquals("少油", dish.specialNote)
        assertEquals("/sdcard/cookbook/img/a_thum.jpg", dish.thumbnailPath)
        assertEquals(2, dish.steps.size)
        assertEquals("材料准备，土豆切丝", dish.steps[0].text)
        assertEquals("/sdcard/cookbook/img/step1_t.jpg", dish.steps[0].thumbnailPath)
        assertEquals("热锅冷油，下锅翻炒", dish.steps[1].text)
    }

    @Test
    fun updateDishReplacesSteps() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val dishId = repo.saveDish(
            id = 0,
            name = "步骤替换菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
            steps = listOf(DishStep(text = "旧步骤")),
        )

        repo.saveDish(
            id = dishId,
            name = "步骤替换菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
            steps = listOf(DishStep(text = "新步骤")),
        )

        val dish = repo.getDishById(dishId)

        assertNotNull(dish)
        assertEquals(listOf("新步骤"), dish.steps.map { it.text })
    }

    // [AI生成] 用户2026-07-22报 bug：加食材默认克数但 unit_id 落空(编辑器单位字典未就绪)→存 NULL→
    //   重载详情按 default_unit_id 回退显"100.0个"、营养按错单位折算。saveDish 应把空单位回填"克"。
    @Test
    fun saveDishBackfillsGramUnitWhenUnitIdNull() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val now = 0L
        // 生产真实单位名是"g"(迁移23把"克"→"g")：测主分支 selectMeasurementUnitIdByName("g")，非只测"克"降级兜底。
        q.insertMeasurementUnit("g", "preset", 1.0)
        val gramUnit = q.lastInsertId().executeAsOne()
        q.insertMeasurementUnit("个", "preset", null)
        val pieceUnit = q.lastInsertId().executeAsOne()
        // 食材默认单位=个(计件)，模拟"回退到食材计件单位显个"的场景。
        q.insertIngredient("青椒", "", "qingjiao", "", "", "🫑", pieceUnit, "preset", now)
        val qingjiaoId = q.lastInsertId().executeAsOne()

        val repo = DishRepository(db)
        // 加食材时单位字典未就绪→unitId=null，quantity=100(克)。
        val dishId = repo.saveDish(
            id = 0, name = "青椒炒肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = qingjiaoId, name = "青椒"), quantity = 100.0, unitId = null, unitName = "", isMain = false),
            ),
        )

        val dish = repo.getDishById(dishId)
        assertNotNull(dish)
        val di = dish.ingredients.first()
        assertEquals(gramUnit, di.unitId, "空单位应回填为'g'的id，不再是NULL")
        assertEquals("g", di.unitName, "重载单位名为'g'，不再回退成'个'/空")
    }

    // [AI生成] 回归网:锁住"营养不再被放大"(bug 用户可感面)——小剂量(≤20)调料丢单位曾被当"N个×piece"放大。
    @Test
    fun saveDishBackfillKeepsSmallQuantityNutritionUnamplified() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val now = 0L
        q.insertMeasurementUnit("g", "preset", 1.0)
        q.lastInsertId().executeAsOne()
        q.insertMeasurementUnit("个", "preset", null)
        val pieceUnit = q.lastInsertId().executeAsOne()
        // 盐(默认单位个·无 piece_gram)：钠 39000mg/100g。
        q.insertIngredient("盐", "", "yan", "", "", "🧂", pieceUnit, "preset", now)
        val saltId = q.lastInsertId().executeAsOne()
        q.upsertIngredientNutrition(
            ingredient_id = saltId, energy_kcal = 0.0, protein_g = 0.0, fat_g = 0.0, carb_g = 0.0,
            fiber_g = 0.0, sodium_mg = 39000.0, potassium_mg = 0.0, calcium_mg = 0.0, gi = null, purine_mg = 0.0,
            saturated_fat_g = 0.0, cholesterol_mg = 0.0, piece_gram = null, ref = "ref", review = 1L, updated_at = now,
        )

        val repo = DishRepository(db)
        // 加盐默认 3g，但单位字典未就绪→unitId=null。回填"g"前:3g 被当"3个×DEFAULT_PIECE_GRAM(60)=180g"→钠放大60倍。
        val dishId = repo.saveDish(
            id = 0, name = "少盐菜", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = saltId, name = "盐"), quantity = 3.0, unitId = null, unitName = "", isMain = false),
            ),
        )

        val nutrition = NutritionRepository(db).dishNutrition(listOf(dishId))[dishId]
        assertNotNull(nutrition)
        // 3g × 39000/100 = 1170mg 钠(正确)，而非 180g × 390 = 70200mg(放大60倍)。
        assertEquals(1170.0, nutrition.totals.sodiumMg, 1.0, "小剂量回填克后钠按3g算(1170mg)，不再放大")
    }

    // [AI生成] 用户显式选的单位(如"个")不被回填篡改——只兜 null，不覆盖已选。
    @Test
    fun saveDishKeepsExplicitNonGramUnit() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val q = db.cookbookQueries
        val now = 0L
        q.insertMeasurementUnit("克", "preset", 1.0)
        q.lastInsertId().executeAsOne()
        q.insertMeasurementUnit("个", "preset", null)
        val pieceUnit = q.lastInsertId().executeAsOne()
        q.insertIngredient("鸡蛋", "", "jidan", "", "", "🥚", pieceUnit, "preset", now)
        val eggId = q.lastInsertId().executeAsOne()

        val repo = DishRepository(db)
        val dishId = repo.saveDish(
            id = 0, name = "水煮蛋", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = eggId, name = "鸡蛋"), quantity = 2.0, unitId = pieceUnit, unitName = "个", isMain = true),
            ),
        )

        val di = repo.getDishById(dishId)!!.ingredients.first()
        assertEquals(pieceUnit, di.unitId, "用户显式选的'个'不被回填改成'克'")
        assertEquals("个", di.unitName)
    }

    @Test
    fun deleteDishMarksDishInvalidForNormalQueries() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val repo = DishRepository(db)
        val dishId = repo.saveDish(
            id = 0,
            name = "待删除菜品",
            cookingMethodId = null,
            specialNote = "",
            description = "",
            imagePath = "",
            thumbnailPath = "",
            tagNames = emptyList(),
            ingredients = emptyList(),
        )

        repo.deleteDish(dishId)

        assertNull(repo.getDishById(dishId))
        assertEquals(emptyList(), repo.searchDishes("待删除菜品"))
    }
}
