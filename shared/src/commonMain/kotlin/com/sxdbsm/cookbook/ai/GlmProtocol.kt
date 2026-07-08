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

    /** 组装 chat/completions 请求体 JSON。[AI生成] */
    fun buildRequestBody(model: String, system: String, user: String, temperature: Double): String =
        json.encodeToString(
            ChatRequest.serializer(),
            ChatRequest(
                model = model,
                temperature = temperature,
                messages = listOf(ChatMessage("system", system), ChatMessage("user", user)),
            ),
        )

    /** 从响应 JSON 解析出模型文本；失败/空返回 null。[AI生成] */
    fun parseContent(responseText: String): String? = runCatching {
        json.decodeFromString(ChatResponse.serializer(), responseText)
            .choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
    }.getOrNull()

    @Serializable
    private data class ChatRequest(val model: String, val messages: List<ChatMessage>, val temperature: Double)

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList())

    @Serializable
    private data class Choice(val message: ChatMessage? = null)
}
