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
 **/
class NutritionLineTest {

    private fun agg(days: List<List<String>>) = WeeklyNutritionLineAggregator.aggregate(days)

    @Test
    fun `空计划_零值不崩`() {
        val line = agg(emptyList())
        assertEquals(0, line.dayCount)
        assertEquals(0, line.balanceScore)
        assertTrue(line.pillarGapDays.isEmpty())
        assertTrue(NutritionLineAdvisor.advise(line).isEmpty())
    }

    @Test
    fun `全主食计划_蛋白源为0且每天缺蛋白缺蔬菜`() {
        val line = agg(listOf(listOf("大米"), listOf("大米"), listOf("大米")))
        assertEquals(3, line.dayCount)
        assertEquals(0, line.proteinSourceKinds)
        assertEquals(3, line.pillarGapDays[NutritionLine.Pillar.PROTEIN])
        assertEquals(3, line.pillarGapDays[NutritionLine.Pillar.VEG])
        assertEquals(null, line.pillarGapDays[NutritionLine.Pillar.STAPLE]) // 主食天天有→无缺失(被 filter 掉)
        assertTrue(line.perDayLevel.all { it == 1 }, "全主食→每天结构级1: ${line.perDayLevel}")
    }

    @Test
    fun `均衡计划_三支柱齐_无缺失_均衡度高`() {
        val line = agg(listOf(listOf("五花肉", "青菜", "大米"), listOf("鱼", "青菜", "大米")))
        assertEquals(2, line.dayCount)
        assertTrue(line.pillarGapDays.isEmpty(), "三支柱天天齐→无缺失: ${line.pillarGapDays}")
        assertEquals(2, line.proteinSourceKinds, "红肉+水产=2种蛋白源")
        assertTrue(line.perDayLevel.all { it == 3 }, "三支柱齐(size<5)→级3: ${line.perDayLevel}")
        // 结构75×.35+广度50×.25+蛋白50×.20+连贯100×.20≈68(2天仅4大类/2蛋白源·结构满但多样度中)→偏高但非满。
        assertTrue(line.balanceScore >= 65, "均衡计划均衡度应偏高: ${line.balanceScore}")
    }

    @Test
    fun `含空天_空天不算支柱缺失且按已排天算均衡度不崩`() {
        // 4 天:2 天已排(三支柱齐)+2 天空(未排菜)。空天不计缺失、均衡度按已排天算(建议1)。
        val line = agg(listOf(listOf("五花肉", "青菜", "大米"), emptyList(), listOf("鱼", "青菜", "大米"), emptyList()))
        assertEquals(4, line.dayCount)
        assertEquals(listOf(3, 0, 3, 0), line.perDayLevel, "空天结构级=0")
        assertTrue(line.pillarGapDays.isEmpty(), "已排2天都三支柱齐·空天不算缺: ${line.pillarGapDays}")
        assertTrue(line.balanceScore >= 60, "含空天不崩·按已排天算均衡度: ${line.balanceScore}")
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
    fun `建议_缺蔬菜时给支柱缺口建议含天数与蔬菜大类`() {
        val line = agg(listOf(listOf("五花肉", "大米"), listOf("鱼", "大米"), listOf("鸡蛋", "大米")))
        val advices = NutritionLineAdvisor.advise(line)
        val gap = advices.firstOrNull { it.kind == LineAdvice.Kind.GAP_PILLAR }
        assertTrue(gap != null, "缺蔬菜应出支柱缺口建议: $advices")
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
    fun `建议_全覆盖多样时不硬凑建议`() {
        // 每天三支柱齐、蛋白源多样(红肉/水产/禽/蛋/豆各现)→无支柱缺口。
        val line = agg(
            listOf(
                listOf("五花肉", "青菜", "大米"),
                listOf("鱼", "青菜", "大米"),
                listOf("鸡蛋", "豆腐", "青菜", "大米"),
            ),
        )
        val advices = NutritionLineAdvisor.advise(line)
        assertTrue(advices.none { it.kind == LineAdvice.Kind.GAP_PILLAR }, "三支柱天天齐→不应有支柱缺口建议: $advices")
    }
}
