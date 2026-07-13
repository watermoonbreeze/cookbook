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
    fun `换一换按整批10个轮转_不重复且全推完循环`() = runBlocking {
        // 25 个候选、每批 10：换一换取下一批不重复；批数=3，rotation 循环回第 1 批。
        val many = (1L..25L).map { RuleDish(it, "菜$it", listOf(main(100 + it, "料$it"))) }
        val bigInput = RecommendationInput(
            dishes = many,
            pantryIngredientIds = (101L..125L).toSet(),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 走兜底(确定性)
        // 第 N 批候选(orchestrator 分批后 candidates 头部即该批)
        suspend fun batchIds(rotation: Int) = orch.recommend(bigInput, mealCount = 1, rotation = rotation)
            .candidates.take(10).map { it.id }
        val b0 = batchIds(0)
        val b1 = batchIds(1)
        val b2 = batchIds(2)
        val b3 = batchIds(3)
        assertEquals(10, b0.size)
        assertEquals(10, b1.size)
        assertTrue(b0.intersect(b1.toSet()).isEmpty(), "第2批不应与第1批重复: b0=$b0 b1=$b1")
        assertTrue(b1.intersect(b2.toSet()).isEmpty(), "第3批不应与前批重复")
        assertEquals(b0, b3, "批数=3(25/10向上取整), rotation=3 应循环回第1批")
    }

    @Test
    fun `候选不外溢_orchestrator只保留当前一批`() = runBlocking {
        // H2 守护：25 候选时 result.candidates 应≤一批(10)，不再把长尾整体外露、也不整体喂 prompt/validate。
        val many = (1L..25L).map { RuleDish(it, "菜$it", listOf(main(100 + it, "料$it"))) }
        val bigInput = RecommendationInput(many, (101L..125L).toSet(), HealthConstraints(), emptySet())
        val orch = RecommendationOrchestrator(MockAiRuntime())
        (0..3).forEach { rot ->
            val c = orch.recommend(bigInput, mealCount = 1, rotation = rot).candidates
            assertTrue(c.size <= 10, "rotation=$rot 候选应≤一批(10)，实际=${c.size}")
        }
    }

    @Test
    fun `模型选到当前批外的菜被过滤_与展示批一致`() = runBlocking {
        // H2 守护：rotation=0 批=id1..10；模型偏选 15(第2批)应被 validate 剔除(候选=展示批)。
        val many = (1L..25L).map { RuleDish(it, "菜$it", listOf(main(100 + it, "料$it"))) }
        val bigInput = RecommendationInput(many, (101L..125L).toSet(), HealthConstraints(), emptySet())
        val json = """{"suggestions":[{"dishIds":[1,15],"reason":"x"}]}"""
        val orch = RecommendationOrchestrator(MockAiRuntime(json))
        val result = orch.recommend(bigInput, mealCount = 3, rotation = 0)
        assertEquals(RecommendationSource.MODEL, result.source)
        assertEquals(listOf(1L), result.suggestions.first().dishIds, "批外 id 15 应被过滤")
    }

    @Test
    fun `候选不足一批时换一换循环回同一批`() = runBlocking {
        // ≤10 个候选只有一批，换一换循环回同批(符合"全推完再循环")。
        val few = (1L..5L).map { RuleDish(it, "菜$it", listOf(main(100 + it, "料$it"))) }
        val input = RecommendationInput(few, (101L..105L).toSet(), HealthConstraints(), emptySet())
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val a = orch.recommend(input, mealCount = 1, rotation = 0).candidates.map { it.id }
        val b = orch.recommend(input, mealCount = 1, rotation = 1).candidates.map { it.id }
        assertEquals(a, b, "≤1批时换一换回同批")
    }
}
