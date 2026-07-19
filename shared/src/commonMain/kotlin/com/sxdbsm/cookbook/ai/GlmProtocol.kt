package com.sxdbsm.cookbook.ai

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * @File : GlmProtocol
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : OpenAI 兼容 chat/completions 的请求/响应编解码（放 shared 用 kotlinx.serialization）
 * <p>
 * CloudAiRuntime(androidApp) 只负责 HTTP，JSON 组装与解析交给本对象——因 serialization 依赖在 shared。
 * <p>
 * [AI生成] S2：把云端协议编解码收敛在 shared，平台侧零 serialization 依赖。
 **/
object GlmProtocol {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 组装 chat/completions 请求体 JSON。[AI生成] R3:jsonMode=true 时加 response_format 强约束 JSON 输出(仅支持的模型开)。
     *
     * [AI修改] 修超时+防截断(2026-07-19·会诊)：GLM-4.5/4.6 是混合推理模型、**默认开启动态思考**——
     *   思考链既吃输出 token 预算(易致 JSON 未闭合被截断)、又显著拉长响应时间(用户实测 glm-4.5-flash "超时")。
     *   本任务是"从候选里选+组装"、无需推理，故对 GLM-4.5/4.6 显式传 thinking:{"type":"disabled"} 关闭思考；
     *   并显式设 max_tokens 防话痨截断、让延迟可预期。thinking/max_tokens 为 null 时(其他厂商/老模型)不序列化，兼容。
     */
    fun buildRequestBody(
        model: String,
        system: String,
        user: String,
        temperature: Double,
        jsonMode: Boolean = false,
        maxTokens: Int? = null, // [AI生成] 显式输出上限(须在关思考之后设,否则思考先吃满、JSON 没预算反更易半截)。
    ): String {
        // 仅智谱 GLM-4.5/4.6 系列有 thinking 开关(混合推理·默认开)；老 glm-4-flash 及他厂无此字段，传了可能报错，故精确 gate。
        val disableThinking = model.startsWith("glm-4.5") || model.startsWith("glm-4.6")
        return json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = model,
                temperature = temperature,
                messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
                response_format = if (jsonMode) ResponseFormat("json_object") else null, // null→encodeDefaults=false 不序列化(老模型不带此字段)
                thinking = if (disableThinking) Thinking("disabled") else null, // [AI生成] 关 GLM-4.5/4.6 默认思考(省token/降延迟/防截断)
                max_tokens = maxTokens,
            ),
        )
    }

    /** 从响应 JSON 解析出模型文本；失败/空返回 null。[AI生成] */
    fun parseContent(responseText: String): String? = runCatching {
        json.decodeFromString(ChatResponse.serializer(), responseText)
            .choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
    }.getOrNull()

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        val response_format: ResponseFormat? = null, // [AI生成] R3:JSON 强约束(仅支持的模型带);null 不序列化。
        val thinking: Thinking? = null, // [AI生成] 修超时:GLM-4.5/4.6 关闭默认动态思考(省token/降延迟/防JSON截断);null 不序列化(他厂/老模型无此字段)。
        val max_tokens: Int? = null, // [AI生成] 显式输出上限,防话痨截断+延迟可预期;null 不序列化。
    )

    @Serializable
    private data class ResponseFormat(val type: String)

    @Serializable
    private data class Thinking(val type: String) // [AI生成] 智谱思考开关:{"type":"disabled"} 关闭思考。

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage? = null)
}
