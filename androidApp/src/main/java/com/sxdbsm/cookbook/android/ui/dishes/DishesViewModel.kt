package com.sxdbsm.cookbook.android.ui.dishes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class DishesSortTab { RECENT, FAVORITE, PINYIN, ALL }

data class DishesUiState(
    val popular: List<DishMini> = emptyList(),
    val all: List<DishMini> = emptyList(),
    val keyword: String = "",
    val sortTab: DishesSortTab = DishesSortTab.RECENT,
)

class DishesViewModel(
    private val dishRepo: DishRepository,
    @Suppress("unused") private val mealRepo: MealRecordRepository,
) : ViewModel() {

    private val _keyword = MutableStateFlow("")
    private val _sortTab = MutableStateFlow(DishesSortTab.RECENT)
    private val _list = MutableStateFlow<List<DishMini>>(emptyList())

    val popular: StateFlow<List<DishMini>> = dishRepo.observePopularDishes(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val uiState: StateFlow<DishesUiState>
        get() = combineState()

    private fun combineState(): StateFlow<DishesUiState> {
        return kotlinx.coroutines.flow.combine(
            popular, _list, _keyword, _sortTab,
        ) { popular, list, kw, tab ->
            DishesUiState(popular = popular, all = list, keyword = kw, sortTab = tab)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DishesUiState())
    }

    init {
        viewModelScope.launch {
            // 初始全列表
            _list.value = dishRepo.searchDishes("")
        }
    }

    fun setKeyword(kw: String) {
        _keyword.value = kw
        viewModelScope.launch { _list.value = dishRepo.searchDishes(kw) }
    }

    fun setSortTab(tab: DishesSortTab) { _sortTab.value = tab /* MVP 暂不影响列表排序，后续按 tab 重查 */ }
}
