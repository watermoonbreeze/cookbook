package com.sxdbsm.cookbook.android.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.PreferenceKeys
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * @File : FeatureSettingsViewModel
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 功能设置 ViewModel
 * <p>
 * 集中管理各功能开关（当前：分步执行；库存相关后续接入）。开关值持久化到偏好表，响应式驱动相关页面。
 * <p>
 * [AI生成] 用户要求把分步执行等功能配置集中到独立设置页。
 **/
class FeatureSettingsViewModel(
    private val prefs: PreferenceRepository,
) : ViewModel() {

    /** 分步执行开关：**默认开**(用户可关)。[AI修改] 2026-07-22 用户决策默认展示能力·可关闭·统一引用集中默认常量。 */
    val stepModeEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.STEP_MODE_ENABLED, default = PreferenceKeys.DEFAULT_STEP_MODE)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenceKeys.DEFAULT_STEP_MODE)

    fun setStepMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.STEP_MODE_ENABLED, enabled) }
    }

    /** 营养色系开关：**默认开**(用户可关)。[AI修改] 同上决策。 */
    val nutritionColorEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.NUTRITION_COLOR_ENABLED, default = PreferenceKeys.DEFAULT_NUTRITION_COLOR)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenceKeys.DEFAULT_NUTRITION_COLOR)

    fun setNutritionColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.NUTRITION_COLOR_ENABLED, enabled) }
    }

    /** 热量数值显示开关：**默认开**(用户可关)。[AI修改] 同上决策(旧"默认关·热量个人概念红线"按用户新决策更新)·与营养色系独立控制。 */
    val calorieNumberEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.CALORIE_NUMBER_ENABLED, default = PreferenceKeys.DEFAULT_CALORIE_NUMBER)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenceKeys.DEFAULT_CALORIE_NUMBER)

    fun setCalorieNumber(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.CALORIE_NUMBER_ENABLED, enabled) }
    }

    /** 库存挂钩开关：**默认开**(既有行为，关掉=去噪不再显示库存/采购标注)。[AI生成] */
    val pantryHookEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.PANTRY_HOOK_ENABLED, default = true)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun setPantryHook(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.PANTRY_HOOK_ENABLED, enabled) }
    }

    /** P2 餐次结构建议开关：**默认开**(用户可关)——今日卡"缺蔬菜/早餐缺蛋白"一句浅灰下一步小字。[AI生成] */
    val mealStructureHintEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.MEAL_STRUCTURE_HINT_ENABLED, default = PreferenceKeys.DEFAULT_MEAL_STRUCTURE_HINT)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PreferenceKeys.DEFAULT_MEAL_STRUCTURE_HINT)

    fun setMealStructureHint(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.MEAL_STRUCTURE_HINT_ENABLED, enabled) }
    }
}
