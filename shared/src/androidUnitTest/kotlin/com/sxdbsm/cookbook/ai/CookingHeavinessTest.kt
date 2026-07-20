package com.sxdbsm.cookbook.ai

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @File : CookingHeavinessTest
 * @Time : 2026/07/21
 * @Author : SXD-AI
 * @Desc : 烹饪油腻度族判定测试(MMR 重油族维度用·纯烹饪常识分类)。
 * [AI生成]
 **/
class CookingHeavinessTest {

    @Test
    fun 重油族() {
        assertEquals(2, cookingHeaviness(listOf("红烧")))
        assertEquals(2, cookingHeaviness(listOf("油炸")))
        assertEquals(2, cookingHeaviness(listOf("香煎")))
        assertEquals(2, cookingHeaviness(listOf("干煸")))
        assertEquals(2, cookingHeaviness(listOf("爆炒")))
    }

    @Test
    fun 清淡族() {
        assertEquals(0, cookingHeaviness(listOf("清蒸")))
        assertEquals(0, cookingHeaviness(listOf("白灼")))
        assertEquals(0, cookingHeaviness(listOf("水煮")))
        assertEquals(0, cookingHeaviness(listOf("清炖")))
        assertEquals(0, cookingHeaviness(listOf("凉拌")))
    }

    @Test
    fun 中性与无数据() {
        assertEquals(1, cookingHeaviness(listOf("炒")), "普通炒(非爆炒)归中性")
        assertEquals(1, cookingHeaviness(emptyList()), "无做法数据→中性(不误判)")
        assertEquals(1, cookingHeaviness(listOf("焖")), "未列关键词→中性")
    }

    @Test
    fun 多做法取最油() {
        assertEquals(2, cookingHeaviness(listOf("清蒸", "红烧")), "任一重油→整体重油")
        assertEquals(0, cookingHeaviness(listOf("清蒸", "炖")), "都清淡→清淡")
        assertEquals(1, cookingHeaviness(listOf("炒", "焖")), "都中性→中性")
    }
}
