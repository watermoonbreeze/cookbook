package com.sxdbsm.cookbook.android.ui.dishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.MealSlot
import com.sxdbsm.cookbook.android.util.AppLogger
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 菜品列表排序 Tab。[AI修改]
 *
 * 当前 MVP 只保存选择状态，后续可让不同 Tab 对应不同查询或排序。
 */
enum class DishesSortTab { RECENT, FAVORITE, ALL, SLOT, HOME } // [AI修改] 2026-07-19:SLOT=餐次,升为与最近/喜爱/菜系/家庭同级的一级分类Tab(选中左侧列餐次分堆)

/**
 * 菜品页专属"餐次筛选"分类。[AI生成] 2026-07-19
 *
 * 每个筛选项映射到底层 MealSlot 集合。ALL(全部)=空集=不筛。
 * [AI修改] K4：上午餐/下午餐已合并为 SNACK(加餐)，筛选项直接映射 MealSlot.SNACK。
 */
enum class DishSlotFilter(val label: String, val slots: Set<MealSlot>) {
    ALL("全部", emptySet()),
    BREAKFAST("早餐", setOf(MealSlot.BREAKFAST)),
    LUNCH("中餐", setOf(MealSlot.LUNCH)),
    SNACK("加餐", setOf(MealSlot.SNACK)), // [AI修改] K4：上午餐/下午餐已合并为 SNACK
    DINNER("晚餐", setOf(MealSlot.DINNER)),
    NIGHT("宵夜", setOf(MealSlot.NIGHT_SNACK)),
    ;

    /** 该筛选项是否匹配某菜的餐次集(全部=不筛恒 true；否则命中任一底层餐次即算)。[AI生成] */
    fun matches(dishSlots: List<MealSlot>): Boolean = slots.isEmpty() || dishSlots.any { it in slots }

    companion object {
        // [AI生成] K4：纯餐次搜索词→菜品餐次筛(整词命中)。"上午餐/下午餐"保留为搜索别名，都归 SNACK(加餐)。
        private val KEYWORDS: Map<String, DishSlotFilter> = mapOf(
            "早餐" to BREAKFAST, "早饭" to BREAKFAST,
            "午餐" to LUNCH, "中餐" to LUNCH, "午饭" to LUNCH,
            "加餐" to SNACK, "上午餐" to SNACK, "上午加餐" to SNACK,
            "下午餐" to SNACK, "下午茶" to SNACK, "下午加餐" to SNACK, // "点心"不列:更可能是真实菜名/菜系(广式点心)易误判餐次筛(Google审查建议)
            "晚餐" to DINNER, "晚饭" to DINNER,
            "宵夜" to NIGHT, "夜宵" to NIGHT,
        )

        /** 整词命中纯餐次词→对应筛选项；否则 null(走普通菜名/菜系搜索)。[AI生成] */
        fun fromKeyword(kw: String): DishSlotFilter? = KEYWORDS[kw.trim()]
    }
}

