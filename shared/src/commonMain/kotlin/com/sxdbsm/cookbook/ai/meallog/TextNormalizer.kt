package com.sxdbsm.cookbook.ai.meallog

/**
 * @File : TextNormalizer
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 文本预处理工具——统一全角/半角、标点归一、空行合并
 * <p>
 * 纯函数，无副作用，可单测。规则引擎和 AI Prompt 输入前共用此归一化步骤。
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：文本预处理层。
 **/
object TextNormalizer {

    /**
     * 归一化用户输入文本。[AI生成]
     *
     * 处理步骤：
     * 1. 全角标点→半角（，。；：（）＋ → ,.;:()+）
     * 2. 全角数字/字母→半角
     * 3. 合并连续空白行为单个换行
     * 4. 去除首尾空白行
     */
    fun normalize(input: String): String {
        var t = input.replace("\r\n", "\n").replace("\r", "\n")

        // 1. 全角标点→半角
        t = t.replace("，", ",")   // ，
            .replace("。", ".")    // 。
            .replace("；", ";")    // ；
            .replace("：", ":")    // ：
            .replace("（", "(")    // （
            .replace("）", ")")    // ）
            .replace("＋", "+")    // ＋
            .replace("、", ",")    // 、→, 以便后续统一分隔
            .replace("“", "\"")   // "
            .replace("”", "\"")   // "

        // 2. 全角数字→半角
        val sb = StringBuilder(t.length)
        for (ch in t) {
            sb.append(
                when {
                    ch in '０'..'９' -> (ch - '０' + '0'.code).toChar()
                    ch in 'Ａ'..'Ｚ' -> (ch - 'Ａ' + 'A'.code).toChar()
                    ch in 'ａ'..'ｚ' -> (ch - 'ａ' + 'a'.code).toChar()
                    ch == '　' -> ' ' // 全角空格
                    else -> ch
                }
            )
        }
        t = sb.toString()

        // 3. 合并 3+ 连续空行为 2 空行（保留段落分隔）
        t = t.replace(Regex("""\n{3,}"""), "\n\n")

        // 4. 首尾 trim
        t = t.trim()

        // 5. 去除行首行尾多余空白
        t = t.lines().joinToString("\n") { it.trim() }

        return t
    }

    /**
     * 快速归一化（仅标点，不做全角数字转换，用于展示场景）。[AI生成]
     */
    fun normalizeLight(input: String): String {
        return input.replace("\r\n", "\n").replace("\r", "\n")
            .replace("，", ",")
            .replace("、", ",")
            .replace("＋", "+")
            .trim()
    }
}
