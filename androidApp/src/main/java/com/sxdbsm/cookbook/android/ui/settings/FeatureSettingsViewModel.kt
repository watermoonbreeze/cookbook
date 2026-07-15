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

    /** 分步执行开关：默认关（只按用户书写顺序展示步骤，不显示步骤序号、不进分步烹饪）。[AI生成] */
    val stepModeEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.STEP_MODE_ENABLED, default = false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setStepMode(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.STEP_MODE_ENABLED, enabled) }
    }

    /** 营养色系开关：默认关。[AI生成] */
    val nutritionColorEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.NUTRITION_COLOR_ENABLED, default = false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setNutritionColor(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.NUTRITION_COLOR_ENABLED, enabled) }
    }

    /** 热量数值显示开关：默认关。[AI生成] 与营养色系独立控制(数字/配色分开)。 */
    val calorieNumberEnabled: StateFlow<Boolean> =
        prefs.observeFlag(PreferenceKeys.CALORIE_NUMBER_ENABLED, default = false)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun setCalorieNumber(enabled: Boolean) {
        viewModelScope.launch { prefs.setFlag(PreferenceKeys.CALORIE_NUMBER_ENABLED, enabled) }
    }
}
