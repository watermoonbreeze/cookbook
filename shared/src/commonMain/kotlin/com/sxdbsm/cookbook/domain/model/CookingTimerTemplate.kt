package com.sxdbsm.cookbook.domain.model

/**
 * @File : CookingTimerTemplate
 * @Time : 2026/06/12
 * @Author : SXD-AI
 * @Desc : 烹饪计时模板领域模型
 * <p>
 * 保存用户常用的烹饪倒计时配置。运行态、暂停态、响铃态只属于页面临时状态，不进入数据库。
 * <p>
 * [AI生成] 用户要求烹饪计时可本地保存，便于下次烹饪直接复用。
 **/
data class CookingTimerTemplate(
    val id: Long = 0,
    val name: String,
    val durationSeconds: Int,
    val note: String = "",
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "系统默认铃声",
    val sortOrder: Int = 0,
    // [AI生成] 连续多段：空=单段(用 durationSeconds)；≥1 段=按序执行(一段停止后进下一段)。[用户 2026-07-18]
    val segments: List<TimerSegment> = emptyList(),
) {
    /** 实际运行的段序列：多段用 segments，否则退化为单段(用 durationSeconds)。[AI生成] */
    val runSegments: List<TimerSegment>
        get() = if (segments.isNotEmpty()) segments else listOf(TimerSegment("", durationSeconds))
    val isMultiSegment: Boolean get() = segments.size > 1
}

/** 连续倒计时的一段（可选段名 + 时长秒）。[AI生成] 用户 2026-07-18 多段倒计时 */
@kotlinx.serialization.Serializable
data class TimerSegment(
    val name: String = "",
    val seconds: Int,
)
