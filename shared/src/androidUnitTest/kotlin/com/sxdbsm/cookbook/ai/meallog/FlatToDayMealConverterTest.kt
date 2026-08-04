package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** [AI生成] AI 扁平餐食按真实日期、再按餐次聚合的回归测试。 */
class FlatToDayMealConverterTest {

    @Test
    fun `同日早午餐聚合为一天且保留两个餐次`() {
        val result = FlatToDayMealConverter.convert(
            FlatMealJson(items = listOf(
                FlatMealItem(date = "2026-08-04", meal_type = "breakfast", dish_name = "鸡蛋"),
                FlatMealItem(date = "2026-08-04", meal_type = "lunch", dish_name = "米饭"),
            )),
            LocalDate(2026, 8, 1),
        )

        assertTrue(result.errors.isEmpty())
        assertEquals(1, result.days.size)
        assertEquals("2026-08-04", result.days.single().date)
        assertEquals(listOf("breakfast", "lunch"), result.days.single().meals.map { it.meal_type })
    }

    @Test
    fun `不同绝对日期的同餐次不得混合`() {
        val result = FlatToDayMealConverter.convert(
            FlatMealJson(items = listOf(
                FlatMealItem(date = "2026-08-03", meal_type = "lunch", dish_name = "面条"),
                FlatMealItem(date = "2026-08-04", meal_type = "lunch", dish_name = "米饭"),
            )),
            LocalDate(2026, 8, 1),
        )

        assertEquals(2, result.days.size)
        assertEquals(listOf("2026-08-03", "2026-08-04"), result.days.map { it.date })
    }

    @Test
    fun `缺失日期使用目标日期并给出可见提示`() {
        val result = FlatToDayMealConverter.convert(
            FlatMealJson(items = listOf(FlatMealItem(meal_type = "lunch", dish_name = "米饭"))),
            LocalDate(2026, 8, 4),
        )

        assertEquals("2026-08-04", result.days.single().date)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun `非法绝对日期阻断AI结果`() {
        val result = FlatToDayMealConverter.convert(
            FlatMealJson(items = listOf(FlatMealItem(date = "2026-02-30", meal_type = "lunch", dish_name = "米饭"))),
            LocalDate(2026, 8, 4),
        )

        assertFalse(result.errors.isEmpty())
    }
}
