package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : EatDrinkStripper
 * @Time : 2026/08/02
 * @Author : SXD-AI
 * @Desc : 句首吃/喝动词上下文剥离器——类别化迭代贪心算法
 * <p>
 * 问题：RuleMealParser 去掉餐次关键词后，句首仍可能残留吃/喝动词上下文（"吃了""准备吃""想喝"等）。
 * 这些词既不是菜名也不是食材，需要剥离后才能正确拆分菜品。
 * <p>
 * 语言学模型：句首吃/喝上下文 = [修饰层]* + [动词核(吃/喝)] + [体标记/补语]*
 * 不预计算所有组合（数千种），而是从 pos=0 开始逐词迭代贪心消费。
 * <p>
 * 安全边界：仅句首剥离·动词核必须命中·剥离后为空→保留原文。
 * <p>
 * [AI生成] 算法工程师设计·多角度会审。
 **/
object EatDrinkStripper {

    // ══════════════════════════════════════
    // 词集（每集按长度降序，贪心优先长词）
    // ══════════════════════════════════════

    /** 动词核：必须命中至少一个。[AI生成] */
    private val VERB_CORE = sortByLenDesc("吃", "喝")

    /** 体标记：完成/经历/名物化。[AI生成] */
    private val ASPECT_MARKER = sortByLenDesc("了", "过", "的")

    /** 体补语：量小/尝试·可与体标记叠加（了+些=了些）。[AI生成] */
    private val ASPECT_COMPLEMENT = sortByLenDesc("些", "点", "个")

    /** 修饰子类 A：时间副词。[AI生成] */
    private val TIME_ADVERB = sortByLenDesc("刚才", "刚刚", "刚")

    /** 修饰子类 B：意愿/情态。[AI生成] */
    private val MODALITY = sortByLenDesc("准备", "打算", "想", "要")

    /** 修饰子类 C：衔接副词。[AI生成] */
    private val CONNECTIVE = sortByLenDesc("顺便", "随便", "就", "还", "又", "也", "再")

    /** 修饰子类聚合（按此顺序迭代；如需调优先级可调整列表顺序）。[AI生成] */
    private val MODIFIER_CATEGORIES: List<List<String>> = listOf(
        CONNECTIVE, TIME_ADVERB, MODALITY,
    )

    // ══════════════════════════════════════
    // Public API
    // ══════════════════════════════════════

    /**
     * 从句首剥离吃/喝动词上下文。[AI生成]
     *
     * 流程：① 迭代消费修饰词 → ② 必须消费动词核 → ③ 消费体标记/补语 → 结果判定。
     *
     * @param text 已去掉餐次关键词的文本
     * @return 剥离后文本；未剥离或剥离后为空→返回原文
     */
    fun strip(text: String): String {
        val n = text.length
        var pos = 0
        var verbFound = false

        // ── 阶段 1：预动词修饰层（可选·可多轮叠加） ──
        while (pos < n && !verbFound) {
            // 先试动词核（无修饰词直连动词："吃了" / "喝了"）
            val verbLen = matchLongest(text, pos, VERB_CORE)
            if (verbLen > 0) {
                pos += verbLen
                verbFound = true
                break
            }
            // 再试修饰词
            var matched = false
            for (cat in MODIFIER_CATEGORIES) {
                val len = matchLongest(text, pos, cat)
                if (len > 0) {
                    pos += len
                    matched = true
                    break
                }
            }
            if (!matched) break // 既无动词也无修饰词 → 停止阶段1
        }

        if (!verbFound) return text // 未命中动词核 → 不剥离

        // ── 阶段 2：后动词体标记/补语（可选·可叠加） ──
        while (pos < n) {
            val markerLen = matchLongest(text, pos, ASPECT_MARKER)
            if (markerLen > 0) { pos += markerLen; continue }
            val complLen = matchLongest(text, pos, ASPECT_COMPLEMENT)
            if (complLen > 0) { pos += complLen; continue }
            break
        }

        // ── 结果判定 ──
        if (pos >= n) return text // 全部被剥离（如"吃了"→""）→ 保留原文
        return text.substring(pos).trimStart()
    }

    // ══════════════════════════════════════
    // 内部
    // ══════════════════════════════════════

    /** 在 text[pos] 处匹配 words 中最长前缀，返回匹配长度；0=无匹配。[AI生成] */
    private fun matchLongest(text: String, pos: Int, words: List<String>): Int {
        for (w in words) {
            if (text.regionMatches(pos, w, 0, w.length)) return w.length
        }
        return 0
    }

    /** 按长度降序排列（等长保持传入顺序）。[AI生成] */
    private fun sortByLenDesc(vararg words: String): List<String> =
        words.sortedByDescending { it.length }
}
