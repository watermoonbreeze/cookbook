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
}
