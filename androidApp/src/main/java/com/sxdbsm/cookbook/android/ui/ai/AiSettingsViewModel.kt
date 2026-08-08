package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sxdbsm.cookbook.ai.AiRuntimeConfig
import com.sxdbsm.cookbook.ai.AiRuntimeType
import com.sxdbsm.cookbook.ai.CloudAiConsent
import com.sxdbsm.cookbook.ai.CloudModel
import com.sxdbsm.cookbook.ai.CloudModels
import com.sxdbsm.cookbook.ai.ConsentSource
import com.sxdbsm.cookbook.ai.ConsentStatus
import com.sxdbsm.cookbook.android.ui.ai.CloudAiDisclosure
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.coroutines.launch

/**
 * @File : AiSettingsViewModel
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : AI 设置 ViewModel（三档来源 + 云端选模型 + 按厂商 Key，改动即时生效）
 * <p>
 * [AI修改] 云端模型可下拉选择(多厂商框架)，Key 按厂商存、可设置/编辑；来源/模型/Key 改动即时持久化。
 * [AI修改] L1：新增云端 AI 同意状态（grantConsent/declineConsent/confirmVendorSwitch/closeCloudAi/resolveGrandfather）。
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
                cloudAiConsent = config.cloudAiConsent(), // [AI修改] L1：同意状态
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

    /**
     * [AI修改] v2b：同意后的完整保存流程——先写 consent 再写 Key（INV-L1-06），next 一次构造两处复用（GC-29）。
     * `acknowledgedVendors` 扫描**此刻全部已配置 Key 的厂商**（非仅当前 vendor）——否则用户此前（grandfather 时期）
     * 已配置过的另一厂商 Key，会在本次只针对 vendor A 同意之后，被用户切换模型下拉悄悄启用而未经任何确认
     * （蓝图 §10 v2 挑战第8项）。`vendor` 自身此刻可能还没落库（`onSaveVendorKey` 在这之后才调），须显式并入。
     */
    fun grantConsent(vendor: String, key: String, source: ConsentSource) {
        viewModelScope.launch {
            val existingVendors = CloudModels.ALL.map { it.vendor }.distinct()
                .filter { config.vendorApiKey(it).isNotBlank() }.toSet()
            val next = state.cloudAiConsent.copy(
                status = ConsentStatus.GRANTED, source = source,
                grantedAtEpochSeconds = DateTime.nowEpochSeconds(),
                scopeVersion = CloudAiDisclosure.SCOPE_VERSION,
                acknowledgedVendors = existingVendors + vendor,
            )
            config.setCloudAiConsent(next)
            state = state.copy(cloudAiConsent = next)
            onSaveVendorKey(vendor, key)
        }
    }

    /** [AI生成] 拒绝/关闭——Key 不动，consent 置 DECLINED，activeType 回退 MOCK（INV-L1-07）。 */
    fun declineConsent() {
        viewModelScope.launch {
            val next = state.cloudAiConsent.copy(status = ConsentStatus.DECLINED)
            config.setCloudAiConsent(next)
            if (state.type == AiRuntimeType.CLOUD) onTypeChange(AiRuntimeType.MOCK)
            state = state.copy(cloudAiConsent = next)
        }
    }

    /** [AI生成] 换厂商轻量确认——仅追加 vendor 到已确认集合。 */
    fun confirmVendorSwitch(vendor: String, key: String) {
        viewModelScope.launch {
            val next = state.cloudAiConsent.copy(acknowledgedVendors = state.cloudAiConsent.acknowledgedVendors + vendor)
            config.setCloudAiConsent(next)
            state = state.copy(cloudAiConsent = next)
            onSaveVendorKey(vendor, key)
        }
    }

    /** [AI生成] 设置页常驻状态块"关闭"入口；deleteKey=true 时一并清空当前厂商 Key。 */
    fun closeCloudAi(vendor: String, deleteKey: Boolean) {
        viewModelScope.launch {
            val next = state.cloudAiConsent.copy(status = ConsentStatus.DECLINED)
            config.setCloudAiConsent(next)
            // [AI修改] L1 审查建议：与 declineConsent 的守卫一致（正常路径下状态块仅 CLOUD 渲染，此处防御重复写 MOCK）。
            if (state.type == AiRuntimeType.CLOUD) onTypeChange(AiRuntimeType.MOCK)
            if (deleteKey) onSaveVendorKey(vendor, "")
            // [AI修改] L1 审查建议：镜像写在副作用之后（与其余四函数先镜像后副作用的写法顺序不同），
            //   同一协程内无挂起点、执行顺序可预测，不影响正确性；注明避免维护困惑（蓝图 §10 v2 挑战第14项已知）。
            state = state.copy(cloudAiConsent = next)
        }
    }

    /** [AI生成] grandfather 补确认专用——不接收/不改动 Key（INV-L1-09），acknowledgedVendors 取全部已配置厂商。 */
    fun resolveGrandfather(confirm: Boolean) {
        viewModelScope.launch {
            if (confirm) {
                val vendors = CloudModels.ALL.map { it.vendor }.distinct()
                    .filter { config.vendorApiKey(it).isNotBlank() }.toSet()
                val next = CloudAiConsent(
                    status = ConsentStatus.GRANTED, source = ConsentSource.GRANDFATHER_CONFIRMED,
                    grantedAtEpochSeconds = DateTime.nowEpochSeconds(),
                    scopeVersion = CloudAiDisclosure.SCOPE_VERSION, acknowledgedVendors = vendors,
                )
                config.setCloudAiConsent(next)
                state = state.copy(cloudAiConsent = next)
            } else {
                val next = CloudAiConsent(status = ConsentStatus.DECLINED)
                config.setCloudAiConsent(next)
                onTypeChange(AiRuntimeType.MOCK)
                state = state.copy(cloudAiConsent = next)
            }
        }
    }

    fun selectedModel(): CloudModel = CloudModels.byId(state.selectedModelId)
}

/** AI 设置 UI 状态。[AI生成] */
data class AiSettingsUiState(
    val type: AiRuntimeType = AiRuntimeType.CLOUD, // [AI修改] v2：撤销 v1 的"顺手修"，保持原值不动（蓝图 §10 C-04）
    val models: List<CloudModel> = CloudModels.ALL,
    val selectedModelId: String = CloudModels.DEFAULT.id,
    val keyByVendor: Map<String, String> = emptyMap(), // vendor -> key（状态展示用）
    val loaded: Boolean = false,
    val cloudAiConsent: CloudAiConsent = CloudAiConsent(), // [AI修改] L1：云端 AI 同意状态
)
