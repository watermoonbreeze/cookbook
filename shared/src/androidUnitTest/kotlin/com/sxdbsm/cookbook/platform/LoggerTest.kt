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
        BusinessTrace.navigationStarted("home", "ai_recommend")
        BusinessTrace.navigationCompleted("home", "ai_recommend")
        BusinessTrace.screenEntered("ai_recommend")
        BusinessTrace.screenLoaded("ai_recommend")
        assertEquals(5, events.size)
        assertTrue(events.all { it.traceId == traceId })
        assertEquals("ui.action.clicked", events.first().event)
        assertEquals("screen.loaded", events.last().event)
    }
}
