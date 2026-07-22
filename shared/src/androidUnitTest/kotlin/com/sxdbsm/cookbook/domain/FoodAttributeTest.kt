package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * @File : FoodAttributeTest
 * @Time : 2026/07/22
 * @Author : SXD-AI
 * @Desc : 食材属性标签 → care 映射（FoodAttributeCare.expand）快照单测，锁定属性→病种/level 不漂移。
 * <p>
 * [AI生成] 属性标签体系(方案B)守卫：映射改动须同步改预期，防误改导致忌口判定静默变化。
 **/
class FoodAttributeTest {

    @Test
    fun `含酒精展开为痛风避免加高血压限`() {
        val r = FoodAttributeCare.expand(listOf(FoodAttribute.CONTAINS_ALCOHOL))
        assertEquals(2, r.size)
        assertTrue(r.any { it.categoryCode == "care_gout" && it.level == "avoid" }, "痛风应 avoid")
        assertTrue(r.any { it.categoryCode == "care_hypertension" && it.level == "limit" }, "高血压应 limit")
    }

    @Test
    fun `加工浓缩果糖展开为痛风限加糖尿病限`() {
        val r = FoodAttributeCare.expand(listOf(FoodAttribute.PROCESSED_FRUCTOSE))
        assertEquals(2, r.size)
        assertTrue(r.any { it.categoryCode == "care_gout" && it.level == "limit" })
        assertTrue(r.any { it.categoryCode == "care_diabetes" && it.level == "limit" })
    }

    @Test
    fun `反式脂肪展开为高血脂避免`() {
        val r = FoodAttributeCare.expand(listOf(FoodAttribute.TRANS_FAT))
        assertEquals(1, r.size)
        assertEquals("care_hyperlipidemia", r[0].categoryCode)
        assertEquals("avoid", r[0].level)
    }

    @Test
    fun `高胆固醇内脏展开为高血脂限`() {
        val r = FoodAttributeCare.expand(listOf(FoodAttribute.ORGAN_HIGH_CHOLESTEROL))
        assertEquals(1, r.size)
        assertEquals("care_hyperlipidemia", r[0].categoryCode)
        assertEquals("limit", r[0].level)
    }

    @Test
    fun `fromCode解析已知返回枚举未知返回null`() {
        assertEquals(FoodAttribute.CONTAINS_ALCOHOL, FoodAttribute.fromCode("CONTAINS_ALCOHOL"))
        assertEquals(FoodAttribute.PROCESSED_FRUCTOSE, FoodAttribute.fromCode("PROCESSED_FRUCTOSE"))
        assertEquals(null, FoodAttribute.fromCode("UNKNOWN_ATTR"))
    }

    @Test
    fun `多属性标签展开合并全部care`() {
        val r = FoodAttributeCare.expand(listOf(FoodAttribute.CONTAINS_ALCOHOL, FoodAttribute.PROCESSED_FRUCTOSE))
        assertEquals(4, r.size) // 2 + 2
    }

    @Test
    fun `每个属性都有至少一条care映射防漏配`() {
        for (attr in FoodAttribute.values()) {
            assertTrue(FoodAttributeCare.MAP[attr]?.isNotEmpty() == true, "$attr 缺 care 映射")
        }
    }
}
