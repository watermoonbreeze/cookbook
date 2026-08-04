package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate

/**
 * @File : MealDateAnchorPolicy
 * @Time : 2026/08/04
 * @Author : SXD-AI
 * @Desc : AI 记餐日期锚点规则。
 *
 * [AI生成] 解析模型没有权利在用户未给出日期时把设备当天写入结果；添加页选择的日期
 * 是唯一锚点，星期表达则映射到该锚点所在的自然周。
 */
object MealDateAnchorPolicy {

    data class Result(
        val days: List<DayMealJson>,
        val warning: String? = null,
    )

    private val absoluteDatePattern = Regex(
        """\d{4}[-/.]\d{1,2}[-/.]\d{1,2}|\d{1,2}月\d{1,2}(?:日|号)?|[一二三四五六七八九十两〇零]+月[一二三四五六七八九十两〇零]+(?:日|号)""",
    )
    private val weekdayPattern = Regex("""(?:周|星期|礼拜)[一二三四五六日天1-7]""")

    /** 将已解析日期收口到用户输入的日期语义。 */
    fun apply(input: String, targetDate: LocalDate, days: List<DayMealJson>): Result {
        if (days.isEmpty() || absoluteDatePattern.containsMatchIn(input)) return Result(days)

        val inputWeekdays = weekdayPattern.findAll(input).map { it.value }.toList()
        val hasWeekday = inputWeekdays.isNotEmpty() || days.any { it.weekday != null }
        if (hasWeekday) {
            val mondayOffset = targetDate.dayOfWeek.ordinal
            val anchored = days.mapIndexed { index, day ->
                val weekday = TextSegmenter.weekdayToIso(day.weekday)
                    ?: inputWeekdays.getOrNull(index)?.let(TextSegmenter::weekdayToIso)
                if (weekday == null) {
                    day.copy(date = targetDate.toString(), date_offset = 0)
                } else {
                    day.copy(date = null, date_offset = weekday - 1 - mondayOffset)
                }
            }
            return Result(anchored, "当前餐食以选择的餐食日期为参照；星期按该日期所在周计算。")
        }

        return Result(
            days = days.map { it.copy(date = targetDate.toString(), date_offset = 0) },
            warning = "当前餐食以选择的餐食日期为参照。",
        )
    }
}
