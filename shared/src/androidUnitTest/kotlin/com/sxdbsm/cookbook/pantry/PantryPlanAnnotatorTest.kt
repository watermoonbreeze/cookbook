package com.sxdbsm.cookbook.pantry

import com.sxdbsm.cookbook.ai.model.DayPlan
import com.sxdbsm.cookbook.ai.model.PeriodPlan
import com.sxdbsm.cookbook.ai.model.PlannedDish
import com.sxdbsm.cookbook.ai.model.PlannedMeal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : PantryPlanAnnotatorTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 周期规划采购/缺料标注单测
 * <p>
 * [AI生成] 覆盖：不在库→采购、在库份数够→不标、跨天份数用尽→缺料。
 **/
class PantryPlanAnnotatorTest {

    private val PORK = 1L
    private val EGG = 2L

    private fun plan(vararg dishIdsPerDay: Long): PeriodPlan {
        val days = dishIdsPerDay.mapIndexed { i, id ->
            DayPlan(i, listOf(PlannedMeal("中餐", listOf(PlannedDish(id, "菜$id", "")))))
        }
        return PeriodPlan(days, healthAware = false, healthyRatio = 0.0)
    }

    @Test
    fun `不在库主料标采购`() {
        val p = plan(10)
        val mains = mapOf(10L to listOf(PlanMainIngredient(PORK, "猪肉")))
        val out = PantryPlanAnnotator.annotate(p, mains, pantryIds = emptySet(), remaining = emptyMap())
        val dish = out.days[0].meals[0].dishes[0]
        assertEquals(listOf("猪肉"), dish.purchaseNames)
        assertTrue(dish.shortageNames.isEmpty())
    }

    @Test
    fun `在库份数够不标`() {
        val p = plan(10)
        val mains = mapOf(10L to listOf(PlanMainIngredient(PORK, "猪肉")))
        val out = PantryPlanAnnotator.annotate(p, mains, pantryIds = setOf(PORK), remaining = mapOf(PORK to 1))
        val dish = out.days[0].meals[0].dishes[0]
        assertTrue(dish.purchaseNames.isEmpty())
        assertTrue(dish.shortageNames.isEmpty())
    }

    @Test
    fun `跨天份数用尽后标缺料`() {
        // 两天都用猪肉，库存剩 1 份 → 第1天够、第2天缺
        val p = plan(10, 20)
        val mains = mapOf(
            10L to listOf(PlanMainIngredient(PORK, "猪肉")),
            20L to listOf(PlanMainIngredient(PORK, "猪肉")),
        )
        val out = PantryPlanAnnotator.annotate(p, mains, pantryIds = setOf(PORK), remaining = mapOf(PORK to 1))
        assertTrue(out.days[0].meals[0].dishes[0].shortageNames.isEmpty()) // 第1天够
        assertEquals(listOf("猪肉"), out.days[1].meals[0].dishes[0].shortageNames) // 第2天缺
    }

    @Test
    fun `采购与缺料混合`() {
        val p = plan(10)
        val mains = mapOf(10L to listOf(PlanMainIngredient(PORK, "猪肉"), PlanMainIngredient(EGG, "鸡蛋")))
        // 猪肉在库但0份→缺；鸡蛋不在库→采购
        val out = PantryPlanAnnotator.annotate(p, mains, pantryIds = setOf(PORK), remaining = mapOf(PORK to 0))
        val dish = out.days[0].meals[0].dishes[0]
        assertEquals(listOf("猪肉"), dish.shortageNames)
        assertEquals(listOf("鸡蛋"), dish.purchaseNames)
    }
}
