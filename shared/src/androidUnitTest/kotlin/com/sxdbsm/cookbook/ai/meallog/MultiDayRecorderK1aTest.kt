package com.sxdbsm.cookbook.ai.meallog

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.NutritionRepository
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver
import com.sxdbsm.cookbook.domain.autogen.ResolveKind
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : MultiDayRecorderK1aTest
 * @Time : 2026/08/08
 * @Author : SXD-AI
 * @Desc : MultiDayRecorder.previewAll REUSE 菜品批量营养回填单测（K1a 批次）——T-K1A-02/03
 * <p>
 * 覆盖 INV-K1A-02：
 * - T-K1A-02：1 个 REUSE 菜 → 回填的 nutrition 等于 dishNutrition(id) 真实值（单菜正确性）
 * - T-K1A-03：3 个 REUSE 菜 → 对单次 previewAll()，selectNutritionInputsByDishIds 恰好执行 1 次（零 N+1）。
 *   用 CountingSqlDriver 在 driver 层数批量查询执行次数——比数 repo 方法调用更严格（连 SQL 是否被拆多次都能抓）。
 * <p>
 * [AI生成] K1a 营养展示统一化批次。
 **/
class MultiDayRecorderK1aTest {

    private lateinit var db: CookbookDatabase
    private lateinit var counting: CountingSqlDriver
    private lateinit var recorder: MultiDayRecorder
    private var gramUnitId = 0L
    private var porkId = 0L

    @BeforeTest
    fun setUp() {
        val real = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        counting = CountingSqlDriver(real)
        CookbookDatabase.Schema.create(counting)
        db = CookbookDatabase(counting)
        val q = db.cookbookQueries

        // 单位：g（克当量 1.0）
        q.insertMeasurementUnit("g", "preset", 1.0)
        gramUnitId = q.selectMeasurementUnitIdByName("g").executeAsOne()

        // 餐次
        q.insertMealType("BREAKFAST", "早餐", "07:30", 1, "preset")
        q.insertMealType("LUNCH", "午餐", "12:00", 1, "preset")
        q.insertMealType("DINNER", "晚餐", "18:00", 1, "preset")

        // 分类
        val catNames = listOf(
            "谷薯主食类", "蔬菜类", "菌藻类", "水果类", "水产类",
            "畜禽肉类", "蛋类", "奶类", "大豆及坚果",
        )
        catNames.forEachIndexed { i, cn ->
            q.insertFoodCategory(cn, "general", null, null, (i + 1).toLong(), "", "preset", 0)
        }

        // 食材：五花肉（带营养 508 kcal / 7.7 蛋白 / 53 脂肪）
        q.insertIngredient("五花肉", "", "wuhuarou", "", "", "🥩", 1, "preset", 0)
        porkId = q.lastInsertId().executeAsOne()
        q.upsertIngredientNutrition(
            porkId, 508.0, 7.7, 53.0, 0.0, null, 79.0, null, null, null, 100.0,
            null, null, null, "中国食物成分表", 1, 0,
        )

        val aliasResolver = IngredientAliasResolver.fromJson("{}")
        recorder = MultiDayRecorder(
            ingredientRepo = IngredientRepository(db),
            dishRepo = DishRepository(db),
            mealRepo = MealRecordRepository(db),
            nutritionRepo = NutritionRepository(db),
            aliasResolver = aliasResolver,
            db = db,
        )
    }

    private fun seedDish(name: String): Long {
        val q = db.cookbookQueries
        q.insertDish(
            name = name,
            cooking_method_id = null,
            special_note = "",
            description = "",
            image_path = "",
            thumbnail_path = "",
            source = "preset",
            created_at = 0L,
            updated_at = 0L,
            cuisine = "家常菜",
        )
        val dishId = q.lastInsertId().executeAsOne()
        q.insertDishIngredient(dish_id = dishId, ingredient_id = porkId, quantity = 100.0, unit_id = gramUnitId, is_main = 1)
        return dishId
    }

    private fun dayWith(vararg dishNames: String): DayMealJson = DayMealJson(
        date = "2026-08-08",
        meals = listOf(
            MealJson(
                meal_type = "LUNCH",
                meal_time = "12:00",
                dishes = dishNames.map { MealDishRefJson(name = it, ref = it) },
            ),
        ),
    )

