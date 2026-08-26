package com.sxdbsm.cookbook.android.ui.addmeal

import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FavoriteComboRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.data.repository.MealProjectionRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.FavoriteCombo
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordDraft
import com.sxdbsm.cookbook.usecase.mealrecording.MealRecordUseCase
import com.sxdbsm.cookbook.platform.LogLevel
import com.sxdbsm.cookbook.platform.Logger
import com.sxdbsm.cookbook.platform.OperationState
import com.sxdbsm.cookbook.platform.StructuredLogEvent
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    val careHint: com.sxdbsm.cookbook.ai.MealHealthHint? = null, // [AI生成] 运营#177 ③ 记菜命中慢病轻提示(保存成功一次性·仅新增·由 UI 拼进 Snackbar·消费即清)
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
    private val mealProjectionRepository: MealProjectionRepository,
    private val dishRepo: DishRepository,
    private val comboRepo: FavoriteComboRepository,
    private val analytics: com.sxdbsm.cookbook.analytics.Analytics, // [AI生成] 阶段3-b：记一餐埋点(meal_logged·仅餐次枚举)
    private val hintUseCase: com.sxdbsm.cookbook.ai.MealHealthHintUseCase, // [AI生成] 运营#177 ③ 记菜命中慢病轻提示判定(纯读·薄 VM 只编排)
    private val mealRecordUseCase: MealRecordUseCase,
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

    /** [AI修改] 日期月历直接复用食历同一数据真相源，不逐日查询。 */
    val mealDates: StateFlow<Set<LocalDate>> =
        mealProjectionRepository.observeTimelineDates()
            .map { it.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /**
     * "常吃"菜品(供餐次块内一键 chips 快捷加菜)。[AI生成] part1 餐次常吃 chips
     *
     * 口径(Apple-UX 设计门禁定)：**喜爱度>0(真被记过餐)** 的常吃菜为主 + 最近记录兜底补齐，去重取 16；
     * UI 层各餐次块再按"排除本块已加菜"过滤取前 8（每块已选不同，过滤放 UI 而非此处）。
     * 全新用户无历史→两路皆空→列表空→UI 不渲染 chips 区(无数据不占位)。
     * 冷流用 stateIn(WhileSubscribed)，UI 用 collectAsStateWithLifecycle 消费(勿裸 collect)。
     */
    val frequentDishes: StateFlow<List<DishMini>> =
        combine(
            dishRepo.observePopularDishes(24),
            dishRepo.observeRecentDishes(16),
        ) { popular, recent ->
            val eaten = popular.filter { it.preference > 0 } // 真正被记过餐的"常吃"
            (eaten + recent).distinctBy { it.id }.take(16) // 不足则最近记录兜底(recent 亦均有餐记录)
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList()) // [AI修改] 用户反馈"常吃要选了菜才出现"：改 Eagerly 在 VM 创建即预热，打开餐次块就已就绪(WhileSubscribed 有初始空窗期)。

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
    private var baselineSig: String? = null // [AI生成] 未保存返回守卫(§9.17)：加载完成时的内容签名基线；与当前不同=有未保存改动。

    /** 当前表单内容签名(日期+各餐次的餐次/时间/菜品id)。[AI生成] */
    private fun sigNow(): String = buildString {
        append(_state.value.date)
        _state.value.mealBlocks.forEach { b ->
            append("|"); append(b.mealTypeId); append(":"); append(b.mealTime)
            append(":"); append(b.dishes.map { it.id }.sorted().joinToString(","))
        }
    }

    /** 加载完成后记基线(此后任何增删改=dirty)。[AI生成] */
    private fun markBaseline() { baselineSig = sigNow() }

    /** 有未保存改动(供 UnsavedGuard)。[AI生成] 未记基线(尚未加载完)时视为不脏，避免误报。 */
    fun isDirty(): Boolean = baselineSig?.let { it != sigNow() } ?: false

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
            AppLogger.d(TAG, "configure_reload_skipped")
            return // [AI修改] 页面从新建菜品返回时可能重新组合，入口日期相同则保留当前未保存表单，避免旧餐食覆盖用户编辑。
        }
        AppLogger.d(TAG, "configure_load_started")
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
        // [AI修改] 修"提示不随日期更新"(用户2026-07-19)：原 `if (date==当前) return` 会在**重选当前(已生效的空)日期**时
        //   直接返回、不清掉残留的旧冲突提示——警告后重开日历不改直接确认(日历默认选中即当前日期)就会命中此路径，
        //   导致"X 已有餐食"一直挂在顶上。原则=落到无餐食的日期(含重选当前日期)一律隐藏提示，只有落到有餐食的日期才提示。
        if (date == _state.value.date) {
            if (_state.value.dateWarning != null) _state.value = _state.value.copy(dateWarning = null)
            return
        }
        configured = true
        // [AI修改] #2：复制场景有下限(最新餐食+1)，早于下限直接提示不切换。
        val minDate = _state.value.minSelectableDate
        if (minDate != null && date < minDate) {
            _state.value = _state.value.copy(dateWarning = "只能选择 $minDate 及之后的日期(接在现有餐食之后)")
            return
        }
        viewModelScope.launch {
            val occupied = mealRecordUseCase.queryDayForEdit(date).isNotEmpty()
            if (occupied) {
                // [AI修改] 冲突时日期不变，也不置 userPickedDate(避免污染后续 configure 重载判断)。
                AppLogger.d(TAG, "date_change_conflict existing_meals=true")
                _state.value = _state.value.copy(dateWarning = "$date 已经有餐食了，请到食历里编辑那天，或选一个空日期")
            } else {
                AppLogger.d(TAG, "date_change_applied")
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

    /** 消费一次性慢病轻提示(Snackbar 弹过即清，避免重组重复)。[AI生成] 运营#177 ③ */
    fun consumeCareHint() {
        if (_state.value.careHint != null) _state.value = _state.value.copy(careHint = null)
    }

    /**
     * 新增一个餐食模块。[AI生成]
     */
    fun addMealBlock() {
        // [AI修改] F6+K4：默认取"下一个尚未添加"的餐次(按 mealTypes 顺序:早餐→中餐→晚餐→加餐→宵夜)，
        // 而非每次都早餐。已有早餐→默认中餐…全用完则回退第一个。
        val usedTypeIds = _state.value.mealBlocks.map { it.mealTypeId }.toSet()
        val defaultType = _state.value.mealTypes.firstOrNull { it.id !in usedTypeIds }
            ?: _state.value.mealTypes.firstOrNull { it.code == "BREAKFAST" }
            ?: _state.value.mealTypes.firstOrNull()
        val block = newBlock(defaultType)
        _state.value = _state.value.copy(
            mealBlocks = _state.value.mealBlocks + block,
            activeBlockId = block.id,
        )
        AppLogger.d(TAG, "meal_block_added")
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
        AppLogger.d(TAG, "meal_block_removed remaining_count=${blocks.size}")
    }

    /**
     * 修改指定模块的餐次。[AI修改]
     *
     * 固定餐次带出其默认时间(如"加餐"默认15:00)，用户想改再改——
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
        AppLogger.d(TAG, "active_block_changed")
    }

    /**
     * 向指定餐食模块添加菜品。[AI修改]
     */
    fun addDishes(blockId: Long, dishes: List<DishMini>) {
        AppLogger.d(TAG, "dishes_add_requested count=${dishes.size}")
        updateBlock(blockId) { block ->
            block.copy(dishes = (block.dishes + dishes).distinctBy { it.id })
        }
        AppLogger.d(TAG, "dishes_add_applied")
    }

    /**
     * 新建菜品返回后，按 id 拉取轻量菜品并加入当前餐食模块。[AI生成]
     */
    fun addCreatedDish(dishId: Long, blockId: Long? = _state.value.activeBlockId, onMerged: (Boolean) -> Unit = {}) {
        if (dishId <= 0 || blockId == null) {
            onMerged(false)
            return
        }
        AppLogger.d(TAG, "created_dish_add_started")
        viewModelScope.launch {
            val dish = dishRepo.getDishMiniById(dishId)
            if (dish != null) {
                AppLogger.d(TAG, "created_dish_loaded")
                addDishes(blockId, listOf(dish))
                onMerged(true)
            } else {
                onMerged(false)
            }
        }
    }

    fun addComboDishes(blockId: Long, combo: FavoriteCombo) {
        addDishes(blockId, combo.dishes) // [AI生成] 组合复用只把组合内菜品加入当前餐食模块，不改变组合本身。
    }

    /** [AI生成] 组合复用支持部分选：只把用户勾选的菜品(全选/部分选)加入餐食块，不改组合本身。 */
    fun addComboDishes(blockId: Long, combo: FavoriteCombo, selectedDishIds: Set<Long>) {
        val picked = combo.dishes.filter { it.id in selectedDishIds }
        if (picked.isNotEmpty()) addDishes(blockId, picked)
    }

    /** [AI生成] AI 推荐"选它"从餐次进入时，把菜品直接加入该餐次块。 */
    fun addDishesByIds(blockId: Long, ids: List<Long>, onMerged: (Boolean) -> Unit = {}) {
        if (ids.isEmpty()) {
            onMerged(false)
            return
        }
        viewModelScope.launch {
            val dishes = ids.mapNotNull { dishRepo.getDishMiniById(it) }
            if (dishes.isNotEmpty()) {
                addDishes(blockId, dishes)
                onMerged(true)
            } else {
                onMerged(false)
            }
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
        AppLogger.d(TAG, "dish_remove_requested")
        updateBlock(blockId) { block ->
            block.copy(dishes = block.dishes.filterNot { it.id == dishId })
        }
        AppLogger.d(TAG, "dish_remove_applied")
    }

    /**
     * 批量保存当天所有有效餐食模块。[AI修改]
     */
    /**
     * 餐次 code → 匿名统计餐次枚举(四值粗粒度·去标识化)。[AI生成] 阶段3-b
     *
     * 项目餐次 code=BREAKFAST/LUNCH/DINNER/SNACK/NIGHT_SNACK(K4 后)。
     * **SNACK/NIGHT_SNACK 均归"加餐"粗粒度统计**，非三餐一律归 SNACK。
     */
    private fun slotTagOf(code: String?): com.sxdbsm.cookbook.analytics.MealSlotTag = when (code) {
        "BREAKFAST" -> com.sxdbsm.cookbook.analytics.MealSlotTag.BREAKFAST
        "LUNCH" -> com.sxdbsm.cookbook.analytics.MealSlotTag.LUNCH
        "DINNER" -> com.sxdbsm.cookbook.analytics.MealSlotTag.DINNER
        else -> com.sxdbsm.cookbook.analytics.MealSlotTag.SNACK // [AI修改] K4：SNACK/NIGHT_SNACK 均=加餐粗粒度统计
    }

    fun save() {
        val s = _state.value
        val drafts = s.mealBlocks
            .filter { it.canSave }
            .map { block ->
                // canSave 已保证 mealTypeId/mealTime 非空，用 !! 明确不变量(原 `?: return` 在 filter 后不可达且误导)。
                MealRecordDraft(
                    date = s.date,
                    mealTypeId = block.mealTypeId!!,
                    mealTime = block.mealTime!!,
                    note = block.note,
                    dishIds = block.dishes.map { it.id },
                )
            }
        if (drafts.isEmpty()) return
        val trace = Logger.operation("meal.save")
        AppLogger.d(TAG, "meal_save_started block_count=${drafts.size} dish_count=${drafts.sumOf { it.dishIds.size }}")
        viewModelScope.launch {
            // [AI修改] viewModelScope 会随 ViewModel 销毁自动取消，避免页面关闭后继续持有 UI。
            // [AI修改] D10：saving 标志用最新 _state.value 写回(不用启动前捕获的 s 快照)，避免理论上冲掉并发字段。
            _state.value = _state.value.copy(saving = true)
            trace.start()
            try {
                // [AI修改] 抬喜爱度基线=loadedFromDate：编辑同日=本日、移动=来源日、新增=null(视目标日)。
                // 修"移动到空日期时把来源日已计过的菜再+1"的 bug。
                mealRecordUseCase.saveDay(date = s.date, drafts = drafts, incrementBaselineDate = loadedFromDate)
                // [AI生成] F7：若本次是编辑已有某天并把日期改到了新日期(移动)，保存到新日期后删除旧日期，避免重复。
                val from = loadedFromDate
                if (from != null && from != s.date) {
                    AppLogger.d(TAG, "meal_move_delete_source")
                    mealRecordUseCase.deleteDay(from)
                }
                AppLogger.d(TAG, "meal_save_succeeded block_count=${drafts.size}")
                AppLogger.event(
                    "meal_save",
                    mapOf(
                        "blockCount" to drafts.size,
                        "dishCount" to drafts.sumOf { it.dishIds.size },
                        "success" to true,
                    ),
                ) // [AI生成] 内测埋点：记录餐食保存成功摘要。
                // [AI生成] 阶段3-b 匿名统计：记了一餐(核心KPI周频次)。**仅上报餐次枚举**·不带菜名/日期/给谁。
                //   口径:一次save(可含多餐次块)=一次"记一餐"动作、只报一次(不逐块虚增)·slot取首块餐次(审查建议3)。
                analytics.track(com.sxdbsm.cookbook.analytics.AnalyticsEvent.MealLogged(slotTagOf(s.mealTypes.firstOrNull { mt -> mt.id == drafts.first().mealTypeId }?.code)))
                // [AI生成] 运营#177 ③：仅新增/复制新建(loadedFromDate==null·编辑既有餐不提示、天然一次性)判本餐是否命中已登记慢病关注点，
                //   与 done 同帧置入 careHint 供 UI 拼一句 Snackbar(Top-1·定性不显数字)。判定纯读、失败降级 null，绝不阻断保存(行为师 T1 契约)。
                val hint = if (loadedFromDate == null)
                    runCatching { hintUseCase.evaluate(drafts.flatMap { it.dishIds }.distinct(), s.date) }.getOrNull()
                else null
                _state.value = _state.value.copy(saving = false, done = true, careHint = hint, errorMessage = null)
                trace.succeed()
            } catch (e: CancellationException) {
                trace.cancel()
                throw e
            } catch (e: Throwable) {
                trace.fail(e.javaClass.simpleName)
                AppLogger.e(TAG, "meal_save_failed error_type=${e.javaClass.simpleName}")
                AppLogger.event(
                    "meal_save",
                    mapOf(
                        "blockCount" to drafts.size,
                        "dishCount" to drafts.sumOf { draft -> draft.dishIds.size },
                        "success" to false,
                        "errorType" to e.javaClass.simpleName,
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
        // 无餐食时退回源+1。[AI修改] J18:minSelectableDate=today(允许复制到今天及以后·不再被未来计划餐锁死)。
        val today = DateTime.today()
        val latest = mealRecordUseCase.dateRange().second
        val target = if (latest != null) maxOf(DateTime.plusDays(latest, 1), today) else DateTime.plusDays(sourceDate, 1)
        val src = mealRecordUseCase.queryDayForEdit(sourceDate)
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
            minSelectableDate = today, // [AI修改] J18:放低到today——复制不锁死在"末次餐食之后"，允许用户复制到今天/明天/任意未来日
        )
        markBaseline() // [AI生成] 复制加载完成记基线
        AppLogger.d(TAG, "copy_load_applied block_count=${finalBlocks.size}")
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
        val latest = runCatching { mealRecordUseCase.dateRange().second }.getOrNull()
        if (latest != null && latest >= today) {
            return DateTime.plusDays(latest, 1) // 接在最后餐食之后
        }
        // 无记录或最后餐食早于今天：上午可选今天，下午/晚上默认明天。
        return if (DateTime.currentHour() < AFTERNOON_START_HOUR) today else DateTime.plusDays(today, 1)
    }

    /**
     * 实际执行日期回填；调用方负责管理 loadJob，避免配置任务被自己取消。[AI生成]
     */
    /** [AI修改] AI 记餐保存后触发重新加载当前日期的餐食 */
    fun reloadAfterAiSave(date: LocalDate) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            loadMealsForDateInternal(date)
        }
    }

    /** [AI生成] B6: 查询一周内哪些天已有餐食，供周期记灰显。 */
    suspend fun datesWithMealsInWeek(weekMonday: LocalDate): Set<LocalDate> {
        val result = mutableSetOf<LocalDate>()
        for (i in 0..6) {
            val d = DateTime.plusDays(weekMonday, i)
            if (mealRecordUseCase.queryDayForEdit(d).isNotEmpty()) result.add(d)
        }
        return result
    }

    private suspend fun loadMealsForDateInternal(date: LocalDate) {
        AppLogger.d(TAG, "meal_load_started")
        val existingMeals = mealRecordUseCase.queryDayForEdit(date)
        // [AI生成] F7：记录本次是否加载自"已有餐食的日期"，供保存时"改日期=移动"删旧。
        loadedFromDate = if (existingMeals.isNotEmpty()) date else null
        AppLogger.d(TAG, "meal_load_read block_count=${existingMeals.size}")
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
        markBaseline() // [AI生成] 加载完成记基线，供未保存返回守卫
        AppLogger.d(TAG, "meal_load_applied block_count=${finalBlocks.size}")
    }

    private fun updateBlock(blockId: Long, transform: (MealBlockUiState) -> MealBlockUiState) {
        _state.value = _state.value.copy(
            mealBlocks = _state.value.mealBlocks.map { block ->
                if (block.id == blockId) transform(block) else block
            },
        )
    }
}
