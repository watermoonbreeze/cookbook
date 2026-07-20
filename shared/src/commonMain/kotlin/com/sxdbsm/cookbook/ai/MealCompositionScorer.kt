package com.sxdbsm.cookbook.ai

/**
 * @File : MealCompositionScorer
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 单餐"荤素搭配 + 主食覆盖"组合补分（纯函数·单一真相源）
 * <p>
 * 把散落在 [RecommendationOrchestrator.combineScore] 与 [PeriodPlanner.score] 两处、
 * 语义与常量完全相同的"补荤素缺口 + 补主食缺口"评分抽成唯一真相源，杜绝两份常量各改各的调参漂移。
 * 纯函数、无状态、可测；两个调用方接入后行为零变化（见下方 hasChosen 说明）。
 * <p>
 * [AI生成] 算法打磨·技术债收敛：MealCompositionScorer 统一荤素/主食组合补分。
 **/
object MealCompositionScorer {
    /** 同餐荤素平衡补分：本餐荤/素偏少的一方补上时给候选加分。[AI生成] */
    const val BALANCE_BONUS = 0.7

    /** 本餐还没有主食时，给主食候选的补分（略高于应季，让每餐尽量含一道主食）。[AI生成] */
    const val STAPLE_BONUS = 0.9

    /**
     * 候选加入本餐已选后的"组合完整性"补分（荤素平衡 + 主食覆盖）。[AI生成]
     *
     * @param candMeat 候选是否荤菜
     * @param candStaple 候选是否主食菜
     * @param chosenMeat 本餐已选荤菜数
     * @param chosenVeg 本餐已选素菜数
     * @param chosenHasStaple 本餐已选是否已含主食
     * @param balanceFactor 荤素补分的风格系数（周计划按 f.balance 调；单餐固定 1.0）。**只作用荤素、不作用主食**，与两处原实现一致。
     *
     * 关键：本餐**为空**（chosenMeat+chosenVeg==0）时不给荤素补分——空餐无"偏少的一方"，
     *   给了会让首道菜凭荤素身份白拿分。此守卫内建于此，使 PeriodPlanner 可无条件调用而保持原
     *   `if(mealChosen.isNotEmpty())` 守卫的行为；而主食补分在空餐时**照常**生效（首道主食该加分），
     *   与 PeriodPlanner 原实现（主食分不受 isNotEmpty 守卫）一致。combineScore 调用时本餐恒非空，两分支行为不变。
     */
    fun compositionBonus(
        candMeat: Boolean,
        candStaple: Boolean,
        chosenMeat: Int,
        chosenVeg: Int,
        chosenHasStaple: Boolean,
        balanceFactor: Double = 1.0,
    ): Double {
        var s = 0.0
        if (chosenMeat + chosenVeg > 0) { // 空餐无"偏少一方"→不给荤素补分(守恒两处原行为)
            if (candMeat && chosenMeat <= chosenVeg) s += balanceFactor * BALANCE_BONUS // 本餐荤不多于素→加荤合理
            if (!candMeat && chosenVeg <= chosenMeat) s += balanceFactor * BALANCE_BONUS // 本餐素不多于荤→加素合理
        }
        if (!chosenHasStaple && candStaple) s += STAPLE_BONUS // 本餐还没主食→补主食(空餐首道主食也加)
        return s
    }
}
