package com.sxdbsm.cookbook.domain

/**
 * @File : DishNameIngredientGuesser
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 自建菜品按**菜名**推演可能的食材（如"土豆牛腩"→[土豆, 牛腩]），供用户确认加入
 * <p>
 * 从已有食材名里找**出现在菜名中的**（子串匹配，长名优先、不重叠），推出候选食材名让用户勾选确认。
 * 纯本地、纯函数、可单测。只推"已有食材"，避免误建；≥2 字防单字噪音(蛋/油/盐)。**用户确认后才加**，不自动加。
 * <p>
 * [AI生成] 用户 2026-07-18：自建菜品输"土豆牛腩"要推出土豆、牛腩让用户确认。
 **/
object DishNameIngredientGuesser {

    /**
     * 从菜名推演候选食材名。[AI生成]
     *
     * @param dishName 菜名（如"土豆牛腩"/"西红柿炒鸡蛋"）
     * @param ingredientNames 已有食材名（预设+自建），只从中匹配（不编造新食材）
     * @return 命中的食材名，按在菜名中出现的先后排序；长名优先、匹配区不重叠（"土豆"命中则其中的"豆"不再单独算）
     */
    fun guess(dishName: String, ingredientNames: List<String>): List<String> {
        val name = dishName.trim().replace(" ", "")
        if (name.length < 2) return emptyList()
        // 候选：≥2 字、去重、按长度降序（长名先占位，防"牛肉"被"肉"抢、"土豆"被"豆"拆）。
        val cands = ingredientNames.asSequence()
            .map { it.trim().replace(" ", "") }
            .filter { it.length >= 2 }
            .distinct()
            .sortedByDescending { it.length }
        val used = BooleanArray(name.length)
        val hit = mutableSetOf<String>()
        for (c in cands) {
            var idx = name.indexOf(c)
            while (idx >= 0) {
                val range = idx until idx + c.length
                if (range.none { used[it] }) { // 该区间未被更长的候选占用
                    for (i in range) used[i] = true
                    hit += c
                    break // 一次命中即可（同名多处只算一个食材）
                }
                idx = name.indexOf(c, idx + 1)
            }
        }
        return hit.sortedBy { name.indexOf(it) }
    }
}
