package com.sxdbsm.cookbook.sync

import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FavoriteComboRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.RepositoryTestDatabase
import com.sxdbsm.cookbook.db.CookbookDatabase
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * @File : SyncRepositoryTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 选择性同步 导出/合并导入 单测(P1 菜品+食材)
 * <p>
 * [AI生成] 覆盖：导出→导入到另一库(按名合并+ID重映射)、同名不重复、重复导入更新不新增。
 **/
class SyncRepositoryTest {

    private fun sync(db: CookbookDatabase) = SyncRepository(
        db, DishRepository(db), IngredientRepository(db), PantryRepository(db),
        HealthProfileRepository(db), FavoriteComboRepository(db, DishRepository(db)),
        com.sxdbsm.cookbook.data.repository.MealRecordRepository(db),
        com.sxdbsm.cookbook.data.repository.StepTemplateRepository(db),
        com.sxdbsm.cookbook.data.repository.IngredientGroupRepository(db),
    )

    private fun seedSource(db: com.sxdbsm.cookbook.db.CookbookDatabase) = runBlocking {
        val ingRepo = IngredientRepository(db)
        val dishRepo = DishRepository(db)
        val porkId = ingRepo.createUserIngredient(name = "猪肉", alias = "瘦肉")
        val potatoId = ingRepo.createUserIngredient(name = "土豆")
        dishRepo.saveDish(
            id = 0, name = "土豆炖肉", cookingMethodId = null, cookingMethodNames = listOf("炖"),
            specialNote = "少盐", description = "家常炖菜", imagePath = "d.jpg", thumbnailPath = "d_t.jpg",
            tagNames = listOf("家常"),
            ingredients = listOf(
                DishIngredient(ingredient = Ingredient(id = porkId, name = "猪肉"), isMain = true),
                DishIngredient(ingredient = Ingredient(id = potatoId, name = "土豆"), isMain = true),
            ),
            steps = emptyList(),
        )
    }

    @Test
    fun `导出后导入到另一库-按名合并且食材重映射`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        seedSource(src)
        val bundle = sync(src).export(SyncSelection(ingredients = true, dishes = true))
        // 导出内容
        assertTrue(bundle.dishes.any { it.name == "土豆炖肉" })
        assertTrue(bundle.ingredients.any { it.name == "猪肉" } && bundle.ingredients.any { it.name == "土豆" })

        // 导入到空库(目标 id 与源不同也应正确重映射)
        val dst = RepositoryTestDatabase.create()
        val dstDishRepo = DishRepository(dst)
        val result = sync(dst).import(bundle)
        assertEquals(1, result.dishesAdded)
        assertTrue(result.ingredientsAdded >= 2)

