package com.sxdbsm.cookbook.domain.model

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : DietReportAggregatorTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 饮食报告聚合——记录概况/TOP/结构均级/个人营养(按份额)/家庭视角无个人
 * <p>
 * [AI生成] 守：覆盖率=记餐天/周期天、TOP 计次、个人营养按 share 折算、家庭视角 personal=null。
 **/
class DietReportAggregatorTest {

    private fun dish(id: Long, name: String, mains: List<String>) =
        DishMini(id = id, name = name, mainIngredientNames = mains)

    // [AI生成] 带食用比例(是否吃完)的菜，供 eatenRatio 折算用例。
    private fun dishR(id: Long, name: String, mains: List<String>, eaten: Double) =
        DishMini(id = id, name = name, mainIngredientNames = mains, eatenRatio = eaten)

    private fun card(date: LocalDate, dishes: List<DishMini>) = DayMealCardData(
        date = date, temporalRole = MealDayTemporalRole.PAST,
        meals = if (dishes.isEmpty()) emptyList() else listOf(
            MealSection(mealTypeId = 1, mealName = "午餐", mealTime = LocalTime(12, 0), dishes = dishes),
        ),
    )

    private val d0 = LocalDate(2026, 7, 13)
    private fun day(n: Int) = LocalDate(2026, 7, 13 + n)

    @Test
    fun `记录概况与TOP`() {
        val egg = dish(1, "番茄炒蛋", listOf("番茄", "鸡蛋"))
        val pork = dish(2, "青椒肉丝", listOf("青椒", "猪肉"))
        // 7 天周期，记了 3 天(番茄炒蛋出现 2 次)。
        val cards = listOf(
            card(day(0), listOf(egg, pork)),
            card(day(1), emptyList()),
            card(day(2), listOf(egg)),
            card(day(3), emptyList()),
            card(day(4), listOf(pork)),
            card(day(5), emptyList()),
            card(day(6), emptyList()),
        )
        val r = DietReportAggregator.aggregate(cards, periodDays = 7, share = null, dishNutrition = emptyMap(), target = null)
        assertEquals(7, r.periodDays)
        assertEquals(3, r.recordedDays, "有菜的天数=3")
        assertEquals(3, r.mealCount)
        assertEquals(2, r.distinctDishes, "不同菜=番茄炒蛋/青椒肉丝")
        assertEquals(CountItem("番茄炒蛋", 2), r.topDishes.first(), "TOP=出现2次的番茄炒蛋")
        assertTrue(r.ingredientKinds >= 4, "主料含番茄/鸡蛋/青椒/猪肉")
        assertNull(r.personal, "家庭视角无个人营养")
        assertTrue(r.hasData)
    }

    @Test
    fun `个人营养按份额折算_达标天`() {
        val d = dish(1, "套餐", listOf("米饭"))
        val cards = listOf(card(day(0), listOf(d)), card(day(1), listOf(d)))
        // 该菜家庭口径 1600 千卡；份额 0.5 → 个人 800/天；目标 800 → 达标。
        val nutri = mapOf(1L to NutritionTotals(energyKcal = 1600.0, proteinG = 40.0, fatG = 20.0, carbG = 200.0, sodiumMg = 1000.0))
        val r = DietReportAggregator.aggregate(cards, periodDays = 7, share = 0.5, dishNutrition = nutri, target = 800)
        val p = r.personal
        assertNotNull(p)
        assertEquals(800, p.avgKcal, "1600×0.5=800")
        assertEquals(2, p.onTargetDays, "两天都达标")
        assertEquals(500, p.avgSodiumMg, "1000×0.5")
        assertTrue(p.proteinPct in 1..99 && p.carbPct in 1..99, "宏量比有值: $p")
    }

