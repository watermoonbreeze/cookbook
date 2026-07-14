package com.sxdbsm.cookbook.flow

import com.sxdbsm.cookbook.ai.HealthRuleEngine
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.ai.model.RecommendMode
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishStep
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : FlowIntegrationTest
 * @Time : 2026/07/13
 * @Author : SXD-AI
 * @Desc : 食材/菜品/餐食/推荐 全流程集成测试（正常流程 + 异常但符合人类操作的流程）
 * <p>
 * 走真实 Repository + 内存库，覆盖"加餐时创建菜品、添加食材、创建食材"等端到端链路，
 * 以及重名复用、删引用、软删恢复、库存推荐、份数结算等边界。
 * <p>
 * [AI生成] 重构/测试加强：把主流程与人类常见异常操作固化为回归。
 **/
class FlowIntegrationTest {

    private fun newRepos(db: com.sxdbsm.cookbook.db.CookbookDatabase) = Repos(
        ing = IngredientRepository(db),
        dish = DishRepository(db),
        meal = MealRecordRepository(db),
        pantry = PantryRepository(db),
    )

    private data class Repos(
        val ing: IngredientRepository,
        val dish: DishRepository,
        val meal: MealRecordRepository,
        val pantry: PantryRepository,
    )

    /** 造一个餐次并返回其 id。[AI生成] */
    private fun mealTypeId(db: com.sxdbsm.cookbook.db.CookbookDatabase, code: String, name: String, time: String): Long {
        db.cookbookQueries.insertMealType(code, name, time, 1, "preset")
        return db.cookbookQueries.lastInsertId().executeAsOne()
    }

    // ============ 正常主流程 ============

