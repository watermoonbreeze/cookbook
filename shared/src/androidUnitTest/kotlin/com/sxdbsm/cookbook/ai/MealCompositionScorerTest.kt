package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.domain.DietaryGuideline.PagodaLayer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
        candLayers: Set<PagodaLayer>? = null,
        expectedLayers: Set<PagodaLayer>? = null,
        coveredLayers: Set<PagodaLayer> = emptySet(),
    ) = MealCompositionScorer.compositionBonus(
        candMeat, candStaple, chosenMeat, chosenVeg, chosenHasStaple, balanceFactor,
        candLayers, expectedLayers, coveredLayers,
    )

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

    // ============ P2 餐次差异化 ============
    private val L = MealCompositionScorer.LAYER_FILL_BONUS // 0.5
    private val O = MealCompositionScorer.OFF_LAYER_PENALTY // 0.3

    @Test
    fun candidateLayers_一道菜可覆盖多层() {
        // 白菜(→蔬果层)+猪肉(→鱼禽肉蛋层):一道合炒菜覆盖两层,正是宝塔覆盖度语义。
        val layers = MealCompositionScorer.candidateLayers(listOf("白菜", "猪肉"), isMeat = true, isStaple = false)
        assertTrue(PagodaLayer.VEGETABLES_FRUITS in layers)
        assertTrue(PagodaLayer.ANIMAL_FOODS in layers)
    }

    @Test
    fun candidateLayers_classify全失败走布尔兜底且永不为空() {
        // 主料录不出(空)时落 isMeat/isStaple 粗兜底,保证非空(空集会让缺层判断退化)。
        assertEquals(setOf(PagodaLayer.ANIMAL_FOODS), MealCompositionScorer.candidateLayers(emptyList(), isMeat = true, isStaple = false))
        assertEquals(setOf(PagodaLayer.GRAINS), MealCompositionScorer.candidateLayers(emptyList(), isMeat = false, isStaple = true))
        assertEquals(setOf(PagodaLayer.VEGETABLES_FRUITS), MealCompositionScorer.candidateLayers(emptyList(), isMeat = false, isStaple = false))
    }

    @Test
    fun 补缺口_候选能补期待但还缺的层则加分() {
        val expected = setOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS, PagodaLayer.ANIMAL_FOODS)
        // 基线(旧分量):已选1荤1素+已含主食,素候选拿平衡补分 B、无主食分。两次仅 P2 分量不同。
        val fills = bonus(
            candMeat = false, candStaple = false, chosenMeat = 1, chosenVeg = 1, chosenHasStaple = true,
            candLayers = setOf(PagodaLayer.VEGETABLES_FRUITS), expectedLayers = expected, coveredLayers = emptySet(),
        )
        val alreadyCovered = bonus(
            candMeat = false, candStaple = false, chosenMeat = 1, chosenVeg = 1, chosenHasStaple = true,
            candLayers = setOf(PagodaLayer.VEGETABLES_FRUITS), expectedLayers = expected, coveredLayers = setOf(PagodaLayer.VEGETABLES_FRUITS),
        )
        assertEquals(B + L, fills, 1e-9) // 缺蔬果→补分
        assertEquals(B, alreadyCovered, 1e-9) // 已覆盖蔬果→不重复补(touchesExpected 仍 true 故不降)
    }

    @Test
    fun 晚餐纯肉轻降_合炒菜不降() {
        val dinner = setOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS) // 晚餐期待:谷+蔬(不含鱼禽肉蛋)
        val covered = setOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS) // 已覆盖谷蔬,无缺口
        // 基线:已选0荤1素+已含主食,荤候选拿平衡补分 B(0<=1)、无主食分。
        val pureMeat = bonus(
            candMeat = true, candStaple = false, chosenMeat = 0, chosenVeg = 1, chosenHasStaple = true,
            candLayers = setOf(PagodaLayer.ANIMAL_FOODS), expectedLayers = dinner, coveredLayers = covered,
        )
        val stirFry = bonus(
            candMeat = true, candStaple = false, chosenMeat = 0, chosenVeg = 1, chosenHasStaple = true,
            candLayers = setOf(PagodaLayer.VEGETABLES_FRUITS, PagodaLayer.ANIMAL_FOODS), expectedLayers = dinner, coveredLayers = covered,
        )
        assertEquals(B - O, pureMeat, 1e-9) // 纯肉完全不碰期待层→轻降
        assertEquals(B, stirFry, 1e-9) // 合炒菜碰了蔬果层→不降(晚餐不禁肉,禁的是整桌硬荤)
        assertTrue(stirFry > pureMeat) // 晚餐合炒优于纯肉
    }

    @Test
    fun 加餐空期待跳过P2_等价旧行为() {
        // SNACK expectedLayers 为空→整体跳过 P2,与不传 candLayers 的旧行为一致。
        val old = bonus(candMeat = true, candStaple = false, chosenMeat = 0, chosenVeg = 1, chosenHasStaple = true)
        val snack = bonus(
            candMeat = true, candStaple = false, chosenMeat = 0, chosenVeg = 1, chosenHasStaple = true,
            candLayers = setOf(PagodaLayer.ANIMAL_FOODS), expectedLayers = emptySet(), coveredLayers = emptySet(),
        )
        assertEquals(old, snack, 1e-9)
    }

    @Test
    fun 量级不碾压_锁定常量序防调参漂移() {
        // 主食(0.9) > 荤素(0.7) > 补层(0.5) > 非期待层降(0.3):主食始终最优先,P2 是软信号不压基本盘。
        assertTrue(O < L)
        assertTrue(L < B)
        assertTrue(B < S)
    }

    @Test
    fun P2不影响主食最优先_补层不挤掉主食() {
        // 首道主食(谷层)同时拿 STAPLE_BONUS + 补层分,仍应 > 单纯补层的非主食菜,守住"每餐尽量一道主食"。
        val lunch = setOf(PagodaLayer.GRAINS, PagodaLayer.VEGETABLES_FRUITS, PagodaLayer.ANIMAL_FOODS)
        val stapleFirst = bonus(
            candMeat = false, candStaple = true, chosenMeat = 0, chosenVeg = 0, chosenHasStaple = false,
            candLayers = setOf(PagodaLayer.GRAINS), expectedLayers = lunch, coveredLayers = emptySet(),
        )
        val vegFirst = bonus(
            candMeat = false, candStaple = false, chosenMeat = 0, chosenVeg = 0, chosenHasStaple = false,
            candLayers = setOf(PagodaLayer.VEGETABLES_FRUITS), expectedLayers = lunch, coveredLayers = emptySet(),
        )
        assertEquals(S + L, stapleFirst, 1e-9) // 空餐主食分照给 + 补谷层
        assertEquals(L, vegFirst, 1e-9) // 空餐无荤素分,仅补蔬果层
        assertTrue(stapleFirst > vegFirst)
    }
}
