package com.sxdbsm.cookbook.android.ui.newdish

import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
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

/**
 * 新建/编辑菜品页 UI 状态。[AI修改]
 *
 * 同一个状态类同时支持“新建菜品”和“编辑菜品”：`editingId == null` 表示新建。
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
    val steps: List<DishStep> = emptyList(), // [AI生成] 菜品操作步骤草稿，保存时随菜品事务一起落库。
    val specialNote: String = "",
    val description: String = "",
    val cuisine: String = "", // [AI生成] 菜系(可空)
    val imagePath: String = "",
    val thumbnailPath: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val editProbeToastMessage: String? = null,
    val editProbeToastSerial: Int = 0,
    val saving: Boolean = false,
    val done: Boolean = false,
    val savedDishId: Long? = null,

    val availableUnits: List<MeasurementUnit> = emptyList(), // [AI修改] 食材用量单位下拉列表。
    val stepTemplates: List<com.sxdbsm.cookbook.domain.model.StepTemplate> = emptyList(), // [AI生成] #2 "选择步骤"可套用的步骤模板(预设+自建)。
    val ingredientGroups: List<com.sxdbsm.cookbook.domain.model.IngredientGroup> = emptyList(), // [AI生成] B5 "配料组"可一键加入的常用食材组。
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
) : ViewModel() {

    private val _state = MutableStateFlow(NewDishUiState()) // [AI修改] 表单内部可变状态。
    val state: StateFlow<NewDishUiState> = _state.asStateFlow() // [AI修改] UI 只能观察，不能直接改。
    private var activeStartKey: String? = null // [AI生成] 记录当前路由参数，避免同一编辑页重复触发 start 清空表单。
    private var baselineSig: String? = null // [AI生成] 加载/新建时的表单内容签名，用于返回前判断是否有未保存改动。

    /** 表单可编辑内容的签名(只含用户内容，排除字典/瞬态标志)。[AI生成] */
    private fun NewDishUiState.contentSig(): String = listOf(
        name.trim(), tags.sorted().toString(), cookingMethodId, cookingMethodNames.sorted().toString(),
        cookingMethodInput.trim(), cuisine, specialNote.trim(), description.trim(), imagePath, thumbnailPath,
        ingredients.map { it.ingredient.id to it.quantity }.toString(),
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

    init {
        viewModelScope.launch {
            // [AI修改] 页面打开后加载计量单位字典，用于食材用量输入。
            // [AI修改] 先把挂起查询结果放到局部变量，再基于最新 state 合并，避免旧空表单快照覆盖编辑加载结果。
            val units = ingredientRepo.listMeasurementUnits()
            val cookingMethods = dishRepo.listCookingMethods()
            val tags = dishRepo.listDishTags() // [AI生成] T3：标签库
            _state.update { current ->
                AppLogger.d(TAG, "init dictionaries merged: currentEditId=${current.editingId} currentName=${current.name} units=${units.size} methods=${cookingMethods.size}")
                current.copy(
                    availableUnits = units,
                    availableCookingMethods = cookingMethods,
                    availableTags = tags,
                )
            }
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
            AppLogger.d(TAG, "skip duplicate start: key=$startKey loading=${current.loading} name=${current.name} error=${current.errorMessage}")
            return
        } // [AI修改] 同一路由参数重复进入时绝不重置表单，避免加载成功后被空状态覆盖。
        AppLogger.d(TAG, "start: key=$startKey currentName=${current.name} currentEditId=${current.editingId}")
        activeStartKey = startKey
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
            AppLogger.d(TAG, "loadForEdit begin: dishId=$dishId")
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
                        AppLogger.w(TAG, "loadForEdit empty: dishId=$dishId")
                        _state.value = _state.value.copy(
                            loading = false,
                            saving = false,
                            errorMessage = "未找到要编辑的菜品",
                            editProbeToastMessage = "编辑菜品 ID=$dishId 当前为空",
                            editProbeToastSerial = _state.value.editProbeToastSerial + 1,
                        ) // [AI生成] 避免编辑页标题正确但表单空白时没有任何提示。
                    } else {
                        AppLogger.d(TAG, "loadForEdit success: dishId=$dishId loadedId=${d.id} name=${d.name} tags=${d.tags.size} ingredients=${d.ingredients.size}")
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
                    AppLogger.e(TAG, "loadForEdit failed: dishId=$dishId", error)
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

    fun setName(v: String) { _state.value = _state.value.copy(name = v) }
    fun setCookingMethodInput(v: String) {
        addCookingMethod(v)
    }
    fun selectCookingMethod(method: CookingMethod) {
        addCookingMethod(method.name)
    }
    fun clearCookingMethod() {
        // [AI修改] 兼容旧调用：清空全部烹饪方式。
        _state.value = _state.value.copy(cookingMethodId = null, cookingMethodName = null, cookingMethodInput = "", cookingMethodNames = emptyList())
    }
    fun addCookingMethod(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val next = (_state.value.cookingMethodNames + trimmed).distinct()
        _state.value = _state.value.copy(
            cookingMethodNames = next,
            cookingMethodName = next.firstOrNull(),
            cookingMethodInput = "",
            cookingMethodId = null,
        ) // [AI生成] 支持多烹饪方式；保存时统一按名称创建/复用字典并写关联表。
    }
    fun removeCookingMethod(name: String) {
        val next = _state.value.cookingMethodNames.filterNot { it == name }
        _state.value = _state.value.copy(
            cookingMethodNames = next,
            cookingMethodName = next.firstOrNull(),
            cookingMethodInput = "",
            cookingMethodId = null,
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
    fun addIngredient(ingredient: Ingredient, quantity: Double? = null) {
        if (_state.value.ingredients.any { it.ingredient.id == ingredient.id }) return
        // [AI修改] #55：新加食材默认剂量 100 克(克为单位)，用户可用 −N+ 调整(±5，最小0)。
        // [AI修改] B5/需求2：配料组套用时带过来的克数优先(quantity)，否则默认 100g。
        val gram = gramUnit()
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients + DishIngredient(
                ingredient = ingredient,
                isMain = false, // [AI修改] 当前版本暂不暴露/保存“主料”语义，后续再扩展原料/调味料分类。
                quantity = quantity ?: DEFAULT_GRAMS.toDouble(),
                unitName = gram?.name ?: "克",
                unitId = gram?.id ?: ingredient.defaultUnitId,
            ),
        )
    }

    // ========== B5 常用配料组 ==========

    /** 加载可用配料组(预设+自建)。[AI生成] B5 */
    fun loadIngredientGroups() {
        viewModelScope.launch {
            runCatching { ingredientGroupRepo.listGroups() }
                .onSuccess { _state.value = _state.value.copy(ingredientGroups = it) }
                .onFailure { AppLogger.d(TAG, "load ingredient groups failed: ${it.message}") }
        }
    }

    /** 套用配料组：把组内食材(按名解析/兜底建)加入食材清单(已存在的跳过)。[AI生成] B5 */
    fun applyIngredientGroup(group: com.sxdbsm.cookbook.domain.model.IngredientGroup) {
        viewModelScope.launch {
            group.items.forEach { item ->
                val id = runCatching { ingredientRepo.createUserIngredient(item.name) }.getOrNull() ?: return@forEach
                addIngredient(Ingredient(id = id, name = item.name), quantity = item.quantity) // [AI修改] 需求2：带过来克数
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
                .onFailure { AppLogger.d(TAG, "create ingredient group failed: ${it.message}") }
        }
    }

    /** 删除自建配料组。[AI生成] B5 */
    fun deleteIngredientGroup(id: Long) {
        viewModelScope.launch {
            runCatching { ingredientGroupRepo.deleteGroup(id) }
                .onSuccess { loadIngredientGroups() }
                .onFailure { AppLogger.d(TAG, "delete ingredient group failed: ${it.message}") }
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

    /** 取"克"单位(用于剂量)。[AI修改] 单位已英文化(克→g)，兼容新(g)/老库(克)。 */
    private fun gramUnit(): com.sxdbsm.cookbook.domain.model.MeasurementUnit? =
        _state.value.availableUnits.firstOrNull { it.name == "g" || it.name == "克" }

    /**
     * 切换某个食材是否为主料。[AI修改]
     */
    fun toggleMain(ingredientId: Long) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.map {
                if (it.ingredient.id == ingredientId) it.copy(isMain = !it.isMain) else it
            },
        )
    }

    fun removeIngredient(ingredientId: Long) {
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients.filterNot { it.ingredient.id == ingredientId },
        )
    }

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
                .onFailure { AppLogger.d(TAG, "load step templates failed: ${it.message}") }
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
                .onFailure { AppLogger.d(TAG, "create step template failed: ${it.message}") }
        }
    }


    /** 删除自建步骤模板。[AI生成] */
    fun deleteStepTemplate(id: Long) {
        viewModelScope.launch {
            runCatching { stepTemplateRepo.deleteTemplate(id) }
                .onSuccess { loadStepTemplates() }
                .onFailure { AppLogger.d(TAG, "delete step template failed: ${it.message}") }
        }
    }

    /**
     * 保存当前菜品表单。[AI修改]
     */
    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.loading) return
        viewModelScope.launch {
            // [AI修改] D10：saving 标志用最新 _state.value 写回，不用启动前捕获的 s 快照。
            _state.value = _state.value.copy(saving = true)
            runCatching {
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
                    ingredients = s.ingredients,
                    steps = s.steps,
                    cuisine = s.cuisine,
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
                _state.value = _state.value.copy(
                    saving = false,
                    done = true,
                    savedDishId = savedId,
                    cookingMethodId = null,
                    availableCookingMethods = dishRepo.listCookingMethods(),
                )
            }.onFailure { error ->
                AppLogger.e(TAG, "save dish failed: editingId=${s.editingId}", error) // [AI生成] 保存失败时写入本地日志。
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
