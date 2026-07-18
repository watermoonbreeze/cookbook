package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : TasteProfileTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 口味画像匹配度单测（纯函数、不依赖 DB）
 * <p>
 * [AI生成] 算法3项·口味画像：证明菜系/做法/主料偏好归一与匹配加权正确、空画像中性。
 **/
class TasteProfileTest {

    @Test
    fun `空画像匹配分恒0且isEmpty`() {
        assertTrue(TasteProfile.EMPTY.isEmpty)
        assertEquals(0.0, TasteProfile.EMPTY.matchScore("川菜", listOf("红烧"), listOf("五花肉")), 1e-9)
    }

    @Test
    fun `最常吃的菜系匹配满分_没吃过的为0`() {
        // 川菜吃10次(最高)、粤菜2次 → 川菜菜系维=1.0；只测菜系维(无做法/主料数据该维不贡献)。
        val p = TasteProfile(cuisineFreq = mapOf("川菜" to 10, "粤菜" to 2))
        assertFalse(p.isEmpty)
        // 菜系权重0.4：川菜候选 = 0.4×1.0 = 0.4
        assertEquals(0.4, p.matchScore("川菜", emptyList(), emptyList()), 1e-9)
        // 粤菜 = 0.4×(2/10)=0.08
        assertEquals(0.08, p.matchScore("粤菜", emptyList(), emptyList()), 1e-9)
        // 没吃过的鲁菜 = 0
        assertEquals(0.0, p.matchScore("鲁菜", emptyList(), emptyList()), 1e-9)
        // 空菜系不计菜系维
        assertEquals(0.0, p.matchScore("", emptyList(), emptyList()), 1e-9)
    }

    @Test
    fun `做法与主料取匹配最强的一味_多值不稀释`() {
        val p = TasteProfile(
            methodFreq = mapOf("清蒸" to 8, "红烧" to 4),
            mainFreq = mapOf("鱼" to 6, "豆腐" to 3),
        )
        // 做法维0.3：候选含"红烧+清蒸"→取最强清蒸(8/8=1.0)→0.3×1.0=0.3
        // 主料维0.3：候选含"鱼+青菜"→鱼(6/6=1.0)→0.3×1.0=0.3；共0.6
        assertEquals(0.6, p.matchScore("", listOf("红烧", "清蒸"), listOf("鱼", "青菜")), 1e-9)
        // 只含次高做法红烧(4/8=0.5)→0.15；主料豆腐(3/6=0.5)→0.15；共0.3
        assertEquals(0.3, p.matchScore("", listOf("红烧"), listOf("豆腐")), 1e-9)
    }

    @Test
    fun `三维加权合计_封顶1`() {
        val p = TasteProfile(
            cuisineFreq = mapOf("川菜" to 5),
            methodFreq = mapOf("爆炒" to 5),
            mainFreq = mapOf("牛肉" to 5),
        )
        // 全部满偏好：0.4+0.3+0.3=1.0
        assertEquals(1.0, p.matchScore("川菜", listOf("爆炒"), listOf("牛肉")), 1e-9)
    }
}
