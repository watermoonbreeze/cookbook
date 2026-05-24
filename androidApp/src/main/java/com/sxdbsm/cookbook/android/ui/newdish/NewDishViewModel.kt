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

data class NewDishUiState(
    val editingId: Long? = null,
    val name: String = "",
    val tags: List<String> = emptyList(),
    val cookingMethodId: Long? = null,
    val cookingMethodName: String? = null,
    val ingredients: List<DishIngredient> = emptyList(),
    val specialNote: String = "",
    val description: String = "",
    val imagePath: String = "",
    val saving: Boolean = false,
    val done: Boolean = false,

    val availableUnits: List<MeasurementUnit> = emptyList(),
)

class NewDishViewModel(
    private val dishRepo: DishRepository,
    private val ingredientRepo: IngredientRepository,
    @Suppress("unused") private val categoryRepo: FoodCategoryRepository,
    @Suppress("unused") private val pref: com.sxdbsm.cookbook.data.repository.PreferenceRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(NewDishUiState())
    val state: StateFlow<NewDishUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(availableUnits = ingredientRepo.listMeasurementUnits())
        }
    }

    fun loadForEdit(dishId: Long) {
        viewModelScope.launch {
            val d = dishRepo.getDishById(dishId) ?: return@launch
            _state.value = _state.value.copy(
                editingId = d.id,
                name = d.name,
                tags = d.tags,
                cookingMethodId = d.cookingMethodId,
                cookingMethodName = d.cookingMethodName,
                ingredients = d.ingredients,
                specialNote = d.specialNote,
                description = d.description,
                imagePath = d.imagePath,
            )
        }
    }

    /** 从其他菜品导入，回填字段并自动加 #复制 标签。 */
    fun importFrom(d: Dish) {
        _state.value = _state.value.copy(
            editingId = null,    // 始终新建
            name = d.name,
            tags = (d.tags + "#复制").distinct(),
            cookingMethodId = d.cookingMethodId,
            cookingMethodName = d.cookingMethodName,
            ingredients = d.ingredients,
            specialNote = d.specialNote,
            description = d.description,
            imagePath = d.imagePath,
        )
    }

    fun importFromDishId(dishId: Long) {
        viewModelScope.launch {
            dishRepo.getDishById(dishId)?.let { importFrom(it) }
        }
    }

    fun setName(v: String) { _state.value = _state.value.copy(name = v) }
    fun setSpecialNote(v: String) { _state.value = _state.value.copy(specialNote = v) }
    fun setDescription(v: String) { _state.value = _state.value.copy(description = v) }
    fun addTag(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        if (_state.value.tags.contains(trimmed)) return
        _state.value = _state.value.copy(tags = _state.value.tags + trimmed)
    }
    fun removeTag(name: String) {
        _state.value = _state.value.copy(tags = _state.value.tags.filterNot { it == name })
    }

    fun addIngredient(ingredient: Ingredient) {
        if (_state.value.ingredients.any { it.ingredient.id == ingredient.id }) return
        _state.value = _state.value.copy(
            ingredients = _state.value.ingredients + DishIngredient(
                ingredient = ingredient,
                isMain = true,
                unitName = ingredient.defaultUnitName,
                unitId = ingredient.defaultUnitId,
            ),
        )
    }

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

    fun save() {
        val s = _state.value
        if (s.name.isBlank()) return
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
            dishRepo.saveDish(
                id = s.editingId ?: 0L,
                name = s.name.trim(),
                cookingMethodId = s.cookingMethodId,
                specialNote = s.specialNote,
                description = s.description,
                imagePath = s.imagePath,
                tagNames = s.tags,
                ingredients = s.ingredients,
            )
            _state.value = _state.value.copy(saving = false, done = true)
        }
    }
}
