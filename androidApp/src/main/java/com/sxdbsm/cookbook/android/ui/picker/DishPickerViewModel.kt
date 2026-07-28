package com.sxdbsm.cookbook.android.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.MealSlot // [AI生成] v28:记一餐按当前餐次预筛
import com.sxdbsm.cookbook.android.ui.dishes.DishesSortTab // [AI生成] C深度:复用菜品页分类Tab枚举(最近/喜爱/菜系ALL/家庭)
import com.sxdbsm.cookbook.android.ui.dishes.dishInitial // [AI生成] C深度:菜系/家庭档按拼音首字母排序
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FamilyRepository // [AI生成] Phase 2 列表徽章:取当前查看成员
import com.sxdbsm.cookbook.domain.model.Cuisines // [AI生成] C深度:菜系固定顺序
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.TrafficLight // [AI生成] Phase 2 列表徽章:红绿灯色
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
    // [AI生成] v28:记一餐按当前餐次预筛(null=无预筛/其他入口)。mealSlotOnly=true只看适合该餐次,可切"全部"(告知不替决定)。
    val mealSlot: MealSlot? = null,
    val mealSlotOnly: Boolean = false,
    val mealSlotMatchCount: Int = 0, // 当前档下适合该餐次的菜数(toggle 标签显数)
    // [AI生成] Phase 2 列表徽章:当前查看成员对每道菜的红绿灯色。null=未加载/无成员/无健康档案(不显点)。
    val memberLights: Map<Long, TrafficLight>? = null,
)

/**
 * 菜品选择器 ViewModel。[AI修改]
 *
 * 为“添加餐食”和“复制/导入菜品”等入口提供统一的菜品搜索与选择逻辑。
 */
