package com.sxdbsm.cookbook.domain

/**
 * @File : NutritionLine
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 周计划"营养线"（整体均衡）聚合——把 N 天计划看成一条有关联的营养线，算整周覆盖/缺口/均衡度
 * <p>
 * 用户洞察：周计划的价值不是"N 天各自独立的菜单"，而是餐与餐、天与天有营养关联的一条"营养线"——
 * 顺着吃下来整体能补齐哪些营养、达到均衡。本聚合器为**一期结构覆盖层**（数据最全·不依赖营养数值·最安全）：
 * 复用 [FoodGroup] 的九大类，升到"整周"维度看覆盖广度、**宝塔正向层缺失天数**、蛋白源多样性、整体均衡度。
 * 宏量比例/慢病分布层（需 NutritionTotals）作 follow-up。纯函数、UI 中立、可单测。
 * <p>
 * [AI修改] 权威化重审 P3(2026/07/25·口径收敛)：把此前"自创三支柱(蛋白/主食/蔬菜)"缺口口径收敛到
 *   **膳食宝塔四正向层**([DietaryGuideline.POSITIVE_LAYERS]：谷薯/蔬果/鱼禽肉蛋/奶豆坚果)，
 *   与色系墙(P1)、餐次差异化(P2)统一到同一套宝塔结构口径（全 App 单一真相源·消灭"三支柱 vs 宝塔"并存漂移）。
 *   关键差异：原三支柱把"鱼禽肉蛋 + 奶 + 豆坚果"混为一个 PROTEIN 支柱；宝塔把它拆成 ANIMAL_FOODS 与
 *   DAIRY_BEANS_NUTS 两层——能识别"顿顿有肉却常年不喝奶/不吃豆"这类中国膳食常见结构缺口。
 * <p>
 * 口径与免责：均为膳食**结构**参考（惯例判断·非国标·仅供了解、非医嘱）；缺数据不臆造。[AI生成] 周计划营养线一期·结构层。
 **/

/** 一周（或 N 天）营养线聚合结果。纯数据、UI 中立。[AI生成] */
data class NutritionLine(
    val dayCount: Int,                                     // 计划天数（1~30）
    val perDayLevel: List<Int>,                           // 每天结构级 0~4（[FoodGroup.nutritionLevel]）
    val weekGroupFrequency: Map<FoodGroup.Group, Int>,    // 每大类整周**出现天数**（天维度去重·非菜次）
    val coveredGroups: Set<FoodGroup.Group>,             // 整周覆盖到的大类
    // [AI修改] P3 口径收敛：三支柱缺失 → 宝塔正向层缺失（几天没覆盖谷薯/蔬果/鱼禽肉蛋/奶豆坚果各层）。
    val layerGapDays: Map<DietaryGuideline.PagodaLayer, Int>,
    val proteinSourceKinds: Int,                         // 蛋白源大类种数（1=单一，越多越好）
    val balanceScore: Int,                               // 整周均衡度 0~100（仅结构维度·合"色系墙不关联热量/慢病"红线）
)

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
    private const val W_COHERENCE = 0.20 // 连贯性（惩罚"某宝塔正向层长期缺"）
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
        // 说明：第 5 个 emptyMap() 即 layerGapDays 的空值（字段顺序未变，仅语义由三支柱→宝塔层）。
        val perDayGroups = perDayMainNames.map { FoodGroup.groupsOf(it, explicit) }
        val perDayLevel = perDayGroups.map { FoodGroup.nutritionLevel(it) }

        // 每大类出现"天数"（天维度去重：一天三道猪肉红肉只记 1 天，避免虚高）。
        val weekGroupFrequency = FoodGroup.Group.entries.associateWith { g ->
            perDayGroups.count { g in it }
        }.filterValues { it > 0 }

        val coveredGroups = perDayGroups.flatten().toSet()
        val proteinSourceKinds = (coveredGroups intersect FoodGroup.PROTEIN_GROUPS).size

        // [AI修改] P3 口径收敛：宝塔各正向层缺失天数（单天缺不重要·一周多天缺才是营养线问题）。
        // 引用 [DietaryGuideline]：一天的大类经 coveredLayers 归到宝塔层，未覆盖的正向层记一次缺。
        // 空天不计入缺失（没排菜≠缺·避免惩罚未排的天）。禁在此复制宝塔层份量/映射常量（防漂移·全走 DietaryGuideline）。
        val layerGap = DietaryGuideline.POSITIVE_LAYERS.associateWith { 0 }.toMutableMap()
        perDayGroups.forEach { groups ->
            if (groups.isEmpty()) return@forEach // 空天跳过（未排菜不算层缺失）
            val covered = DietaryGuideline.coveredLayers(groups).toSet()
            DietaryGuideline.POSITIVE_LAYERS.forEach { layer ->
                if (layer !in covered) layerGap[layer] = layerGap.getValue(layer) + 1
            }
        }
        val layerGapDays = layerGap.filterValues { it > 0 }

        val balanceScore = balanceScore(perDayLevel, coveredGroups.size, proteinSourceKinds, layerGapDays.values.sum(), dayCount)

        return NutritionLine(
            dayCount = dayCount,
            perDayLevel = perDayLevel,
            weekGroupFrequency = weekGroupFrequency,
            coveredGroups = coveredGroups,
            layerGapDays = layerGapDays,
            proteinSourceKinds = proteinSourceKinds,
            balanceScore = balanceScore,
        )
    }

    /**
     * 整周均衡度 0~100（**仅结构维度**·四分项加权·各项归一到 0~100 防单因子碾压）。[AI生成]
     * 结构分(天天均衡) + 广度分(吃够大类) + 蛋白多样 + 连贯分(惩罚宝塔正向层长期缺)。
     */
    private fun balanceScore(
        perDayLevel: List<Int>,
        coveredGroupCount: Int,
        proteinSourceKinds: Int,
        totalLayerGapDays: Int,
        dayCount: Int,
    ): Int {
        if (dayCount == 0) return 0
        // [AI修改] Google审建议1:连贯分/结构分的"每天"分母用**已排天数**(perDayLevel>0),否则空天(未排菜·gap 已跳过)会撑大分母、偏乐观抬高分。
        val recordedDays = perDayLevel.count { it > 0 }
        if (recordedDays == 0) return 0 // 全空天(未排菜)→无营养线可言
        val structure = perDayLevel.sum().toDouble() / (4.0 * recordedDays) * 100.0 // 已排部分的结构级均值
        val breadth = minOf(coveredGroupCount, BREADTH_TARGET).toDouble() / BREADTH_TARGET * 100.0 // 累计广度(整周覆盖)
        val protein = minOf(proteinSourceKinds, 4).toDouble() / 4.0 * 100.0 // 累计蛋白源多样
        // [AI修改] P3:连贯分分母由 3 支柱→宝塔正向层数(4)，每天最多缺 4 层；引用 POSITIVE_LAYERS.size 防硬编码漂移。
        val layerCount = DietaryGuideline.POSITIVE_LAYERS.size.toDouble()
        val coherence = (100.0 - totalLayerGapDays.toDouble() / (layerCount * recordedDays) * 100.0).coerceAtLeast(0.0)
        val score = W_STRUCT * structure + W_BREADTH * breadth + W_PROTEIN * protein + W_COHERENCE * coherence
        return score.coerceIn(0.0, 100.0).toInt()
    }
}
