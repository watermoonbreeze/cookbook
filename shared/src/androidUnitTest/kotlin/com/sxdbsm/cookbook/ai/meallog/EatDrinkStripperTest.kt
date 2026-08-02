package com.sxdbsm.cookbook.ai.meallog

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * EatDrinkStripper 纯函数白盒测试。
 * 覆盖：16 条剥离场景 + 7 条不剥离场景。
 */
class EatDrinkStripperTest {

    // ══════════════════════════════════════
    // 剥离场景
    // ══════════════════════════════════════

    @Test fun `S1 动词+体标记 吃了`() =
        assertStrip("红烧肉", "吃了红烧肉")

    @Test fun `S2 喝+体标记`() =
        assertStrip("排骨汤", "喝了排骨汤")

    @Test fun `S3 时间副词+动词+体标记`() =
        assertStrip("饺子", "刚吃了饺子")

    @Test fun `S4 意愿情态+动词`() =
        assertStrip("火锅", "准备吃火锅")

    @Test fun `S5 单字情态+动词`() =
        assertStrip("红烧肉", "想吃红烧肉")

    @Test fun `S6 双字情态+动词`() =
        assertStrip("火锅", "打算吃火锅")

    @Test fun `S7 情态+喝`() =
        assertStrip("牛奶", "要喝牛奶")

    @Test fun `S8 双修饰叠加`() =
        assertStrip("火锅", "刚准备吃火锅")

    @Test fun `S9 双情态叠加`() =
        assertStrip("排骨汤", "想要喝排骨汤")

    @Test fun `S10 衔接+时间+动词+体标记`() =
        assertStrip("饺子", "就刚吃了饺子")

    @Test fun `S11 体标记+体补语`() =
        assertStrip("火锅", "吃了些火锅")

    @Test fun `S12 体标记+体补语(个)`() =
        assertStrip("苹果", "吃了个苹果")

    @Test fun `S13 体标记(的)`() =
        assertStrip("火锅", "吃的火锅")

    @Test fun `S14 体标记(过)`() =
        assertStrip("火锅", "吃过火锅")

    @Test fun `S15 动词+体补语(无体标记)`() =
        assertStrip("水果", "吃点水果")

    @Test fun `S16 动词+体标记+空格`() =
        assertStrip("红烧肉", "吃了 红烧肉")

    // ══════════════════════════════════════
    // 不剥离场景
    // ══════════════════════════════════════

    @Test fun `N1 无动词核`() =
        assertNoStrip("火锅")

    @Test fun `N2 无动词核(菜名)`() =
        assertNoStrip("红烧肉")

    @Test fun `N3 有修饰无动词`() =
        assertNoStrip("准备")

    @Test fun `N4 剥离后为空`() =
        assertNoStrip("吃了")

    @Test fun `N5 剥离后为空(双修饰)`() =
        assertNoStrip("刚喝了")

    @Test fun `N6 剥离后为空(修饰+动词)`() =
        assertNoStrip("准备吃")

    @Test fun `N7 无动词核(餐次词残留)`() =
        assertNoStrip("中午")

    // ══════════════════════════════════════
    // 辅助
    // ══════════════════════════════════════

    private fun assertStrip(expected: String, input: String) {
        assertEquals(expected, EatDrinkStripper.strip(input),
            "strip(\"$input\") 应为 \"$expected\"")
    }

    private fun assertNoStrip(input: String) {
        assertEquals(input, EatDrinkStripper.strip(input),
            "strip(\"$input\") 应保留原文（不剥离）")
    }
}
