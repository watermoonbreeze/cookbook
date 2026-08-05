package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/**
 * @File : StreamingMealParserTest
 * @Time : 2026/08/05
 * @Author : SXD-AI
 * @Desc : NDJSON 流式解析器测试——覆盖 T-01 至 T-08。
 * <p>
 * [AI生成] B1 周期记+NDJSON流式改造：解析层测试。
 **/
class StreamingMealParserTest {

    private val targetDate = LocalDate(2026, 8, 5) // 周三
    private val genId = "test-gen-1"

    // ═══════════════════════════════════════════════════════════
    // T-01：正常 NDJSON：meal→dish→ingredient → 归组正确
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T01 正常NDJSON 逐行解析后归组成一餐一菜一食材`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "午餐吃了番茄炒蛋", 0)
        )
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

        // 验证 segments
        assertEquals(1, draft.segments.size)
        val seg = draft.segments["quick-2026-08-05"]!!
        assertTrue(seg.done)

        // 验证餐次
        assertEquals(1, seg.meals.size)
        val meal = seg.meals["2026-08-05|lunch"]!!
        assertEquals("2026-08-05", meal.date)
        assertEquals("lunch", meal.slot)
        assertEquals("12:00", meal.time)

        // 验证菜品
        assertEquals(1, meal.dishes.size)
        val dish = meal.dishes["2026-08-05|lunch|d1"]!!
        assertEquals("番茄炒蛋", dish.name)
        assertEquals("炒", dish.cookingMethod)

        // 验证食材
        assertEquals(2, dish.ingredients.size)
        assertEquals("番茄", dish.ingredients[0].name)
        assertEquals("vegetable", dish.ingredients[0].foodGroup)
        assertEquals("鸡蛋", dish.ingredients[1].name)
        assertEquals("egg", dish.ingredients[1].foodGroup)
    }

    // ═══════════════════════════════════════════════════════════
    // T-02：dish 先到但携带合法 date/slot → 补建父餐次并产生 warning
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T02 dish先到但携带合法date和slot 自动补建父餐次并记录warning`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "午餐红烧肉", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        // dish 先到（尚无父 meal）
        val ndjson = """
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","date":"2026-08-05","slot":"lunch"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val seg = draft.segments["quick-2026-08-05"]!!
        assertEquals(1, seg.meals.size)

        // 补建的餐次应存在
        val meal = seg.meals["2026-08-05|lunch"]!!
        assertEquals("2026-08-05", meal.date)
        assertEquals("lunch", meal.slot)
        assertTrue(meal.warnings.any { it.contains("补建了父餐次") })

        // 菜品应挂到该餐次下
        assertEquals(1, meal.dishes.size)
        assertEquals("红烧肉", meal.dishes["2026-08-05|lunch|d1"]!!.name)
    }

    // ═══════════════════════════════════════════════════════════
    // T-03：ingredient 缺失/冲突 dish_id → 不挂菜，进入诊断
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T03 ingredient缺少dish_id或dish_id不存在 进入诊断不静默挂靠`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d99","name":"大米"}
            {"type":"ingredient","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","name":"鸡蛋"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        // 米饭的食材应为空（ingredient dish_id 不存在 + 缺少 dish_id 都被拒绝）
        val meal = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!
        val dish = meal.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(0, dish.ingredients.size)

        // 诊断应包含孤儿事件
        val diagMsgs = draft.diagnostics.map { it.message }
        assertTrue(diagMsgs.any { it.contains("dish_id「2026-08-05|lunch|d99」不存在") })
        assertTrue(diagMsgs.any { it.contains("缺少 dish_id") })
    }

    // ═══════════════════════════════════════════════════════════
    // T-04：同 dish_id 跨 meal_id → 拒绝冲突，保留先到合法归属
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T04 同dish_id跨meal_id 保留先到合法归属拒绝后到冲突`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|dinner","date":"2026-08-05","slot":"dinner"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|dinner","dish_id":"2026-08-05|lunch|d1","name":"米饭-冲突复制"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val seg = draft.segments["quick-2026-08-05"]!!
        // 午餐有米饭
        assertEquals("米饭", seg.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.name)
        // 晚餐不应有该 dish
        assertEquals(0, seg.meals["2026-08-05|dinner"]!!.dishes.size)

        // 诊断应有冲突记录
        assertTrue(draft.diagnostics.any {
            it.message.contains("已存在于") && it.message.contains("2026-08-05|lunch")
        })
    }

    // ═══════════════════════════════════════════════════════════
    // T-05：SSE chunk 在一行 JSON 中间断开 → 仅完整换行后解析
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T05 SSE分片行中间断开 仅完整换行后解析`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        // 模拟 SSE 分片：先发半行，再补全
        parser.feedDelta("""{"type":"meal","seg""")
        // 此时应无任何解析结果（半行在缓冲中）
        assertEquals(0, parser.currentDraft.segments.size)

