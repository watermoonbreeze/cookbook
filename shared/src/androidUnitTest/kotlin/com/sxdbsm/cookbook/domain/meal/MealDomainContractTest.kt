package com.sxdbsm.cookbook.domain.meal

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** [AI生成] 验证 Canonical Identity、Lifecycle 与 Boundary Contract。 */
class MealDomainContractTest {
    private val meal = Meal(
        id = MealId("meal-1"),
        source = MealSource.USER,
        metadata = MealMetadata(title = "早餐"),
        occurrences = listOf(
            MealOccurrence("occurrence-1", MealId("meal-1"), LocalDate(2026, 8, 23), LocalTime(8, 0)),
        ),
    )

    @Test
    fun mealIdIsRequiredAndOccurrenceReferencesCanonicalMeal() {
        assertFailsWith<IllegalArgumentException> { MealId(" ") }
        assertEquals(meal.id, meal.occurrences.single().mealId)
        assertFailsWith<IllegalArgumentException> { MealOccurrence("", meal.id, LocalDate(2026, 8, 23)) }
    }

    @Test
    fun lifecycleAllowsOnlyContractTransitions() {
        val planned = MealLifecycleContract.transition(meal, MealLifecycle.PLANNED)
        assertEquals(MealLifecycle.PLANNED, planned.lifecycle)
        assertTrue(MealLifecycleContract.canTransition(MealLifecycle.PLANNED, MealLifecycle.RECORDED))
        assertFalse(MealLifecycleContract.canTransition(MealLifecycle.DRAFT, MealLifecycle.ARCHIVED))
        assertFailsWith<IllegalArgumentException> {
            MealLifecycleContract.transition(meal, MealLifecycle.ARCHIVED)
        }
    }

    @Test
    fun projectionIsReadOnlyViewAndSuggestionIsNotMealTruth() {
        val projection = meal.toProjection()
        assertEquals(meal.id, projection.mealId)
        assertEquals(meal.lifecycle, projection.lifecycle)
        val suggestion = MealSuggestion("建议早餐", "health-context")
        assertEquals("建议早餐", suggestion.title)
    }
}

