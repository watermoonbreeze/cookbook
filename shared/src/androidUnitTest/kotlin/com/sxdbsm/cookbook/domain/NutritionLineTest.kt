package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : NutritionLineTest
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 周计划营养线聚合 + 补充建议单测（结构层）
 * <p>
 * [AI生成] 用合成计划验证整周覆盖/缺口/均衡度与建议，不依赖数据源。
 * [AI修改] 权威化重审 P3(2026/07/25)：缺口口径由"自创三支柱"收敛到"膳食宝塔四正向层"
 *   (谷薯 GRAINS / 蔬果 VEGETABLES_FRUITS / 鱼禽肉蛋 ANIMAL_FOODS / 奶豆坚果 DAIRY_BEANS_NUTS)。
 *   断言随之改为按 [DietaryGuideline.PagodaLayer] 判缺；新增"肉菜饭齐但缺奶豆坚果层"的核心口径测试。
 **/
class NutritionLineTest {

    private fun agg(days: List<List<String>>) = WeeklyNutritionLineAggregator.aggregate(days)

    @Test
    fun `空计划_零值不崩`() {
        val line = agg(emptyList())
        assertEquals(0, line.dayCount)
        assertEquals(0, line.balanceScore)
        assertTrue(line.layerGapDays.isEmpty())
        assertTrue(NutritionLineAdvisor.advise(line).isEmpty())
    }

