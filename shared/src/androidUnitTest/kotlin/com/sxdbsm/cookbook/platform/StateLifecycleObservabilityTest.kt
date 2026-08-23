package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** State Lifecycle Observability R2 的 snapshot/restore/merge 契约测试。[AI生成] */
class StateLifecycleObservabilityTest {
    @Test
    fun lifecycleHelpersEmitStableEventsAndFields() {
        val events = mutableListOf<StructuredLogEvent>()
        installCookbookLogSink(object : CookbookLogSink {
            override fun emitLegacy(level: LogLevel, tag: String, message: String, throwable: Throwable?) = Unit
            override fun emitStructured(event: StructuredLogEvent) { events += event }
        })
        val traceId = TraceId.fromTestValue("state-r2-trace")
        BusinessTrace.stateSnapshotBeforeNavigation("unified_add_meal", "manual", "editing", traceId)
        BusinessTrace.stateRestore("ai_recommend", "success", "page_state,active_block", traceId)
        BusinessTrace.stateMergeResult("unified_add_meal", "editing", "ai_picked_dishes", "editing+dishes", traceId)

        val lifecycle = events.filterIsInstance<StructuredLogEvent.StateLifecycle>()
        assertEquals(3, lifecycle.size)
        assertEquals("state.snapshot.before_navigation", lifecycle[0].event)
        assertEquals("manual", lifecycle[0].currentTab)
        assertEquals("state.restore", lifecycle[1].event)
        assertEquals("page_state,active_block", lifecycle[1].restoredFields)
        assertEquals("state.merge.result", lifecycle[2].event)
        assertEquals("ai_picked_dishes", lifecycle[2].childResult)
        assertTrue(lifecycle.all { it.traceId == traceId })
    }

    @Test
    fun lifecycleJsonKeepsControlledCodesAndTraceId() {
        val line = StructuredLogJson.encode(
            StructuredLogEvent.StateLifecycle(
                level = LogLevel.DEBUG,
                event = "state.restore",
                traceId = TraceId.fromValue("state-r2-trace"),
                restoreSource = "new_dish",
                restoreResult = "success",
                restoredFields = "created_dish_active_block",
            ),
            timestampEpochMs = 10,
            sessionId = "session-1",
            sequence = 1,
        )
        assertTrue(line.contains("state.restore"))
        assertTrue(line.contains("state-r2-trace"))
        assertTrue(line.contains("created_dish_active_block"))
        assertTrue(!line.contains("redacted_invalid_code"))
    }
}
