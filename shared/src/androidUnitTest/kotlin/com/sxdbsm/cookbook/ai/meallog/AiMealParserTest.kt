package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * @File : AiMealParserTest
 * @Time : 2026/07/28
 * @Author : SXD-AI
 * @Desc : AiMealParser 单元测试（JSON 解析 + 本地兜底）
 * <p>
 * [AI生成] K1 AI快捷输入记餐：Parser 单测。
 **/
class AiMealParserTest {

    // ===== JSON 解析 =====

    @Test
    fun `合法 JSON 解析成功`() {
        val json = """{"date_offset":-1,"meals":[{"meal_type":"lunch","dishes":[{"name":"红烧肉"},{"name":"米饭"}]}]}"""
        val result = AiMealParser.parse(json)
        assertNotNull(result)
        assertEquals(-1, result.date_offset)
        assertEquals(1, result.meals.size)
        assertEquals("lunch", result.meals[0].meal_type)
        assertEquals(2, result.meals[0].dishes.size)
        assertEquals("红烧肉", result.meals[0].dishes[0].name)
    }

    @Test
    fun `JSON 含 Markdown 代码块也能解析`() {
        val json = """```json
{"date_offset":0,"meals":[{"meal_type":"dinner","dishes":[{"name":"清蒸鲈鱼"}]}]}
```"""
        val result = AiMealParser.parse(json)
        assertNotNull(result)
        assertEquals(0, result.date_offset)
        assertEquals("dinner", result.meals[0].meal_type)
        assertEquals("清蒸鲈鱼", result.meals[0].dishes[0].name)
    }

    @Test
    fun `空响应返回 null`() {
        assertNull(AiMealParser.parse(""))
        assertNull(AiMealParser.parse("   "))
    }

    @Test
    fun `非法 JSON 返回 null`() {
        assertNull(AiMealParser.parse("这不是JSON"))
        assertNull(AiMealParser.parse("{broken json"))
    }

    @Test
    fun `JSON 无 meals 返回 null`() {
        assertNull(AiMealParser.parse("""{"date_offset":0,"meals":[]}"""))
    }

    @Test
    fun `JSON meal 无 dishes 返回 null`() {
        assertNull(AiMealParser.parse("""{"date_offset":0,"meals":[{"meal_type":"lunch","dishes":[]}]}"""))
    }

    @Test
    fun `含份量和食用比例的完整 JSON`() {
        val json = """{"date_offset":-1,"meals":[{"meal_type":"lunch","meal_time":"12:30","note":"少盐","dishes":[
            {"name":"红烧肉","quantity":1,"quantity_unit":"份","eaten_ratio":0.5,"cooking_methods":["烧"]},
            {"name":"米饭","quantity":2,"quantity_unit":"碗"}
        ]}]}"""
        val result = AiMealParser.parse(json)
        assertNotNull(result)
        assertEquals(-1, result.date_offset)
        val meal = result.meals[0]
        assertEquals("lunch", meal.meal_type)
        assertEquals("12:30", meal.meal_time)
        assertEquals("少盐", meal.note)
        assertEquals(2, meal.dishes.size)
        assertEquals(0.5, meal.dishes[0].eaten_ratio)
        assertEquals(2.0, meal.dishes[1].quantity)
        assertEquals("碗", meal.dishes[1].quantity_unit)
    }

    @Test
    fun `含 AI 推断食材的 JSON`() {
        val json = """{"date_offset":0,"meals":[{"meal_type":"lunch","dishes":[
            {"name":"牛肉面","ingredients":[{"name":"牛肉","quantity":80,"unit":"g","is_main":true},{"name":"面条","quantity":150,"unit":"g","is_main":true}]}
        ]}]}"""
        val result = AiMealParser.parse(json)
        assertNotNull(result)
        val dish = result.meals[0].dishes[0]
        assertEquals(2, dish.ingredients.size)
        assertEquals("牛肉", dish.ingredients[0].name)
        assertEquals(80.0, dish.ingredients[0].quantity)
    }

    // ===== 本地规则兜底 =====

    @Test
    fun `本地兜底——简单菜名`() {
        val result = AiMealParser.localFallback("中午吃了红烧肉")
        assertEquals(1, result.meals.size)
        assertEquals("lunch", result.meals[0].meal_type)
        assertEquals(1, result.meals[0].dishes.size)
        assertEquals("红烧肉", result.meals[0].dishes[0].name)
    }

    @Test
    fun `本地兜底——逗号分隔多道菜`() {
        val result = AiMealParser.localFallback("鸡蛋、米饭")
        assertTrue(result.meals[0].dishes.size >= 1)
    }

    @Test
    fun `本地兜底——三个鸡蛋解析份量`() {
        val result = AiMealParser.localFallback("三个鸡蛋")
        val dish = result.meals[0].dishes.firstOrNull { it.name.contains("蛋") }
        if (dish != null) {
            assertEquals(3.0, dish.quantity)
            assertEquals("个", dish.quantity_unit)
        }
    }

    @Test
    fun `本地兜底——昨天早餐`() {
        val result = AiMealParser.localFallback("昨天早上吃了小米粥")
        assertEquals(-1, result.date_offset)
        assertEquals("breakfast", result.meals[0].meal_type)
    }

    @Test
    fun `本地兜底——食用比例一半`() {
        val result = AiMealParser.localFallback("红烧肉吃了一半")
        val dish = result.meals[0].dishes.firstOrNull { it.name.contains("红烧肉") || it.name.contains("肉") }
        if (dish != null) {
            assertEquals(0.5, dish.eaten_ratio)
        }
    }

    @Test
    fun `本地兜底——少盐备注`() {
        val result = AiMealParser.localFallback("少盐番茄炒蛋")
        assertTrue(result.meals[0].note.contains("少盐"))
    }

    @Test
    fun `本地兜底——空输入返回默认`() {
        val result = AiMealParser.localFallback("")
        assertEquals(0, result.date_offset)
        assertTrue(result.meals.isEmpty() || result.meals[0].dishes.isEmpty())
    }

    @Test
    fun `本地兜底——今晚吃面`() {
        val result = AiMealParser.localFallback("晚上吃了牛肉面")
        assertEquals("dinner", result.meals[0].meal_type)
    }

    @Test
    fun `本地兜底括号内加号不拆菜`() {
        val result = AiMealParser.localFallback("晚饭 凉皮（黄瓜丝+绿豆芽）+番茄炒蛋")

        assertEquals(listOf("凉皮（黄瓜丝+绿豆芽）", "番茄炒蛋"), result.meals.single().dishes.map { it.name })
    }
}
