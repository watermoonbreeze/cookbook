package com.sxdbsm.cookbook.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/**
 * 食历页 UI 状态。[AI修改]
 */
data class TimelineUiState(
    val pages: List<DayMealCardData> = emptyList(),
    val rangeMin: LocalDate? = null,
    val rangeMax: LocalDate? = null,
    val loading: Boolean = false,
)

/**
 * 食历页 ViewModel。[AI修改]
 *
 * 通过 Flow 监听数据库变化，添加/编辑餐食后食历会自动刷新。
 */
class TimelineViewModel(
    private val repo: MealRecordRepository,
) : ViewModel() {

    val state: StateFlow<TimelineUiState> = repo.observeTimelineCards(limit = 60).map { cards ->
        val today = DateTime.today()
        val dates = cards.map { it.date }
        TimelineUiState(
            pages = cards,
            rangeMin = dates.minOrNull() ?: today,
            rangeMax = dates.maxOrNull() ?: today,
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TimelineUiState(loading = true))

    /**
     * 兼容旧 UI 的加载更多入口。[AI修改]
     *
     * 当前先监听最近 60 个日期，MVP 阶段避免频繁分页查询造成切换卡顿。
     */
    fun loadMore(offset: Int) {
        offset.hashCode() // [AI修改] 保留签名兼容 UI，当前监听模式下无需额外分页加载。
    }
}
