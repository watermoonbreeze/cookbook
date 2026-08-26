package com.sxdbsm.cookbook.android.ui.meal

import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import kotlinx.datetime.LocalDate

internal class MealDayUndoToken internal constructor(internal val value: Any)

internal interface MealDayMutationPort {
    suspend fun deleteDay(date: LocalDate)
    suspend fun deleteDayWithUndo(date: LocalDate): MealDayUndoToken?
    suspend fun restoreDeletedDay(token: MealDayUndoToken)
}

internal fun MealRecordUseCase.asMealDayMutationPort() = object : MealDayMutationPort {
    override suspend fun deleteDay(date: LocalDate): Unit { this@asMealDayMutationPort.deleteDay(date) }
    override suspend fun deleteDayWithUndo(date: LocalDate): MealDayUndoToken? =
        this@asMealDayMutationPort.deleteDayWithUndo(date)?.let(::MealDayUndoToken)
    override suspend fun restoreDeletedDay(token: MealDayUndoToken) {
        restoreDeletedDay(token.value as MealRecordUseCase.DeletedDayToken)
    }
}
