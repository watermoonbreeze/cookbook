package com.sxdbsm.cookbook.platform

/** [AI生成] 跨页面治理模型；只承载代码标识，不持有业务数据。 */
enum class DomainState { EMPTY, DRAFT, READY, SAVING, SAVED, FAILED }
enum class PageState { IDLE, LOADING, EDITING, SUBMITTING, COMPLETED, ERROR }
enum class NavigationState { STABLE, LEAVING, RETURNING, MERGING }
enum class StateLifecycleEvent { CREATE, SAVE, RESTORE, MERGE, CLEAR }

data class StateLifecycleContract(
    val domain: DomainState,
    val page: PageState,
    val navigation: NavigationState,
    val events: List<StateLifecycleEvent>,
) {
    fun isValid(): Boolean = events.isNotEmpty() && events.zipWithNext().none { it.first == StateLifecycleEvent.CLEAR }
}

/** [AI生成] 导航三件套：参数只允许稳定代码，结果由受控 key 回传。 */
data class NavigationContract(
    val source: String,
    val destination: String,
    val parameters: Map<String, String> = emptyMap(),
    val resultKey: String? = null,
) {
    fun isSafe(): Boolean = source.isNotBlank() && destination.isNotBlank() &&
        parameters.values.all { StructuredLogJson.sanitizeCode(it) == it }
}

data class ResultContract(
    val source: String,
    val resultType: String,
    val payload: String,
    val timestampEpochMs: Long,
)

/** [AI生成] 推荐能力上下文，仅供解释/反馈和 Trace 使用，不参与推荐排序。 */
data class RecommendationContext(
    val mealType: String,
    val source: String,
    val inventory: List<String> = emptyList(),
    val nutritionGoal: String? = null,
)

data class RecommendationReason(
    val code: String,
    val explanationKey: String? = null,
)

data class FeedbackModel(
    val recommendationKey: String,
    val action: RecommendationFeedbackAction,
    val reasonCode: String? = null,
    val traceId: TraceId? = null,
)
