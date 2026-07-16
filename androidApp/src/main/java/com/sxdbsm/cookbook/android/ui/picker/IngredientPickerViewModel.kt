package com.sxdbsm.cookbook.android.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.HealthProfileRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.domain.model.DishIngredientMatch
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.domain.model.MeasurementUnit
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 左侧分类节点（手风琴展开后的扁平化项）。[AI修改] */
data class CategoryNode(
    val category: FoodCategory,
    val level: Int,    // [AI修改] 1 = 一级；后续可继续展开为任意层级。
    val expanded: Boolean = false,
)

/**
 * 食材浏览器顶部主分类。[AI修改]
 *
 * 食材页和菜品编辑里的食材选择器共用同一套入口。
 */
enum class IngredientMainTab(val label: String) {
    RECENT("最近"),
    PANTRY("库存"), // [AI生成] 我家食材（在手库存），无左侧分类树、平铺展示。
    GENERAL("常规"),
    NUTRITION("营养"),
    CARE("调养"),
    CUSTOM("家庭"), // [AI修改] 用户自建食材分类，命名与菜品"家庭"一致
}

/**
 * 食材选择器 UI 状态。[AI修改]
 *
 * 左侧分类树、右侧食材列表、已选食材、新建食材状态都集中在这里。
 */
data class IngredientPickerUiState(
    val keyword: String = "",
    val mainTab: IngredientMainTab = IngredientMainTab.RECENT, // [AI修改] 食材浏览器默认进入最近。
    val tree: List<CategoryNode> = emptyList(),  // "全部" + 一级 + 展开的二级
    val allCategories: List<FoodCategory> = emptyList(), // [AI生成] 添加食材和分类管理使用完整分类列表，不依赖左侧展开状态。
    val selectedCategoryId: Long? = null,        // -1 表示当前主分类下“全部”。
    val ingredients: List<Ingredient> = emptyList(),
    val availableUnits: List<MeasurementUnit> = emptyList(), // [AI生成] 食材完整编辑表单的默认单位选项。
    val selectedIds: Set<Long> = emptySet(),
    val selectedIngredients: List<Ingredient> = emptyList(),
    val excludeIngredientIds: Set<Long> = emptySet(),
    val selectionMode: Boolean = true, // [AI生成] bug1：仅"选择食材"场景新建后自动选中；食材管理页(false)新建不打钩。
    val creatingIngredient: Boolean = false,
    val createError: String? = null,
    val operationError: String? = null,
    val lastCreatedIngredientId: Long? = null,
    val lastSavedIngredientId: Long? = null, // [AI生成] 完整编辑表单保存成功后用于关闭弹层。
    val editorLoading: Boolean = false,
    val editorIngredientId: Long? = null,
    val editorCategoryIds: Set<Long> = emptySet(),
    val editorDetail: IngredientDetail? = null,
    val editorNutrition: com.sxdbsm.cookbook.domain.model.IngredientNutrition? = null, // [AI生成] Item4：编辑器预填的已有营养
    val editorCareRules: List<IngredientCareRule> = emptyList(),
    val detailLoading: Boolean = false,
    val detailIngredientId: Long? = null,
    val detailCategories: List<FoodCategory> = emptyList(),
    val detailInfo: IngredientDetail? = null,
    val detailCareRules: List<IngredientCareRule> = emptyList(),
    val detailDishMatches: List<DishIngredientMatch> = emptyList(),
    val filterDishMatches: List<DishIngredientMatch> = emptyList(),
    val canLoadMoreIngredients: Boolean = false, // [AI生成] 分类筛选结果按 30 条一页展示，避免一次渲染过多食材。
    val inactiveIngredients: List<Ingredient> = emptyList(), // [AI生成] 回收站：失效的自定义食材列表。
    val pantryIngredientIds: Set<Long> = emptySet(), // [AI生成] 在手库存食材 id 集合，用于详情按钮显示「加入/移出库存」与标记「家里有」。
    val pantryRemaining: Map<Long, Int> = emptyMap(), // [AI生成] 库存剩余份数(份数-今天及过去占用)，库存Tab展示；0=已用尽仍在库。
    val pantryServings: Map<Long, Int> = emptyMap(), // [AI生成] 库存原始份数(用户提供总量)，用于减份数。
    val searchResults: List<Ingredient> = emptyList(), // [AI生成] 全局搜索下拉结果（跨全库，不限当前 Tab）。
    val highlightIngredientId: Long? = null, // [AI生成] 搜索跳转后在网格中高亮定位的食材。
    val enabledCareCategoryIds: Set<Long> = emptySet(), // [AI生成] 用户已启用的健康档案病种(care 分类 id)，详情忌口区置顶高亮。
)

private const val ALL_CATEGORY_ID = -1L // [AI生成] 虚拟分类：当前主分类下全部。

/**
 * 食材选择器 ViewModel。[AI修改]
 *
 * 管理分类展开、食材搜索、多选确认和用户自定义食材创建。
 */
