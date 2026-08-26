package com.sxdbsm.cookbook.platform

/** Stable event-name registry for structured Trace analysis. */
object TraceEventContract {
    val knownEvents: Set<String> = setOf(
        "ui.click", "ui.action.result", "navigation.started", "navigation.completed",
        "screen.entered", "screen.loaded", "state.changed",
        "state.snapshot.before_navigation", "state.restore", "state.merge.result", "recommend.route",
        "operation.started", "operation.dropped", "operation.succeeded", "operation.failed",
        "operation.cancelled", "operation.duration", "operation.error", "system.crash", "system.session_started", "legacy.log",
        "meal_data.stage",
    )

    fun isKnown(event: String): Boolean = event in knownEvents
}
