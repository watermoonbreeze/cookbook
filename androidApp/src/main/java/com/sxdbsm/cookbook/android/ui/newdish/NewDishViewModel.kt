package com.sxdbsm.cookbook.android.ui.newdish

import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.MealSlot
import com.sxdbsm.cookbook.ai.MealSlotMatcher
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.ShareLinkRepository
import com.sxdbsm.cookbook.domain.model.CookingMethod
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.DishStep
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.MeasurementUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock // [AI生成] AUTOGEN-UNIFY：AutoGenContext 懒加载单飞

/**
 * 新建/编辑菜品页 UI 状态。[AI修改]
 *
 * 同一个状态类同时支持"新建菜品"和"编辑菜品"：`editingId == null` 表示新建。
 */
data class NewDishUiState(
    val editingId: Long? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val cookingMethodId: Long? = null,
    val cookingMethodName: String? = null,
    val cookingMethodInput: String = "",
    val cookingMethodNames: List<String> = emptyList(),
    val availableCookingMethods: List<CookingMethod> = emptyList(),
    val availableTags: List<com.sxdbsm.cookbook.domain.model.DishTag> = emptyList(), // [AI生成] T3：标签库(选择+管理)
    val ingredients: List<DishIngredient> = emptyList(),
    val nameSuggestions: List<String> = emptyList(), // [AI生成] 菜名推食材：从菜名推出、尚未加入的候选食材名(点击确认加入)
    val steps: List<DishStep> = emptyList(), // [AI生成] 菜品操作步骤草稿，保存时随菜品事务一起落库。
    val specialNote: String = "",
    val description: String = "",
    val cuisine: String = com.sxdbsm.cookbook.domain.model.Cuisines.HOME, // [AI修改] 菜系分类:自建菜默认"家常菜"(零操作·家庭菜天然家常，解决自建菜在菜系Tab看不到)；编辑既有菜时按 d.cuisine 覆盖
    val mealSlots: List<MealSlot> = emptyList(), // [AI生成] v28：适合餐次(多选)，新建按菜名智能预选、编辑回显存储值
    val mealSlotTouched: Boolean = false, // [AI生成] v28：用户是否手动碰过餐次chip(碰过则不再自动预选覆盖)
    val mealSlotPrefilled: Boolean = false, // [AI生成] v28：当前餐次是否为Matcher智能预选(用于显"已按菜名智能预选"提示行)
    // [AI生成] AUTOGEN-UNIFY STEP-AU-5.5：菜名自动预选烹饪方式(同构 v28 餐次预选范式)。
    //   touched=用户手动碰过任一烹饪方式入口(碰过则锁定不再自动预选)；prefilled=当前做法为智能预选(显"已按菜名预选，可改"内联提示行)。
    val cookingMethodTouched: Boolean = false,
    val cookingMethodPrefilled: Boolean = false,
    val imagePath: String = "",
    val thumbnailPath: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val editProbeToastMessage: String? = null,
    val editProbeToastSerial: Int = 0,
    val autoAddMessage: String? = null, // [AI生成] 菜名自动加食材的一次性 Snackbar 文案(待自建会说明)
    val autoAddSerial: Int = 0, // [AI生成] 配合上者:序号变化触发一次 Snackbar
    val saving: Boolean = false,
    val done: Boolean = false,
    val savedDishId: Long? = null,
    val cookingMethodImported: Boolean = false, // [AI生成] STEP-L4-5.7：做法为导入预填(提示行"来自菜谱导入，可改"+推演锁定·INV-L4-09 双锁之一)。
    val ingredientSourceTag: String = "user", // [AI生成] STEP-L4-5.7：食材来源标签(链接导入"link"·save() ensureCreated 透传；默认"user"保住既有门控)。

    val availableUnits: List<MeasurementUnit> = emptyList(), // [AI修改] 食材用量单位下拉列表。
    val stepTemplates: List<com.sxdbsm.cookbook.domain.model.StepTemplate> = emptyList(), // [AI生成] #2 "选择步骤"可套用的步骤模板(预设+自建)。
    val ingredientGroups: List<com.sxdbsm.cookbook.domain.model.IngredientGroup> = emptyList(), // [AI生成] B5 "配料组"可一键加入的常用食材组。
    val mainMissingHint: Boolean = false, // [AI生成] 保存时有食材但0主料→内联提示(非阻断)，用户标任一主料后自动消失。
)

/**
 * 新建/编辑菜品 ViewModel。[AI修改]
 *
 * 负责表单状态、导入已有菜品、增删标签/食材以及保存菜品。
 */
