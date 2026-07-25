package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.domain.DietaryGuideline
import com.sxdbsm.cookbook.domain.DietaryGuideline.PagodaLayer
import com.sxdbsm.cookbook.domain.FoodGroup

/**
 * @File : MealCompositionScorer
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 单餐"荤素搭配 + 主食覆盖 + 餐次差异化"组合补分（纯函数·单一真相源）
 * <p>
 * 把散落在 [RecommendationOrchestrator.combineScore] 与 [PeriodPlanner.score] 两处、
 * 语义与常量完全相同的"补荤素缺口 + 补主食缺口"评分抽成唯一真相源，杜绝两份常量各改各的调参漂移。
 * 纯函数、无状态、可测；两个调用方接入后行为零变化（见下方 hasChosen 说明）。
 * <p>
 * [AI生成] 算法打磨·技术债收敛：MealCompositionScorer 统一荤素/主食组合补分。
 * [AI修改] P2 餐次差异化(2026/07/25)：叠加"按餐次期待宝塔层补缺口 + 非期待层轻降"分量，
 *   引用权威真相源 [DietaryGuideline]（早/午/晚各餐 expectedLayers）。默认参不传 → 旧行为一字不差(兼容开关)。
 **/
object MealCompositionScorer {
    /** 同餐荤素平衡补分：本餐荤/素偏少的一方补上时给候选加分。[AI生成] */
    const val BALANCE_BONUS = 0.7

    /** 本餐还没有主食时，给主食候选的补分（略高于应季，让每餐尽量含一道主食）。[AI生成] */
    const val STAPLE_BONUS = 0.9

    /**
     * 候选能补上"本餐次期待但本餐还没覆盖"的宝塔层时的补分。[AI生成] P2
     * < [BALANCE_BONUS]：补对期待层是"锦上添花"，不压过荤素/主食基本盘（主食始终最优先）。
     * 补一层即给固定分（不按层数线性累加）——防一道多主料杂烩菜靠"补 3 层"碾压所有因子(封顶思想)。
     */
    const val LAYER_FILL_BONUS = 0.5

    /**
     * "晚餐宜清淡"：**纯鱼禽肉蛋(整块硬荤)候选**在不期待荤的餐次(仅晚餐 expectedLayers 不含 ANIMAL_FOODS)时的轻降。[AI生成] P2
     * 量级同西式降权(0.3)——软信号轻推、不删菜、不彻底沉底。
     * **只针对纯 ANIMAL_FOODS**：不误伤①荤素合炒菜(碰了蔬果层)②豆/奶/坚果(豆腐/牛奶是清淡好选择，不该降)③蔬果。
     * 权威原意=晚餐"肉类适量、别整桌硬荤"，而非禁一切非主粮/非蔬。
     */
    const val OFF_LAYER_PENALTY = 0.3

    /**
     * 候选一道菜覆盖的宝塔正向层。[AI生成] P2
     *
     * 优先用 [FoodGroup.classify] 对每个主料逐个归类 → [DietaryGuideline.LAYER_OF_GROUP] 得层（一道菜可覆盖多层，
     * 如番茄炒蛋=蔬果+鱼禽肉蛋，这正是宝塔覆盖度的正确语义）。classify 全部失败(主料录得不规范/料理包)时
     * 落到 isStaple/isMeat 粗兜底，**保证永不返回空集**（空集会让"缺层判断"退化）。
     *
     * 纯函数、零 DB、零字段扩展——不改 DishCandidate/PlanDish，避免"插字段要改全仓构造点"的踩坑。
     */
    fun candidateLayers(mainNames: List<String>, isMeat: Boolean, isStaple: Boolean): Set<PagodaLayer> {
        val byClassify = mainNames
            .mapNotNull { FoodGroup.classify(it) }
            .mapNotNull { DietaryGuideline.LAYER_OF_GROUP[it] }
            .filter { it in DietaryGuideline.POSITIVE_LAYERS }
            .toSet()
        if (byClassify.isNotEmpty()) return byClassify
        // 兜底：classify 全空时用已有布尔粗判，至少不空。
        return when {
            isStaple -> setOf(PagodaLayer.GRAINS)
            isMeat -> setOf(PagodaLayer.ANIMAL_FOODS)
            else -> setOf(PagodaLayer.VEGETABLES_FRUITS)
        }
    }

