package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationCapabilityTest {
    @Test
    fun capabilityPreparationKeepsTraceAndFeedbackKeys() {
        val trace = RecommendationTrace(TraceId.fromTestValue("recommend-1"), "rule", "meal_edit", "reason-v1")
        val feedback = RecommendationFeedback(trace.traceId, "recommendation-1", RecommendationFeedbackAction.ACCEPTED, "health_fit")
        assertEquals(trace.traceId, feedback.traceId)
        assertEquals("reason-v1", trace.reasonModelVersion)
        assertEquals("health_fit", feedback.reasonCode)
    }
}
