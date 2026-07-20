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
    fun `A1_兜底组合避免两荤_优先荤素搭配`() = runBlocking {
        // 3荤 + 1素，模型空→兜底；QW-1 起每餐 3 菜(主食+荤+素更完整)；首餐应荤素搭配(含素菜4)，而非全荤。
        val meatVegInput = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "糖醋排骨", listOf(main(102, "排骨"))),
                RuleDish(3, "宫保鸡丁", listOf(main(103, "鸡肉"))),
                RuleDish(4, "清炒油菜", listOf(main(104, "油菜"))),
            ),
            pantryIngredientIds = setOf(101, 102, 103, 104),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 空→兜底(确定性)
        val result = orch.recommend(meatVegInput, mealCount = 1)
        assertEquals(RecommendationSource.RULE_FALLBACK, result.source)
        val meal = result.suggestions.first().dishIds
        assertEquals(3, meal.size) // [AI修改] QW-1:FALLBACK_DISHES_PER_MEAL 2→3
        assertTrue(4L in meal, "首餐应含素菜(荤素搭配)而非全荤: $meal")
    }

    @Test
    fun `A1_兜底组合优先补主食`() = runBlocking {
        // 2荤 + 1主食，每餐2菜：首餐第2道应优先补主食(主食补分>荤素补分)。
        val stapleInput = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "糖醋排骨", listOf(main(102, "排骨"))),
                RuleDish(3, "米饭", listOf(main(103, "大米"))),
            ),
            pantryIngredientIds = setOf(101, 102, 103),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val result = orch.recommend(stapleInput, mealCount = 1)
        val meal = result.suggestions.first().dishIds
        assertTrue(3L in meal, "首餐应补主食(米饭): $meal")
    }

    @Test
    fun `A1_全忌口或全最近时兜底仍返非空建议`() = runBlocking {
        // 两道菜都最近吃过(正常层为空)→ fallback 的 normal.ifEmpty{candidates} 兜底应仍返建议、不空、dishIds 非空。
        val recentInput = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "清炒油菜", listOf(main(102, "油菜"))),
            ),
            pantryIngredientIds = setOf(101, 102),
            constraints = HealthConstraints(),
            recentDishIds = setOf(1, 2), // 全部最近吃过
        )
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val result = orch.recommend(recentInput, mealCount = 1)
        assertEquals(RecommendationSource.RULE_FALLBACK, result.source)
        assertTrue(result.suggestions.isNotEmpty(), "全最近时兜底仍应给建议")
        assertTrue(result.suggestions.first().dishIds.isNotEmpty(), "兜底建议 dishIds 不空")
    }

    @Test
    fun `R1_模型选到忌口菜会被剔除但候选仍保留供UI标红`() = runBlocking {
        // 候选含忌口菜(id=2,含猪肝忌口)，模型偏偏选它 → validate 用非忌口可选集 → 剔除。
        val avoidInput = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "清炒菠菜", listOf(main(101, "菠菜"))),
                RuleDish(2, "菠菜猪肝汤", listOf(main(101, "菠菜"), main(103, "猪肝"))),
            ),
            pantryIngredientIds = setOf(101, 103),
            constraints = HealthConstraints(avoidIngredientIds = setOf(103)),
            recentDishIds = emptySet(),
        )
        val json = """{"suggestions":[{"dishIds":[1,2],"reason":"x"}]}""" // 模型选了忌口菜 2
        val orch = RecommendationOrchestrator(MockAiRuntime(json))
        val result = orch.recommend(avoidInput, mealCount = 3)
        assertTrue(result.suggestions.all { s -> 2L !in s.dishIds }, "忌口菜不得进模型建议")
        assertTrue(result.candidates.any { it.id == 2L }, "忌口菜仍保留在候选供 UI 标红")
    }

    @Test
    fun `R1_全部候选都忌口时兜底不崩且候选仍保留标红`() = runBlocking {
        // 极端:所有可做菜主料都命中忌口 → selectable 空 → 模型不可选 → fallback 用全量(仅忌口菜)兜底不崩。
        val allAvoidInput = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "猪肝汤", listOf(main(102, "猪肝"))),
            ),
            pantryIngredientIds = setOf(101, 102),
            constraints = HealthConstraints(avoidIngredientIds = setOf(101, 102)), // 两菜主料都忌口
            recentDishIds = emptySet(),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 空→兜底
        val result = orch.recommend(allAvoidInput, mealCount = 2)
        assertEquals(RecommendationSource.RULE_FALLBACK, result.source)
        assertTrue(result.suggestions.isNotEmpty(), "全忌口时兜底仍给建议(不空、不崩)")
        assertTrue(result.candidates.all { it.avoidNames.isNotEmpty() }, "候选全为忌口菜、仍保留供UI标红")
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
    fun `MMR四风格全开都打散同主料霸屏_首位仍最相关`() = runBlocking {
        // 5 道五花肉菜 + 青菜/鱼/豆腐各1，主料都在手→分数相同(引擎稳定序保持输入序:五花肉在前)。
        val dishes = listOf(
            RuleDish(1, "五花肉A", listOf(main(200, "五花肉"))),
            RuleDish(2, "五花肉B", listOf(main(200, "五花肉"))),
            RuleDish(3, "五花肉C", listOf(main(200, "五花肉"))),
            RuleDish(4, "五花肉D", listOf(main(200, "五花肉"))),
            RuleDish(5, "五花肉E", listOf(main(200, "五花肉"))),
            RuleDish(6, "清炒青菜", listOf(main(201, "青菜"))),
            RuleDish(7, "清蒸鱼", listOf(main(202, "鱼"))),
            RuleDish(8, "小葱豆腐", listOf(main(203, "豆腐"))),
        )
        val base = RecommendationInput(dishes, setOf(200L, 201, 202, 203), HealthConstraints(), emptySet())
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 兜底确定性

        // 四风格全开(λ 各异但都<1)：首位仍最相关(高分五花肉)，第2名应被打散成不同主料，前3含≥2种主料。
        RecommendationStyle.entries.forEach { style ->
            val c = orch.recommend(base.copy(style = style), mealCount = 1, rotation = 0).candidates
            assertEquals(listOf("五花肉"), c.first().mainNames, "[$style] 首位仍是最相关的高分菜")
            assertTrue(c[1].mainNames != listOf("五花肉"), "[$style] 第2名应被打散成不同主料: ${c.take(3).map { it.name }}")
            assertTrue(c.take(3).map { it.mainNames.firstOrNull() }.distinct().size >= 2, "[$style] 前3应含≥2种主料")
        }
    }

    @Test
    fun `MMR扩维_主料相同时不同菜系做法的更被打散优先`() = runBlocking {
        // 3 道主料都=鸡(在手·同分)：A/B 同川菜红烧、C 粤菜清蒸。MMR 第2名应优先 C(菜系+做法都不同=更多样)。
        val dishes = listOf(
            RuleDish(1, "川味红烧鸡A", listOf(main(200, "鸡")), cuisine = "川菜", cookingMethodNames = listOf("红烧")),
            RuleDish(2, "川味红烧鸡B", listOf(main(200, "鸡")), cuisine = "川菜", cookingMethodNames = listOf("红烧")),
            RuleDish(3, "粤式清蒸鸡C", listOf(main(200, "鸡")), cuisine = "粤菜", cookingMethodNames = listOf("清蒸")),
        )
        val input = RecommendationInput(dishes, setOf(200L), HealthConstraints(), emptySet())
        val orch = RecommendationOrchestrator(MockAiRuntime())
        // 用 FRESH(λ 小、强打散)确保多样性维度显著。
        val c = orch.recommend(input.copy(style = RecommendationStyle.FRESH), mealCount = 1, rotation = 0).candidates
        assertEquals("川味红烧鸡A", c.first().name, "首位仍最相关(输入序A)")
        assertEquals("粤式清蒸鸡C", c[1].name, "第2名应是菜系+做法都不同的 C(而非与A雷同的B): ${c.map { it.name }}")
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
