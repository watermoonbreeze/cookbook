package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * B3: MealStreamDraftMapper 纯映射测试（蓝图 §3.2 + T-B3-09）。
 */
class MealStreamDraftMapperTest {

    private fun seg(id: String, date: LocalDate, text: String, ord: Int) =
        InputSegment(id, date, text, ord)

    private fun dish(dishId: String, name: String, cooking: String? = null,
                     ingredients: List<DraftIngredient> = emptyList(),
                     seasonings: List<DraftSeasoning> = emptyList(),
                     steps: List<DraftCookingStep> = emptyList()): DishDraftNode =
        DishDraftNode(dishId = dishId, name = name, cookingMethod = cooking,
            ingredients = ingredients, seasonings = seasonings, cookingSteps = steps)

    @Test
    fun `T09 日期餐次排序与同餐合并`() {
        val segments = listOf(
            seg("s-1", LocalDate(2026, 8, 5), "周三", 0),
            seg("s-2", LocalDate(2026, 8, 6), "周四", 1),
        )
        val draft = MealStreamDraft(
            segments = mapOf(
                "s-1" to SegmentDraft("s-1", meals = mapOf(
                    "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                        dishes = mapOf("2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", "米饭"))),
                    "2026-08-05|breakfast" to MealDraftNode("2026-08-05|breakfast", "2026-08-05", "breakfast",
                        dishes = mapOf("2026-08-05|breakfast|d1" to dish("2026-08-05|breakfast|d1", "鸡蛋"))),
                )),
                "s-2" to SegmentDraft("s-2", meals = mapOf(
                    "2026-08-06|dinner" to MealDraftNode("2026-08-06|dinner", "2026-08-06", "dinner",
                        dishes = mapOf("2026-08-06|dinner|d1" to dish("2026-08-06|dinner|d1", "面条"))),
                )),
            ),
        )
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)

        // 日期升序：08-05 在前
        assertEquals(2, days.size)
        assertEquals("2026-08-05", days[0].date)
        assertEquals("2026-08-06", days[1].date)
        // 同日餐次 slot 顺序：breakfast 在 lunch 前
        assertEquals(listOf("breakfast", "lunch"), days[0].meals.map { it.meal_type })
        // raw_input 取 owner segment
        assertEquals("周三", days[0].raw_input)
        assertEquals("周四", days[1].raw_input)
        assertEquals("ai", days[0].parse_method)
    }

    @Test
    fun `T09 seasoning转is_main等于false并保留`() {
        val segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周三", 0))
        val draft = MealStreamDraft(segments = mapOf(
            "s-1" to SegmentDraft("s-1", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf(
                        "2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", "红烧肉",
                            ingredients = listOf(DraftIngredient("五花肉", quantity = 150.0)),
                            seasonings = listOf(DraftSeasoning("盐", quantity = 3.0))),
                    )),
            )),
        ))
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)
        val ings = days.single().meals.single().dishes.single().dish!!.ingredients

        assertEquals(2, ings.size)
        assertTrue(ings[0].is_main, "主料 is_main=true")
        assertFalse(ings[1].is_main, "调料 is_main=false")
        assertEquals("盐", ings[1].food?.name)
        assertEquals(3.0, ings[1].quantity)
    }

    @Test
    fun `T09 乱序steps按order排序`() {
        val segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周三", 0))
        val draft = MealStreamDraft(segments = mapOf(
            "s-1" to SegmentDraft("s-1", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf(
                        "2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", "菜",
                            steps = listOf(
                                DraftCookingStep("步骤3", 3),
                                DraftCookingStep("步骤1", 1),
                                DraftCookingStep("步骤2", 2),
                            )),
                    )),
            )),
        ))
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)
        val steps = days.single().meals.single().dishes.single().dish!!.steps
        assertEquals(listOf("步骤1", "步骤2", "步骤3"), steps)
    }

    @Test
    fun `T09 未知segment不生成day`() {
        val segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周三", 0))
        val draft = MealStreamDraft(segments = mapOf(
            "s-1" to SegmentDraft("s-1", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf("2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", "米饭"))),
            )),
            "unknown-seg" to SegmentDraft("unknown-seg", meals = mapOf(
                "2026-08-99|lunch" to MealDraftNode("2026-08-99|lunch", "2026-08-99", "lunch",
                    dishes = mapOf("2026-08-99|lunch|d1" to dish("2026-08-99|lunch|d1", "幽灵菜"))),
            )),
        ))
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)

        assertEquals(1, days.size)
        assertEquals("2026-08-05", days.single().date)
    }

    @Test
    fun `AF-B3-06 两个segment同date和mealId两dish合并为一个meal`() {
        val segments = listOf(
            seg("s-1", LocalDate(2026, 8, 5), "周三", 0),
            seg("s-2", LocalDate(2026, 8, 5), "周三", 1),
        )
        val draft = MealStreamDraft(segments = mapOf(
            "s-1" to SegmentDraft("s-1", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf("2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", "米饭"))),
            )),
            "s-2" to SegmentDraft("s-2", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf("2026-08-05|lunch|d2" to dish("2026-08-05|lunch|d2", "青菜"))),
            )),
        ))
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)

        // 只产一个 day、一个 meal，两 dish 都保留
        assertEquals(1, days.size)
        val meals = days.single().meals
        assertEquals(1, meals.size)
        assertEquals(listOf("米饭", "青菜"), meals.single().dishes.map { it.name })
        // raw_input 取 ordinal 最小 segment
        assertEquals("周三", days.single().raw_input)
    }

    @Test
    fun `AF-B3-06 乱序map输入不改变结果`() {
        val segments = listOf(
            seg("s-1", LocalDate(2026, 8, 5), "周三", 0),
            seg("s-2", LocalDate(2026, 8, 6), "周四", 1),
        )
        // 反向构造 map 顺序
        val draft = MealStreamDraft(segments = linkedMapOf(
            "s-2" to SegmentDraft("s-2", meals = mapOf(
                "2026-08-06|dinner" to MealDraftNode("2026-08-06|dinner", "2026-08-06", "dinner",
                    dishes = mapOf("2026-08-06|dinner|d1" to dish("2026-08-06|dinner|d1", "面条"))),
            )),
            "s-1" to SegmentDraft("s-1", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf("2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", "米饭"))),
            )),
        ))
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)

        // 日期仍按 ordinal 驱动排序：08-05 在前
        assertEquals(listOf("2026-08-05", "2026-08-06"), days.map { it.date })
        assertEquals("周三", days[0].raw_input)
    }

    @Test
    fun `T09 无名称dish不生成meal`() {
        val segments = listOf(seg("s-1", LocalDate(2026, 8, 5), "周三", 0))
        val draft = MealStreamDraft(segments = mapOf(
            "s-1" to SegmentDraft("s-1", meals = mapOf(
                "2026-08-05|lunch" to MealDraftNode("2026-08-05|lunch", "2026-08-05", "lunch",
                    dishes = mapOf(
                        "2026-08-05|lunch|d1" to dish("2026-08-05|lunch|d1", ""),
                        "2026-08-05|lunch|d2" to dish("2026-08-05|lunch|d2", "米饭"),
                    )),
            )),
        ))
        val days = MealStreamDraftMapper.toDayMealJson(draft, segments)

        // 只保留有名称的菜
        assertEquals(1, days.single().meals.single().dishes.size)
        assertEquals("米饭", days.single().meals.single().dishes.single().name)
    }
}
