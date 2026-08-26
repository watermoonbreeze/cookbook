package com.sxdbsm.cookbook.android.ui.nav

import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.util.UUID

class TraceRouteContractTest {
    @Test
    fun allAiRecommendRouteBuildersCarryTheProvidedUuid() {
        val id = UUID.randomUUID().toString()
        val direct = Routes.aiRecommend(id)
        val meal = Routes.aiRecommendForMeal("LUNCH", id)
        assertTrue(direct.contains("traceId=$id"))
        assertTrue(meal.contains("traceId=$id"))
    }

    @Test
    fun routeTraceValueMustRemainAUuidAndCannotUseTheLegacyDefault() {
        val id = UUID.randomUUID().toString()
        assertEquals(id, requireAiRecommendRouteTrace(id).value)
        assertThrows(IllegalArgumentException::class.java) { requireAiRecommendRouteTrace("not-a-uuid") }
        assertFalse(Routes.AI_RECOMMEND.contains("traceId=default"))
        assertTrue(Routes.AI_RECOMMEND.contains("traceId={traceId}"))
    }
}