/** 列表排序 + 筛选合并载体(供 combine 一路传递)。[AI生成] */
private data class ViewOpts(
    val sortTab: DishesSortTab,
    val method: String?,
    val tag: String?,
    val cuisine: String?,
    val mealSlot: DishSlotFilter, // [AI修改] v28→2026-07-19:餐次筛选改统称版(上午/下午合并"加餐")，作用所有档；ALL=不筛
)

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
    val selectedCuisine: String? = null, // [AI生成] 菜系筛选
    val availableMethods: List<String> = emptyList(), // 当前列表可选烹饪方式
    val availableTags: List<String> = emptyList(), // 当前列表可选标签
    val availableCuisines: List<String> = emptyList(), // [AI生成] 有菜的菜系(CuisineRail 只列这些，去稀疏)
    val recentCount: Int = 0, // [AI生成] 最近 Tab 菜品数(前30, 与该 Tab 实际展示一致)
    val favoriteCount: Int = 0, // [AI生成] 喜爱 Tab 菜品数(已评分 preference>0)
    val allCount: Int = 0, // [AI生成] 全部 Tab 菜品数
    val slotCount: Int = 0, // [AI生成] 2026-07-19:餐次 Tab 菜品数(当前餐次分堆筛后)
    val homeCount: Int = 0, // [AI生成] 家庭 Tab 菜品数(自建 source=user)
    val searchResults: List<DishMini> = emptyList(), // [AI生成] 搜索关键字的原始结果(不受 Tab/菜系筛选)，供搜索弹框展示
    val favoriteIds: Set<Long> = emptySet(), // [AI生成] B1：收藏菜品 id(列表置顶+★标记)
    val selectedMealSlot: DishSlotFilter = DishSlotFilter.ALL, // [AI修改] v28→2026-07-19:二级餐次筛选栏当前选中(统称版,ALL=全部)
    val searchMealSlot: DishSlotFilter? = null, // [AI修改] v28→2026-07-19:搜索命中纯餐次词时的餐次筛(搜"早餐/加餐"→按餐次筛模式，头部提示"适合X的菜品")
    val searchCuisine: String? = null, // [AI生成] 2026-07-19:搜索命中纯菜系词时的菜系(搜"家常菜"→按菜系筛模式，头部提示"X 菜品")
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
) : ViewModel() {

    private val _keyword = MutableStateFlow("") // [AI修改] 搜索框文本。
    private val _sortTab = MutableStateFlow(DishesSortTab.RECENT) // [AI修改] 当前选中的排序 Tab。
    private val _list = MutableStateFlow<List<DishMini>>(emptyList()) // [AI修改] 搜索结果列表。
    private val _refreshing = MutableStateFlow(false) // [AI生成] 下拉刷新状态。
    private val _deleteState = MutableStateFlow(DishDeleteState()) // [AI生成] 长按删除前的引用检查/确认弹框状态。
    private val _methodFilter = MutableStateFlow<String?>(null) // [AI生成] 烹饪方式筛选
    private val _tagFilter = MutableStateFlow<String?>(null) // [AI生成] 标签筛选
    private val _cuisineFilter = MutableStateFlow<String?>(null) // [AI生成] 菜系筛选
    private val _mealSlotFilter = MutableStateFlow(DishSlotFilter.ALL) // [AI修改] v28→2026-07-19:二级餐次筛选(统称版,ALL=不筛)
    private val _searchMealSlot = MutableStateFlow<DishSlotFilter?>(null) // [AI修改] v28→2026-07-19:搜索命中的纯餐次词(按餐次筛模式)
    private val _searchCuisine = MutableStateFlow<String?>(null) // [AI生成] 2026-07-19:搜索命中的纯菜系词(搜"家常菜"出该菜系全部菜·按菜系筛模式)
    private val _favoriteIds = MutableStateFlow<Set<Long>>(emptySet()) // [AI生成] B1：收藏菜品 id
    private var searchJob: Job? = null // [AI修改] 连续输入时取消上一次搜索，避免每个字符都触发数据库查询。

    private companion object {
        private const val TAG = "DishActions" // [AI生成] 菜品列表操作统一日志 Tag，便于排查长按编辑/删除。
        private const val LIST_LIMIT = 30 // [AI生成] "最近"(updated_at DESC)与"喜爱"(preference DESC) Tab 各只展示前 30 个；"全部"才展示所有。
        // [AI修改] 2026-07-19:纯餐次搜索词映射迁到 DishSlotFilter.fromKeyword(统称"加餐"含上午/下午)。
    }

    /**
     * 热门菜品列表。[AI修改]
     */
    val popular: StateFlow<List<DishMini>> = dishRepo.observePopularDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val allObserved: StateFlow<List<DishMini>> = dishRepo.observeAllDishes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // [AI生成] 排序+筛选合并为一路(combine 5 路)。
    private val viewOpts = kotlinx.coroutines.flow.combine(
        _sortTab, _methodFilter, _tagFilter, _cuisineFilter, _mealSlotFilter,
    ) { s, m, t, c, ms -> ViewOpts(s, m, t, c, ms) }

    private val baseState = kotlinx.coroutines.flow.combine(
        popular, allObserved, _list, _keyword, viewOpts,
    ) { popular, observed, searched, kw, opts ->
        val (tab, method, tag, cuisine, mealSlot) = opts
        val raw = if (kw.isBlank()) observed else searched
        // 可选筛选项从筛选前列表派生，避免选中后选项消失。
        val methods = raw.flatMap { it.cookingMethodNames }.filter { it.isNotBlank() }.distinct().sorted()
        val tags = raw.flatMap { it.tags }.filter { it.isNotBlank() }.distinct().sorted()
        // [AI生成] 菜系分类:CuisineRail 只列"有菜的菜系"(去徽菜4道/西餐1道的稀疏死胡同)，从全量派生、按 Cuisines.ALL 固定顺序。
        //   [AI修改] 审查建议2:HashSet 派生降 O(cuisines×observed)→O(observed+ALL)。
        val cuisineSet = observed.mapTo(HashSet()) { it.cuisine }
        val cuisines = com.sxdbsm.cookbook.domain.model.Cuisines.ALL.filter { it in cuisineSet }
        // [AI生成] 审查建议1:选中的菜系若已无菜(该菜系最后一道被删/改)→视为未选(否则 rail 无高亮+右侧空态的自愈死角)。仅局部修正不回写 flow。
        val effectiveCuisine = cuisine?.takeIf { it in cuisineSet }
        val filtered = raw.filter { d ->
            (method == null || method in d.cookingMethodNames) &&
                (tag == null || tag in d.tags) &&
                // [AI修改] 菜系筛选只在"菜系"Tab(ALL)生效；最近/喜爱不受菜系影响。
                (tab != DishesSortTab.ALL || effectiveCuisine == null || effectiveCuisine == d.cuisine) &&
                // [AI修改] 2026-07-19:餐次升为一级Tab后,餐次筛选只在"餐次"(SLOT)档生效(与菜系筛只在菜系档一致);"加餐"统称命中上午/下午任一。
                (tab != DishesSortTab.SLOT || mealSlot.matches(d.mealSlots))
        }
        // [AI生成] 最近(updated_at DESC)、喜爱(已评分按 preference DESC)各取前 30；全部/家庭展示所有。计数与各 Tab 展示一致。
        val favorites = filtered.filter { it.preference > 0 }.sortedByDescending { it.preference }.take(LIST_LIMIT)
        val recentList = filtered.take(LIST_LIMIT)
        val userDishes = filtered.filter { it.source == "user" } // [AI生成] 家庭=用户自建菜品
        val listForTab = when (tab) {
            DishesSortTab.RECENT -> recentList
            DishesSortTab.FAVORITE -> favorites
            DishesSortTab.ALL -> filtered
            DishesSortTab.SLOT -> filtered // [AI生成] 2026-07-19:餐次档=按左侧餐次分堆筛后的全部(拼音分组·同菜系档)
            DishesSortTab.HOME -> userDishes
        }
        DishesUiState(
            popular = popular, all = sortDishes(listForTab, tab), keyword = kw, sortTab = tab,
            selectedMethod = method, selectedTag = tag, selectedCuisine = effectiveCuisine,
            availableMethods = methods, availableTags = tags, availableCuisines = cuisines,
            recentCount = recentList.size, favoriteCount = favorites.size, allCount = filtered.size,
            slotCount = filtered.size, // [AI生成] 2026-07-19:餐次档计数(当前餐次分堆筛后)
            homeCount = userDishes.size,
            // [AI生成] 搜索弹框用原始搜索结果(不叠加 Tab/菜系筛选)：搜索是全局的。
            searchResults = if (kw.isBlank()) emptyList() else searched,
            selectedMealSlot = mealSlot,
        )
    }

    // [AI生成] 2026-07-19:两个搜索分类态(餐次/菜系)合一路，避免 uiState combine 超 5 路 typed 重载。
    private val searchClassify = kotlinx.coroutines.flow.combine(_searchMealSlot, _searchCuisine) { slot, cuisine -> slot to cuisine }

    val uiState: StateFlow<DishesUiState> = kotlinx.coroutines.flow.combine(
        baseState, _refreshing, _deleteState, _favoriteIds, searchClassify,
    ) { state, refreshing, deleteState, favIds, searchClass ->
        val (searchSlot, searchCuisine) = searchClass
        state.copy(
            refreshing = refreshing,
            deleteState = deleteState,
            favoriteIds = favIds,
            searchMealSlot = searchSlot,
            searchCuisine = searchCuisine, // [AI生成] 2026-07-19:菜系搜索模式
            // [AI生成] B1：收藏置顶——稳定排序把收藏的菜提到最前，保留各 Tab 原有次序。
            all = state.all.sortedByDescending { it.id in favIds },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishesUiState())

    init {
        refresh()
        loadFavorites()
    }

    /** 重载收藏 id 集合。[AI修改] 公开：详情页收藏后返回列表需即时刷新置顶(ON_RESUME 调用)。 */
    fun loadFavorites() {
        viewModelScope.launch { runCatching { dishRepo.favoriteDishIds() }.onSuccess { _favoriteIds.value = it } }
    }

    /** 收藏/取消收藏(置顶)。[AI生成] B1 */
    fun toggleFavorite(dishId: Long) {
        viewModelScope.launch {
            val fav = dishId !in _favoriteIds.value
            runCatching { dishRepo.setDishFavorite(dishId, fav) }.onSuccess { loadFavorites() }
        }
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

    /** 直接选择菜系(左侧菜系栏)：null=全部(不筛)。[AI生成] */
    fun selectCuisine(cuisine: String?) { _cuisineFilter.value = cuisine }

    /** 选择二级餐次筛选(ALL=全部/不筛，作用所有档；"加餐"统称含上午/下午)。[AI修改] v28→2026-07-19 */
    fun selectMealSlot(filter: DishSlotFilter) { _mealSlotFilter.value = filter }

    /** 一键清除所有筛选(烹饪方式/标签/菜系/餐次/关键词)。[AI生成] 叠了多个筛选后回到全量免逐个再点。 */
    fun clearFilters() {
        _methodFilter.value = null
        _tagFilter.value = null
        _cuisineFilter.value = null
        _mealSlotFilter.value = DishSlotFilter.ALL
        setKeyword("")
    }

    /**
     * 从搜索弹框点某道菜：跳到它所在菜系分类下，并清空搜索关闭弹框。[AI生成]
     *
     * 切到「菜系」Tab + 选中该菜的菜系(空菜系归到"全部")，随后由页面打开该菜详情。
     */
    fun openFromSearch(dish: DishMini) {
        _sortTab.value = DishesSortTab.ALL
        _cuisineFilter.value = dish.cuisine.ifBlank { null }
        setKeyword("")
    }

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
            AppLogger.d(TAG, "request delete dish: id=${dish.id} name=${dish.name}")
            _deleteState.value = DishDeleteState(checking = true)
            runCatching { dishRepo.listMealReferencesByDish(dish.id) }
                .onSuccess { references ->
                    AppLogger.d(TAG, "delete reference check: id=${dish.id} refs=${references.size}")
                    _deleteState.value = if (references.isNotEmpty()) {
                        DishDeleteState(warningDish = dish, warningReferences = references)
                    } else {
                        DishDeleteState(pendingDish = dish)
                    }
                }
                .onFailure { error ->
                    AppLogger.e(TAG, "delete reference check failed: id=${dish.id}", error)
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
            AppLogger.d(TAG, "confirm delete dish: id=${dish.id} name=${dish.name}")
            _deleteState.value = _deleteState.value.copy(checking = true)
            runCatching { dishRepo.deleteDish(dish.id) }
                .onSuccess {
                    AppLogger.d(TAG, "delete dish success: id=${dish.id}")
                    _deleteState.value = DishDeleteState()
                    refresh()
                }
                .onFailure { error ->
                    AppLogger.e(TAG, "delete dish failed: id=${dish.id}", error)
                    _deleteState.value = DishDeleteState(errorMessage = "删除菜品失败，请稍后重试")
                }
        }
    }

    fun dismissDeleteDialog() {
        AppLogger.d(TAG, "dismiss delete dialog")
        _deleteState.value = DishDeleteState()
    }

    private fun sortDishes(list: List<DishMini>, tab: DishesSortTab): List<DishMini> =
        when (tab) {
            DishesSortTab.RECENT -> list
            DishesSortTab.FAVORITE -> list.sortedByDescending { it.preference }
            // 全部/餐次/家庭都按拼音首字母分组排序(字母检索)。[AI修改] 2026-07-19:餐次档同菜系档走字母分组。
            DishesSortTab.ALL, DishesSortTab.SLOT, DishesSortTab.HOME -> list.sortedWith(compareBy({ dishInitial(it.name) }, { it.name }))
        }


    /**
     * 执行菜品搜索。[AI生成]
     *
     * 首次加载立即查询；用户输入时延迟一小段时间，连续输入会取消旧任务，只保留最后一次关键词。
     */
    private fun searchNow(keyword: String, debounce: Boolean = false, force: Boolean = false) {
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            if (debounce) delay(com.sxdbsm.cookbook.android.util.SearchDefaults.DEBOUNCE_MS)
            _refreshing.value = true
            if (keyword.isBlank()) {
                _searchMealSlot.value = null // [AI生成] v28：清空搜索退出按餐次筛模式
                _searchCuisine.value = null // [AI生成] 2026-07-19:清空退出按菜系筛模式
                if (force) delay(120) // [AI生成] 空关键词列表来自 Flow，保留一个短刷新反馈。
            } else {
                // [AI修改] v28→2026-07-19:整词命中分类词→按分类筛(不是菜名搜索)：①纯餐次词(早餐/中餐/加餐…统称"加餐"=上午/下午)→查 mealSlots ②纯菜系词(家常菜/川菜…)→查 cuisine，出该分类全部菜、头部提示；否则普通菜名搜索。
                val slot = DishSlotFilter.fromKeyword(keyword)
                // [AI修改] Google审查建议1:泛词"其他"作菜系值语义弱、会遮蔽名字含"其他"的菜名搜索→不纳入菜系整词命中。
                val cuisine = keyword.trim().takeIf { it in com.sxdbsm.cookbook.domain.model.Cuisines.ALL && it != "其他" }
                when {
                    slot != null -> {
                        _searchMealSlot.value = slot
                        _searchCuisine.value = null
                        // [AI修改] 用一次性实时查询(非 allObserved.value)：stateIn(WhileSubscribed) 无订阅时 .value 冻结在初值会静默返回空(红线)。
                        _list.value = dishRepo.observeAllDishes().first().filter { slot.matches(it.mealSlots) }
                    }
                    cuisine != null -> {
                        _searchCuisine.value = cuisine
                        _searchMealSlot.value = null
                        _list.value = dishRepo.observeAllDishes().first().filter { it.cuisine == cuisine }
                    }
                    else -> {
                        _searchMealSlot.value = null
                        _searchCuisine.value = null
                        _list.value = dishRepo.searchDishes(keyword)
                    }
                }
            }
            _refreshing.value = false
        }
    }
}
