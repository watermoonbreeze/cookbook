package com.sxdbsm.cookbook.android.ui.timeline

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class TimelineUiState(
    val pages: List<DayMealCardData> = emptyList(),
    val rangeMin: LocalDate? = null,
    val rangeMax: LocalDate? = null,
    val loading: Boolean = false,
)

class TimelineViewModel(
    private val repo: MealRecordRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(TimelineUiState())
    val state: StateFlow<TimelineUiState> = _state.asStateFlow()

    init { loadFirstPage() }

    fun loadFirstPage() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val today = DateTime.today()
            val (min, max) = repo.dateRange()
            val dates = repo.listDistinctDates(limit = 15, offset = 0)
            val cards = dates.map { repo.loadDayMealCard(it, today) }
            _state.value = TimelineUiState(
                pages = cards,
                rangeMin = min ?: today,
                rangeMax = max ?: today,
                loading = false,
            )
        }
    }

    fun loadMore(offset: Int) {
        viewModelScope.launch {
            val today = DateTime.today()
            val dates = repo.listDistinctDates(limit = 15, offset = offset.toLong())
            if (dates.isEmpty()) return@launch
            val cards = dates.map { repo.loadDayMealCard(it, today) }
            _state.value = _state.value.copy(pages = _state.value.pages + cards)
        }
    }
}
