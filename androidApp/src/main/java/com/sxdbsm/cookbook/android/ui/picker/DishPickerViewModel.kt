package com.sxdbsm.cookbook.android.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DishPickerUiState(
    val keyword: String = "",
    val dishes: List<DishMini> = emptyList(),
    val popular: List<DishMini> = emptyList(),
    val recent: List<DishMini> = emptyList(),
    val selected: List<DishMini> = emptyList(),
    val excludeDishIds: Set<Long> = emptySet(),
)

class DishPickerViewModel(
    private val dishRepo: DishRepository,
) : ViewModel() {

    private val _keyword = MutableStateFlow("")
    private val _dishes = MutableStateFlow<List<DishMini>>(emptyList())
    private val _selected = MutableStateFlow<List<DishMini>>(emptyList())
    private val _excludeDishIds = MutableStateFlow<Set<Long>>(emptySet())

    private val popular = dishRepo.observePopularDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val recent = dishRepo.observeRecentDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val baseState = combine(_keyword, _dishes, popular) { keyword, dishes, popular ->
        Triple(keyword, dishes, popular)
    }

    private val listState = combine(baseState, recent, _selected) { base, recent, selected ->
        ListState(base.first, base.second, base.third, recent, selected)
    }

    val state: StateFlow<DishPickerUiState> = combine(listState, _excludeDishIds) { list, exclude ->
        DishPickerUiState(
            keyword = list.keyword,
            dishes = list.dishes.filterNot { it.id in exclude },
            popular = list.popular.filterNot { it.id in exclude },
            recent = list.recent.filterNot { it.id in exclude },
            selected = list.selected.filterNot { it.id in exclude },
            excludeDishIds = exclude,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishPickerUiState())

    init {
        refresh("")
    }

    fun configure(excludeDishIds: Set<Long>, initialSelected: List<DishMini>) {
        _excludeDishIds.value = excludeDishIds
        _selected.value = initialSelected.filterNot { it.id in excludeDishIds }.distinctBy { it.id }
    }

    fun setKeyword(value: String) {
        _keyword.value = value
        refresh(value)
    }

    fun toggle(dish: DishMini, multiSelect: Boolean) {
        val current = _selected.value
        _selected.value = if (multiSelect) {
            if (current.any { it.id == dish.id }) {
                current.filterNot { it.id == dish.id }
            } else {
                current + dish
            }
        } else {
            listOf(dish)
        }
    }

    fun isSelected(dishId: Long): Boolean = _selected.value.any { it.id == dishId }

    fun confirmSelected(): List<DishMini> = _selected.value

    private fun refresh(keyword: String) {
        viewModelScope.launch {
            _dishes.value = dishRepo.searchDishes(keyword)
        }
    }
}

private data class ListState(
    val keyword: String,
    val dishes: List<DishMini>,
    val popular: List<DishMini>,
    val recent: List<DishMini>,
    val selected: List<DishMini>,
)
