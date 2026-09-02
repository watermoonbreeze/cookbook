package com.sxdbsm.cookbook.android.ui.picker

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 营养字段键集契约测试。[AI修改] 2026-09-02 审查建议2：
 * 键名是 editedN 标记 / guessedField 弱化 / writeNutritionFields 回写 / UI NutrientField 四方共用的契约——
 * 改键名或增删字段时本测试先红，提醒四处同步（见 NutrientKey KDoc 的"加第 11 个字段必改三处"）。
 */
class NutrientKeyContractTest {

    @Test
    fun 键集内容锁定() {
        assertEquals(
            setOf("kcal", "protein", "fat", "carb", "fiber", "sodium", "potassium", "calcium", "gi", "purine"),
            NutrientKey.ALL,
        )
    }

    @Test
    fun 键集无重复且数量为10() {
        assertEquals(10, NutrientKey.ALL.size)
        assertEquals(NutrientKey.ALL.size, NutrientKey.ALL.distinct().size)
    }
}
