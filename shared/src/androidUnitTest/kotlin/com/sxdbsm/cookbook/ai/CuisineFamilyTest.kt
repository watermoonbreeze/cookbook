package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * @File : CuisineFamilyTest
 * @Time : 2026/07/20
 * @Author : SXD-AI
 * @Desc : 菜系族群判定测试(用户#2:一餐同菜系不混搭·中式优先)。
 * [AI生成]
 **/
class CuisineFamilyTest {

    @Test
    fun 西餐判西式其余判中式() {
        assertTrue(isWesternCuisine("西餐"))
        assertTrue(isWesternCuisine("西式"))
        assertFalse(isWesternCuisine("家常菜"))
        assertFalse(isWesternCuisine("川菜"))
        assertFalse(isWesternCuisine(""), "空/未知按中式(国内为主·不误罚)")
    }

    @Test
    fun 同族判定() {
        assertTrue(sameCuisineFamily("家常菜", "川菜"), "都中式=同族")
        assertTrue(sameCuisineFamily("西餐", "西式"), "都西式=同族")
        assertFalse(sameCuisineFamily("家常菜", "西餐"), "中式↔西式=不同族(不混搭)")
        assertTrue(sameCuisineFamily("", "粤菜"), "空按中式与中式同族")
    }
}