    @Test
    fun `全流程_创建食材_建菜含食材步骤标签_加餐_读取校验`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)

        // 1) 创建自建食材
        val potato = r.ing.createUserIngredient("土豆")
        val pork = r.ing.createUserIngredient("猪肉")
        assertTrue(potato > 0 && pork > 0)

        // 2) 创建菜品(带主料/辅料、步骤、标签、烹饪方式)
        val dishId = r.dish.saveDish(
            id = 0, name = "土豆炒肉", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "少油", description = "家常", imagePath = "", thumbnailPath = "", tagNames = listOf("家常", "下饭"),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = potato, name = "土豆"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = pork, name = "猪肉"), isMain = true),
            ),
            steps = listOf(DishStep(text = "土豆切丝"), DishStep(text = "热锅下肉翻炒")),
        )
        val dish = r.dish.getDishById(dishId)
        assertNotNull(dish)
        assertEquals("土豆炒肉", dish.name)
        assertEquals(setOf("家常", "下饭"), dish.tags.toSet())
        assertEquals(2, dish.ingredients.size)
        assertEquals(2, dish.steps.size)

        // 3) 加餐食：早餐带这道菜
        val breakfast = mealTypeId(db, "BREAKFAST", "早餐", "07:30")
        val date = LocalDate(2026, 7, 13)
        r.meal.saveDayMeals(date, listOf(DayMealDraft(breakfast, LocalTime(7, 30), "", listOf(dishId))))

        // 4) 读取校验
        val loaded = r.meal.loadDayMealsForEdit(date)
        assertEquals(1, loaded.size)
        assertEquals(listOf(dishId), loaded.first().dishes.map { it.id })
    }

    @Test
    fun `DishMini_mainIngredientNames_被正确填充`() = runBlocking {
        // 回归守护：mainIngredientNames 曾恒空，导致分类图标/主食判定失效。
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val rice = r.ing.createUserIngredient("大米")
        val dishId = r.dish.saveDish(
            id = 0, name = "米饭", cookingMethodId = null, cookingMethodNames = listOf("煮"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = rice, name = "大米"), isMain = true)),
        )
        val mini = r.dish.getDishMiniById(dishId)
        assertNotNull(mini)
        assertEquals(listOf("大米"), mini.mainIngredientNames)
    }

    @Test
    fun `编辑菜品_替换食材与步骤`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val a = r.ing.createUserIngredient("青椒")
        val b = r.ing.createUserIngredient("茄子")
        val dishId = r.dish.saveDish(
            id = 0, name = "可变菜", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = a, name = "青椒"), isMain = true)),
            steps = listOf(DishStep(text = "旧步骤")),
        )
        // 编辑：换食材、换步骤
        r.dish.saveDish(
            id = dishId, name = "可变菜", cookingMethodId = null, cookingMethodNames = listOf("焖"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = b, name = "茄子"), isMain = true)),
            steps = listOf(DishStep(text = "新步骤1"), DishStep(text = "新步骤2")),
        )
        val dish = r.dish.getDishById(dishId)!!
        assertEquals(listOf("茄子"), dish.ingredients.map { it.ingredient.name })
        assertEquals(2, dish.steps.size)
        assertEquals("新步骤1", dish.steps.first().text)
    }

    @Test
    fun `餐食_多餐次保存_编辑替换_空列表清空`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val d1 = r.dish.saveDish(id = 0, name = "菜1", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())
        val d2 = r.dish.saveDish(id = 0, name = "菜2", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())
        val bf = mealTypeId(db, "BREAKFAST", "早餐", "07:30")
        val dn = mealTypeId(db, "DINNER", "晚餐", "18:30")
        val date = LocalDate(2026, 7, 13)
        r.meal.saveDayMeals(date, listOf(
            DayMealDraft(bf, LocalTime(7, 30), "", listOf(d1)),
            DayMealDraft(dn, LocalTime(18, 30), "", listOf(d1, d2)),
        ))
        assertEquals(2, r.meal.loadDayMealsForEdit(date).size)
        // 编辑：晚餐只留 d2
        r.meal.saveDayMeals(date, listOf(
            DayMealDraft(bf, LocalTime(7, 30), "", listOf(d1)),
            DayMealDraft(dn, LocalTime(18, 30), "", listOf(d2)),
        ))
        val dinner = r.meal.loadDayMealsForEdit(date).first { it.mealTypeId == dn }
        assertEquals(listOf(d2), dinner.dishes.map { it.id })
        // 空列表清空当天
        r.meal.saveDayMeals(date, emptyList())
        assertTrue(r.meal.loadDayMealsForEdit(date).isEmpty())
    }

    // ============ 库存 + 推荐 端到端 ============

    @Test
    fun `库存加食材_gather把用到它的菜纳入候选_引擎推荐`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val potato = r.ing.createUserIngredient("土豆")
        val dishId = r.dish.saveDish(
            id = 0, name = "醋溜土豆丝", cookingMethodId = null, cookingMethodNames = listOf("炒"),
            specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(),
            ingredients = listOf(DishIngredient(ingredient = Ingredient(id = potato, name = "土豆"), isMain = true)),
        )
        r.pantry.addToPantry(potato, null, null, null, "")
        r.pantry.addServings(potato, 2)

        val ds = RecommendationDataSource(db, r.pantry, r.dish, HealthProfileRepository(db), r.ing, com.sxdbsm.cookbook.data.repository.NutritionRepository(db))
        val input = ds.gather(RecommendMode.PANTRY)
        assertTrue(input.dishes.any { it.id == dishId }, "在手土豆→醋溜土豆丝应进候选")
        val cands = HealthRuleEngine().evaluate(input.dishes, input.pantryIngredientIds, input.constraints, input.recentDishIds, input.shortageIngredientIds)
        assertTrue(cands.any { it.id == dishId }, "应被库存推荐")
    }

    @Test
    fun `加份数累加_读剩余`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val egg = r.ing.createUserIngredient("鸡蛋")
        r.pantry.addServings(egg, 2)
        r.pantry.addServings(egg, 3)
        assertEquals(5, r.pantry.remaining()[egg])
        assertTrue(r.pantry.listPantryIngredients().any { it.id == egg })
    }

    // ============ 异常但符合人类操作的流程 ============

    @Test
    fun `重名食材_多次创建复用同一id`() = runBlocking {
        // 用户加菜时随手又建了个同名"五花肉"，应复用已有 id，不产生重复行。
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val id1 = r.ing.createUserIngredient("五花肉")
        val id2 = r.ing.createUserIngredient("  五花肉 ") // 带空格
        assertEquals(id1, id2, "同名(去空格)食材应复用同一 id")
    }

    @Test
    fun `删除被餐食引用的菜_能查出引用不误删`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val dishId = r.dish.saveDish(id = 0, name = "被引用菜", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())
        val bf = mealTypeId(db, "BREAKFAST", "早餐", "07:30")
        r.meal.saveDayMeals(LocalDate(2026, 7, 13), listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(dishId))))
        val refs = r.dish.listMealReferencesByDish(dishId)
        assertTrue(refs.isNotEmpty(), "应查出该菜被餐食引用")
    }

    @Test
    fun `软删自建食材_进回收站可恢复`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val id = r.ing.createUserIngredient("临时食材")
        r.ing.deleteUserIngredient(id, "用户删除")
        assertTrue(r.ing.search("临时食材").none { it.id == id }, "软删后不在正常列表")
        assertTrue(r.ing.listInactiveUserIngredients().any { it.id == id }, "应在回收站")
        r.ing.restoreUserIngredient(id)
        assertTrue(r.ing.search("临时食材").any { it.id == id }, "恢复后回到正常列表")
    }

    @Test
    fun `搜索菜品_命中菜名`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        r.dish.saveDish(id = 0, name = "宫保鸡丁", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = listOf("川菜"), ingredients = emptyList())
        assertTrue(r.dish.searchDishes("宫保").any { it.name == "宫保鸡丁" })
    }

    @Test
    fun `餐食日期区间_随保存更新`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val d = r.dish.saveDish(id = 0, name = "菜", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())
        val bf = mealTypeId(db, "BREAKFAST", "早餐", "07:30")
        r.meal.saveDayMeals(LocalDate(2026, 7, 10), listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        r.meal.saveDayMeals(LocalDate(2026, 7, 15), listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        val (min, max) = r.meal.dateRange()
        assertEquals(LocalDate(2026, 7, 10), min)
        assertEquals(LocalDate(2026, 7, 15), max)
    }

    @Test
    fun `复制式移动_保存到新日期再删旧日期`() = runBlocking {
        // 模拟"编辑餐食把日期改到新的一天=移动"：保存新日期后删旧日期，旧日期应空、新日期有。
        val db = RepositoryTestDatabase.create()
        val r = newRepos(db)
        val d = r.dish.saveDish(id = 0, name = "移动菜", cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())
        val bf = mealTypeId(db, "BREAKFAST", "早餐", "07:30")
        val from = LocalDate(2026, 7, 13)
        val to = LocalDate(2026, 7, 14)
        r.meal.saveDayMeals(from, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        // 移动
        r.meal.saveDayMeals(to, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        r.meal.deleteDayMeals(from)
        assertTrue(r.meal.loadDayMealsForEdit(from).isEmpty(), "旧日期已清")
        assertEquals(1, r.meal.loadDayMealsForEdit(to).size)
    }
}
