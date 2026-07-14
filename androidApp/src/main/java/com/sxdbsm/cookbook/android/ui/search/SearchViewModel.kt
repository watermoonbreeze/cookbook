package com.sxdbsm.cookbook.android.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.DishRepository
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.data.repository.MealRecordRepository
import com.sxdbsm.cookbook.domain.model.DayMealCardData
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 全局搜索页状态。[AI生成]
 *
 * 同一关键词会分别搜索菜品、食材、餐食，UI 按分组展示结果。
 */
data class SearchUiState(
    val keyword: String = "",
    val dishes: List<DishMini> = emptyList(),
    val ingredients: List<Ingredient> = emptyList(),
    val meals: List<DayMealCardData> = emptyList(),
    val loading: Boolean = false,
    val loadingMoreMeals: Boolean = false,
    val canLoadMoreMeals: Boolean = false,
)

/**
 * 全局搜索 ViewModel。[AI生成]
 *
 * 搜索输入做防抖，避免用户输入时同时触发三类数据库查询造成卡顿。
 */
class SearchViewModel(
    private val dishRepo: DishRepository,
    private val ingredientRepo: IngredientRepository,
    private val mealRepo: MealRecordRepository,
) : ViewModel() {

    private companion object {
        // [AI修改] 与其余搜索页共用去抖常量，单一真相源避免漂移。
        private const val DEBOUNCE_MS = com.sxdbsm.cookbook.android.util.SearchDefaults.DEBOUNCE_MS
        private const val MEAL_PAGE_SIZE = 10L
    }

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var mealOffset = 0L

    fun setKeyword(value: String) {
        _state.value = _state.value.copy(keyword = value)
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            search(value)
        }
    }

    /**
     * 加载更多餐食结果。[AI生成]
     */
    fun loadMoreMeals() {
        val current = _state.value
        if (current.keyword.isBlank() || current.loading || current.loadingMoreMeals || !current.canLoadMoreMeals) return
        viewModelScope.launch {
            _state.value = _state.value.copy(loadingMoreMeals = true)
            val more = mealRepo.searchMealCards(current.keyword, limit = MEAL_PAGE_SIZE, offset = mealOffset)
            mealOffset += more.size
            _state.value = _state.value.copy(
                meals = (current.meals + more).distinctBy { it.date },
                loadingMoreMeals = false,
                canLoadMoreMeals = more.size.toLong() == MEAL_PAGE_SIZE,
            )
        }
    }

    private suspend fun search(keyword: String) {
        val trimmed = keyword.trim()
        if (trimmed.isBlank()) {
            mealOffset = 0
            _state.value = SearchUiState(keyword = keyword)
            return
        }
        _state.value = _state.value.copy(loading = true)
        val dishes = dishRepo.searchDishes(trimmed).take(20)
        val ingredients = ingredientRepo.search(trimmed).take(20)
        val meals = mealRepo.searchMealCards(trimmed, limit = MEAL_PAGE_SIZE, offset = 0)
        mealOffset = meals.size.toLong()
        _state.value = _state.value.copy(
            dishes = dishes,
            ingredients = ingredients,
            meals = meals,
            loading = false,
            loadingMoreMeals = false,
            canLoadMoreMeals = meals.size.toLong() == MEAL_PAGE_SIZE,
        )
    }
}
