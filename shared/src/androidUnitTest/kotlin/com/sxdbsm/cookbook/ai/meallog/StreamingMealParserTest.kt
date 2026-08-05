package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * @File : StreamingMealParserTest
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : NDJSON 流式解析器测试——覆盖 T-01~T-08 + AF-03~05 修复验证。
 * <p>
 * [AI修改] AF-03~05：更新 T04/T07b 适配新校验规则；新增 AF 专项测试。
 **/
class StreamingMealParserTest {

    private val targetDate = LocalDate(2026, 8, 5) // 周三
    private val genId = "test-gen-1"

    // ═══════════════════════════════ T-01 ═══════════════════════════════

    @Test
    fun `T01 正常NDJSON 逐行解析后归组成一餐一菜一食材`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "午餐吃了番茄炒蛋", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch","time":"12:00"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋","cooking_method":"炒"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄","role":"主料","food_group":"vegetable","quantity":100,"unit":"g","is_main":true}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"鸡蛋","role":"主料","food_group":"egg","quantity":50,"unit":"g","is_main":true}
            {"type":"done","segment_id":"quick-2026-08-05"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        assertEquals(1, draft.segments.size)
        val seg = draft.segments["quick-2026-08-05"]!!
        assertTrue(seg.done)
        assertEquals(1, seg.meals.size)
        val meal = seg.meals["2026-08-05|lunch"]!!
        assertEquals("lunch", meal.slot)
        assertEquals(1, meal.dishes.size)
        val dish = meal.dishes["2026-08-05|lunch|d1"]!!
        assertEquals("番茄炒蛋", dish.name)
        assertEquals(2, dish.ingredients.size)
    }

    // ═══════════════════════════════ T-02 ═══════════════════════════════

