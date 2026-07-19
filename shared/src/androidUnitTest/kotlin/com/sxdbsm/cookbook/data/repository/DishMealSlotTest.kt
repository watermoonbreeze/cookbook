package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.ai.MealSlot
import com.sxdbsm.cookbook.ai.MealSlotMatcher
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : DishMealSlotTest
 * @Time : 2026/07/19
 * @Author : SXD-AI
 * @Desc : 菜品「适合餐次」(dish_meal_slot 表 + Repository) 数据往返 + Matcher 兜底测试
 * <p>
 * 测试库走 Schema.create(含 dish_meal_slot 表)。校验保存/回显往返、空则 Matcher 兜底、批量查、DishMini 填充。
 * <p>
 * [AI生成] v28：餐次分类维度。恒非空(永不无餐次菜)。
 **/
class DishMealSlotTest {

    /** 建 6 个固定餐次(测试库 Schema.create 不含 seed，手动插)。[AI生成] */
    private fun seedMealTypes(db: com.sxdbsm.cookbook.db.CookbookDatabase) {
        val q = db.cookbookQueries
        listOf(
            "BREAKFAST" to "早餐", "MORNING_SNACK" to "上午餐", "LUNCH" to "中餐",
            "AFTERNOON_SNACK" to "下午餐", "DINNER" to "晚餐", "NIGHT_SNACK" to "宵夜",
        ).forEach { (code, name) -> q.insertMealType(code, name, "12:00", 1L, "preset") }
    }

    @Test
    fun `保存与回显往返正确`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        seedMealTypes(db)
        val repo = DishRepository(db)

        // 显式给早餐+加餐(上午餐)两个餐次
        val id = repo.saveDish(
            id = 0L, name = "皮蛋瘦肉粥", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
            cuisine = "家常菜", mealSlotCodes = listOf("BREAKFAST", "MORNING_SNACK"),
        )
        val loaded = repo.getDishById(id)!!
        assertEquals(setOf(MealSlot.BREAKFAST, MealSlot.MORNING_SNACK), loaded.mealSlots.toSet())
    }

    @Test
    fun `空餐次保存时按菜名Matcher兜底_永不无餐次`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        seedMealTypes(db)
        val repo = DishRepository(db)

        // 不给餐次 → repo 按菜名 Matcher 兜底(红烧肉推不出早餐/加餐 → 正餐中/晚)
        val id = repo.saveDish(
            id = 0L, name = "红烧肉", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
            cuisine = "家常菜", mealSlotCodes = emptyList(),
        )
        val loaded = repo.getDishById(id)!!
        assertTrue(loaded.mealSlots.isNotEmpty(), "永不出现无餐次菜")
        assertEquals(setOf(MealSlot.LUNCH, MealSlot.DINNER), loaded.mealSlots.toSet())
    }

    @Test
    fun `编辑改餐次为全量替换`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        seedMealTypes(db)
        val repo = DishRepository(db)
        val id = repo.saveDish(
            id = 0L, name = "鸡蛋羹", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
            cuisine = "家常菜", mealSlotCodes = listOf("BREAKFAST", "MORNING_SNACK", "DINNER"),
        )
        // 改为只留早餐
        repo.saveDish(
            id = id, name = "鸡蛋羹", cookingMethodId = null, specialNote = "", description = "",
            imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList(),
            cuisine = "家常菜", mealSlotCodes = listOf("BREAKFAST"),
        )
        val loaded = repo.getDishById(id)!!
        assertEquals(setOf(MealSlot.BREAKFAST), loaded.mealSlots.toSet())
    }

    @Test
    fun `DishMini按餐次填充_未打标回退Matcher`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        seedMealTypes(db)
        val repo = DishRepository(db)
        val q = db.cookbookQueries
        // 直接插一条不打餐次标的菜(模拟老库/未 seed)
        q.insertDish("番茄炒蛋", null, "", "", "", "", "user", 0L, 0L, "家常菜")
        val minis = repo.searchDishes("番茄炒蛋")
        assertEquals(1, minis.size)
        // 未打标 → DishMini.mealSlots 回退 Matcher 兜底，恒非空
        assertTrue(minis.first().mealSlots.isNotEmpty())
    }

    @Test
    fun `Matcher默认推断器_早餐菜命中早餐_恒非空`() {
        assertTrue(MealSlot.BREAKFAST in MealSlotMatcher.defaultSlotsFor("小米粥"))
        assertTrue(MealSlot.BREAKFAST in MealSlotMatcher.defaultSlotsFor("豆浆"))
        // 推不出具体餐次 → 兜底正餐(中+晚)，非空
        val fallback = MealSlotMatcher.defaultSlotsFor("宫保鸡丁")
        assertTrue(fallback.isNotEmpty())
        assertTrue(MealSlot.LUNCH in fallback && MealSlot.DINNER in fallback)
        assertTrue(MealSlot.ALL !in fallback) // 不含"全部"
        Unit
    }
}
