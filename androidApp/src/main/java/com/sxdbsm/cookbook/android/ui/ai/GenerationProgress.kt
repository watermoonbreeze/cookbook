package com.sxdbsm.cookbook.android.ui.ai

/**
 * @File : GenerationProgress
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B5 生成进度——UI 友好的段进度封装。由 ViewModel 从 segmentStates 推导。
 * <p>
 * [AI生成] B5 确认页流式展示。
 */
data class GenerationProgress(
    /** 非空段总数。 */
    val totalSegments: Int,
    /** 已完成段数（COMPLETED）。 */
    val completedSegments: Int,
    /** 已失败段数（FAILED）。 */
    val failedSegments: Int,
    /** 当前正在流的段 ordinal（0-based）。 */
    val currentSegmentOrdinal: Int,
    /** 当前段人读标签，如"周一 8/4"。 */
    val currentSegmentLabel: String,
) {
    /** 已终态段数 = completed + failed。 */
    val terminalSegments: Int get() = completedSegments + failedSegments

    /** 进度 0.0~1.0。 */
    val progress: Float get() =
        if (totalSegments > 0) terminalSegments.toFloat() / totalSegments else 0f
}
