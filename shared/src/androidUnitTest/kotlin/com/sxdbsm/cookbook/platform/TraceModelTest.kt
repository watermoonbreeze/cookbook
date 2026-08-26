package com.sxdbsm.cookbook.platform

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
    fun failedOperationEmitsSafeErrorAndPerformanceWithSameTraceId() = runBlocking {
        var now = 10L
        val events = mutableListOf<StructuredLogEvent>()
        val traceId = TraceId.fromTestValue("trace-error-1")
        val trace = OperationTrace(traceId, "load", { events += it }, MonotonicTimeSource { now })
        assertTrue(trace.start())
        now = 42L
        assertTrue(trace.fail("java.io.IOException"))
        val error = events.filterIsInstance<StructuredLogEvent.Error>().single()
        assertEquals(traceId, error.traceId)
        assertEquals("IOException", error.errorType)
        assertEquals(32L, events.filterIsInstance<StructuredLogEvent.Performance>().single().durationMs)
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

    @Test
    fun concurrentSucceedFailAndCancelHaveExactlyOneTerminalWinner() = runBlocking {
        val events = mutableListOf<StructuredLogEvent>()
        val trace = OperationTrace(
            TraceId.fromTestValue("concurrent-trace"),
            "sync",
            { event -> synchronized(events) { events += event } },
            MonotonicTimeSource { 1L },
        )
        assertTrue(trace.start())
        val release = CompletableDeferred<Unit>()
        val ready = List(3) { CompletableDeferred<Unit>() }

        val outcomes = coroutineScope {
            listOf(
                async(Dispatchers.Default) { ready[0].complete(Unit); release.await(); trace.succeed() },
                async(Dispatchers.Default) { ready[1].complete(Unit); release.await(); trace.fail("IOException") },
                async(Dispatchers.Default) { ready[2].complete(Unit); release.await(); trace.cancel() },
            ).also {
                ready.forEach { it.await() }
                release.complete(Unit)
            }.awaitAll()
        }

        assertEquals(1, outcomes.count { it })
        assertEquals(1, events.filterIsInstance<StructuredLogEvent.Operation>().count {
            it.event in setOf("operation.succeeded", "operation.failed", "operation.cancelled")
        })
        assertEquals(1, events.filterIsInstance<StructuredLogEvent.Performance>().count { it.event == "operation.duration" })
        if (trace.state == OperationState.CANCELLED) {
            assertEquals(0, events.filterIsInstance<StructuredLogEvent.Error>().size)
        }
    }

    @Test
    fun independentOperationsNeverShareTraceOrTerminalState() = runBlocking {
        val events = mutableListOf<StructuredLogEvent>()
        val first = OperationTrace(TraceId.fromTestValue("trace-a"), "a", { events += it }, MonotonicTimeSource { 1 })
        val second = OperationTrace(TraceId.fromTestValue("trace-b"), "b", { events += it }, MonotonicTimeSource { 1 })
        assertTrue(first.start())
        assertTrue(second.start())
        assertTrue(first.succeed())
        assertTrue(second.fail("IOException"))
        assertEquals(1, events.filterIsInstance<StructuredLogEvent.Operation>().count { it.traceId == first.traceId && it.event == "operation.succeeded" })
        assertEquals(1, events.filterIsInstance<StructuredLogEvent.Operation>().count { it.traceId == second.traceId && it.event == "operation.failed" })
    }

    @Test
    fun operationFirstTerminalWins() = runBlocking {
        val events = mutableListOf<StructuredLogEvent>()
        val trace = OperationTrace(TraceId.fromTestValue("save-trace"), "meal.save", { events += it }, MonotonicTimeSource { 1 })
        assertTrue(trace.start())
        assertTrue(trace.succeed())
        assertFalse(trace.fail("IOException"))
        assertEquals(1, events.filterIsInstance<StructuredLogEvent.Operation>().count { it.event == "operation.succeeded" || it.event == "operation.failed" || it.event == "operation.cancelled" })
    }
}
