package com.sxdbsm.cookbook.platform

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TraceModelTest {
    @Test
    fun traceHasIndependentIdAndControlledDuration() = runBlocking {
        var now = 100L
        val events = mutableListOf<StructuredLogEvent>()
        val clock = MonotonicTimeSource { now }
        val first = OperationTrace(TraceId.create(), "load", { events += it }, clock)
        val second = OperationTrace(TraceId.create(), "load", { events += it }, clock)
        assertNotEquals(first.traceId, second.traceId)
        assertTrue(first.start())
        now += 1234
        assertTrue(first.succeed())
        assertEquals(OperationState.SUCCEEDED, first.state)
        assertFalse(first.cancel())
        assertEquals(1234L, events.filterIsInstance<StructuredLogEvent.Performance>().single().durationMs)
    }

    @Test
    fun terminalRaceIsFirstWinsAndCancelIsNotFailure() = runBlocking {
        val events = mutableListOf<StructuredLogEvent>()
        val trace = OperationTrace(TraceId.fromTestValue("test-trace"), "sync", { events += it }, MonotonicTimeSource { 1L })
        assertTrue(trace.start())
        assertTrue(trace.cancel())
        assertFalse(trace.fail("IllegalStateException"))
        assertEquals(OperationState.CANCELLED, trace.state)
        assertEquals(1, events.filterIsInstance<StructuredLogEvent.Operation>().count { it.event == "operation.cancelled" })
        assertEquals(0, events.filterIsInstance<StructuredLogEvent.Operation>().count { it.state == OperationState.FAILED })
    }
}
