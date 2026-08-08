package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString

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

/** AI 运行时配置（偏好存储读写；Key 只存本机）。[AI修改] AF-13: 恢复 final · [AI修改] L1: 新增云端 AI 同意状态读写 */
class AiRuntimeConfig(private val prefs: PreferenceRepository) {
    private val consentJson = kotlinx.serialization.json.Json { ignoreUnknownKeys = true } // [AI生成] L1：同意状态序列化
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

    /**
     * 读取云端 AI 同意状态；**只有"确无记录"**（`prefs.get(...)==null`）才扫描全部厂商推导 grandfather 态（INV-L1-01）。[AI修改] v2b：
     * "记录存在但解码失败"必须 fail-closed 返回 `NOT_ASKED`，不得落入 grandfather 推导——否则一条被损坏的 `DECLINED` 记录会
     * 自动"升级"成已同意（蓝图 L1 §10 v2 挑战第9项），且这本质上正是蓝图自己禁止的"用 Key 非空隐式推断同意"。
     */
    suspend fun cloudAiConsent(): CloudAiConsent {
        val raw = prefs.get(KEY_CLOUD_AI_CONSENT)
        if (raw != null) {
            return runCatching { consentJson.decodeFromString<CloudAiConsent>(raw) }
                .getOrDefault(CloudAiConsent(status = ConsentStatus.NOT_ASKED)) // fail-closed，不做二次推导
        }
        val anyVendorKeyPresent = CloudModels.ALL.map { it.vendor }.distinct().any { vendorApiKey(it).isNotBlank() }
        return if (anyVendorKeyPresent) CloudAiConsent(status = ConsentStatus.GRANDFATHER_PENDING) else CloudAiConsent()
    }

    /** 写入云端 AI 同意状态。[AI生成] */
    suspend fun setCloudAiConsent(consent: CloudAiConsent) =
        prefs.set(KEY_CLOUD_AI_CONSENT, consentJson.encodeToString(consent))

    /** 同意是否已满足（供运行时闸门与 UI 共用同一判据）。[AI生成] */
    suspend fun cloudAiConsentGranted(): Boolean =
        cloudAiConsent().status.let { it == ConsentStatus.GRANTED || it == ConsentStatus.GRANDFATHER_PENDING }

    companion object {
        const val KEY_TYPE = "ai_runtime_type"
        const val KEY_MODEL_ID = "ai_cloud_model_id"
        const val KEY_CLOUD_KEY_LEGACY = "ai_cloud_api_key" // 旧单一 key，迁移到默认厂商。
        const val KEY_CLOUD_AI_CONSENT = "cloud_ai_consent" // [AI生成] L1：云端 AI 同意状态（偏好 JSON）
        private fun vendorKey(vendor: String) = "ai_cloud_key_$vendor"
    }
}

/**
 * 可切换运行时：按配置把请求路由到 Mock/云端/端侧。[AI生成]
 *
 * 后期新增端侧实现，只需在构造的 runtimes 映射里加一项，SwitchableAiRuntime 与业务代码都不用改。
 * [AI修改] L1：唯一真实联网闸门——CLOUD 已选中但同意未满足时直接短路，不路由到 CloudAiRuntime（INV-L1-02）。
 */
class SwitchableAiRuntime(
    private val config: AiRuntimeConfig,
    private val runtimes: Map<AiRuntimeType, AiRuntime>,
) : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> {
        val type = config.activeType()
        if (type == AiRuntimeType.CLOUD && !config.cloudAiConsentGranted()) {
            return Result.failure(CloudAiConsentRequiredException())
        }
        val runtime = runtimes[type]
            ?: runtimes[AiRuntimeType.MOCK]
            ?: return Result.failure(IllegalStateException("no AiRuntime registered for $type"))
        return runtime.complete(request)
    }
    // stream() 不重写：AiRuntime 接口默认实现已把 complete() 的 Result.failure 转成 LlmStreamEvent.Failed（AiRuntime.kt:42-53），
    // 本类本就未重写 stream()（K1a GC-37 挑战 #14 已记录此既有事实），本次改动零新增行为分歧。
}
