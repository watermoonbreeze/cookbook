package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.PlanDish
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : PeriodPlannerTest
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划规则单测（天数/忌口剔除/健康80%/应季优先）
 * <p>
 * [AI生成] 合成候选验证规划规则，不依赖数据源与模型。
 **/
class PeriodPlannerTest {

    private val planner = PeriodPlanner()
    private val meals = listOf("早餐", "中餐", "晚餐")

    private fun dish(
        id: Long,
        main: List<String> = listOf("料$id"),
        nutrition: Set<String> = emptySet(),
        season: Set<String> = emptySet(),
        healthy: Boolean = false,
        avoid: Boolean = false,
        breakfast: Boolean = false,
        meat: Boolean = false,
        methods: List<String> = emptyList(), // [AI生成] 做法名(重油族去重测试用)
        highGi: List<String> = emptyList(), // [AI生成] D4:高GI主料命中(慢病软降测试用)
        highPurine: List<String> = emptyList(), // [AI生成] D4:高嘌呤主料命中(慢病软降测试用)
    ) = PlanDish(
        id = id, name = "菜$id", mainNames = main, nutritionTags = nutrition, seasonTags = season,
        isHealthy = healthy, hasAvoid = avoid, isMeat = meat, isBreakfast = breakfast,
        cookingMethodNames = methods, highGiHits = highGi, highPurineHits = highPurine,
    )

    @Test
    fun `天数与餐次数量正确`() {
        val dishes = (1L..20L).map { dish(it) }
        val plan = planner.plan(dishes, days = 3, mealNames = meals, dishesMin = 2, dishesMax = 2)
        assertEquals(3, plan.days.size)
        assertEquals(3, plan.days[0].meals.size)
        assertEquals(2, plan.days[0].meals[0].dishes.size)
    }

    @Test
    fun `每餐菜数在2到5之间`() {
        val dishes = (1L..40L).map { dish(it, main = listOf("料$it")) }
        val plan = planner.plan(dishes, days = 5, mealNames = meals, dishesMin = 2, dishesMax = 5, seed = 3)
        val counts = plan.days.flatMap { it.meals }.map { it.dishes.size }
        assertTrue(counts.all { it in 2..5 }, "每餐菜数应在2~5: $counts")
        assertTrue(counts.toSet().size > 1, "菜数应有变化而非固定: $counts")
    }

    @Test
    fun `天数封顶30`() {
        val dishes = (1L..30L).map { dish(it) }
        val plan = planner.plan(dishes, days = 100, mealNames = meals, dishesMin = 1, dishesMax = 1)
        assertEquals(30, plan.days.size)
    }