        // 目标库能查到菜, 且其食材(重映射后)名字正确
        val dishId = dst.cookbookQueries.selectUserDishIdByName("土豆炖肉").executeAsOneOrNull()
        assertNotNull(dishId)
        val dish = dstDishRepo.getDishById(dishId)
        assertNotNull(dish)
        val names = dish.ingredients.map { it.ingredient.name }.toSet()
        assertEquals(setOf("猪肉", "土豆"), names)
    }

    @Test
    fun `重复导入更新不新增`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        seedSource(src)
        val bundle = sync(src).export(SyncSelection(ingredients = true, dishes = true))

        val dst = RepositoryTestDatabase.create()
        val sync = sync(dst)
        sync.import(bundle)
        val second = sync.import(bundle) // 再导一次
        assertEquals(0, second.dishesAdded) // 同名不新增
        assertEquals(1, second.dishesUpdated) // 而是更新
        assertEquals(0, second.ingredientsAdded) // 食材同名复用
    }

    @Test
    fun `步骤模板随菜品域同步-同名不重复`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        seedSource(src)
        com.sxdbsm.cookbook.data.repository.StepTemplateRepository(src)
            .createTemplate("我的红烧", listOf("焯水", "炒糖色", "焖煮", "收汁"))
        // 选菜品域 → 自建步骤模板一并带出
        val bundle = sync(src).export(SyncSelection(dishes = true))
        assertTrue(bundle.stepTemplates.any { it.name == "我的红烧" && it.steps.size == 4 })

        val dst = RepositoryTestDatabase.create()
        val dstSync = sync(dst)
        val r1 = dstSync.import(bundle)
        assertEquals(1, r1.stepTemplatesAdded)
        val tpls = com.sxdbsm.cookbook.data.repository.StepTemplateRepository(dst).listTemplates()
        assertEquals(listOf("焯水", "炒糖色", "焖煮", "收汁"), tpls.first { it.name == "我的红烧" }.steps)
        // 再导一次 → 同名跳过
        val r2 = dstSync.import(bundle)
        assertEquals(0, r2.stepTemplatesAdded)
    }

    @Test
    fun `库存同步-份数按名合并且自带食材`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        val porkId = IngredientRepository(src).createUserIngredient(name = "猪肉")
        PantryRepository(src).addServings(porkId, 3)
        val bundle = sync(src).export(SyncSelection(pantry = true))
        assertTrue(bundle.pantry.any { it.ingredientName == "猪肉" && it.servingCount == 3 })
        assertTrue(bundle.ingredients.any { it.name == "猪肉" }) // 依赖食材自带

        val dst = RepositoryTestDatabase.create()
        val res = sync(dst).import(bundle)
        assertTrue(res.pantryMerged >= 1)
        val dstPorkId = dst.cookbookQueries.selectActiveIngredientIdByName("猪肉").executeAsOneOrNull()
        assertNotNull(dstPorkId)
        assertEquals(3, PantryRepository(dst).remaining()[dstPorkId])
    }

    @Test
    fun `收藏组合同步-按菜品名重映射`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        seedSource(src) // 建 土豆炖肉
        val dishId = src.cookbookQueries.selectUserDishIdByName("土豆炖肉").executeAsOneOrNull()
        assertNotNull(dishId)
        FavoriteComboRepository(src, DishRepository(src)).createCombo("我的最爱", listOf(dishId))
        val bundle = sync(src).export(SyncSelection(favorites = true))
        assertTrue(bundle.favorites.any { it.name == "我的最爱" && it.dishNames.contains("土豆炖肉") })
        assertTrue(bundle.dishes.any { it.name == "土豆炖肉" }) // 依赖菜品自带

        val dst = RepositoryTestDatabase.create()
        val res = sync(dst).import(bundle)
        assertEquals(1, res.favoritesAdded)
        val combos = FavoriteComboRepository(dst, DishRepository(dst)).listCombos()
        assertTrue(combos.any { c -> c.name == "我的最爱" && c.dishes.any { it.name == "土豆炖肉" } })
    }

    @Test
    fun `弹性合并-单条坏数据被跳过不中断整批且skipped计数`() = runBlocking {
        // [AI生成] 综合审查：import 是幂等合并，单行异常(如日期非法)应隔离跳过、不"半写后抛错"中断整批。
        val dst = RepositoryTestDatabase.create()
        dst.cookbookQueries.insertMealType("LUNCH", "中餐", "12:00", 1L, "preset")
        seedSource(dst) // dst 建 土豆炖肉，供餐食引用
        val bundle = SyncBundle(
            schemaVersion = CookbookDatabase.Schema.version.toInt(),
            meals = listOf(
                SyncMeal(date = "2026-07-11", mealTypeCode = "LUNCH", mealTime = "12:00", note = "正常", dishNames = listOf("土豆炖肉")),
                SyncMeal(date = "非法日期", mealTypeCode = "LUNCH", mealTime = "12:00", note = "坏行", dishNames = listOf("土豆炖肉")),
            ),
        )
        val res = sync(dst).import(bundle) // 不应抛异常
        assertEquals(1, res.mealsMerged, "正常餐食应导入成功")
        assertTrue(res.skipped >= 1, "坏日期行应被跳过并计入 skipped")
        // 正常行确实落地
        val lunchId = dst.cookbookQueries.selectMealTypeIdByCode("LUNCH").executeAsOneOrNull()
        assertNotNull(lunchId)
        assertNotNull(dst.cookbookQueries.selectMealRecordIdByDateType("2026-07-11", lunchId).executeAsOneOrNull())
        Unit
    }

    @Test
    fun `餐食历史同步-按日期餐次合并且菜品重映射`() = runBlocking {
        val src = RepositoryTestDatabase.create()
        src.cookbookQueries.insertMealType("LUNCH", "中餐", "12:00", 1L, "preset")
        seedSource(src) // 建 土豆炖肉
        val dishId = src.cookbookQueries.selectUserDishIdByName("土豆炖肉").executeAsOneOrNull()
        assertNotNull(dishId)
        val lunchId = src.cookbookQueries.selectMealTypeIdByCode("LUNCH").executeAsOneOrNull()
        assertNotNull(lunchId)
        MealRecordRepository(src).save(DateTime.parseDate("2026-07-10"), lunchId, DateTime.parseTime("12:00"), "午饭", listOf(dishId))

        val bundle = sync(src).export(SyncSelection(meals = true))
        assertTrue(bundle.meals.any { it.date == "2026-07-10" && it.mealTypeCode == "LUNCH" && it.dishNames.contains("土豆炖肉") })
        assertTrue(bundle.dishes.any { it.name == "土豆炖肉" }) // 依赖菜品自带

        val dst = RepositoryTestDatabase.create()
        dst.cookbookQueries.insertMealType("LUNCH", "中餐", "12:00", 1L, "preset")
        val res = sync(dst).import(bundle)
        assertEquals(1, res.mealsMerged)
        val dstLunchId = dst.cookbookQueries.selectMealTypeIdByCode("LUNCH").executeAsOneOrNull()
        assertNotNull(dstLunchId)
        val recId = dst.cookbookQueries.selectMealRecordIdByDateType("2026-07-10", dstLunchId).executeAsOneOrNull()
        assertNotNull(recId)
        Unit
    }
}