class DishPickerViewModel(
    private val dishRepo: DishRepository,
    private val familyRepo: FamilyRepository? = null, // [AI生成] Phase 2 列表徽章:取当前查看成员(选填·无则无灯)
    private val memberHealth: com.sxdbsm.cookbook.ai.MemberDishHealthUseCase? = null, // [AI生成] Phase 2 列表徽章:批量评估红绿灯
) : ViewModel() {

    private val _keyword = MutableStateFlow("") // [AI修改] 搜索关键词。
    private val _dishes = MutableStateFlow<List<DishMini>>(emptyList()) // [AI修改] 当前搜索结果。
    private val _selected = MutableStateFlow<List<DishMini>>(emptyList()) // [AI修改] 当前已选菜品。
    private val _excludeDishIds = MutableStateFlow<Set<Long>>(emptySet()) // [AI修改] 外部传入的不可选菜品 id。
    private val _sortTab = MutableStateFlow(DishesSortTab.RECENT) // [AI生成] C深度:分类Tab(最近/喜爱/菜系/家庭)
    private val _cuisineFilter = MutableStateFlow<String?>(null) // [AI生成] C深度:菜系筛选(仅菜系Tab生效)
    private val _pickerMealSlot = MutableStateFlow<MealSlot?>(null) // [AI生成] v28:当前餐次(记一餐传入,其他入口 null)
    private val _mealSlotOnly = MutableStateFlow(false) // [AI生成] v28:是否只看适合该餐次(可切"全部")
    private val _memberLights = MutableStateFlow<Map<Long, TrafficLight>?>(null) // [AI生成] Phase 2 列表徽章:当前查看成员对各菜的红绿灯
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

    // [AI生成] C深度:分类Tab+菜系筛选+餐次预筛合一路。
    private val viewOpts = combine(_sortTab, _cuisineFilter, _pickerMealSlot, _mealSlotOnly) { t, c, ms, only ->
        PickerOpts(t, c, ms, only)
    }

    val state: StateFlow<DishPickerUiState> = combine(listState, _excludeDishIds, viewOpts, _memberLights) { list, exclude, opts, lights ->
        val (tab, cuisine, mealSlot, mealSlotOnly) = opts
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
            // [AI生成] 2026-07-19:DishesSortTab 加了 SLOT(菜品页餐次档);选菜器无餐次Tab、SLOT 不可达,按拼音全量兜底保穷尽。
            DishesSortTab.SLOT -> visible.sortedWith(compareBy({ dishInitial(it.name) }, { it.name }))
        }
        // [AI生成] v28:记一餐按当前餐次预筛(DishMini.mealSlots 已含兜底)。只在传入餐次且开"只看"时收窄;可切"全部"。
        //   搜索时(keyword非空)不预筛(搜索是全局的,与Tab同口径)。matchCount 供 toggle 标签显数。
        val slotActive = mealSlot != null && list.keyword.isBlank()
        val matchCount = if (slotActive) tabDishes.count { mealSlot in it.mealSlots } else 0
        val shownDishes = if (slotActive && mealSlotOnly) tabDishes.filter { mealSlot in it.mealSlots } else tabDishes
        DishPickerUiState(
            keyword = list.keyword,
            dishes = shownDishes,
            popular = list.popular.filterNot { it.id in exclude },
            recent = list.recent.filterNot { it.id in exclude },
            selected = list.selected.filterNot { it.id in exclude },
            excludeDishIds = exclude,
            sortTab = tab,
            selectedCuisine = effCuisine,
            availableCuisines = cuisines,
            mealSlot = if (slotActive) mealSlot else null,
            mealSlotOnly = mealSlotOnly,
            mealSlotMatchCount = matchCount,
            memberLights = lights, // [AI生成] Phase 2 列表徽章:当前查看成员的红绿灯
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishPickerUiState())

    init { refresh("", force = true); evaluateMemberLights() }

    /**
     * 配置选择器的排除项和初始选中项。[AI修改]
     *
     * @param mealSlot [AI生成] v28:记一餐传入当前餐次→按餐次预筛(默认只看适合该餐次,可切"全部");其他入口传 null 不预筛。
     */
    fun configure(excludeDishIds: Set<Long>, initialSelected: List<DishMini>, mealSlot: MealSlot? = null) {
        _keyword.value = "" // [AI生成] 每次打开选择器清空上次搜索词(否则加早餐搜了"蛋"→加午餐还带"蛋")
        _excludeDishIds.value = excludeDishIds
        _selected.value = initialSelected.filterNot { it.id in excludeDishIds }.distinctBy { it.id }
        // [AI修改] 只在餐次真正变化时重置"只看/全部"默认——避免勾选菜品触发的重配(excludeDishIds/initialSelected 变)把用户切的"全部"拉回"只看适合"(Google审查建议1)。
        if (_pickerMealSlot.value != mealSlot) {
            _pickerMealSlot.value = mealSlot
            _mealSlotOnly.value = mealSlot != null // 传入餐次默认"只看适合"，用户可切全部
        }
    }

    /** 切换"只看适合该餐次 / 全部"。[AI生成] v28 记一餐不硬隐藏,告知不替用户决定。 */
    fun toggleMealSlotOnly() { _mealSlotOnly.value = !_mealSlotOnly.value }

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
            evaluateMemberLights() // [AI生成] Phase 2 列表徽章:菜品刷新后重评红绿灯(同协程·dish 已更新)
        }
    }

    /** 加载当前查看成员的红绿灯（批量评估多菜对一人）。[AI生成] Phase 2 列表徽章。 */
    private fun evaluateMemberLights() {
        val fRepo = familyRepo ?: return
        val mHealth = memberHealth ?: return
        viewModelScope.launch {
            val members = fRepo.listMembers()
            // 取当前查看成员（优先 focus 集合首位，与 resolveViewing 口径一致）
            val viewing = members.firstOrNull { it.isFocus } ?: members.firstOrNull()
            if (viewing != null && (viewing.careCategoryIds.isNotEmpty() || viewing.avoidIngredientIds.isNotEmpty())) {
                val dishIds = _dishes.value.map { it.id }
                if (dishIds.isNotEmpty()) {
                    _memberLights.value = mHealth.evaluateDishLightsForMember(dishIds, viewing)
                }
            } else {
                _memberLights.value = null // 无健康约束→不显灯(降噪)
            }
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

/** 分类Tab+菜系+餐次预筛合并载体。[AI生成] v28 */
private data class PickerOpts(
    val tab: DishesSortTab,
    val cuisine: String?,
    val mealSlot: MealSlot?,
    val only: Boolean,
)
