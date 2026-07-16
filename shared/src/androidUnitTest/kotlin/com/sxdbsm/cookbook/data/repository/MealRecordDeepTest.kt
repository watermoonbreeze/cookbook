package com.sxdbsm.cookbook.data.repository

import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : MealRecordDeepTest
 * @Time : 2026/07/13
 * @Author : SXD-AI
 * @Desc : 餐食模块深入测试（移动不重复抬喜爱度 / 往返完整性 / 编辑删餐次真删）
 * <p>
 * [AI生成] 餐食模块深挖：守护"移动日期喜爱度重复+1"修复与整日替换语义。
 **/
class MealRecordDeepTest {

    private fun mealType(db: com.sxdbsm.cookbook.db.CookbookDatabase, code: String, name: String, time: String): Long {
        db.cookbookQueries.insertMealType(code, name, time, 1, "preset")
        return db.cookbookQueries.lastInsertId().executeAsOne()
    }

    private suspend fun newDish(dishRepo: DishRepository, name: String): Long =
        dishRepo.saveDish(id = 0, name = name, cookingMethodId = null, cookingMethodNames = listOf("炒"), specialNote = "", description = "", imagePath = "", thumbnailPath = "", tagNames = emptyList(), ingredients = emptyList())

    @Test
    fun `移动日期_不重复抬高喜爱度`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val d = newDish(dishRepo, "红烧肉")
        val bf = mealType(db, "BREAKFAST", "早餐", "07:30")
        val day1 = LocalDate(2026, 7, 13)
        val day2 = LocalDate(2026, 7, 14)
        // 首次保存到 day1：新菜 → 喜爱度 +1 = 1
        mealRepo.saveDayMeals(day1, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        assertEquals(1, dishRepo.getDishMiniById(d)!!.preference)
        // 移动到 day2(空)：基线=来源 day1，菜已计过 → 不再 +1
        mealRepo.saveDayMeals(day2, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))), incrementBaselineDate = day1)
        mealRepo.deleteDayMeals(day1)
        assertEquals(1, dishRepo.getDishMiniById(d)!!.preference, "移动不应把已计过的菜再+1")
    }

    @Test
    fun `删整天撤销还原_不重复抬喜爱度`() = runBlocking {
        // [AI生成] Google 代码审查阻断项复验：删→撤销(saveDayMeals bumpPreference=false)不应把每道菜再+1。
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val d = newDish(dishRepo, "回锅肉")
        val bf = mealType(db, "BREAKFAST", "早餐", "07:30")
        val day = LocalDate(2026, 7, 13)
        mealRepo.saveDayMeals(day, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        assertEquals(1, dishRepo.getDishMiniById(d)!!.preference)
        val snap = mealRepo.snapshotDay(day)
        assertEquals(1, snap.size, "快照应含那天的餐")
        mealRepo.deleteDayMeals(day)
        mealRepo.saveDayMeals(day, snap, bumpPreference = false)
        assertEquals(1, dishRepo.getDishMiniById(d)!!.preference, "撤销还原不应重复抬喜爱度")
        assertEquals(1, mealRepo.loadDayMealsForEdit(day).size, "餐食应被还原")
    }

    @Test
    fun `新增新菜_首次入餐抬喜爱度`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val d = newDish(dishRepo, "青菜")
        val bf = mealType(db, "BREAKFAST", "早餐", "07:30")
        val day = LocalDate(2026, 7, 13)
        mealRepo.saveDayMeals(day, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        assertEquals(1, dishRepo.getDishMiniById(d)!!.preference)
        // 同日编辑再保存(菜不变) → 不重复 +1
        mealRepo.saveDayMeals(day, listOf(DayMealDraft(bf, LocalTime(7, 30), "改备注", listOf(d))))
        assertEquals(1, dishRepo.getDishMiniById(d)!!.preference)
    }

    @Test
    fun `往返完整性_餐次备注菜品顺序`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val d1 = newDish(dishRepo, "菜1")
        val d2 = newDish(dishRepo, "菜2")
        val dn = mealType(db, "DINNER", "晚餐", "18:30")
        val date = LocalDate(2026, 7, 13)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(dn, LocalTime(18, 30), "今天加班", listOf(d2, d1))))
        val loaded = mealRepo.loadDayMealsForEdit(date)
        assertEquals(1, loaded.size)
        assertEquals("今天加班", loaded.first().note)
        assertEquals(listOf(d2, d1), loaded.first().dishes.map { it.id }, "菜品顺序(sort_order)应保留")
    }

    @Test
    fun `编辑删掉一个餐次_真删不残留`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val d = newDish(dishRepo, "菜")
        val bf = mealType(db, "BREAKFAST", "早餐", "07:30")
        val dn = mealType(db, "DINNER", "晚餐", "18:30")
        val date = LocalDate(2026, 7, 13)
        mealRepo.saveDayMeals(date, listOf(
            DayMealDraft(bf, LocalTime(7, 30), "", listOf(d)),
            DayMealDraft(dn, LocalTime(18, 30), "", listOf(d)),
        ))
        assertEquals(2, mealRepo.loadDayMealsForEdit(date).size)
        // 编辑：只留早餐(相当于删掉晚餐)
        mealRepo.saveDayMeals(date, listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        val after = mealRepo.loadDayMealsForEdit(date)
        assertEquals(1, after.size, "删掉的晚餐应真删、不残留")
        assertEquals(bf, after.first().mealTypeId)
    }

    @Test
    fun `dateRange_只含有餐食的日期`() = runBlocking {
        val db = RepositoryTestDatabase.create()
        val dishRepo = DishRepository(db)
        val mealRepo = MealRecordRepository(db)
        val d = newDish(dishRepo, "菜")
        val bf = mealType(db, "BREAKFAST", "早餐", "07:30")
        assertEquals(null to null, mealRepo.dateRange())
        mealRepo.saveDayMeals(LocalDate(2026, 7, 10), listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        mealRepo.saveDayMeals(LocalDate(2026, 7, 20), listOf(DayMealDraft(bf, LocalTime(7, 30), "", listOf(d))))
        assertEquals(LocalDate(2026, 7, 10) to LocalDate(2026, 7, 20), mealRepo.dateRange())
    }
}
