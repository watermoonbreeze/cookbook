package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoggerTest {
    @Test
    fun structuredEventsUseOneSinkInOrder() {
        val events = mutableListOf<StructuredLogEvent>()
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
            override fun emitStructured(event: StructuredLogEvent) { events += event }
        })
        Logger.emit(StructuredLogEvent.UiState(LogLevel.INFO, "ui.opened", screen = "home"))
        Logger.emit(StructuredLogEvent.DataFlow(LogLevel.DEBUG, "data.loaded", stage = "meal", count = 2))
        assertEquals(listOf("ui.opened", "data.loaded"), events.map { it.event })
    }

    @Test
    fun jsonUsesFixedEnvelopeAndRedactsUnsafeCodes() {
        val line = StructuredLogJson.encode(
            StructuredLogEvent.DataFlow(LogLevel.INFO, "data.loaded", stage = "meal", result = "菜谱原文"),
            timestampEpochMs = 10,
            sessionId = "session-1",
            sequence = 3,
        )
        assertTrue(line.indexOf("schema") < line.indexOf("ts_epoch_ms"))
        assertTrue(line.contains("redacted_invalid_code"))
        assertTrue(!line.contains("菜谱原文"))
    }

    @Test
    fun businessTraceEmitsActionNavigationAndScreenWithOneTraceId() {
        val events = mutableListOf<StructuredLogEvent>()
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
            override fun emitStructured(event: StructuredLogEvent) { events += event }
        })
        val traceId = BusinessTrace.action("home", "open_ai_recommend", "next_meal")
        BusinessTrace.actionResult("home", "open_ai_recommend", ActionResultStatus.HANDLED, traceId)
        BusinessTrace.navigationStarted("home", "ai_recommend")
        BusinessTrace.navigationCompleted("home", "ai_recommend")
        BusinessTrace.screenEntered("ai_recommend")
        BusinessTrace.screenLoaded("ai_recommend")
        assertEquals(6, events.size)
        assertTrue(events.all { it.traceId == traceId })
        assertEquals("ui.click", events.first().event)
        assertEquals("ui.action.result", events[1].event)
        assertEquals(ActionResultStatus.HANDLED, (events[1] as StructuredLogEvent.ActionResult).result)
        assertEquals("screen.loaded", events.last().event)
    }

    @Test
    fun errorJsonContainsTypeButNotExceptionMessage() {
        val line = StructuredLogJson.encode(
            StructuredLogEvent.Error(
                traceId = TraceId.fromTestValue("trace-1"),
                operation = "recommend.load",
                errorType = "IOException",
            ),
            timestampEpochMs = 10,
            sessionId = "session-1",
            sequence = 4,
        )
        assertTrue(line.contains("operation.error"))
        assertTrue(line.contains("IOException"))
        assertTrue(!line.contains("message"))
    }

    @Test
    fun recommendRouteDiagnosticsKeepTheTwoMealEntryPointsDistinct() {
        val events = mutableListOf<StructuredLogEvent>()
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
            override fun emitStructured(event: StructuredLogEvent) { events += event }
        })
        val mealEdit = BusinessTrace.action("add_meal", "open_ai_recommend", "meal_edit")
        BusinessTrace.recommendRoute("ai_recommend", "meal_edit", mealEdit)
        val recordMeal = BusinessTrace.action("add_meal", "open_ai_recommend", "record_meal_manual")
        BusinessTrace.recommendRoute("ai_recommend", "record_meal_manual", recordMeal)
        val routes = events.filterIsInstance<StructuredLogEvent.DataFlow>().map { it.result }
        assertEquals(listOf("meal_edit", "record_meal_manual"), routes)
        assertTrue(events.filterIsInstance<StructuredLogEvent.DataFlow>().all { it.stage == "ai_recommend" })
    }
}
