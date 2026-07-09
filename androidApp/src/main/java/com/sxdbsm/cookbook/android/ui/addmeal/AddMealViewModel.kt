package com.sxdbsm.cookbook.android.ui.addmeal

import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.FavoriteComboRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.FavoriteCombo
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

/**
 * 单个餐食模块 UI 状态。[AI生成]
 *
 * 类似 Java 里的一个表单 Bean：保存某一餐的餐次、时间、已选菜品和备注。
 */
data class MealBlockUiState(
    val id: Long,
    val mealTypeId: Long? = null,
    val mealTime: LocalTime? = null,
    val dishes: List<DishMini> = emptyList(),
    val note: String = "",
) {
    /**
     * 当前餐食模块是否具备保存条件。[AI生成]
     */
    val canSave: Boolean
        get() = mealTypeId != null && mealTime != null && dishes.isNotEmpty()
}

/**
 * 添加餐食页 UI 状态。[AI修改]
 *
 * 这是一个不可变快照；现在支持一天内多个餐食模块，每个模块保存为一条 meal_record。
 */
data class AddMealUiState(
    val date: LocalDate = DateTime.today(),
    val mealTypes: List<MealType> = emptyList(),
    val mealBlocks: List<MealBlockUiState> = emptyList(),
    val favoriteCombos: List<FavoriteCombo> = emptyList(),
    val activeBlockId: Long? = null,
    val isPlan: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
    val errorMessage: String? = null,
) {
    /**
     * 页面是否允许保存。[AI生成]
     */
    val canSave: Boolean
        get() = mealBlocks.any { it.canSave } && !saving
}

/**
 * 添加餐食 ViewModel。[AI修改]
 *
 * 管理“某一天的多餐食模块”表单，并在保存时调用 shared 层 Repository 批量写入数据库。
 */
