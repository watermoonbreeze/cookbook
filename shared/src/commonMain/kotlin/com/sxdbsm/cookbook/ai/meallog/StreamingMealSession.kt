package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : StreamingMealSession
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : B3: 会话 reducer —— 冻结的 StreamingMealRequest 与共享 StreamingMealParser 的编排。
 * <p>
 * 认识的对象：InputSegment / StreamingMealParser / MealStreamDraftMapper。
 * 不认识：ViewModel、Compose、Repository、AutoGenPreview、AiRuntime。
 * 不发网络、不启动协程、不写库。
 * <p>
 * [AI生成] B3 会话层。
 */

/** 分段状态；仅由 StreamingMealSession 写入。[AI生成] */
enum class StreamSegmentState { PENDING, STREAMING, COMPLETED, FAILED, CANCELLED }

/** 跨模块只读会话快照；不含原始响应。[AI生成] */
data class StreamingSessionSnapshot(
    val generationId: String,
    val segmentStates: Map<String, StreamSegmentState>,
    val draft: MealStreamDraft,
    val days: List<DayMealJson>,
    val diagnostics: List<StreamDiagnostic>,
    val hasValidMeals: Boolean,
    val isTerminal: Boolean,
)

/**
 * B3 唯一会话 reducer。[AI生成]
 *
 * 生命周期（由 ViewModel 驱动）：nextSegment() 取段 → onDelta/onCompleted/onFailed 喂事件 →
 * 终态后 nextSegment() 取下一段；全部段终态后 snapshot().isTerminal=true。
 */
class StreamingMealSession(
    val request: StreamingMealRequest,
) {
    private val parser = StreamingMealParser(
        segments = request.segments,
        generationId = request.generationId,
        fallbackDate = request.segments.firstOrNull()?.targetDate ?: request.weekAnchor,
    )

    /** 仅非空分段，按 ordinal 升序。 */
    private val orderedSegments = request.nonBlankSegments.sortedBy { it.ordinal }

    private var currentIndex = -1
    private val segmentStates = linkedMapOf<String, StreamSegmentState>()
    private val segmentFailures = mutableMapOf<String, String>()
    private var cancelled = false

    val generationId: String get() = request.generationId

    /**
     * 返回下一段应请求的 segment；无更多段返回 null。
     *
     * 仅在尚未开始，或当前状态为 COMPLETED/FAILED 时返回下一段；
     * CANCELLED 后永远返回 null。
     */
    fun nextSegment(): InputSegment? {
        if (cancelled) return null
        // 当前段尚未终态（STREAMING）时不得推进
        val current = orderedSegments.getOrNull(currentIndex)
        if (current != null) {
            val curState = segmentStates[current.segmentId]
            if (curState == StreamSegmentState.STREAMING || curState == StreamSegmentState.PENDING) return null
        }
        val next = currentIndex + 1
        if (next >= orderedSegments.size) return null
        currentIndex = next
        val seg = orderedSegments[next]
        segmentStates[seg.segmentId] = StreamSegmentState.STREAMING
        return seg
    }

    /** 当前正在流式的 segmentId（无则 null）。 */
    fun currentSegmentId(): String? = orderedSegments.getOrNull(currentIndex)?.segmentId

    /** 接收 Delta 增量文本；仅当属于当前 STREAMING 段才喂给 parser。 */
    fun onDelta(segmentId: String, text: String) {
        if (segmentStates[segmentId] != StreamSegmentState.STREAMING) return
        if (text.isEmpty()) return
        parser.feedDelta(text)
    }

    /** 当前段网络完成；将缓冲尾部与整体 JSON fallback 交给 parser 收尾。 */
    fun onCompleted(segmentId: String, finishReason: String) {
        if (segmentStates[segmentId] != StreamSegmentState.STREAMING) return
        parser.finish(finishReason)
        segmentStates[segmentId] = StreamSegmentState.COMPLETED
    }

    /** 当前段失败；记录诊断，不写 parser。 */
    fun onFailed(segmentId: String, message: String) {
        if (segmentStates[segmentId] != StreamSegmentState.STREAMING) return
        segmentStates[segmentId] = StreamSegmentState.FAILED
        segmentFailures[segmentId] = message
    }

    /** 取消整个会话：当前 STREAMING 段标记 CANCELLED，后续不再接受任何段。 */
    fun cancel() {
        cancelled = true
        val cur = orderedSegments.getOrNull(currentIndex)
        if (cur != null && segmentStates[cur.segmentId] == StreamSegmentState.STREAMING) {
            segmentStates[cur.segmentId] = StreamSegmentState.CANCELLED
        }
    }

    /** 不可变快照。 */
    fun snapshot(): StreamingSessionSnapshot {
        val draft = parser.currentDraft
        val days = MealStreamDraftMapper.toDayMealJson(draft, request.segments)
        val diagnostics = (
            draft.diagnostics +
                segmentFailures.map { (seg, msg) ->
                    StreamDiagnostic(DiagnosticLevel.ERROR, seg, null, null, msg)
                }
            ).toList()
        return StreamingSessionSnapshot(
            generationId = request.generationId,
            segmentStates = segmentStates.toMap(),
            draft = draft,
            days = days,
            diagnostics = diagnostics,
            hasValidMeals = days.any { day -> day.meals.any { it.dishes.isNotEmpty() } },
            isTerminal = orderedSegments.all { segmentStates[it.segmentId] in TERMINAL_STATES },
        )
    }

    private companion object {
        val TERMINAL_STATES = setOf(
            StreamSegmentState.COMPLETED, StreamSegmentState.FAILED, StreamSegmentState.CANCELLED,
        )
    }
}
