package com.sxdbsm.cookbook.ai

/**
 * @File : CloudModel
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 云端可选模型注册表（多厂商，OpenAI 兼容协议）
 * <p>
 * 主流国内大模型（智谱/DeepSeek/通义/Kimi）都提供 OpenAI 兼容的 chat/completions 接口，
 * 故一个通用 CloudAiRuntime + 不同 endpoint/model 即可切换厂商。密钥按 vendor 存（同厂多模型共用一个 key）。
 * 当前默认智谱 GLM-4-Flash（免费）；新增厂商只往 ALL 里加一项，UI/运行时零改动。
 * <p>
 * [AI生成] 把云端模型做成可插拔框架，下拉选择具体模型。
 **/
data class CloudModel(
    val id: String, // 唯一 id，持久化用
    val displayName: String, // 下拉显示名
    val vendor: String, // 厂商（密钥按此分组：同厂模型共用一个 key）
    val vendorName: String, // 厂商中文名（key 提示用）
    val endpoint: String, // OpenAI 兼容 chat/completions URL
    val model: String, // API 模型名
    val free: Boolean = false, // 是否免费
    val applyUrl: String = "", // [AI生成] 该厂商 API Key 申请入口官网(配置指南"打开官网"跳转用；同厂共用)
    val supportsJsonMode: Boolean = false, // [AI生成] R3:是否支持 response_format:json_object(强约束 JSON 输出→解析成功率↑)。老型号 glm-4-flash 不支持。
)

object CloudModels {
    val ALL: List<CloudModel> = listOf(
        // [AI生成] R3(用户2026-07-19拍板):默认换 glm-4.5-flash——同免费档、更强、支持 JSON 强约束输出(解析更稳)。
        //   与 glm-4-flash 同厂(zhipu)API Key 通用,已设 glm-4-flash key 的用户切到此模型无需重设 key。
        CloudModel(
            id = "zhipu_glm45_flash",
            displayName = "智谱 GLM-4.5-Flash（免费·推荐）",
            vendor = "zhipu",
            vendorName = "智谱",
            endpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            model = "glm-4.5-flash",
            free = true,
            applyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
            supportsJsonMode = true,
        ),
        CloudModel(
            id = "zhipu_glm4_flash",
            displayName = "智谱 GLM-4-Flash（免费）",
            vendor = "zhipu",
            vendorName = "智谱",
            endpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            model = "glm-4-flash",
            free = true,
            applyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        ),
        CloudModel(
            id = "zhipu_glm4_air",
            displayName = "智谱 GLM-4-Air",
            vendor = "zhipu",
            vendorName = "智谱",
            endpoint = "https://open.bigmodel.cn/api/paas/v4/chat/completions",
            model = "glm-4-air",
            applyUrl = "https://open.bigmodel.cn/usercenter/apikeys",
        ),
        CloudModel(
            id = "deepseek_chat",
            displayName = "DeepSeek Chat",
            vendor = "deepseek",
            vendorName = "DeepSeek",
            endpoint = "https://api.deepseek.com/chat/completions",
            model = "deepseek-chat",
            applyUrl = "https://platform.deepseek.com/api_keys",
        ),
        CloudModel(
            id = "qwen_turbo",
            displayName = "通义千问 Qwen-Turbo",
            vendor = "dashscope",
            vendorName = "通义千问",
            endpoint = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions",
            model = "qwen-turbo",
            applyUrl = "https://bailian.console.aliyun.com/",
        ),
        CloudModel(
            id = "moonshot_v1_8k",
            displayName = "月之暗面 Kimi (moonshot-v1-8k)",
            vendor = "moonshot",
            vendorName = "月之暗面",
            endpoint = "https://api.moonshot.cn/v1/chat/completions",
            model = "moonshot-v1-8k",
            applyUrl = "https://platform.moonshot.cn/console/api-keys",
        ),
    )

    val DEFAULT: CloudModel = ALL.first()

    fun byId(id: String?): CloudModel = ALL.firstOrNull { it.id == id } ?: DEFAULT
}
