package com.sxdbsm.cookbook.data.repository

import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.MealDayContent
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

/**
 * Meal read-side projection boundary.
 *
 * UI consumers depend on this read-only seam rather than the legacy meal repository
 * APIs. It deliberately exposes projection models only; mutations remain on
 * [MealRecordRepository].
 *
 * @Author : Codex-AI
 * [AI生成] Phase 3 Projection Migration: isolate presentation reads without
 * changing storage, domain truth, or the legacy compatibility API.
 */
class MealProjectionRepository(
    private val mealRepository: MealRecordRepository,
) {
    fun observeUpcomingMealDayContents(
        referenceDate: LocalDate,
        futureDayLimit: Int = 2,
    ): Flow<List<MealDayContent>> =
        mealRepository.observeUpcomingMealDayContents(referenceDate, futureDayLimit)

    fun observeTimelineDates(): Flow<List<LocalDate>> =
        mealRepository.observeTimelineDates()

    fun observeTimelineWindow(start: LocalDate, end: LocalDate): Flow<List<DayMealCardData>> =
        mealRepository.observeTimelineWindow(start, end)

    suspend fun dateRange(): Pair<LocalDate?, LocalDate?> =
        mealRepository.dateRange()

    suspend fun loadMealDayContentsByDates(dates: List<LocalDate>): List<MealDayContent> =
        mealRepository.loadMealDayContentsByDates(dates)

    suspend fun searchMealCards(
        keyword: String,
        limit: Long = 20,
        offset: Long = 0,
    ): List<DayMealCardData> =
        mealRepository.searchMealCards(keyword, limit, offset)
}
