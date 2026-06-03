package com.sxdbsm.cookbook.android.ui.dishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 菜品列表排序 Tab。[AI修改]
 *
 * 当前 MVP 只保存选择状态，后续可让不同 Tab 对应不同查询或排序。
 */
enum class DishesSortTab { RECENT, FAVORITE, ALL }

/**
 * 菜品页 UI 状态。[AI修改]
 */
data class DishesUiState(
    val popular: List<DishMini> = emptyList(),
    val all: List<DishMini> = emptyList(),
    val keyword: String = "",
    val sortTab: DishesSortTab = DishesSortTab.RECENT,
    val refreshing: Boolean = false,
)

/**
 * 菜品页 ViewModel。[AI修改]
 *
 * 管理搜索关键字、排序 Tab、热门列表和全部列表。
 */
class DishesViewModel(
    private val dishRepo: DishRepository,
    @Suppress("unused") private val mealRepo: MealRecordRepository,
) : ViewModel() {

    private val _keyword = MutableStateFlow("") // [AI修改] 搜索框文本。
    private val _sortTab = MutableStateFlow(DishesSortTab.RECENT) // [AI修改] 当前选中的排序 Tab。
    private val _list = MutableStateFlow<List<DishMini>>(emptyList()) // [AI修改] 搜索结果列表。
    private val _refreshing = MutableStateFlow(false) // [AI生成] 下拉刷新状态。
    private var searchJob: Job? = null // [AI修改] 连续输入时取消上一次搜索，避免每个字符都触发数据库查询。

    /**
     * 热门菜品列表。[AI修改]
     */
    val popular: StateFlow<List<DishMini>> = dishRepo.observePopularDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allObserved: StateFlow<List<DishMini>> = dishRepo.observeAllDishes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val baseState = kotlinx.coroutines.flow.combine(
        popular, allObserved, _list, _keyword, _sortTab,
    ) { popular, observed, searched, kw, tab ->
        val list = if (kw.isBlank()) observed else searched
        DishesUiState(popular = popular, all = sortDishes(list, tab), keyword = kw, sortTab = tab)
    }

    val uiState: StateFlow<DishesUiState> = kotlinx.coroutines.flow.combine(
        baseState, _refreshing,
    ) { state, refreshing ->
        state.copy(refreshing = refreshing)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishesUiState())

    init {
        refresh()
    }

    fun setKeyword(kw: String) {
        _keyword.value = kw
        searchNow(kw, debounce = true)
    }

    fun setSortTab(tab: DishesSortTab) { _sortTab.value = tab }

    fun refresh() {
        searchNow(_keyword.value, force = true)
    } // [AI生成] 给返回页面和下拉刷新使用；空关键词时依赖 Flow 自动刷新，仅短暂显示刷新态。

    private fun sortDishes(list: List<DishMini>, tab: DishesSortTab): List<DishMini> =
        when (tab) {
            DishesSortTab.RECENT -> list
            DishesSortTab.FAVORITE -> list.sortedByDescending { it.preference }
            DishesSortTab.ALL -> list.sortedWith(compareBy({ dishInitial(it.name) }, { it.name }))
        }


    /**
     * 执行菜品搜索。[AI生成]
     *
     * 首次加载立即查询；用户输入时延迟一小段时间，连续输入会取消旧任务，只保留最后一次关键词。
     */
    private fun searchNow(keyword: String, debounce: Boolean = false, force: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(280)
            _refreshing.value = true
            if (keyword.isBlank()) {
                if (force) delay(120) // [AI生成] 空关键词列表来自 Flow，保留一个短刷新反馈。
            } else {
                _list.value = dishRepo.searchDishes(keyword)
            }
            _refreshing.value = false
        }
    }
}
