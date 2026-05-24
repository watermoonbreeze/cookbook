package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DayMealDraft
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.util.DateTime
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
    val activeBlockId: Long? = null,
    val isPlan: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
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
) : ViewModel() {

    private val _state = MutableStateFlow(AddMealUiState()) // [AI修改] 内部可变状态，只允许 ViewModel 修改。
    val state: StateFlow<AddMealUiState> = _state.asStateFlow() // [AI修改] 对 UI 暴露只读 StateFlow。

    private var nextBlockId = 1L

    init {
        viewModelScope.launch {
            val types = mealRepo.listMealTypes()
            _state.value = _state.value.copy(mealTypes = types)
            loadMealsForDate(_state.value.date)
        }
    }

    /**
     * 修改整天餐食的日期。[AI修改]
     */
    fun setDate(date: LocalDate) {
        _state.value = _state.value.copy(date = date, isPlan = date > DateTime.today())
        loadMealsForDate(date)
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
            mealRepo.saveDayMeals(date = s.date, meals = drafts)
            _state.value = _state.value.copy(saving = false, done = true)
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
     * 根据日期加载已有餐食；没有记录时创建默认早餐模块。[AI修改]
     */
    private fun loadMealsForDate(date: LocalDate) {
        viewModelScope.launch {
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
    }

    private fun updateBlock(blockId: Long, transform: (MealBlockUiState) -> MealBlockUiState) {
        _state.value = _state.value.copy(
            mealBlocks = _state.value.mealBlocks.map { block ->
                if (block.id == blockId) transform(block) else block
            },
        )
    }
}
