package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : StreamingMealSession
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : B3: 会话 reducer —— 冻结的 StreamingMealRequest 与按 segment 惰性创建的 StreamingMealParser 的编排。
 * <p>
 * 认识的对象：InputSegment / StreamingMealParser / MealStreamDraftMapper。
 * 不认识：ViewModel、Compose、Repository、AutoGenPreview、AiRuntime。
 * 不发网络、不启动协程、不写库。
 * <p>
 * [AI修改] AF-ARCH-02: parser 按 segmentId 惰性创建，每个 segment 独立 parser（单段列表+独立 fallbackDate），
 * snapshot 合并所有 parser 草稿。解决 B4 多段场景下整体 JSON fallback 永久失效、截断标记跨段覆盖、fallback 日期错锚。
 * <p>
 * [AI生成] B3 会话层。
 */

/** 分段状态；仅由 StreamingMealSession 写入。[AI生成] */
enum class StreamSegmentState { STREAMING, COMPLETED, FAILED }

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
    /** 仅非空分段，按 ordinal 升序。 */
    private val orderedSegments = request.nonBlankSegments.sortedBy { it.ordinal }

    private var currentIndex = -1
    private val segmentStates = linkedMapOf<String, StreamSegmentState>()
    private val segmentFailures = mutableMapOf<String, String>()

    /**
     * AF-ARCH-02: 每个 segment 独立 parser，惰性创建。
     * 每个 parser 只持有自己的单个 segment，保证整体 JSON fallback（要求 segments.size==1）始终有效，
     * 且截断标记、fallback 日期相互隔离。
     */
    private val segmentParsers = linkedMapOf<String, StreamingMealParser>()

    val generationId: String get() = request.generationId

    /**
     * 返回下一段应请求的 segment；无更多段返回 null。
     *
     * 仅在尚未开始，或当前状态为 COMPLETED/FAILED 时返回下一段；
     * 当前段尚未终态（STREAMING）时不得推进。
     */
    fun nextSegment(): InputSegment? {
        val current = orderedSegments.getOrNull(currentIndex)
        if (current != null) {
            val curState = segmentStates[current.segmentId]
            if (curState == StreamSegmentState.STREAMING) return null
        }
        val next = currentIndex + 1
        if (next >= orderedSegments.size) return null
        currentIndex = next
        val seg = orderedSegments[next]
        segmentStates[seg.segmentId] = StreamSegmentState.STREAMING
        // AF-ARCH-02: 惰性创建该 segment 的独立 parser
        segmentParsers.getOrPut(seg.segmentId) {
            StreamingMealParser(
                segments = listOf(seg),
                generationId = request.generationId,
                fallbackDate = seg.targetDate,
            )
        }
        return seg
    }

    /** 当前正在流式的 segmentId（无则 null）。 */
    fun currentSegmentId(): String? = orderedSegments.getOrNull(currentIndex)?.segmentId

    /** [AI新增] 该 segmentId 当前是否仍处于 STREAMING（未终态）。区别于 [currentSegmentId]——后者只看下标顺序，
     *  不代表该段还没结束（下标不推进≠仍在流），调用方需要"真的没终态"语义时必须用这个。 */
    fun isStreaming(segmentId: String): Boolean = segmentStates[segmentId] == StreamSegmentState.STREAMING

    /**
     * [AI新增] 该段自身已解析出的 day(s)（只看这一个 segmentId 的草稿，不受其他段影响）。
     * 用于业务层判断"这段是否已有合法 AI 结果"，不依赖日期字符串跨段匹配——AI 可能为该段声明与
     * `targetDate` 不同的绝对日期（用户说"昨天"）或使用非 `-` 分隔符，字符串匹配会漏判。
     * 空列表代表该段尚无合法内容（未开始/进行中/失败/解析出的菜为空）。
     */
    fun daysForSegment(segmentId: String): List<DayMealJson> {
        val seg = orderedSegments.firstOrNull { it.segmentId == segmentId } ?: return emptyList()
        val draftSeg = segmentParsers[segmentId]?.currentDraft?.segments?.get(segmentId) ?: return emptyList()
        return MealStreamDraftMapper.toDayMealJson(
            MealStreamDraft(segments = mapOf(segmentId to draftSeg)),
            listOf(seg),
        )
    }

    /** 接收 Delta 增量文本；仅当属于当前 STREAMING 段才喂给对应 parser。 */
    fun onDelta(segmentId: String, text: String) {
        if (segmentStates[segmentId] != StreamSegmentState.STREAMING) return
        if (text.isEmpty()) return
        segmentParsers[segmentId]?.feedDelta(text)
    }

    /** 当前段网络完成；将缓冲尾部与整体 JSON fallback 交给该段 parser 收尾。 */
    fun onCompleted(segmentId: String, finishReason: String) {
        if (segmentStates[segmentId] != StreamSegmentState.STREAMING) return
        segmentParsers[segmentId]?.finish(finishReason)
        segmentStates[segmentId] = StreamSegmentState.COMPLETED
    }

    /** 当前段失败；记录诊断，不写 parser。 */
    fun onFailed(segmentId: String, message: String) {
        if (segmentStates[segmentId] != StreamSegmentState.STREAMING) return
        segmentStates[segmentId] = StreamSegmentState.FAILED
        segmentFailures[segmentId] = message
    }

    /** 不可变快照。AF-ARCH-02: 合并所有 segment parser 的草稿。 */
    fun snapshot(): StreamingSessionSnapshot {
        // AF-ARCH-02: 合并所有 parser 的 segment draft + diagnostics + finishReason + isTruncated
        val mergedSegments = linkedMapOf<String, SegmentDraft>()
        val mergedDiagnostics = mutableListOf<StreamDiagnostic>()
        var mergedFinishReason: String? = null
        var mergedIsTruncated = false

        for ((_, parser) in segmentParsers) {
            val draft = parser.currentDraft
            mergedSegments.putAll(draft.segments)
            mergedDiagnostics.addAll(draft.diagnostics)
            // "length" 优先级高于其他 finishReason
            if (draft.finishReason != null) {
                mergedFinishReason = if (draft.finishReason == "length") {
                    draft.finishReason
                } else {
                    mergedFinishReason ?: draft.finishReason
                }
            }
            if (draft.isTruncated) mergedIsTruncated = true
        }

        mergedDiagnostics.addAll(
            segmentFailures.map { (seg, msg) ->
                StreamDiagnostic(DiagnosticLevel.ERROR, seg, null, null, msg)
            }
        )

        val mergedDraft = MealStreamDraft(
            segments = mergedSegments,
            diagnostics = mergedDiagnostics,
            finishReason = mergedFinishReason,
            isTruncated = mergedIsTruncated,
        )

        val days = MealStreamDraftMapper.toDayMealJson(mergedDraft, request.segments)

        return StreamingSessionSnapshot(
            generationId = request.generationId,
            segmentStates = segmentStates.toMap(),
            draft = mergedDraft,
            days = days,
            diagnostics = mergedDiagnostics,
            hasValidMeals = days.any { day -> day.meals.any { it.dishes.isNotEmpty() } },
            isTerminal = orderedSegments.all { segmentStates[it.segmentId] in TERMINAL_STATES },
        )
    }

    private companion object {
        val TERMINAL_STATES = setOf(StreamSegmentState.COMPLETED, StreamSegmentState.FAILED)
    }
}
