package com.sxdbsm.cookbook.android.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.FoodCategoryRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** 左侧分类节点（手风琴展开后的扁平化项） */
data class CategoryNode(
    val category: FoodCategory,
    val level: Int,    // 1 = 一级，2 = 二级
    val expanded: Boolean = false,
)

data class IngredientPickerUiState(
    val keyword: String = "",
    val tree: List<CategoryNode> = emptyList(),  // "全部" + 一级 + 展开的二级
    val selectedCategoryId: Long? = null,        // -1 表示"全部"
    val ingredients: List<Ingredient> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val selectedIngredients: List<Ingredient> = emptyList(),
    val excludeIngredientIds: Set<Long> = emptySet(),
    val creatingIngredient: Boolean = false,
    val createError: String? = null,
    val lastCreatedIngredientId: Long? = null,
)

class IngredientPickerViewModel(
    private val ingredientRepo: IngredientRepository,
    private val categoryRepo: FoodCategoryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(IngredientPickerUiState())
    val state: StateFlow<IngredientPickerUiState> = _state.asStateFlow()

    init { loadCategories(); loadAllIngredients() }

    fun configure(excludeIngredientIds: Set<Long>) {
        _state.value = _state.value.copy(
            excludeIngredientIds = excludeIngredientIds,
            selectedIds = _state.value.selectedIds - excludeIngredientIds,
            selectedIngredients = _state.value.selectedIngredients.filterNot { it.id in excludeIngredientIds },
            ingredients = _state.value.ingredients.filterNot { it.id in excludeIngredientIds },
        )
    }

    private fun loadCategories() {
        viewModelScope.launch {
            val tops = categoryRepo.listTopLevel()
            val tree = tops.map { CategoryNode(it, level = 1) }
            _state.value = _state.value.copy(tree = tree, selectedCategoryId = -1L /* 全部 */)
        }
    }

    private fun loadAllIngredients() {
        viewModelScope.launch {
            _state.value = _state.value.copy(ingredients = ingredientRepo.search("").withoutExcluded())
        }
    }

    fun setKeyword(kw: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                keyword = kw,
                ingredients = ingredientRepo.search(kw).withoutExcluded(),
            )
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                selectedCategoryId = -1L,
                ingredients = ingredientRepo.search("").withoutExcluded(),
            )
        }
    }

    fun toggleExpand(node: CategoryNode) {
        viewModelScope.launch {
            if (node.level != 1 || !node.category.hasChildren) return@launch
            val current = _state.value.tree
            val idx = current.indexOf(node)
            if (idx < 0) return@launch

            val newTree = current.toMutableList()
            if (node.expanded) {
                // 收起：移除该一级后面的所有子项
                val end = newTree.subList(idx + 1, newTree.size).indexOfFirst { it.level == 1 }
                val to = if (end < 0) newTree.size else idx + 1 + end
                newTree.subList(idx + 1, to).clear()
                newTree[idx] = node.copy(expanded = false)
            } else {
                // 展开：查二级并插入
                val children = categoryRepo.listChildren(node.category.id)
                newTree.add(idx + 1, *children.map { CategoryNode(it, level = 2) }.toTypedArray())
                newTree[idx] = node.copy(expanded = true)
            }
            _state.value = _state.value.copy(tree = newTree)
        }
    }

    fun selectCategory(node: CategoryNode) {
        viewModelScope.launch {
            val cat = node.category
            val crowdId = cat.crowdTypeId
            val ingredients = if (crowdId != null) ingredientRepo.listByCrowd(crowdId)
            else ingredientRepo.listByCategory(cat.id)
            _state.value = _state.value.copy(
                selectedCategoryId = cat.id,
                ingredients = ingredients.withoutExcluded(),
            )
        }
    }

    fun toggleSelection(ingredient: Ingredient) {
        val ingredientId = ingredient.id
        val current = _state.value.selectedIds
        val selected = _state.value.selectedIngredients
        val removing = ingredientId in current
        _state.value = _state.value.copy(
            selectedIds = if (removing) current - ingredientId else current + ingredientId,
            selectedIngredients = if (removing) {
                selected.filterNot { it.id == ingredientId }
            } else {
                (selected + ingredient).distinctBy { it.id }
            },
        )
    }

    fun confirmSelected(): List<Ingredient> {
        return _state.value.selectedIngredients
    }

    fun createUserIngredient(name: String, alias: String = "") {
        val trimmedName = name.trim()
        val trimmedAlias = alias.trim()
        if (trimmedName.isBlank()) {
            _state.value = _state.value.copy(createError = "请输入食材名称")
            return
        }
        viewModelScope.launch {
            _state.value = _state.value.copy(creatingIngredient = true, createError = null)
            runCatching {
                val id = ingredientRepo.createUserIngredient(trimmedName, trimmedAlias)
                Ingredient(
                    id = id,
                    name = trimmedName,
                    alias = trimmedAlias,
                    source = "user",
                )
            }.onSuccess { ingredient ->
                _state.value = _state.value.copy(
                    creatingIngredient = false,
                    selectedIds = _state.value.selectedIds + ingredient.id,
                    selectedIngredients = (_state.value.selectedIngredients + ingredient).distinctBy { it.id },
                    lastCreatedIngredientId = ingredient.id,
                )
                reloadCurrentList()
            }.onFailure {
                _state.value = _state.value.copy(
                    creatingIngredient = false,
                    createError = "创建失败，可能已存在同名食材",
                )
            }
        }
    }

    fun clearCreateError() {
        _state.value = _state.value.copy(createError = null)
    }

    private fun List<Ingredient>.withoutExcluded(): List<Ingredient> =
        filterNot { it.id in _state.value.excludeIngredientIds }

    private suspend fun reloadCurrentList() {
        val current = _state.value
        val ingredients = when (val categoryId = current.selectedCategoryId) {
            null, -1L -> ingredientRepo.search(current.keyword)
            else -> {
                val node = current.tree.firstOrNull { it.category.id == categoryId }
                val crowdId = node?.category?.crowdTypeId
                if (crowdId != null) ingredientRepo.listByCrowd(crowdId) else ingredientRepo.listByCategory(categoryId)
            }
        }
        _state.value = _state.value.copy(ingredients = ingredients.withoutExcluded())
    }
}

// 扩展：在指定位置插入多个元素
private fun <T> MutableList<T>.add(index: Int, vararg items: T) {
    items.forEachIndexed { i, item -> add(index + i, item) }
}
