package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TraceDiagnosticTest {
    @Test
    fun completeReturnFlowIsDiagnosed() {
        val trace = TraceId.fromTestValue("diagnostic-1")
        val events = listOf<StructuredLogEvent>(
            StructuredLogEvent.Action(LogLevel.INFO, traceId = trace, screen = "add_meal", action = "open_child", source = "manual"),
            StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.started", trace, "add_meal", "new_dish"),
            StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.snapshot.before_navigation", traceId = trace),
            StructuredLogEvent.Operation(LogLevel.DEBUG, "operation.succeeded", trace, "new_dish.save", OperationState.SUCCEEDED),
            StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.restore", traceId = trace),
            StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.merge.result", traceId = trace),
        )
        val result = TraceDiagnostic.diagnose(events)
        assertEquals(TraceDiagnostic.Status.COMPLETE, result.status)
        assertEquals(TraceDiagnostic.Finding.FLOW_PASS, result.finding)
        assertTrue(result.missing.isEmpty())
    }

    @Test
    fun missingRestoreIsVisibleInDiagnostic() {
        val trace = TraceId.fromTestValue("diagnostic-2")
        val result = TraceDiagnostic.diagnose(
            listOf(
                StructuredLogEvent.Action(LogLevel.INFO, traceId = trace, screen = "add_meal", action = "open_child", source = "manual"),
                StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.started", trace, "add_meal", "search"),
                StructuredLogEvent.Operation(LogLevel.DEBUG, "operation.succeeded", trace, "search", OperationState.SUCCEEDED),
                StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.snapshot.before_navigation", traceId = trace),
                StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.merge.result", traceId = trace),
            ),
        )
        assertEquals(TraceDiagnostic.Status.INCOMPLETE, result.status)
        assertTrue(TraceDiagnostic.Node.RESTORE_STATE in result.missing)
        assertTrue(result.summary.contains("RESTORE_STATE"))
        assertEquals(TraceDiagnostic.Finding.STATE_RESTORE_FAILURE, result.finding)
    }

    @Test
    fun diagnosticClassifiesNavigationMergeAndOperationFailures() {
        val trace = TraceId.fromTestValue("diagnostic-3")
        val base = listOf<StructuredLogEvent>(
            StructuredLogEvent.Action(LogLevel.INFO, traceId = trace, screen = "add_meal", action = "open_child", source = "manual"),
            StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.snapshot.before_navigation", traceId = trace),
        )
        assertEquals(TraceDiagnostic.Finding.NAVIGATION_FAILURE, TraceDiagnostic.diagnose(base).finding)
        val navigated = base + StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.started", trace, "add_meal", "new_dish") + StructuredLogEvent.Navigation(LogLevel.DEBUG, "navigation.completed", trace, "add_meal", "new_dish") + StructuredLogEvent.StateLifecycle(LogLevel.DEBUG, "state.restore", traceId = trace)
        assertEquals(TraceDiagnostic.Finding.STATE_MERGE_FAILURE, TraceDiagnostic.diagnose(navigated).finding)
        val failed = navigated + StructuredLogEvent.Operation(LogLevel.ERROR, "operation.failed", trace, "new_dish.save", OperationState.FAILED)
        assertEquals(TraceDiagnostic.Finding.OPERATION_FAILURE, TraceDiagnostic.diagnose(failed).finding)
    }
}
