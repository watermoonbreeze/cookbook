package com.sxdbsm.cookbook.domain

/**
 * @File : DishNameIngredientGuesser
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 自建菜品按**菜名**推演可能的食材（如"土豆牛腩"→[土豆, 牛腩]），供用户确认加入
 * <p>
 * 从已有食材名里找**出现在菜名中的**（子串匹配，长名优先、不重叠）；剩余未匹配的连续中文段
 * 剔除烹饪方式/口味/刀工等停用词后，作"库外候选(待自建)"提出（保存菜品时自动创建到食材库）。
 * 纯本地、纯函数、可单测。≥2 字防单字噪音(蛋/油/盐)。
 * <p>
 * [AI生成] 用户 2026-07-18：自建菜品输"土豆牛腩"要推出土豆、牛腩。
 * [AI修改] 用户 2026-07-18：推演还要能识别"库里没有的食材"标"待自建"、保存时自动建（`guessDetailed`）。
 **/
object DishNameIngredientGuesser {

    /** 推演出的一味食材：`name` 名称，`inLibrary` 是否已在食材库（false=待自建，保存时创建）。[AI生成] */
    data class GuessedIngredient(val name: String, val inLibrary: Boolean)

    /**
     * 停用词：烹饪做法/口味/风格等**明确不是食材**的词，防止被当成"库外食材"误提。[AI生成]
     *
     * 只收多字词（单字靠 ≥2 过滤天然排除）；刻意**不含**本身是食材的词（冰糖/红油/咖喱等），宁漏勿误。
     * 烹饪方式字典里的词由调用方通过 `extraStopWords` 传入补充（避免此处与 DB 字典重复维护）。
     */
    private val STOP_WORDS = setOf(
        "红烧", "清蒸", "糖醋", "水煮", "干煸", "干锅", "爆炒", "小炒", "清炒", "炝炒",
        "凉拌", "白灼", "油焖", "红焖", "黄焖", "葱爆", "酱爆", "葱烧", "蒜蓉", "蒜香",
        "家常", "农家", "风味", "秘制", "私房", "孜然", "椒盐", "香辣", "麻辣", "麻婆",
        "鱼香", "宫保", "京酱", "五香", "十三香", "手撕", "水晶", "翡翠",
    )

    /**
     * 从菜名推演候选食材名（仅库内命中）。[AI生成] 兼容旧调用。
     *
     * @param dishName 菜名（如"土豆牛腩"）
     * @param ingredientNames 已有食材名（预设+自建）
     * @return 命中的库内食材名，按在菜名中出现的先后排序
     */
    fun guess(dishName: String, ingredientNames: List<String>): List<String> =
        guessDetailed(dishName, ingredientNames).filter { it.inLibrary }.map { it.name }

    /**
     * 从菜名推演候选食材（库内 + 库外待自建）。[AI生成]
     *
     * 算法：①库内食材名长名优先占位子串匹配（防"牛肉"被"肉"抢、"土豆"被"豆"拆）；
     * ②剔除停用词占位；③剩余连续中文段(≥2 字)作库外候选(待自建)。按出现先后排序。
     *
     * @param dishName 菜名
     * @param ingredientNames 已有食材名（预设+自建），库内命中 `inLibrary=true`
     * @param extraStopWords 额外停用词（如烹饪方式字典），与内置 [STOP_WORDS] 合并
     */
    fun guessDetailed(
        dishName: String,
        ingredientNames: List<String>,
        extraStopWords: List<String> = emptyList(),
    ): List<GuessedIngredient> {
        val name = dishName.trim().replace(" ", "")
        if (name.length < 2) return emptyList()
        val used = BooleanArray(name.length)
        val hits = mutableListOf<Pair<Int, GuessedIngredient>>() // (起始下标, 候选) 便于按出现先后排序

        // ① 库内食材：≥2 字、去重、长名先占位
        val libCands = ingredientNames.asSequence()
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
                    hits += idx to GuessedIngredient(c, inLibrary = true)
                    break // 同名多处只算一个食材
                }
                idx = name.indexOf(c, idx + 1)
            }
        }

        // ② 剔除停用词（长词先占，防"香辣"被短词拆）——占位但不作为候选
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

        // ③ 剩余连续中文段(≥2 字) → 库外候选(待自建)
        var i = 0
        while (i < name.length) {
            if (!used[i] && name[i].isCjk()) {
                var j = i
                while (j < name.length && !used[j] && name[j].isCjk()) j++
                val seg = name.substring(i, j)
                if (seg.length >= 2) hits += i to GuessedIngredient(seg, inLibrary = false)
                i = j
            } else {
                i++
            }
        }

        return hits.sortedBy { it.first }.map { it.second }
    }

    /** 是否 CJK 中文字符（基本区）。[AI生成] 非中文(数字/字母/符号)不作库外食材切分。 */
    private fun Char.isCjk(): Boolean = this in '一'..'鿿'
}
