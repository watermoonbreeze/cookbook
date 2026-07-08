package com.sxdbsm.cookbook.android.ui.ingredients

import com.sxdbsm.cookbook.domain.model.Ingredient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * @File : IngredientJumpBus
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 跨屏"跳到具体食材"事件总线
 * <p>
 * 首页/全局搜索点某个食材结果时 request()，食材一级页(IngredientsScreen)消费并调 jumpToIngredient 定位高亮。
 * 因搜索页与食材页各自持有独立的 IngredientPickerViewModel(viewModel 作用域)，用单例总线传递。
 * 消费失败(如页面未及时收到)只退回"跳到食材页"当前行为，不影响原流程。
 * <p>
 * [AI生成] 补齐"首页食材搜索跳到具体食材并高亮"，复用成熟的 jumpToIngredient。
 **/
class IngredientJumpBus {
    private val _pending = MutableStateFlow<Ingredient?>(null)
    val pending: StateFlow<Ingredient?> = _pending.asStateFlow()

    /** 请求跳转到某食材。[AI生成] */
    fun request(ingredient: Ingredient) {
        _pending.value = ingredient
    }

    /** 消费后清空，避免重复触发。[AI生成] */
    fun consume() {
        _pending.value = null
    }
}
