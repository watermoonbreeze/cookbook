package com.sxdbsm.cookbook.android.ui.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class AiRecommendTraceBoundaryTest {
    @Test
    fun screenProvidedTraceIsConsumedWithoutReplacement() {
        val id = UUID.randomUUID().toString()
        assertEquals(id, consumeInitialTraceId(id).value)
        assertThrows(IllegalArgumentException::class.java) { consumeInitialTraceId("missing") }
    }

    @Test
    fun initialOperationUsesEntryTraceAndFollowUpUsesANewTrace() {
        val entry = consumeInitialTraceId(UUID.randomUUID().toString())
        val session = RecommendTraceSession(entry)
        val initial = session.nextOperation()
        val followUp = session.nextOperation()

        assertEquals(entry, initial.traceId)
        assertNotEquals(initial.traceId, followUp.traceId)
    }
}