class AddMealViewModel(
    private val mealRepo: MealRecordRepository,
    private val dishRepo: DishRepository,
    private val comboRepo: FavoriteComboRepository,
) : ViewModel() {
    companion object {
        private const val TAG = "MealFlow" // [AI生成] 添加/编辑餐食链路统一日志 Tag。
    }

    private val _state = MutableStateFlow(AddMealUiState()) // [AI修改] 内部可变状态，只允许 ViewModel 修改。
    val state: StateFlow<AddMealUiState> = _state.asStateFlow() // [AI修改] 对 UI 暴露只读 StateFlow。

    private var nextBlockId = 1L
    private var configured = false // [AI生成] 标记外部入口是否已指定，避免 init 默认日期覆盖编辑日期。
    private var pendingEditDate: LocalDate? = null
    private var pendingPresetDishIds: List<Long> = emptyList() // [AI生成] AI 推荐"选它"带入的菜品，加载完块后并入第一块。
    private var presetApplied = false // [AI生成] 预填只应用一次，避免返回时路由参数不变导致重复重载。
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            val types = mealRepo.listMealTypes()
            _state.value = _state.value.copy(mealTypes = types, favoriteCombos = comboRepo.listCombos())
            if (!configured) {
                configure(editDate = null)
            } else {
                loadConfiguredDate()
            }
        }
    }

    /**
     * 配置添加/编辑入口日期。[AI生成]
     *
     * editDate 优先；新建入口会取“今天”或最后计划日期的后一天，避免 init 默认加载和编辑入口 setDate 竞态。
     */
    fun configure(editDate: LocalDate? = null, presetDishIds: List<Long> = emptyList()) {
        // [AI修改] 预填只应用一次：路由里的 presetDishIds 在返回时不变，若不加 presetApplied 守卫，
        // 从餐次进 AI 推荐再返回会重复触发重载、清掉用户新加的餐次块。
        if (presetDishIds.isNotEmpty() && !presetApplied) {
            presetApplied = true
            pendingPresetDishIds = presetDishIds
            configured = true
            pendingEditDate = editDate
            if (_state.value.mealTypes.isNotEmpty()) loadConfiguredDate()
            return
        }
        if (configured && pendingEditDate == editDate && _state.value.mealBlocks.isNotEmpty()) {
            AppLogger.d(TAG, "configure skip reload: editDate=$editDate currentDate=${_state.value.date} blocks=${_state.value.mealBlocks.size}") // [AI生成] 排查返回新建菜品后是否误触发重载。
            return // [AI修改] 页面从新建菜品返回时可能重新组合，入口日期相同则保留当前未保存表单，避免旧餐食覆盖用户编辑。
        }
        AppLogger.d(TAG, "configure load: editDate=$editDate mealTypes=${_state.value.mealTypes.size} configured=$configured") // [AI生成] 记录入口配置触发加载的参数。
        configured = true
        pendingEditDate = editDate
        if (_state.value.mealTypes.isNotEmpty()) {
            loadConfiguredDate()
        }
    }

    /**
     * 修改整天餐食的日期。[AI修改]
     */
    fun setDate(date: LocalDate) {
        AppLogger.d(TAG, "setDate manual: date=$date previous=${_state.value.date}") // [AI生成] 用户主动切换日期时记录前后日期。
        configured = true
        pendingEditDate = date
        if (_state.value.mealTypes.isNotEmpty()) {
            loadConfiguredDate()
        } // [AI修改] 用户主动选择日期时仍要重新加载目标日期餐食，不受入口防重载保护。
    }

    /**
     * 新增一个餐食模块。[AI生成]
     */
    fun addMealBlock() {
        val defaultType = _state.value.mealTypes.firstOrNull { it.code == "BREAKFAST" }
            ?: _state.value.mealTypes.firstOrNull()
        val block = newBlock(defaultType)
        _state.value = _state.value.copy(
            mealBlocks = _state.value.mealBlocks + block,
            activeBlockId = block.id,
        )
        AppLogger.d(TAG, "add meal block: blockId=${block.id} mealTypeId=${block.mealTypeId} time=${block.mealTime}") // [AI生成] 记录新增餐食模块默认餐次和时间。
    }

    /**
     * 删除一个餐食模块。[AI生成]
     */
    fun removeMealBlock(blockId: Long) {
        val current = _state.value.mealBlocks
        if (current.size <= 1) return
        val blocks = current.filterNot { it.id == blockId }
        _state.value = _state.value.copy(
            mealBlocks = blocks,
            activeBlockId = blocks.firstOrNull()?.id,
        )
        AppLogger.d(TAG, "remove meal block: blockId=$blockId remaining=${blocks.map { it.id }}") // [AI生成] 记录删除餐食模块后的剩余模块。
    }

    /**
     * 修改指定模块的餐次。[AI修改]
     *
     * 固定餐次会自动带出默认时间；“加餐”等非固定餐次要求用户手动选择时间。
     */
    fun setMealType(blockId: Long, mealTypeId: Long) {
        val type = _state.value.mealTypes.firstOrNull { it.id == mealTypeId }
        updateBlock(blockId) { block ->
            block.copy(
                mealTypeId = mealTypeId,
                mealTime = if (type?.isFixed == true) type.defaultTime else null,
            )
        }
    }

    /**
     * 修改指定模块的用餐时间。[AI修改]
     */
    fun setMealTime(blockId: Long, time: LocalTime) {
        updateBlock(blockId) { it.copy(mealTime = time) }
    }

    /**
     * 修改指定模块备注。[AI生成]
     */
    fun setNote(blockId: Long, note: String) {
        updateBlock(blockId) { it.copy(note = note) }
    }

    /**
     * 打开菜品选择器前记录当前操作的餐食模块。[AI生成]
     */
    fun setActiveBlock(blockId: Long) {
        _state.value = _state.value.copy(activeBlockId = blockId)
        AppLogger.d(TAG, "set active block: blockId=$blockId") // [AI生成] 记录当前菜品选择目标餐食模块。
    }

    /**
     * 向指定餐食模块添加菜品。[AI修改]
     */
    fun addDishes(blockId: Long, dishes: List<DishMini>) {
        AppLogger.d(TAG, "add dishes request: blockId=$blockId dishIds=${dishes.map { it.id }} current=${_state.value.mealBlocks.firstOrNull { it.id == blockId }?.dishes?.map { it.id }}") // [AI生成] 记录加入菜品前后的关键输入。
        updateBlock(blockId) { block ->
            block.copy(dishes = (block.dishes + dishes).distinctBy { it.id })
        }
        AppLogger.d(TAG, "add dishes result: blockId=$blockId result=${_state.value.mealBlocks.firstOrNull { it.id == blockId }?.dishes?.map { it.id }}") // [AI生成] 记录去重合并后的菜品列表。
    }

    /**
     * 新建菜品返回后，按 id 拉取轻量菜品并加入当前餐食模块。[AI生成]
     */
    fun addCreatedDish(dishId: Long, blockId: Long? = _state.value.activeBlockId) {
        if (dishId <= 0 || blockId == null) return
        AppLogger.d(TAG, "add created dish begin: dishId=$dishId blockId=$blockId") // [AI生成] 记录新建菜品回填开始。
        viewModelScope.launch {
            dishRepo.getDishMiniById(dishId)?.let { dish ->
                AppLogger.d(TAG, "add created dish loaded: dishId=$dishId name=${dish.name} blockId=$blockId") // [AI生成] 记录新建菜品轻量信息读取成功。
                addDishes(blockId, listOf(dish))
            }
        }
    }

    fun addComboDishes(blockId: Long, combo: FavoriteCombo) {
        addDishes(blockId, combo.dishes) // [AI生成] 组合复用只把组合内菜品加入当前餐食模块，不改变组合本身。
    }

    /** [AI生成] AI 推荐"选它"从餐次进入时，把菜品直接加入该餐次块。 */
    fun addDishesByIds(blockId: Long, ids: List<Long>) {
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val dishes = ids.mapNotNull { dishRepo.getDishMiniById(it) }
            if (dishes.isNotEmpty()) addDishes(blockId, dishes)
        }
    }

    fun saveCurrentBlockAsCombo(blockId: Long, name: String) {
        val block = _state.value.mealBlocks.firstOrNull { it.id == blockId } ?: return
        if (block.dishes.isEmpty()) return
        viewModelScope.launch {
            comboRepo.createCombo(name, block.dishes.map { it.id })
            _state.value = _state.value.copy(favoriteCombos = comboRepo.listCombos())
        }
    }

    /**
     * 从指定餐食模块移除菜品。[AI修改]
     */
    fun removeDish(blockId: Long, dishId: Long) {
        AppLogger.d(TAG, "remove dish request: blockId=$blockId dishId=$dishId before=${_state.value.mealBlocks.firstOrNull { it.id == blockId }?.dishes?.map { it.id }}") // [AI生成] 记录删除菜品前列表，排查编辑回退。
        updateBlock(blockId) { block ->
            block.copy(dishes = block.dishes.filterNot { it.id == dishId })
        }
        AppLogger.d(TAG, "remove dish result: blockId=$blockId after=${_state.value.mealBlocks.firstOrNull { it.id == blockId }?.dishes?.map { it.id }}") // [AI生成] 记录删除后列表。
    }

    /**
     * 批量保存当天所有有效餐食模块。[AI修改]
     */
    fun save() {
        val s = _state.value
        val drafts = s.mealBlocks
            .filter { it.canSave }
            .map { block ->
                DayMealDraft(
                    mealTypeId = block.mealTypeId ?: return,
                    mealTime = block.mealTime ?: return,
                    note = block.note,
                    dishIds = block.dishes.map { it.id },
                )
            }
        if (drafts.isEmpty()) return
        AppLogger.d(TAG, "save meals begin: date=${s.date} drafts=${drafts.map { it.mealTypeId to it.dishIds }}") // [AI生成] 记录保存前的餐食草稿摘要。
        viewModelScope.launch {
            // [AI修改] viewModelScope 会随 ViewModel 销毁自动取消，避免页面关闭后继续持有 UI。
            _state.value = s.copy(saving = true)
            runCatching {
                mealRepo.saveDayMeals(date = s.date, meals = drafts)
            }.onSuccess {
                AppLogger.d(TAG, "save meals success: date=${s.date} drafts=${drafts.size}") // [AI生成] 记录保存成功。
                AppLogger.event(
                    "meal_save",
                    mapOf(
                        "date" to s.date,
                        "blockCount" to drafts.size,
                        "dishCount" to drafts.sumOf { it.dishIds.size },
                        "success" to true,
                    ),
                ) // [AI生成] 内测埋点：记录餐食保存成功摘要。
                _state.value = _state.value.copy(saving = false, done = true, errorMessage = null)
            }.onFailure {
                AppLogger.e(TAG, "save meals failed: date=${s.date}", it) // [AI生成] 记录保存失败异常。
                AppLogger.event(
                    "meal_save",
                    mapOf(
                        "date" to s.date,
                        "blockCount" to drafts.size,
                        "dishCount" to drafts.sumOf { draft -> draft.dishIds.size },
                        "success" to false,
                        "errorType" to it.javaClass.simpleName,
                    ),
                ) // [AI生成] 内测埋点：记录餐食保存失败摘要。
                _state.value = _state.value.copy(
                    saving = false,
                    errorMessage = "保存餐食失败，请检查数据后重试",
                )
            } // [AI生成] 保存失败时保持页面可操作并给出错误提示，避免崩溃或卡在保存中。
        }
    }

    private fun newBlock(defaultType: MealType?): MealBlockUiState {
        val id = nextBlockId++
        return MealBlockUiState(
            id = id,
            mealTypeId = defaultType?.id,
            mealTime = defaultType?.takeIf { it.isFixed }?.defaultTime,
        )
    }

    /**
     * 按入口类型解析目标日期并触发加载。[AI生成]
     */
    private fun loadConfiguredDate() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val targetDate = pendingEditDate ?: resolveNewMealDate()
            loadMealsForDateInternal(targetDate)
        }
    }

    /**
     * 新建餐食默认日期：没有记录或最后记录早于今天则用今天，否则顺延到最后记录的后一天。[AI生成]
     */
    private suspend fun resolveNewMealDate(): LocalDate {
        val today = DateTime.today()
        val maxDate = mealRepo.dateRange().second
        return if (maxDate == null || maxDate < today) {
            today
        } else {
            DateTime.plusDays(maxDate, 1)
        }
    }

    /**
     * 实际执行日期回填；调用方负责管理 loadJob，避免配置任务被自己取消。[AI生成]
     */
    private suspend fun loadMealsForDateInternal(date: LocalDate) {
        AppLogger.d(TAG, "load meals begin: date=$date") // [AI生成] 记录数据库加载日期，排查未保存编辑被覆盖。
        val existingMeals = mealRepo.loadDayMealsForEdit(date)
        AppLogger.d(TAG, "load meals db result: date=$date existing=${existingMeals.map { it.mealTypeId to it.dishes.map { dish -> dish.id } }}") // [AI生成] 记录数据库返回的餐食摘要。
        val blocks = if (existingMeals.isEmpty()) {
            val defaultType = _state.value.mealTypes.firstOrNull { it.code == "BREAKFAST" }
                ?: _state.value.mealTypes.firstOrNull()
            listOf(newBlock(defaultType))
        } else {
            existingMeals.map { meal ->
                MealBlockUiState(
                    id = nextBlockId++,
                    mealTypeId = meal.mealTypeId,
                    mealTime = meal.mealTime,
                    dishes = meal.dishes,
                    note = meal.note,
                )
            }
        }
        // [AI生成] 若有 AI 推荐预填菜品，解析后并入第一块（去重），随后清空。
        val finalBlocks = if (pendingPresetDishIds.isEmpty()) {
            blocks
        } else {
            val presetDishes = pendingPresetDishIds.mapNotNull { dishRepo.getDishMiniById(it) }
            pendingPresetDishIds = emptyList()
            if (presetDishes.isEmpty() || blocks.isEmpty()) {
                blocks
            } else {
                blocks.mapIndexed { i, b ->
                    if (i == 0) b.copy(dishes = (b.dishes + presetDishes).distinctBy { it.id }) else b
                }
            }
        }
        _state.value = _state.value.copy(
            date = date,
            isPlan = date > DateTime.today(),
            mealBlocks = finalBlocks,
            activeBlockId = finalBlocks.firstOrNull()?.id,
        )
        AppLogger.d(TAG, "load meals applied: date=$date blocks=${finalBlocks.map { it.id to it.dishes.map { dish -> dish.id } }}") // [AI生成] 记录加载应用到 UI 状态后的摘要。
    }

    private fun updateBlock(blockId: Long, transform: (MealBlockUiState) -> MealBlockUiState) {
        _state.value = _state.value.copy(
            mealBlocks = _state.value.mealBlocks.map { block ->
                if (block.id == blockId) transform(block) else block
            },
        )
    }
}
