package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * B3: StreamingMealSession reducer 纯测试。
 */
class StreamingMealSessionTest {

    private fun seg(id: String, date: LocalDate, text: String, ord: Int) =
        InputSegment(id, date, text, ord)

    @Test
    fun `多段按ordinal串行取段 前段完成才取下一段`() {
        val request = StreamingMealRequest(
            segments = listOf(
                seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0),
                seg("s-2", LocalDate(2026, 8, 6), "周二晚餐面条", 1),
            ),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)

        val first = session.nextSegment()
        assertEquals("s-1", first?.segmentId)
        assertEquals(StreamSegmentState.STREAMING, session.snapshot().segmentStates["s-1"])
        // 第二段尚未开始
        assertNull(session.snapshot().segmentStates["s-2"])

        session.onCompleted("s-1", "stop")
        assertEquals(StreamSegmentState.COMPLETED, session.snapshot().segmentStates["s-1"])
        // 取第二段
        val second = session.nextSegment()
        assertEquals("s-2", second?.segmentId)

        session.onCompleted("s-2", "stop")
        val snap = session.snapshot()
        assertTrue(snap.isTerminal, "全部段终态后 isTerminal=true")
    }

    @Test
    fun `Delta只喂当前STREAMING段`() {
        val request = StreamingMealRequest(
            segments = listOf(
                seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0),
                seg("s-2", LocalDate(2026, 8, 6), "周二晚餐面条", 1),
            ),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        session.nextSegment() // s-1

        // 当前段 Delta
        session.onDelta("s-1", """{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        // 非当前段 Delta 忽略（s-2 未开始）
        session.onDelta("s-2", """{"type":"meal","segment_id":"s-2","meal_id":"2026-08-06|dinner","date":"2026-08-06","slot":"dinner"}""" + "\n")

        val snap = session.snapshot()
        assertTrue(snap.days.isEmpty() || snap.days.none { it.date == "2026-08-06" }, "s-2 事件不得混入 s-1 会话")
    }

    @Test
    fun `全段失败 snapshot有合法餐食时hasValidMeals仍为false但isTerminal为true`() {
        val request = StreamingMealRequest(
            segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周一", 0)),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        session.nextSegment()
        session.onFailed("s-1", "HTTP 500 STREAM_HTTP_ERROR")

        val snap = session.snapshot()
        assertFalse(snap.hasValidMeals)
        assertTrue(snap.isTerminal)
        assertTrue(snap.diagnostics.any { it.message.contains("HTTP 500") })
    }

    @Test
    fun `Delta到合法dish后hasValidMeals为true且days非空`() {
        val request = StreamingMealRequest(
            segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0)),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        session.nextSegment()
        session.onDelta("s-1", """{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        session.onDelta("s-1", """{"type":"dish","segment_id":"s-1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")

        val snap = session.snapshot()
        assertTrue(snap.hasValidMeals)
        assertEquals(1, snap.days.size)
        assertEquals("米饭", snap.days.single().meals.single().dishes.single().name)
    }

    @Test
    fun `T-B3-02 逆序ordinal声明 首取ordinal0 终态前二次nextSegment为null`() {
        // 声明顺序为 ordinal=1/0（逆序），必须按 ordinal 升序取段
        val request = StreamingMealRequest(
            segments = listOf(
                seg("s-2", LocalDate(2026, 8, 6), "周二", 1),
                seg("s-1", LocalDate(2026, 8, 5), "周一", 0),
            ),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)

        // 首取必须 ordinal=0
        val first = session.nextSegment()
        assertEquals("s-1", first?.segmentId)

        // 当前段 STREAMING 时再次取段为 null
        assertNull(session.nextSegment(), "STREAMING 中不得取下一段")

        // Completed 前再次取段仍为 null（同段未推进）
        assertNull(session.nextSegment())

        // 标 Completed 后才能取 ordinal=1
        session.onCompleted("s-1", "stop")
        val second = session.nextSegment()
        assertEquals("s-2", second?.segmentId)

        // 第二段 STREAMING 时取段为 null
        assertNull(session.nextSegment())
        // Failed 后终态，无更多段
        session.onFailed("s-2", "HTTP 500")
        assertNull(session.nextSegment())

        // R3-04: 独立 session 验证 Failed 门控——首段 STREAMING 时 null，onFailed 后才取下一段
        val request2 = StreamingMealRequest(
            segments = listOf(
                seg("b-2", LocalDate(2026, 8, 6), "周二", 1),
                seg("b-1", LocalDate(2026, 8, 5), "周一", 0),
            ),
            generationId = "g2",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session2 = StreamingMealSession(request2)
        assertEquals("b-1", session2.nextSegment()?.segmentId)
        assertNull(session2.nextSegment(), "STREAMING 中不得推进")
        // 首段 onFailed 后，下一次才返回 ordinal=1
        session2.onFailed("b-1", "HTTP 500")
        assertEquals("b-2", session2.nextSegment()?.segmentId)
    }

    @Test
    fun `T-B3-04 第一段合法第二段失败 保留前缀且isTerminal`() {
        val request = StreamingMealRequest(
            segments = listOf(
                seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0),
                seg("s-2", LocalDate(2026, 8, 6), "周二晚餐面条", 1),
            ),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        session.nextSegment() // s-1
        session.onDelta("s-1", """{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        session.onDelta("s-1", """{"type":"dish","segment_id":"s-1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        session.onCompleted("s-1", "stop")

        session.nextSegment() // s-2
        session.onFailed("s-2", "HTTP 500 STREAM_HTTP_ERROR")

        val snap = session.snapshot()
        // 合法前缀保留：days 含 first dish
        assertTrue(snap.hasValidMeals)
        assertTrue(snap.isTerminal)
        assertEquals("米饭", snap.days.single().meals.single().dishes.single().name)
        // 第二段失败诊断
        assertTrue(snap.diagnostics.any { it.message.contains("HTTP 500") })
    }

    @Test
    fun `generationId透传`() {
        val request = StreamingMealRequest(
            segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周一", 0)),
            generationId = "meal-7",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        assertEquals("meal-7", session.snapshot().generationId)
    }

    // ═══════════════════════════════ AF-ARCH-01 ═══════════════════════════════

    @Test
    fun `T-AF-ARCH-01 done事件静默消费不产生警告`() {
        val request = StreamingMealRequest(
            segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0)),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        session.nextSegment()

        // 正常 meal + dish + done
        session.onDelta("s-1", """{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        session.onDelta("s-1", """{"type":"dish","segment_id":"s-1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        session.onDelta("s-1", """{"type":"done","segment_id":"s-1","summary":"完成"}""" + "\n")

        val snap = session.snapshot()
        // done 不应产生"未知事件类型"警告
        val unknownWarnings = snap.diagnostics.filter {
            it.message.contains("未知事件类型") && it.message.contains("done")
        }
        assertTrue(unknownWarnings.isEmpty(), "done 事件不应产生'未知事件类型'警告，实际: $unknownWarnings")
        // 合法餐食仍正确
        assertTrue(snap.hasValidMeals)
    }

    // ═══════════════════════════════ AF-ARCH-02 ═══════════════════════════════

    @Test
    fun `T-AF-ARCH-02 多段各自独立parser 第一段合法第二段也合法 两段days不互相污染`() {
        val request = StreamingMealRequest(
            segments = listOf(
                seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0),
                seg("s-2", LocalDate(2026, 8, 6), "周二晚餐面条", 1),
            ),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)

        // 第一段
        session.nextSegment()
        session.onDelta("s-1", """{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        session.onDelta("s-1", """{"type":"dish","segment_id":"s-1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        session.onCompleted("s-1", "stop")

        // 第二段
        session.nextSegment()
        session.onDelta("s-2", """{"type":"meal","segment_id":"s-2","meal_id":"2026-08-06|dinner","date":"2026-08-06","slot":"dinner"}""" + "\n")
        session.onDelta("s-2", """{"type":"dish","segment_id":"s-2","meal_id":"2026-08-06|dinner","dish_id":"2026-08-06|dinner|d1","name":"面条"}""" + "\n")
        session.onCompleted("s-2", "stop")

        val snap = session.snapshot()
        assertTrue(snap.isTerminal)
        assertTrue(snap.hasValidMeals)

        // 两天的 days 应各自独立
        val dates = snap.days.map { it.date }.toSet()
        assertEquals(2, dates.size, "两段应产出两个不同日期的 day")
        assertTrue("2026-08-05" in dates)
        assertTrue("2026-08-06" in dates)

        // 第一天的 dish 是米饭，不是面条
        val day1 = snap.days.first { it.date == "2026-08-05" }
        assertEquals("米饭", day1.meals.single().dishes.single().name)

        val day2 = snap.days.first { it.date == "2026-08-06" }
        assertEquals("面条", day2.meals.single().dishes.single().name)
    }

    @Test
    fun `T-AF-ARCH-02 多段第二段失败 第一段数据保留 诊断含第二段失败信息`() {
        val request = StreamingMealRequest(
            segments = listOf(
                seg("s-1", LocalDate(2026, 8, 5), "周一午餐米饭", 0),
                seg("s-2", LocalDate(2026, 8, 6), "周二晚餐面条", 1),
            ),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)

        session.nextSegment()
        session.onDelta("s-1", """{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        session.onDelta("s-1", """{"type":"dish","segment_id":"s-1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        session.onCompleted("s-1", "stop")

        session.nextSegment()
        session.onFailed("s-2", "HTTP 500")

        val snap = session.snapshot()
        assertTrue(snap.isTerminal)
        assertTrue(snap.hasValidMeals) // 第一段仍合法

        // 诊断含第二段失败
        assertTrue(snap.diagnostics.any { it.message.contains("HTTP 500") && it.segmentId == "s-2" })

        // 第一段数据完好
        assertEquals("米饭", snap.days.single().meals.single().dishes.single().name)
    }
}
