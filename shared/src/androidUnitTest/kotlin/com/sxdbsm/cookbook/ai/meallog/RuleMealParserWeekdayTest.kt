package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * K1c 集成测试：RuleMealParser.parse(text, today=...) → weekday → date_offset 路径。
 * 验证收合同 T-04 在完整调用链上生效。
 */
class RuleMealParserWeekdayTest {

    // ═══════════════════════════════════════════════════
    // weekday hint → date_offset（无相对词时走 weekday 兜底）
    // ═══════════════════════════════════════════════════

    @Test
    fun `周三文本今天周四 offset=-1`() {
        val today = LocalDate(2026, 8, 6) // 周四
        val result = RuleMealParser.parse("周三\n午餐吃了红烧肉和米饭", today = today)
        assertEquals(1, result.size)
        assertEquals(-1, result[0].date_offset)
    }

    @Test
    fun `周五文本今天周四 offset=-6即上周五`() {
        val today = LocalDate(2026, 8, 6) // 周四
        val result = RuleMealParser.parse("周五\n晚饭吃了饺子", today = today)
        assertEquals(1, result.size)
        assertEquals(-6, result[0].date_offset)
    }

    @Test
    fun `周四文本今天周四 offset=0即今天`() {
        val today = LocalDate(2026, 8, 6) // 周四
        val result = RuleMealParser.parse("周四\n中午吃了牛肉面", today = today)
        assertEquals(1, result.size)
        assertEquals(0, result[0].date_offset)
    }

    @Test
    fun `无日期无weekday 默认offset=0`() {
        val today = LocalDate(2026, 8, 6) // 周四
        val result = RuleMealParser.parse("午餐吃了红烧肉", today = today)
        assertEquals(1, result.size)
        assertEquals(0, result[0].date_offset)
    }

    // ═══════════════════════════════════════════════════
    // 相对词优先于 weekday
    // ═══════════════════════════════════════════════════

    @Test
    fun `昨天优先于块内的weekday`() {
        val today = LocalDate(2026, 8, 6) // 周四
        // 文本同时含"昨天"和"周三"，相对词应胜出
        val result = RuleMealParser.parse("周三 昨天午餐吃了红烧肉", today = today)
        assertEquals(1, result.size)
        assertEquals(-1, result[0].date_offset)
    }

    // ═══════════════════════════════════════════════════
    // 多天分段（weekday hint 各自对应正确 offset）
    // ═══════════════════════════════════════════════════

    @Test
    fun `多天分段各自weekday正确推算`() {
        val today = LocalDate(2026, 8, 6) // 周四
        val input = """
            周一
            午餐 红烧肉
            周三
            早餐 小米粥
        """.trimIndent()
        val result = RuleMealParser.parse(input, today = today)
        assertEquals(2, result.size)
        // 周四说周一 → -3；周四说周三 → -1
        assertEquals(-3, result[0].date_offset)
        assertEquals(-1, result[1].date_offset)
    }

    // ═══════════════════════════════════════════════════
    // 解析出菜品（确保不只是 offset 正确，数据完整）
    // ═══════════════════════════════════════════════════

    @Test
    fun `周三文本能解析出午餐和菜品`() {
        val today = LocalDate(2026, 8, 6) // 周四
        val result = RuleMealParser.parse("周三\n午餐吃了红烧肉", today = today)
        val meals = result.firstOrNull()?.meals ?: emptyList()
        assertTrue(meals.isNotEmpty(), "应解析出至少一个餐次")
        val dishes = meals.flatMap { it.dishes }
        assertTrue(dishes.isNotEmpty(), "应解析出至少一道菜")
    }
}
