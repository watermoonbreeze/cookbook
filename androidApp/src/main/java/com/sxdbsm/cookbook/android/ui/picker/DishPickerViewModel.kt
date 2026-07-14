package com.sxdbsm.cookbook.android.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 菜品选择器 UI 状态。[AI修改]
 *
 * 选择器既支持单选也支持多选，所以 `selected` 用列表保存当前已选菜品。
 */
data class DishPickerUiState(
    val keyword: String = "",
    val dishes: List<DishMini> = emptyList(),
    val popular: List<DishMini> = emptyList(),
    val recent: List<DishMini> = emptyList(),
    val selected: List<DishMini> = emptyList(),
    val excludeDishIds: Set<Long> = emptySet(),
)

/**
 * 菜品选择器 ViewModel。[AI修改]
 *
 * 为“添加餐食”和“复制/导入菜品”等入口提供统一的菜品搜索与选择逻辑。
 */
class DishPickerViewModel(
    private val dishRepo: DishRepository,
) : ViewModel() {

    private val _keyword = MutableStateFlow("") // [AI修改] 搜索关键词。
    private val _dishes = MutableStateFlow<List<DishMini>>(emptyList()) // [AI修改] 当前搜索结果。
    private val _selected = MutableStateFlow<List<DishMini>>(emptyList()) // [AI修改] 当前已选菜品。
    private val _excludeDishIds = MutableStateFlow<Set<Long>>(emptySet()) // [AI修改] 外部传入的不可选菜品 id。
    private var lastRefreshKeyword: String? = null
    private var searchJob: Job? = null // [AI修改] 搜索输入防抖任务；force 刷新不走延迟。

    private val popular = dishRepo.observePopularDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recent = dishRepo.observeRecentDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val baseState = combine(_keyword, _dishes, popular) { keyword, dishes, popular ->
        Triple(keyword, dishes, popular)
    }

    private val listState = combine(baseState, recent, _selected) { base, recent, selected ->
        ListState(base.first, base.second, base.third, recent, selected)
    }

    val state: StateFlow<DishPickerUiState> = combine(listState, _excludeDishIds) { list, exclude ->
        DishPickerUiState(
            keyword = list.keyword,
            dishes = list.dishes.filterNot { it.id in exclude },
            popular = list.popular.filterNot { it.id in exclude },
            recent = list.recent.filterNot { it.id in exclude },
            selected = list.selected.filterNot { it.id in exclude },
            excludeDishIds = exclude,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishPickerUiState())

    init { refresh("", force = true) }

    /**
     * 配置选择器的排除项和初始选中项。[AI修改]
     */
    fun configure(excludeDishIds: Set<Long>, initialSelected: List<DishMini>) {
        _excludeDishIds.value = excludeDishIds
        _selected.value = initialSelected.filterNot { it.id in excludeDishIds }.distinctBy { it.id }
    }

    fun setKeyword(value: String) {
        _keyword.value = value
        refresh(value, debounce = true)
    }

    /**
     * 切换菜品选中状态。[AI修改]
     */
    fun toggle(dish: DishMini, multiSelect: Boolean) {
        val current = _selected.value
        _selected.value = if (multiSelect) {
            if (current.any { it.id == dish.id }) {
                current.filterNot { it.id == dish.id }
            } else {
                current + dish
            }
        } else {
            listOf(dish)
        }
    }

    fun isSelected(dishId: Long): Boolean = _selected.value.any { it.id == dishId }

    fun confirmSelected(): List<DishMini> = _selected.value

    /**
     * 刷新当前关键词下的菜品库列表。[AI修改]
     *
     * 添加餐食页跳到新建菜品后再返回菜品库时，会主动调用它，确保刚创建的菜品排在前面。
     */
    fun refresh(keyword: String = _keyword.value, force: Boolean = false, debounce: Boolean = false) {
        if (!force && keyword == lastRefreshKeyword && _dishes.value.isNotEmpty()) return
        lastRefreshKeyword = keyword
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            // [AI修改] 用户连续输入时只执行最后一次查询；force=true 的返回刷新保持立即执行。
            if (debounce && !force) delay(com.sxdbsm.cookbook.android.util.SearchDefaults.DEBOUNCE_MS)
            _dishes.value = dishRepo.searchDishes(keyword)
        }
    }
}

/**
 * 内部组合状态。[AI修改]
 *
 * 用 private data class 避免把中间组合结构暴露给 UI。
 */
private data class ListState(
    val keyword: String,
    val dishes: List<DishMini>,
    val popular: List<DishMini>,
    val recent: List<DishMini>,
    val selected: List<DishMini>,
)
