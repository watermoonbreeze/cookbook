package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * AF-14/18: D-01~D-08 精确字段断言 fallback 归属 + T-01~T-08/AF 回归。
 */
class StreamingMealParserTest {
    private val genId = "test-gen-1"

    private fun parser(vararg segs: InputSegment) =
        StreamingMealParser(segs.toList(), genId, segs.firstOrNull()?.targetDate ?: LocalDate(2026, 8, 5))

    // ════════════════════ T-01~T-08 回归 ════════════════════

    @Test fun `T-01 正常NDJSON meal后dish后ingredient归组`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"大米"}""" + "\n")
        val d = p.finish("stop")
        val dish = d.segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals("米饭", dish.name)
        assertEquals(1, dish.ingredients.size)
    }

    @Test fun `T-02 dish先到携带合法date和slot补建父餐次`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val d = p.finish("stop")
        assertNotNull(d.segments["q1"]!!.meals["2026-08-05|lunch"])
        assertTrue(d.segments["q1"]!!.meals["2026-08-05|lunch"]!!.warnings.any { it.contains("补建") })
    }

    @Test fun `T-03 ingredient缺dish_id不静默挂靠进诊断`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","name":"大米"}""" + "\n")
        val d = p.finish("stop")
        val dish = d.segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(0, dish.ingredients.size)
        assertTrue(d.diagnostics.any { it.message.contains("缺少 dish_id") })
    }

    @Test fun `T-04 同dish_id跨meal_id拒绝后到冲突`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|dinner","date":"2026-08-05","slot":"dinner"}""" + "\n")
        // dish_id 前缀与 meal_id 不符（dinner 却用 lunch 的 dish_id）
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|dinner","dish_id":"2026-08-05|lunch|d1","name":"冲突"}""" + "\n")
        val d = p.finish("stop")
        // 格式校验拒绝：dish_id 前缀必须等于 meal_id
        assertEquals(0, d.segments["q1"]!!.meals["2026-08-05|dinner"]!!.dishes.size)
        assertEquals("米饭", d.segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.name)
    }

    @Test fun `T-05 SSE分片行中间断开仅完整换行后解析`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","se""")
        assertEquals(0, p.currentDraft.segments.size)
        p.feedDelta("""gment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        assertNotNull(p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"])
    }

    @Test fun `T-06 完成时半行保留合法事件尾部进诊断`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"qu""")
        val d = p.finish("stop")
        assertNotNull(d.segments["q1"]!!.meals["2026-08-05|lunch"])
        assertTrue(d.diagnostics.any { it.message.contains("未完成") })
    }

    @Test fun `T-07 整体JSON FlatMealJson单segment回退入预览`() {
        val p = parser(InputSegment("s-1", LocalDate(2026, 8, 5), "午餐米饭", 0))
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
        assertNotNull(d.segments["s-1"]!!.meals["2026-08-05|lunch"])
    }

    @Test fun `T-08 finish_reason等于length产生截断警告保留内容`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val d = p.finish("length")
        assertTrue(d.isTruncated)
        assertTrue(d.diagnostics.any { it.message.contains("截断") })
        assertNotNull(d.segments["q1"]!!.meals["2026-08-05|lunch"])
    }

    // ════════════════════ AF-03 归属校验回归 ════════════════════

    @Test fun `AF-03 未知segment_id拒绝不创建`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"unknown","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        val d = p.finish("stop")
        assertFalse(d.segments.containsKey("unknown"))
        assertTrue(d.diagnostics.any { it.message.contains("不匹配") })
    }

    @Test fun `AF-03 非法slot拒绝meal事件`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|morning","date":"2026-08-05","slot":"morning"}""" + "\n")
        val d = p.finish("stop")
        assertEquals(0, d.segments["q1"]?.meals?.size ?: 0)
        assertTrue(d.diagnostics.any { it.message.contains("无效") })
    }

    @Test fun `AF-03 dish_id格式无效拒绝事件`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"invalid","name":"米饭"}""" + "\n")
        val d = p.finish("stop")
        assertEquals(0, d.segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes.size)
    }

    @Test fun `AF-03 dish补建时meal_id与date竖线slot不一致拒绝`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","date":"2026-08-05","slot":"dinner"}""" + "\n")
        val d = p.finish("stop")
        assertEquals(0, d.segments["q1"]?.meals?.size ?: 0)
    }

    @Test fun `AF-08 dish_id等于d0被拒绝`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d0","name":"米饭"}""" + "\n")
        assertEquals(0, p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes.size)
    }

    // ════════════════════ AF-05 合并/去重/补挂回归 ════════════════════

    @Test fun `AF-05 dish同键合并非空字段覆盖保留子项`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","cooking_method":"烧"}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"五花肉","quantity":150}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉","quantity":2}""" + "\n")
        val dish = p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(2.0, dish.quantity)
        assertEquals("烧", dish.cookingMethod)
        assertEquals(1, dish.ingredients.size)
    }

    @Test fun `AF-05 ingredient同名去重合并`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋"}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"鸡蛋","quantity":50}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"鸡蛋","quantity":60,"food_group":"egg"}""" + "\n")
        val dish = p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(1, dish.ingredients.size)
        assertEquals(60.0, dish.ingredients[0].quantity)
        assertEquals("egg", dish.ingredients[0].foodGroup)
    }

    @Test fun `AF-05 dish_name唯一补挂ingredient成功`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"番茄炒蛋"}""" + "\n")
        p.feedDelta("""{"type":"ingredient","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_name":"番茄炒蛋","name":"鸡蛋","quantity":50}""" + "\n")
        val dish = p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(1, dish.ingredients.size)
        assertTrue(dish.warnings.any { it.contains("唯一补挂") })
    }

    @Test fun `seasoning和cooking_step正确关联`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"红烧肉"}""" + "\n")
        p.feedDelta("""{"type":"seasoning","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"盐","quantity":3}""" + "\n")
        p.feedDelta("""{"type":"cooking_step","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","text":"热锅凉油","order":1}""" + "\n")
        val dish = p.finish("stop").segments["q1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!
        assertEquals(1, dish.seasonings.size)
        assertEquals(1, dish.cookingSteps.size)
    }

    @Test fun `warning关联到不同层级`() {
        val p = parser(InputSegment("q1", LocalDate(2026, 8, 5), "x", 0))
        p.feedDelta("""{"type":"meal","segment_id":"q1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        p.feedDelta("""{"type":"warning","segment_id":"q1","message":"全局"}""" + "\n")
        p.feedDelta("""{"type":"warning","segment_id":"q1","meal_id":"2026-08-05|lunch","message":"餐次"}""" + "\n")
        p.feedDelta("""{"type":"warning","segment_id":"q1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","message":"菜品"}""" + "\n")
        val d = p.finish("stop")
        val seg = d.segments["q1"]!!
        assertTrue(seg.warnings.any { it == "全局" })
        assertTrue(seg.meals["2026-08-05|lunch"]!!.warnings.any { it == "餐次" })
        assertTrue(seg.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.warnings.any { it == "菜品" })
    }

    // ════════════════════ D-01~D-08 fallback（§7.5.6 精确字段断言）════════════════════

    // D-01: 绝对日期，对象 → key=s-1, date/meal_id=2026-08-10
    @Test fun `D-01 绝对日期对象 断言key-date-mealId`() {
        val p = parser(InputSegment("s-1", LocalDate(2026, 8, 5), "8月10日午餐米饭", 0))
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-10","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
        val meal = d.segments["s-1"]!!.meals["2026-08-10|lunch"]
        assertNotNull(meal, "meal_id 必须为 2026-08-10|lunch")
        assertEquals("2026-08-10", meal!!.date)
    }

    // D-02: 绝对日期，数组 → 同 D-01
    @Test fun `D-02 绝对日期数组 断言key-date-mealId`() {
        val p = parser(InputSegment("s-1", LocalDate(2026, 8, 5), "8月10日午餐米饭", 0))
        p.feedDelta("""{"days":[{"date":"2026-08-10","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭"}]}]}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
        val meal = d.segments["s-1"]!!.meals["2026-08-10|lunch"]
        assertNotNull(meal)
        assertEquals("2026-08-10", meal!!.date)
    }

    // D-03: 星期，对象 → target 2026-08-05(周三)，原始 date=2026-08-10，结果必须 2026-08-05
    @Test fun `D-03 星期对象 断言key-date-mealId为周三`() {
        val p = parser(InputSegment("s-1", LocalDate(2026, 8, 5), "周三午餐", 0))
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-10","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
        val meal = d.segments["s-1"]!!.meals["2026-08-05|lunch"]
        assertNotNull(meal, "星期必须映射到所选周周三 2026-08-05")
        assertEquals("2026-08-05", meal!!.date)
    }

    // D-04: 星期，数组 → 同 D-03
    @Test fun `D-04 星期数组 断言key-date-mealId为周三`() {
        val p = parser(InputSegment("s-1", LocalDate(2026, 8, 5), "周三午餐", 0))
        p.feedDelta("""{"days":[{"date":"2026-08-10","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭"}]}]}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-1"))
        val meal = d.segments["s-1"]!!.meals["2026-08-05|lunch"]
        assertNotNull(meal)
        assertEquals("2026-08-05", meal!!.date)
    }

    // D-05: 无日期，对象 → owner s-2 target 2026-08-06，原始错误 date=2026-08-10 → 结果 2026-08-06
    @Test fun `D-05 无日期对象 模型错误日期被覆盖为targetDate`() {
        val p = parser(InputSegment("s-2", LocalDate(2026, 8, 6), "午餐米饭", 1))
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-10","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-2"))
        val meal = d.segments["s-2"]!!.meals["2026-08-06|lunch"]
        assertNotNull(meal, "无日期时必须使用 owner target 2026-08-06")
        assertEquals("2026-08-06", meal!!.date)
    }

    // D-06: 无日期，数组 → 同 D-05
    @Test fun `D-06 无日期数组 模型错误日期被覆盖为targetDate`() {
        val p = parser(InputSegment("s-2", LocalDate(2026, 8, 6), "午餐米饭", 1))
        p.feedDelta("""{"days":[{"date":"2026-08-10","meals":[{"meal_type":"lunch","dishes":[{"name":"米饭"}]}]}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.containsKey("s-2"))
        val meal = d.segments["s-2"]!!.meals["2026-08-06|lunch"]
        assertNotNull(meal)
        assertEquals("2026-08-06", meal!!.date)
    }

    // D-07: 多段 fallback 拒绝
    @Test fun `D-07 多段fallback拒绝 无preview节点`() {
        val p = parser(
            InputSegment("s-1", LocalDate(2026, 8, 5), "周一", 0),
            InputSegment("s-2", LocalDate(2026, 8, 6), "周二", 1),
        )
        p.feedDelta("""{"schema_version":"2.0","items":[{"date":"2026-08-05","meal_type":"lunch","dish_name":"米饭"}]}""")
        val d = p.finish("stop")
        assertTrue(d.segments.isEmpty(), "多段 fallback 不得创建任何 draft")
        assertTrue(d.diagnostics.any { it.message.contains("whole_json_fallback_requires_single_segment") })
    }

    // D-08: NDJSON 第二段归属（显式 segment_id=s-2）
    @Test fun `D-08 NDJSON第二段归属只进s2`() {
        val p = parser(
            InputSegment("s-1", LocalDate(2026, 8, 5), "周一", 0),
            InputSegment("s-2", LocalDate(2026, 8, 6), "周二", 1),
        )
        p.feedDelta("""{"type":"meal","segment_id":"s-2","meal_id":"2026-08-06|lunch","date":"2026-08-06","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"s-2","meal_id":"2026-08-06|lunch","dish_id":"2026-08-06|lunch|d1","name":"米饭"}""" + "\n")
        val d = p.finish("stop")
        assertFalse(d.segments.containsKey("s-1"), "s-1 必须为空")
        assertNotNull(d.segments["s-2"]!!.meals["2026-08-06|lunch"])
    }

    // ════════════════════ 周期记多段 NDJSON ════════════════════

    @Test fun `周期记两段按segment_id隔离`() {
        val p = parser(
            InputSegment("s-1", LocalDate(2026, 8, 5), "周一", 0),
            InputSegment("s-2", LocalDate(2026, 8, 6), "周二", 1),
        )
        p.feedDelta("""{"type":"meal","segment_id":"s-1","meal_id":"2026-08-05|lunch","date":"2026-08-05","slot":"lunch"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"s-1","meal_id":"2026-08-05|lunch","dish_id":"2026-08-05|lunch|d1","name":"米饭"}""" + "\n")
        p.feedDelta("""{"type":"meal","segment_id":"s-2","meal_id":"2026-08-06|dinner","date":"2026-08-06","slot":"dinner"}""" + "\n")
        p.feedDelta("""{"type":"dish","segment_id":"s-2","meal_id":"2026-08-06|dinner","dish_id":"2026-08-06|dinner|d1","name":"面条"}""" + "\n")
        val d = p.finish("stop")
        assertEquals(2, d.segments.size)
        assertEquals("米饭", d.segments["s-1"]!!.meals["2026-08-05|lunch"]!!.dishes["2026-08-05|lunch|d1"]!!.name)
        assertEquals("面条", d.segments["s-2"]!!.meals["2026-08-06|dinner"]!!.dishes["2026-08-06|dinner|d1"]!!.name)
    }
}
