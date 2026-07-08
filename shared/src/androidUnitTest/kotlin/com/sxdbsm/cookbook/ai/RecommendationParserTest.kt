package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : RecommendationParserTest
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 模型输出 JSON 解析单测（合法/夹带文字/垃圾/缺字段）
 * <p>
 * [AI生成] S1：证明解析容错——夹带文字可截取、垃圾返回 null（交由兜底）。
 **/
class RecommendationParserTest {

    @Test
    fun `解析合法JSON`() {
        val json = """{"suggestions":[{"dishIds":[1,2],"reason":"清淡","cookingHint":"清炒"}]}"""
        val result = RecommendationParser.parse(json)
        assertEquals(1, result?.size)
        assertEquals(listOf(1L, 2L), result?.first()?.dishIds)
        assertEquals("清炒", result?.first()?.cookingHint)
    }

    @Test
    fun `夹带多余文字也能截取解析`() {
        val raw = "好的，这是推荐：\n{\"suggestions\":[{\"dishIds\":[3],\"reason\":\"y\"}]}\n希望有帮助"
        val result = RecommendationParser.parse(raw)
        assertEquals(1, result?.size)
        assertEquals(listOf(3L), result?.first()?.dishIds)
    }

    @Test
    fun `缺cookingHint字段可空`() {
        val json = """{"suggestions":[{"dishIds":[1],"reason":"r"}]}"""
        val result = RecommendationParser.parse(json)
        assertNull(result?.first()?.cookingHint)
    }

    @Test
    fun `垃圾文本返回null`() {
        assertNull(RecommendationParser.parse("抱歉我不知道"))
        assertNull(RecommendationParser.parse(""))
    }

    @Test
    fun `空suggestions解析为空列表`() {
        val result = RecommendationParser.parse("""{"suggestions":[]}""")
        assertTrue(result != null && result.isEmpty())
    }
}