    /**
     * 候选加入本餐已选后的"组合完整性"补分（荤素平衡 + 主食覆盖 + 餐次差异化）。[AI生成]
     *
     * @param candMeat 候选是否荤菜
     * @param candStaple 候选是否主食菜
     * @param chosenMeat 本餐已选荤菜数
     * @param chosenVeg 本餐已选素菜数
     * @param chosenHasStaple 本餐已选是否已含主食
     * @param balanceFactor 荤素补分的风格系数（周计划按 f.balance 调；单餐固定 1.0）。**只作用荤素、不作用主食**，与两处原实现一致。
     * @param candLayers 候选覆盖的宝塔正向层（[candidateLayers] 求得）。为 null → 不启用 P2 餐次差异。[AI生成] P2
     * @param expectedLayers 本餐次"应有"的宝塔层（[DietaryGuideline] 各餐 expectedLayers）。null/空 → 不启用 P2(加餐等)。[AI生成] P2
     * @param coveredLayers 本餐已选已覆盖的宝塔层（用于"还缺哪层"判断）。[AI生成] P2
     *
     * 关键：本餐**为空**（chosenMeat+chosenVeg==0）时不给荤素补分——空餐无"偏少的一方"，
     *   给了会让首道菜凭荤素身份白拿分。此守卫内建于此，使 PeriodPlanner 可无条件调用而保持原
     *   `if(mealChosen.isNotEmpty())` 守卫的行为；而主食补分在空餐时**照常**生效（首道主食该加分），
     *   与 PeriodPlanner 原实现（主食分不受 isNotEmpty 守卫）一致。combineScore 调用时本餐恒非空，两分支行为不变。
     *
     * P2 分量为**独立叠加**（不重写荤素/主食旧公式、不动量级基线）：
     *   ① 补缺口——候选能补"期待但还缺"的层 → +[LAYER_FILL_BONUS]；
     *   ② 晚餐清淡——**纯鱼禽肉蛋**候选在不期待荤的餐次(晚餐) → −[OFF_LAYER_PENALTY]（只降整块硬荤，不误伤合炒/豆/奶/蔬）。
     *   `expectedLayers` 为 null/空(如加餐 SNACK)时整体跳过 P2，等价旧行为。
     */
    fun compositionBonus(
        candMeat: Boolean,
        candStaple: Boolean,
        chosenMeat: Int,
        chosenVeg: Int,
        chosenHasStaple: Boolean,
        balanceFactor: Double = 1.0,
        candLayers: Set<PagodaLayer>? = null,
        expectedLayers: Set<PagodaLayer>? = null,
        coveredLayers: Set<PagodaLayer> = emptySet(),
    ): Double {
        var s = 0.0
        if (chosenMeat + chosenVeg > 0) { // 空餐无"偏少一方"→不给荤素补分(守恒两处原行为)
            if (candMeat && chosenMeat <= chosenVeg) s += balanceFactor * BALANCE_BONUS // 本餐荤不多于素→加荤合理
            if (!candMeat && chosenVeg <= chosenMeat) s += balanceFactor * BALANCE_BONUS // 本餐素不多于荤→加素合理
        }
        if (!chosenHasStaple && candStaple) s += STAPLE_BONUS // 本餐还没主食→补主食(空餐首道主食也加)

        // [AI生成] P2 餐次差异化：仅当期待层非空且候选层已知时叠加(加餐 SNACK 期待空→跳过,等价旧行为)。
        if (!expectedLayers.isNullOrEmpty() && candLayers != null) {
            val stillNeeded = expectedLayers - coveredLayers // 期待里还缺的层
            if ((candLayers intersect stillNeeded).isNotEmpty()) s += LAYER_FILL_BONUS // 能补缺层→加分(补一层即给,不线性累加)
            // 晚餐清淡:仅"纯鱼禽肉蛋"候选在不期待荤的餐次(晚餐)轻降——只降整块硬荤,不误伤合炒/豆/奶/蔬(豆腐/牛奶清淡好选择)。
            if (PagodaLayer.ANIMAL_FOODS !in expectedLayers && candLayers == setOf(PagodaLayer.ANIMAL_FOODS)) s -= OFF_LAYER_PENALTY
        }
        return s
    }
}
