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

    /** 组装 chat/completions 请求体 JSON。[AI生成] R3:jsonMode=true 时加 response_format 强约束 JSON 输出(仅支持的模型开)。 */
    fun buildRequestBody(model: String, system: String, user: String, temperature: Double, jsonMode: Boolean = false): String =
        json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = model,
                temperature = temperature,
                messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
                response_format = if (jsonMode) ResponseFormat("json_object") else null, // null→encodeDefaults=false 不序列化(老模型不带此字段)
            ),
        )

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
    )

    @Serializable
    private data class ResponseFormat(val type: String)

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage? = null)
}