    @Test
    fun `忌口菜被剔除`() {
        val plan = planner.plan(
            listOf(dish(1), dish(2, avoid = true)),
            days = 1, mealNames = listOf("中餐"), dishesMin = 1, dishesMax = 1,
        )
        val ids = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }
        assertTrue(2L !in ids)
    }

    @Test
    fun `健康档案下利健康占比不低于80%`() {
        val healthy = (1L..10L).map { dish(it, main = listOf("m$it"), healthy = true) }
        val unhealthy = (11L..15L).map { dish(it, main = listOf("m$it"), healthy = false) }
        val plan = planner.plan(healthy + unhealthy, days = 3, mealNames = meals, dishesMin = 2, dishesMax = 2, healthAware = true)
        assertTrue(plan.healthyRatio >= 0.8, "healthyRatio=${plan.healthyRatio}")
    }

    @Test
    fun `同餐尽量荤素搭配`() {
        // 2 荤 2 素、每餐 2 菜：应荤素各一，而非两荤或两素。
        val dishes = listOf(
            dish(1, main = listOf("五花肉"), meat = true),
            dish(2, main = listOf("鸡腿"), meat = true),
            dish(3, main = listOf("青椒"), meat = false),
            dish(4, main = listOf("白菜"), meat = false),
        )
        val plan = planner.plan(dishes, days = 1, mealNames = listOf("中餐"), dishesMin = 2, dishesMax = 2, seed = 1)
        val picked = plan.days[0].meals[0].dishes.map { it.id }.toSet()
        val meatCount = picked.count { it == 1L || it == 2L }
        val vegCount = picked.count { it == 3L || it == 4L }
        assertEquals(1, meatCount, "一餐应有 1 荤: picked=$picked")
        assertEquals(1, vegCount, "一餐应有 1 素: picked=$picked")
    }

    @Test
    fun `重油族跨餐去重_一餐不两道重油菜`() {
        // 2 道重油(红烧/干煸)+1 道清淡(清蒸)、全素(荤素不干扰)、同 base、1 餐 2 道、FRESH 强去重：
        //   选一道重油后另一道重油被降权(-f.repeat×0.4>jitter)→清淡菜必入选,一餐不出现两道重油。
        val dishes = listOf(
            dish(1, main = listOf("茄子"), methods = listOf("红烧")),
            dish(2, main = listOf("豆角"), methods = listOf("干煸")),
            dish(3, main = listOf("南瓜"), methods = listOf("清蒸")),
        )
        val plan = planner.plan(
            dishes, days = 1, mealNames = listOf("晚餐"), dishesMin = 2, dishesMax = 2,
            seed = 0, style = RecommendationStyle.FRESH,
        )
        val ids = plan.days[0].meals[0].dishes.map { it.id }.toSet()
        assertEquals(2, ids.size)
        assertTrue(!(1L in ids && 2L in ids), "一餐不应两道重油菜(红烧茄子+干煸豆角): $ids")
        assertTrue(3L in ids, "重油已用后应补清淡菜(清蒸南瓜): $ids")
    }

    @Test
    fun `应季菜优先`() {
        val plan = planner.plan(
            listOf(dish(1, season = setOf("夏季")), dish(2)),
            days = 1, mealNames = listOf("中餐"), dishesMin = 1, dishesMax = 1, currentSeason = "夏季",
        )
        assertEquals(1L, plan.days[0].meals[0].dishes[0].id)
    }

    @Test
    fun `早餐档只选早餐菜午晚选非早餐菜`() {
        val plan = planner.plan(
            listOf(dish(1, breakfast = true), dish(2, breakfast = false)),
            days = 1, mealNames = listOf("早餐", "中餐"), dishesMin = 1, dishesMax = 1,
        )
        val bfDishes = plan.days[0].meals.first { it.mealName == "早餐" }.dishes.map { it.id }
        val lunchDishes = plan.days[0].meals.first { it.mealName == "中餐" }.dishes.map { it.id }
        assertTrue(1L in bfDishes && 2L !in bfDishes, "早餐应只出早餐菜: $bfDishes")
        assertTrue(2L in lunchDishes && 1L !in lunchDishes, "中餐应只出非早餐菜: $lunchDishes")
    }

    @Test
    fun `偏营养风格_高嘌呤高GI菜被软降到最后`() {
        // 4 道干净全素菜 + 1 道命中高嘌呤/高GI的菜(id=5)，同 base、同素(荤素/主食不干扰)、5 选 4：
        //   偏营养风格开启慢病软降(-0.6×0.42)，命中菜应被挤出成唯一落选者。seed 固定确定性。
        //   [AI修改] P2:flagged 用中性主料(料5·与 clean 同宝塔层)隔离慢病降权——避免"猪肝→荤"被午餐期待层补分抵消降权,
        //   高GI/高嘌呤命中经显式参数注入(不依赖主料名),纯粹考验慢病软降机制。
        val clean = (1L..4L).map { dish(it, main = listOf("料$it")) }
        val flagged = dish(5, main = listOf("料5"), highGi = listOf("白米饭"), highPurine = listOf("猪肝"))
        val plan = planner.plan(
            clean + flagged, days = 1, mealNames = listOf("中餐"), dishesMin = 4, dishesMax = 4,
            seed = 0, style = RecommendationStyle.NUTRITION,
        )
        val picked = plan.days[0].meals[0].dishes.map { it.id }.toSet()
        assertEquals(4, picked.size)
        assertTrue(5L !in picked, "偏营养风格下高嘌呤/高GI菜应被软降落选: picked=$picked")
    }

    @Test
    fun `非营养风格_慢病命中不改变计划(gate关闭)`() {
        // 同一组菜、同 seed、非营养风格(FAMILIAR·chronicWeight=0)：带不带慢病命中，生成的计划必须完全一致——
        //   证明软降门禁只在"偏营养"风格生效，其余风格零影响(向后兼容·不误伤)。
        val base = (1L..4L).map { dish(it, main = listOf("料$it")) }
        val withoutHits = base + dish(5, main = listOf("猪肝"))
        val withHits = base + dish(5, main = listOf("猪肝"), highGi = listOf("白米饭"), highPurine = listOf("猪肝"))
        fun idsOf(dishes: List<PlanDish>) = planner.plan(
            dishes, days = 1, mealNames = listOf("中餐"), dishesMin = 4, dishesMax = 4,
            seed = 0, style = RecommendationStyle.FAMILIAR,
        ).days[0].meals[0].dishes.map { it.id }
        assertEquals(idsOf(withoutHits), idsOf(withHits), "非营养风格下慢病命中不应改变计划(gate 关闭)")
    }

    @Test
    fun `P2餐次差异化_晚餐不以纯肉打头且荤菜不多于午餐`() {
        // [AI生成] P2 集成级:验证 mealName→expectedLayers→compositionBonus 全链路接通(纯函数单测已锁分量,此测 plan() 端到端行为)。
        //   5素(v·主料 classify 均 null→布尔兜底蔬果层)+5荤(m·兜底鱼禽肉蛋层),隔离不靠具体食材名。固定 seed 确定性。
        val veg = (1L..5L).map { dish(it, main = listOf("v$it"), meat = false) }
        val meat = (6L..10L).map { dish(it, main = listOf("m$it"), meat = true) }
        val meatIds = meat.map { it.id }.toSet()
        val plan = planner.plan(
            veg + meat, days = 1, mealNames = listOf("午餐", "晚餐"),
            dishesMin = 3, dishesMax = 3, seed = 0,
        )
        val lunch = plan.days[0].meals[0].dishes
        val dinner = plan.days[0].meals[1].dishes
        // 晚餐期待{谷,蔬}不含荤→空餐首道:蔬菜补层+0.5 vs 纯肉轻降-0.3(差0.8>jitter幅度0.5)→晚餐首道恒非纯肉。
        assertTrue(dinner.first().id !in meatIds, "晚餐宜清淡:首道不应是纯肉菜 dinner=${dinner.map { it.id }}")
        // 晚餐纯肉数 ≤ 午餐(午餐期待含荤·补 ANIMAL 层;晚餐不期待荤+纯肉轻降)。
        val lunchMeat = lunch.count { it.id in meatIds }
        val dinnerMeat = dinner.count { it.id in meatIds }
        assertTrue(dinnerMeat <= lunchMeat, "晚餐荤菜数应≤午餐: 午餐=$lunchMeat 晚餐=$dinnerMeat")
    }

    @Test
    fun `候选充足时尽量不重复同一道菜`() {
        val dishes = (1L..60L).map { dish(it, main = listOf("料$it")) }
        val plan = planner.plan(dishes, days = 2, mealNames = meals, dishesMin = 2, dishesMax = 2, seed = 7)
        val ids = plan.days.flatMap { it.meals }.flatMap { it.dishes }.map { it.id }
        // 每餐固定 2 道、12 个坑、60 个候选，应无重复
        assertEquals(ids.size, ids.toSet().size)
    }
}
