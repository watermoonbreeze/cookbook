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
    private val meals = listOf("早餐", "午餐", "晚餐")

    private fun dish(
        id: Long,
        main: List<String> = listOf("料$id"),
        nutrition: Set<String> = emptySet(),
        season: Set<String> = emptySet(),
        healthy: Boolean = false,
        avoid: Boolean = false,
    ) = PlanDish(id, "菜$id", main, nutrition, season, healthy, avoid)

    @Test
    fun `天数与餐次数量正确`() {
        val dishes = (1L..20L).map { dish(it) }
        val plan = planner.plan(dishes, days = 3, mealNames = meals, dishesPerMeal = 2)
        assertEquals(3, plan.days.size)
        assertEquals(3, plan.days[0].meals.size)
        assertEquals(2, plan.days[0].meals[0].dishes.size)
    }

    @Test
    fun `天数封顶30`() {
        val dishes = (1L..30L).map { dish(it) }
        val plan = planner.plan(dishes, days = 100, mealNames = meals, dishesPerMeal = 1)
        assertEquals(30, plan.days.size)
    }

    @Test
    fun `忌口菜被剔除`() {
        val plan = planner.plan(
            listOf(dish(1), dish(2, avoid = true)),
            days = 1, mealNames = listOf("午餐"), dishesPerMeal = 1,
        )
        val ids = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }
        assertTrue(2L !in ids)
    }

    @Test
    fun `健康档案下利健康占比不低于80%`() {
        val healthy = (1L..10L).map { dish(it, main = listOf("m$it"), healthy = true) }
        val unhealthy = (11L..15L).map { dish(it, main = listOf("m$it"), healthy = false) }
        val plan = planner.plan(healthy + unhealthy, days = 3, mealNames = meals, dishesPerMeal = 2, healthAware = true)
        assertTrue(plan.healthyRatio >= 0.8, "healthyRatio=${plan.healthyRatio}")
    }

    @Test
    fun `应季菜优先`() {
        val plan = planner.plan(
            listOf(dish(1, season = setOf("夏季")), dish(2)),
            days = 1, mealNames = listOf("午餐"), dishesPerMeal = 1, currentSeason = "夏季",
        )
        assertEquals(1L, plan.days[0].meals[0].dishes[0].id)
    }

    @Test
    fun `候选充足时尽量不重复同一道菜`() {
        val dishes = (1L..30L).map { dish(it, main = listOf("料$it")) }
        val plan = planner.plan(dishes, days = 2, mealNames = meals, dishesPerMeal = 2, seed = 7)
        val ids = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }
        // 12 个坑、30 个候选，应无重复
        assertEquals(ids.size, ids.toSet().size)
    }
}