        parser.feedDelta("""ment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""")
        parser.feedDelta("\n") // 关键：换行符触发
        parser.feedDelta("""{"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")

        val draft = parser.finish("stop")

        assertEquals(1, draft.segments.size)
        val meal = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!
        assertEquals("米饭", meal.dishes["2026-08-05|lunch|d1"]!!.name)
    }

    // ═══════════════════════════════════════════════════════════
    // T-06：完成时半行/未闭合 JSON → 已完成事件保留，尾部仅诊断
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T06 完成时半行保留合法事件 尾部进诊断`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        // 先发完整的 meal
        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        // 再发半行（无 \n）
        parser.feedDelta("""{"type":"dish","segment_id":"quick""")

        val draft = parser.finish("stop")

        // 餐次应完整保留
        assertEquals(1, draft.segments.size)
        assertNotNull(draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"])

        // 诊断应有未完成内容提示
        assertTrue(draft.diagnostics.any { it.message.contains("未完成") })
    }

    // ═══════════════════════════════════════════════════════════
    // T-07：整体 JSON（FlatMealJson）→ 规范化为内部片段
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T07 整体JSON回退 FlatMealJson规范化后可预览`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        // 不喂 NDJSON，直接发整体 JSON（无 \n 无法解析为 NDJSON 行）
        val flatJson = """{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"米饭"},{"date":"2026-08-05","meal_type":"dinner","dish_name":"青菜"}]}"""
        parser.feedDelta(flatJson)
        // 注意：FlatMealJson 没有 \n → 不会被当做 NDJSON 行 → 进入整体 JSON 回退

        val draft = parser.finish("stop")

        // 应有回退诊断
        assertTrue(draft.diagnostics.any { it.message.contains("整体 JSON") || it.message.contains("FlatMealJson") || it.message.contains("MultiDayJson") })

        // 应有至少一个 segment
        assertTrue(draft.segments.isNotEmpty())
    }

    @Test
    fun `T07b 整体JSON回退 MultiDayJson规范化后可预览`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val multiDayJson = """{"schema_version":"1.0","days":[{"date":"2026-08-05","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭","quantity":1,"quantity_unit":"份"}]}]}]}"""
        parser.feedDelta(multiDayJson)

        val draft = parser.finish("stop")

        assertTrue(draft.diagnostics.any { it.message.contains("MultiDayJson") })
        assertTrue(draft.segments.isNotEmpty())
    }

    // ═══════════════════════════════════════════════════════════
    // T-08：finish_reason=length → 截断 warning，合法前缀可确认
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `T08 finish_reason等于length 产生截断警告但保留已解析内容`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        // 喂入完整的 meal 事件
        parser.feedDelta("""{"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")

        val draft = parser.finish("length")

        // 应标记截断
        assertTrue(draft.isTruncated)
        assertEquals("length", draft.finishReason)

        // 截断警告应存在
        assertTrue(draft.diagnostics.any { it.message.contains("截断") })

        // 已解析的餐次应保留
        assertNotNull(draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"])
    }

    // ═══════════════════════════════════════════════════════════
    // 补充：多段（周期记）
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `多段周期记 按segment_id正确隔离`() {
        val weekAnchor = LocalDate(2026, 8, 3) // 周一
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
            {"type":"done","segment_id":"week-2026-08-03-day1"}
            {"type":"done","segment_id":"week-2026-08-03-day2"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        assertEquals(2, draft.segments.size)

        // Day1
        val day1 = draft.segments["week-2026-08-03-day1"]!!
        assertTrue(day1.done)
        assertEquals("米饭", day1.meals["2026-08-03|lunch"]!!.dishes["2026-08-03|lunch|d1"]!!.name)

        // Day2
        val day2 = draft.segments["week-2026-08-03-day2"]!!
        assertTrue(day2.done)
        assertEquals("面条", day2.meals["2026-08-04|dinner"]!!.dishes["2026-08-04|dinner|d1"]!!.name)
    }

    // ═══════════════════════════════════════════════════════════
    // 补充：边界情况
    // ═══════════════════════════════════════════════════════════

    @Test
    fun `未知事件类型忽略但不中断其他行`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"unknown_event","segment_id":"quick-2026-08-05","data":"blah"}
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        // 未知类型应被警告
        assertTrue(draft.diagnostics.any { it.message.contains("未知事件类型") })
        // 但后续合法行应正常解析
        assertNotNull(draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"])
    }

    @Test
    fun `同键重复事件按字段合并 菜品以最新非空字段覆盖`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch","time":"11:00","note":"少盐"}
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch","time":"12:30"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val meal = draft.segments["quick-2026-08-05"]!!.meals["2026-08-05|lunch"]!!
        assertEquals("12:30", meal.time) // 后到覆盖
        assertEquals("少盐", meal.note)  // 旧的保留（后到没有 note）
    }

    @Test
    fun `无效日期格式拒绝meal事件`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-02-30|lunch","date":"2026-02-30","slot":"lunch"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        // 无效日期应被拒绝
        assertEquals(0, draft.segments.size)
        assertTrue(draft.diagnostics.any { it.message.contains("无效") })
    }

    @Test
    fun `meal_id与date竖线slot不一致 拒绝事件`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|breakfast","date":"2026-08-05","slot":"lunch"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        // meal_id 与 date|slot 不匹配应被拒绝
        assertEquals(0, draft.segments.size)
        assertTrue(draft.diagnostics.any { it.message.contains("不一致") })
    }

    @Test
    fun `空segment_id事件进入诊断`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        parser.feedDelta("""{"type":"meal","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val draft = parser.finish("stop")

        assertTrue(draft.diagnostics.any { it.message.contains("缺少 segment_id") })
    }

    @Test
    fun `seasoning和cooking_step事件正确关联到菜品`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉"}
            {"type":"seasoning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"盐","quantity":3,"unit":"g"}
            {"type":"seasoning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"酱油","quantity":10,"unit":"g"}
            {"type":"cooking_step","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","text":"热锅凉油，下五花肉煸炒","order":1}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val dish = draft.segments["quick-2026-08-05"]!!
            .meals["2026-08-05|lunch"]!!
            .dishes["2026-08-05|lunch|d1"]!!

        assertEquals(2, dish.seasonings.size)
        assertEquals("盐", dish.seasonings[0].name)
        assertEquals("酱油", dish.seasonings[1].name)
        assertEquals(1, dish.cookingSteps.size)
        assertEquals("热锅凉油，下五花肉煸炒", dish.cookingSteps[0].text)
    }

    @Test
    fun `warning事件正确关联到不同层级`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)

        val ndjson = """
            {"type":"meal","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}
            {"type":"dish","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}
            {"type":"warning","segment_id":"quick-2026-08-05","message":"全局警告"}
            {"type":"warning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","message":"午餐盐偏多"}
            {"type":"warning","segment_id":"quick-2026-08-05","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","message":"米饭未给份量"}
        """.trimIndent()

        parser.feedDelta(ndjson + "\n")
        val draft = parser.finish("stop")

        val seg = draft.segments["quick-2026-08-05"]!!
        // 段级
        assertTrue(seg.warnings.any { it == "全局警告" })
        // 餐次级
        val meal = seg.meals["2026-08-05|lunch"]!!
        assertTrue(meal.warnings.any { it == "午餐盐偏多" })
        // 菜品级
        val dish = meal.dishes["2026-08-05|lunch|d1"]!!
        assertTrue(dish.warnings.any { it == "米饭未给份量" })
    }

    @Test
    fun `空Delta不改变解析状态`() {
        val segments = listOf(
            InputSegment("quick-2026-08-05", targetDate, "测试", 0)
        )
        val parser = StreamingMealParser(segments, genId, targetDate)
        parser.feedDelta("")
        // 不抛异常
        assertEquals(0, parser.currentDraft.segments.size)
    }
}