    @Test
    fun `全主食计划_蛋白源为0且每天缺蔬果_鱼禽肉蛋_奶豆坚果三层`() {
        val line = agg(listOf(listOf("大米"), listOf("大米"), listOf("大米")))
        assertEquals(3, line.dayCount)
        assertEquals(0, line.proteinSourceKinds)
        // 只覆盖谷薯层，其余三个正向层每天都缺。
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.GRAINS], "主食天天有→谷薯层无缺失(被 filter 掉)")
        assertEquals(3, line.layerGapDays[DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS])
        assertEquals(3, line.layerGapDays[DietaryGuideline.PagodaLayer.ANIMAL_FOODS])
        assertEquals(3, line.layerGapDays[DietaryGuideline.PagodaLayer.DAIRY_BEANS_NUTS])
        assertTrue(line.perDayLevel.all { it == 1 }, "全主食→每天结构级1: ${line.perDayLevel}")
    }

    @Test
    fun `肉菜饭齐但无奶豆_识别奶豆坚果缺层_这是P3口径新增能力`() {
        // 顿顿有肉有菜有饭、却常年不喝奶不吃豆——旧三支柱口径会判"蛋白齐、无缺口"，
        // 宝塔四层口径应识别出"奶豆坚果层"缺失（中国膳食常见结构缺口）。
        val line = agg(listOf(listOf("五花肉", "青菜", "大米"), listOf("鱼", "青菜", "大米")))
        assertEquals(2, line.dayCount)
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.GRAINS], "主食天天有")
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS], "蔬菜天天有")
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.ANIMAL_FOODS], "鱼禽肉蛋天天有")
        assertEquals(2, line.layerGapDays[DietaryGuideline.PagodaLayer.DAIRY_BEANS_NUTS], "两天都没奶豆坚果→缺2天")
        // 建议应指向奶豆坚果层。
        val advices = NutritionLineAdvisor.advise(line)
        val gap = advices.firstOrNull { it.kind == LineAdvice.Kind.GAP_LAYER }
        assertTrue(gap != null, "缺奶豆坚果应出层缺口建议: $advices")
        assertTrue(gap!!.text.contains("奶豆坚果"), "建议应指向奶豆坚果: ${gap.text}")
        assertTrue(FoodGroup.Group.DAIRY in gap.suggestGroups || FoodGroup.Group.BEAN in gap.suggestGroups, "可补大类含奶/豆")
    }

    @Test
    fun `四正向层天天齐_无缺失_均衡度高`() {
        // 谷薯+蔬果+鱼禽肉蛋+奶豆坚果四层天天齐（加牛奶/豆腐补齐奶豆坚果层）。
        val line = agg(listOf(listOf("五花肉", "青菜", "大米", "牛奶"), listOf("鱼", "青菜", "大米", "豆腐")))
        assertEquals(2, line.dayCount)
        assertTrue(line.layerGapDays.isEmpty(), "四层天天齐→无缺失: ${line.layerGapDays}")
        assertTrue(line.balanceScore >= 65, "四层齐的均衡计划均衡度应偏高: ${line.balanceScore}")
    }

    @Test
    fun `含空天_空天不算层缺失且按已排天算均衡度不崩`() {
        // 4 天:2 天已排(肉菜饭·缺奶豆)+2 天空(未排菜)。空天不计入缺失、均衡度按已排天算(建议1)。
        val line = agg(listOf(listOf("五花肉", "青菜", "大米"), emptyList(), listOf("鱼", "青菜", "大米"), emptyList()))
        assertEquals(4, line.dayCount)
        assertEquals(listOf(3, 0, 3, 0), line.perDayLevel, "空天结构级=0")
        // 奶豆坚果仅在已排的 2 天缺（空天不算），验证空天不撑大缺失天数。
        assertEquals(2, line.layerGapDays[DietaryGuideline.PagodaLayer.DAIRY_BEANS_NUTS], "空天不算缺·只已排2天缺奶豆: ${line.layerGapDays}")
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.GRAINS], "主食天天有")
        assertTrue(line.balanceScore >= 55, "含空天不崩·按已排天算均衡度: ${line.balanceScore}")
    }

    @Test
    fun `全空天_均衡度为0不崩`() {
        val line = agg(listOf(emptyList(), emptyList()))
        assertEquals(2, line.dayCount)
        assertEquals(0, line.balanceScore, "全空天(未排菜)→均衡度0")
        assertTrue(NutritionLineAdvisor.advise(line).isEmpty(), "全空天无建议")
    }

    @Test
    fun `天维度去重_一天三道红肉只记一天`() {
        val line = agg(listOf(listOf("五花肉", "五花肉", "五花肉", "青菜", "大米")))
        assertEquals(1, line.weekGroupFrequency[FoodGroup.Group.RED_MEAT], "一天3道红肉→红肉出现天数=1(非3)")
    }

    @Test
    fun `建议_缺蔬菜时给层缺口建议含天数与蔬菜大类`() {
        // 三天都无蔬果：蔬果层与奶豆坚果层都缺3天，蔬果层排序在前→topGap取蔬果。
        val line = agg(listOf(listOf("五花肉", "大米"), listOf("鱼", "大米"), listOf("鸡蛋", "大米")))
        val advices = NutritionLineAdvisor.advise(line)
        val gap = advices.firstOrNull { it.kind == LineAdvice.Kind.GAP_LAYER }
        assertTrue(gap != null, "缺蔬菜应出层缺口建议: $advices")
        assertTrue(gap!!.text.contains("蔬菜") && gap.text.contains("3"), "建议含'蔬菜'与天数: ${gap.text}")
        assertTrue(FoodGroup.Group.VEGETABLE in gap.suggestGroups, "可补大类含蔬菜")
    }

    @Test
    fun `建议_蛋白单一时给单调建议且鼓励口吻非责备`() {
        val line = agg(listOf(listOf("五花肉", "青菜"), listOf("五花肉", "青菜"), listOf("五花肉", "青菜")))
        val advices = NutritionLineAdvisor.advise(line)
        val mono = advices.firstOrNull { it.kind == LineAdvice.Kind.MONOTONE }
        assertTrue(mono != null, "蛋白单一(全红肉)应出单调建议: $advices")
        // 鼓励非责备：不含"缺乏/不足/不够"等责备词。
        assertFalse(mono!!.text.contains("缺乏") || mono.text.contains("不足") || mono.text.contains("不够"), "单调建议应鼓励非责备: ${mono.text}")
    }

    @Test
    fun `已排周7天窗口_肉菜饭常年无奶豆_概览识别奶豆坚果缺层_锁WeekPlan与AiPlan共享消费口径`() {
        // [AI生成] Google审🟡2:显式建模 WeekPlan"已排周"/AiPlan 透传给聚合器的输入形状——
        //   7 天窗口(observeTimelineWindow 恒返每天含空天)、已排的都是"肉+青菜+米饭"、常年无奶豆。
        //   两屏共用同一 aggregate/advise(单一真相源),本测锁住共享消费路径的宝塔四层口径回归,
        //   不为一条纯透传断言从零搭 androidApp VM 测试架(反过度设计)。
        val week = listOf(
            listOf("五花肉", "青菜", "大米"),
            listOf("牛肉", "青菜", "大米"),
            emptyList(),                        // 空天(未排)
            listOf("鱼", "青菜", "大米"),
            emptyList(),
            listOf("鸡蛋", "青菜", "大米"),      // 有蛋(ANIMAL)但仍无奶豆
            listOf("五花肉", "青菜", "大米"),
        )
        val line = agg(week)
        assertEquals(7, line.dayCount)
        assertEquals(5, line.layerGapDays[DietaryGuideline.PagodaLayer.DAIRY_BEANS_NUTS], "5个已排天都缺奶豆坚果·空天不计入: ${line.layerGapDays}")
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.GRAINS], "主食天天有")
        assertEquals(null, line.layerGapDays[DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS], "蔬菜天天有")
        val advices = NutritionLineAdvisor.advise(line)
        val gap = advices.firstOrNull { it.kind == LineAdvice.Kind.GAP_LAYER }
        assertTrue(gap != null && gap.text.contains("奶豆坚果"), "已排周概览应提示奶豆坚果缺口(WeekPlan/AiPlan 同款): $advices")
    }

    @Test
    fun `建议_四层全覆盖多样时不硬凑层缺口建议`() {
        // 每天四正向层齐、蛋白源多样(红肉/水产/禽/蛋/豆各现)→无层缺口。
        val line = agg(
            listOf(
                listOf("五花肉", "青菜", "大米", "牛奶"),
                listOf("鱼", "青菜", "大米", "豆腐"),
                listOf("鸡蛋", "豆腐", "青菜", "大米"),
            ),
        )
        val advices = NutritionLineAdvisor.advise(line)
        assertTrue(advices.none { it.kind == LineAdvice.Kind.GAP_LAYER }, "四层天天齐→不应有层缺口建议: $advices")
    }
}
