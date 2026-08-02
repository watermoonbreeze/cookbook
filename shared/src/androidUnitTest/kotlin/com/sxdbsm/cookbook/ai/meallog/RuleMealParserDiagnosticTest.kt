package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 临时诊断测试：复杂自然语言用例，验证解析鲁棒性。
 * 运行通过后合并进主回归测试。
 */
class RuleMealParserDiagnosticTest {

    private val today = LocalDate(2026, 8, 2)

    // ═══════════════════════════════════════════════════
    // 用户提供的复杂用例
    // ═══════════════════════════════════════════════════

    @Test
    fun `中午准备吃红烧鹅炒青菜煮玉米和白米饭`() {
        val result = RuleMealParser.parse("中午准备吃红烧鹅炒青菜煮玉米和白米饭", today = today)
        val meals = result.flatMap { it.meals }
        assertTrue(meals.isNotEmpty(), "应解析出至少一餐")

        val allDishes = meals.flatMap { it.dishes }
        assertTrue(allDishes.isNotEmpty(), "应解析出至少一道菜")

        // 打印所有菜名用于诊断
        val dishNames = allDishes.map { it.dish?.name ?: it.name }
        println("=== 诊断输出 ===")
        dishNames.forEachIndexed { i, n -> println(" 菜${i + 1}: '$n'") }

        // 各菜食材
        allDishes.forEach { dish ->
            val d = dish.dish
            if (d != null) {
                val ings = d.ingredients.map { it.ref ?: it.food?.name ?: "?" }
                println(" ${d.name} 食材: $ings")
            }
        }

        // 核心断言：菜名不应含"准备吃"
        dishNames.forEach { name ->
            assertTrue(!name.contains("准备吃"), "菜名不应含'准备吃'，实际: $name")
            assertTrue(!name.contains("准备"), "菜名不应含'准备'，实际: $name")
            assertTrue(!name.contains("吃"), "菜名不应含'吃'，实际: $name")
        }
    }

    // ═══════════════════════════════════════════════════
    // 更多"同类"变体
    // ═══════════════════════════════════════════════════

    @Test
    fun `中午想吃红烧肉`() {
        val result = RuleMealParser.parse("中午想吃红烧肉", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        println("想吃: $dishNames")
        dishNames.forEach { name ->
            assertTrue(!name.contains("想吃"), "菜名不应含'想吃'")
        }
    }

    @Test
    fun `晚上打算吃火锅`() {
        val result = RuleMealParser.parse("晚上打算吃火锅", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        println("打算吃: $dishNames")
        dishNames.forEach { name ->
            assertTrue(!name.contains("打算吃"), "菜名不应含'打算吃'")
        }
    }

    @Test
    fun `早餐要喝牛奶`() {
        val result = RuleMealParser.parse("早餐要喝牛奶", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        println("要喝: $dishNames")
        dishNames.forEach { name ->
            assertTrue(!name.contains("要喝"), "菜名不应含'要喝'")
        }
    }
}
