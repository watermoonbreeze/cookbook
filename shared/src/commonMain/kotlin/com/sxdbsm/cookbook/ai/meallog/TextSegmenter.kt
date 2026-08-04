package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate

/**
 * @File : TextSegmenter
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 多天文本分段器——按星期/日期行切分用户输入为多个日期块
 * <p>
 * 分隔符：行首出现星期/日期标记即视为新一天开始。
 * 连续无标记的餐食行归到上一个已识别天的 same-day 块。
 * 纯函数，可单测。
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：多天分段层。
 * [AI修改] 自动化基础能力层 P2-1：补 K1c weekday→date_offset 推算。
 **/

/** 一个日期文本块。[AI生成] */
data class RawDayBlock(
    val text: String,          // 该天的餐食文本（去掉了星期前缀行）
    val weekdayHint: String?,  // "周一"/"星期一"/"礼拜一" 等
    val dateHint: String?,     // "7/28"/"7月28日"/"2026-07-28"
    val indexInInput: Int,     // 在原输入中的序号（0-based）
)

object TextSegmenter {

    /**
     * 按星期/日期行将文本切分为多个日期块。[AI生成]
     *
     * 分隔逻辑：
     * - 某行以星期/日期关键词开头 → 新一天开始
     * - 没有识别到任何日期行 → 整段归为一个块（weekdayHint=null, dateHint=null）
     *
     * @param input 已归一化的用户文本（建议先过 TextNormalizer.normalize）
     * @return 至少 1 个 RawDayBlock
     */
    fun segment(input: String): List<RawDayBlock> {
        val lines = input.lines()
        if (lines.isEmpty()) return listOf(RawDayBlock("", null, null, 0))

        val blocks = mutableListOf<RawDayBlock>()
        var currentLines = mutableListOf<String>()
        var currentWeekday: String? = null
        var currentDate: String? = null
        var blockIndex = 0

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                // 空行：如果在已收集内容中，保留为段落分隔；如果当前块尚无内容，跳过
                if (currentLines.isNotEmpty()) {
                    currentLines.add("")
                }
                continue
            }

            val weekday = extractWeekday(trimmed)
            val date = extractDate(trimmed)