class NewDishViewModel(
    private val dishRepo: DishRepository,
    private val ingredientRepo: IngredientRepository,
    @Suppress("unused") private val categoryRepo: FoodCategoryRepository,
    @Suppress("unused") private val pref: com.sxdbsm.cookbook.data.repository.PreferenceRepository,
    private val stepTemplateRepo: com.sxdbsm.cookbook.data.repository.StepTemplateRepository, // [AI生成] #2 步骤模板
    private val ingredientGroupRepo: com.sxdbsm.cookbook.data.repository.IngredientGroupRepository, // [AI生成] B5 配料组
    // [AI生成] AUTOGEN-UNIFY STEP-AU-5.1：统一自动入库管线三件套(快速自建食材走 ensureCreated 归类+营养一次做齐)。
    private val autoGen: com.sxdbsm.cookbook.domain.autogen.IngredientAutoGenerator,
    private val db: com.sxdbsm.cookbook.db.CookbookDatabase,
    private val aliasResolver: com.sxdbsm.cookbook.domain.autogen.IngredientAliasResolver,
    private val shareLinkRepo: ShareLinkRepository, // [AI生成] STEP-L4-5.1：链接导入回写 share_link.dish_id(DP-P1-4·保存成功 markSaved)。
) : ViewModel() {

    private val _state = MutableStateFlow(NewDishUiState()) // [AI修改] 表单内部可变状态。
    val state: StateFlow<NewDishUiState> = _state.asStateFlow() // [AI修改] UI 只能观察，不能直接改。

    // [AI生成] AUTOGEN-UNIFY：AutoGenContext 懒加载(Mutex 单飞防 save/applyGroup 并发首调双 load)·
    //   会话内复用(防 N+1)；陈旧误判 CREATE 由 commit 内"createUserIngredient 双层去重+仅空列守卫"兜住(审核 A1)。
    private val autoGenCtxMutex = kotlinx.coroutines.sync.Mutex()
    private var autoGenCtx: com.sxdbsm.cookbook.domain.autogen.AutoGenContext? = null

    private suspend fun autoGenCtx(): com.sxdbsm.cookbook.domain.autogen.AutoGenContext {
        autoGenCtx?.let { return it }
        return autoGenCtxMutex.withLock {
            autoGenCtx ?: com.sxdbsm.cookbook.domain.autogen.AutoGenContext
                .load(db, aliasResolver)
                .also { autoGenCtx = it }
        }
    }
    private var activeStartKey: String? = null // [AI生成] 记录当前路由参数，避免同一编辑页重复触发 start 清空表单。
    private var baselineSig: String? = null // [AI生成] 加载/新建时的表单内容签名，用于返回前判断是否有未保存改动。
    private var prefillLinkId: Long? = null // [AI生成] STEP-L4-5.9：链接导入行的 share_link.id(保存成功回写 dish_id·applyPrefill 时记)。

    /** 表单可编辑内容的签名(只含用户内容，排除字典/瞬态标志)。[AI生成] */
    private fun NewDishUiState.contentSig(): String = listOf(
        name.trim(), tags.sorted().toString(), cookingMethodId, cookingMethodNames.sorted().toString(),
        cookingMethodInput.trim(), cuisine, mealSlots.map { it.code }.sorted().toString(), specialNote.trim(), description.trim(), imagePath, thumbnailPath,
        ingredients.map { it.ingredient.id to (it.quantity to it.isMain) }.toString(), // [AI修改] 纳入 isMain，切换主料标=有改动(§9.17未保存守卫)
        steps.map { it.text.trim() to it.imagePath }.toString(),
    ).toString()

    /** 记录当前为"无改动基线"(加载完成/新建重置后调用)。[AI生成] */
    private fun markBaseline() { baselineSig = _state.value.contentSig() }

    /** 表单相对基线是否有未保存改动(供返回前"放弃更改?"守卫)。[AI生成] */
    fun isDirty(): Boolean = baselineSig?.let { it != _state.value.contentSig() } ?: false

    private companion object {
        private const val TAG = "NewDishEdit" // [AI生成] 菜品编辑链路统一日志 Tag，方便 logcat 过滤排查。
        private const val DEFAULT_GRAMS = 100 // [AI生成] #55 新加食材默认剂量(克)。
    }

    // [AI生成] 调料(调味品/油脂类)id 集合：加配料时按调料给正常默认克数(非100g)。init 异步加载，空集时退回默认100g。
    @Volatile
    private var seasoningIds: Set<Long> = emptySet()

    // [AI生成] 菜名推食材：缓存全部食材名(init 加载)，setName 时本地推演候选(纯函数·快)。
    @Volatile
    private var cachedIngredientNames: List<String> = emptyList()

    // [AI生成] 待自建食材的占位临时负 id(递减唯一)；保存时经统一自动入库管线(ensureCreated)换真 id。
    private var pendingIdSeq = -1L

    // [AI生成] 菜名自动加食材的防抖 job：用户停顿后再推演，避免逐字命中弹多次。
    private var autoAddJob: kotlinx.coroutines.Job? = null

    // [AI生成] 单位字典就绪信号：加食材(尤其菜名自动加食材)必须等单位加载完再取"克"单位，
    // 否则 gramUnit() 返 null → 默认克数落到食材计件默认单位(个/只)→"100.0个"错单位(bug根因)。
    private val unitsReady = kotlinx.coroutines.CompletableDeferred<Unit>()

    // [AI生成] 缓存"克"单位：availableUnits 一加载即缓存，避免依赖 state 时序；gramUnit() 兜底读它。
    private var cachedGramUnit: MeasurementUnit? = null

    init {
        viewModelScope.launch {
            seasoningIds = runCatching { ingredientRepo.seasoningIngredientIds() }.getOrDefault(emptySet())
        }
        viewModelScope.launch {
            cachedIngredientNames = runCatching { ingredientRepo.allActiveNames() }.getOrDefault(emptyList())
            // [AI修改] 餐次预选只依赖菜名、不需单位，先做——避免被 autoAddFromName 内的 unitsReady.await() 顺延(慢机字典慢时观感卡半拍)。
            updateMealSlotPreselect() // [AI生成] v28：预填菜名的智能预选餐次
            autoAddFromName() // 名字可能已预填(新建带菜名/组成菜品)，加载完名单后补推一次(内部会 await 单位就绪)
        }
        viewModelScope.launch {
            // [AI修改] 页面打开后加载计量单位字典，用于食材用量输入。
            // [AI修改] 先把挂起查询结果放到局部变量，再基于最新 state 合并，避免旧空表单快照覆盖编辑加载结果。
            // [AI修改] 三个字典查询各自 runCatching 降级(对齐同 init 里 seasoningIds/cachedIngredientNames)：
            //          任一抛异常也不能让 unitsReady 永不完成——否则 autoAddFromName 的 await 永挂、
            //          连带 scheduleAutoAddFromName 里紧随的餐次预选也永久失效(Google 审查建议1)。
            val units = runCatching { ingredientRepo.listMeasurementUnits() }.getOrDefault(emptyList())
            val cookingMethods = runCatching { dishRepo.listCookingMethods() }.getOrDefault(emptyList())
            val tags = runCatching { dishRepo.listDishTags() }.getOrDefault(emptyList()) // [AI生成] T3：标签库
            // [AI生成] 单位一到手先缓存"克"并放行 unitsReady，让等待中的自动加食材拿到正确单位(空字典则 null，下游走 UI 显示瑕疵而非永挂)。
            cachedGramUnit = units.firstOrNull { it.name == "g" || it.name == "克" }
            _state.update { current ->
                AppLogger.d(TAG, "dictionaries_initialized unit_count=${units.size} method_count=${cookingMethods.size}")
                current.copy(
                    availableUnits = units,
                    availableCookingMethods = cookingMethods,
                    availableTags = tags,
                )
            }
            if (!unitsReady.isCompleted) unitsReady.complete(Unit)
        }
    }

    /**
     * 根据路由参数启动新建、编辑或导入模式。[AI生成]
     *
     * 导航栈可能复用同一个 ViewModel，如果不先重置表单，旧的新建状态会污染编辑页。
     * 这里统一入口：编辑优先于导入；无参数时就是干净的新建表单。
     */
    fun start(editingDishId: Long?, importDishId: Long?) {
        val editId = editingDishId?.takeIf { it > 0L }
        val sourceId = importDishId?.takeIf { it > 0L }
        val current = _state.value
        val startKey = "edit=${editId ?: -1L};import=${sourceId ?: -1L}"
        if (activeStartKey == startKey) {
            AppLogger.d(TAG, "start_ignored reason=duplicate loading=${current.loading} has_error=${current.errorMessage != null}")
            return
        } // [AI修改] 同一路由参数重复进入时绝不重置表单，避免加载成功后被空状态覆盖。
        AppLogger.d(TAG, "start_requested")
        activeStartKey = startKey
        prefillLinkId = null // [AI生成] STEP-L4-5.9：新表单重置时清掉上次链接导入的 linkId(编辑/导入不得残留；放防重复守卫之后——重复 start 不误清；UiState 全新构造已天然重置 imported/sourceTag 两新字段)。
        _state.value = NewDishUiState(
            editingId = editId,
            availableUnits = current.availableUnits,
            availableCookingMethods = current.availableCookingMethods,
            availableTags = current.availableTags,
            loading = editId != null || sourceId != null,
        ) // [AI生成] 保留字典数据，只清空表单草稿和保存完成标记。

        when {
            editId != null -> loadForEdit(editId)
            sourceId != null -> importFromDishId(sourceId)
            else -> markBaseline() // [AI生成] 纯新建：空表单为基线，之后任何输入即算 dirty。
        }
    }

    /**
     * 加载已有菜品进入编辑模式。[AI修改]
     */
    fun loadForEdit(dishId: Long) {
        viewModelScope.launch {
            AppLogger.d(TAG, "edit_load_started")
            _state.value = _state.value.copy(
                editingId = dishId,
                loading = true,
                errorMessage = null,
                saving = false,
                done = false,
            ) // [AI修改] 先进入编辑模式，避免加载期间标题或保存逻辑误判为新建。
            runCatching { dishRepo.getDishById(dishId) }
                .onSuccess { d ->
                    if (d == null) {
                        AppLogger.w(TAG, "edit_load_empty")
                        _state.value = _state.value.copy(
                            loading = false,
                            saving = false,
                            errorMessage = "未找到要编辑的菜品",
                            editProbeToastMessage = "编辑菜品 ID=$dishId 当前为空",
                            editProbeToastSerial = _state.value.editProbeToastSerial + 1,
                        ) // [AI生成] 避免编辑页标题正确但表单空白时没有任何提示。
                    } else {
                        AppLogger.d(TAG, "edit_load_succeeded tag_count=${d.tags.size} ingredient_count=${d.ingredients.size}")
                        _state.value = _state.value.copy(
                            editingId = d.id,
                            name = d.name,
                            tags = d.tags,
                            cuisine = d.cuisine,
                            cookingMethodId = d.cookingMethodId,
                            cookingMethodName = d.cookingMethodName,
                            cookingMethodInput = d.cookingMethodName.orEmpty(),
                            cookingMethodNames = d.cookingMethods.map { it.name }.ifEmpty { d.cookingMethodName?.let(::listOf).orEmpty() },
                            ingredients = d.ingredients,
                            steps = d.steps,
                            // [AI生成] v28：编辑回显存储餐次；老库未打标(空)则按菜名 Matcher 智能预选并显提示行，保存即补齐。
                            mealSlots = d.mealSlots.ifEmpty { MealSlotMatcher.defaultSlotsFor(d.name) },
                            mealSlotTouched = false,
                            mealSlotPrefilled = d.mealSlots.isEmpty(),
                            specialNote = d.specialNote,
                            description = d.description,
                            imagePath = d.imagePath,
                            thumbnailPath = d.thumbnailPath,
                            loading = false,
                            errorMessage = null,
                            done = false,
                            saving = false,
                            editProbeToastMessage = "正在编辑：${d.name} ID=${d.id}",
                            editProbeToastSerial = _state.value.editProbeToastSerial + 1,
                        )
                        markBaseline() // [AI生成] 载入既有菜品后记基线，之后改动才算 dirty。
                    }
                }
                .onFailure { error ->
                    AppLogger.e(TAG, "edit_load_failed error_type=${error.javaClass.simpleName}")
                    _state.value = _state.value.copy(
                        loading = false,
                        saving = false,
                        errorMessage = "加载菜品失败，请返回后重试",
                        editProbeToastMessage = "编辑菜品 ID=$dishId 加载失败：加载菜品失败，请返回后重试",
                        editProbeToastSerial = _state.value.editProbeToastSerial + 1,
                    )
                }
        }
    }

    /**
     * 消费编辑诊断 Toast。[AI生成]
     *
     * Toast 是一次性事件，展示后清空消息但保留序号，避免 Compose 重组或返回前台时重复弹出。
     */
    fun consumeEditProbeToast() {
        _state.update { it.copy(editProbeToastMessage = null) } // [AI修改] 只清一次性消息，保留最新表单字段。
    }

    /** 从其他菜品导入，回填字段并自动加 #复制 标签。[AI修改] */
    fun importFrom(d: Dish) {
        _state.value = _state.value.copy(
            editingId = null,    // 始终新建
            name = d.name,
            tags = (d.tags + "#复制").distinct(),
            cuisine = d.cuisine,
            cookingMethodId = d.cookingMethodId,
            cookingMethodName = d.cookingMethodName,
            cookingMethodInput = d.cookingMethodName.orEmpty(),
            cookingMethodNames = d.cookingMethods.map { it.name }.ifEmpty { d.cookingMethodName?.let(::listOf).orEmpty() },
            ingredients = d.ingredients,
            steps = d.steps,
            // [AI生成] v28：复制带上源菜餐次(源为空则按菜名预选)，作为新菜的初始餐次。
            mealSlots = d.mealSlots.ifEmpty { MealSlotMatcher.defaultSlotsFor(d.name) },
            mealSlotTouched = false,
            mealSlotPrefilled = d.mealSlots.isEmpty(),
            specialNote = d.specialNote,
            description = d.description,
            imagePath = d.imagePath,
            thumbnailPath = d.thumbnailPath,
            loading = false,
            errorMessage = null,
        )
    }

    /**
     * 通过 id 加载菜品并导入为新菜品草稿。[AI修改]
     */
    fun importFromDishId(dishId: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, errorMessage = null)
            runCatching { dishRepo.getDishById(dishId) }
                .onSuccess { dish ->
                    if (dish == null) {
                        _state.value = _state.value.copy(loading = false, errorMessage = "未找到要导入的菜品")
                    } else {
                        importFrom(dish)
                        markBaseline() // [AI生成] 导入(复制)完成后以导入内容为基线，之后改动才算 dirty。
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, errorMessage = "导入菜品失败，请稍后重试")
                }
        }
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v); scheduleAutoAddFromName() }

    /** 菜名变化(仅新建模式)：防抖后按菜名推演并**自动加入**食材。[AI修改] 菜名推食材·推出即加入 */
    private fun scheduleAutoAddFromName() {
        if (_state.value.editingId != null) return // 编辑既有菜不自动加(防把用户删掉的食材又加回来)
        autoAddJob?.cancel()
        autoAddJob = viewModelScope.launch {
            kotlinx.coroutines.delay(350) // 防抖：等停顿再推演，避免逐字命中弹多次
            // [AI修改] 餐次预选只依赖菜名、不需单位，先做——避免被 autoAddFromName 内的 unitsReady.await() 顺延(慢机字典慢时观感卡半拍)。
            updateMealSlotPreselect() // [AI生成] v28：菜名稳定后按名智能预选餐次(未手动碰过才覆盖)
            updateCookingMethodPreselect() // [AI生成] AUTOGEN-UNIFY STEP-AU-5.5：菜名稳定后按名预选烹饪方式(未手动碰过才覆盖·同构 v28)
            autoAddFromName()
        }
    }

    /** 新建模式下按菜名智能预选餐次(未手动碰过才覆盖)。[AI生成] v28 */
    private fun updateMealSlotPreselect() {
        if (_state.value.editingId != null) return // 编辑既有菜不自动改
        if (_state.value.mealSlotTouched) return   // 用户手动碰过则锁定
        val name = _state.value.name
        val slots = if (name.isBlank()) emptyList() else MealSlotMatcher.defaultSlotsFor(name)
        _state.update { it.copy(mealSlots = slots, mealSlotPrefilled = slots.isNotEmpty()) }
    }

    /** 切换某餐次选中(手动碰过则锁定，不再自动预选)。[AI生成] v28 */
    fun toggleMealSlot(slot: MealSlot) {
        _state.update {
            val next = if (slot in it.mealSlots) it.mealSlots - slot else it.mealSlots + slot
            it.copy(mealSlots = next, mealSlotTouched = true, mealSlotPrefilled = false)
        }
    }

    /**
     * 新建模式下按菜名智能预选烹饪方式(未手动碰过才覆盖)。[AI生成] AUTOGEN-UNIFY STEP-AU-5.5b
     *
     * 只预选**烹饪方式字典实际存在**的项(推断结果与 availableCookingMethods 名交集——18 个推断字中
     * 仅 9 字在预设字典，「拌」不在〔字典名是"凉拌"〕、烧/熘/焗/烩/涮/煲/炝/熬 不在)，无交集不预选不提示；
     * **整组替换**(对齐 updateMealSlotPreselect 范式·禁止追加——连续改菜名会累积[蒸,烧])，替换自身不置 touched。
     */
    private fun updateCookingMethodPreselect() {
        if (_state.value.editingId != null) return // 编辑既有菜不自动改
        if (_state.value.cookingMethodTouched || _state.value.cookingMethodImported) return // [AI修改] STEP-L4-5.6：用户手动碰过或导入预填都锁定(INV-L4-09 双锁)。
        // [AI生成] 终审 S-4：烹饪方式字典未就绪(慢机)时显式跳过——下一次菜名变化防抖会重推(自然补偿)，
        //   不在空字典上做无效推断。
        if (_state.value.availableCookingMethods.isEmpty()) return
        val name = _state.value.name
        val dictNames = _state.value.availableCookingMethods.map { it.name }.toSet()
        val slots = if (name.isBlank()) emptyList()
        else com.sxdbsm.cookbook.domain.CookingMethodInferrer.inferFromName(name).filter { it in dictNames }
        // [AI生成] 推断结果与当前已选完全一致时不重复写(去重·防无谓重组)，且提示行状态跟随实际预选
        if (slots == _state.value.cookingMethodNames) {
            if (_state.value.cookingMethodPrefilled != slots.isNotEmpty()) {
                _state.update { it.copy(cookingMethodPrefilled = slots.isNotEmpty()) }
            }
            return
        }
        _state.update {
            it.copy(
                cookingMethodNames = slots,
                cookingMethodName = slots.firstOrNull(),
                cookingMethodId = null,
                cookingMethodPrefilled = slots.isNotEmpty(),
            )
        }
    }

    /**
     * 按菜名推演并自动加入食材：库内解析真实食材、库外用占位(负 id)加入并在名后标"待自建"。[AI生成] 菜名推食材
     *
     * 只加不删（用户可能已调过克数）；挂起期间以最新 state 去重后一次性写回，避免竞态。
     */
    private suspend fun autoAddFromName() {
        if (_state.value.editingId != null) return
        val dishName = _state.value.name
        if (dishName.isBlank() || cachedIngredientNames.isEmpty()) return
        unitsReady.await() // [AI生成] 等单位字典就绪再取"克"单位，避免默认克数落到错单位("100.0个"根因)。
        val existingNames = _state.value.ingredients.map { it.ingredient.name.trim() }.toSet()
        val methods = _state.value.availableCookingMethods.map { it.name } // 烹饪方式字典作额外停用词
        val cands = com.sxdbsm.cookbook.domain.DishNameIngredientGuesser
            .guessDetailed(dishName, cachedIngredientNames, methods)
            .filter { it.name !in existingNames }
        if (cands.isEmpty()) return
        // [AI生成] 菜名推演命中的食材名集合——自动标为主料
        val guessedNames = cands.map { it.name }.toSet()
        val gram = gramUnit()
        val toAdd = mutableListOf<DishIngredient>()
        val pendingNames = mutableListOf<String>()
        for (c in cands) {
            if (c.inLibrary) {
                // [AI修改] 归一匹配：guesser 候选名已去空格，库名可能含内部空格(老库/手输)，按去空格名比对防漏配→误建。
                val ing = runCatching { ingredientRepo.search(c.name) }.getOrDefault(emptyList())
                    .firstOrNull { it.name.replace(" ", "").trim() == c.name } ?: continue
                toAdd += buildAutoDishIngredient(ing, gram, guessedNames)
            } else {
                // 库外候选：占位负 id + 标记待自建；保存时经统一自动入库管线(ensureCreated)换真 id。
                val placeholder = Ingredient(id = pendingIdSeq--, name = c.name)
                toAdd += buildAutoDishIngredient(placeholder, gram, guessedNames)
                pendingNames += c.name
            }
        }
        if (toAdd.isEmpty()) return
        _state.update { s ->
            val curNames = s.ingredients.map { it.ingredient.name.trim() }.toSet()
            val add = toAdd.filter { it.ingredient.name.trim() !in curNames } // 挂起期间可能已被别处加入，再去重
            if (add.isEmpty()) return@update s
            val addedNameSet = add.map { it.ingredient.name.trim() }.toSet()
            val addedPending = pendingNames.filter { it in addedNameSet }
            s.copy(
                ingredients = s.ingredients + add,
                autoAddMessage = autoAddSnackbar(add.size, addedPending),
                autoAddSerial = s.autoAddSerial + 1,
            )
        }
    }

    /** 构造一条自动加入的食材项(默认克数按调料/普通区分，菜名命中的自动标主料)。[AI修改] */
    private fun buildAutoDishIngredient(ing: Ingredient, gram: MeasurementUnit?, guessedNames: Set<String>): DishIngredient {
        val defaultGram = com.sxdbsm.cookbook.domain.SeasoningDefaults
            .defaultGramFor(ing.name, ing.id in seasoningIds).toDouble()
        return DishIngredient(
            ingredient = ing,
            isMain = ing.name in guessedNames, // [AI修改] 菜名推演命中→自动标主料
            quantity = defaultGram,
            // [AI修改] 默认克数必配"克"单位；不再落到食材计件默认单位(ing.defaultUnitId=个/只)——那会把"100克"显成"100个"、
            // 且营养按错单位折算(bug根因)。此路径 gram 恒非空(autoAddFromName 已 await 单位就绪)；
            // 极端兜底为 null 单位时营养由 Nutrition.resolveGrams 的 PIECE_QUANTITY_MAX 按克折算兜住，不放大。
            unitName = gram?.name ?: "g",
            unitId = gram?.id,
        )
    }

    /** 自动加入的 Snackbar 文案(含待自建说明)。[AI生成] 守文案准则：说人话·动词开头 */
    private fun autoAddSnackbar(count: Int, pending: List<String>): String = when {
        pending.isEmpty() -> "已按菜名添加 $count 项食材，多余的可删掉"
        pending.size == 1 -> "已添加 $count 项，「${pending.first()}」将在保存时加入食材库"
        else -> "已添加 $count 项，其中「${pending.first()}」等 ${pending.size} 味将在保存时加入食材库"
    }
    fun setCookingMethodInput(v: String) {
        addCookingMethod(v)
    }
    fun selectCookingMethod(method: CookingMethod) {
        addCookingMethod(method.name)
    }
    fun clearCookingMethod() {
        // [AI修改] 兼容旧调用：清空全部烹饪方式。
        // [AI生成] AUTOGEN-UNIFY STEP-AU-5.5a：手动触碰任一烹饪方式入口→锁定，不再自动预选(同构 mealSlotTouched)。
        _state.value = _state.value.copy(
            cookingMethodId = null, cookingMethodName = null, cookingMethodInput = "", cookingMethodNames = emptyList(),
            cookingMethodTouched = true, cookingMethodPrefilled = false,
        )
    }
    /**
     * 加入一个烹饪方式。[AI修改] STEP-L4-5.5 加参支持导入预填静默加入。
     *
     * @param markUserTouched 导入预填传 false(不算用户触碰·不锁推演——imported 锁由 applyPrefill 置)。[AI生成] STEP-L4-5.5
     */
    fun addCookingMethod(name: String, markUserTouched: Boolean = true) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val next = (_state.value.cookingMethodNames + trimmed).distinct()
        // [AI生成] AUTOGEN-UNIFY STEP-AU-5.5a：手动加做法→锁定(自动预选整组替换不走此入口·不置 touched)。
        _state.value = _state.value.copy(
            cookingMethodNames = next,
            cookingMethodName = next.firstOrNull(),
            cookingMethodInput = "",
            cookingMethodId = null,
            cookingMethodTouched = markUserTouched || _state.value.cookingMethodTouched, // [AI修改] STEP-L4-5.5：false 时不回落覆盖已锁定态。
            cookingMethodPrefilled = false,
        ) // [AI生成] 支持多烹饪方式；保存时统一按名称创建/复用字典并写关联表。
    }
    fun removeCookingMethod(name: String) {
        val next = _state.value.cookingMethodNames.filterNot { it == name }
        _state.value = _state.value.copy(
            cookingMethodNames = next,
            cookingMethodName = next.firstOrNull(),
            cookingMethodInput = "",
            cookingMethodId = null,
            cookingMethodTouched = true,
            cookingMethodPrefilled = false,
        )
    }
    fun setCuisine(v: String) { _state.value = _state.value.copy(cuisine = v) } // [AI生成] 菜系选择(再选同一个可清空)
    fun setSpecialNote(v: String) { _state.value = _state.value.copy(specialNote = v) }
    fun setDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun setImagePath(v: String) { _state.value = _state.value.copy(imagePath = v) } // [AI生成] 保存最多 3 张菜品图片 URI。
    fun setImages(imagePath: String, thumbnailPath: String) {
        _state.value = _state.value.copy(imagePath = imagePath, thumbnailPath = thumbnailPath)
    } // [AI生成] 原图和缩略图成对保存，列表默认读取缩略图。
    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (_state.value.tags.contains(trimmed)) return
        _state.value = _state.value.copy(tags = _state.value.tags + trimmed)
    }
    fun removeTag(name: String) {
        _state.value = _state.value.copy(tags = _state.value.tags.filterNot { it == name })
    }

    /** T3：输入的新标签——加入本菜 + 存进标签库(下次可选)。[AI生成] */
    fun saveAndAddTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        addTag(trimmed)
        viewModelScope.launch {
            dishRepo.createDishTag(trimmed)
            _state.update { it.copy(availableTags = dishRepo.listDishTags()) }
        }
    }

    /** T3：从标签库删除(仅自建)；若本菜已选该标签也同步移除，避免保存时被重新入库"复活"。[AI生成] */
    fun deleteTagFromLibrary(id: Long) {
        val name = _state.value.availableTags.firstOrNull { it.id == id }?.name
        if (name != null && name in _state.value.tags) removeTag(name)
        viewModelScope.launch {
            dishRepo.deleteDishTag(id)
            _state.update { it.copy(availableTags = dishRepo.listDishTags()) }
        }
    }

    /** T4：输入的新烹饪方式——加入本菜 + 存进烹饪库(下次可选)。[AI生成] */
    fun saveAndAddCookingMethod(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        addCookingMethod(trimmed)
        viewModelScope.launch {
            dishRepo.ensureCookingMethod(trimmed)
            _state.update { it.copy(availableCookingMethods = dishRepo.listCookingMethods()) }
        }
    }

    /** T4：从烹饪库删除(仅自建)；若本菜已选该方式也同步移除，避免保存时被重新入库"复活"。[AI生成] */
    fun deleteCookingMethodFromLibrary(id: Long) {
        val name = _state.value.availableCookingMethods.firstOrNull { it.id == id }?.name
        if (name != null && name in _state.value.cookingMethodNames) removeCookingMethod(name)
        viewModelScope.launch {
            dishRepo.deleteCookingMethod(id)
            _state.update { it.copy(availableCookingMethods = dishRepo.listCookingMethods()) }
        }
    }

    /**
     * 添加食材到当前菜品。[AI修改]
     */
    fun addIngredient(ingredient: Ingredient, quantity: Double? = null, isMain: Boolean = false) {
        if (_state.value.ingredients.any { it.ingredient.id == ingredient.id }) return
        // [AI修改] #55：新加食材默认剂量(克)，用户可用 −N+ 调整(±5，最小0)。配料组套用带来的克数优先(quantity)。
        // [AI修改] 调料按正常每菜用量给默认(盐3g/酱油10g…)，非调料按食物大类给经验默认(见 SeasoningDefaults)。
        // [AI修改] 此为同步路径(无 await)，units 未加载时 gram 可能为 null → unitId=null，营养由
        //          Nutrition.resolveGrams 的 PIECE_QUANTITY_MAX 按克折算兜住(不放大)；正常时序 units 早已就绪。
        val gram = gramUnit()
        val defaultGram = com.sxdbsm.cookbook.domain.SeasoningDefaults
            .defaultGramFor(ingredient.name, ingredient.id in seasoningIds).toDouble()
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients + DishIngredient(
                ingredient = ingredient,
                isMain = isMain, // [AI修改] 主料分级：由调用方按入口(主料组/其他食材组)决定，默认 false(非主料)
                quantity = quantity ?: defaultGram,
                // [AI修改] 默认克数必配"克"单位；不再落到 ingredient.defaultUnitId(个/只)——避免"100克"显成"100个"、营养算错(bug根因)。gram 为 null 时留空单位由营养层兜。
                unitName = gram?.name ?: "g",
                unitId = gram?.id,
            ),
        )
    }

    /** 预填新建菜品(搜索"点此新建"/食材页"组成菜品"/自由搭配/分享链接导入 四入口统一)：菜名+含量纲食材+多做法+步骤图+封面+描述一次入表单，并以此为"无改动基线"。[AI修改] STEP-L4-5.2 整体化。 */
    fun applyPrefill(prefill: NewDishPrefill) {
        AppLogger.d(
            TAG,
            "apply_prefill ingredient_count=${prefill.ingredients.size} step_count=${prefill.steps.size} " +
                "method_count=${prefill.cookingMethodNames.size} has_cover=${prefill.imagePath.isNotBlank()} from_link=${prefill.linkId != null}",
        ) // [AI生成] 诊断日志：只记计数/布尔，禁记菜名/URL(Y-04 日志红线精神)。
        if (prefill.name.isNotBlank()) _state.value = _state.value.copy(name = prefill.name)
        prefill.ingredients.forEach { addPrefilledIngredient(it) }
        // [AI生成] STEP-L4-5.10+DP-P1-13：做法互斥——多值非空忽略单值；预填不算用户触碰(markUserTouched=false)。
        val methods = if (prefill.cookingMethodNames.isNotEmpty()) prefill.cookingMethodNames else {
            if (prefill.cookingMethodName.isNotBlank()) listOf(prefill.cookingMethodName) else emptyList()
        }
        methods.forEach { addCookingMethod(it, markUserTouched = false) }
        if (prefill.steps.isNotEmpty()) {
            // [AI生成] STEP-L4-5.3：步骤+过程图(单图经 encodeImagePaths 包装——单元素 join 无分隔符，与图片选择器口径一致)。
            _state.value = _state.value.copy(
                steps = prefill.steps.mapIndexed { i, s ->
                    DishStep(
                        sortOrder = i,
                        text = s.text,
                        imagePath = if (s.imagePath.isNotBlank()) encodeImagePaths(listOf(s.imagePath)) else "",
                        thumbnailPath = s.thumbnailPath,
                    )
                },
            )
        }
        if (prefill.imagePath.isNotBlank()) {
            setImages(encodeImagePaths(listOf(prefill.imagePath)), prefill.thumbnailPath) // [AI生成] STEP-L4-5.2：封面成对(同 encodeImagePaths 口径)。
        }
        if (prefill.description.isNotBlank()) _state.value = _state.value.copy(description = prefill.description)
        // [AI生成] STEP-L4-5.7：导入态置位(有做法才置 imported——空做法预填不禁菜名推演)与来源标签。
        val imported = methods.isNotEmpty()
        _state.value = _state.value.copy(
            cookingMethodImported = imported,
            ingredientSourceTag = prefill.sourceTag,
        )
        prefillLinkId = prefill.linkId
        if (prefill.ingredients.isNotEmpty()) {
            // [AI生成] STEP-L4-5.10：一次性反馈与既有 autoAddSerial 成对同一 copy(ARCH-06——只换 message 不递增 serial 则 Snackbar 永不显示)。
            val pending = prefill.ingredients.filter { it.ingredient.id <= 0 }.map { it.ingredient.name }
            // [AI修改] 审核对齐交互规范 C.2 冻结文案(2026-09-04)：无新食材「已导入 N 项食材」/有新食材后半句逐字复用既有句式。
            val msg = if (pending.isEmpty()) "已导入 ${prefill.ingredients.size} 项食材"
            else if (pending.size == 1) "已导入 ${prefill.ingredients.size} 项，其中「${pending.first()}」将在保存时加入食材库"
            else "已导入 ${prefill.ingredients.size} 项，其中「${pending.first()}」等 ${pending.size} 味将在保存时加入食材库"
            _state.update { s -> s.copy(autoAddMessage = msg, autoAddSerial = s.autoAddSerial + 1) }
        }
        updateMealSlotPreselect() // [AI生成] v28：预填菜名后按名预选餐次
        updateCookingMethodPreselect() // [AI生成] AUTOGEN-UNIFY S-3+STEP-L4-5.6：imported/touched 已锁则内部自 return。
        markBaseline() // [AI生成] GC-27：预填态成为基线(返回不误弹放弃)。
    }

    /** 导入/预填食材直入清单(绕过 addIngredient 强制克单位——量纲住 DishIngredient 由上游带全；仅完全无量纲的旧生产点项兜底默认克数)。[AI生成] STEP-L4-5.2 */
    private fun addPrefilledIngredient(di: DishIngredient) {
        if (_state.value.ingredients.any { it.ingredient.id == di.ingredient.id }) return
        val filled = if (di.quantity == null && di.unitId == null && di.unitName.isBlank()) {
            // 旧生产点(组成菜品/自由搭配)无量纲项：默认克数+克单位兜底(对齐 addIngredient 语义)。
            val gram = gramUnit()
            val defaultGram = com.sxdbsm.cookbook.domain.SeasoningDefaults
                .defaultGramFor(di.ingredient.name, di.ingredient.id in seasoningIds).toDouble()
            di.copy(quantity = defaultGram, unitName = gram?.name ?: "g", unitId = gram?.id)
        } else {
            di // 链接导入项(含适量→quantity=null)原样保留，用户可在表单改。
        }
        _state.value = _state.value.copy(ingredients = _state.value.ingredients + filled)
    }

    // ========== B5 常用配料组 ==========

    /** 加载可用配料组(预设+自建)。[AI生成] B5 */
    fun loadIngredientGroups() {
        viewModelScope.launch {
            runCatching { ingredientGroupRepo.listGroups() }
                .onSuccess { _state.value = _state.value.copy(ingredientGroups = it) }
                .onFailure { AppLogger.d(TAG, "ingredient_groups_load_failed error_type=${it.javaClass.simpleName}") }
        }
    }

    /** 套用配料组：把组内食材(按名解析/兜底建)加入食材清单(已存在的跳过)。[AI修改] B5 + 主料分级 */
    fun applyIngredientGroup(group: com.sxdbsm.cookbook.domain.model.IngredientGroup, asMain: Boolean = false) {
        viewModelScope.launch {
            group.items.forEach { item ->
                // [AI生成] AUTOGEN-UNIFY STEP-AU-5.2：改走统一自动入库管线(归一/分类/营养预估/food_group 一次做齐)·
                //   source="user" 保住删除/回收站/备份导出三处门控(架构审 AF-AU-01)；ctx() 在 runCatching 内层
                //   (防字典加载失败崩整次套用·对齐原裸调静默跳过语义)+失败留痕一行。
                val id = runCatching { autoGen.ensureCreated(item.name, autoGenCtx(), source = "user") }
                    .onFailure { AppLogger.d(TAG, "apply_group_ensure_failed error_type=${it.javaClass.simpleName}") }
                    .getOrNull() ?: return@forEach
                addIngredient(Ingredient(id = id, name = item.name), quantity = item.quantity, isMain = asMain) // [AI修改] 主料分级：按入口(主料组/其他食材组)决定
            }
        }
    }

    /** 按编辑器给定的食材项(名+克数)创建配料组(需求2：从食材库真实选+克数)。[AI生成] */
    fun createIngredientGroup(name: String, items: List<com.sxdbsm.cookbook.domain.model.IngredientGroupItem>) {
        val clean = items.map { it.copy(name = it.name.trim()) }.filter { it.name.isNotBlank() }
        if (name.isBlank() || clean.isEmpty()) return
        viewModelScope.launch {
            runCatching { ingredientGroupRepo.createGroup(name, clean) }
                .onSuccess { loadIngredientGroups() }
                .onFailure { AppLogger.d(TAG, "ingredient_group_create_failed error_type=${it.javaClass.simpleName}") }
        }
    }

    /** 删除自建配料组。[AI生成] B5 */
    fun deleteIngredientGroup(id: Long) {
        viewModelScope.launch {
            runCatching { ingredientGroupRepo.deleteGroup(id) }
                .onSuccess { loadIngredientGroups() }
                .onFailure { AppLogger.d(TAG, "ingredient_group_delete_failed error_type=${it.javaClass.simpleName}") }
        }
    }

    /** 调整某食材克数(±，最小0)。[AI生成] #55 剂量 −N+，步进 5g。 */
    fun changeIngredientGrams(ingredientId: Long, delta: Int) {
        val cur = _state.value.ingredients.firstOrNull { it.ingredient.id == ingredientId }?.quantity?.toInt() ?: DEFAULT_GRAMS
        setIngredientGrams(ingredientId, (cur + delta).coerceAtLeast(0))
    }

    /** 直接设置某食材克数(大跨度改用量时点数值输入,免狂点±)。[AI生成] */
    fun setIngredientGrams(ingredientId: Long, grams: Int) {
        val gram = gramUnit()
        val g = grams.coerceAtLeast(0)
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.map {
                if (it.ingredient.id == ingredientId) {
                    it.copy(quantity = g.toDouble(), unitName = gram?.name ?: it.unitName, unitId = gram?.id ?: it.unitId)
                } else {
                    it
                }
            },
        )
    }

    /** 取"克"单位(用于剂量)。[AI修改] 单位已英文化(克→g)，兼容新(g)/老库(克)；state 未含时兜底缓存值(防时序 race)。 */
    private fun gramUnit(): com.sxdbsm.cookbook.domain.model.MeasurementUnit? =
        _state.value.availableUnits.firstOrNull { it.name == "g" || it.name == "克" } ?: cachedGramUnit

    /**
     * 切换某个食材是否为主料。[AI修改]
     */
    fun toggleMain(ingredientId: Long) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.map {
                if (it.ingredient.id == ingredientId) it.copy(isMain = !it.isMain) else it
            },
            mainMissingHint = false, // [AI生成] 用户手动切换主料标→即时清除保存校验提示。
        )
    }

    fun removeIngredient(ingredientId: Long) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.filterNot { it.ingredient.id == ingredientId },
        )
    }

    /** 某食材是否属于调料(调味品/油脂类分类)。[AI生成] 主料分级：供 UI 显示调料 chip。 */
    fun isSeasoningIngredient(ingredientId: Long): Boolean = ingredientId in seasoningIds

    /**
     * 新增一个操作步骤。[AI生成]
     */
    fun addStep() {
        val nextOrder = _state.value.steps.size
        _state.value = _state.value.copy(
            steps = _state.value.steps + DishStep(sortOrder = nextOrder),
        )
    }

    /**
     * 修改某一步的文字说明。[AI生成]
     */
    fun updateStepText(index: Int, text: String) {
        _state.value = _state.value.copy(
            steps = _state.value.steps.mapIndexed { i, step ->
                if (i == index) step.copy(text = text) else step
            },
        )
    }

    /**
     * 修改某一步的过程图片。[AI生成]
     */
    fun updateStepImages(index: Int, imagePath: String, thumbnailPath: String) {
        _state.value = _state.value.copy(
            steps = _state.value.steps.mapIndexed { i, step ->
                if (i == index) step.copy(imagePath = imagePath, thumbnailPath = thumbnailPath) else step
            },
        )
    }

    /**
     * 上移/下移某个操作步骤并重排序号。[AI生成]
     *
     * @param toStart true=上移，false=下移；越界不动。
     */
    fun moveStep(index: Int, toStart: Boolean) {
        val steps = _state.value.steps
        val target = if (toStart) index - 1 else index + 1
        if (index !in steps.indices || target !in steps.indices) return
        val reordered = steps.toMutableList().apply {
            val tmp = this[index]; this[index] = this[target]; this[target] = tmp
        }.mapIndexed { i, step -> step.copy(sortOrder = i) }
        _state.value = _state.value.copy(steps = reordered)
    }

    /**
     * 删除某个操作步骤并重排序号。[AI生成]
     */
    fun removeStep(index: Int) {
        _state.value = _state.value.copy(
            steps = _state.value.steps
                .filterIndexed { i, _ -> i != index }
                .mapIndexed { i, step -> step.copy(sortOrder = i) },
        )
    }

    // ========== #2 操作步骤模板 ==========

    /** 加载可用步骤模板(预设+自建)，供"选择步骤"弹层展示。[AI生成] */
    fun loadStepTemplates() {
        viewModelScope.launch {
            runCatching { stepTemplateRepo.listTemplates() }
                .onSuccess { _state.value = _state.value.copy(stepTemplates = it) }
                .onFailure { AppLogger.d(TAG, "step_templates_load_failed error_type=${it.javaClass.simpleName}") }
        }
    }

    /**
     * 套用某个步骤模板。[AI修改]
     *
     * @param multiStep 多步插入开关：
     *  - true：模板每一步**分别追加成独立步骤**（接在现有步骤末尾）；无步骤则新增相应多条。
     *  - false（默认行为）：模板内容合并**插入当前定位的那一步**（targetIndex，无定位则末步；已有文字换行追加）；
     *    无步骤则新增一条承载全部模板文字，不展开多条。
     */
    fun applyStepTemplate(
        template: com.sxdbsm.cookbook.domain.model.StepTemplate,
        targetIndex: Int?,
        multiStep: Boolean,
    ) {
        val texts = template.steps.map { it.trim() }.filter { it.isNotBlank() }
        if (texts.isEmpty()) return
        val steps = _state.value.steps
        if (multiStep) {
            // 每步独立成条，追加到末尾（无步骤则等同新增相应多条）。
            val appended = texts.mapIndexed { i, t -> DishStep(sortOrder = steps.size + i, text = t) }
            _state.value = _state.value.copy(steps = steps + appended)
            return
        }
        val merged = texts.joinToString("\n")
        if (steps.isEmpty()) {
            _state.value = _state.value.copy(steps = listOf(DishStep(sortOrder = 0, text = merged)))
            return
        }
        val idx = targetIndex?.takeIf { it in steps.indices } ?: steps.lastIndex
        _state.value = _state.value.copy(
            steps = steps.mapIndexed { i, s ->
                if (i == idx) s.copy(text = if (s.text.isBlank()) merged else s.text.trimEnd() + "\n" + merged) else s
            },
        )
    }

    /** 按编辑器给定的步骤文字列表创建模板(bug2：弹层"+添加"编辑后保存)。[AI生成] */
    fun createStepTemplate(name: String, texts: List<String>) {
        val clean = texts.map { it.trim() }.filter { it.isNotBlank() }
        if (name.isBlank() || clean.isEmpty()) return
        viewModelScope.launch {
            runCatching { stepTemplateRepo.createTemplate(name, clean) }
                .onSuccess { loadStepTemplates() }
                .onFailure { AppLogger.d(TAG, "step_template_create_failed error_type=${it.javaClass.simpleName}") }
        }
    }


    /** 删除自建步骤模板。[AI生成] */
    fun deleteStepTemplate(id: Long) {
        viewModelScope.launch {
            runCatching { stepTemplateRepo.deleteTemplate(id) }
                .onSuccess { loadStepTemplates() }
                .onFailure { AppLogger.d(TAG, "step_template_delete_failed error_type=${it.javaClass.simpleName}") }
        }
    }

    /**
     * 保存当前菜品表单。[AI修改]
     */
    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.loading) return
        // [AI生成] 主料校验：有食材但0主料 → 设内联提示(非阻断)，用户标任一主料后自动消失。
        if (s.ingredients.isNotEmpty() && s.ingredients.none { it.isMain }) {
            _state.value = _state.value.copy(mainMissingHint = true)
        } else {
            _state.value = _state.value.copy(mainMissingHint = false)
        }
        viewModelScope.launch {
            // [AI修改] D10：saving 标志用最新 _state.value 写回，不用启动前捕获的 s 快照。
            _state.value = _state.value.copy(saving = true)
            runCatching {
                // [AI生成] 待自建食材(占位负 id)：保存前经统一自动入库管线换真 id(归一/分类/营养预估/food_group 一次做齐)。
                //   [AI修改] STEP-L4-5.8：导入食材 source=ingredientSourceTag(默认"user"·链接导入"link"走统一管线归一/分类/营养)——
                //   默认"user"保住删除/回收站/备份导出门控；创建失败丢弃(不写坏 FK)不阻断。AUTOGEN-UNIFY STEP-AU-5.3/5.4。
                //   [AI修改] 同 id 去重优先保留"非占位"(用户手动加/已调克数)那条：占位按名解析可能撞上清单已有真实食材，
                //   若不去重，saveDish 的 INSERT OR REPLACE(uq_dish_ingredient)会用占位默认克数覆盖用户已调用量。
                val byId = LinkedHashMap<Long, DishIngredient>()
                val fromPlaceholder = HashSet<Long>()
                s.ingredients.forEach { di ->
                    val wasPlaceholder = di.ingredient.id <= 0
                    val resolved = if (!wasPlaceholder) {
                        di
                    } else {
                        val realId = runCatching { autoGen.ensureCreated(di.ingredient.name.trim(), autoGenCtx(), source = s.ingredientSourceTag) }
                            .onFailure { AppLogger.d(TAG, "save_ensure_failed error_type=${it.javaClass.simpleName}") }
                            .getOrNull() ?: return@forEach
                        di.copy(ingredient = di.ingredient.copy(id = realId))
                    }
                    val rid = resolved.ingredient.id
                    val prev = byId[rid]
                    when {
                        prev == null -> { byId[rid] = resolved; if (wasPlaceholder) fromPlaceholder.add(rid) }
                        // 之前存的是占位、当前是用户显式项 → 用用户项(含其克数)替换；其余保留先存的
                        rid in fromPlaceholder && !wasPlaceholder -> { byId[rid] = resolved; fromPlaceholder.remove(rid) }
                    }
                }
                // [AI修改] 终审 R-3 收口：链接菜谱「适量」项(quantity=null·INV-L4-18)保存时折默认克数——
                //   否则落库 NULL 使该食材营养永久不贡献(resolveGrams 对 null 返 null·今日卡热量系统性偏低)。
                //   编辑页显示「适量」占位(诚实)与详情页显示估算克数(营养完整)各司其职；用户调过的量非 null 不动。
                val gramUnitForSave = gramUnit()
                val resolvedIngredients = byId.values.map { di ->
                    if (di.quantity == null) {
                        di.copy(
                            quantity = com.sxdbsm.cookbook.domain.SeasoningDefaults
                                .defaultGramFor(di.ingredient.name, di.ingredient.id in seasoningIds).toDouble(),
                            unitName = di.unitName.ifBlank { gramUnitForSave?.name ?: "g" },
                            unitId = di.unitId ?: gramUnitForSave?.id,
                        )
                    } else {
                        di
                    }
                }
                dishRepo.saveDish(
                    id = s.editingId ?: 0L,
                    name = s.name.trim(),
                    cookingMethodId = s.cookingMethodId,
                    cookingMethodNames = s.cookingMethodNames,
                    specialNote = s.specialNote,
                    description = s.description,
                    imagePath = s.imagePath,
                    thumbnailPath = s.thumbnailPath,
                    tagNames = s.tags,
                    ingredients = resolvedIngredients,
                    steps = s.steps,
                    cuisine = s.cuisine,
                    mealSlotCodes = s.mealSlots.map { it.code }, // [AI生成] v28：适合餐次(空则 repo 按名 Matcher 兜底)
                )
            }.onSuccess { savedId ->
                AppLogger.event(
                    "new_dish_save",
                    mapOf(
                        "dishId" to savedId,
                        "mode" to if (s.editingId == null) "create" else "edit",
                        "ingredientCount" to s.ingredients.size,
                        "tagCount" to s.tags.size,
                        "imageCount" to s.imagePath.split("|").filter { it.isNotBlank() }.size,
                        "stepCount" to s.steps.count { it.text.isNotBlank() || it.imagePath.isNotBlank() },
                        "success" to true,
                    ),
                ) // [AI生成] 内测埋点：记录菜品保存成功摘要。
                // [AI生成] STEP-L4-5.9：链接导入回写 share_link.dish_id(WHERE dish_id IS NULL 幂等·DP-P1-4)；失败只留痕不阻断保存结果。
                prefillLinkId?.let { linkId ->
                    runCatching { shareLinkRepo.markSaved(linkId, savedId) }
                        .onFailure { AppLogger.d(TAG, "link_save_mark_failed linkId=$linkId error_type=${it.javaClass.simpleName}") } // [AI生成] Y-04 日志红线：只记 linkId/错误类型，禁完整 URL/菜名。
                }
                _state.value = _state.value.copy(
                    saving = false,
                    done = true,
                    savedDishId = savedId,
                    cookingMethodId = null,
                    availableCookingMethods = dishRepo.listCookingMethods(),
                )
            }.onFailure { error ->
                AppLogger.e(TAG, "dish_save_failed error_type=${error.javaClass.simpleName}")
                AppLogger.event(
                    "new_dish_save",
                    mapOf(
                        "mode" to if (s.editingId == null) "create" else "edit",
                        "ingredientCount" to s.ingredients.size,
                        "tagCount" to s.tags.size,
                        "stepCount" to s.steps.count { it.text.isNotBlank() || it.imagePath.isNotBlank() },
                        "success" to false,
                        "errorType" to error.javaClass.simpleName,
                    ),
                ) // [AI生成] 内测埋点：记录菜品保存失败摘要。
                _state.value = _state.value.copy(saving = false, errorMessage = "保存菜品失败，请稍后重试")
            }
        }
    }
}
