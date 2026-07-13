package com.sxdbsm.cookbook.android.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.PantryRepository
import com.sxdbsm.cookbook.data.repository.ShoppingListRepository
import com.sxdbsm.cookbook.domain.model.ShoppingItem
import com.sxdbsm.cookbook.domain.model.ShoppingReason
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
    private val pantryRepo: PantryRepository, // [AI生成] 采购勾选 → 按份数入库。
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val items: List<ShoppingItem> = emptyList(),
        val checked: Set<String> = emptySet(), // 已勾选的食材名(前端临时态，不落库)
        val servings: Map<String, Int> = emptyMap(), // [AI生成] 各项计划采购份数(名→份数)，缺省视为1
        val stocked: Set<String> = emptySet(), // [AI生成] 本次已入库的项(划掉灰显)
        val toast: String? = null, // [AI生成] 一次性提示(入库结果/请先勾选)
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** 取某项的采购份数(默认1)。[AI生成] */
    fun servingOf(name: String): Int = _state.value.servings[name] ?: 1

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val items = repo.aggregate(DateTime.today())
            // [AI修改] N1：份数默认按该食材在未来餐食中的实际所需(mealCount)预填，用户可再调，而非都默认1。
            // 刷新=重新拉取并重置所有临时态(勾选/已入库/提示)。
            val servings = items.associate { it.ingredientName to it.mealCount.coerceAtLeast(1) }
            _state.value = UiState(loading = false, items = items, servings = servings)
        }
    }

    /** 勾选/取消某项（买到了打勾）。[AI生成] */
    fun toggleChecked(name: String) {
        val cur = _state.value.checked
        _state.value = _state.value.copy(checked = if (name in cur) cur - name else cur + name)
    }

    /** 全选/取消全选某组(按 reason)。[AI生成] */
    fun toggleSelectAll(reason: ShoppingReason) {
        val groupNames = _state.value.items.filter { it.reason == reason }.map { it.ingredientName }.toSet()
        val cur = _state.value.checked
        val allChecked = groupNames.isNotEmpty() && groupNames.all { it in cur }
        _state.value = _state.value.copy(
            checked = if (allChecked) cur - groupNames else cur + groupNames,
        )
    }

    /** 调整某项份数(±，最小1)。[AI生成] */
    fun changeServing(name: String, delta: Int) {
        val cur = _state.value.servings[name] ?: 1
        val next = (cur + delta).coerceAtLeast(1)
        _state.value = _state.value.copy(servings = _state.value.servings + (name to next))
    }

    /** 把已勾选项按份数入库。[AI生成] 没勾选则提示；无法解析 id 的项跳过并计数。 */
    fun stockInChecked() {
        val checked = _state.value.checked
        if (checked.isEmpty()) {
            _state.value = _state.value.copy(toast = "请先勾选要入库的食材")
            return
        }
        viewModelScope.launch {
            val targets = _state.value.items.filter { it.ingredientName in checked && it.ingredientId != null }
            var ok = 0
            targets.forEach { item ->
                runCatching { pantryRepo.addServings(item.ingredientId!!, _state.value.servings[item.ingredientName] ?: 1) }
                    .onSuccess { ok++ }
            }
            val skipped = checked.size - targets.size
            val msg = buildString {
                append("已入库 $ok 项")
                if (skipped > 0) append("，$skipped 项无法识别已跳过")
            }
            _state.value = _state.value.copy(
                stocked = _state.value.stocked + targets.map { it.ingredientName },
                checked = _state.value.checked - targets.map { it.ingredientName }.toSet(),
                toast = msg,
            )
        }
    }

    /** 消费一次性提示。[AI生成] */
    fun consumeToast() {
        _state.value = _state.value.copy(toast = null)
    }
}
