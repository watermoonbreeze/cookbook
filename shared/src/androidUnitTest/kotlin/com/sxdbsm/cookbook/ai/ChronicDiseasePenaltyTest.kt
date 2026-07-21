package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : ChronicDiseasePenaltyTest
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 慢病软降罚分公式单测（步长/每维封顶/整体封顶）
 * <p>
 * 锁死单餐与周计划共用的 [ChronicDiseasePenalty] 公式，防调参漂移。
 * <p>
 * [AI生成] D4 慢病软降共享公式回归守卫。
 **/
class ChronicDiseasePenaltyTest {

    @Test
    fun `无命中罚0`() {
        assertEquals(0.0, ChronicDiseasePenalty.penaltyBase(0, 0), 1e-9)
    }

    @Test
    fun `单命中按步长`() {
        assertEquals(0.25, ChronicDiseasePenalty.penaltyBase(1, 0), 1e-9)
        assertEquals(0.25, ChronicDiseasePenalty.penaltyBase(0, 1), 1e-9)
        assertEquals(0.5, ChronicDiseasePenalty.penaltyBase(1, 1), 1e-9)
    }

    @Test
    fun `每维最多计2味`() {
        // 单维 5 味 GI 仍只按 2 味计(0.5)，不随味数线性膨胀。
        assertEquals(0.5, ChronicDiseasePenalty.penaltyBase(5, 0), 1e-9)
        assertEquals(0.5, ChronicDiseasePenalty.penaltyBase(0, 9), 1e-9)
    }

    @Test
    fun `两维叠加整体封顶0点7`() {
        // 2GI+2嘌呤 = 4×0.25 = 1.0，整体封顶到 0.7（非 1.0）。
        assertEquals(0.7, ChronicDiseasePenalty.penaltyBase(2, 2), 1e-9)
        assertEquals(0.7, ChronicDiseasePenalty.penaltyBase(9, 9), 1e-9)
    }
}
