package com.sxdbsm.cookbook.android.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.PairingSuggestion
import com.sxdbsm.cookbook.ai.RecommendationDataSource
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
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val suggestions: List<PairingSuggestion> = emptyList(),
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

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
}
