package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.AiRuntimeType
import kotlinx.coroutines.launch

/**
 * @File : AiSettingsViewModel
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 设置 ViewModel（云端 Key + 运行时选择）
 * <p>
 * [AI生成] S3：Key 只存本机偏好；切换运行时(云/Mock/端侧)即时生效。
 **/
class AiSettingsViewModel(private val config: AiRuntimeConfig) : ViewModel() {

    var state by mutableStateOf(AiSettingsUiState())
        private set

    init {
        viewModelScope.launch {
            state = state.copy(apiKey = config.cloudApiKey(), type = config.activeType(), loaded = true)
        }
    }

    fun onKeyChange(value: String) {
        state = state.copy(apiKey = value, savedTip = null)
    }

    fun onTypeChange(type: AiRuntimeType) {
        state = state.copy(type = type, savedTip = null)
    }

    fun save() {
        viewModelScope.launch {
            config.setCloudApiKey(state.apiKey)
            config.setActiveType(state.type)
            state = state.copy(savedTip = "已保存")
        }
    }
}

/** AI 设置 UI 状态。[AI生成] */
data class AiSettingsUiState(
    val apiKey: String = "",
    val type: AiRuntimeType = AiRuntimeType.CLOUD,
    val loaded: Boolean = false,
    val savedTip: String? = null,
)
