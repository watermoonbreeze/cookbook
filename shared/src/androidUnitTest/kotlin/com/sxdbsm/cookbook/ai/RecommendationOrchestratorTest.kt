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

    private fun reproducibilityInput(count: Int = 8) = RecommendationInput(
        dishes = (1L..count.toLong()).map { RuleDish(it, "固定菜$it", listOf(main(1000 + it, "固定料$it"))) },
        pantryIngredientIds = (1001L..1000 + count).toSet(),
        constraints = HealthConstraints(),
        recentDishIds = emptySet(),
    )

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
    fun `CF1_模型给两荤无素时validate后补一道素菜`() = runBlocking {
        // [AI生成] C#F1强版:模型只给两荤(红烧肉+糖醋排骨),候选有素菜油菜→补菜应补进油菜(全荤→补素·组合完整)。
        val meatOnly = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "糖醋排骨", listOf(main(102, "排骨"))),
                RuleDish(3, "清炒油菜", listOf(main(103, "油菜"))),
            ),
            pantryIngredientIds = setOf(101, 102, 103),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val json = """{"suggestions":[{"dishIds":[1,2],"reason":"两道肉"}]}"""
        val result = RecommendationOrchestrator(MockAiRuntime(json)).recommend(meatOnly, mealCount = 1)
        assertEquals(RecommendationSource.MODEL, result.source)
        val meal = result.suggestions.first().dishIds
        assertTrue(3L in meal, "全荤餐应补进素菜油菜: $meal")
        assertEquals(3, meal.size, "补一道后共3道")
    }

    @Test
    fun `CF1_模型已含素菜时不补菜`() = runBlocking {
        // [AI生成] C#F1强版:已含素菜(1荤1素)的均衡餐不应被补菜(只治全荤无素·不误改均衡餐)。
        val balanced = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "清炒油菜", listOf(main(102, "油菜"))),
                RuleDish(3, "米饭", listOf(main(103, "大米"))),
            ),
            pantryIngredientIds = setOf(101, 102, 103),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val json = """{"suggestions":[{"dishIds":[1,2],"reason":"荤素搭配"}]}"""
        val meal = RecommendationOrchestrator(MockAiRuntime(json)).recommend(balanced, mealCount = 1).suggestions.first().dishIds
        assertEquals(listOf(1L, 2L), meal, "已含素菜(1荤1素)不应补菜")
    }

    @Test
    fun `CF1_不把忌口菜补进餐`() = runBlocking {
        // [AI生成] C#F1强版:补菜只从非忌口 selectable 取——唯一素菜是忌口菜时,不得补进(宁可不补)。
        val avoidVeg = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "红烧肉", listOf(main(101, "五花肉"))),
                RuleDish(2, "糖醋排骨", listOf(main(102, "排骨"))),
                RuleDish(3, "凉拌木耳", listOf(main(103, "木耳"))), // 木耳忌口→素菜但不可补
            ),
            pantryIngredientIds = setOf(101, 102, 103),
            constraints = HealthConstraints(avoidIngredientIds = setOf(103)),
            recentDishIds = emptySet(),
        )
        val json = """{"suggestions":[{"dishIds":[1,2],"reason":"两道肉"}]}"""
        val meal = RecommendationOrchestrator(MockAiRuntime(json)).recommend(avoidVeg, mealCount = 1).suggestions.first().dishIds
        assertTrue(3L !in meal, "忌口素菜不得被补进餐: $meal")
        assertEquals(listOf(1L, 2L), meal, "无非忌口素菜可补→原样(不补忌口菜)")
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
    fun `用户2_兜底一餐不混搭中西_同菜系优先`() = runBlocking {
        // 2中式+2西式,mealCount=1,FALLBACK每餐3道;西式降权→首道中式,combineScore罚混搭→整餐同菜系不出"排骨汤+帕尼尼"。
        val mixInput = RecommendationInput(
            dishes = listOf(
                RuleDish(1, "米饭", listOf(main(101, "大米")), cuisine = "家常菜"),
                RuleDish(2, "青椒炒肉", listOf(main(102, "青椒"), main(103, "猪肉")), cuisine = "家常菜"),
                RuleDish(3, "帕尼尼", listOf(main(104, "面包")), cuisine = "西餐"),
                RuleDish(4, "凯撒沙拉", listOf(main(105, "生菜")), cuisine = "西餐"),
            ),
            pantryIngredientIds = setOf(101, 102, 103, 104, 105),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val result = orch.recommend(mixInput, mealCount = 1)
        val mealIds = result.suggestions.first().dishIds.toSet()
        val mealDishes = result.candidates.filter { it.id in mealIds }
        val families = mealDishes.map { isWesternCuisine(it.cuisine) }.distinct()
        assertEquals(1, families.size, "一餐不应混搭中西: ${mealDishes.map { it.name to it.cuisine }}")
        assertTrue(mealDishes.none { isWesternCuisine(it.cuisine) }, "中式优先→首餐应为中式: ${mealDishes.map { it.name }}")
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
    fun `CF2_早餐上下文已选全软时补硬食避免两软无蛋`() = runBlocking {
        // [AI生成] C#F2:早餐 3软(白粥/豆浆/燕麦)+1硬(馒头)·每餐3道·空模型走兜底。
        //   隔离软硬补分:4菜全素(无荤素差异)、白粥/馒头均主食(首道白粥后馒头无 STAPLE_BONUS)→馒头唯一优势=早餐软硬补分。
        val dishes = listOf(
            RuleDish(1, "白粥", listOf(main(101, "大米")), breakfastSoft = true),
            RuleDish(2, "豆浆", listOf(main(102, "黄豆")), breakfastSoft = true),
            RuleDish(3, "燕麦", listOf(main(104, "燕麦片")), breakfastSoft = true),
            RuleDish(4, "馒头", listOf(main(103, "面粉")), breakfastSoft = false),
        )
        val pantry = setOf(101L, 102, 103, 104)
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 空→兜底(确定性)
        // 早餐上下文:首道软(白粥)后,第2道软硬补分让硬食(馒头)胜出→一餐含硬食。
        val bf = orch.recommend(
            RecommendationInput(dishes, pantry, HealthConstraints(), emptySet(), isBreakfastMeal = true),
            mealCount = 1,
        ).suggestions.first().dishIds.toSet()
        assertTrue(4L in bf, "早餐已选全软→应补硬食(馒头)避免两软无蛋: $bf")
        // 非早餐上下文:软硬补分不生效→硬食无优势(同分取先者=软),馒头不入选(证 gate)。
        val notBf = orch.recommend(
            RecommendationInput(dishes, pantry, HealthConstraints(), emptySet(), isBreakfastMeal = false),
            mealCount = 1,
        ).suggestions.first().dishIds.toSet()
        assertTrue(4L !in notBf, "非早餐上下文软硬补分不生效(gate)→馒头不因软硬入选: $notBf")
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
    fun `MMR重油族_主料做法名各异但都重口时清淡菜被打散优先`() = runBlocking {
        // 4 道全素菜(protein 维恒相同不区分)、主料各异(main维=0)、做法名各异(method Jaccard=0)、菜系空：
        //   仅"油腻度族"能区分——前3为重油(红烧/干煸/煎)、第4清淡(白灼)。加重油族维后,#1选定后应优先打散出清淡菜到第2位。
        val dishes = listOf(
            RuleDish(1, "红烧茄子", listOf(main(201, "茄子")), cookingMethodNames = listOf("红烧")),
            RuleDish(2, "干煸豆角", listOf(main(202, "豆角")), cookingMethodNames = listOf("干煸")),
            RuleDish(3, "香煎藕饼", listOf(main(203, "藕")), cookingMethodNames = listOf("香煎")),
            RuleDish(4, "白灼芥蓝", listOf(main(204, "芥蓝")), cookingMethodNames = listOf("白灼")),
        )
        val input = RecommendationInput(dishes, setOf(201L, 202, 203, 204), HealthConstraints(), emptySet())
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val c = orch.recommend(input.copy(style = RecommendationStyle.FRESH), mealCount = 1, rotation = 0).candidates
        assertEquals("红烧茄子", c.first().name, "首位仍最相关(输入序1)")
        assertEquals("白灼芥蓝", c[1].name, "第2名应是唯一清淡菜(重油族打散),而非另一道重油菜: ${c.map { it.name }}")
    }

    @Test
    fun `fallback已含主食时不再堆第二道主食`() = runBlocking {
        // 证伪"主食补分方向"(Google审查抓到的阻断:chosenHasStaple 传反会致"已有主食反而堆主食")。
        // 1荤+2主食+1素,每餐3道;preferenceScores 定序 红烧肉>米饭>油菜>馒头(差<主食补分0.9)。
        // 正确:首荤→补米饭(首个主食,合理)→第3道应补素菜油菜(已有主食不再堆馒头)。传反则第3道错堆馒头(两主食)。
        val dishes = listOf(
            RuleDish(1, "红烧肉", listOf(main(301, "五花肉"))),
            RuleDish(2, "米饭", listOf(main(302, "大米"))),
            RuleDish(3, "馒头", listOf(main(303, "面粉"))),
            RuleDish(4, "清炒油菜", listOf(main(304, "油菜"))),
        )
        val input = RecommendationInput(
            dishes = dishes,
            pantryIngredientIds = setOf(301L, 302, 303, 304),
            constraints = HealthConstraints(),
            recentDishIds = emptySet(),
            preferenceScores = mapOf(1L to 1.0, 2L to 0.8, 4L to 0.6, 3L to 0.4),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 空→兜底(确定性)
        val meal = orch.recommend(input, mealCount = 1).suggestions.first().dishIds.toSet()
        assertEquals(3, meal.size)
        assertTrue(!(2L in meal && 3L in meal), "一餐不应堆两道主食(米饭+馒头): $meal")
        assertTrue(4L in meal, "已含主食后第3道应补素菜(油菜)而非第二主食: $meal")
    }

    @Test
    fun `fallback一餐不选两道同主料菜`() = runBlocking {
        // 两道五花肉菜 + 油菜 + 米饭，每餐3道：主料不重复轻罚应让一餐不出现两道五花肉(改推油菜),更贴近真实吃法。
        val dishes = listOf(
            RuleDish(1, "红烧肉", listOf(main(301, "五花肉"))),
            RuleDish(2, "回锅肉", listOf(main(301, "五花肉"))), // 同主料五花肉
            RuleDish(3, "清炒油菜", listOf(main(302, "油菜"))),
            RuleDish(4, "米饭", listOf(main(303, "大米"))),
        )
        val input = RecommendationInput(dishes, setOf(301L, 302, 303), HealthConstraints(), emptySet())
        val orch = RecommendationOrchestrator(MockAiRuntime()) // 空→兜底(确定性)
        val meal = orch.recommend(input, mealCount = 1).suggestions.first().dishIds.toSet()
        assertEquals(3, meal.size)
        assertTrue(!(1L in meal && 2L in meal), "一餐不应同时含两道五花肉菜(红烧肉+回锅肉): $meal")
        assertTrue(3L in meal, "应改推不同主料的油菜: $meal")
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

    @Test
    fun `T-RR-01_固定八菜重复二十次候选与首条建议稳定`() = runBlocking {
        val fixture = reproducibilityInput()
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val first = orch.recommend(fixture, mealCount = 1)
        repeat(19) {
            val next = orch.recommend(fixture, mealCount = 1)
            assertEquals(first.candidates.map { it.id }, next.candidates.map { it.id })
            assertEquals(first.suggestions.firstOrNull()?.dishIds, next.suggestions.firstOrNull()?.dishIds)
        }
    }

    @Test
    fun `T-RR-02_同层同分乱序输入按id升序且不跨层`() {
        val dishes = listOf(
            RuleDish(3, "忌口", listOf(main(1003, "忌料"))),
            RuleDish(2, "最近", listOf(main(1002, "近期料"))),
            RuleDish(4, "正常4", listOf(main(1004, "料4"))),
            RuleDish(1, "正常1", listOf(main(1001, "料1"))),
        )
        val result = HealthRuleEngine().evaluate(
            dishes, setOf(1001L, 1002, 1003, 1004), HealthConstraints(avoidIngredientIds = setOf(1003)), setOf(2L),
        )
        assertEquals(listOf(1L, 4L, 2L, 3L), result.map { it.id })
    }

    @Test
    fun `T-RR-03_二十五菜四轮批次稳定且循环`() = runBlocking {
        val fixture = reproducibilityInput(25)
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val expected = listOf((1L..10L).toList(), (11L..20L).toList(), (21L..25L).toList(), (1L..10L).toList())
        expected.forEachIndexed { rotation, ids ->
            repeat(5) { assertEquals(ids, orch.recommend(fixture, mealCount = 1, rotation = rotation).candidates.map { it.id }) }
        }
    }

    @Test
    fun `T-RR-04_四种style各自重复稳定`() = runBlocking {
        val fixture = reproducibilityInput()
        RecommendationStyle.entries.forEach { style ->
            val input = fixture.copy(style = style)
            val orch = RecommendationOrchestrator(MockAiRuntime())
            val first = orch.recommend(input, mealCount = 1)
            repeat(4) { assertEquals(first.candidates.map { it.id }, orch.recommend(input, mealCount = 1).candidates.map { it.id }) }
        }
    }

    @Test
    fun `T-RR-05_正常最近忌口分层顺序重复稳定`() = runBlocking {
        val fixture = reproducibilityInput(8).copy(
            constraints = HealthConstraints(avoidIngredientIds = setOf(1008L)),
            recentDishIds = setOf(7L),
        )
        val orch = RecommendationOrchestrator(MockAiRuntime())
        val expected = orch.recommend(fixture, mealCount = 1).candidates.map { it.id }
        repeat(4) { assertEquals(expected, orch.recommend(fixture, mealCount = 1).candidates.map { it.id }) }
        assertTrue(expected.indexOf(6L) < expected.indexOf(7L))
        assertTrue(expected.indexOf(7L) < expected.indexOf(8L))
    }
}
