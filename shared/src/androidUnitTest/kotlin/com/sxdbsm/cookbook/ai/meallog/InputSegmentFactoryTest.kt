package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.util.DateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * B4: InputSegmentFactory 纯单测。[AI生成]
 */
class InputSegmentFactoryTest {

    // ── forQuickRecord ──

    @Test
    fun `快速记 segmentId 格式正确`() {
        val result = InputSegmentFactory.forQuickRecord("午饭", LocalDate(2026, 8, 5))
        assertEquals(1, result.size)
        val seg = result.single()
        assertEquals("quick-2026-08-05", seg.segmentId)
        assertEquals(LocalDate(2026, 8, 5), seg.targetDate)
        assertEquals("午饭", seg.inputText)
        assertEquals(0, seg.ordinal)
        assertTrue(!seg.isBlank)
    }

    @Test
    fun `快速记 inputText 前后空格被 trim`() {
        val result = InputSegmentFactory.forQuickRecord("  午饭  ", LocalDate(2026, 8, 5))
        assertEquals("午饭", result.single().inputText)
    }

    @Test
    fun `快速记 空白输入 isBlank 为 true`() {
        val result = InputSegmentFactory.forQuickRecord("   ", LocalDate(2026, 8, 5))
        assertEquals(1, result.size) // 仍返回 1 个 segment
        assertTrue(result.single().isBlank)
    }

    // ── forPeriodicRecord ──

    @Test
    fun `周期记 生成 7 个 segment segmentId 格式正确`() {
        val anchor = LocalDate(2026, 8, 3) // 周一
        val texts = listOf("a", "b", "c", "d", "e", "f", "g")
        val result = InputSegmentFactory.forPeriodicRecord(texts, anchor)

        assertEquals(7, result.size)
        result.forEachIndexed { index, seg ->
            assertEquals("week-2026-08-03-day${index + 1}", seg.segmentId)
            assertEquals(DateTime.plusDays(anchor, index), seg.targetDate)
            assertEquals(index, seg.ordinal)
        }
    }

    @Test
    fun `周期记 空白段保留在列表中 isBlank 为 true`() {
        val anchor = LocalDate(2026, 8, 3)
        val texts = listOf("", "", "", "", "", "", "")
        val result = InputSegmentFactory.forPeriodicRecord(texts, anchor)

        assertEquals(7, result.size)
        result.forEach { assertTrue(it.isBlank) }
    }

    @Test
    fun `周期记 inputText 被 trim`() {
        val anchor = LocalDate(2026, 8, 3)
        val texts = listOf("  周一  ", "", "", "", "", "", "")
        val result = InputSegmentFactory.forPeriodicRecord(texts, anchor)

        assertEquals("周一", result[0].inputText)
    }

    @Test
    fun `周期记 跨年周 日期计算正确`() {
        // 2025-12-29 是周一，其周日为 2026-01-04
        val anchor = LocalDate(2025, 12, 29)
        val texts = List(7) { "" }
        val result = InputSegmentFactory.forPeriodicRecord(texts, anchor)

        assertEquals(LocalDate(2025, 12, 29), result[0].targetDate) // 周一
        assertEquals(LocalDate(2026, 1, 4), result[6].targetDate)   // 周日
        assertEquals("week-2025-12-29-day7", result[6].segmentId)
    }

    // ── mondayOfWeek ──

    @Test
    fun `mondayOfWeek 正常周`() {
        // 2026-08-05 是周三，所在周周一 = 2026-08-03
        assertEquals(
            LocalDate(2026, 8, 3),
            InputSegmentFactory.mondayOfWeek(LocalDate(2026, 8, 5))
        )
    }

    @Test
    fun `mondayOfWeek 当天已是周一`() {
        assertEquals(
            LocalDate(2026, 8, 3),
            InputSegmentFactory.mondayOfWeek(LocalDate(2026, 8, 3))
        )
    }

    @Test
    fun `mondayOfWeek 周日返回同周周一`() {
        // 2026-08-09 是周日，所在周周一 = 2026-08-03
        assertEquals(
            LocalDate(2026, 8, 3),
            InputSegmentFactory.mondayOfWeek(LocalDate(2026, 8, 9))
        )
    }

    @Test
    fun `mondayOfWeek 跨年周 12月31日`() {
        // 2025-12-31 是周三，所在周周一 = 2025-12-29
        assertEquals(
            LocalDate(2025, 12, 29),
            InputSegmentFactory.mondayOfWeek(LocalDate(2025, 12, 31))
        )
    }

    @Test
    fun `mondayOfWeek 跨年周 1月1日`() {
        // 2026-01-01 是周四，所在周周一 = 2025-12-29
        assertEquals(
            LocalDate(2025, 12, 29),
            InputSegmentFactory.mondayOfWeek(LocalDate(2026, 1, 1))
        )
    }
}
