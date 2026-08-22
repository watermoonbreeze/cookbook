package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TraceEventContractTest {
    @Test
    fun allProducedEventNamesAreRegistered() {
        val events = listOf(
            "ui.click", "ui.action.result", "navigation.started", "navigation.completed",
            "screen.entered", "screen.loaded", "state.changed", "recommend.route",
            "operation.started", "operation.dropped", "operation.succeeded", "operation.failed",
            "operation.cancelled", "operation.duration", "operation.error", "system.crash", "legacy.log",
        )
        assertTrue(events.all(TraceEventContract::isKnown))
        assertEquals(events.size, TraceEventContract.knownEvents.size)
    }

    @Test
    fun undefinedEventIsRejectedByContractProbe() {
        assertFalse(TraceEventContract.isKnown("recommend.result_with_free_text"))
    }
}
