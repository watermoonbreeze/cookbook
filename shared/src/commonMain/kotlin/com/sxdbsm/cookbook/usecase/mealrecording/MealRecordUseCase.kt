package com.sxdbsm.cookbook.usecase.mealrecording

import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.MealRecordEditData
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.legacy.LegacyMealRecordAdapter
import com.sxdbsm.cookbook.domain.mealrecording.MealRecord
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/** Domain 输入；旧仓储的 DayMealDraft 不向 UI 暴露。 */
data class MealRecordDraft(
    val mealTypeId: Long,
    val date: LocalDate,
    val mealTime: LocalTime,
    val note: String = "",
    val dishIds: List<Long> = emptyList(),
)

/**
 * MealRecord 业务入口。
 *
 * 该层只编排 Domain 生命周期和旧存储适配，暂不改变 MealRecordRepository 的兼容 API。
 * 编辑页使用的 MealRecordEditData 是现有读取投影，保留在这里作为迁移期边界。
 */
class MealRecordUseCase(private val mealRepository: MealRecordRepository) {

    /** Opaque restore payload; callers can only hand it back to this use case. */
    class DeletedDayToken internal constructor(
        internal val date: LocalDate,
        internal val drafts: List<DayMealDraft>,
    )

    suspend fun create(draft: MealRecordDraft): MealRecord {
        val id = mealRepository.save(
            date = draft.date,
            mealTypeId = draft.mealTypeId,
            mealTime = draft.mealTime,
            note = draft.note,
            dishIds = draft.dishIds,
        )
        return recorded(id)
    }

    suspend fun saveDay(
        date: LocalDate,
        drafts: List<MealRecordDraft>,
        incrementBaselineDate: LocalDate? = null,
        bumpPreference: Boolean = true,
    ): List<MealRecord> {
        require(drafts.all { it.date == date }) { "All meal drafts must belong to the saved date" }
        val ids = mealRepository.saveDayMeals(
            date = date,
            meals = drafts.map { it.toLegacyDraft() },
            incrementBaselineDate = incrementBaselineDate,
            bumpPreference = bumpPreference,
        )
        return ids.map { id -> recorded(id) }
    }

    suspend fun queryDayForEdit(date: LocalDate): List<MealRecordEditData> =
        mealRepository.loadDayMealsForEdit(date)

    suspend fun dateRange(): Pair<LocalDate?, LocalDate?> = mealRepository.dateRange()

    suspend fun deleteDay(date: LocalDate) = mealRepository.deleteDayMeals(date)

    suspend fun deleteDayWithUndo(date: LocalDate): DeletedDayToken? {
        val snapshot = mealRepository.snapshotDay(date)
        if (snapshot.isEmpty()) return null
        mealRepository.deleteDayMeals(date)
        return DeletedDayToken(date, snapshot)
    }

    suspend fun restoreDeletedDay(token: DeletedDayToken): List<MealRecord> = saveDay(
        date = token.date,
        drafts = token.drafts.map { draft ->
            MealRecordDraft(draft.mealTypeId, token.date, draft.mealTime, draft.note, draft.dishIds)
        },
        bumpPreference = false,
    )

    suspend fun updateDishEatenRatio(mealRecordId: Long, dishId: Long, ratio: Double) =
        mealRepository.setEatenRatio(mealRecordId, dishId, ratio)

    suspend fun updateMealEatenRatio(mealRecordId: Long, ratio: Double) =
        mealRepository.setEatenRatioForMeal(mealRecordId, ratio)

    private suspend fun recorded(id: Long): MealRecord =
        mealRepository.loadMealRecord(id)?.let(LegacyMealRecordAdapter::toDomain)
            ?: error("Saved meal record $id could not be read back")

    private fun MealRecordDraft.toLegacyDraft() = DayMealDraft(
        mealTypeId = mealTypeId,
        mealTime = mealTime,
        note = note,
        dishIds = dishIds,
    )
}
