package com.sxdbsm.cookbook.android.ui.dishes

import android.util.Log
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
    val deleteState: DishDeleteState = DishDeleteState(),
    val selectedMethod: String? = null, // [AI生成] 烹饪方式筛选
    val selectedTag: String? = null, // [AI生成] 标签筛选
    val availableMethods: List<String> = emptyList(), // 当前列表可选烹饪方式
    val availableTags: List<String> = emptyList(), // 当前列表可选标签
    val recentCount: Int = 0, // [AI生成] 最近 Tab 菜品数(前30, 与该 Tab 实际展示一致)
    val favoriteCount: Int = 0, // [AI生成] 喜爱 Tab 菜品数(已评分 preference>0)
    val allCount: Int = 0, // [AI生成] 全部 Tab 菜品数
)

/**
 * 菜品删除弹框状态。[AI生成]
 *
 * 删除前必须先检查餐食引用；有引用时展示风险提示，无引用时展示二次确认。
 */
data class DishDeleteState(
    val checking: Boolean = false,
    val pendingDish: DishMini? = null,
    val warningDish: DishMini? = null,
    val warningReferences: List<DishRepository.DishMealReference> = emptyList(),
    val errorMessage: String? = null,
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
    private val _deleteState = MutableStateFlow(DishDeleteState()) // [AI生成] 长按删除前的引用检查/确认弹框状态。
    private val _methodFilter = MutableStateFlow<String?>(null) // [AI生成] 烹饪方式筛选
    private val _tagFilter = MutableStateFlow<String?>(null) // [AI生成] 标签筛选
    private var searchJob: Job? = null // [AI修改] 连续输入时取消上一次搜索，避免每个字符都触发数据库查询。

    private companion object {
        private const val TAG = "DishActions" // [AI生成] 菜品列表操作统一日志 Tag，便于排查长按编辑/删除。
        private const val LIST_LIMIT = 30 // [AI生成] "最近"(updated_at DESC)与"喜爱"(preference DESC) Tab 各只展示前 30 个；"全部"才展示所有。
    }

    /**
     * 热门菜品列表。[AI修改]
     */
    val popular: StateFlow<List<DishMini>> = dishRepo.observePopularDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allObserved: StateFlow<List<DishMini>> = dishRepo.observeAllDishes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [AI生成] 排序+筛选合并为一路(combine 最多 5 路)。
    private val viewOpts = kotlinx.coroutines.flow.combine(_sortTab, _methodFilter, _tagFilter) { s, m, t -> Triple(s, m, t) }

    private val baseState = kotlinx.coroutines.flow.combine(
        popular, allObserved, _list, _keyword, viewOpts,
    ) { popular, observed, searched, kw, opts ->
        val (tab, method, tag) = opts
        val raw = if (kw.isBlank()) observed else searched
        // 可选筛选项从筛选前列表派生，避免选中后选项消失。
        val methods = raw.flatMap { it.cookingMethodNames }.filter { it.isNotBlank() }.distinct().sorted()
        val tags = raw.flatMap { it.tags }.filter { it.isNotBlank() }.distinct().sorted()
        val filtered = raw.filter { d ->
            (method == null || method in d.cookingMethodNames) && (tag == null || tag in d.tags)
        }
        // [AI生成] 最近(updated_at DESC)、喜爱(已评分按 preference DESC)各取前 30；全部展示所有。计数与各 Tab 展示一致。
        val favorites = filtered.filter { it.preference > 0 }.sortedByDescending { it.preference }.take(LIST_LIMIT)
        val recentList = filtered.take(LIST_LIMIT)
        val listForTab = when (tab) {
            DishesSortTab.RECENT -> recentList
            DishesSortTab.FAVORITE -> favorites
            DishesSortTab.ALL -> filtered
        }
        DishesUiState(
            popular = popular, all = sortDishes(listForTab, tab), keyword = kw, sortTab = tab,
            selectedMethod = method, selectedTag = tag, availableMethods = methods, availableTags = tags,
            recentCount = recentList.size, favoriteCount = favorites.size, allCount = filtered.size,
        )
    }

    val uiState: StateFlow<DishesUiState> = kotlinx.coroutines.flow.combine(
        baseState, _refreshing, _deleteState,
    ) { state, refreshing, deleteState ->
        state.copy(refreshing = refreshing, deleteState = deleteState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishesUiState())

    init {
        refresh()
    }

    fun setKeyword(kw: String) {
        _keyword.value = kw
        searchNow(kw, debounce = true)
    }

    fun setSortTab(tab: DishesSortTab) { _sortTab.value = tab }

    /** 选/取消 烹饪方式筛选(再点同一项取消)。[AI生成] */
    fun toggleMethodFilter(method: String) { _methodFilter.value = if (_methodFilter.value == method) null else method }

    /** 选/取消 标签筛选。[AI生成] */
    fun toggleTagFilter(tag: String) { _tagFilter.value = if (_tagFilter.value == tag) null else tag }

    fun refresh() {
        searchNow(_keyword.value, force = true)
    } // [AI生成] 给返回页面和下拉刷新使用；空关键词时依赖 Flow 自动刷新，仅短暂显示刷新态。

    /**
     * 请求删除菜品。[AI生成]
     *
     * 先查餐食引用；若被引用，只展示风险提示，不直接删除，避免破坏食历。
     */
    fun requestDeleteDish(dish: DishMini) {
        viewModelScope.launch {
            Log.d(TAG, "request delete dish: id=${dish.id} name=${dish.name}")
            _deleteState.value = DishDeleteState(checking = true)
            runCatching { dishRepo.listMealReferencesByDish(dish.id) }
                .onSuccess { references ->
                    Log.d(TAG, "delete reference check: id=${dish.id} refs=${references.size}")
                    _deleteState.value = if (references.isNotEmpty()) {
                        DishDeleteState(warningDish = dish, warningReferences = references)
                    } else {
                        DishDeleteState(pendingDish = dish)
                    }
                }
                .onFailure { error ->
                    Log.e(TAG, "delete reference check failed: id=${dish.id}", error)
                    _deleteState.value = DishDeleteState(errorMessage = "检查菜品引用失败，请稍后重试")
                }
        }
    }

    /**
     * 确认删除未被餐食引用的菜品。[AI生成]
     */
    fun confirmDeleteDish() {
        val dish = _deleteState.value.pendingDish ?: return
        viewModelScope.launch {
            Log.d(TAG, "confirm delete dish: id=${dish.id} name=${dish.name}")
            _deleteState.value = _deleteState.value.copy(checking = true)
            runCatching { dishRepo.deleteDish(dish.id) }
                .onSuccess {
                    Log.d(TAG, "delete dish success: id=${dish.id}")
                    _deleteState.value = DishDeleteState()
                    refresh()
                }
                .onFailure { error ->
                    Log.e(TAG, "delete dish failed: id=${dish.id}", error)
                    _deleteState.value = DishDeleteState(errorMessage = "删除菜品失败，请稍后重试")
                }
        }
    }

    fun dismissDeleteDialog() {
        Log.d(TAG, "dismiss delete dialog")
        _deleteState.value = DishDeleteState()
    }

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
