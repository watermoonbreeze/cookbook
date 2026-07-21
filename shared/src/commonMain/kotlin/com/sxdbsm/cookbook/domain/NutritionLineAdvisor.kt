package com.sxdbsm.cookbook.domain

/**
 * @File : NutritionLineAdvisor
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 周计划"营养线"跨餐跨天补充建议（连贯性叙事·鼓励非责备·守免责）
 * <p>
 * 基于 [NutritionLine] 生成"顺着这周吃下来，还差 X、建议再加 Y"的连贯建议——**用天数说话**（跨天关联），
 * 而非逐餐独立点评。这是营养线比静态报告更有价值处。措辞守健康免责红线（仅供参考·非医嘱·不制造焦虑）。
 * 纯函数、UI 中立、可单测。文案为**默认稿**，落地 UI 时由 copywriter 精修。
 * <p>
 * [AI生成] 周计划营养线一期·补充建议（结构层）。
 **/

/** 一条营养线建议。kind 供 UI 分类/图标；text 为可直接展示的默认文案；suggestGroups 可驱动"一键补一道X类菜"。[AI生成] */
data class LineAdvice(
    val kind: LineAdvice.Kind,
    val text: String,
    val suggestGroups: List<FoodGroup.Group> = emptyList(),
) {
    enum class Kind { GAP_PILLAR, MONOTONE }
}

object NutritionLineAdvisor {
    /** 某大类出现天数达"该周天数×此比例"即视为"堆积/偏多"。[AI生成] */
    private const val MONOTONE_RATIO = 0.7

    /**
     * 基于营养线产出跨天连贯建议（鼓励口吻·非医嘱·非焦虑）。[AI生成]
     *
     * 只在**确有缺口/堆积**时给建议（不硬凑）；全均衡→空列表。至多 3 条，避免啰嗦。
     */
    fun advise(line: NutritionLine): List<LineAdvice> {
        if (line.dayCount == 0) return emptyList()
        val out = mutableListOf<LineAdvice>()

        // 1) 支柱缺口（GAP_PILLAR）：缺失天数最多的支柱→用天数说话、给可补的大类。
        val topGap = line.pillarGapDays.maxByOrNull { it.value }
        if (topGap != null && topGap.value > 0) {
            val (pillar, days) = topGap
            val (word, groups) = when (pillar) {
                NutritionLine.Pillar.PROTEIN -> "优质蛋白" to listOf(FoodGroup.Group.FISH, FoodGroup.Group.EGG, FoodGroup.Group.BEAN)
                NutritionLine.Pillar.STAPLE -> "主食" to listOf(FoodGroup.Group.STAPLE)
                NutritionLine.Pillar.VEG -> "蔬菜" to listOf(FoodGroup.Group.VEGETABLE, FoodGroup.Group.FUNGI)
            }
            out += LineAdvice(
                LineAdvice.Kind.GAP_PILLAR,
                "这周有 $days 天还没吃到$word，挑一两天各加一道，整周会更均衡",
                groups,
            )
        }

        // 2) 单调堆积（MONOTONE）：某大类出现天数≥dayCount×0.7 且蛋白源单一→正向提示换花样。
        // [AI修改] Google审建议2:阈值向上取整(整数天·"≥70%天"边界确定·避免浮点边界歧义)。
        val threshold = kotlin.math.ceil(line.dayCount * MONOTONE_RATIO).toInt()
        val heavy = line.weekGroupFrequency.filter { it.value >= threshold && it.key in FoodGroup.PROTEIN_GROUPS }
            .maxByOrNull { it.value }
        if (heavy != null && line.proteinSourceKinds <= 2) {
            out += LineAdvice(
                LineAdvice.Kind.MONOTONE,
                "这周蛋白主要来自${heavy.key.label}，换一两天鱼、蛋或豆腐，口感和营养都更丰富",
                listOf(FoodGroup.Group.FISH, FoodGroup.Group.EGG, FoodGroup.Group.BEAN),
            )
        }

        return out.take(3)
    }
}
