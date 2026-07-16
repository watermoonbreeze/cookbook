package com.sxdbsm.cookbook.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * 食历页 UI 状态。[AI修改]
 */
data class TimelineUiState(
    val pages: List<DayMealCardData> = emptyList(),
    val rangeMin: LocalDate? = null,
    val rangeMax: LocalDate? = null,
    val mealDates: Set<LocalDate> = emptySet(),
    val loading: Boolean = false,
    val loadingPrevious: Boolean = false,
    val loadingNext: Boolean = false,
    val todayIndex: Int = -1,
    val prependCount: Int = 0,
    val scrollTargetIndex: Int = -1,
    val scrollRequestVersion: Int = 0,
    val copyMessage: String? = null,
    val copyError: String? = null,
)

/**
 * 食历页 ViewModel。[AI修改]
 *
 * 通过 Flow 监听数据库变化，添加/编辑餐食后食历会自动刷新。
 */
class TimelineViewModel(
    private val repo: MealRecordRepository,
) : ViewModel() {

    private companion object {
        private const val PAGE_DAYS = 7
    }

    private val today = DateTime.today() // [AI生成] 当前 ViewModel 生命周期内固定“今天”，避免跨午夜导致索引跳动。
    private var observeJob: Job? = null
    private var refreshJob: Job? = null // [AI生成] 食历窗口刷新只保留最新任务，避免连续加载时旧结果覆盖新结果。
    private var allDates: List<LocalDate> = emptyList()
    private var loadedStartIndex = 0
    private var loadedEndIndex = -1
    private var initialized = false
    private var pendingPrependCount = 0 // [AI生成] 只在新窗口数据到达后发布给 UI，避免旧列表上提前消费。
    private var pendingScrollTargetDate: LocalDate? = null

    private val _state = MutableStateFlow(TimelineUiState(loading = true)) // [AI修改] 内部维护窗口式食历状态。
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    init {
        observeTimelineDates()
    }

    /**
     * 顶部/下拉加载历史 7 天。[AI生成]
     *
     * prependCount 会告诉 UI 本次前插了多少个日期，方便 LazyList 保持当前锚点不跳。
     */
    fun loadPrevious() {
        if (_state.value.loading || loadedStartIndex <= 0) return
        val oldStart = loadedStartIndex
        loadedStartIndex = (loadedStartIndex - PAGE_DAYS).coerceAtLeast(0)
        pendingPrependCount = oldStart - loadedStartIndex
        _state.value = _state.value.copy(
            loading = true,
            loadingPrevious = true,
            prependCount = 0,
        )
        refreshLoadedPages()
    }

    /**
     * 底部/上滑加载未来 7 天。[AI生成]
     */
    fun loadNext() {
        if (_state.value.loading || loadedEndIndex >= allDates.lastIndex) return
        loadedEndIndex = (loadedEndIndex + PAGE_DAYS).coerceAtMost(allDates.lastIndex)
        _state.value = _state.value.copy(loading = true, loadingNext = true)
        refreshLoadedPages()
    }

    /**
     * UI 在完成滚动锚点修正后调用，避免重复消费同一次前插数量。[AI生成]
     */
    fun consumePrependCount() {
        if (_state.value.prependCount == 0) return
        _state.value = _state.value.copy(prependCount = 0)
    }

    /**
     * 兼容旧 UI 的加载更多入口。[AI修改]
     */
    fun loadMore(offset: Int) {
        offset.hashCode() // [AI修改] 保留签名兼容 UI；旧按钮位于列表底部，因此加载未来窗口。
        loadNext()
    }

    /**
     * 日历中选择某个有餐食的日期后定位到对应 item。[AI生成]
     */
    fun jumpToDate(date: LocalDate) {
        val index = allDates.indexOf(date)
        if (index < 0) return
        val visibleIndex = dateIndexInLoadedPage(date)
        if (visibleIndex >= 0) {
            requestScroll(visibleIndex)
            return
        }
        loadedStartIndex = (index - PAGE_DAYS + 1).coerceAtLeast(0)
        loadedEndIndex = (index + PAGE_DAYS - 1).coerceAtMost(allDates.lastIndex)
        pendingScrollTargetDate = date
        _state.value = _state.value.copy(loading = true)
        refreshLoadedPages()
    }

    fun consumeScrollRequest() {
        if (_state.value.scrollTargetIndex < 0) return
        _state.value = _state.value.copy(scrollTargetIndex = -1)
    }

    /** 删除指定日期的全部餐食（食历 Flow 会自动刷新）。[AI生成] */
    fun deleteDay(date: LocalDate) {
        viewModelScope.launch {
            runCatching { repo.deleteDayMeals(date) }
                .onSuccess { _state.value = _state.value.copy(copyMessage = "已删除 ${DateTime.formatDate(date)} 的餐食", copyError = null) }
                .onFailure { e -> _state.value = _state.value.copy(copyMessage = null, copyError = e.message ?: "删除失败，请稍后重试") }
        }
    }

    /** 删整天并支持撤销(§9.12)：先快照→删→回调给 showUndo(点撤销即 saveDayMeals 还原)。[AI生成] B-5 */
    fun deleteDayUndoable(date: LocalDate, showUndo: (onUndo: () -> Unit) -> Unit) {
        viewModelScope.launch {
            // [AI修改] 代码审查#3：快照读失败/为空不该照删致无法撤销。
            val snapshot = runCatching { repo.snapshotDay(date) }.getOrNull()
            if (snapshot.isNullOrEmpty()) {
                _state.value = _state.value.copy(copyMessage = null, copyError = "删除失败，请稍后重试")
                return@launch
            }
            runCatching { repo.deleteDayMeals(date) }
                .onSuccess {
                    showUndo {
                        viewModelScope.launch {
                            // [AI修改] 代码审查#1：撤销不抬喜爱度(bumpPreference=false)；#2：还原失败落提示。
                            runCatching { repo.saveDayMeals(date, snapshot, bumpPreference = false) }
                                .onFailure { e -> _state.value = _state.value.copy(copyMessage = null, copyError = e.message ?: "撤销失败，请稍后重试") }
                        }
                    }
                }
                .onFailure { e -> _state.value = _state.value.copy(copyMessage = null, copyError = e.message ?: "删除失败，请稍后重试") }
        }
    }

    fun consumeCopyMessage() {
        if (_state.value.copyMessage == null && _state.value.copyError == null) return
        _state.value = _state.value.copy(copyMessage = null, copyError = null)
    }

    /**
     * 监听所有有 meal_record 的日期，分页窗口只从这些日期中截取。[AI修改]
     */
    private fun observeTimelineDates() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            repo.observeTimelineDates().collect { dates ->
                allDates = dates.sorted()
                if (allDates.isEmpty()) {
                    initialized = false
                    loadedStartIndex = 0
                    loadedEndIndex = -1
                    _state.value = TimelineUiState(loading = false)
                } else {
                    if (!initialized || loadedEndIndex < loadedStartIndex) {
                        val target = defaultTargetIndex(allDates)
                        loadedStartIndex = (target - PAGE_DAYS + 1).coerceAtLeast(0)
                        loadedEndIndex = (target + PAGE_DAYS - 1).coerceAtMost(allDates.lastIndex)
                        pendingScrollTargetDate = allDates[target]
                        initialized = true
                    } else {
                        loadedStartIndex = loadedStartIndex.coerceIn(0, allDates.lastIndex)
                        loadedEndIndex = loadedEndIndex.coerceIn(loadedStartIndex, allDates.lastIndex)
                    }
                    refreshLoadedPages()
                }
            }
        }
    }

    /**
     * 按当前日期索引窗口加载真实有记录的食历卡片。[AI生成]
     */
    private fun refreshLoadedPages() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val visibleDates = if (loadedEndIndex >= loadedStartIndex && allDates.isNotEmpty()) {
                allDates.subList(loadedStartIndex, loadedEndIndex + 1)
            } else {
                emptyList()
            }
            val cards = repo.loadTimelineCardsByDates(visibleDates)
            val prependCount = pendingPrependCount
            pendingPrependCount = 0
            val pendingDate = pendingScrollTargetDate
            pendingScrollTargetDate = null
            val scrollIndex = pendingDate?.let { date -> cards.indexOfFirst { it.date == date } } ?: -1
            _state.value = _state.value.copy(
                pages = cards,
                rangeMin = allDates.firstOrNull(),
                rangeMax = allDates.lastOrNull(),
                mealDates = allDates.toSet(),
                loading = false,
                loadingPrevious = false,
                loadingNext = false,
                todayIndex = cards.indexOfFirst { it.date == today },
                prependCount = prependCount,
                scrollTargetIndex = scrollIndex,
                scrollRequestVersion = if (scrollIndex >= 0) _state.value.scrollRequestVersion + 1 else _state.value.scrollRequestVersion,
            )
        }
    }

    private fun defaultTargetIndex(dates: List<LocalDate>): Int {
        val todayIndex = dates.indexOf(today)
        if (todayIndex >= 0) return todayIndex
        val futureIndex = dates.indexOfFirst { it > today }
        return if (futureIndex >= 0) futureIndex else dates.lastIndex
    }

    private fun dateIndexInLoadedPage(date: LocalDate): Int =
        _state.value.pages.indexOfFirst { it.date == date }

    private fun requestScroll(index: Int) {
        _state.value = _state.value.copy(
            scrollTargetIndex = index,
            scrollRequestVersion = _state.value.scrollRequestVersion + 1,
        )
    }
}
