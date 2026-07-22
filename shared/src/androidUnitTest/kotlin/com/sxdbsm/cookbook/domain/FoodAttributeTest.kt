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

    @Test
    fun `每个属性的care_code都能解析到分类名`() {
        for (attr in FoodAttribute.values()) {
            FoodAttributeCare.MAP[attr].orEmpty().forEach { c ->
                assertTrue(
                    FoodAttributeCare.CARE_CODE_TO_NAME.containsKey(c.categoryCode),
                    "$attr 的 ${c.categoryCode} 缺 CARE_CODE_TO_NAME 映射",
                )
            }
        }
    }

    @Test
    fun `expandDeduped同病种取更严level`() {
        // 含酒精(痛风 avoid) + 浓肉汤(痛风 avoid)：痛风只留一条 avoid，不因去重降级。
        val r = FoodAttributeCare.expandDeduped(listOf(FoodAttribute.CONTAINS_ALCOHOL, FoodAttribute.RICH_BROTH))
        val gout = r.filter { it.categoryCode == "care_gout" }
        assertEquals(1, gout.size, "痛风去重后只剩一条")
        assertEquals("avoid", gout[0].level)
        // 高血压来自含酒精仍在。
        assertTrue(r.any { it.categoryCode == "care_hypertension" && it.level == "limit" })
    }

    @Test
    fun `expandDeduped两属性同病种不同level取avoid`() {
        // 加工果糖(痛风 limit) + 含酒精(痛风 avoid) → 痛风取 avoid（更严）。
        val r = FoodAttributeCare.expandDeduped(listOf(FoodAttribute.PROCESSED_FRUCTOSE, FoodAttribute.CONTAINS_ALCOHOL))
        val gout = r.filter { it.categoryCode == "care_gout" }
        assertEquals(1, gout.size)
        assertEquals("avoid", gout[0].level)
    }

    @Test
    fun `deriveAttributes按reason精确反推属性`() {
        val reasons = FoodAttributeCare.expand(listOf(FoodAttribute.CONTAINS_ALCOHOL, FoodAttribute.DEEP_FRIED)).map { it.reason }
        val got = FoodAttributeCare.deriveAttributes(reasons).toSet()
        assertEquals(setOf(FoodAttribute.CONTAINS_ALCOHOL, FoodAttribute.DEEP_FRIED), got)
    }

    @Test
    fun `deriveAttributes未知reason忽略不误报`() {
        val got = FoodAttributeCare.deriveAttributes(listOf("这是用户手写的原因，不属于任何属性模板"))
        assertTrue(got.isEmpty())
    }

    @Test
    fun `deriveAttributes与expand互为逆运算`() {
        val attrs = listOf(FoodAttribute.PROCESSED_FRUCTOSE, FoodAttribute.CURED_PROCESSED_MEAT, FoodAttribute.PICKLED_HIGH_SALT)
        val reasons = FoodAttributeCare.expand(attrs).map { it.reason }
        assertEquals(attrs.toSet(), FoodAttributeCare.deriveAttributes(reasons).toSet())
    }
}