    @Test
    fun `T-K1A-02 REUSE菜nutrition回填等于dishNutrition真实值`() = runBlocking {
        val dishId = seedDish("红烧肉")
        val preview = recorder.previewAll(listOf(dayWith("红烧肉")), LocalDate(2026, 8, 8))

        val dish = preview.days.single().meals.single().dishes.single()
        assertEquals(ResolveKind.REUSE, dish.resolution)
        assertEquals(dishId, dish.existingId)

        val expected = NutritionRepository(db).dishNutrition(listOf(dishId))[dishId]
        assertNotNull(expected, "seed 的菜应能算出营养")
        val actual = assertNotNull(dish.nutrition, "REUSE 菜必须被回填非空 nutrition（INV-K1A-02）")
        assertEquals(expected, actual, "回填的 nutrition 应等于 dishNutrition(id) 的批量查询值")
        assertTrue(actual.totals.energyKcal > 0, "五花肉 100g 应产出热量>0")
        Unit
    }

    @Test
    fun `T-K1A-03 三个REUSE菜_previewAll一次只执行一次批量营养查询`() = runBlocking {
        seedDish("红烧肉")
        seedDish("清蒸鲈鱼")
        seedDish("清炒菠菜")
        counting.resetCount()

        val preview = recorder.previewAll(
            listOf(dayWith("红烧肉", "清蒸鲈鱼", "清炒菠菜")),
            LocalDate(2026, 8, 8),
        )

        assertEquals(3, preview.days.single().meals.single().dishes.size)
        // 三个 REUSE 菜全部回填非空营养（证明批量查询覆盖了全部 3 个 id）
        preview.days.single().meals.single().dishes.forEach { d ->
            assertEquals(ResolveKind.REUSE, d.resolution)
            assertNotNull(d.nutrition, "每个 REUSE 菜都应被回填营养")
        }
        assertEquals(1, counting.selectNutritionInputsCount, "单次 previewAll 内批量营养查询必须恰好 1 次（零 N+1·INV-K1A-02）")
        Unit
    }

    @Test
    fun `T-B7-03 DayMealJson的steps经toSemanticDay与commit端到端落库`() = runBlocking {
        val day = DayMealJson(
            date = "2026-08-08",
            meals = listOf(
                MealJson(
                    meal_type = "lunch", // 故意用小写，贴近流式路径真实值
                    meal_time = "12:00",
                    dishes = listOf(
                        MealDishRefJson(
                            name = "番茄炒蛋",
                            dish = DishJson(
                                name = "番茄炒蛋",
                                cooking_methods = listOf("炒"),
                                steps = listOf("打散鸡蛋", "下锅翻炒", "加番茄"),
                                ingredients = listOf(DishIngredientJson(food = FoodJson(name = "番茄"), quantity = 100.0)),
                            ),
                        ),
                    ),
                ),
            ),
        )

        val preview = recorder.previewAll(listOf(day), LocalDate(2026, 8, 8))
        val dish = preview.days.single().meals.single().dishes.single()
        assertEquals(ResolveKind.CREATE, dish.resolution)
        assertEquals(
            listOf("打散鸡蛋", "下锅翻炒", "加番茄"), dish.steps,
            "toSemanticDay 必须把 DishJson.steps 透传进 SemanticDish→DishPreview",
        )

        recorder.commitPreview(preview)
        val repo = DishRepository(db)
        val savedId = assertNotNull(repo.dishIdByName("番茄炒蛋"))
        val saved = assertNotNull(repo.getDishById(savedId))
        assertEquals(listOf("打散鸡蛋", "下锅翻炒", "加番茄"), saved.steps.map { it.text })
        assertEquals(setOf("炒"), saved.cookingMethods.map { it.name }.toSet())
        Unit
    }
}

/**
 * 包装真实 driver，数 `selectNutritionInputsByDishIds`（SQL 含 FROM dish_ingredient 且 IN）的执行次数。[AI生成]
 */
private class CountingSqlDriver(private val delegate: SqlDriver) : SqlDriver by delegate {
    var selectNutritionInputsCount = 0
        private set

    fun resetCount() {
        selectNutritionInputsCount = 0
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?,
    ): QueryResult<R> {
        if (sql.contains("FROM dish_ingredient") && sql.contains("IN")) {
            selectNutritionInputsCount++
        }
        return delegate.executeQuery(identifier, sql, mapper, parameters, binders)
    }
}
