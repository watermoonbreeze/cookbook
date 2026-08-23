package com.sxdbsm.cookbook.domain.mealrecording

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

@JvmInline
value class MealRecordId(val value: String) {
    init { require(value.isNotBlank()) { "MealRecordId must not be blank" } }
}

/** A meal that is recorded as having happened. It is not a planning object. */
data class MealRecord(
    val id: MealRecordId,
    val date: LocalDate,
    val mealTypeId: Long,
    val mealName: String,
    val mealTime: LocalTime,
    val note: String = "",
    val createdAt: Long = 0L,
    val dishIds: List<Long> = emptyList(),
    val lifecycle: MealRecordLifecycle = MealRecordLifecycle.CREATED,
)

enum class MealRecordLifecycle {
    CREATED,
    RECORDED,
    MODIFIED,
    ARCHIVED,
}

object MealRecordLifecycleContract {
    private val allowed = setOf(
        MealRecordLifecycle.CREATED to MealRecordLifecycle.RECORDED,
        MealRecordLifecycle.RECORDED to MealRecordLifecycle.MODIFIED,
        MealRecordLifecycle.MODIFIED to MealRecordLifecycle.RECORDED,
        MealRecordLifecycle.RECORDED to MealRecordLifecycle.ARCHIVED,
        MealRecordLifecycle.MODIFIED to MealRecordLifecycle.ARCHIVED,
    )

    fun canTransition(from: MealRecordLifecycle, to: MealRecordLifecycle): Boolean =
        from to to in allowed

    fun transition(record: MealRecord, to: MealRecordLifecycle): MealRecord {
        require(canTransition(record.lifecycle, to)) {
            "Illegal MealRecord lifecycle transition: ${record.lifecycle} -> $to"
        }
        return record.copy(lifecycle = to)
    }
}
