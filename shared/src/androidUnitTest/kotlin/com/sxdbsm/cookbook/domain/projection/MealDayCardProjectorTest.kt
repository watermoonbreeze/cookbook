package com.sxdbsm.cookbook.domain.projection

import com.sxdbsm.cookbook.domain.model.MealDayContent
import com.sxdbsm.cookbook.domain.model.MealDayTemporalRole
import com.sxdbsm.cookbook.domain.model.MealSection
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate

class MealDayCardProjectorTest {
    private val referenceDate = LocalDate(2026, 8, 21)

    @Test
    fun temporalRoleCoversPastTodayFuture() {
        assertEquals(MealDayTemporalRole.PAST, MealDayCardProjector.temporalRole(LocalDate(2026, 8, 20), referenceDate))
        assertEquals(MealDayTemporalRole.TODAY, MealDayCardProjector.temporalRole(referenceDate, referenceDate))
        assertEquals(MealDayTemporalRole.FUTURE, MealDayCardProjector.temporalRole(LocalDate(2026, 8, 22), referenceDate))
    }

    @Test
    fun compatibilityGettersAreDerivedFromOneRole() {
        val past = MealDayCardProjector.project(MealDayContent(LocalDate(2026, 8, 20), emptyList()), referenceDate)
        val today = MealDayCardProjector.project(MealDayContent(referenceDate, emptyList()), referenceDate)
        val future = MealDayCardProjector.project(MealDayContent(LocalDate(2026, 8, 22), emptyList()), referenceDate)

        assertFalse(past.isToday)
        assertFalse(past.isPlanState)
        assertTrue(today.isToday)
        assertFalse(today.isPlanState)
        assertFalse(future.isToday)
        assertTrue(future.isPlanState)
    }

    @Test
    fun projectionPreservesStableContentAndAddsOnlyTemporalRole() {
        val meals = listOf(
            MealSection(
                mealTypeId = 1L,
                mealName = "午餐",
                mealTime = kotlinx.datetime.LocalTime(12, 0),
                dishes = emptyList(),
            ),
        )
        val content = MealDayContent(referenceDate, meals)

        val card = MealDayCardProjector.project(content, referenceDate)

        assertEquals(content.date, card.date)
        assertEquals(content.meals, card.meals)
        assertEquals(MealDayTemporalRole.TODAY, card.temporalRole)
    }
}
