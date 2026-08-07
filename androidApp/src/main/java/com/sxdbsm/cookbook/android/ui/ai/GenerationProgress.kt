package com.sxdbsm.cookbook.android.ui.ai

import com.sxdbsm.cookbook.ai.meallog.StreamSegmentState

/**
 * @File : GenerationProgress
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B5 生成进度——UI 友好的段进度封装。由 ViewModel 从 segmentStates 推导。
 * <p>
 * [AI生成] B5 确认页流式展示。
 * [AI修改] B6-fix: segmentStatuses 由 VM 直接产出逐段状态列表（GC-17·AF-B456-05 修复）。
 * currentSegmentOrdinal 重命名为 currentSegmentIndex 并改用显示下标空间。
 */
data class GenerationProgress(
    /** 非空段总数。 */
    val totalSegments: Int,
    /** 已完成段数（COMPLETED）。 */
    val completedSegments: Int,
    /** 已失败段数（FAILED）。 */
    val failedSegments: Int,
    /** [AI修改] B6-fix: 当前正在流的段在显示序中的下标（0..totalSegments-1）。原名 currentSegmentOrdinal，现改用显示下标空间（AF-B456-05·INV-B456-R05b）。 */
    val currentSegmentIndex: Int,
    /** 当前段人读标签，如"周一 8/4"。 */
    val currentSegmentLabel: String,
    /** [AI生成] B6-fix: 逐段状态列表（按 nonBlank.sortedBy{ordinal} 顺序），UI 只做 1:1 映射（AF-B456-05·INV-B456-R05a·GC-17）。 */
    val segmentStatuses: List<StreamSegmentState> = emptyList(),
) {
    /** 已终态段数 = completed + failed。 */
    val terminalSegments: Int get() = completedSegments + failedSegments

    /** 进度 0.0~1.0。 */
    val progress: Float get() =
        if (totalSegments > 0) terminalSegments.toFloat() / totalSegments else 0f
}
