package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : SeasoningDefaultsTest
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 配料默认克数——调料给正常用量、普通食材仍100g、名字带"油"的菜不误缩
 * <p>
 * [AI生成] 守"调料默认克数"红线：只有分类判定为调料才缩小默认，油菜等普通食材必须仍100g。
 **/
class SeasoningDefaultsTest {

    @Test
    fun `普通食材恒100g_即使名字含油`() {
        assertEquals(100, SeasoningDefaults.defaultGramFor("油菜", isSeasoning = false))
        assertEquals(100, SeasoningDefaults.defaultGramFor("油麦菜", isSeasoning = false))
        assertEquals(100, SeasoningDefaults.defaultGramFor("五花肉", isSeasoning = false))
        assertEquals(100, SeasoningDefaults.defaultGramFor("糖心苹果", isSeasoning = false))
    }

    @Test
    fun `调料按常见每菜用量`() {
        assertEquals(3, SeasoningDefaults.defaultGramFor("盐", isSeasoning = true))
        assertEquals(3, SeasoningDefaults.defaultGramFor("食盐", isSeasoning = true))
        assertEquals(10, SeasoningDefaults.defaultGramFor("生抽", isSeasoning = true))
        assertEquals(10, SeasoningDefaults.defaultGramFor("酱油", isSeasoning = true))
        assertEquals(10, SeasoningDefaults.defaultGramFor("蚝油", isSeasoning = true)) // 含"油"但先命中蚝油
        assertEquals(10, SeasoningDefaults.defaultGramFor("花生油", isSeasoning = true))
        assertEquals(5, SeasoningDefaults.defaultGramFor("白糖", isSeasoning = true))
        assertEquals(2, SeasoningDefaults.defaultGramFor("胡椒粉", isSeasoning = true))
        assertEquals(15, SeasoningDefaults.defaultGramFor("豆瓣酱", isSeasoning = true))
    }

    @Test
    fun `未命中具体名的调料用通用默认`() {
        assertEquals(SeasoningDefaults.GENERAL_SEASONING_GRAMS, SeasoningDefaults.defaultGramFor("某某调味料", isSeasoning = true))
    }
}
