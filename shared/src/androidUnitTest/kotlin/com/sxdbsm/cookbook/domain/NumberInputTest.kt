package com.sxdbsm.cookbook.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * @File : NumberInputTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 数字输入容错解析测试(UX走查 H3)——结尾/开头小数点不丢值。
 * [AI生成]
 **/
class NumberInputTest {

    @Test
    fun 结尾小数点不丢值() {
        assertEquals(30.0, parseDecimalInput("30."))
        assertEquals(1.5, parseDecimalInput("1.5"))
        assertEquals(175.0, parseDecimalInput("175"))
    }

    @Test
    fun 开头小数点补零() {
        assertEquals(0.5, parseDecimalInput(".5"))
        assertEquals(0.5, parseDecimalInput("0.5"))
    }

    @Test
    fun 空与纯点与多点返null() {
        assertNull(parseDecimalInput(""))
        assertNull(parseDecimalInput("."))
        assertNull(parseDecimalInput("  "))
        assertNull(parseDecimalInput("1.7.5"))
        assertNull(parseDecimalInput("abc"))
    }
}
