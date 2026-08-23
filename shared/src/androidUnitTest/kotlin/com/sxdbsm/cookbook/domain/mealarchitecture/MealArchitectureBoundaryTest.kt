package com.sxdbsm.cookbook.domain.mealarchitecture

import com.sxdbsm.cookbook.domain.foodknowledge.DishRef
import com.sxdbsm.cookbook.domain.foodknowledge.IngredientRef
import com.sxdbsm.cookbook.domain.legacy.LegacyMealRecordAdapter
import com.sxdbsm.cookbook.domain.mealplanning.MealPlan
import com.sxdbsm.cookbook.domain.mealplanning.MealPlanId
import com.sxdbsm.cookbook.domain.mealplanning.MealPlanLifecycle
import com.sxdbsm.cookbook.domain.mealplanning.MealPlanLifecycleContract
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordLifecycle
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordLifecycleContract
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealRecord as LegacyMealRecord
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MealArchitectureBoundaryTest {
    @Test
    fun planAndRecordHaveSeparateLifecycleContracts() {
        assertTrue(MealPlanLifecycleContract.canTransition(MealPlanLifecycle.DRAFT, MealPlanLifecycle.PLANNED))
        assertFalse(MealPlanLifecycleContract.canTransition(MealPlanLifecycle.DRAFT, MealPlanLifecycle.CANCELLED))
        assertTrue(MealRecordLifecycleContract.canTransition(MealRecordLifecycle.CREATED, MealRecordLifecycle.RECORDED))
        assertFalse(MealRecordLifecycleContract.canTransition(MealRecordLifecycle.CREATED, MealRecordLifecycle.ARCHIVED))
    }

    @Test
    fun foodKnowledgeRefsDoNotBecomeMealRecords() {
        assertEquals(7L, DishRef(7).id)
        assertEquals(8L, IngredientRef(8).id)
    }

    @Test
    fun legacyMealRecordMapsToRecordedDomainWithoutChangingIdentity() {
        val legacy = LegacyMealRecord(
            id = 42,
            date = LocalDate(2026, 8, 23),
            mealTypeId = 1,
            mealName = "早餐",
            mealTime = LocalTime(8, 0),
            note = "legacy",
            createdAt = 100L,
            dishes = listOf(DishMini(id = 9, name = "粥")),
        )

        val record = LegacyMealRecordAdapter.toDomain(legacy)
        assertEquals("42", record.id.value)
        assertEquals(legacy.date, record.date)
        assertEquals(listOf(9L), record.dishIds)
        assertEquals(MealRecordLifecycle.RECORDED, record.lifecycle)
    }

    @Test
    fun planIsNotARecord() {
        val plan = MealPlan(MealPlanId("plan-1"), LocalDate(2026, 8, 23), 1, "早餐", LocalTime(8, 0))
        assertEquals(MealPlanLifecycle.DRAFT, plan.lifecycle)
    }
}
