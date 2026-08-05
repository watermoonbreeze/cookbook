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
    fun `cancel后不再接受新段且当前段标记CANCELLED`() {
        val request = StreamingMealRequest(
            segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周一", 0)),
            generationId = "g1",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        session.nextSegment()
        session.cancel()

        assertEquals(StreamSegmentState.CANCELLED, session.snapshot().segmentStates["s-1"])
        assertNull(session.nextSegment(), "取消后无下一段")
        assertTrue(session.snapshot().isTerminal)
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
    fun `generationId透传`() {
        val request = StreamingMealRequest(
            segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周一", 0)),
            generationId = "meal-7",
            weekAnchor = LocalDate(2026, 8, 3),
        )
        val session = StreamingMealSession(request)
        assertEquals("meal-7", session.snapshot().generationId)
    }
}
