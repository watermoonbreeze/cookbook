package com.sxdbsm.cookbook.platform

/**
 * AI 推荐能力准备层：只描述可追踪的来源、理由模型和用户反馈，不负责推荐决策或持久化。[AI生成]
 */
data class RecommendationTrace(
    val traceId: TraceId,
    val source: String,
    val entryPoint: String,
    val reasonModelVersion: String? = null,
)

data class RecommendationReason(
    val code: String,
    val modelVersion: String? = null,
)

enum class RecommendationFeedbackAction { ACCEPTED, DISMISSED, EDITED }

data class RecommendationFeedback(
    val traceId: TraceId,
    val recommendationKey: String,
    val action: RecommendationFeedbackAction,
    val reasonCode: String? = null,
)
