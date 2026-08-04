package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 2026-08-02 Bug修复回归测试：吃/喝动词剥离 + 软分隔修复 + 日期词剥离。
 */
class RuleMealParserRegressionTest {

    private val today = LocalDate(2026, 8, 2)

    // ═══════════════════════════════════════════════════
    // Bug 1 回归：吃/喝动词剥离
    // ═══════════════════════════════════════════════════

    @Test
    fun `吃了不被保留在菜名中`() {
        val result = RuleMealParser.parse("中午吃了红烧肉", today = today)
        val dishes = result.flatMap { it.meals }.flatMap { it.dishes }
        assertTrue(dishes.isNotEmpty(), "应解析出至少一道菜")
        dishes.forEach { dish ->
            val name = dish.dish?.name ?: dish.name
            assertTrue(!name.contains("吃了"), "菜名不应含'吃了'，实际: $name")
        }
    }

    @Test
    fun `喝动词不被保留`() {
        val result = RuleMealParser.parse("晚上喝了排骨汤", today = today)
        val dishes = result.flatMap { it.meals }.flatMap { it.dishes }
        dishes.forEach { dish ->
            val name = dish.dish?.name ?: dish.name
            assertTrue(!name.contains("喝"), "菜名不应含'喝'，实际: $name")
        }
    }

    @Test
    fun `刚吃动词不被保留`() {
        val result = RuleMealParser.parse("中午刚吃了饺子", today = today)
        val dishes = result.flatMap { it.meals }.flatMap { it.dishes }
        dishes.forEach { dish ->
            val name = dish.dish?.name ?: dish.name
            assertTrue(!name.contains("刚吃"), "菜名不应含'刚吃'，实际: $name")
        }
    }

    // ═══════════════════════════════════════════════════
    // Bug 2 回归：软分隔（验证菜名不含分隔词·不要求精确数量）
    // ═══════════════════════════════════════════════════

    @Test
    fun `和分隔后菜名不含和`() {
        val result = RuleMealParser.parse("午餐 红烧肉和青菜", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        assertTrue(dishNames.isNotEmpty())
        dishNames.forEach { name ->
            assertTrue(!name.contains("和"), "菜名不应含'和'，实际: $name")
        }
    }

    @Test
    fun `跟分隔后菜名不含跟`() {
        val result = RuleMealParser.parse("午餐 红烧肉跟青菜", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        dishNames.forEach { name ->
            assertTrue(!name.contains("跟"), "菜名不应含'跟'")
        }
    }

    @Test
    fun `然后分隔后菜名不含然后`() {
        val result = RuleMealParser.parse("午餐 红烧肉然后青菜", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        dishNames.forEach { name ->
            assertTrue(!name.contains("然后"), "菜名不应含'然后'")
        }
    }

    // ═══════════════════════════════════════════════════
    // 组合场景：Bug 1+2 叠加（核心回归）
    // ═══════════════════════════════════════════════════

    @Test
    fun `中午吃了红烧肉和米饭菜名不含吃了和和`() {
        val result = RuleMealParser.parse("中午吃了红烧肉和米饭", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        assertTrue(dishNames.isNotEmpty(), "应解析出至少一道菜")
        dishNames.forEach { name ->
            assertTrue(!name.contains("吃了"), "菜名不应含'吃了'，实际: $name")
            assertTrue(!name.contains("和"), "菜名不应含'和'，实际: $name")
        }
    }

    @Test
    fun `中午吃了土豆粉菜名为土豆粉不含吃了`() {
        val result = RuleMealParser.parse("中午吃了土豆粉", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        assertTrue(dishNames.isNotEmpty(), "应解析出至少一道菜")
        assertEquals("土豆粉", dishNames.first(), "菜名应为'土豆粉'")
        assertTrue(!dishNames.first().contains("吃了"))
    }

    @Test
    fun `早餐吃了两个鸡蛋和牛奶菜名不含吃了和和`() {
        val result = RuleMealParser.parse("早餐吃了两个鸡蛋和牛奶", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        assertTrue(dishNames.isNotEmpty())
        dishNames.forEach { name ->
            assertTrue(!name.contains("吃了"), "菜名不应含'吃了'，实际: $name")
            assertTrue(!name.contains("和"), "菜名不应含'和'，实际: $name")
        }
    }

    // ═══════════════════════════════════════════════════
    // 日期词剥离
    // ═══════════════════════════════════════════════════

    @Test
    fun `昨天红烧肉菜名不含昨天`() {
        val result = RuleMealParser.parse("昨天红烧肉", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        dishNames.forEach { name ->
            assertTrue(!name.contains("昨天"), "菜名不应含'昨天'，实际: $name")
        }
    }

    @Test
    fun `中文日期和显式时间不成为菜名且日期时间正确`() {
        val result = RuleMealParser.parse("八月十五号晚上七点半晚饭糖醋排骨和米饭", today = today)
        val day = result.single()
        assertEquals("2026-08-15", day.date)
        val meal = day.meals.single()
        assertEquals("19:30", meal.meal_time)
        meal.dishes.map { it.dish?.name ?: it.name }.forEach { name ->
            assertTrue(!name.contains("八月十五号") && !name.contains("七点半"), "日期时间不能成为菜名：$name")
        }
    }

    // ═══════════════════════════════════════════════════
    // 不该拆的保持不拆
    // ═══════════════════════════════════════════════════

    @Test
    fun `宫保鸡丁不被误拆`() {
        val result = RuleMealParser.parse("午餐 宫保鸡丁", today = today)
        val dishNames = result.flatMap { it.meals }.flatMap { it.dishes }
            .map { it.dish?.name ?: it.name }
        dishNames.forEach { name ->
            assertTrue(name.contains("宫保") || name == "宫保鸡丁",
                "宫保鸡丁不应被拆分，实际: $name")
        }
    }

    @Test
    fun `括号内加号不拆菜且提取配料`() {
        val result = RuleMealParser.parse("晚饭 凉皮（黄瓜丝+绿豆芽）+番茄炒蛋", today = today)
        val dishes = result.single().meals.single().dishes

        assertEquals(listOf("凉皮", "番茄炒蛋"), dishes.map { it.name })
        assertEquals(
            listOf("凉皮", "黄瓜丝", "绿豆芽"),
            dishes.first().dish?.ingredients?.mapNotNull { it.food?.name },
        )
    }
}
