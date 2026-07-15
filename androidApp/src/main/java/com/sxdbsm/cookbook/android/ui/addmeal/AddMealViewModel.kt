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
    val dateWarning: String? = null, // [AI生成] F7：选到已有餐食的日期时的一次性提示(不切换过去)
    val isEditingExisting: Boolean = false, // [AI生成] N3：编辑既有某天餐食时日期锁定不可改(防改日期导致数据错乱)；仅新增可改
    val minSelectableDate: LocalDate? = null, // [AI生成] 复制场景可选日期下限(=最新餐食日期+1)
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
        private const val AFTERNOON_START_HOUR = 12 // [AI生成] ≥12点视为下午/晚上：无未来餐食时新建默认明天；上午(<12)默认今天。

        /**
         * 新建空餐次块的默认餐次：按当前钟点选"默认时间最接近现在"的固定餐次。[AI生成]
         *
         * 修"晚上记晚饭进来却是早餐/07:30"——原来恒取 BREAKFAST。未来计划日从早餐排起更自然。
         * 纯函数(nowHour 提参)便于单测，遵守"today/now 提参"红线。
         */
        internal fun pickDefaultMealType(types: List<MealType>, isFuture: Boolean, nowHour: Int): MealType? {
            if (types.isEmpty()) return null
            if (isFuture) return types.firstOrNull { it.code == "BREAKFAST" } ?: types.first()
            return types.filter { it.isFixed }
                .minByOrNull { kotlin.math.abs(it.defaultTime.hour - nowHour) }
                ?: types.firstOrNull { it.code == "BREAKFAST" } ?: types.first()
        }
    }

    private val _state = MutableStateFlow(AddMealUiState()) // [AI修改] 内部可变状态，只允许 ViewModel 修改。
    val state: StateFlow<AddMealUiState> = _state.asStateFlow() // [AI修改] 对 UI 暴露只读 StateFlow。

    private var nextBlockId = 1L
    private var configured = false // [AI生成] 标记外部入口是否已指定，避免 init 默认日期覆盖编辑日期。
    private var pendingEditDate: LocalDate? = null
    private var pendingPresetDishIds: List<Long> = emptyList() // [AI生成] AI 推荐"选它"带入的菜品，加载完块后并入第一块。
    private var presetApplied = false // [AI生成] 预填只应用一次，避免返回时路由参数不变导致重复重载。
    private var loadJob: Job? = null
    private var userPickedDate = false // [AI生成] F7：用户手动改过日期后，configure 不再重载(保留当前编辑)。
    private var loadedFromDate: LocalDate? = null // [AI生成] F7：本次编辑加载自哪个已有日期(用于"改日期=移动"时删旧)。
    private var copyFromDate: LocalDate? = null // [AI生成] F8：食历"复制"来源日期→按其餐次预填成新建草稿(日期=源+1，可改)。
    private var copyConfigured = false // [AI生成] F8：复制入口一次性守卫(独立于 configured，避免被 init 默认 configure 抢跑吞掉)。

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
        if (userPickedDate && _state.value.mealBlocks.isNotEmpty()) {
            AppLogger.d(TAG, "configure skip reload: user picked date manually, keep current edit")
            return // [AI修改] F7：用户手动改过日期后，返回重组不再重载，避免丢失"改到新日期+当前内容"。
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
     * 修改当前餐食的日期。[AI修改]
     *
     * F7：只把当前正在编辑的餐次/菜品「改到」这个日期，**不带出该日期已有的餐次**。
     * 若目标日期已有餐食 → 提示且不切换(去食历编辑那天或另选空日期)；空日期则直接切换、保留当前内容。
     */
    fun setDate(date: LocalDate) {
        if (date == _state.value.date) return
        configured = true
        // [AI修改] #2：复制场景有下限(最新餐食+1)，早于下限直接提示不切换。
        val minDate = _state.value.minSelectableDate
        if (minDate != null && date < minDate) {
            _state.value = _state.value.copy(dateWarning = "只能选择 $minDate 及之后的日期(接在现有餐食之后)")
            return
        }
        viewModelScope.launch {
            val occupied = mealRepo.loadDayMealsForEdit(date).isNotEmpty()
            if (occupied) {
                // [AI修改] 冲突时日期不变，也不置 userPickedDate(避免污染后续 configure 重载判断)。
                AppLogger.d(TAG, "setDate conflict: date=$date already has meals")
                _state.value = _state.value.copy(dateWarning = "$date 已经有餐食了，请到食历里编辑那天，或选一个空日期")
            } else {
                AppLogger.d(TAG, "setDate move: date=$date previous=${_state.value.date} (保留当前餐次)")
                userPickedDate = true // 仅成功改到空日期时才标记"用户改过日期"
                pendingEditDate = date
                _state.value = _state.value.copy(date = date, isPlan = date > DateTime.today(), dateWarning = null)
            }
        }
    }

    /** 消费一次性日期冲突提示。[AI生成] */
    fun consumeDateWarning() {
        if (_state.value.dateWarning != null) _state.value = _state.value.copy(dateWarning = null)
    }

    /**
     * 新增一个餐食模块。[AI生成]
     */
    fun addMealBlock() {
        // [AI修改] F6：默认取"下一个尚未添加"的餐次(按 mealTypes 顺序:早餐→上午餐→中餐→下午餐→晚餐→夜宵)，
        // 而非每次都早餐。已有早餐→默认上午餐/中餐…全用完则回退第一个。
        val usedTypeIds = _state.value.mealBlocks.map { it.mealTypeId }.toSet()
        val defaultType = _state.value.mealTypes.firstOrNull { it.id !in usedTypeIds }
            ?: _state.value.mealTypes.firstOrNull { it.code == "BREAKFAST" }
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
     * 固定餐次带出其默认时间；非固定餐次(如"加餐")默认取**当前时间**，用户想改再改——
     * [AI修改] A2：不再强制手动选时间才能保存(家庭多为"刚吃过/正在吃"，默认当前更顺手)。
     */
    fun setMealType(blockId: Long, mealTypeId: Long) {
        val type = _state.value.mealTypes.firstOrNull { it.id == mealTypeId }
        updateBlock(blockId) { block ->
            block.copy(
                mealTypeId = mealTypeId,
                mealTime = if (type?.isFixed == true) type.defaultTime else DateTime.nowTime(),
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
                // canSave 已保证 mealTypeId/mealTime 非空，用 !! 明确不变量(原 `?: return` 在 filter 后不可达且误导)。
                DayMealDraft(
                    mealTypeId = block.mealTypeId!!,
                    mealTime = block.mealTime!!,
                    note = block.note,
                    dishIds = block.dishes.map { it.id },
                )
            }
        if (drafts.isEmpty()) return
        AppLogger.d(TAG, "save meals begin: date=${s.date} drafts=${drafts.map { it.mealTypeId to it.dishIds }}") // [AI生成] 记录保存前的餐食草稿摘要。
        viewModelScope.launch {
            // [AI修改] viewModelScope 会随 ViewModel 销毁自动取消，避免页面关闭后继续持有 UI。
            // [AI修改] D10：saving 标志用最新 _state.value 写回(不用启动前捕获的 s 快照)，避免理论上冲掉并发字段。
            _state.value = _state.value.copy(saving = true)
            runCatching {
                // [AI修改] 抬喜爱度基线=loadedFromDate：编辑同日=本日、移动=来源日、新增=null(视目标日)。
                // 修"移动到空日期时把来源日已计过的菜再+1"的 bug。
                mealRepo.saveDayMeals(date = s.date, meals = drafts, incrementBaselineDate = loadedFromDate)
                // [AI生成] F7：若本次是编辑已有某天并把日期改到了新日期(移动)，保存到新日期后删除旧日期，避免重复。
                val from = loadedFromDate
                if (from != null && from != s.date) {
                    AppLogger.d(TAG, "move meals: delete old date=$from after saving to ${s.date}")
                    mealRepo.deleteDayMeals(from)
                }
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
            // [AI修改] H1：固定餐次带默认时间；非固定(加餐)默认取**当前时间**，不再强制手动选，可改。
            mealTime = if (defaultType?.isFixed == true) defaultType.defaultTime else DateTime.nowTime(),
        )
    }

    /**
     * 按入口类型解析目标日期并触发加载。[AI生成]
     */
    private fun loadConfiguredDate() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            val cf = copyFromDate
            if (cf != null) {
                loadCopyFrom(cf)
                return@launch
            }
            val targetDate = pendingEditDate ?: resolveNewMealDate()
            loadMealsForDateInternal(targetDate)
        }
    }

    /**
     * 食历"复制"：把来源某天的餐次/菜品预填为**新建草稿**(日期默认源+1，可改)。[AI生成]
     *
     * F8：复制不再直接写库、也不是"复用到今天/明天"，而是走加餐流程——预填餐次/菜品，日期用户可再改。
     */
    fun configureCopy(sourceDate: LocalDate) {
        // [AI修改] 用独立 copyConfigured 守卫(而非 configured)：即便 init 的默认 configure 先跑并置 configured=true，
        // 复制仍能应用；且复制入口只应用一次，返回重组时不重复加载。
        if (copyConfigured) return
        copyConfigured = true
        configured = true
        userPickedDate = true // 复制草稿视为"用户已定日期"，防止返回重组时 configure 按目标日重载覆盖草稿
        copyFromDate = sourceDate
        pendingEditDate = DateTime.plusDays(sourceDate, 1)
        if (_state.value.mealTypes.isNotEmpty()) loadConfiguredDate()
    }

    private suspend fun loadCopyFrom(sourceDate: LocalDate) {
        // [AI修改] #2：复制的目标日期 = 当前**最新餐食日期 + 1**(接在整个食历之后)，而非源日期+1；
        // 无餐食时退回源+1。同时设可选下限 minSelectableDate=target，弹框里 target 之前的日期都不能选。
        val today = DateTime.today()
        val latest = mealRepo.dateRange().second
        val target = if (latest != null) maxOf(DateTime.plusDays(latest, 1), today) else DateTime.plusDays(sourceDate, 1)
        val src = mealRepo.loadDayMealsForEdit(sourceDate)
        val blocks = src.map { meal ->
            MealBlockUiState(id = nextBlockId++, mealTypeId = meal.mealTypeId, mealTime = meal.mealTime, dishes = meal.dishes, note = meal.note)
        }
        loadedFromDate = null // 复制=新建，保存不删源
        copyFromDate = null // 一次性
        val fallback = _state.value.mealTypes.firstOrNull { it.code == "BREAKFAST" } ?: _state.value.mealTypes.firstOrNull()
        val finalBlocks = blocks.ifEmpty { listOf(newBlock(fallback)) }
        _state.value = _state.value.copy(
            date = target,
            isPlan = target > today,
            mealBlocks = finalBlocks,
            activeBlockId = finalBlocks.firstOrNull()?.id,
            isEditingExisting = false, // 复制是新建，日期可改
            minSelectableDate = target, // target 之前(含所有已有餐食日期)不可选
        )
        AppLogger.d(TAG, "load copy-from: source=$sourceDate latest=$latest target=$target blocks=${finalBlocks.size}")
    }

    /**
     * 新建餐食默认日期。[AI修改]
     *
     * 规则(按用户要求)：
     * 1. 有"今天或未来"的餐食记录 → 接在**最后餐食日期 + 1**(顺延排在已有餐食之后)；
     * 2. 无记录 / 最后餐食早于今天 → 按当前时段：上午(<12点)默认今天、下午或晚上(≥12点)默认明天。
     */
    private suspend fun resolveNewMealDate(): LocalDate {
        val today = DateTime.today()
        val latest = runCatching { mealRepo.dateRange().second }.getOrNull()
        if (latest != null && latest >= today) {
            return DateTime.plusDays(latest, 1) // 接在最后餐食之后
        }
        // 无记录或最后餐食早于今天：上午可选今天，下午/晚上默认明天。
        return if (DateTime.currentHour() < AFTERNOON_START_HOUR) today else DateTime.plusDays(today, 1)
    }

    /**
     * 实际执行日期回填；调用方负责管理 loadJob，避免配置任务被自己取消。[AI生成]
     */
    private suspend fun loadMealsForDateInternal(date: LocalDate) {
        AppLogger.d(TAG, "load meals begin: date=$date") // [AI生成] 记录数据库加载日期，排查未保存编辑被覆盖。
        val existingMeals = mealRepo.loadDayMealsForEdit(date)
        // [AI生成] F7：记录本次是否加载自"已有餐食的日期"，供保存时"改日期=移动"删旧。
        loadedFromDate = if (existingMeals.isNotEmpty()) date else null
        AppLogger.d(TAG, "load meals db result: date=$date existing=${existingMeals.map { it.mealTypeId to it.dishes.map { dish -> dish.id } }}") // [AI生成] 记录数据库返回的餐食摘要。
        val blocks = if (existingMeals.isEmpty()) {
            // [AI修改] 新建空块默认餐次按当前时段智能推断(晚上→晚餐,不再恒早餐)；未来计划日从早餐排起。
            val defaultType = pickDefaultMealType(
                _state.value.mealTypes,
                isFuture = date > DateTime.today(),
                nowHour = DateTime.currentHour(),
            )
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
            // [AI生成] N3：加载到既有餐食=编辑模式→日期锁定；空日期(新增)→可改。
            isEditingExisting = existingMeals.isNotEmpty(),
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
