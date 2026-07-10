package com.sxdbsm.cookbook.android.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.ShoppingListRepository
import com.sxdbsm.cookbook.domain.model.ShoppingItem
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * @File : ShoppingListViewModel
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 采购清单 ViewModel
 * <p>
 * 拉取"今天及未来"餐食聚合出的采购/缺料购物清单；本地记录已勾选项(仅当次会话，纯前端)。
 * <p>
 * [AI生成] 待办"采购清单聚合"。
 **/
class ShoppingListViewModel(
    private val repo: ShoppingListRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<ShoppingItem> = emptyList(),
        val checked: Set<String> = emptySet(), // 已勾选的食材名(前端临时态，不落库)
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val items = repo.aggregate(DateTime.today())
            _state.value = _state.value.copy(loading = false, items = items)
        }
    }

    /** 勾选/取消某项（买到了打勾）。[AI生成] */
    fun toggleChecked(name: String) {
        val cur = _state.value.checked
        _state.value = _state.value.copy(checked = if (name in cur) cur - name else cur + name)
    }
}
