package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/** [AI修改] 当前 AI 扁平餐食协议的解析、校验与规则降级测试。 */
class AiMealParserTest {
    private val targetDate = LocalDate(2026, 8, 4)

    @Test
    fun `扁平JSON经聚合和校验后可预览`() {
        val outcome = AiMealParser.parseOutcome(
            """{"items":[{"date":"2026-08-03","meal_type":"lunch","dish_name":"米饭"},{"date":"2026-08-03","meal_type":"dinner","dish_name":"青菜"}]}""",
            targetDate,
        )
        assertTrue(outcome.isValid)
        assertEquals(1, outcome.days.size)
        assertEquals(2, outcome.days.single().meals.size)
    }

    @Test
    fun `非法份量阻断AI结果`() {
        val outcome = AiMealParser.parseOutcome(
            """{"items":[{"date":"2026-08-03","meal_type":"lunch","dish_name":"米饭","dish_quantity":0}]}""",
            targetDate,
        )
        assertFalse(outcome.isValid)
        assertTrue(outcome.errors.any { it.contains("份量无效") })
    }

    @Test
    fun `缺失日期会提示但仍按目标日期预览`() {
        val outcome = AiMealParser.parseOutcome(
            """{"items":[{"meal_type":"lunch","dish_name":"米饭"}]}""",
            targetDate,
        )
        assertTrue(outcome.isValid)
        assertTrue(outcome.warnings.isNotEmpty())
        assertEquals(targetDate.toString(), outcome.days.single().date)
    }

    @Test
    fun `可选字段为null不应使有效菜品降级`() {
        val outcome = AiMealParser.parseOutcome(
            """{"items":[{"date":"2026-08-15","meal_type":"lunch","meal_note":null,"dish_name":"红烧肉","dish_note":null,"dish_cuisine":null,"dish_tags":null,"dish_cooking_methods":null,"ingredients":null}]}""",
            targetDate,
        )
        assertTrue(outcome.isValid, "可选 null 应回退默认值，不应丢弃整餐：${outcome.errors}")
        assertEquals("红烧肉", outcome.days.single().meals.single().dishes.single().name)
    }
}
