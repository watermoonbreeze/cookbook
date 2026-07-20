package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.PlanDish
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : PeriodPlannerTest
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划规则单测（天数/忌口剔除/健康80%/应季优先）
 * <p>
 * [AI生成] 合成候选验证规划规则，不依赖数据源与模型。
 **/
class PeriodPlannerTest {

    private val planner = PeriodPlanner()
    private val meals = listOf("早餐", "中餐", "晚餐")

    private fun dish(
        id: Long,
        main: List<String> = listOf("料$id"),
        nutrition: Set<String> = emptySet(),
        season: Set<String> = emptySet(),
        healthy: Boolean = false,
        avoid: Boolean = false,
        breakfast: Boolean = false,
        meat: Boolean = false,
        methods: List<String> = emptyList(), // [AI生成] 做法名(重油族去重测试用)
    ) = PlanDish(
        id = id, name = "菜$id", mainNames = main, nutritionTags = nutrition, seasonTags = season,
        isHealthy = healthy, hasAvoid = avoid, isMeat = meat, isBreakfast = breakfast,
        cookingMethodNames = methods,
    )

    @Test
    fun `天数与餐次数量正确`() {
        val dishes = (1L..20L).map { dish(it) }
        val plan = planner.plan(dishes, days = 3, mealNames = meals, dishesMin = 2, dishesMax = 2)
        assertEquals(3, plan.days.size)
        assertEquals(3, plan.days[0].meals.size)
        assertEquals(2, plan.days[0].meals[0].dishes.size)
    }

    @Test
    fun `每餐菜数在2到5之间`() {
        val dishes = (1L..40L).map { dish(it, main = listOf("料$it")) }
        val plan = planner.plan(dishes, days = 5, mealNames = meals, dishesMin = 2, dishesMax = 5, seed = 3)
        val counts = plan.days.flatMap { it.meals }.map { it.dishes.size }
        assertTrue(counts.all { it in 2..5 }, "每餐菜数应在2~5: $counts")
        assertTrue(counts.toSet().size > 1, "菜数应有变化而非固定: $counts")
    }

    @Test
    fun `天数封顶30`() {
        val dishes = (1L..30L).map { dish(it) }
        val plan = planner.plan(dishes, days = 100, mealNames = meals, dishesMin = 1, dishesMax = 1)
        assertEquals(30, plan.days.size)
    }

    @Test
    fun `忌口菜被剔除`() {
        val plan = planner.plan(
            listOf(dish(1), dish(2, avoid = true)),
            days = 1, mealNames = listOf("中餐"), dishesMin = 1, dishesMax = 1,
        )
        val ids = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }
        assertTrue(2L !in ids)
    }

    @Test
    fun `健康档案下利健康占比不低于80%`() {
        val healthy = (1L..10L).map { dish(it, main = listOf("m$it"), healthy = true) }
        val unhealthy = (11L..15L).map { dish(it, main = listOf("m$it"), healthy = false) }
        val plan = planner.plan(healthy + unhealthy, days = 3, mealNames = meals, dishesMin = 2, dishesMax = 2, healthAware = true)
        assertTrue(plan.healthyRatio >= 0.8, "healthyRatio=${plan.healthyRatio}")
    }

    @Test
    fun `同餐尽量荤素搭配`() {
        // 2 荤 2 素、每餐 2 菜：应荤素各一，而非两荤或两素。
        val dishes = listOf(
            dish(1, main = listOf("五花肉"), meat = true),
            dish(2, main = listOf("鸡腿"), meat = true),
            dish(3, main = listOf("青椒"), meat = false),
            dish(4, main = listOf("白菜"), meat = false),
        )
        val plan = planner.plan(dishes, days = 1, mealNames = listOf("中餐"), dishesMin = 2, dishesMax = 2, seed = 1)
        val picked = plan.days[0].meals[0].dishes.map { it.id }.toSet()
        val meatCount = picked.count { it == 1L || it == 2L }
        val vegCount = picked.count { it == 3L || it == 4L }
        assertEquals(1, meatCount, "一餐应有 1 荤: picked=$picked")
        assertEquals(1, vegCount, "一餐应有 1 素: picked=$picked")
    }

    @Test
    fun `重油族跨餐去重_一餐不两道重油菜`() {
        // 2 道重油(红烧/干煸)+1 道清淡(清蒸)、全素(荤素不干扰)、同 base、1 餐 2 道、FRESH 强去重：
        //   选一道重油后另一道重油被降权(-f.repeat×0.4>jitter)→清淡菜必入选,一餐不出现两道重油。
        val dishes = listOf(
            dish(1, main = listOf("茄子"), methods = listOf("红烧")),
            dish(2, main = listOf("豆角"), methods = listOf("干煸")),
            dish(3, main = listOf("南瓜"), methods = listOf("清蒸")),
        )
        val plan = planner.plan(
            dishes, days = 1, mealNames = listOf("晚餐"), dishesMin = 2, dishesMax = 2,
            seed = 0, style = RecommendationStyle.FRESH,
        )
        val ids = plan.days[0].meals[0].dishes.map { it.id }.toSet()
        assertEquals(2, ids.size)
        assertTrue(!(1L in ids && 2L in ids), "一餐不应两道重油菜(红烧茄子+干煸豆角): $ids")
        assertTrue(3L in ids, "重油已用后应补清淡菜(清蒸南瓜): $ids")
    }

    @Test
    fun `应季菜优先`() {
        val plan = planner.plan(
            listOf(dish(1, season = setOf("夏季")), dish(2)),
            days = 1, mealNames = listOf("中餐"), dishesMin = 1, dishesMax = 1, currentSeason = "夏季",
        )
        assertEquals(1L, plan.days[0].meals[0].dishes[0].id)
    }

    @Test
    fun `早餐档只选早餐菜午晚选非早餐菜`() {
        val plan = planner.plan(
            listOf(dish(1, breakfast = true), dish(2, breakfast = false)),
            days = 1, mealNames = listOf("早餐", "中餐"), dishesMin = 1, dishesMax = 1,
        )
        val bfDishes = plan.days[0].meals.first { it.mealName == "早餐" }.dishes.map { it.id }
        val lunchDishes = plan.days[0].meals.first { it.mealName == "中餐" }.dishes.map { it.id }
        assertTrue(1L in bfDishes && 2L !in bfDishes, "早餐应只出早餐菜: $bfDishes")
        assertTrue(2L in lunchDishes && 1L !in lunchDishes, "中餐应只出非早餐菜: $lunchDishes")
    }

    @Test
    fun `候选充足时尽量不重复同一道菜`() {
        val dishes = (1L..60L).map { dish(it, main = listOf("料$it")) }
        val plan = planner.plan(dishes, days = 2, mealNames = meals, dishesMin = 2, dishesMax = 2, seed = 7)
        val ids = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }
        // 每餐固定 2 道、12 个坑、60 个候选，应无重复
        assertEquals(ids.size, ids.toSet().size)
    }
}
