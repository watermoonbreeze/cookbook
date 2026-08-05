package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate

/**
 * @File : InputSegment
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : AI 记一餐的输入分段——不可变值对象，由 UI 创建，经 Prompt/Runtime/Parser 共享。
 * <p>
 * 快速记 = 单一 InputSegment；周期记 = 每天一个 InputSegment。
 * 发送后不得修改；编辑、切日期、重试都创建新的 generation。
 * <p>
 * [AI生成] B1 周期记+NDJSON流式改造：协议层值对象。
 **/

/** AI 记一餐的一次输入分段。[AI生成] */
data class InputSegment(
    /** 分段标识键：快速记="quick-{targetDate}"，周期记="week-{weekAnchor}-day{1..7}" */
    val segmentId: String,
    /** 该分段的锚点日期（以用户选择的添加页日期/周期为基准） */
    val targetDate: LocalDate,
    /** 用户输入文本（trim 后） */
    val inputText: String,
    /** 在本次 generation 中的序号（0-based），顺序发送 */
    val ordinal: Int,
) {
    /** 空白段不可发送，但可保留在编辑器中。[AI生成] */
    val isBlank: Boolean get() = inputText.isBlank()
    val charCount: Int get() = inputText.length
}

/**
 * 流式餐食请求——一组 InputSegment + 上下文参数。[AI生成]
 *
 * 周期记时包含多天分段；快速记时仅单个分段。
 * 非空分段按 ordinal 顺序单并发请求。
 */
data class StreamingMealRequest(
    val segments: List<InputSegment>,
    /** 生成标识，单调递增；所有流事件携带此 ID */
    val generationId: String,
    /** 周期锚点日期（用于周期记的星期推算），等于最早一个 segment 所在周的周一 */
    val weekAnchor: LocalDate,
    /** 健康上下文（脱敏后的在餐成员健康档案摘要，可选） */
    val healthContext: String? = null,
) {
    val nonBlankSegments: List<InputSegment> get() = segments.filter { !it.isBlank }
}
