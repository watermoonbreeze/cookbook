package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MealDataTraceLoggerTest {
    @Test
    fun mdc3EmitsOnlyStructuredSafeStageAndCount() {
        val events = mutableListOf<StructuredLogEvent>()
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
            override fun emitStructured(event: StructuredLogEvent) { events += event }
        })
        MealDataTraceLogger.projectionGenerated(2, 3)
        val event = events.single() as StructuredLogEvent.DataFlow
        assertEquals("meal_data.stage", event.event)
        assertEquals("projection.generated", event.stage)
        assertEquals(5, event.count)
        assertFalse(StructuredLogJson.encode(event, 1, "s", 1).contains("2026-"))
    }
}
