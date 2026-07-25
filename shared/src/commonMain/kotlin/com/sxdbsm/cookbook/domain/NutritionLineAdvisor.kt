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
    /** [AI修改] P3:GAP_PILLAR(三支柱缺口)→GAP_LAYER(宝塔正向层缺口)，口径统一到膳食宝塔。 */
    enum class Kind { GAP_LAYER, MONOTONE }
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

        // 1) 层缺口（GAP_LAYER）：缺失天数最多的宝塔正向层→用天数说话、给可补的大类。[AI修改] P3 口径收敛到宝塔层。
        // [AI修改] Google审🟡1:并列最大时**显式按 POSITIVE_LAYERS 顺序 tie-break**(谷薯→蔬果→鱼禽肉蛋→奶豆坚果)，
        //   不依赖 layerGapDays 的 Map 迭代顺序(隐式契约脆弱·换构建方式会静默改选层)。
        val maxDays = line.layerGapDays.values.maxOrNull() ?: 0
        val layer = if (maxDays > 0) DietaryGuideline.POSITIVE_LAYERS.firstOrNull { (line.layerGapDays[it] ?: 0) == maxDays } else null
        if (layer != null) {
            val days = maxDays
            // 建议的可补大类按膳食宝塔"优先鱼禽蛋、奶豆坚果"取更优选项；油盐层不属正向层、不会命中。
            val (word, groups) = when (layer) {
                DietaryGuideline.PagodaLayer.GRAINS -> "主食" to listOf(FoodGroup.Group.STAPLE)
                DietaryGuideline.PagodaLayer.VEGETABLES_FRUITS -> "蔬菜水果" to listOf(FoodGroup.Group.VEGETABLE, FoodGroup.Group.FUNGI, FoodGroup.Group.FRUIT)
                DietaryGuideline.PagodaLayer.ANIMAL_FOODS -> "鱼禽肉蛋" to listOf(FoodGroup.Group.FISH, FoodGroup.Group.EGG, FoodGroup.Group.WHITE_MEAT)
                DietaryGuideline.PagodaLayer.DAIRY_BEANS_NUTS -> "奶豆坚果" to listOf(FoodGroup.Group.DAIRY, FoodGroup.Group.BEAN)
                DietaryGuideline.PagodaLayer.OILS_SALT -> "" to emptyList() // 不会出现(layerGapDays 仅正向层)
            }
            if (word.isNotEmpty()) {
                out += LineAdvice(
                    LineAdvice.Kind.GAP_LAYER,
                    "这周有 $days 天还没吃到$word，挑一两天各加一道，整周会更均衡",
                    groups,
                )
            }
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