class IngredientPickerViewModel(
    private val ingredientRepo: IngredientRepository,
    private val categoryRepo: FoodCategoryRepository,
    private val dishRepo: DishRepository,
    private val pantryRepo: PantryRepository,
    private val healthRepo: HealthProfileRepository,
    private val nutritionRepo: com.sxdbsm.cookbook.data.repository.NutritionRepository, // [AI生成] Item4：自定义食材营养录入
) : ViewModel() {

    private val _state = MutableStateFlow(IngredientPickerUiState()) // [AI修改] 内部可变选择器状态。
    val state: StateFlow<IngredientPickerUiState> = _state.asStateFlow() // [AI修改] 对 UI 暴露只读状态。
    private var searchJob: Job? = null // [AI修改] 食材搜索防抖任务，避免每个字符都访问数据库。
    private var currentIngredientResult: List<Ingredient> = emptyList() // [AI生成] 保存当前查询全集，UI 只分页展示前 N 条。
    private var currentIngredientPage: Int = 1 // [AI生成] 当前食材分页页码。

    // [AI修改] ViewModel 创建后立即加载分类和全部食材，页面首屏可直接显示。
    init { loadCategories(); loadAllIngredients(); loadUnits(); loadPantryIds(); loadHealthProfiles() }

    /**
     * 监听已启用健康档案病种，供详情忌口区置顶高亮。[AI生成]
     */
    private fun loadHealthProfiles() {
        viewModelScope.launch {
            healthRepo.observeEnabled().collect { profiles ->
                _state.value = _state.value.copy(enabledCareCategoryIds = profiles.map { it.crowdTypeId }.toSet())
            }
        }
    }

    /**
     * 配置需要排除的食材。[AI修改]
     */
    fun configure(excludeIngredientIds: Set<Long>, selectionMode: Boolean = true) {
        _state.value = _state.value.copy(
            excludeIngredientIds = excludeIngredientIds,
            selectionMode = selectionMode, // [AI生成] bug1

            selectedIds = emptySet(),
            selectedIngredients = emptyList(),
            ingredients = _state.value.ingredients.filterNot { it.id in excludeIngredientIds },
            canLoadMoreIngredients = currentIngredientResult.size > _state.value.ingredients.size,
            createError = null,
            operationError = null,
            lastCreatedIngredientId = null,
            lastSavedIngredientId = null,
        )
    } // [AI修改] 选择器每次打开都应是干净状态，避免取消后下次打开仍保留上次已选。

    private fun loadUnits() {
        viewModelScope.launch {
            _state.value = _state.value.copy(availableUnits = ingredientRepo.listMeasurementUnits())
        }
    }

    /** 新建自定义单位入库并刷新单位列表；回调返回新单位 id 供表单选中。[AI生成] 单位库 */
    fun addUnit(name: String, onCreated: (Long?) -> Unit) {
        viewModelScope.launch {
            val id = ingredientRepo.ensureMeasurementUnit(name)
            _state.value = _state.value.copy(availableUnits = ingredientRepo.listMeasurementUnits())
            onCreated(id)
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val tops = categoryRepo.listTopLevel()
            val allCategories = categoryRepo.listAll()
            _state.value = _state.value.copy(
                tree = buildTreeForTab(IngredientMainTab.RECENT, tops, allCategories),
                allCategories = allCategories,
                selectedCategoryId = ALL_CATEGORY_ID /* 全部 */,
            )
        }
    }

    /**
     * 配置管理页使用的顶部主维度。[AI生成]
     */
    fun selectMainTab(tab: IngredientMainTab, force: Boolean = false) {
        val current = _state.value
        if (current.mainTab == tab && !force) return
        searchJob?.cancel()
        viewModelScope.launch {
            val allCategories = current.allCategories.ifEmpty { categoryRepo.listAll() }
            val tops = allCategories.filter { it.parentId == null }
            val ingredients = loadAllForTab(tab, "")
            // [AI修改] 基于最新 _state.value 写回(非启动时捕获的旧快照 current)：
            // 否则挂起期间并发更新的字段(如 loadUnits 填的 availableUnits)会被旧快照冲掉→单位下拉空。
            _state.value = _state.value.copy(
                mainTab = tab,
                keyword = "",
                selectedCategoryId = ALL_CATEGORY_ID,
                allCategories = allCategories,
                tree = buildTreeForTab(tab, tops, allCategories),
                searchResults = emptyList(),
                highlightIngredientId = null, // [AI生成] 切 Tab 清掉搜索下拉与高亮。
            )
            applyIngredientResult(ingredients.withoutExcluded(), resetPage = true)
        }
    }

    private fun loadAllIngredients() {
        viewModelScope.launch {
            applyIngredientResult(loadAllForTab(_state.value.mainTab, "").withoutExcluded(), resetPage = true)
        }
    }

    /**
     * 全局搜索关键词变化。[AI修改]
     *
     * 改为**全局下拉搜索**：跨全库（不限当前 Tab/分类）按字符包含匹配，结果放进 searchResults 供下拉展示；
     * 不再过滤当前 Tab 网格。清空则收起下拉。
     */
    fun setKeyword(kw: String) {
        _state.value = _state.value.copy(keyword = kw, highlightIngredientId = null)
        searchJob?.cancel()
        if (kw.isBlank()) {
            _state.value = _state.value.copy(searchResults = emptyList())
            return
        }
        searchJob = viewModelScope.launch {
            delay(com.sxdbsm.cookbook.android.util.SearchDefaults.DEBOUNCE_MS) // [AI修改] 连续输入时只保留最后一次搜索，降低卡顿。
            val results = ingredientRepo.search(kw).filterNot { it.id in _state.value.excludeIngredientIds }
            _state.value = _state.value.copy(searchResults = results)
        }
    }

    /**
     * 点击搜索结果：跳到该食材所属分类并在网格高亮定位。[AI生成]
     *
     * 切到常规 Tab，选中该食材最具体的常规分类（展开分类树到它），加载该分类食材并高亮目标；不自动打开详情。
     */
    fun jumpToIngredient(ingredient: Ingredient) {
        searchJob?.cancel()
        viewModelScope.launch {
            val all = _state.value.allCategories.ifEmpty { categoryRepo.listAll() }
            fun depth(id: Long): Int {
                var d = 0
                var c = all.firstOrNull { it.id == id }
                while (c?.parentId != null) { d++; c = all.firstOrNull { it.id == c!!.parentId } }
                return d
            }
            val general = ingredientRepo.listCategories(ingredient.id)
                .filter { it.dimension == "general" && it.crowdTypeId == null }
            val target = general.maxByOrNull { depth(it.id) } ?: general.firstOrNull()
            if (target == null) {
                // 无常规分类（理论上不该发生）：切到常规全部并高亮。
                _state.value = _state.value.copy(
                    mainTab = IngredientMainTab.GENERAL,
                    keyword = "",
                    searchResults = emptyList(),
                    selectedCategoryId = ALL_CATEGORY_ID,
                    allCategories = all,
                    tree = buildTreeForTab(IngredientMainTab.GENERAL, all.filter { it.parentId == null }, all),
                    highlightIngredientId = ingredient.id,
                )
                applyIngredientResult(loadAllForTab(IngredientMainTab.GENERAL, "").withoutExcluded(), resetPage = true)
                return@launch
            }
            val ingredients = loadByCategoryWithChildren(target).withoutExcluded()
            _state.value = _state.value.copy(
                mainTab = IngredientMainTab.GENERAL,
                keyword = "",
                searchResults = emptyList(),
                selectedCategoryId = target.id,
                allCategories = all,
                tree = buildGeneralTreeExpandedTo(target.id, all),
                highlightIngredientId = ingredient.id,
            )
            applyIngredientResult(ingredients, resetPage = true)
        }
    }

    /**
     * 构建常规分类树并沿目标分类的祖先链展开，使目标节点可见且选中。[AI生成]
     */
    private fun buildGeneralTreeExpandedTo(targetId: Long, all: List<FoodCategory>): List<CategoryNode> {
        val ancestors = mutableSetOf<Long>()
        var cur = all.firstOrNull { it.id == targetId }
        while (cur != null) {
            ancestors.add(cur.id)
            cur = cur.parentId?.let { pid -> all.firstOrNull { it.id == pid } }
        }
        fun isGeneral(c: FoodCategory) = c.dimension == "general" && c.crowdTypeId == null && c.source != "user"
        fun build(parentId: Long?, level: Int): List<CategoryNode> =
            all.filter { it.parentId == parentId && isGeneral(it) }
                .sortedBy { it.sortOrder }
                .flatMap { cat ->
                    val hasChildren = all.any { it.parentId == cat.id && isGeneral(it) }
                    val expanded = hasChildren && cat.id in ancestors
                    listOf(CategoryNode(cat.copy(hasChildren = hasChildren), level = level, expanded = expanded)) +
                        if (expanded) build(cat.id, level + 1) else emptyList()
                }
        return build(null, 1)
    }

    fun selectAll() {
        searchJob?.cancel()
        viewModelScope.launch {
            val ingredients = loadAllForTab(_state.value.mainTab, "")
            _state.value = _state.value.copy(
                keyword = "", // [AI修改] 选择“全部”时清空搜索条件，避免旧关键词影响后续刷新。
                selectedCategoryId = ALL_CATEGORY_ID,
                highlightIngredientId = null,
            )
            applyIngredientResult(ingredients.withoutExcluded(), resetPage = true)
        }
    }

    fun toggleExpand(node: CategoryNode) {
        viewModelScope.launch {
            if (!node.category.hasChildren) return@launch
            val current = _state.value.tree
            val idx = current.indexOf(node)
            if (idx < 0) return@launch

            val newTree = current.toMutableList()
            if (node.expanded) {
                // [AI修改] 收起：移除当前节点后所有更深层级的后代。
                val end = newTree.subList(idx + 1, newTree.size).indexOfFirst { it.level <= node.level }
                val to = if (end < 0) newTree.size else idx + 1 + end
                newTree.subList(idx + 1, to).clear()
                newTree[idx] = node.copy(expanded = false)
            } else {
                // [AI修改] 展开：按当前节点层级插入下一层，支持任意深度分类树。
                val children = categoryRepo.listChildren(node.category.id).filterForTab(_state.value.mainTab)
                newTree.add(idx + 1, *children.map { CategoryNode(it, level = node.level + 1) }.toTypedArray())
                newTree[idx] = node.copy(expanded = true)
            }
            _state.value = _state.value.copy(tree = newTree)
        }
    }

    /**
     * 选择某个分类并加载对应食材。[AI修改]
     */
    fun selectCategory(node: CategoryNode) {
        viewModelScope.launch {
            val cat = node.category
            val ingredients = loadByCategoryWithChildren(cat)
            _state.value = _state.value.copy(
                selectedCategoryId = cat.id,
                highlightIngredientId = null, // [AI生成] 手动切分类清掉搜索高亮。
            )
            applyIngredientResult(ingredients.withoutExcluded(), resetPage = true)
        }
    }

    /**
     * 继续展示下一页食材。[AI生成]
     */
    fun loadMoreIngredients() {
        if (!_state.value.canLoadMoreIngredients) return
        currentIngredientPage += 1
        _state.value = _state.value.copy(
            ingredients = currentIngredientResult.take(currentIngredientPage * PAGE_SIZE),
            canLoadMoreIngredients = currentIngredientResult.size > currentIngredientPage * PAGE_SIZE,
        )
    }

    /**
     * 新增用户自建分类。[AI生成]
     */
    fun createCategory(name: String, parentId: Long?) {
        viewModelScope.launch {
            runCatching {
                categoryRepo.createUserCategory(name = name, parentId = parentId)
            }.onSuccess {
                refreshCategories()
            }.onFailure { error ->
                _state.value = _state.value.copy(operationError = error.message ?: "新增分类失败")
            }
        }
    }

    /**
     * 编辑用户自建分类。[AI生成]
     */
    fun renameCategory(category: FoodCategory, name: String) {
        viewModelScope.launch {
            runCatching {
                categoryRepo.renameUserCategory(category.id, name, category.icon)
            }.onSuccess {
                refreshCategories()
                reloadCurrentList()
            }.onFailure { error ->
                _state.value = _state.value.copy(operationError = error.message ?: "编辑分类失败")
            }
        }
    }

    /**
     * 删除用户自建分类。[AI生成]
     */
    fun deleteCategory(category: FoodCategory) {
        viewModelScope.launch {
            runCatching {
                categoryRepo.deleteUserCategory(category.id)
            }.onSuccess {
                refreshCategories()
                if (_state.value.selectedCategoryId == category.id) {
                    selectAll()
                }
            }.onFailure { error ->
                _state.value = _state.value.copy(operationError = error.message ?: "删除分类失败")
            }
        }
    }

    /**
     * 切换食材选中状态。[AI修改]
     */
    fun toggleSelection(ingredient: Ingredient) {
        val ingredientId = ingredient.id
        val current = _state.value.selectedIds
        val selected = _state.value.selectedIngredients
        val removing = ingredientId in current
        _state.value = _state.value.copy(
            selectedIds = if (removing) current - ingredientId else current + ingredientId,
            selectedIngredients = if (removing) {
                selected.filterNot { it.id == ingredientId }
            } else {
                (selected + ingredient).distinctBy { it.id }
            },
        )
    }

    /** 清空已选(食材页"组成菜品"多选态退出用)。[AI生成] */
    fun clearSelection() {
        _state.value = _state.value.copy(selectedIds = emptySet(), selectedIngredients = emptyList())
    }

    /**
     * 读取食材详情弹层需要展示的真实数据。[AI生成]
     */
    fun loadIngredientDetail(ingredient: Ingredient) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                detailLoading = true,
                detailIngredientId = ingredient.id,
                detailCategories = emptyList(),
                detailInfo = null,
                detailCareRules = emptyList(),
                detailDishMatches = emptyList(),
            )
            runCatching {
                val categories = ingredientRepo.listCategories(ingredient.id)
                val detail = ingredientRepo.getIngredientDetail(ingredient.id)
                val careRules = ingredientRepo.listCareRules(ingredient.id)
                val matches = dishRepo.findDishesByIngredients(listOf(ingredient.id), limit = 8)
                IngredientDetailPayload(categories, detail, careRules, matches)
            }.onSuccess { payload ->
                _state.value = _state.value.copy(
                    detailLoading = false,
                    detailIngredientId = ingredient.id,
                    detailCategories = payload.categories,
                    detailInfo = payload.detail,
                    detailCareRules = payload.careRules,
                    detailDishMatches = payload.matches,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    detailLoading = false,
                    operationError = error.message ?: "加载食材详情失败",
                )
            }
        }
    }

    /**
     * 按当前已选食材查可做菜品。[AI生成]
     */
    fun findDishesBySelectedIngredients() {
        viewModelScope.launch {
            val ids = _state.value.selectedIds.toList()
            val matches = dishRepo.findDishesByIngredients(ids, limit = 20)
            _state.value = _state.value.copy(filterDishMatches = matches)
        }
    }

    fun confirmSelected(): List<Ingredient> {
        return _state.value.selectedIngredients
    }

    /**
     * 创建用户自定义食材，并默认选中它。[AI修改]
     */
    fun createUserIngredient(name: String, alias: String = "", imagePath: String = "", thumbnailPath: String = "", categoryId: Long? = null) {
        val trimmedName = name.trim()
        val trimmedAlias = alias.trim()
        val trimmedImagePath = imagePath.trim()
        val trimmedThumbnailPath = thumbnailPath.trim()
        if (trimmedName.isBlank()) {
            _state.value = _state.value.copy(createError = "请输入食材名称")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(creatingIngredient = true, createError = null)
            runCatching {
                val id = ingredientRepo.createUserIngredient(trimmedName, trimmedAlias, trimmedImagePath, trimmedThumbnailPath, categoryId)
                Ingredient(
                    id = id,
                    name = trimmedName,
                    alias = trimmedAlias,
                    imagePath = trimmedImagePath, // [AI修改] 新建食材弹框的可选图片路径。
                    thumbnailPath = trimmedThumbnailPath, // [AI生成] 新建食材同步保存缩略图路径。
                    source = "user",
                )
            }.onSuccess { ingredient ->
                // [AI修改] E2：新建后跳到该食材所在分类("家庭"Tab + 选中的分类/全部)。
                // [AI修改] bug1：仅"选择食材"场景(selectionMode)保持它为已选(打钩)；食材管理页新建不打钩。
                val autoSelect = _state.value.selectionMode
                _state.value = _state.value.copy(
                    creatingIngredient = false,
                    selectedIds = if (autoSelect) _state.value.selectedIds + ingredient.id else _state.value.selectedIds,
                    selectedIngredients = if (autoSelect) (_state.value.selectedIngredients + ingredient).distinctBy { it.id } else _state.value.selectedIngredients,
                    lastCreatedIngredientId = ingredient.id,
                    lastSavedIngredientId = ingredient.id,
                    mainTab = IngredientMainTab.CUSTOM, // 家庭(用户自建)
                    selectedCategoryId = categoryId ?: -1L, // 选了分类则跳该分类，否则家庭-全部
                    keyword = "", // 清空搜索，回到分类视图
                )
                reloadCurrentList()
            }.onFailure {
                _state.value = _state.value.copy(
                    creatingIngredient = false,
                    createError = "创建失败，可能已存在同名食材",
                )
            }
        }
    }

    /**
     * 加载完整食材编辑表单需要的关联数据。[AI生成]
     */
    fun loadIngredientEditor(ingredient: Ingredient?) {
        val ingredientId = ingredient?.id
        if (ingredientId == null) {
            _state.value = _state.value.copy(
                editorLoading = false,
                editorIngredientId = null,
                editorCategoryIds = emptySet(),
                editorDetail = null,
                editorNutrition = null,
                editorCareRules = emptyList(),
                createError = null,
                lastSavedIngredientId = null,
            )
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(editorLoading = true, editorIngredientId = ingredientId, createError = null, lastSavedIngredientId = null)
            runCatching {
                val categoryIds = ingredientRepo.listCategoryIds(ingredientId).toSet()
                val detail = ingredientRepo.getIngredientDetail(ingredientId)
                val careRules = ingredientRepo.listCareRules(ingredientId)
                val nutrition = nutritionRepo.ingredientNutrition(ingredientId) // [AI生成] Item4：预填已有营养
                listOf(categoryIds, detail, careRules, nutrition)
            }.onSuccess { loaded ->
                @Suppress("UNCHECKED_CAST")
                val categoryIds = loaded[0] as Set<Long>
                val detail = loaded[1] as IngredientDetail?
                val careRules = loaded[2] as List<IngredientCareRule>
                val nutrition = loaded[3] as com.sxdbsm.cookbook.domain.model.IngredientNutrition?
                _state.value = _state.value.copy(
                    editorLoading = false,
                    editorIngredientId = ingredientId,
                    editorCategoryIds = categoryIds,
                    editorDetail = detail,
                    editorNutrition = nutrition,
                    editorCareRules = careRules,
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    editorLoading = false,
                    operationError = error.message ?: "加载食材编辑信息失败",
                )
            }
        }
    }

    /**
     * 保存完整食材编辑表单。[AI生成]
     */
    fun saveIngredientEditor(
        ingredient: Ingredient?,
        name: String,
        alias: String,
        imagePath: String,
        thumbnailPath: String,
        defaultUnitId: Long?,
        categoryIds: List<Long>,
        detail: IngredientDetail,
        careRules: List<IngredientCareRule>,
        nutrition: com.sxdbsm.cookbook.domain.model.IngredientNutrition? = null, // [AI生成] Item4：自定义营养(每100g)，非空且有值才写
        foodGroup: String = "", // [AI生成] A1：营养大类(FoodGroup.Group 名，空=未选)——存 food_group + 挂到对应顶层分类。
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) {
            _state.value = _state.value.copy(createError = "请输入食材名称")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(creatingIngredient = true, createError = null, lastSavedIngredientId = null)
            runCatching {
                // [AI生成] A1：营养大类 → 顶层分类 id；及全部可归类顶层分类 id 集(编辑切换时去旧留新)。
                val groupCatId = foodGroup.takeIf { it.isNotBlank() }?.let { g ->
                    runCatching { com.sxdbsm.cookbook.domain.FoodGroup.Group.valueOf(g) }.getOrNull()?.let { grp ->
                        val catName = com.sxdbsm.cookbook.domain.FoodGroup.CATEGORY_NAME[grp]
                        _state.value.allCategories.firstOrNull { it.parentId == null && it.name == catName }?.id
                    }
                }
                val foodGroupCatIds = _state.value.allCategories
                    .filter { it.parentId == null && it.name in com.sxdbsm.cookbook.domain.FoodGroup.CATEGORY_NAMES }
                    .map { it.id }.toSet()
                // [AI修改] 营养大类必选,其它分类可选：创建时把大类分类并入。
                val ingredientId = ingredient?.id ?: ingredientRepo.createUserIngredient(
                    name = trimmedName,
                    alias = alias.trim(),
                    imagePath = imagePath.trim(),
                    thumbnailPath = thumbnailPath.trim(),
                    defaultUnitId = defaultUnitId,
                    categoryIds = (categoryIds + listOfNotNull(groupCatId)).distinct(),
                )
                if (ingredient?.source == "preset") {
                    ingredientRepo.updateUserIngredient(
                        id = ingredientId,
                        name = ingredient.name,
                        alias = alias.trim(),
                        imagePath = imagePath.trim(),
                        thumbnailPath = thumbnailPath.trim(),
                        defaultUnitId = ingredient.defaultUnitId,
                    )
                } else if (ingredient != null) {
                    ingredientRepo.updateUserIngredient(
                        id = ingredientId,
                        name = trimmedName,
                        alias = alias.trim(),
                        imagePath = imagePath.trim(),
                        thumbnailPath = thumbnailPath.trim(),
                        defaultUnitId = defaultUnitId,
                    )
                    // [AI修改] 编辑器 UI 只维护用户自建普通分类；保存时合并该食材既有的其他维度分类关联
                    // （营养/调养/预设普通分类等），避免编辑自定义食材时这些关联被静默清空。
                    val preservedCategoryIds = if (_state.value.editorIngredientId == ingredientId) {
                        _state.value.editorCategoryIds.filterNot { id ->
                            _state.value.allCategories.firstOrNull { it.id == id }?.isEditableUserGeneralCategory() == true
                        }
                    } else {
                        emptyList()
                    }
                    // [AI修改] A1：保留其它维度分类，但顶层营养大类去旧留新(排除所有可归类顶层，再并入选中的)。
                    val preservedNoGroup = preservedCategoryIds.filterNot { it in foodGroupCatIds }
                    ingredientRepo.replaceIngredientCategories(
                        ingredientId, (categoryIds + preservedNoGroup + listOfNotNull(groupCatId)).distinct(),
                    )
                }
                // [AI生成] A1：存营养大类到 food_group(仅自定义食材;预设由 seeder 回填,不覆盖)。
                if (ingredient?.source != "preset" && foodGroup.isNotBlank()) {
                    ingredientRepo.setFoodGroup(ingredientId, foodGroup)
                }
                if (ingredient?.source != "preset") {
                    ingredientRepo.saveIngredientDetail(detail.copy(ingredientId = ingredientId))
                    ingredientRepo.replaceCareRules(
                        ingredientId,
                        careRules.map { it.copy(ingredientId = ingredientId, source = if (ingredient == null) "user" else it.source) },
                    )
                }
                // [AI生成] Item4：填了任一项(含GI/单件克重)才写(空则不动，避免清空已有)——自定义食材由此纳入营养/热量统计。
                if (nutrition != null && nutrition.hasAnyInput) {
                    nutritionRepo.upsertNutrition(nutrition.copy(ingredientId = ingredientId))
                }
                ingredientId
            }.onSuccess { ingredientId ->
                val savedIngredient = Ingredient(
                    id = ingredientId,
                    name = if (ingredient?.source == "preset") ingredient.name else trimmedName,
                    alias = alias.trim(),
                    imagePath = imagePath.trim(),
                    thumbnailPath = thumbnailPath.trim(),
                    defaultUnitId = if (ingredient?.source == "preset") ingredient.defaultUnitId else defaultUnitId,
                    source = ingredient?.source ?: "user",
                )
                _state.value = _state.value.copy(
                    creatingIngredient = false,
                    ingredients = _state.value.ingredients.map { if (it.id == ingredientId) it.copy(
                        name = savedIngredient.name,
                        alias = alias.trim(),
                        imagePath = imagePath.trim(),
                        thumbnailPath = thumbnailPath.trim(),
                        defaultUnitId = savedIngredient.defaultUnitId,
                    ) else it }, // [AI生成] 保存后立即替换当前列表对象，支撑详情弹层实时刷新。
                    // [AI修改] bug1：仅"选择食材"场景(selectionMode)新建后自动选中；食材管理页不打钩。
                    selectedIds = if (ingredient == null && _state.value.selectionMode) _state.value.selectedIds + ingredientId else _state.value.selectedIds,
                    selectedIngredients = if (ingredient == null && _state.value.selectionMode) {
                        (_state.value.selectedIngredients + savedIngredient).distinctBy { it.id }
                    } else {
                        _state.value.selectedIngredients.map { if (it.id == ingredientId) savedIngredient else it }
                    },
                    lastCreatedIngredientId = if (ingredient == null) ingredientId else _state.value.lastCreatedIngredientId,
                    lastSavedIngredientId = ingredientId,
                    // [AI修改] E2：新建食材后跳到"家庭"Tab + 所选分类(或家庭-全部)，并清空搜索，让用户看到刚建的已选中食材。
                    mainTab = if (ingredient == null) IngredientMainTab.CUSTOM else _state.value.mainTab,
                    // [AI修改] Bug 修复：跳到 CUSTOM 时同步重建左侧分类树，否则树停留在上个 Tab、看不到刚建的自定义分类节点。
                    tree = if (ingredient == null)
                        buildTreeForTab(IngredientMainTab.CUSTOM, _state.value.allCategories.filter { it.parentId == null }, _state.value.allCategories)
                    else _state.value.tree,
                    selectedCategoryId = if (ingredient == null) (categoryIds.firstOrNull() ?: -1L) else _state.value.selectedCategoryId,
                    keyword = if (ingredient == null) "" else _state.value.keyword,
                )
                reloadCurrentList()
            }.onFailure { error ->
                _state.value = _state.value.copy(
                    creatingIngredient = false,
                    createError = error.message ?: "保存食材失败",
                )
            }
        }
    }

    /**
     * 删除用户自建食材。[AI生成]
     */
    fun deleteIngredient(ingredient: Ingredient) {
        if (ingredient.source != "user") return
        viewModelScope.launch {
            runCatching {
                ingredientRepo.deleteUserIngredient(ingredient.id)
            }.onSuccess {
                _state.value = _state.value.copy(
                    selectedIds = _state.value.selectedIds - ingredient.id,
                    selectedIngredients = _state.value.selectedIngredients.filterNot { it.id == ingredient.id },
                    operationError = null,
                )
                reloadCurrentList()
            }.onFailure {
                // [AI修改] 删除失败要反馈给 UI，避免外键或事务异常时用户误以为已删除。
                _state.value = _state.value.copy(operationError = "删除失败，请稍后重试")
            }
        }
    }

    /**
     * 加载回收站（失效的自定义食材）。[AI生成]
     */
    fun loadInactiveIngredients() {
        viewModelScope.launch {
            runCatching { ingredientRepo.listInactiveUserIngredients() }
                .onSuccess { _state.value = _state.value.copy(inactiveIngredients = it) }
                .onFailure { _state.value = _state.value.copy(operationError = "加载已失效食材失败") }
        }
    }

    /**
     * 恢复失效的自定义食材。[AI生成]
     */
    fun restoreIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            runCatching { ingredientRepo.restoreUserIngredient(ingredient.id) }
                .onSuccess {
                    loadInactiveIngredients()
                    reloadCurrentList()
                }
                .onFailure { _state.value = _state.value.copy(operationError = "恢复失败，请稍后重试") }
        }
    }

    /**
     * 彻底删除失效的自定义食材。[AI生成]
     */
    fun hardDeleteIngredient(ingredient: Ingredient) {
        viewModelScope.launch {
            runCatching { ingredientRepo.hardDeleteUserIngredient(ingredient.id) }
                .onSuccess { loadInactiveIngredients() }
                .onFailure { _state.value = _state.value.copy(operationError = "彻底删除失败，请稍后重试") }
        }
    }

    /**
     * 加载在手库存食材 id 集合与剩余份数。[AI修改]
     */
    private fun loadPantryIds() {
        refreshPantryState()
    }

    /** 刷新库存 id 集合 + 剩余份数(份数-今天及过去占用)。[AI生成] */
    private fun refreshPantryState() {
        viewModelScope.launch {
            runCatching {
                val ids = pantryRepo.pantryIngredientIds()
                val servings = pantryRepo.servingCounts()
                val remaining = pantryRepo.remaining() // [AI修改] "入库日起"窗口剩余份数
                Triple(ids, remaining, servings)
            }.onSuccess { (ids, remaining, servings) ->
                _state.value = _state.value.copy(pantryIngredientIds = ids, pantryRemaining = remaining, pantryServings = servings)
            }
        }
    }

    /**
     * 加入库存（我家食材），默认加 1 份。[AI修改]
     */
    fun addToPantry(ingredient: Ingredient) = addServings(ingredient.id, 1)

    /**
     * 把某食材快速存成一道"同名单食材菜品"(即食品/直接吃的场景一步到位，便于记餐+算营养)。[AI生成]
     *
     * 已有同名菜品则不重复建。回调 already=true 表示已存在。默认用量 100g。
     */
    fun saveIngredientAsDish(ingredient: Ingredient, onResult: (already: Boolean) -> Unit) {
        viewModelScope.launch {
            val existed = dishRepo.dishIdByName(ingredient.name)
            if (existed != null) { onResult(true); return@launch }
            val gram = _state.value.availableUnits.firstOrNull { it.name == "g" || it.name == "克" }
            dishRepo.saveDish(
                id = 0L,
                name = ingredient.name,
                cookingMethodId = null,
                specialNote = "",
                description = "",
                imagePath = "",
                thumbnailPath = "",
                tagNames = emptyList(),
                ingredients = listOf(
                    com.sxdbsm.cookbook.domain.model.DishIngredient(
                        ingredient = ingredient,
                        quantity = 100.0,
                        unitId = gram?.id,
                        unitName = gram?.name ?: "g",
                        isMain = true,
                    ),
                ),
            )
            onResult(false)
        }
    }

    /**
     * 按 id 加入库存（供「新建食材入库」创建后直接入库），默认 1 份。[AI修改]
     */
    fun addToPantry(ingredientId: Long) = addServings(ingredientId, 1)

    /**
     * 加份数（续加累加，不覆盖）。[AI生成]
     */
    fun addServings(ingredientId: Long, count: Int) {
        if (count <= 0) return
        viewModelScope.launch {
            runCatching { pantryRepo.addServings(ingredientId, count) }
                .onSuccess {
                    refreshPantryState()
                    if (_state.value.mainTab == IngredientMainTab.PANTRY) reloadCurrentList()
                }
                .onFailure { _state.value = _state.value.copy(operationError = "加入库存失败，请稍后重试") }
        }
    }

    /**
     * 设置份数（用于库存 Tab 减少份数）。[AI生成]
     */
    fun setServings(ingredientId: Long, count: Int) {
        viewModelScope.launch {
            runCatching { pantryRepo.setServings(ingredientId, count) }
                .onSuccess { refreshPantryState() }
                .onFailure { _state.value = _state.value.copy(operationError = "更新份数失败，请稍后重试") }
        }
    }

    /**
     * 移出库存（软失效，保留复购历史；份数清零）。[AI修改]
     */
    fun removeFromPantry(ingredient: Ingredient) {
        viewModelScope.launch {
            runCatching { pantryRepo.removeFromPantry(ingredient.id) }
                .onSuccess {
                    refreshPantryState()
                    if (_state.value.mainTab == IngredientMainTab.PANTRY) reloadCurrentList()
                }
                .onFailure { _state.value = _state.value.copy(operationError = "移出库存失败，请稍后重试") }
        }
    }

    fun clearCreateError() {
        _state.value = _state.value.copy(createError = null)
    }

    fun clearOperationError() {
        _state.value = _state.value.copy(operationError = null)
    }

    private fun List<Ingredient>.withoutExcluded(): List<Ingredient> =
        filterNot { it.id in _state.value.excludeIngredientIds }

    private suspend fun reloadCurrentList() {
        val current = _state.value
        val ingredients = when (val categoryId = current.selectedCategoryId) {
            null, ALL_CATEGORY_ID -> loadAllForTab(current.mainTab, "")
            else -> {
                // [AI修改] Bug 修复：原按陈旧的左侧展开树 tree 找节点，新建分类还没进树时找不到→返回空，
                // 导致"挂到自建分类的食材保存后该分类下为空"。改为从 allCategories 找分类(不依赖树是否已含该节点)。
                val category = current.allCategories.firstOrNull { it.id == categoryId }
                if (category == null) emptyList() else loadByCategoryWithChildren(category)
            }
        }
        // [AI修改] 搜索已解耦为全局下拉，网格不再按 keyword 过滤，只按 Tab 来源过滤。
        val sourceFiltered = ingredients.filterForTabSource(current.mainTab)
        applyIngredientResult(sourceFiltered.withoutExcluded(), resetPage = true)
    }

    /**
     * 按 Tab 读取“全部”食材。[AI生成]
     */
    private suspend fun loadAllForTab(tab: IngredientMainTab, keyword: String): List<Ingredient> =
        when (tab) {
            IngredientMainTab.RECENT -> ingredientRepo.listRecentlyUsed()
            IngredientMainTab.PANTRY -> pantryRepo.listPantryIngredients() // [AI生成] 库存 Tab：在手食材，平铺展示。
            else -> ingredientRepo.search(keyword).filterForTabSource(tab)
        }

    /**
     * 当前分类筛选包含该分类和所有后代分类。[AI修改]
     *
     * 调养 tab 的分类数据存在 ingredient_care_rule 表而非分类关联表，需要单独走调养规则聚合。
     */
    private suspend fun loadByCategoryWithChildren(category: FoodCategory): List<Ingredient> {
        val ids = expandCategoryIds(listOf(category.id))
        val tab = _state.value.mainTab
        val ingredients = if (tab == IngredientMainTab.CARE) {
            ingredientRepo.listByCareCategories(ids).dedupeByMostSevereAdvice()
        } else {
            ingredientRepo.listByCategories(ids)
        }
        return ingredients.filterForTabSource(tab)
    }

    /**
     * 同一食材命中多个调养分类时按最严等级去重（避免>限量>推荐），并按 绿灯→黄灯→红灯 排序。[AI生成]
     */
    private fun List<Ingredient>.dedupeByMostSevereAdvice(): List<Ingredient> {
        fun severity(level: AdviceLevel?): Int = when (level) {
            AdviceLevel.AVOID -> 2
            AdviceLevel.LIMIT -> 1
            AdviceLevel.RECOMMEND -> 0
            null -> -1
        }
        return groupBy { it.id }
            .values
            .map { rows -> rows.maxBy { severity(it.adviceLevel) } }
            .sortedWith(compareBy({ severity(it.adviceLevel) }, { it.pinyin }, { it.name }))
    }

    /**
     * 展开分类 id 为自身 + 所有后代。[AI生成]
     */
    private fun expandCategoryIds(categoryIds: List<Long>): List<Long> {
        val all = _state.value.allCategories
        val result = linkedSetOf<Long>()
        fun collect(id: Long) {
            if (!result.add(id)) return
            all.filter { it.parentId == id }.forEach { collect(it.id) }
        }
        categoryIds.forEach(::collect)
        return result.toList()
    }

    /**
     * 系统 Tab 只展示预设食材，自定义 Tab 只展示用户食材，最近保留全部来源。[AI生成]
     */
    private fun List<Ingredient>.filterForTabSource(tab: IngredientMainTab): List<Ingredient> =
        when (tab) {
            IngredientMainTab.RECENT, IngredientMainTab.PANTRY -> this // [AI修改] 最近/库存保留全部来源。
            IngredientMainTab.CUSTOM -> filter { it.source == "user" }
            IngredientMainTab.GENERAL, IngredientMainTab.NUTRITION, IngredientMainTab.CARE -> filter { it.source != "user" }
        }

    /**
     * 应用分页结果。[AI生成]
     */
    private fun applyIngredientResult(result: List<Ingredient>, resetPage: Boolean) {
        currentIngredientResult = result
        if (resetPage) currentIngredientPage = 1
        val visibleCount = currentIngredientPage * PAGE_SIZE
        _state.value = _state.value.copy(
            ingredients = result.take(visibleCount),
            canLoadMoreIngredients = result.size > visibleCount,
        )
    }

    /**
     * 刷新分类树和完整分类列表。[AI生成]
     */
    private suspend fun refreshCategories() {
        val tops = categoryRepo.listTopLevel()
        val allCategories = categoryRepo.listAll()
        _state.value = _state.value.copy(
            tree = buildTreeForTab(_state.value.mainTab, tops, allCategories),
            allCategories = allCategories,
        )
    }

    /**
     * 按顶部主维度构建左侧一级树。[AI生成]
     */
    private fun buildTreeForTab(
        tab: IngredientMainTab,
        tops: List<FoodCategory>,
        allCategories: List<FoodCategory>,
    ): List<CategoryNode> =
        when (tab) {
            IngredientMainTab.RECENT, IngredientMainTab.PANTRY -> emptyList() // [AI修改] 最近/库存无左侧分类树。
            IngredientMainTab.GENERAL -> tops
                .filter { it.source != "user" && it.dimension == "general" && it.crowdTypeId == null }
                .map { category ->
                    CategoryNode(category.copy(hasChildren = allCategories.any { it.parentId == category.id && it.matchesTab(tab) }), level = 1)
                }
            IngredientMainTab.NUTRITION -> {
                val nutritionRootIds = tops.filter { it.isNutritionGroupRoot() }.map { it.id }.toSet()
                val promotedChildren = allCategories
                    .filter { it.parentId in nutritionRootIds && it.matchesTab(tab) }
                val childRoots = allCategories
                    .filter { it.parentId == null && it.dimension in nutritionDimensions && !it.isNutritionGroupRoot() }
                (promotedChildren + childRoots)
                    .distinctBy { it.id }
                    .map { category ->
                        CategoryNode(category.copy(hasChildren = allCategories.any { it.parentId == category.id && it.matchesTab(tab) }), level = 1)
                    }
            }
            IngredientMainTab.CARE -> {
                val careRootIds = tops.filter { it.isCareGroupRoot() }.map { it.id }.toSet()
                val promotedChildren = allCategories
                    .filter { it.parentId in careRootIds && it.matchesTab(tab) }
                val childRoots = tops
                    .filter { (it.dimension == "crowd" || it.crowdTypeId != null) && !it.isCareGroupRoot() }
                (promotedChildren + childRoots)
                    .distinctBy { it.id }
                    .map { category ->
                        CategoryNode(category.copy(hasChildren = allCategories.any { it.parentId == category.id && it.matchesTab(tab) }), level = 1)
                    }
            }
            // [AI修改] 拼接需先加括号再 distinct/map，否则链式调用只作用于第二个列表，产生 List<Any> 编译错误（遗留问题修复）。
            IngredientMainTab.CUSTOM -> (
                tops.filter { it.source == "user" } + allCategories.filter { category ->
                    category.source == "user" && category.parentId != null && allCategories.firstOrNull { it.id == category.parentId }?.source != "user"
                } // [AI生成] 兼容旧数据：历史上挂到系统分类下的用户分类，在新规则下提升为自定义根节点。
                )
                .distinctBy { it.id }
                .map { category ->
                    CategoryNode(category.copy(hasChildren = allCategories.any { it.parentId == category.id && it.source == "user" }), level = 1)
                }
        }

    private fun List<FoodCategory>.filterForTab(tab: IngredientMainTab): List<FoodCategory> =
        when (tab) {
            IngredientMainTab.RECENT, IngredientMainTab.PANTRY -> emptyList()
            IngredientMainTab.GENERAL -> filter { it.matchesTab(tab) }
            IngredientMainTab.NUTRITION -> filter { it.matchesTab(tab) }
            IngredientMainTab.CARE -> filter { it.matchesTab(tab) }
            IngredientMainTab.CUSTOM -> filter { it.matchesTab(tab) }
        }

    private fun FoodCategory.matchesTab(tab: IngredientMainTab): Boolean =
        when (tab) {
            IngredientMainTab.RECENT, IngredientMainTab.PANTRY -> false
            IngredientMainTab.GENERAL -> dimension == "general" && crowdTypeId == null
            IngredientMainTab.NUTRITION -> dimension in nutritionDimensions
            IngredientMainTab.CARE -> dimension == "crowd" || crowdTypeId != null
            IngredientMainTab.CUSTOM -> source == "user"
        }

    // [AI生成] 营养/调养顶部只显示真实标签节点，隐藏“健康饮食/人群分类”这类聚合根。
    private fun FoodCategory.isNutritionGroupRoot(): Boolean =
        parentId == null && dimension == "nutrition" && name == "健康饮食"

    // [AI生成] 调养顶部只显示具体人群/病种节点，隐藏“人群分类”聚合根。
    private fun FoodCategory.isCareGroupRoot(): Boolean =
        parentId == null && dimension == "crowd" && name == "人群分类"

    // [AI生成] 与编辑器 UI 的可维护分类判定保持一致：仅用户自建的普通分类可在编辑器中增删。
    private fun FoodCategory.isEditableUserGeneralCategory(): Boolean =
        source == "user" && dimension == "general" && crowdTypeId == null

    private companion object {
        val nutritionDimensions = NUTRITION_TAB_DIMENSIONS // [AI修改] 统一用共享维度集(营养族+应季)，见 IngredientPickerCommon。
        const val PAGE_SIZE = 30 // [AI生成] 食材分类列表默认每页 30 个。
    }
}

private data class IngredientDetailPayload(
    val categories: List<FoodCategory>,
    val detail: IngredientDetail?,
    val careRules: List<IngredientCareRule>,
    val matches: List<DishIngredientMatch>,
)

// [AI修改] 扩展函数：在指定位置插入多个元素，等价于给 MutableList 增加一个小工具方法。
private fun <T> MutableList<T>.add(index: Int, vararg items: T) {
    items.forEachIndexed { i, item -> add(index + i, item) }
}
