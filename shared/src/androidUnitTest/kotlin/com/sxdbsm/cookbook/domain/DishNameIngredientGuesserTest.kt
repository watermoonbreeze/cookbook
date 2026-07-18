package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : DishNameIngredientGuesserTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 菜名推食材——长名优先不重叠、按出现序、只从已有食材、单字噪音不推
 * <p>
 * [AI生成] 守：土豆牛腩→[土豆,牛腩]，不把"豆"单独推出；未知菜名不硬凑。
 **/
class DishNameIngredientGuesserTest {

    private val lib = listOf("土豆", "牛腩", "牛肉", "豆", "西红柿", "鸡蛋", "蛋", "青椒", "肉丝", "米饭", "油", "盐")

    @Test
    fun `土豆牛腩推出土豆和牛腩`() {
        assertEquals(listOf("土豆", "牛腩"), DishNameIngredientGuesser.guess("土豆牛腩", lib))
    }

    @Test
    fun `西红柿炒鸡蛋_长名优先不拆蛋`() {
        val g = DishNameIngredientGuesser.guess("西红柿炒鸡蛋", lib)
        assertEquals(listOf("西红柿", "鸡蛋"), g, "应命中西红柿+鸡蛋，不把'蛋'再单列: $g")
    }

    @Test
    fun `按出现先后排序`() {
        // 青椒肉丝 → 青椒 在前、肉丝 在后。
        assertEquals(listOf("青椒", "肉丝"), DishNameIngredientGuesser.guess("青椒肉丝", lib))
    }

    @Test
    fun `未命中或太短_返回空`() {
        assertTrue(DishNameIngredientGuesser.guess("红烧天外飞仙", lib).isEmpty())
        assertTrue(DishNameIngredientGuesser.guess("汤", lib).isEmpty())
    }

    @Test
    fun `单字食材不推_防噪音`() {
        // "豆浆" 中不该把库里的单字"豆"推出(≥2字过滤)。
        assertTrue(DishNameIngredientGuesser.guess("豆浆", lib).none { it == "豆" })
    }

    // ========== guessDetailed：库外待自建候选 ==========

    @Test
    fun `库外食材标待自建_inLibrary为false`() {
        // "杏鲍菇" 不在库 → 库外候选(待自建)；"香辣" 是停用词不当食材。
        val g = DishNameIngredientGuesser.guessDetailed("香辣杏鲍菇", lib)
        assertEquals(listOf(DishNameIngredientGuesser.GuessedIngredient("杏鲍菇", false)), g)
    }

    @Test
    fun `混合_库内土豆真_库外神仙腩待自建`() {
        val g = DishNameIngredientGuesser.guessDetailed("土豆神仙腩", lib)
        assertEquals(
            listOf(
                DishNameIngredientGuesser.GuessedIngredient("土豆", true),
                DishNameIngredientGuesser.GuessedIngredient("神仙腩", false),
            ),
            g,
        )
    }

    @Test
    fun `停用词不被当食材`() {
        // 红烧=停用词，只应推出库内牛腩，不推"红烧"。
        val g = DishNameIngredientGuesser.guessDetailed("红烧牛腩", lib)
        assertEquals(listOf(DishNameIngredientGuesser.GuessedIngredient("牛腩", true)), g)
    }

    @Test
    fun `额外停用词_烹饪方式字典生效`() {
        // 传入"外婆"作额外停用词后，"外婆红烧肉"里"外婆"不再被当库外食材(肉是单字被过滤)。
        val g = DishNameIngredientGuesser.guessDetailed("外婆红烧排骨", lib, extraStopWords = listOf("外婆"))
        // 排骨不在 lib → 库外待自建；外婆(停用词)、红烧(内置停用词)均不出现。
        assertEquals(listOf(DishNameIngredientGuesser.GuessedIngredient("排骨", false)), g)
    }
}
