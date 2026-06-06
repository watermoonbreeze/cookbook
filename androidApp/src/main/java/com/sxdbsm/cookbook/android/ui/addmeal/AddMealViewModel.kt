package com.sxdbsm.cookbook.android.ui.addmeal

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

    private val _state = MutableStateFlow(AddMealUiState()) // [AI修改] 内部可变状态，只允许 ViewModel 修改。
    val state: StateFlow<AddMealUiState> = _state.asStateFlow() // [AI修改] 对 UI 暴露只读 StateFlow。

    private var nextBlockId = 1L
    private var configured = false // [AI生成] 标记外部入口是否已指定，避免 init 默认日期覆盖编辑日期。
    private var pendingEditDate: LocalDate? = null
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
    fun configure(editDate: LocalDate? = null) {
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
        configure(editDate = date)
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
    }

    /**
     * 向指定餐食模块添加菜品。[AI修改]
     */
    fun addDishes(blockId: Long, dishes: List<DishMini>) {
        updateBlock(blockId) { block ->
            block.copy(dishes = (block.dishes + dishes).distinctBy { it.id })
        }
    }

    /**
     * 新建菜品返回后，按 id 拉取轻量菜品并加入当前餐食模块。[AI生成]
     */
    fun addCreatedDish(dishId: Long, blockId: Long? = _state.value.activeBlockId) {
        if (dishId <= 0 || blockId == null) return
        viewModelScope.launch {
            dishRepo.getDishMiniById(dishId)?.let { dish ->
                addDishes(blockId, listOf(dish))
            }
        }
    }

    fun addComboDishes(blockId: Long, combo: FavoriteCombo) {
        addDishes(blockId, combo.dishes) // [AI生成] 组合复用只把组合内菜品加入当前餐食模块，不改变组合本身。
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
        updateBlock(blockId) { block ->
            block.copy(dishes = block.dishes.filterNot { it.id == dishId })
        }
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
        viewModelScope.launch {
            // [AI修改] viewModelScope 会随 ViewModel 销毁自动取消，避免页面关闭后继续持有 UI。
            _state.value = s.copy(saving = true)
            runCatching {
                mealRepo.saveDayMeals(date = s.date, meals = drafts)
            }.onSuccess {
                _state.value = _state.value.copy(saving = false, done = true, errorMessage = null)
            }.onFailure {
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
        val existingMeals = mealRepo.loadDayMealsForEdit(date)
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
        _state.value = _state.value.copy(
            date = date,
            isPlan = date > DateTime.today(),
            mealBlocks = blocks,
            activeBlockId = blocks.firstOrNull()?.id,
        )
    }

    private fun updateBlock(blockId: Long, transform: (MealBlockUiState) -> MealBlockUiState) {
        _state.value = _state.value.copy(
            mealBlocks = _state.value.mealBlocks.map { block ->
                if (block.id == blockId) transform(block) else block
            },
        )
    }
}
