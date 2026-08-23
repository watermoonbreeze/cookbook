package com.sxdbsm.cookbook.domain.meal

/** [AI生成] Meal 生命周期由 Domain 持有，避免 Repository 或 Projection 复制状态机。 */
enum class MealLifecycle {
    DRAFT,
    PLANNED,
    RECORDED,
    ARCHIVED,
}

/** [AI生成] 非法迁移显式失败；调用方必须通过该 Contract 申请状态变化。 */
data class TransitionRule(
    val from: MealLifecycle,
    val to: MealLifecycle,
)

object MealLifecycleContract {
    private val allowed = setOf(
        TransitionRule(MealLifecycle.DRAFT, MealLifecycle.PLANNED),
        TransitionRule(MealLifecycle.DRAFT, MealLifecycle.RECORDED),
        TransitionRule(MealLifecycle.PLANNED, MealLifecycle.RECORDED),
        TransitionRule(MealLifecycle.PLANNED, MealLifecycle.DRAFT),
        TransitionRule(MealLifecycle.RECORDED, MealLifecycle.ARCHIVED),
    )

    fun canTransition(from: MealLifecycle, to: MealLifecycle): Boolean =
        TransitionRule(from, to) in allowed

    fun transition(meal: Meal, to: MealLifecycle): Meal {
        require(canTransition(meal.lifecycle, to)) {
            "Illegal Meal lifecycle transition: ${meal.lifecycle} -> $to"
        }
        return meal.copy(lifecycle = to)
    }
}

