package com.sxdbsm.cookbook.ai.meallog

import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** [AI生成] 以添加页所选日期为唯一锚点的回归测试。 */
class MealDateAnchorPolicyTest {

    @Test
    fun `无日期时覆盖模型擅自填入的当天`() {
        val selected = LocalDate(2026, 8, 14)
        val result = MealDateAnchorPolicy.apply(
            input = "午餐吃了番茄炒蛋",
            targetDate = selected,
            days = listOf(DayMealJson(date = "2026-08-04")),
        )

        assertEquals(selected.toString(), result.days.single().date)
        assertEquals(0, result.days.single().date_offset)
        assertEquals("当前餐食以选择的餐食日期为参照。", result.warning)
    }

    @Test
    fun `星期按所选日期所在周计算`() {
        val selected = LocalDate(2026, 8, 13)
        val result = MealDateAnchorPolicy.apply(
            input = "周一 午餐米饭\n周三 晚餐青菜",
            targetDate = selected,
            days = listOf(DayMealJson(weekday = "周一"), DayMealJson(weekday = "周三")),
        )

        assertNull(result.days[0].date)
        assertEquals(-3, result.days[0].date_offset)
        assertEquals(-1, result.days[1].date_offset)
        assertEquals("当前餐食以选择的餐食日期为参照；星期按该日期所在周计算。", result.warning)
    }

    @Test
    fun `输入包含绝对日期时保留已解析日期`() {
        val result = MealDateAnchorPolicy.apply(
            input = "8月14日午餐吃了米饭",
            targetDate = LocalDate(2026, 8, 13),
            days = listOf(DayMealJson(date = "2026-08-14")),
        )

        assertEquals("2026-08-14", result.days.single().date)
        assertNull(result.warning)
    }
}
