package com.sxdbsm.cookbook.domain

/**
 * @File : IngredientNameExtractor
 * @Time : 2026/07/29
 * @Author : SXD-AI
 * @Desc : 通用食材名提取器——从菜名反推所含食材（抽自 DishNameIngredientGuesser）
 * <p>
 * 算法：①库内食材名长名优先占位子串匹配（防"牛肉"被"肉"拆）；②停用词剔除；
 * ③剩余连续中文段(≥2字，不以做法字开头)→库外候选。
 * 纯函数，可单测。供规则引擎和 AI 解析共用。
 * <p>
 * [AI生成] K2 AI快捷输入记餐专项重构：将 DishNameIngredientGuesser 核心算法通用化。
 **/
object IngredientNameExtractor {

    /** 提取结果。[AI生成] */
    data class ExtractedIngredient(
        val name: String,
        val inLibrary: Boolean,   // 是否在库内
    )

    /**
     * 停用词：烹饪做法/口味/风格等**明确不是食材**的词。
     * 只收多字词（单字靠 ≥2 过滤天然排除）；刻意不含本身是食材的词。
     */
    private val STOP_WORDS = setOf(
        "红烧", "清蒸", "糖醋", "水煮", "干煸", "干锅", "爆炒", "小炒", "清炒", "炝炒",
        "凉拌", "白灼", "油焖", "红焖", "黄焖", "葱爆", "酱爆", "葱烧", "蒜蓉", "蒜香",
        "家常", "农家", "风味", "秘制", "私房", "孜然", "椒盐", "香辣", "麻辣", "麻婆",
        "鱼香", "宫保", "京酱", "五香", "十三香", "手撕", "水晶", "翡翠",
        "醋溜", "干烧", "酱烧", "蜜汁", "铁板", "藤椒", "椒麻", "怪味", "软炸", "干炸",
        "油炸", "香煎", "石锅", "砂锅",
    )

    /**
     * 做法/连接字：库外候选若以这些字开头，视为"做法+食材"的脏切分，拒绝提出。
     */
    private val COOKING_LEADING_CHARS = setOf(
        '烧', '炒', '煮', '蒸', '焖', '炖', '煎', '炸', '烤', '卤', '熘', '溜', '爆', '焗',
        '烩', '汆', '涮', '煨', '扒', '焯', '煲', '炝', '拌', '烹', '熬',
        '包', '夹', '酿', '镶', '塞', '裹',
    )

    /**
     * 从菜名提取食材（通用版）。[AI生成]
     *
     * @param dishName 菜名（如"土豆牛腩"）
     * @param libraryNames 库内食材名列表（≥2字，去重）
     * @param extraStopWords 调用方额外停用词（如烹饪方式字典）
     * @return 按在菜名中出现的先后排序
     */
    fun extract(
        dishName: String,
        libraryNames: List<String>,
        extraStopWords: List<String> = emptyList(),
    ): List<ExtractedIngredient> {
        val name = dishName.trim().replace(" ", "")
        if (name.length < 2) return emptyList()

        val used = BooleanArray(name.length)
        val hits = mutableListOf<Pair<Int, ExtractedIngredient>>()

        // ① 库内食材：≥2 字、去重、长名先占位
        val libCands = libraryNames.asSequence()
            .map { it.trim().replace(" ", "") }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
        for (c in libCands) {
            var idx = name.indexOf(c)
            while (idx >= 0) {
                val range = idx until idx + c.length
                if (range.none { used[it] }) {
                    for (i in range) used[i] = true
                    hits += idx to ExtractedIngredient(c, inLibrary = true)
                    break
                }
                idx = name.indexOf(c, idx + 1)
            }
        }

        // ② 剔除停用词（长词先占，防"香辣"被短词拆）
        val stops = (STOP_WORDS + extraStopWords.map { it.trim() }.filter { it.length >= 2 })
            .sortedByDescending { it.length }
        for (sw in stops) {
            var idx = name.indexOf(sw)
            while (idx >= 0) {
                val range = idx until idx + sw.length
                if (range.none { used[it] }) {
                    for (i in range) used[i] = true
                }
                idx = name.indexOf(sw, idx + 1)
            }
        }

        // ③ 剩余连续中文段(≥2字) → 库外候选(待自建)
        var i = 0
        while (i < name.length) {
            if (!used[i] && name[i].isCjk()) {
                var j = i
                while (j < name.length && !used[j] && name[j].isCjk()) j++
                val seg = name.substring(i, j)
                if (seg.length >= 2 && seg.first() !in COOKING_LEADING_CHARS) {
                    hits += i to ExtractedIngredient(seg, inLibrary = false)
                }
                i = j
            } else {
                i++
            }
        }

        return hits.sortedBy { it.first }.map { it.second }
    }

    /**
     * 仅提取库内命中的食材名。[AI生成]
     */
    fun extractInLibrary(
        dishName: String,
        libraryNames: List<String>,
        extraStopWords: List<String> = emptyList(),
    ): List<String> = extract(dishName, libraryNames, extraStopWords)
        .filter { it.inLibrary }.map { it.name }

    /**
     * 仅提取库外候选。[AI生成]
     */
    fun extractOutsideLibrary(
        dishName: String,
        libraryNames: List<String>,
        extraStopWords: List<String> = emptyList(),
    ): List<String> = extract(dishName, libraryNames, extraStopWords)
        .filter { !it.inLibrary }.map { it.name }

    /** 是否 CJK 中文字符（基本区）。[AI生成] */
    private fun Char.isCjk(): Boolean = this in '一'..'鿿'
}
