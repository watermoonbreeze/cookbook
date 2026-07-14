package com.sxdbsm.cookbook.android.ui.weekplan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber

/**
 * @File : WeekPlanViewModel
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 一周计划视图 ViewModel（B3）
 * <p>
 * 以周一为起点的一周(7天)计划一览，可上一周/下一周/回本周切换；每天卡片来自 observeTimelineWindow
 * (含空日)，供"周末排下周饭"的整周概览 + 逐日快捷编辑/复制/安排。
 * <p>
 * [AI生成] B3：家庭"排下周饭"强场景的一周网格视图。
 **/
class WeekPlanViewModel(
    private val mealRepo: MealRecordRepository,
) : ViewModel() {

    private val today: LocalDate = DateTime.today()

    /** 该日期所在周的周一。[AI生成] ISO：周一=1…周日=7。 */
    private fun mondayOf(d: LocalDate): LocalDate = DateTime.plusDays(d, -(d.dayOfWeek.isoDayNumber - 1))

    private val _weekStart = MutableStateFlow(mondayOf(today))

    data class UiState(
        val weekStart: LocalDate,
        val weekEnd: LocalDate,
        val today: LocalDate,
        val days: List<DayMealCardData>,
    )

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<UiState> = _weekStart.flatMapLatest { ws ->
        val we = DateTime.plusDays(ws, 6)
        mealRepo.observeTimelineWindow(ws, we).map { cards ->
            UiState(weekStart = ws, weekEnd = we, today = today, days = cards)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UiState(mondayOf(today), DateTime.plusDays(mondayOf(today), 6), today, emptyList()),
    )

    fun prevWeek() { _weekStart.value = DateTime.plusDays(_weekStart.value, -7) }
    fun nextWeek() { _weekStart.value = DateTime.plusDays(_weekStart.value, 7) }
    fun thisWeek() { _weekStart.value = mondayOf(today) }
}
