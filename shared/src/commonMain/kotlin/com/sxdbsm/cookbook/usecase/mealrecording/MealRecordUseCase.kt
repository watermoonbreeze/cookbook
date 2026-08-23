package com.sxdbsm.cookbook.usecase.mealrecording

import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.MealRecordEditData
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.mealrecording.MealRecord
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordId
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordLifecycle
import com.sxdbsm.cookbook.domain.mealrecording.MealRecordLifecycleContract
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

    suspend fun create(draft: MealRecordDraft): MealRecord {
        val id = mealRepository.save(
            date = draft.date,
            mealTypeId = draft.mealTypeId,
            mealTime = draft.mealTime,
            note = draft.note,
            dishIds = draft.dishIds,
        )
        return recorded(draft, id)
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
        return drafts.zip(ids).map { (draft, id) -> recorded(draft, id) }
    }

    suspend fun queryDayForEdit(date: LocalDate): List<MealRecordEditData> =
        mealRepository.loadDayMealsForEdit(date)

    suspend fun dateRange(): Pair<LocalDate?, LocalDate?> = mealRepository.dateRange()

    suspend fun deleteDay(date: LocalDate) = mealRepository.deleteDayMeals(date)

    private fun recorded(draft: MealRecordDraft, id: Long): MealRecord {
        val created = MealRecord(
            id = MealRecordId(id.toString()),
            date = draft.date,
            mealTypeId = draft.mealTypeId,
            mealName = draft.note,
            mealTime = draft.mealTime,
            note = draft.note,
            dishIds = draft.dishIds,
            lifecycle = MealRecordLifecycle.CREATED,
        )
        return MealRecordLifecycleContract.transition(created, MealRecordLifecycle.RECORDED)
    }

    private fun MealRecordDraft.toLegacyDraft() = DayMealDraft(
        mealTypeId = mealTypeId,
        mealTime = mealTime,
        note = note,
        dishIds = dishIds,
    )
}
