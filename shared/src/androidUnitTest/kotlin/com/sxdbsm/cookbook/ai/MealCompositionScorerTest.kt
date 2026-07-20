package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : MealCompositionScorerTest
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 单餐组合补分(荤素平衡+主食覆盖)单一真相源测试——锁定两处调用方(combineScore/PeriodPlanner)接入后零行为漂移。
 * [AI生成] 算法打磨·技术债收敛守卫。
 **/
class MealCompositionScorerTest {

    private val B = MealCompositionScorer.BALANCE_BONUS // 0.7
    private val S = MealCompositionScorer.STAPLE_BONUS  // 0.9

    private fun bonus(
        candMeat: Boolean, candStaple: Boolean,
        chosenMeat: Int, chosenVeg: Int, chosenHasStaple: Boolean,
        balanceFactor: Double = 1.0,
    ) = MealCompositionScorer.compositionBonus(candMeat, candStaple, chosenMeat, chosenVeg, chosenHasStaple, balanceFactor)

    @Test
    fun 空餐不给荤素补分_守恒PeriodPlanner的isNotEmpty守卫() {
        // 本餐为空(0荤0素):荤/素候选都不该拿平衡补分(无"偏少一方")。
        assertEquals(0.0, bonus(candMeat = true, candStaple = false, chosenMeat = 0, chosenVeg = 0, chosenHasStaple = false), 1e-9)
        assertEquals(0.0, bonus(candMeat = false, candStaple = false, chosenMeat = 0, chosenVeg = 0, chosenHasStaple = false), 1e-9)
    }

    @Test
    fun 空餐首道主食照常加分_守恒原主食分不受isNotEmpty守卫() {
        // 主食补分在空餐时仍生效(原 PeriodPlanner 主食分不受 isNotEmpty 守卫)。
        assertEquals(S, bonus(candMeat = false, candStaple = true, chosenMeat = 0, chosenVeg = 0, chosenHasStaple = false), 1e-9)
    }

    @Test
    fun 本餐已有荤_加素补平衡() {
        // 已选1荤0素:素候选(veg<=meat)拿补分,荤候选(meat<=veg? 1<=0 否)不拿。
        assertEquals(B, bonus(candMeat = false, candStaple = false, chosenMeat = 1, chosenVeg = 0, chosenHasStaple = false), 1e-9)
        assertEquals(0.0, bonus(candMeat = true, candStaple = false, chosenMeat = 1, chosenVeg = 0, chosenHasStaple = false), 1e-9)
    }

    @Test
    fun 平衡态两类候选都能拿补分_倾向补少的一方() {
        // 已选1荤1素(平衡):荤(1<=1)和素(1<=1)候选各自都能拿补分,倾向补少的一方。
        assertEquals(B, bonus(candMeat = true, candStaple = false, chosenMeat = 1, chosenVeg = 1, chosenHasStaple = false), 1e-9)
        assertEquals(B, bonus(candMeat = false, candStaple = false, chosenMeat = 1, chosenVeg = 1, chosenHasStaple = false), 1e-9)
    }

    @Test
    fun 已有主食时主食候选不再加主食分() {
        // 荤候选加进"1荤0素"餐→不触发平衡补分(1<=0 否)；已有主食→不加主食分。二者皆 0→总 0，隔离验证主食分支。
        assertEquals(0.0, bonus(candMeat = true, candStaple = true, chosenMeat = 1, chosenVeg = 0, chosenHasStaple = true), 1e-9)
    }

    @Test
    fun 荤素与主食可叠加() {
        // 已选1荤0素无主食:主食素候选 = 平衡补分(素) + 主食补分。
        assertEquals(B + S, bonus(candMeat = false, candStaple = true, chosenMeat = 1, chosenVeg = 0, chosenHasStaple = false), 1e-9)
    }

    @Test
    fun balanceFactor只缩放荤素不缩放主食() {
        // 周计划 f.balance 系数只作用荤素补分,主食补分不受缩放(与两处原实现一致)。
        val f = 1.6
        assertEquals(f * B + S, bonus(candMeat = false, candStaple = true, chosenMeat = 1, chosenVeg = 0, chosenHasStaple = false, balanceFactor = f), 1e-9)
    }
}
