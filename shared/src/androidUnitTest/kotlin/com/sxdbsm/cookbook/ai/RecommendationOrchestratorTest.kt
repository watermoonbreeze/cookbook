package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.HealthConstraints
import com.sxdbsm.cookbook.ai.model.IngredientRole
import com.sxdbsm.cookbook.ai.model.RecommendationInput
import com.sxdbsm.cookbook.ai.model.RecommendationSource
import com.sxdbsm.cookbook.ai.model.RuleDish
import com.sxdbsm.cookbook.ai.model.RuleDishIngredient
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : RecommendationOrchestratorTest
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 推荐编排单测（Mock 模型：模型路径/兜底/非法id过滤/空候选）
 * <p>
 * [AI生成] S1：用 MockAiRuntime 注入固定 JSON，验证解析+校验+映射与兜底，不联网。
 **/
class RecommendationOrchestratorTest {

    private fun main(id: Long, name: String) = RuleDishIngredient(id, name, IngredientRole.MAIN)

    // 两个都可做的合成菜
    private val input = RecommendationInput(
        dishes = listOf(
            RuleDish(1, "西红柿炒鸡蛋", listOf(main(101, "番茄"), main(102, "鸡蛋"))),
            RuleDish(2, "清炒油菜", listOf(main(103, "油菜"))),
        ),
        pantryIngredientIds = setOf(101, 102, 103),
        constraints = HealthConstraints(),
        recentDishIds = emptySet(),
    )

    @Test
    fun `模型返回合法JSON则采用模型建议`() = runBlocking {
        val json = """{"suggestions":[{"dishIds":[1,2],"reason":"清淡搭配","cookingHint":"清炒"}]}"""
        val orch = RecommendationOrchestrator(MockAiRuntime(json))
        val result = orch.recommend(input, mealCount = 3)
        assertEquals(RecommendationSource.MODEL, result.source)
        assertEquals(1, result.suggestions.size)
        assertEquals(listOf(1L, 2L), result.suggestions.first().dishIds)
        assertEquals("清炒", result.suggestions.first().cookingHint)
    }

    @Test
    fun `模型空返回则规则兜底`() = runBlocking {
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 空 → 兜底
        val result = orch.recommend(input, mealCount = 3)
        assertEquals(RecommendationSource.RULE_FALLBACK, result.source)
        assertTrue(result.suggestions.isNotEmpty())
        // 兜底建议里的 dishId 必须来自候选
        val validIds = result.candidates.map { it.id }.toSet()
        assertTrue(result.suggestions.all { s -> s.dishIds.all { it in validIds } })
    }

    @Test
    fun `模型给出候选外的id会被过滤`() = runBlocking {
        val json = """{"suggestions":[{"dishIds":[1,999],"reason":"x"}]}""" // 999 非候选
        val orch = RecommendationOrchestrator(MockAiRuntime(json))
        val result = orch.recommend(input, mealCount = 3)
        assertEquals(RecommendationSource.MODEL, result.source)
        assertEquals(listOf(1L), result.suggestions.first().dishIds) // 999 被剔除
    }

    @Test
    fun `无可做候选返回EMPTY`() = runBlocking {
        val orch = RecommendationOrchestrator(MockAiRuntime())
        // 库存为空 → 没有可做菜
        val emptyInput = input.copy(pantryIngredientIds = emptySet())
        val result = orch.recommend(emptyInput, mealCount = 3)
        assertEquals(RecommendationSource.EMPTY, result.source)
        assertTrue(result.suggestions.isEmpty())
    }

    @Test
    fun `模型返回垃圾文本则兜底`() = runBlocking {
        val orch = RecommendationOrchestrator(MockAiRuntime("抱歉我不知道"))
        val result = orch.recommend(input, mealCount = 3)
        assertEquals(RecommendationSource.RULE_FALLBACK, result.source)
    }

    @Test
    fun `换一换轮转让兜底换出不同组合`() = runBlocking {
        // 6 个都可做的候选，兜底每餐2菜；rotation 变化应换出不同菜。
        val many = (1L..6L).map { RuleDish(it, "菜$it", listOf(main(100 + it, "料$it"))) }
        val bigInput = RecommendationInput(
            dishes = many,
            pantryIngredientIds = (101L..106L).toSet(),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 走兜底(确定性)
        val r0 = orch.recommend(bigInput, mealCount = 1, rotation = 0).suggestions.first().dishIds
        val r1 = orch.recommend(bigInput, mealCount = 1, rotation = 1).suggestions.first().dishIds
        assertEquals(RecommendationSource.RULE_FALLBACK, orch.recommend(bigInput, mealCount = 1, rotation = 0).source)
        // 轮转后首个组合的菜应不同(兜底不再原地打转)
        assertTrue(r0 != r1, "换一换应换出不同组合: r0=$r0 r1=$r1")
    }
}
