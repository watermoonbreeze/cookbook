package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @File : TextSegmenterTest
 * @Time : 2026/08/02
 * @Author : SXD-AI
 * @Desc : TextSegmenter 单测——weekdayToDateOffset / parseWeekdayHint / segment 分段
 * <p>
 * [AI生成] P2-1 K1c：weekday→date_offset 推算单测（验收合同 T-04）
 **/
class TextSegmenterTest {

    // ═══════════════════════════════════════════════════
    // weekdayToDateOffset（T-04 核心）
    // ═══════════════════════════════════════════════════

    @Test
    fun `今天周四说周三 offset=-1`() {
        val today = LocalDate(2026, 8, 6) // 周四
        assertEquals(-1, TextSegmenter.weekdayToDateOffset(3, today)) // 周三=3
    }

    @Test
    fun `今天周四说周二 offset=-2`() {
        val today = LocalDate(2026, 8, 6) // 周四
        assertEquals(-2, TextSegmenter.weekdayToDateOffset(2, today))
    }

    @Test
    fun `今天周四说周五 offset=-6（最近过去的周五=上周五）`() {
        val today = LocalDate(2026, 8, 6) // 周四
        // 最近的周五是上周五 (-6)
        assertEquals(-6, TextSegmenter.weekdayToDateOffset(5, today))
    }

    @Test
    fun `今天周四说周四 offset=0（今天）`() {
        val today = LocalDate(2026, 8, 6) // 周四
        assertEquals(0, TextSegmenter.weekdayToDateOffset(4, today))
    }

    @Test
    fun `今天周一说周日 offset=-1`() {
        val today = LocalDate(2026, 8, 3) // 周一
        assertEquals(-1, TextSegmenter.weekdayToDateOffset(7, today))
    }

    @Test
    fun `今天周日说周六 offset=-1`() {
        val today = LocalDate(2026, 8, 9) // 周日
        assertEquals(-1, TextSegmenter.weekdayToDateOffset(6, today))
    }

    // ═══════════════════════════════════════════════════
    // weekdayToIso（中文→数字）
    // ═══════════════════════════════════════════════════

    @Test
    fun `周一→1`() = assertEquals(1, TextSegmenter.weekdayToIso("周一"))

    @Test
    fun `周三→3`() = assertEquals(3, TextSegmenter.weekdayToIso("周三"))

    @Test
    fun `星期六→6`() = assertEquals(6, TextSegmenter.weekdayToIso("星期六"))

    @Test
    fun `礼拜天→7`() = assertEquals(7, TextSegmenter.weekdayToIso("礼拜天"))

    @Test
    fun `周日→7`() = assertEquals(7, TextSegmenter.weekdayToIso("周日"))

    @Test
    fun `null输入→null`() = assertNull(TextSegmenter.weekdayToIso(null))

    // ═══════════════════════════════════════════════════
    // segment 分段基本能力
    // ═══════════════════════════════════════════════════

    @Test
    fun `无日期标记→单块`() {
        val blocks = TextSegmenter.segment("午餐吃了红烧肉和米饭")
        assertEquals(1, blocks.size)
        assertNull(blocks[0].weekdayHint)
    }

    @Test
    fun `周三标记→提取 weekdayHint`() {
        val blocks = TextSegmenter.segment("周三\n午餐吃了红烧肉")
        assertEquals(1, blocks.size)
        assertEquals("周三", blocks[0].weekdayHint)
    }

    @Test
    fun `中文数字日期行→提取并剥离日期`() {
        val blocks = TextSegmenter.segment("八月十五号午餐红烧肉和米饭")
        assertEquals(1, blocks.size)
        assertEquals("八月十五号", blocks[0].dateHint)
        assertEquals("午餐红烧肉和米饭", blocks[0].text)
    }

    @Test
    fun `多天分段`() {
        val input = """
            周一
            午餐 红烧肉
            周三
            早餐 小米粥
        """.trimIndent()
        val blocks = TextSegmenter.segment(input)
        assertEquals(2, blocks.size)
        assertEquals("周一", blocks[0].weekdayHint)
        assertEquals("周三", blocks[1].weekdayHint)
    }
}
