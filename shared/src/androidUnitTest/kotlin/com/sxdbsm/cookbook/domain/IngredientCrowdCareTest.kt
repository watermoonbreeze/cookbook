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
    fun 人工建议AVOID单向压制数据判宜升为慎选_LIMIT升为留意() {
        // [AI修改] AVOID→慎选(CAUTION红)、LIMIT→留意(MIND黄)：数据低值(本应FIT)但临床应避免须显红。
        //   如痛风忌啤酒(数据低嘌呤绿·care avoid)→应显慎选而非留意。
        val avoid = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "高血压", adviceLevel = AdviceLevel.AVOID))
        val rA = IngredientCrowdCare.evaluate("低钠腌菜", nut(sodium = 5.0), avoid)
        val vA = fitOf(rA, HealthCondition.HYPERTENSION)
        assertEquals(CrowdFit.CAUTION, vA.fit) // AVOID → 慎选(红)
        assertEquals("见上方宜忌", vA.reason)
        // LIMIT 仍压成留意(黄)
        val limit = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "高血压", adviceLevel = AdviceLevel.LIMIT))
        val rL = IngredientCrowdCare.evaluate("低钠腌菜", nut(sodium = 5.0), limit)
        assertEquals(CrowdFit.MIND, fitOf(rL, HealthCondition.HYPERTENSION).fit) // LIMIT → 留意(黄)
    }

    @Test
    fun 人工建议AVOID对数据判留意的也升为慎选_录低值反判绿修复() {
        // [AI生成] 健康安全修复(审计 F#附2)：动物内脏录了偏低实测嘌呤→痛风数据判 MID(留意/黄)，
        //   但临床应避免、care avoid 须把它升为慎选(红)。原引擎压制只作用 FIT→此类停在黄(bug)。
        val avoid = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "痛风", adviceLevel = AdviceLevel.AVOID))
        // 嘌呤 133(MID·25<133<150) → 数据判留意；care avoid → 应升慎选
        val r = IngredientCrowdCare.evaluate("猪腰", nut(purine = 133.0), avoid)
        val v = fitOf(r, HealthCondition.GOUT)
        assertEquals(CrowdFit.CAUTION, v.fit)
        assertEquals("见上方宜忌", v.reason)
        // LIMIT 对数据判 MID 不再抬升(取更严者·careFit=MIND 不比 MID 更严)→保留留意
        val limit = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "痛风", adviceLevel = AdviceLevel.LIMIT))
        val rL = IngredientCrowdCare.evaluate("猪腰", nut(purine = 133.0), limit)
        assertEquals(CrowdFit.MIND, fitOf(rL, HealthCondition.GOUT).fit)
        // 数据已 HIGH(慎选) + care LIMIT → 不降级，保留慎选(单向只升不降)
        val rH = IngredientCrowdCare.evaluate("沙丁鱼", nut(purine = 399.0), limit)
        assertEquals(CrowdFit.CAUTION, fitOf(rH, HealthCondition.GOUT).fit)
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

    @Test
    fun 植物来源高嘌呤对痛风降留意不判红_动物仍红_Q1修复() {
        // [AI生成] Q1修复:《成人高尿酸血症与痛风食养指南(2024)》植物嘌呤利用率低、不增痛风风险→
        //   植物高嘌呤(黄豆/干香菇)显"留意(MIND)"非"慎选红(CAUTION)"、reason=植物专门措辞；动物高嘌呤仍红。
        val 黄豆 = fitOf(IngredientCrowdCare.evaluate("黄豆", nut(purine = 190.0)), HealthCondition.GOUT)
        assertEquals(CrowdFit.MIND, 黄豆.fit)
        assertEquals("植物嘌呤·利用率低", 黄豆.reason)
        assertEquals(CrowdFit.MIND, fitOf(IngredientCrowdCare.evaluate("干香菇", nut(purine = 214.0)), HealthCondition.GOUT).fit)
        // 动物来源高嘌呤仍判慎选红(不受影响·守现有行为)
        assertEquals(CrowdFit.CAUTION, fitOf(IngredientCrowdCare.evaluate("沙丁鱼", nut(purine = 399.0)), HealthCondition.GOUT).fit)
        // 植物·中嘌呤(不触发降级·本就 MID)措辞仍为"嘌呤中等"
        assertEquals("嘌呤中等", fitOf(IngredientCrowdCare.evaluate("豆腐", nut(purine = 100.0)), HealthCondition.GOUT).reason)
        // 单向压制不被降级破坏:植物高嘌呤若误加 care avoid → 仍升慎选红(只升不降)
        val avoid = listOf(IngredientCareRule(ingredientId = 1L, categoryId = 9L, categoryName = "痛风", adviceLevel = AdviceLevel.AVOID))
        assertEquals(CrowdFit.CAUTION, fitOf(IngredientCrowdCare.evaluate("黄豆", nut(purine = 190.0), avoid), HealthCondition.GOUT).fit)
        // 藻类(紫菜/海带)存疑从严:虽归 FUNGI 但不做植物豁免,实测高嘌呤仍判慎选红(指南植物豁免主要针对豆/菜/菌菇)
        assertEquals(CrowdFit.CAUTION, fitOf(IngredientCrowdCare.evaluate("紫菜", nut(purine = 200.0)), HealthCondition.GOUT).fit)
    }
}