            if (weekday != null || date != null) {
                // 新一天开始→先保存上一个块
                if (currentLines.isNotEmpty()) {
                    val text = currentLines.joinToString("\n").trim()
                    if (text.isNotBlank()) {
                        blocks.add(RawDayBlock(text, currentWeekday, currentDate, blockIndex++))
                    }
                }
                currentLines = mutableListOf()
                currentWeekday = weekday
                currentDate = date
                // 移除星期行中已被识别的部分，剩余部分（如"周一 7/28"中日期部分）作为提示保留
                val remaining = removeWeekdayPrefix(trimmed, weekday, date)
                if (remaining.isNotBlank()) {
                    currentLines.add(remaining)
                }
            } else {
                // 属于当前天的餐食行
                currentLines.add(trimmed)
            }
        }

        // 最后一个块
        val text = currentLines.joinToString("\n").trim()
        if (text.isNotBlank() || blocks.isEmpty()) {
            blocks.add(RawDayBlock(text, currentWeekday, currentDate, blockIndex))
        }

        return blocks.ifEmpty { listOf(RawDayBlock(input, null, null, 0)) }
    }

    // ═══════════════════════════════════════════════
    // 星期/日期提取（内部）
    // ═══════════════════════════════════════════════

    /** 从行首提取星期标识。[AI生成] */
    private fun extractWeekday(line: String): String? {
        val patterns = listOf(
            // 完整格式（长→短匹配）
            Regex("""^(周[一二三四五六日天1-7]|星期[一二三四五六日天1-7]|礼拜[一二三四五六日天1-7])"""),
            // 方言简称
            Regex("""^(拜[一二三四五六日天])"""),
            // 英文
            Regex("""^(Mon(day)?|Tue(sday)?|Wed(nesday)?|Thu(rsday)?|Fri(day)?|Sat(urday)?|Sun(day)?)""", RegexOption.IGNORE_CASE),
        )
        for (p in patterns) {
            p.find(line.trimStart())?.let { return it.value }
        }
        return null
    }

    /** 从行首提取日期标识。[AI生成] */
    private fun extractDate(line: String): String? {
        val patterns = listOf(
            Regex("""^\d{4}[-/]\d{1,2}[-/]\d{1,2}"""),   // 2026-07-29
            Regex("""^\d{1,2}月\d{1,2}[日号]"""),         // 7月29日
            Regex("""^[一二三四五六七八九十两〇零]+月[一二三四五六七八九十两〇零]+[日号]"""), // 八月十五号
            Regex("""^\d{1,2}[-/]\d{1,2}"""),              // 7/29
        )
        for (p in patterns) {
            p.find(line.trimStart())?.let { return it.value }
        }
        return null
    }

    /** 移除行首已经被识别为星期/日期的部分，返回剩余文本。[AI生成] */
    private fun removeWeekdayPrefix(line: String, weekday: String?, date: String?): String {
        var result = line.trimStart()
        weekday?.let { result = result.removePrefix(it).trimStart() }
        date?.let { result = result.removePrefix(it).trimStart() }
        // 移除分隔符（空格、冒号等）
        result = result.trimStart(' ', ':', '：', '-', '—', '~')
        return result
    }

    // ═══════════════════════════════════════════════
    // 星期→偏移推算（public，供 RuleMealParser 用）
    // ═══════════════════════════════════════════════

    /** 星期中文名→ISO weekday（1=周一..7=周日）。[AI生成] */
    fun weekdayToIso(weekdayHint: String?): Int? {
        if (weekdayHint == null) return null
        return when {
            weekdayHint.contains(Regex("""[一1]""")) -> 1
            weekdayHint.contains(Regex("""[二2两]""")) -> 2
            weekdayHint.contains(Regex("""[三3]""")) -> 3
            weekdayHint.contains(Regex("""[四4]""")) -> 4
            weekdayHint.contains(Regex("""[五5]""")) -> 5
            weekdayHint.contains(Regex("""[六6]""")) -> 6
            weekdayHint.contains(Regex("""[日天7]""")) -> 7
            // 英文
            weekdayHint.lowercase().contains("mon") -> 1
            weekdayHint.lowercase().contains("tue") -> 2
            weekdayHint.lowercase().contains("wed") -> 3
            weekdayHint.lowercase().contains("thu") -> 4
            weekdayHint.lowercase().contains("fri") -> 5
            weekdayHint.lowercase().contains("sat") -> 6
            weekdayHint.lowercase().contains("sun") -> 7
            else -> null
        }
    }

    // ═══════════════════════════════════════════════════
    // K1c: weekday → date_offset 推算（P2-1）
    // ═══════════════════════════════════════════════════

    /**
     * 星期几 + today → 最近过去的该星期几的 date_offset。[AI生成] P2-1
     *
     * "今天周四说周三吃了"→ 周三在周四的 -1 天 → offset=-1。
     * 注意：offset<=0（不说未来"下周一吃了"），取"最近的过去"。
     *
     * @param weekday ISO weekday (1=周一..7=周日)
     * @param today 当前日期
     * @return date_offset（负数或0）
     */
    fun weekdayToDateOffset(weekday: Int, today: LocalDate): Int {
        val todayDow = today.dayOfWeek.ordinal + 1 // 1=Mon..7=Sun
        val diff = todayDow - weekday
        return if (diff >= 0) -diff else -(7 + diff) // 最近过去
    }

    /**
     * 从中文文本解析星期几（"周三"/"上周五"/"礼拜天"等）。[AI生成] P2-1
     *
     * 只解析近过去/今天/昨天的口语表达，返回 ISO weekday (1=周一..7=周日)。
     * "上周X"→按 -7 偏移但不改变 weekday 本身（偏移由上层处理）。
     *
     * @return ISO weekday (1-7) 或 null（解析不出）
     */
    fun parseWeekdayHint(text: String): Int? {
        val t = text.trim()
        // 先查"今天"/"昨天"/"前天"等绝对偏移词
        if (t.contains("今天") || t == "今") return null // 今天=offset 0，上层按 date 处理
        if (t.contains("昨天") || t.contains("昨日")) return null

        return weekdayToIso(t)
    }
}
