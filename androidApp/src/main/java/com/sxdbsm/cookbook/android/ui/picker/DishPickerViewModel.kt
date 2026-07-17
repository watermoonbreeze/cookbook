package com.sxdbsm.cookbook.android.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.android.ui.dishes.DishesSortTab // [AI生成] C深度:复用菜品页分类Tab枚举(最近/喜爱/菜系ALL/家庭)
import com.sxdbsm.cookbook.android.ui.dishes.dishInitial // [AI生成] C深度:菜系/家庭档按拼音首字母排序
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.Cuisines // [AI生成] C深度:菜系固定顺序
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
    // [AI生成] C深度:选择菜品加分类导航(与菜品页/选择食材统一操作逻辑)。tab=最近/喜爱/菜系/家庭;菜系用横向chip(弹窗宽度受限,不用左竖栏)。
    val sortTab: com.sxdbsm.cookbook.android.ui.dishes.DishesSortTab = com.sxdbsm.cookbook.android.ui.dishes.DishesSortTab.RECENT,
    val selectedCuisine: String? = null,
    val availableCuisines: List<String> = emptyList(),
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
    private val _sortTab = MutableStateFlow(DishesSortTab.RECENT) // [AI生成] C深度:分类Tab(最近/喜爱/菜系/家庭)
    private val _cuisineFilter = MutableStateFlow<String?>(null) // [AI生成] C深度:菜系筛选(仅菜系Tab生效)
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

    // [AI生成] C深度:分类Tab+菜系筛选合一路(combine 最多 5 路)。
    private val viewOpts = combine(_sortTab, _cuisineFilter) { t, c -> t to c }

    val state: StateFlow<DishPickerUiState> = combine(listState, _excludeDishIds, viewOpts) { list, exclude, opts ->
        val (tab, cuisine) = opts
        val visible = list.dishes.filterNot { it.id in exclude }
        // [AI生成] C深度:菜系可选项从可见菜派生(去无菜的菜系),按 Cuisines.ALL 固定序;选中菜系若已无菜视为未选(自愈)。
        val cuisineSet = visible.mapTo(HashSet()) { it.cuisine }
        val cuisines = Cuisines.ALL.filter { it in cuisineSet }
        val effCuisine = cuisine?.takeIf { it in cuisineSet }
        // [AI修改] 无搜索浏览时把"最近做过/常做"稳定置顶(家庭选菜高度集中在那几道);搜索时保持相关性顺序。
        val priority = (list.recent.map { it.id } + list.popular.map { it.id }).toSet()
        val recentOrdered = if (list.keyword.isBlank()) visible.sortedByDescending { it.id in priority } else visible
        // [AI生成] C深度:按分类Tab过滤/排序(与菜品页口径一致)。**搜索时(keyword非空)显示全部匹配、不受Tab约束**(搜索是全局的,否则"搜了X但在喜爱Tab看不到")。
        val tabDishes = if (list.keyword.isNotBlank()) {
            visible // [AI修改] 审查⑤:搜索时全部匹配原序,直接用 visible(不复用 recentOrdered,避免命名语义漂移)
        } else when (tab) {
            DishesSortTab.RECENT -> recentOrdered
            DishesSortTab.FAVORITE -> visible.filter { it.preference > 0 }.sortedByDescending { it.preference }
            DishesSortTab.ALL -> (if (effCuisine == null) visible else visible.filter { it.cuisine == effCuisine })
                .sortedWith(compareBy({ dishInitial(it.name) }, { it.name }))
            DishesSortTab.HOME -> visible.filter { it.source == "user" }
                .sortedWith(compareBy({ dishInitial(it.name) }, { it.name }))
        }
        DishPickerUiState(
            keyword = list.keyword,
            dishes = tabDishes,
            popular = list.popular.filterNot { it.id in exclude },
            recent = list.recent.filterNot { it.id in exclude },
            selected = list.selected.filterNot { it.id in exclude },
            excludeDishIds = exclude,
            sortTab = tab,
            selectedCuisine = effCuisine,
            availableCuisines = cuisines,
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

    /** 切换分类Tab(最近/喜爱/菜系/家庭);离开菜系Tab清菜系筛选。[AI生成] C深度 */
    fun setSortTab(tab: DishesSortTab) {
        _sortTab.value = tab
        if (tab != DishesSortTab.ALL) _cuisineFilter.value = null
    }

    /** 选/取消菜系(横向chip):null=全部。[AI生成] C深度 */
    fun selectCuisine(cuisine: String?) { _cuisineFilter.value = cuisine }

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
