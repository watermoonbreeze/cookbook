package com.sxdbsm.cookbook.domain

/**
 * @File : NutritionLine
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 周计划"营养线"（整体均衡）聚合——把 N 天计划看成一条有关联的营养线，算整周覆盖/缺口/均衡度
 * <p>
 * 用户洞察：周计划的价值不是"N 天各自独立的菜单"，而是餐与餐、天与天有营养关联的一条"营养线"——
 * 顺着吃下来整体能补齐哪些营养、达到均衡。本聚合器为**一期结构覆盖层**（数据最全·不依赖营养数值·最安全）：
 * 复用 [FoodGroup] 的九大类 + 三支柱口径，升到"整周"维度看覆盖广度、支柱缺失天数、蛋白源多样性、整体均衡度。
 * 宏量比例/慢病分布层（需 NutritionTotals）作 follow-up。纯函数、UI 中立、可单测。
 * <p>
 * 口径与免责：均为膳食**结构**参考（惯例判断·非国标·仅供了解、非医嘱）；缺数据不臆造。[AI生成] 周计划营养线一期·结构层。
 **/

/** 一周（或 N 天）营养线聚合结果。纯数据、UI 中立。[AI生成] */
data class NutritionLine(
    val dayCount: Int,                                     // 计划天数（1~30）
    val perDayLevel: List<Int>,                           // 每天结构级 0~4（[FoodGroup.nutritionLevel]）
    val weekGroupFrequency: Map<FoodGroup.Group, Int>,    // 每大类整周**出现天数**（天维度去重·非菜次）
    val coveredGroups: Set<FoodGroup.Group>,             // 整周覆盖到的大类
    val pillarGapDays: Map<NutritionLine.Pillar, Int>,   // 三支柱各"缺失天数"（本周几天没蛋白/主食/蔬菜）
    val proteinSourceKinds: Int,                         // 蛋白源大类种数（1=单一，越多越好）
    val balanceScore: Int,                               // 整周均衡度 0~100（仅结构维度·合"色系墙不关联热量/慢病"红线）
) {
    /** 三大结构支柱。[AI生成] */
    enum class Pillar { PROTEIN, STAPLE, VEG }
}

/**
 * 周计划营养线聚合器（结构层）。纯 object、无平台依赖、可单测。[AI生成]
 *
 * 与 [DietReportAggregator]（回顾已吃）平行但面向"前瞻计划 + 连贯性"：输入是计划的每一天主料名。
 */
object WeeklyNutritionLineAggregator {

    /** 整周均衡度各分项权重（和=1.0）。[AI生成] */
    private const val W_STRUCT = 0.35   // 每天结构级均值（天天均衡则满）
    private const val W_BREADTH = 0.25  // 一周触及大类广度
    private const val W_PROTEIN = 0.20  // 蛋白源多样性
    private const val W_COHERENCE = 0.20 // 连贯性（惩罚"某支柱长期缺"）
    private const val BREADTH_TARGET = 8 // 一周触及 8 大类即视为广度满（共 10 类·8 已很全）

    /**
     * 聚合 N 天计划为营养线。[AI生成]
     *
     * @param perDayMainNames 每天所有菜的主料名（内层一个 list = 该天）；空天（没排菜）用空 list 占位。
     * @param explicit 食材名→大类显式覆盖（自定义食材的 food_group·同 [DietReportAggregator] 口径）。
     */
    fun aggregate(
        perDayMainNames: List<List<String>>,
        explicit: Map<String, FoodGroup.Group> = emptyMap(),
    ): NutritionLine {
        val dayCount = perDayMainNames.size
        if (dayCount == 0) {
            return NutritionLine(0, emptyList(), emptyMap(), emptySet(), emptyMap(), 0, 0)
        }
        val perDayGroups = perDayMainNames.map { FoodGroup.groupsOf(it, explicit) }
        val perDayLevel = perDayGroups.map { FoodGroup.nutritionLevel(it) }

        // 每大类出现"天数"（天维度去重：一天三道猪肉红肉只记 1 天，避免虚高）。
        val weekGroupFrequency = FoodGroup.Group.entries.associateWith { g ->
            perDayGroups.count { g in it }
        }.filterValues { it > 0 }

        val coveredGroups = perDayGroups.flatten().toSet()
        val proteinSourceKinds = (coveredGroups intersect FoodGroup.PROTEIN_GROUPS).size

        // 三支柱各缺失天数（单天缺不重要·一周多天缺才是营养线问题）。空天不计入缺失（没排菜≠缺·避免惩罚未排的天）。
        var protGap = 0; var stapleGap = 0; var vegGap = 0
        perDayGroups.forEach { groups ->
            if (groups.isEmpty()) return@forEach // 空天跳过（未排菜不算支柱缺失）
            if (groups.none { it in FoodGroup.PROTEIN_GROUPS }) protGap++
            if (FoodGroup.Group.STAPLE !in groups) stapleGap++
            if (groups.none { it == FoodGroup.Group.VEGETABLE || it == FoodGroup.Group.FUNGI || it == FoodGroup.Group.FRUIT }) vegGap++
        }
        val pillarGapDays = mapOf(
            NutritionLine.Pillar.PROTEIN to protGap,
            NutritionLine.Pillar.STAPLE to stapleGap,
            NutritionLine.Pillar.VEG to vegGap,
        ).filterValues { it > 0 }

        val balanceScore = balanceScore(perDayLevel, coveredGroups.size, proteinSourceKinds, protGap + stapleGap + vegGap, dayCount)

        return NutritionLine(
            dayCount = dayCount,
            perDayLevel = perDayLevel,
            weekGroupFrequency = weekGroupFrequency,
            coveredGroups = coveredGroups,
            pillarGapDays = pillarGapDays,
            proteinSourceKinds = proteinSourceKinds,
            balanceScore = balanceScore,
        )
    }

    /**
     * 整周均衡度 0~100（**仅结构维度**·四分项加权·各项归一到 0~100 防单因子碾压）。[AI生成]
     * 结构分(天天均衡) + 广度分(吃够大类) + 蛋白多样 + 连贯分(惩罚支柱长期缺)。
     */
    private fun balanceScore(
        perDayLevel: List<Int>,
        coveredGroupCount: Int,
        proteinSourceKinds: Int,
        totalPillarGapDays: Int,
        dayCount: Int,
    ): Int {
        if (dayCount == 0) return 0
        // [AI修改] Google审建议1:连贯分/结构分的"每天"分母用**已排天数**(perDayLevel>0),否则空天(未排菜·gap 已跳过)会撑大分母、偏乐观抬高分。
        val recordedDays = perDayLevel.count { it > 0 }
        if (recordedDays == 0) return 0 // 全空天(未排菜)→无营养线可言
        val structure = perDayLevel.sum().toDouble() / (4.0 * recordedDays) * 100.0 // 已排部分的结构级均值
        val breadth = minOf(coveredGroupCount, BREADTH_TARGET).toDouble() / BREADTH_TARGET * 100.0 // 累计广度(整周覆盖)
        val protein = minOf(proteinSourceKinds, 4).toDouble() / 4.0 * 100.0 // 累计蛋白源多样
        val coherence = (100.0 - totalPillarGapDays.toDouble() / (3.0 * recordedDays) * 100.0).coerceAtLeast(0.0)
        val score = W_STRUCT * structure + W_BREADTH * breadth + W_PROTEIN * protein + W_COHERENCE * coherence
        return score.coerceIn(0.0, 100.0).toInt()
    }
}
