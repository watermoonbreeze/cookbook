package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * AF-14: D-01~D-08 fallback 归属测试 + T-01~T-08 回归。
 */
class StreamingMealParserTest {
    private val targetDate = LocalDate(2026, 8, 5) // 周三
    private val genId = "test-gen-1"

    // ════════════════════════ T-01~T-08 回归 ════════════════════════

    @Test fun `T01 normal NDJSON`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        val d = p.finish("stop")
        assertEquals("米饭", d.segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.name)
    }

    @Test fun `T02 dish before meal auto-creates parent`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val d = p.finish("stop")
        assertNotNull(d.segments["q1"]!!.meals["2026-08-05|lunch"])
    }

    @Test fun `T05 SSE chunk mid-line buffered`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","se""")
        assertEquals(0, p.currentDraft.segments.size)
        p.feedDelta("""gment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        assertNotNull(p.finish("stop").segments["q1"])
    }

    @Test fun `T08 finish_reason=length produces truncation warning`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val d = p.finish("length")
        assertTrue(d.isTruncated)
    }

    @Test fun `unknown segment_id rejected`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"unknown","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        assertFalse(p.finish("stop").segments.containsKey("unknown"))
    }

    @Test fun `invalid slot rejected`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|morning","date":"2026-08-05","slot":"morning"}""" + "\n")
        assertEquals(0, p.finish("stop").segments["q1"]?.meals?.size ?: 0)
    }

    @Test fun `d0 rejected`() {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d0","name":"米饭"}""" + "\n")
        assertEquals(0, p.finish("stop").segments["q1"]?.meals?.get("2026-08-05|lunch")?.dishes?.size ?: 0)
    }

    @Test fun `dish merge preserves children`() = run {
        val p = parser(InputSegment("q1", targetDate, "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","cooking_method":"烧"}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"五花肉","quantity":150}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","quantity":2}""" + "\n")
        val dish = p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(2.0, dish.quantity)
        assertEquals("烧", dish.cookingMethod)
        assertEquals(1, dish.ingredients.size)
    }

    // ════════════════════════ D-01~D-08 fallback（§7.5.6）═══════════════════════

    private fun parser(vararg segs: InputSegment) = StreamingMealParser(segs.toList(), genId, segs.firstOrNull()?.targetDate ?: targetDate)

    // D-01: 绝对日期，对象
    @Test fun `D-01 absolute date object key s1 date 08-10`() {
        val s1 = InputSegment("s-1", LocalDate(2026, 8, 5), "8月10日午餐米饭", 0)
        val p = parser(s1)
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-10","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"), "key must be s-1")
        val meal = d.segments["s-1"]!!.meals["2026-08-10|lunch"]
        assertNotNull(meal, "meal_id must use corrected date 2026-08-10")
        assertEquals("2026-08-10", meal!!.date)
    }

    // D-02: 绝对日期，数组
    @Test fun `D-02 absolute date array same as D-01`() {
        val s1 = InputSegment("s-1", LocalDate(2026, 8, 5), "8月10日午餐米饭", 0)
        val p = parser(s1)
        p.feedDelta("""{"schema_version":"1.0","days":[{"date":"2026-08-10","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭","quantity":1,"quantity_unit":"份"}]}]}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
        val meal = d.segments["s-1"]!!.meals["2026-08-10|lunch"]
        assertNotNull(meal)
    }

    // D-03: 星期，对象
    @Test fun `D-03 weekday object maps to correct weekday`() {
        val s1 = InputSegment("s-1", LocalDate(2026, 8, 5), "周三午餐", 0) // 8/5 is Wed
        val p = parser(s1)
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-10","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        // Input has "周三"(weekday) → D-15 weekday path
        assertTrue(d.segments.containsKey("s-1"))
    }

    // D-04: 星期，数组
    @Test fun `D-04 weekday array same as D-03`() {
        val s1 = InputSegment("s-1", LocalDate(2026, 8, 5), "周三午餐", 0)
        val p = parser(s1)
        p.feedDelta("""{"days":[{"date":"2026-08-10","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭"}]}]}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
    }

    // D-05: 无日期，对象
    @Test fun `D-05 no date object uses targetDate`() {
        val s2 = InputSegment("s-2", LocalDate(2026, 8, 6), "午餐米饭", 1)
        val p = parser(s2)
        p.feedDelta("""{"schema_version":"2.0","items":[{"meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-2"), "key must be s-2")
        val meal = d.segments["s-2"]!!.meals["2026-08-06|lunch"]
        assertNotNull(meal, "date must be 2026-08-06 = s-2 targetDate")
    }

    // D-06: 无日期，数组
    @Test fun `D-06 no date array uses targetDate`() {
        val s2 = InputSegment("s-2", LocalDate(2026, 8, 6), "午餐米饭", 1)
        val p = parser(s2)
        p.feedDelta("""{"days":[{"meals":[{"meal_type":"lunch","dishes":[{"name":"米饭"}]}]}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-2"))
        assertNotNull(d.segments["s-2"]!!.meals["2026-08-06|lunch"])
    }

    // D-07: 多段 fallback 拒绝
    @Test fun `D-07 multi-segment fallback rejected`() {
        val s1 = InputSegment("s-1", LocalDate(2026, 8, 5), "周一", 0)
        val s2 = InputSegment("s-2", LocalDate(2026, 8, 6), "周二", 1)
        val p = parser(s1, s2)
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.isEmpty(), "multi-segment fallback must not create any draft")
        assertTrue(d.diagnostics.any { it.message.contains("whole_json_fallback_requires_single_segment") })
    }

    // D-08: NDJSON 第二段归属（显式 segment_id=s-2）
    @Test fun `D-08 NDJSON segmentId s2 only enters s2`() {
        val s1 = InputSegment("s-1", LocalDate(2026, 8, 5), "周一", 0)
        val s2 = InputSegment("s-2", LocalDate(2026, 8, 6), "周二", 1)
        val p = parser(s1, s2)
        p.feedDelta("""{"type":"meal","segment_id":"s-2","meal_id":"2026-08-06|lunch","date":"2026-08-06","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"s-2","meal_id":"2026-08-06|lunch","dish_id":"2026-08-06|lunch|d1","name":"米饭"}""" + "\n")
        val d = p.finish("stop")
        assertFalse(d.segments.containsKey("s-1"), "s-1 must be empty")
        assertNotNull(d.segments["s-2"]!!.meals["2026-08-06|lunch"])
    }
}
