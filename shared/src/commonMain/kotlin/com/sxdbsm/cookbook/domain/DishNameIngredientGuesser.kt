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
     * 从菜名推演候选食材名（仅库内命中）。[AI生成] 兼容旧调用。
     *
     * [AI修改] K2 重构：委托给通用 IngredientNameExtractor。
     */
    fun guess(dishName: String, ingredientNames: List<String>): List<String> =
        guessDetailed(dishName, ingredientNames).filter { it.inLibrary }.map { it.name }

    /**
     * 从菜名推演候选食材（库内 + 库外待自建）。[AI生成]
     *
     * [AI修改] K2 重构：委托给通用 IngredientNameExtractor，核心算法不再内联。
     */
    fun guessDetailed(
        dishName: String,
        ingredientNames: List<String>,
        extraStopWords: List<String> = emptyList(),
    ): List<GuessedIngredient> {
        val extracted = IngredientNameExtractor.extract(dishName, ingredientNames, extraStopWords)
        return extracted.map { GuessedIngredient(it.name, it.inLibrary) }
    }
}
