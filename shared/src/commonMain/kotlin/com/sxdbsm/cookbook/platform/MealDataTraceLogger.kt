package com.sxdbsm.cookbook.platform

import kotlinx.datetime.LocalDate
/** MDC3 debug-only trace for the meal revision-to-UI refresh chain. */
object MealDataTraceLogger {
    private const val EVENT = "meal_data.stage"

    fun repositoryWindowStarted(@Suppress("UNUSED_PARAMETER") start: LocalDate, @Suppress("UNUSED_PARAMETER") end: LocalDate) {
        emit("repository.window")
    }

    fun revisionChanged(source: String, @Suppress("UNUSED_PARAMETER") token: Any?) {
        emit("revision.$source")
    }

    fun projectionGenerated(dateCount: Int, mealCount: Int) {
        emit("projection.generated", dateCount.toLong() + mealCount)
    }

    fun uiStateUpdated(feature: String, @Suppress("UNUSED_PARAMETER") dates: String, cardCount: Int) {
        emit("ui.$feature", cardCount.toLong())
    }

    private fun emit(stage: String, count: Long? = null) {
        Logger.emit(StructuredLogEvent.DataFlow(LogLevel.DEBUG, EVENT, stage = stage, count = count))
    }
}
