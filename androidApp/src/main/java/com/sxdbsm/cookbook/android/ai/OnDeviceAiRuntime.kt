package com.sxdbsm.cookbook.android.ai

import com.sxdbsm.cookbook.ai.AiRuntime
import com.sxdbsm.cookbook.ai.LlmRequest

/**
 * @File : OnDeviceAiRuntime
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 端侧本地模型运行时（占位，待接入 LiteRT-LM）
 * <p>
 * 切换框架已就位：选择 ON_DEVICE 时路由到此。当前未接入模型，返回 failure → Orchestrator 走规则兜底。
 * 后期按 `端侧AI能力接入方案.md` 用 LiteRT-LM 实现 complete() 即可，业务与切换框架都不用改。
 * <p>
 * [AI生成] S2：预留端侧位置，方便后期直接替换。
 **/
class OnDeviceAiRuntime : AiRuntime {
    override suspend fun complete(request: LlmRequest): Result<String> =
        Result.failure(NotImplementedError("端侧模型待接入(LiteRT-LM)，见 端侧AI能力接入方案.md"))
}
