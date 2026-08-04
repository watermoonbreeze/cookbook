package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.ai.LlmRequest

/** 仅供本次确认页展示的健康建议请求；调用方只能传去标识化摘要。 [AI生成] */
object AiMealHealthAdvice {
    fun request(healthSummary: String, mealSummary: String): LlmRequest = LlmRequest(
        system = """
你是家庭饮食记录助手。只根据提供的去标识化健康摘要和本餐菜品摘要，给出简短、温和、非诊疗的饮食建议。
不得声称治疗、诊断、替代医生意见；信息不足时明确说“食材或营养信息待补充”。不要复述或猜测个人身份、病史、用药、检查结果。
输出纯中文，最多3条建议，末尾必须包含“仅供参考，非医嘱”。
""".trimIndent(),
        user = "健康摘要：$healthSummary\n本餐摘要：$mealSummary",
        temperature = 0.2,
        maxTokens = 500,
    )
}
