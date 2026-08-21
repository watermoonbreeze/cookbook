package com.sxdbsm.cookbook.platform

import kotlinx.datetime.LocalDate

/** MDC3 debug-only trace for the meal revision-to-UI refresh chain. */
object MealDataTraceLogger {
    private const val TAG = "MDC3"

    fun repositoryWindowStarted(start: LocalDate, end: LocalDate) {
        CookbookDiag.log(TAG) { "[MDC3][Repository] observeTimelineWindow start=$start end=$end" }
    }

    fun revisionChanged(source: String, token: Any?) {
        CookbookDiag.log(TAG) { "[MDC3][Revision] $source changed token=$token" }
    }

    fun projectionGenerated(dateCount: Int, mealCount: Int) {
        CookbookDiag.log(TAG) { "[MDC3][Projection] generated dates=$dateCount meals=$mealCount" }
    }

    fun uiStateUpdated(feature: String, dates: String, cardCount: Int) {
        CookbookDiag.log(TAG) { "[MDC3][UI] feature=$feature dates=$dates cardCount=$cardCount" }
    }
}
