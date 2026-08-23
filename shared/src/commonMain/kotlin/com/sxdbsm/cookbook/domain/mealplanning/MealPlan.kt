package com.sxdbsm.cookbook.domain.mealplanning

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@JvmInline
value class MealPlanId(val value: String) {
    init { require(value.isNotBlank()) { "MealPlanId must not be blank" } }
}

/** A planned meal. It is not evidence that the meal happened. */
data class MealPlan(
    val id: MealPlanId,
    val date: LocalDate,
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val note: String = "",
    val dishIds: List<Long> = emptyList(),
    val lifecycle: MealPlanLifecycle = MealPlanLifecycle.DRAFT,
)

enum class MealPlanLifecycle {
    DRAFT,
    PLANNED,
    CANCELLED,
}

object MealPlanLifecycleContract {
    private val allowed = setOf(
        MealPlanLifecycle.DRAFT to MealPlanLifecycle.PLANNED,
        MealPlanLifecycle.PLANNED to MealPlanLifecycle.CANCELLED,
        MealPlanLifecycle.PLANNED to MealPlanLifecycle.DRAFT,
    )

    fun canTransition(from: MealPlanLifecycle, to: MealPlanLifecycle): Boolean =
        from to to in allowed

    fun transition(plan: MealPlan, to: MealPlanLifecycle): MealPlan {
        require(canTransition(plan.lifecycle, to)) {
            "Illegal MealPlan lifecycle transition: ${plan.lifecycle} -> $to"
        }
        return plan.copy(lifecycle = to)
    }
}
