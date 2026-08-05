package com.sxdbsm.cookbook.ai

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf

/**
 * @File : AiRuntime
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 模型运行时抽象（云/端中立）
 * <p>
 * 业务层只依赖本接口，不关心背后是云端 API 还是端侧小模型。切换实现不动业务代码——
 * 首轮 CloudAiRuntime(免费云端)，后续 OnDeviceAiRuntime(端侧隐私版)。
 * <p>
 * [AI修改] B1 周期记+NDJSON流式改造：增加 stream() 流式方法 + LlmStreamEvent 事件类型。
 **/
interface AiRuntime {
    /** 单轮补全：输入系统/用户提示，输出模型原始文本（期望是 JSON）。失败返回 Result.failure。[AI生成] */
    suspend fun complete(request: LlmRequest): Result<String>

    /**
     * 多轮对话补全（AI 对话生成菜品/餐食的前置基建）。[AI生成]
     *
     * **默认实现把多轮折叠成单轮** 调 [complete]（system 段拼接为系统提示、user/assistant 历史平铺进 user 文本带角色前缀），
     * 使**不支持真多轮的运行时（端侧/Mock）也能工作**、且旧业务零改动；**云端可 override 发真 messages 数组**（保留结构、更准）。
     * 失败返回 Result.failure，由上层回退。
     */
    suspend fun chat(request: LlmChatRequest): Result<String> {
        val system = request.messages.filter { it.role == LlmRole.SYSTEM }.joinToString("\n") { it.content }
        val convo = request.messages.filter { it.role != LlmRole.SYSTEM }
            .joinToString("\n") { (if (it.role == LlmRole.USER) "用户: " else "助手: ") + it.content }
        return complete(LlmRequest(system = system, user = convo, temperature = request.temperature, maxTokens = request.maxTokens))
    }

    /**
     * 流式补全：返回冷 Flow，收集才发网络；取消收集必须取消 HTTP。[AI生成] B1
     *
     * 默认实现：调 complete() 把完整响应封装为单个 Delta + Completed（不支持流式的运行时 fallback）。
     * 云端实现 override 本方法接 SSE 逐 chunk 发 Delta。
     */
    fun stream(request: LlmRequest): Flow<LlmStreamEvent> = flow {
        val result = complete(request)
        result.fold(
            onSuccess = { text ->
                if (text.isNotEmpty()) emit(LlmStreamEvent.Delta(text))
                emit(LlmStreamEvent.Completed(finishReason = "stop", totalChars = text.length))
            },
            onFailure = { e ->
                emit(LlmStreamEvent.Failed(message = e.message ?: "未知错误", retryable = true))
            },
        )
    }
}

// ============================================================
// 流式事件（B1 新增）
// ============================================================

/** 流式响应事件。[AI生成] B1 */
sealed class LlmStreamEvent {
    /** 模型正文增量文本。[AI生成] */
    data class Delta(val text: String) : LlmStreamEvent()
    /** 流正常结束。[AI生成] */
    data class Completed(val finishReason: String, val totalChars: Int) : LlmStreamEvent()
    /** 流出错。[AI生成] */
    data class Failed(val message: String, val httpStatus: Int? = null, val retryable: Boolean = false) : LlmStreamEvent()
}

/** 一次模型请求。[AI生成] */
data class LlmRequest(
    val system: String,
    val user: String,
    val temperature: Double = 0.3, // 推荐类需要稳定/可控，温度偏低。
    val maxTokens: Int = 1024,
)

/** 对话消息角色。[AI生成] 多轮对话前置基建。 */
enum class LlmRole { SYSTEM, USER, ASSISTANT }

/** 一条对话消息（多轮）。[AI生成] */
data class LlmMessage(val role: LlmRole, val content: String)

/** 多轮对话请求。[AI生成] messages 按时间序（首条通常 SYSTEM，其后 USER/ASSISTANT 交替）。 */
data class LlmChatRequest(
    val messages: List<LlmMessage>,
    val temperature: Double = 0.4, // 对话生成比推荐略高一点(自然度)，仍偏低可控。
    val maxTokens: Int = 1024,
)

/**
 * Mock 运行时：默认返回空 → Orchestrator 走规则兜底；测试可注入固定 JSON 验证模型链路。[AI生成]
 *
 * S1 用它把"规则候选 → 模型组合"的全链路跑通，不联网、不花钱。
 * [AI修改] B1 增加 stream()：把 cannedResponse 模拟为单 Delta + Completed。
 */
class MockAiRuntime(private val cannedResponse: String? = null) : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> =
        Result.success(cannedResponse ?: "")

    override fun stream(request: LlmRequest): Flow<LlmStreamEvent> {
        val text = cannedResponse ?: ""
        return flowOf(
            LlmStreamEvent.Delta(text),
            LlmStreamEvent.Completed(finishReason = "stop", totalChars = text.length),
        )
    }
}
