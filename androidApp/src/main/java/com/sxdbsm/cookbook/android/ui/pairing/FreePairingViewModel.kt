package com.sxdbsm.cookbook.android.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.PairingSuggestion
import com.sxdbsm.cookbook.ai.RecommendationDataSource
import com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefill
import com.sxdbsm.cookbook.data.repository.IngredientRepository
import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @File : FreePairingViewModel
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 食材自由搭配 ViewModel
 * <p>
 * 拉取在手食材的离线规则轻搭配建议（不依赖已有菜品/AI）。
 * <p>
 * [AI生成] 待办"自由搭配"一期。
 **/
class FreePairingViewModel(
    private val dataSource: RecommendationDataSource,
    private val ingredientRepo: IngredientRepository, // [AI生成] "存为菜品"：按名解析/复用食材 id
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val suggestions: List<PairingSuggestion> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    // [AI生成] 存为菜品防抖门闩：解析食材期间(IO)连点多张卡会起多协程各自导航压双栈——本闩只放行一次(主线程无真并发)。
    private var building = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val suggestions = dataSource.freePairing()
            _state.value = UiState(loading = false, suggestions = suggestions)
        }
    }

    /**
     * "存为菜品"：把一条搭配建议的食材名(按名 createUserIngredient 复用/建 id)+做法名解析成新建菜品预填契约。[AI生成]
     *
     * 菜名/标签不预填(交用户填)。解析在 IO 后回主线程回调，调用方据此 request(prefill)+跳新建页。
     */
    fun buildPrefill(suggestion: PairingSuggestion, onReady: (NewDishPrefill) -> Unit) {
        if (building) return // 防抖：解析期间连点只放行一次，避免多次导航压栈
        building = true
        viewModelScope.launch {
            try {
                val ingredients = suggestion.items.mapNotNull { name ->
                    runCatching { ingredientRepo.createUserIngredient(name) }.getOrNull()
                        ?.let { id -> Ingredient(id = id, name = name) }
                }
                onReady(NewDishPrefill(ingredients = ingredients, cookingMethodName = suggestion.method))
            } finally {
                building = false
            }
        }
    }
}
