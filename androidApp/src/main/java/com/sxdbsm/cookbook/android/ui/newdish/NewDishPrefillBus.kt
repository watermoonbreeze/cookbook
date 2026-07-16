package com.sxdbsm.cookbook.android.ui.newdish

import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @File : NewDishPrefillBus
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 跨屏"新建菜品预填"事件总线
 * <p>
 * 场景：①菜品/首页搜索无结果"＋新建菜品「x」"→预填菜名；②食材页多选"组成菜品"→预填一批食材。
 * 因新建菜品页与来源页各持独立 ViewModel(viewModel 作用域)，用单例总线传预填数据；
 * NewDishScreen 进入时消费。消费失败只退回"空白新建"，不影响原流程。
 * <p>
 * [AI生成] 统一搜索"点此新建" + 从食材出发生成菜品。
 **/
data class NewDishPrefill(
    val name: String = "",
    val ingredients: List<Ingredient> = emptyList(),
)

class NewDishPrefillBus {
    private val _pending = MutableStateFlow<NewDishPrefill?>(null)
    val pending: StateFlow<NewDishPrefill?> = _pending.asStateFlow()

    /** 请求以给定名称/食材预填新建菜品。[AI生成] */
    fun request(prefill: NewDishPrefill) {
        _pending.value = prefill
    }

    /** 消费后清空，避免重复触发。[AI生成] */
    fun consume() {
        _pending.value = null
    }
}