    @Test
    fun `T02 dish先到但携带合法date和slot meal_id一致 自动补建父餐次`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "午餐红烧肉", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","date":"2026-08-05","slot":"lunch"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val seg = draft.segments["quick-2026-08-05"]!!
        val meal = seg.meals["2026-08-05|lunch"]!!
        assertTrue(meal.warnings.any { it.contains("补建了父餐次") })
        assertEquals("红烧肉", meal.dishes["2026-08-05|lunch|d1"]!!.name)
    }

    // ═══════════════════════════════ T-03 ═══════════════════════════════

    @Test
    fun `T03 ingredient缺少dish_id或dish_id不存在 进入诊断不静默挂靠`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d99","name":"大米"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","name":"鸡蛋"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val dish = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(0, dish.ingredients.size)
        val diagMsgs = draft.diagnostics.map { it.message }
        assertTrue(diagMsgs.any { it.contains("d99") && it.contains("不存在") })
        assertTrue(diagMsgs.any { it.contains("缺少 dish_id") })
    }

    // ═══════════════════════════════ T-04 (AF-03 适配) ═══════════════════════════════

    @Test
    fun `T04 同dish_id跨meal_id 格式校验拒绝前缀不匹配的dish`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        // AF-03: dish_id 必须匹配 {meal_id}|d{N}，前缀不匹配时格式校验直接拒
        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|dinner","date":"2026-08-05","slot":"dinner"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|dinner","dish_id":"2026-08-05|lunch|d1","name":"米饭-冲突"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val seg = draft.segments["quick-2026-08-05"]!!
        assertEquals("米饭", seg.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.name)
        // 晚餐不应有冲突 dish（格式校验已拒绝）
        assertEquals(0, seg.meals["2026-08-05|dinner"]!!.dishes.size)
        assertTrue(draft.diagnostics.any { it.message.contains("格式无效") })
    }

    // ═══════════════════════════════ T-05 ═══════════════════════════════

    @Test
    fun `T05 SSE分片行中间断开 仅完整换行后解析`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","seg""")
        assertEquals(0, parser.currentDraft.segments.size)
        parser.feedDelta("""ment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        parser.feedDelta("""{"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")

        val draft = parser.finish("stop")
        assertEquals("米饭", draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.name)
    }

    // ═══════════════════════════════ T-06 ═══════════════════════════════

    @Test
    fun `T06 完成时半行保留合法事件 尾部进诊断`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        parser.feedDelta("""{"type":"dish","segment_id":"quick""")

        val draft = parser.finish("stop")
        assertNotNull(draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"])
        assertTrue(draft.diagnostics.any { it.message.contains("未完成") })
    }

    // ═══════════════════════════════ T-07 (AF-04 适配) ═══════════════════════════════

    @Test
    fun `T07 整体JSON回退 FlatMealJson映射到已知segment后通过同链校验`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val flatJson = """{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"米饭"},{"date":"2026-08-05","meal_type":"dinner","dish_name":"青菜"}]}"""
        parser.feedDelta(flatJson)

        val draft = parser.finish("stop")
        assertTrue(draft.diagnostics.any { it.message.contains("整体 JSON") || it.message.contains("规范化") })
        // AF-04: 结果应归入已知 segment
        assertTrue(draft.segments.isNotEmpty())
        assertTrue(draft.segments.containsKey("quick-2026-08-05"))
    }

    @Test
    fun `T07b 整体JSON MultiDayJson映射到已知segment后通过同链校验`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val multiDayJson = """{"schema_version":"1.0","days":[{"date":"2026-08-05","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭","quantity":1,"quantity_unit":"份"}]}]}]}"""
        parser.feedDelta(multiDayJson)

        val draft = parser.finish("stop")
        // AF-04: 应通过规范化为已知 segment
        assertTrue(draft.segments.isNotEmpty())
        assertTrue(draft.segments.containsKey("quick-2026-08-05"))
    }

    // ═══════════════════════════════ T-08 ═══════════════════════════════

    @Test
    fun `T08 finish_reason等于length 产生截断警告但保留已解析内容`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val draft = parser.finish("length")

        assertTrue(draft.isTruncated)
        assertTrue(draft.diagnostics.any { it.message.contains("截断") })
        assertNotNull(draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"])
    }

    // ═══════════════════════════════ AF-03 专项 ═══════════════════════════════

    @Test
    fun `AF03 未知segment_id事件被拒绝不创建segment`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","segment_id":"unknown-seg","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val draft = parser.finish("stop")

        // 未知 segment 不应出现在草稿中
        assertFalse(draft.segments.containsKey("unknown-seg"))
        assertTrue(draft.diagnostics.any { it.message.contains("不匹配") })
    }

    @Test
    fun `AF03 非法slot拒绝meal事件`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        // breakfast 是合法值，用 "morning" 非法
        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|morning","date":"2026-08-05","slot":"morning"}""" + "\n")
        val draft = parser.finish("stop")

        assertEquals(0, draft.segments["quick-2026-08-05"]?.meals?.size ?: 0)
        assertTrue(draft.diagnostics.any { it.message.contains("无效") })
    }

    @Test
    fun `AF03 dish_id格式无效拒绝事件`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        // dish_id 不含 d{N} 后缀
        parser.feedDelta("""{"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"invalid-dish-id","name":"米饭"}""" + "\n")
        val draft = parser.finish("stop")

        val dishes = draft.segments["quick-2026-08-05"]?.meals?.get("2026-08-05|lunch")?.dishes ?: emptyMap()
        assertEquals(0, dishes.size)
        assertTrue(draft.diagnostics.any { it.message.contains("格式无效") })
    }

    @Test
    fun `AF03 dish补建时meal_id与date竖线slot不一致导致补建失败`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        // dish meal_id="2026-08-05|lunch" 但 date+slot="2026-08-05|dinner"
        parser.feedDelta("""{"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","date":"2026-08-05","slot":"dinner"}""" + "\n")
        val draft = parser.finish("stop")

        // 补建失败，不应创建任何 meal
        assertEquals(0, draft.segments["quick-2026-08-05"]?.meals?.size ?: 0)
        assertTrue(draft.diagnostics.any { it.message.contains("不一致") })
    }

    // ═══════════════════════════════ AF-04 专项 ═══════════════════════════════

    @Test
    fun `AF04 整体JSON回退不产生fallback-day前缀的segment`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"测试菜"}]}""")
        val draft = parser.finish("stop")

        // 不应有 fallback-* 前缀
        assertTrue(draft.segments.none { it.key.startsWith("fallback-") })
        // 应有合法内容
        assertTrue(draft.segments.isNotEmpty())
    }

    // ═══════════════════════════════ AF-05 专项 ═══════════════════════════════

    @Test
    fun `AF05 dish同键重复合并非空字段保留已有子项`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","cooking_method":"烧"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"五花肉","quantity":150}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","quantity":2}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val dish = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        // 后到 quantity 覆盖
        assertEquals(2.0, dish.quantity)
        // 原有 cookingMethod 保留
        assertEquals("烧", dish.cookingMethod)
        // 原有 ingredient 保留
        assertEquals(1, dish.ingredients.size)
        assertEquals("五花肉", dish.ingredients[0].name)
    }

    @Test
    fun `AF05 ingredient同款名去重合并`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"鸡蛋","quantity":50}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"鸡蛋","quantity":60,"food_group":"egg"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val dish = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        // 同名合并，不重复
        assertEquals(1, dish.ingredients.size)
        assertEquals("鸡蛋", dish.ingredients[0].name)
        assertEquals(60.0, dish.ingredients[0].quantity) // 后到覆盖
        assertEquals("egg", dish.ingredients[0].foodGroup) // 后到补充
    }

    @Test
    fun `AF05 dish_name唯一补挂ingredient成功`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_name":"番茄炒蛋","name":"鸡蛋","quantity":50}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val dish = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        // 按 dish_name 唯一补挂成功
        assertEquals(1, dish.ingredients.size)
        assertEquals("鸡蛋", dish.ingredients[0].name)
        assertTrue(dish.warnings.any { it.contains("唯一补挂") })
    }

    // ═══════════════════════════════ 边界情况 ═══════════════════════════════

    @Test
    fun `多段周期记 按segment_id正确隔离`() {
        val weekAnchor = LocalDate(2026, 8, 3)
        val segments = listOf(
            InputSegment("week-2026-08-03-day1", LocalDate(2026, 8, 3), "周一午餐米饭", 0),
            InputSegment("week-2026-08-03-day2", LocalDate(2026, 8, 4), "周二晚餐面条", 1),
        )
        val parser = StreamingMealParser(segments, genId, weekAnchor)

        val ndjson = """
            {"type":"meal","segment_id":"week-2026-08-03-day1","meal_id":"2026-08-03|lunch","date":"2026-08-03","slot":"lunch"}
            {"type":"dish","segment_id":"week-2026-08-03-day1","meal_id":"2026-08-03|lunch","dish_id":"2026-08-03|lunch|d1","name":"米饭"}
            {"type":"meal","segment_id":"week-2026-08-03-day2","meal_id":"2026-08-04|dinner","date":"2026-08-04","slot":"dinner"}
            {"type":"dish","segment_id":"week-2026-08-03-day2","meal_id":"2026-08-04|dinner","dish_id":"2026-08-04|dinner|d1","name":"面条"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        assertEquals(2, draft.segments.size)
        assertEquals("米饭", draft.segments["week-2026-08-03-day1"]!!.meals["2026-08-03|lunch"]!!.dishes["2026-08-03|lunch|d1"]!!.name)
        assertEquals("面条", draft.segments["week-2026-08-03-day2"]!!.meals["2026-08-04|dinner"]!!.dishes["2026-08-04|dinner|d1"]!!.name)
    }

    @Test
    fun `同键重复事件按字段合并 菜品以最新非空字段覆盖`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch","time":"11:00","note":"少盐"}
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch","time":"12:30"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val meal = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!
        assertEquals("12:30", meal.time)
        assertEquals("少盐", meal.note)
    }

    @Test
    fun `未知事件类型忽略但不中断其他行`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"unknown_event","segment_id":"quick-2026-08-05","data":"blah"}
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")
        assertTrue(draft.diagnostics.any { it.message.contains("未知事件类型") })
        assertNotNull(draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"])
    }

    @Test
    fun `seasoning和cooking_step事件正确关联`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉"}
            {"type":"seasoning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"盐","quantity":3}
            {"type":"cooking_step","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","text":"热锅凉油","order":1}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val dish = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(1, dish.seasonings.size)
        assertEquals(1, dish.cookingSteps.size)
    }

    @Test
    fun `warning事件关联到不同层级`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}
            {"type":"warning","segment_id":"quick-2026-08-05","message":"全局警告"}
            {"type":"warning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","message":"午餐偏咸"}
            {"type":"warning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","message":"米饭未给份量"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val seg = draft.segments["quick-2026-08-05"]!!
        assertTrue(seg.warnings.any { it == "全局警告" })
        assertTrue(seg.meals["2026-08-05|lunch"]!!.warnings.any { it == "午餐偏咸" })
        assertTrue(seg.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.warnings.any { it == "米饭未给份量" })
    }

    // ═══════════════════════════════ AF-07 专项 ═══════════════════════════════

    @Test
    fun `AF07 同日同餐多道菜保留不同dish_id`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        val flatJson = """{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"米饭"},{"date":"2026-08-05","meal_type":"lunch","dish_name":"青菜"}]}"""
        parser.feedDelta(flatJson)
        val draft = parser.finish("stop")

        val meal = draft.segments["quick-2026-08-05"]?.meals?.get("2026-08-05|lunch")
        assertNotNull(meal)
        assertEquals(2, meal!!.dishes.size)
        // 两道菜应有不同的 dish_id
        val dishIds = meal.dishes.keys
        assertTrue(dishIds.any { it.endsWith("d1") })
        assertTrue(dishIds.any { it.endsWith("d2") })
    }

    @Test
    fun `AF07 不匹配任何segment的日期被拒绝不映射到第一段`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        // 日期 2026-08-10 不匹配任何已知 segment
        val flatJson = """{"schema_version":"2.0","items":[{"date":"2026-08-10","meal_type":"lunch","dish_name":"米饭"}]}"""
        parser.feedDelta(flatJson)
        val draft = parser.finish("stop")

        assertTrue(draft.diagnostics.any { it.message.contains("无法映射") })
        // 不应污染已知 segment
        val seg = draft.segments["quick-2026-08-05"]
        assertTrue(seg == null || seg.meals.isEmpty())
    }

    // ═══════════════════════════════ AF-08 专项 ═══════════════════════════════

    @Test
    fun `AF08 dish_id等于d0被拒绝`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        parser.feedDelta("""{"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d0","name":"米饭"}""" + "\n")
        val draft = parser.finish("stop")

        val dishes = draft.segments["quick-2026-08-05"]?.meals?.get("2026-08-05|lunch")?.dishes ?: emptyMap()
        assertEquals(0, dishes.size)
        assertTrue(draft.diagnostics.any { it.message.contains("格式无效") })
    }

    @Test
    fun `AF08 NDJSON Prompt包含dish_name容错字段`() {
        val prompt = AiMealPrompt.NDJSON_SYSTEM_PROMPT
        // dish_name 应出现在 ingredient 事件定义中
        assertTrue(prompt.contains("dish_name"))
        // 规则必须提及缺 dish_id 时提供 dish_name
        assertTrue(prompt.contains("缺 dish_id") || prompt.contains("无法提供正确的 dish_id"))
    }

    @Test
    fun `空Delta不改变状态`() {
        val segments = listOf(InputSegment("quick-2026-08-05", targetDate, "测试", 0))
        val parser = StreamingMealParser(segments, genId, targetDate)
        parser.feedDelta("")
        assertEquals(0, parser.currentDraft.segments.size)
    }
}
