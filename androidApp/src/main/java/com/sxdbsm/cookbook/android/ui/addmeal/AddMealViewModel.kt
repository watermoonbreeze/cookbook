package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.MealType
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

data class AddMealUiState(
    val date: LocalDate = DateTime.today(),
    val mealTypes: List<MealType> = emptyList(),
    val mealTypeId: Long? = null,
    val mealTime: LocalTime = LocalTime(12, 0),
    val dishes: List<DishMini> = emptyList(),
    val note: String = "",
    val isPlan: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
)

class AddMealViewModel(
    private val mealRepo: MealRecordRepository,
    private val dishRepo: DishRepository,
    @Suppress("unused") private val today: kotlinx.datetime.Clock = kotlinx.datetime.Clock.System,
) : ViewModel() {

    private val _state = MutableStateFlow(AddMealUiState())
    val state: StateFlow<AddMealUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val types = mealRepo.listMealTypes()
            val defaultType = types.firstOrNull()
            _state.value = _state.value.copy(
                mealTypes = types,
                mealTypeId = defaultType?.id,
                mealTime = defaultType?.defaultTime ?: LocalTime(12, 0),
            )
        }
    }

    fun setDate(date: LocalDate) {
        _state.value = _state.value.copy(date = date, isPlan = date > DateTime.today())
    }

    fun setMealType(id: Long) {
        val t = _state.value.mealTypes.firstOrNull { it.id == id }
        _state.value = _state.value.copy(
            mealTypeId = id,
            mealTime = t?.defaultTime ?: _state.value.mealTime,
        )
    }

    fun setMealTime(time: LocalTime) {
        _state.value = _state.value.copy(mealTime = time)
    }

    fun setNote(note: String) {
        _state.value = _state.value.copy(note = note)
    }

    fun addDish(dish: DishMini) {
        if (_state.value.dishes.any { it.id == dish.id }) return
        _state.value = _state.value.copy(dishes = _state.value.dishes + dish)
    }

    fun addDishes(dishes: List<DishMini>) {
        val current = _state.value.dishes
        val merged = (current + dishes).distinctBy { it.id }
        _state.value = _state.value.copy(dishes = merged)
    }

    fun removeDish(id: Long) {
        _state.value = _state.value.copy(dishes = _state.value.dishes.filterNot { it.id == id })
    }

    fun save() {
        val s = _state.value
        val mealTypeId = s.mealTypeId ?: return
        if (s.dishes.isEmpty()) return
        viewModelScope.launch {
            _state.value = s.copy(saving = true)
            mealRepo.save(
                date = s.date,
                mealTypeId = mealTypeId,
                mealTime = s.mealTime,
                note = s.note,
                dishIds = s.dishes.map { it.id },
            )
            _state.value = _state.value.copy(saving = false, done = true)
        }
    }
}