    // [AI生成] 食用比例(是否吃完):个人摄入 = 整份 × eatenRatio × share(IntakeCalculator)。守报告折算路径回归。
    @Test
    fun `个人营养_按食用比例折算`() {
        // 吃一半(0.5) × 份额 0.5 → 个人 = 1600×0.5×0.5 = 400；默认吃完(1.0)时退回 1600×share(既有用例已覆盖零回归)。
        val d = dishR(1, "套餐", listOf("米饭"), eaten = 0.5)
        val cards = listOf(card(day(0), listOf(d)))
        val nutri = mapOf(1L to NutritionTotals(energyKcal = 1600.0, sodiumMg = 1000.0))
        val r = DietReportAggregator.aggregate(cards, periodDays = 7, share = 0.5, dishNutrition = nutri, target = null)
        val perDay = r.perDayNutrition
        assertNotNull(perDay)
        assertEquals(400.0, perDay[0]!!.energyKcal, 1e-6, "1600×0.5(吃一半)×0.5(份额)=400")
        assertEquals(250.0, perDay[0]!!.sodiumMg, 1e-6, "钠同折算 1000×0.5×0.5=250")
        assertEquals(400, r.personal!!.avgKcal, "个人均值随食用比例减半")
    }

    @Test
    fun `全空_无数据`() {
        val cards = (0..6).map { card(day(it), emptyList()) }
        val r = DietReportAggregator.aggregate(cards, periodDays = 7, share = 0.5, dishNutrition = emptyMap(), target = 800)
        assertEquals(0, r.recordedDays)
        assertTrue(!r.hasData)
        assertNull(r.personal, "无记餐→个人营养 null")
        assertNull(r.perDayNutrition, "无记餐→逐日营养 null(不画空曲线)")
        assertEquals(List(7) { -1 }, r.perDayLevels, "每天均为没记(-1)")
    }

    // [AI生成] 营养趋势折线(§9.40):逐日个人营养序列与 personal 均值**同源**——防两套口径漂移。
    @Test
    fun `逐日营养与均值同源_防漂移`() {
        val a = dish(1, "菜A", listOf("米饭"))
        val b = dish(2, "菜B", listOf("鸡蛋"))
        val cards = listOf(
            card(day(0), listOf(a)),   // 记
            card(day(1), emptyList()), // 空
            card(day(2), listOf(b)),   // 记
        )
        // 家庭口径 A=1000 / B=2000；share 0.5 → 个人 A=500 / B=1000；均值=(500+1000)/2=750。
        val nutri = mapOf(
            1L to NutritionTotals(energyKcal = 1000.0, proteinG = 30.0, fatG = 10.0, carbG = 150.0, sodiumMg = 800.0),
            2L to NutritionTotals(energyKcal = 2000.0, proteinG = 60.0, fatG = 40.0, carbG = 250.0, sodiumMg = 1200.0),
        )
        val r = DietReportAggregator.aggregate(cards, periodDays = 3, share = 0.5, dishNutrition = nutri, target = null)
        val perDay = r.perDayNutrition
        assertNotNull(perDay, "个人视角逐日营养非空")
        assertEquals(3, perDay.size, "长度=逐天(含空天)")
        assertNull(perDay[1], "空天=null(断点·不脑补 0 暴跌)")
        assertNotNull(perDay[0]); assertNotNull(perDay[2])
        assertEquals(500.0, perDay[0]!!.energyKcal, 1e-6, "A 1000×0.5")
        assertEquals(1000.0, perDay[2]!!.energyKcal, 1e-6, "B 2000×0.5")
        // 防漂移核心：逐日(非空)序列聚合回均值 ≈ personal.avgKcal。
        val recorded = perDay.filterNotNull()
        val avgFromSeries = recorded.sumOf { it.energyKcal } / recorded.size
        assertEquals(r.personal!!.avgKcal.toDouble(), avgFromSeries, 0.5, "逐日序列均值≈个人均值(同源不漂移)")
        assertEquals(750, r.personal!!.avgKcal)
    }

    @Test
    fun `家庭视角_逐日营养为null`() {
        val a = dish(1, "菜A", listOf("米饭"))
        val cards = listOf(card(day(0), listOf(a)))
        val nutri = mapOf(1L to NutritionTotals(energyKcal = 1000.0))
        val r = DietReportAggregator.aggregate(cards, periodDays = 7, share = null, dishNutrition = nutri, target = null)
        assertNull(r.perDayNutrition, "家庭视角不画曲线→null")
    }
}
