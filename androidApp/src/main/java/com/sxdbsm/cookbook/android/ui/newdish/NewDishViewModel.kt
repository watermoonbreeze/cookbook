package com.sxdbsm.cookbook.android.ui.newdish

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.domain.model.CookingMethod
import com.sxdbsm.cookbook.domain.model.Dish
import com.sxdbsm.cookbook.domain.model.DishIngredient
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.MeasurementUnit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    val ingredients: List<DishIngredient> = emptyList(),
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
    val thumbnailPath: String = "",
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val saving: Boolean = false,
    val done: Boolean = false,

    val availableUnits: List<MeasurementUnit> = emptyList(), // [AI修改] 食材用量单位下拉列表。
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
) : ViewModel() {

    private val _state = MutableStateFlow(NewDishUiState()) // [AI修改] 表单内部可变状态。
    val state: StateFlow<NewDishUiState> = _state.asStateFlow() // [AI修改] UI 只能观察，不能直接改。
    init {
        viewModelScope.launch {
            // [AI修改] 页面打开后加载计量单位字典，用于食材用量输入。
            _state.value = _state.value.copy(
                availableUnits = ingredientRepo.listMeasurementUnits(),
                availableCookingMethods = dishRepo.listCookingMethods(),
            )
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
        _state.value = NewDishUiState(
            editingId = editId,
            availableUnits = current.availableUnits,
            availableCookingMethods = current.availableCookingMethods,
            loading = editId != null || sourceId != null,
        ) // [AI生成] 保留字典数据，只清空表单草稿和保存完成标记。

        when {
            editId != null -> loadForEdit(editId)
            sourceId != null -> importFromDishId(sourceId)
        }
    }

    /**
     * 加载已有菜品进入编辑模式。[AI修改]
     */
    fun loadForEdit(dishId: Long) {
        viewModelScope.launch {
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
                        _state.value = _state.value.copy(
                            loading = false,
                            saving = false,
                            errorMessage = "未找到要编辑的菜品",
                        ) // [AI生成] 避免编辑页标题正确但表单空白时没有任何提示。
                    } else {
                        _state.value = _state.value.copy(
                            editingId = d.id,
                            name = d.name,
                            tags = d.tags,
                            cookingMethodId = d.cookingMethodId,
                            cookingMethodName = d.cookingMethodName,
                            cookingMethodInput = d.cookingMethodName.orEmpty(),
                            cookingMethodNames = d.cookingMethods.map { it.name }.ifEmpty { d.cookingMethodName?.let(::listOf).orEmpty() },
                            ingredients = d.ingredients,
                            specialNote = d.specialNote,
                            description = d.description,
                            imagePath = d.imagePath,
                            thumbnailPath = d.thumbnailPath,
                            loading = false,
                            errorMessage = null,
                            done = false,
                            saving = false,
                        )
                    }
                }
                .onFailure {
                    _state.value = _state.value.copy(
                        loading = false,
                        saving = false,
                        errorMessage = "加载菜品失败，请返回后重试",
                    )
                }
        }
    }

    /** 从其他菜品导入，回填字段并自动加 #复制 标签。[AI修改] */
    fun importFrom(d: Dish) {
        _state.value = _state.value.copy(
            editingId = null,    // 始终新建
            name = d.name,
            tags = (d.tags + "#复制").distinct(),
            cookingMethodId = d.cookingMethodId,
            cookingMethodName = d.cookingMethodName,
            cookingMethodInput = d.cookingMethodName.orEmpty(),
            cookingMethodNames = d.cookingMethods.map { it.name }.ifEmpty { d.cookingMethodName?.let(::listOf).orEmpty() },
            ingredients = d.ingredients,
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

    /**
     * 添加食材到当前菜品。[AI修改]
     */
    fun addIngredient(ingredient: Ingredient) {
        if (_state.value.ingredients.any { it.ingredient.id == ingredient.id }) return
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients + DishIngredient(
                ingredient = ingredient,
                isMain = false, // [AI修改] 当前版本暂不暴露/保存“主料”语义，后续再扩展原料/调味料分类。
                unitName = ingredient.defaultUnitName,
                unitId = ingredient.defaultUnitId,
            ),
        )
    }

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
     * 保存当前菜品表单。[AI修改]
     */
    fun save() {
        val s = _state.value
        if (s.name.isBlank() || s.loading) return
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
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
            )
            _state.value = _state.value.copy(
                saving = false,
                done = true,
                cookingMethodId = null,
                availableCookingMethods = dishRepo.listCookingMethods(),
            )
        }
    }
}
