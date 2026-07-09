package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.CloudModel
import com.sxdbsm.cookbook.ai.CloudModels
import kotlinx.coroutines.launch

/**
 * @File : AiSettingsViewModel
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : AI 设置 ViewModel（三档来源 + 云端选模型 + 按厂商 Key，改动即时生效）
 * <p>
 * [AI修改] 云端模型可下拉选择(多厂商框架)，Key 按厂商存、可设置/编辑；来源/模型/Key 改动即时持久化。
 **/
class AiSettingsViewModel(private val config: AiRuntimeConfig) : ViewModel() {

    var state by mutableStateOf(AiSettingsUiState())
        private set

    init {
        reload()
    }

    private fun reload() {
        viewModelScope.launch {
            val vendors = CloudModels.ALL.map { it.vendor }.distinct()
            state = state.copy(
                type = config.activeType(),
                selectedModelId = config.selectedModel().id,
                keyByVendor = vendors.associateWith { config.vendorApiKey(it) },
                loaded = true,
            )
        }
    }

    fun onTypeChange(type: AiRuntimeType) {
        viewModelScope.launch {
            config.setActiveType(type)
            state = state.copy(type = type)
        }
    }

    fun onSelectModel(modelId: String) {
        viewModelScope.launch {
            config.setSelectedModelId(modelId)
            state = state.copy(selectedModelId = modelId)
        }
    }

    /** 保存某厂商 Key（设置/编辑弹框确定时）。[AI生成] */
    fun onSaveVendorKey(vendor: String, key: String) {
        viewModelScope.launch {
            config.setVendorApiKey(vendor, key)
            state = state.copy(keyByVendor = state.keyByVendor + (vendor to key.trim()))
        }
    }

    fun selectedModel(): CloudModel = CloudModels.byId(state.selectedModelId)
}

/** AI 设置 UI 状态。[AI生成] */
data class AiSettingsUiState(
    val type: AiRuntimeType = AiRuntimeType.CLOUD,
    val models: List<CloudModel> = CloudModels.ALL,
    val selectedModelId: String = CloudModels.DEFAULT.id,
    val keyByVendor: Map<String, String> = emptyMap(), // vendor -> key（状态展示用）
    val loaded: Boolean = false,
)
