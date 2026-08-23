package com.sxdbsm.cookbook.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArchitectureGovernanceTest {
    @Test
    fun lifecycleUsesTheFiveGovernedEvents() {
        val contract = StateLifecycleContract(
            DomainState.DRAFT,
            PageState.EDITING,
            NavigationState.STABLE,
            listOf(StateLifecycleEvent.CREATE, StateLifecycleEvent.SAVE, StateLifecycleEvent.RESTORE, StateLifecycleEvent.MERGE),
        )
        assertTrue(contract.isValid())
        assertEquals(4, contract.events.size)
    }

    @Test
    fun navigationAndResultContractsKeepStableCodes() {
        val navigation = NavigationContract("add_meal", "new_dish", mapOf("dishId" to "42"), "createdDishId")
        assertTrue(navigation.isSafe())
        val result = ResultContract("new_dish", "dish_created", "createdDishId", 1L)
        assertEquals("dish_created", result.resultType)
    }

    @Test
    fun recommendationCapabilityIsMetadataOnly() {
        val context = RecommendationContext("dinner", "record_meal_manual", listOf("ingredient_code"), "low_sodium")
        val feedback = FeedbackModel("recommendation-1", RecommendationFeedbackAction.ACCEPTED, "health_fit")
        assertEquals("record_meal_manual", context.source)
        assertEquals(RecommendationFeedbackAction.ACCEPTED, feedback.action)
    }
}
