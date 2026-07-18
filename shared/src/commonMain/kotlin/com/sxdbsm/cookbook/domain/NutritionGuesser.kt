package com.sxdbsm.cookbook.domain

/**
 * @File : NutritionGuesser
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 自建食材"按名智能预填"营养推演（三级兜底：近似同名命中→大类均值兜底→不填）
 * <p>
 * 用户自建食材输入名称后，尽量替其预填每 100g 营养，减少从零填。**纯本地推断、纯函数、可单测**。
 * 三级：①近似同名命中已有营养食材(如"黑毛猪五花"→"五花肉")→预填其值(source=Match，可信度较高)；
 * ②对不上→按 classify 出的营养大类给**保守均值**兜底(source=Group，措辞更保守"粗略估算")；③都不确定→NONE 不填。
 * 免责红线：预填值一律"估算·请核对"，缺字段留空不填 0(沿用"0 千卡"红线精神)，禁当权威。
 * <p>
 * [AI生成] 食材输入智能推演（用户 2026-07-18 要求整体做完）。UI 侧据 source 分级措辞、标来源、可撤销。
 **/

/** 一组可预填的每 100g 营养值（全部可空=未知不填）。[AI生成] */
data class NutritionGuessValues(
    val energyKcal: Double? = null,
    val proteinG: Double? = null,
    val fatG: Double? = null,
    val carbG: Double? = null,
    val fiberG: Double? = null,
    val sodiumMg: Double? = null,
    val potassiumMg: Double? = null,
    val calciumMg: Double? = null,
    val gi: Double? = null,
    val purineMg: Double? = null,
)

/** 推演来源（决定 UI 措辞与可信度暗示）。[AI生成] */
sealed interface NutritionGuessSource {
    /** 近似同名命中某已有营养食材（点名参照物，可信度较高）。 */
    data class Match(val refName: String) : NutritionGuessSource
    /** 无同名→按营养大类粗略估算（措辞更保守）。 */
    data class Group(val groupLabel: String) : NutritionGuessSource
    /** 都不确定，不预填。 */
    data object None : NutritionGuessSource
}

/** 推演结果：values=null 表示不预填（source=None）。[AI生成] */
data class NutritionGuess(val values: NutritionGuessValues?, val source: NutritionGuessSource)

object NutritionGuesser {

    /** 名称归一时剥离的常见修饰词（品质/产地/状态词，不影响营养本体）。[AI生成] */
    private val MODIFIERS = listOf(
        "黑毛", "土", "散养", "农家", "新鲜", "有机", "进口", "国产", "冷冻", "速冻", "急冻",
        "精", "特级", "一级", "优质", "精品", "老", "嫩", "牌",
    )

    /** 大类保守均值兜底（每 100g·惯例估算·非权威，用则标"粗略估算·务必核对"）。[AI生成] */
    private val GROUP_AVG: Map<FoodGroup.Group, NutritionGuessValues> = mapOf(
        FoodGroup.Group.STAPLE to NutritionGuessValues(energyKcal = 340.0, proteinG = 8.0, fatG = 1.5, carbG = 75.0, fiberG = 2.0),
        FoodGroup.Group.VEGETABLE to NutritionGuessValues(energyKcal = 25.0, proteinG = 2.0, fatG = 0.3, carbG = 5.0, fiberG = 2.0, potassiumMg = 200.0),
        FoodGroup.Group.FUNGI to NutritionGuessValues(energyKcal = 30.0, proteinG = 3.0, fatG = 0.4, carbG = 5.0, fiberG = 3.0),
        FoodGroup.Group.FRUIT to NutritionGuessValues(energyKcal = 50.0, proteinG = 0.7, fatG = 0.3, carbG = 13.0, fiberG = 2.0, potassiumMg = 150.0),
        FoodGroup.Group.FISH to NutritionGuessValues(energyKcal = 100.0, proteinG = 18.0, fatG = 3.0),
        FoodGroup.Group.RED_MEAT to NutritionGuessValues(energyKcal = 250.0, proteinG = 17.0, fatG = 20.0),
        FoodGroup.Group.WHITE_MEAT to NutritionGuessValues(energyKcal = 150.0, proteinG = 20.0, fatG = 7.0),
        FoodGroup.Group.EGG to NutritionGuessValues(energyKcal = 145.0, proteinG = 13.0, fatG = 9.0),
        FoodGroup.Group.DAIRY to NutritionGuessValues(energyKcal = 60.0, proteinG = 3.0, fatG = 3.5, calciumMg = 100.0),
        FoodGroup.Group.BEAN to NutritionGuessValues(energyKcal = 350.0, proteinG = 20.0, fatG = 15.0, fiberG = 6.0),
    )

    /** 归一核心名：去空格 + 剥离前导修饰词。[AI生成] */
    fun coreName(name: String): String {
        var s = name.trim().replace(" ", "")
        var changed = true
        while (changed) {
            changed = false
            for (m in MODIFIERS) {
                if (s.length > m.length && s.startsWith(m)) { s = s.removePrefix(m); changed = true }
            }
        }
        return s
    }

    /**
     * 按名推演营养。[AI生成]
     *
     * @param name 用户输入的食材名
     * @param candidates 已有"有营养数据"的食材 (名 -> 营养值)，供近似命中匹配
     * @param group classify(name) 得到的营养大类（可空），供大类均值兜底
     * @return 三级结果；命中不了具体食材也无大类→None（不预填、不编造）
     */
    fun guess(
        name: String,
        candidates: List<Pair<String, NutritionGuessValues>>,
        group: FoodGroup.Group?,
    ): NutritionGuess {
        val core = coreName(name)
        if (core.length >= 2) {
            // 1) 近似同名命中：精确 > 一方为另一方后缀（取匹配名最长者，最贴近）。
            var best: Pair<String, NutritionGuessValues>? = null
            var bestScore = 0
            for (c in candidates) {
                val cn = c.first.trim().replace(" ", "")
                if (cn.isEmpty()) continue
                // 中文食材中心词在末尾，用双向 contains 兼顾"输入含候选"(冷冻五花肉→五花肉)与"候选含输入"(五花→五花肉)；
                // 均要求较短一方≥2 字防泛匹配，按匹配名长度加权取最贴近者。
                val score = when {
                    cn == core -> 100 + cn.length
                    core.contains(cn) && cn.length >= 2 -> 50 + cn.length
                    cn.contains(core) && core.length >= 2 -> 40 + core.length
                    else -> 0
                }
                if (score > bestScore) { bestScore = score; best = c }
            }
            if (best != null) return NutritionGuess(best.second, NutritionGuessSource.Match(best.first))
        }
        // 2) 大类均值兜底。
        if (group != null) {
            GROUP_AVG[group]?.let { return NutritionGuess(it, NutritionGuessSource.Group(FoodGroup.CATEGORY_NAME[group] ?: group.name)) }
        }
        // 3) 不确定，不预填。
        return NutritionGuess(null, NutritionGuessSource.None)
    }
}
