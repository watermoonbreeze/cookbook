package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : SeasoningDefaultsTest
 * @Time : 2026/07/16
 * @Author : SXD-AI
 * @Desc : 配料默认克数——调料给正常用量、普通食材按食物大类给经验默认(#31)、名字带"油"的菜不误缩
 * <p>
 * [AI修改] #31：普通食材由"恒100g"改为按 FoodGroup 分类给经验默认(蛋50/菜150/奶200…)，
 * 守"调料默认克数"红线不变(只有分类判定为调料才缩小)、油菜等仍归蔬菜不被当油误缩。
 **/
class SeasoningDefaultsTest {

    @Test
    fun `普通食材按食物大类给经验默认克数`() {
        // #31：不同大类不同经验值(非都100g)
        assertEquals(50, SeasoningDefaults.defaultGramFor("鸡蛋", isSeasoning = false))   // 蛋≈1个
        assertEquals(200, SeasoningDefaults.defaultGramFor("牛奶", isSeasoning = false))  // 奶一杯
        assertEquals(150, SeasoningDefaults.defaultGramFor("五花肉", isSeasoning = false)) // 红肉
        assertEquals(150, SeasoningDefaults.defaultGramFor("鸡胸肉", isSeasoning = false)) // 禽肉
        assertEquals(150, SeasoningDefaults.defaultGramFor("苹果", isSeasoning = false))   // 水果
        assertEquals(150, SeasoningDefaults.defaultGramFor("豆腐", isSeasoning = false))   // 豆制品
    }

    @Test
    fun `名字带油的菜归蔬菜150_不被当油误缩`() {
        // 守红线：油菜/油麦菜是蔬菜(endsWith菜)，非调料油，给蔬菜经验值 150、绝不缩成调料小值。
        assertEquals(150, SeasoningDefaults.defaultGramFor("油菜", isSeasoning = false))
        assertEquals(150, SeasoningDefaults.defaultGramFor("油麦菜", isSeasoning = false))
    }

    @Test
    fun `分类判不出的普通食材退通用默认100g`() {
        // classify 返 null(启发式未命中，如泛化名/料理包)→ 退 DEFAULT_INGREDIENT_GRAMS。
        assertEquals(SeasoningDefaults.DEFAULT_INGREDIENT_GRAMS, SeasoningDefaults.defaultGramFor("料理包", isSeasoning = false))
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
