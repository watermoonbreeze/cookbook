package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * @File : FreePairingEngineTest
 * @Time : 2026/07/10
 * @Author : SXD-AI
 * @Desc : 离线规则轻搭配引擎单测
 * <p>
 * [AI生成] 待办"自由搭配"一期回归测试。
 **/
class FreePairingEngineTest {

    private fun p(n: String) = PairIngredient(n, PairRole.PROTEIN)
    private fun v(n: String) = PairIngredient(n, PairRole.VEGETABLE)
    private fun egg(n: String) = PairIngredient(n, PairRole.EGG)
    private fun sea(n: String) = PairIngredient(n, PairRole.SEASONING)

    @Test
    fun `荤素搭配并按调味给做法`() {
        val pantry = listOf(p("五花肉"), v("土豆"), sea("老抽"), sea("蚝油"))
        val out = FreePairingEngine.suggest(pantry)
        assertTrue(out.isNotEmpty(), "有荤有素应给出搭配")
        val first = out.first()
        assertEquals(listOf("五花肉", "土豆"), first.items)
        assertEquals("红烧", first.method, "有老抽/蚝油+荤 → 红烧")
    }

    @Test
    fun `无荤时给纯素搭配`() {
        val pantry = listOf(v("青椒"), v("土豆"), sea("盐"))
        val out = FreePairingEngine.suggest(pantry)
        assertTrue(out.isNotEmpty(), "两样素菜应能搭配")
        assertEquals("清炒", out.first().method, "只有盐 → 清炒")
        assertEquals(setOf("青椒", "土豆"), out.first().items.toSet())
    }

    @Test
    fun `蛋与素同炒`() {
        val pantry = listOf(egg("鸡蛋"), v("西红柿"))
        val out = FreePairingEngine.suggest(pantry)
        assertTrue(out.any { it.items.toSet() == setOf("鸡蛋", "西红柿") && it.method == "炒" })
    }

    @Test
    fun `无可搭配食材返回空`() {
        val out = FreePairingEngine.suggest(listOf(sea("盐"), sea("油")))
        assertTrue(out.isEmpty(), "只有调味料无法搭配")
    }

    @Test
    fun `不超过上限且去重`() {
        val pantry = (1..5).map { p("肉$it") } + (1..5).map { v("菜$it") }
        val out = FreePairingEngine.suggest(pantry, maxSuggestions = 6)
        assertTrue(out.size <= 6, "不超过上限")
        assertEquals(out.size, out.toSet().size, "无重复")
        assertFalse(out.isEmpty())
    }
}
