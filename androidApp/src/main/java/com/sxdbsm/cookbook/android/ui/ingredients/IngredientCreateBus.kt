package com.sxdbsm.cookbook.android.ui.ingredients

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @File : IngredientCreateBus
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 跨屏"按名新建食材"事件总线
 * <p>
 * 首页搜索无结果点"＋新建食材"时 request(名称)，导航到食材页，IngredientsScreen 消费并打开
 * 新增食材编辑器、预填名称。与 IngredientJumpBus 同理(各屏独立 ViewModel，用单例总线传)。
 * <p>
 * [AI生成] 统一搜索"点此新建"——食材侧。
 **/
class IngredientCreateBus {
    private val _pending = MutableStateFlow<String?>(null)
    val pending: StateFlow<String?> = _pending.asStateFlow()

    fun request(name: String) { _pending.value = name }
    fun consume() { _pending.value = null }
}
