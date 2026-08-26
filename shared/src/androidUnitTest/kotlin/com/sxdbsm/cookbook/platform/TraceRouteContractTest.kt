package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class TraceRouteContractTest {
    @Test
    fun routeTraceRequiresTheExplicitActionTraceId() {
        val events = mutableListOf<StructuredLogEvent>()
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
            override fun emitStructured(event: StructuredLogEvent) { events += event }
        })
        val trace = BusinessTrace.action("add_meal", "open_ai_recommend", "meal_edit")
        BusinessTrace.navigationStarted("add_meal", "ai_recommend", trace)
        BusinessTrace.recommendRoute("ai_recommend", "meal_edit", trace)
        assertEquals(listOf(trace, trace, trace), events.map { it.traceId })
    }
}
