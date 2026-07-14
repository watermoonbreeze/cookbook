package com.sxdbsm.cookbook.android.util

/**
 * @File : SearchDefaults
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 搜索相关默认参数
 * <p>
 * 统一各页搜索去抖时长，避免同一魔法数在多个 ViewModel 里散落漂移。
 * <p>
 * [AI生成] 综合审查：DishesVM/DishPickerVM/IngredientPickerVM/SearchVM 各写 delay(280)，抽取共享常量。
 **/
object SearchDefaults {
    /** 搜索输入去抖时长(毫秒)：连续输入只保留最后一次查询。 */
    const val DEBOUNCE_MS = 280L
}
