package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.PreferenceRepository

/**
 * @File : AiRuntimeConfig
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 运行时选择与配置（云/端/Mock 切换框架）
 * <p>
 * 用偏好存储记录“当前用哪种模型运行时 + 云端 Key”。业务只依赖 AiRuntime 接口，
 * 由 SwitchableAiRuntime 按本配置路由到具体实现——后期接端侧只需往 runtimes 里加一项，业务零改动。
 * <p>
 * [AI生成] S2：把“云/端切换”做成可配置框架，方便后期直接替换。
 **/
enum class AiRuntimeType {
    MOCK, // 无模型：走规则兜底(离线可用)
    CLOUD, // 云端 API(首轮 GLM-4-Flash 免费)
    ON_DEVICE, // 端侧本地模型(后期 LiteRT-LM)
    ;

    companion object {
        fun from(name: String?): AiRuntimeType = values().firstOrNull { it.name == name } ?: CLOUD
    }
}

/** AI 运行时配置（偏好存储读写；Key 只存本机）。[AI修改] AF-13: 恢复 final */
class AiRuntimeConfig(private val prefs: PreferenceRepository) {
    suspend fun activeType(): AiRuntimeType = AiRuntimeType.from(prefs.get(KEY_TYPE))
    suspend fun setActiveType(type: AiRuntimeType) = prefs.set(KEY_TYPE, type.name)

    /** 当前选中的云端模型。[AI生成] */
    suspend fun selectedModel(): CloudModel = CloudModels.byId(prefs.get(KEY_MODEL_ID))
    suspend fun setSelectedModelId(id: String) = prefs.set(KEY_MODEL_ID, id)

    /** 某厂商的 key（同厂多模型共用）；默认厂商兼容迁移旧的单一 key。[AI生成] */
    suspend fun vendorApiKey(vendor: String): String {
        prefs.get(vendorKey(vendor))?.takeIf { it.isNotBlank() }?.let { return it }
        if (vendor == CloudModels.DEFAULT.vendor) {
            prefs.get(KEY_CLOUD_KEY_LEGACY)?.takeIf { it.isNotBlank() }?.let { return it }
        }
        return ""
    }
    suspend fun setVendorApiKey(vendor: String, key: String) = prefs.set(vendorKey(vendor), key.trim())

    /** 当前选中模型对应厂商的 key。[AI生成] */
    suspend fun currentCloudApiKey(): String = vendorApiKey(selectedModel().vendor)

    /** 是否已具备真实模型能力（云端且选中模型的厂商已填 Key）。[AI生成] */
    suspend fun isModelReady(): Boolean = when (activeType()) {
        AiRuntimeType.CLOUD -> currentCloudApiKey().isNotBlank()
        AiRuntimeType.ON_DEVICE -> false // 端侧未接入
        AiRuntimeType.MOCK -> false
    }

    companion object {
        const val KEY_TYPE = "ai_runtime_type"
        const val KEY_MODEL_ID = "ai_cloud_model_id"
        const val KEY_CLOUD_KEY_LEGACY = "ai_cloud_api_key" // 旧单一 key，迁移到默认厂商。
        private fun vendorKey(vendor: String) = "ai_cloud_key_$vendor"
    }
}

/**
 * 可切换运行时：按配置把请求路由到 Mock/云端/端侧。[AI生成]
 *
 * 后期新增端侧实现，只需在构造的 runtimes 映射里加一项，SwitchableAiRuntime 与业务代码都不用改。
 */
class SwitchableAiRuntime(
    private val config: AiRuntimeConfig,
    private val runtimes: Map<AiRuntimeType, AiRuntime>,
) : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> {
        val type = config.activeType()
        val runtime = runtimes[type]
            ?: runtimes[AiRuntimeType.MOCK]
            ?: return Result.failure(IllegalStateException("no AiRuntime registered for $type"))
        return runtime.complete(request)
    }
}
