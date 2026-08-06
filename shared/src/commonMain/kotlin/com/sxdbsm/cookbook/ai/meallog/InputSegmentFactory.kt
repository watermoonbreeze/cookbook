package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate

/**
 * @File : InputSegmentFactory
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : InputSegment 纯函数工厂——快速记/周期记的 segment 构造入口。
 * <p>
 * segmentId 命名规则 + weekAnchor 推算 + ordinal 排序在此单一工厂内保证一致，
 * 消除调用方手动拼字符串的漂移风险。
 * <p>
 * [AI生成] B4 输入 UI 改造：架构模型终审 S6 + Google 架构工程师复审确认。
 * 替代 ViewModel 私有的 startOfWeek()，消除双真相源。
 */

object InputSegmentFactory {

    /**
     * 快速记：单一 anchor 日期产生 1 个 segment。
     * segmentId = "quick-{anchorDate}"
     */
    fun forQuickRecord(
        inputText: String,
        anchorDate: LocalDate,
    ): List<InputSegment> = listOf(
        InputSegment(
            segmentId = "quick-$anchorDate",
            targetDate = anchorDate,
            inputText = inputText.trim(),
            ordinal = 0,
        )
    )

    /**
     * 周期记：锚点日期所在周一为锚，产生该周 7 天（周一~周日）的 segments。
     * segmentId = "week-{weekAnchorDate}-day{1..7}"
     *
     * @param dayTexts 7 个字符串，index 0=Monday, ..., index 6=Sunday
     * @param weekAnchorDate 该周周一
     * @return 7 个 InputSegment（含空白段；由调用方通过 StreamingMealRequest.nonBlankSegments 过滤）
     */
    fun forPeriodicRecord(
        dayTexts: List<String>,
        weekAnchorDate: LocalDate,
    ): List<InputSegment> {
        require(dayTexts.size == 7) { "dayTexts must have exactly 7 elements, got ${dayTexts.size}" }
        return dayTexts.mapIndexed { index, text ->
            InputSegment(
                segmentId = "week-${weekAnchorDate}-day${index + 1}",
                targetDate = DateTime.plusDays(weekAnchorDate, index),
                inputText = text.trim(),
                ordinal = index,
            )
        }
    }

    /**
     * 推算某日期所在周的周一（ISO 周一）。
     * 跨年周（如 2025-12-29 周一 → 周日 2026-01-04）正确处理。
     *
     * @param date 任意日期
     * @return 该日期所在周的周一
     */
    fun mondayOfWeek(date: LocalDate): LocalDate {
        val dayOfWeek = date.dayOfWeek.ordinal  // 0=Mon, ..., 6=Sun
        return DateTime.plusDays(date, -dayOfWeek)
    }
}
