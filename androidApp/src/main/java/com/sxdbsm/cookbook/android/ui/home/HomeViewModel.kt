package com.sxdbsm.cookbook.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(
    val popular: List<DishMini> = emptyList(),
    val recent: List<DishMini> = emptyList(),
    val plans: List<DayMealCardData> = emptyList(),
)

class HomeViewModel(
    private val dishRepo: DishRepository,
    private val mealRepo: MealRecordRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        dishRepo.observePopularDishes(limit = 6),
        dishRepo.observeRecentDishes(limit = 6),
        mealRepo.observeTodayPlusFuture(DateTime.today()),
    ) { popular, recent, plans ->
        HomeUiState(popular = popular, recent = recent, plans = plans)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())
}
