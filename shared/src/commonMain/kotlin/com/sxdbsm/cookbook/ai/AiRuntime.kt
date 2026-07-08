package com.sxdbsm.cookbook.ai

/**
 * @File : AiRuntime
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : AI 模型运行时抽象（云/端中立）
 * <p>
 * 业务层只依赖本接口，不关心背后是云端 API 还是端侧小模型。切换实现不动业务代码——
 * 首轮 CloudAiRuntime(免费云端)，后续 OnDeviceAiRuntime(端侧隐私版)。
 * <p>
 * [AI生成] S1：把“模型怎么来”隔离到接口后，Orchestrator 面向接口编排。
 **/
interface AiRuntime {
    /** 单轮补全：输入系统/用户提示，输出模型原始文本（期望是 JSON）。失败返回 Result.failure。[AI生成] */
    suspend fun complete(request: LlmRequest): Result<String>
}

/** 一次模型请求。[AI生成] */
data class LlmRequest(
    val system: String,
    val user: String,
    val temperature: Double = 0.3, // 推荐类需要稳定/可控，温度偏低。
    val maxTokens: Int = 1024,
)

/**
 * Mock 运行时：默认返回空 → Orchestrator 走规则兜底；测试可注入固定 JSON 验证模型链路。[AI生成]
 *
 * S1 用它把“规则候选 → 模型组合”的全链路跑通，不联网、不花钱。
 */
class MockAiRuntime(private val cannedResponse: String? = null) : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> =
        Result.success(cannedResponse ?: "")
}
