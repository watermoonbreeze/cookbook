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
    val availableCookingMethods: List<CookingMethod> = emptyList(),
    val ingredients: List<DishIngredient> = emptyList(),
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
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
     * 加载已有菜品进入编辑模式。[AI修改]
     */
    fun loadForEdit(dishId: Long) {
        viewModelScope.launch {
            val d = dishRepo.getDishById(dishId) ?: return@launch
            _state.value = _state.value.copy(
                editingId = d.id,
                name = d.name,
                tags = d.tags,
                cookingMethodId = d.cookingMethodId,
                cookingMethodName = d.cookingMethodName,
                cookingMethodInput = d.cookingMethodName.orEmpty(),
                ingredients = d.ingredients,
                specialNote = d.specialNote,
                description = d.description,
                imagePath = d.imagePath,
            )
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
            ingredients = d.ingredients,
            specialNote = d.specialNote,
            description = d.description,
            imagePath = d.imagePath,
        )
    }

    /**
     * 通过 id 加载菜品并导入为新菜品草稿。[AI修改]
     */
    fun importFromDishId(dishId: Long) {
        viewModelScope.launch {
            dishRepo.getDishById(dishId)?.let { importFrom(it) }
        }
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v) }
    fun setCookingMethodInput(v: String) {
        // [AI生成] 手动输入时清空 id；保存时会按名称创建或复用字典项。
        _state.value = _state.value.copy(cookingMethodInput = v, cookingMethodId = null, cookingMethodName = v)
    }
    fun selectCookingMethod(method: CookingMethod) {
        // [AI生成] 下拉选择时同时保存 id 和展示文本。
        _state.value = _state.value.copy(
            cookingMethodId = method.id,
            cookingMethodName = method.name,
            cookingMethodInput = method.name,
        )
    }
    fun clearCookingMethod() {
        // [AI生成] 烹饪方式是可选字段，用户点 chip 旁的关闭图标时清空已选/已输入内容。
        _state.value = _state.value.copy(cookingMethodId = null, cookingMethodName = null, cookingMethodInput = "")
    }
    fun setSpecialNote(v: String) { _state.value = _state.value.copy(specialNote = v) }
    fun setDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun setImagePath(v: String) { _state.value = _state.value.copy(imagePath = v) } // [AI生成] 保存最多 3 张菜品图片 URI。
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
        if (s.name.isBlank()) return
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
            val cookingMethodId = s.cookingMethodId ?: dishRepo.ensureCookingMethod(s.cookingMethodInput) // [AI修改] 支持用户输入新的烹饪方式。
            dishRepo.saveDish(
                id = s.editingId ?: 0L,
                name = s.name.trim(),
                cookingMethodId = cookingMethodId,
                specialNote = s.specialNote,
                description = s.description,
                imagePath = s.imagePath,
                tagNames = s.tags,
                ingredients = s.ingredients,
            )
            _state.value = _state.value.copy(
                saving = false,
                done = true,
                cookingMethodId = cookingMethodId,
                availableCookingMethods = dishRepo.listCookingMethods(),
            )
        }
    }
}
