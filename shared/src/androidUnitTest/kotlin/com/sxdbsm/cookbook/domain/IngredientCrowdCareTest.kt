package com.sxdbsm.cookbook.domain

import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientNutrition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : IngredientCrowdCareTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 食材·人群适配红绿灯纯函数单测（商业#6）——四人群三级 + 缺数据 + 关键词兜底 + 单向压制。
 * [AI生成]
 **/
class IngredientCrowdCareTest {

    private fun nut(
        sodium: Double? = null, gi: Double? = null, purine: Double? = null,
        sat: Double? = null, chol: Double? = null,
    ) = IngredientNutrition(
        ingredientId = 1L, sodiumMg = sodium, gi = gi, purineMg = purine,
        saturatedFatG = sat, cholesterolMg = chol,
    )

    private fun fitOf(list: List<CrowdCareVerdict>, c: HealthCondition) = list.first { it.condition == c }

    @Test
    fun 顺序固定为高血压糖尿病高血脂痛风() {
        val r = IngredientCrowdCare.evaluate("大米", nut())
        assertEquals(IngredientCrowdCare.ORDER, r.map { it.condition })
    }

    @Test
    fun 全无营养四行皆暂无数据() {
        val r = IngredientCrowdCare.evaluate("神秘食材", null)
        assertTrue(r.all { it.fit == CrowdFit.NO_DATA && it.reason.isEmpty() })
    }

    @Test
    fun 高钠食材对高血压慎选() {
        val r = IngredientCrowdCare.evaluate("酱油", nut(sodium = 5000.0))
        val v = fitOf(r, HealthCondition.HYPERTENSION)
        assertEquals(CrowdFit.CAUTION, v.fit)
        assertEquals("钠偏高", v.reason)
    }

    @Test
    fun 低钠食材对高血压宜() {
        val r = IngredientCrowdCare.evaluate("番茄", nut(sodium = 5.0))
        assertEquals(CrowdFit.FIT, fitOf(r, HealthCondition.HYPERTENSION).fit)
    }

    @Test
    fun 中钠食材对高血压留意() {
        val r = IngredientCrowdCare.evaluate("某食材", nut(sodium = 300.0))
        assertEquals(CrowdFit.MIND, fitOf(r, HealthCondition.HYPERTENSION).fit)
    }

    @Test
    fun 高GI对糖尿病慎选低GI宜() {
        assertEquals(CrowdFit.CAUTION, fitOf(IngredientCrowdCare.evaluate("白米饭", nut(gi = 83.0)), HealthCondition.DIABETES).fit)
        assertEquals(CrowdFit.FIT, fitOf(IngredientCrowdCare.evaluate("燕麦", nut(gi = 55.0)), HealthCondition.DIABETES).fit)
    }

    @Test
    fun 缺钠数据高血压暂无数据但其他指标独立判定() {
        val r = IngredientCrowdCare.evaluate("某食材", nut(gi = 40.0)) // 只有 GI
        assertEquals(CrowdFit.NO_DATA, fitOf(r, HealthCondition.HYPERTENSION).fit)
        assertEquals(CrowdFit.FIT, fitOf(r, HealthCondition.DIABETES).fit)
    }

    @Test
    fun 嘌呤实测高对痛风慎选() {
        val r = IngredientCrowdCare.evaluate("沙丁鱼", nut(purine = 399.0))
        assertEquals(CrowdFit.CAUTION, fitOf(r, HealthCondition.GOUT).fit)
    }

    @Test
    fun 缺嘌呤值但名含内脏关键词兜底判慎选() {
        val r = IngredientCrowdCare.evaluate("猪肝", nut()) // 无嘌呤值，名含"肝"命中应避免关键词
        assertEquals(CrowdFit.CAUTION, fitOf(r, HealthCondition.GOUT).fit)
    }

    @Test
    fun 缺嘌呤值且名不命中关键词判暂无数据() {
        val r = IngredientCrowdCare.evaluate("生菜", nut())
        assertEquals(CrowdFit.NO_DATA, fitOf(r, HealthCondition.GOUT).fit)
    }

    @Test
    fun 高血脂饱脂或胆固醇高判慎选取较重者() {
        // 饱脂高、胆固醇低 → 取较重者 HIGH
        val r1 = IngredientCrowdCare.evaluate("黄油", nut(sat = 50.0, chol = 40.0))
        assertEquals(CrowdFit.CAUTION, fitOf(r1, HealthCondition.HYPERLIPIDEMIA).fit)
        // 均低 → 宜
        val r2 = IngredientCrowdCare.evaluate("脱脂奶", nut(sat = 0.1, chol = 5.0))
        assertEquals(CrowdFit.FIT, fitOf(r2, HealthCondition.HYPERLIPIDEMIA).fit)
        // 饱脂缺、胆固醇高 → 单侧非空路径仍判 CAUTION
        val r3 = IngredientCrowdCare.evaluate("蛋黄", nut(chol = 585.0))
        assertEquals(CrowdFit.CAUTION, fitOf(r3, HealthCondition.HYPERLIPIDEMIA).fit)
    }

    @Test
    fun 人工建议AVOID单向压制数据判宜升为留意() {
        // 数据：低钠(本应 FIT)，但人工 care 对高血压标 AVOID → 压成 MIND
        val care = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "高血压", adviceLevel = AdviceLevel.AVOID))
        val r = IngredientCrowdCare.evaluate("低钠腌菜", nut(sodium = 5.0), care)
        val v = fitOf(r, HealthCondition.HYPERTENSION)
        assertEquals(CrowdFit.MIND, v.fit)
        assertEquals("见上方宜忌", v.reason)
    }

    @Test
    fun 人工建议RECOMMEND不压制() {
        val care = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "高血压", adviceLevel = AdviceLevel.RECOMMEND))
        val r = IngredientCrowdCare.evaluate("番茄", nut(sodium = 5.0), care)
        assertEquals(CrowdFit.FIT, fitOf(r, HealthCondition.HYPERTENSION).fit)
    }

    @Test
    fun 单向压制不洗白高数据不因无care降级() {
        // 数据判 CAUTION，无 care → 仍 CAUTION（压制只抬升不降级）
        val r = IngredientCrowdCare.evaluate("酱油", nut(sodium = 5000.0))
        assertEquals(CrowdFit.CAUTION, fitOf(r, HealthCondition.HYPERTENSION).fit)
    }
}
