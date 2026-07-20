package com.sxdbsm.cookbook.ai

import com.sxdbsm.cookbook.ai.model.DishCandidate
import com.sxdbsm.cookbook.ai.model.HealthConstraints
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * @File : RecommendationPromptTest
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 云端推荐 Prompt 构造单测（R5 免责约束 / R2 近吃标签）
 * <p>
 * [AI生成] 云端 AI 会诊落地：验证 prompt 含禁医疗断言约束、候选带"N天前吃过"标签。
 **/
class RecommendationPromptTest {

    private fun cand(id: Long, name: String, recentDaysAgo: Int? = null, isMeat: Boolean = false, isStaple: Boolean = false) = DishCandidate(
        id = id, name = name, mainNames = listOf("主料"), secondaryNames = emptyList(),
        seasoningsOnHand = emptyList(), limitHits = emptyList(), recommendHits = emptyList(),
        isRecent = recentDaysAgo != null, score = 1.0, recentDaysAgo = recentDaysAgo,
        isMeat = isMeat, isStaple = isStaple,
    )

    @Test
    fun `R5_system含禁医疗断言与免责约束`() {
        val req = RecommendationPrompt.build(listOf(cand(1, "番茄炒蛋")), HealthConstraints(), mealCount = 3)
        assertTrue(req.system.contains("不承诺疗效"), "system 应含不承诺疗效")
        assertTrue(req.system.contains("降压"), "system 应列出禁用医疗断言词")
    }

    @Test
    fun `R2_候选带近吃标签`() {
        val req = RecommendationPrompt.build(
            listOf(cand(1, "红烧肉", 3), cand(2, "清蒸鱼", 0), cand(3, "凉拌黄瓜")),
            HealthConstraints(), mealCount = 3,
        )
        assertTrue(req.user.contains("3天前吃过"), "近吃标签应显示天数")
        assertTrue(req.user.contains("今天吃过"), "当天吃过应显示'今天吃过'")
        // 没吃过的菜不带近吃标签
        assertTrue(!req.user.substringAfter("凉拌黄瓜").substringBefore("\n").contains("吃过"))
    }

    @Test
    fun `组合完整性_system含荤素主食引导_候选带荤素主食标注`() {
        val req = RecommendationPrompt.build(
            listOf(cand(1, "红烧肉", isMeat = true), cand(2, "米饭", isStaple = true), cand(3, "清炒油菜")),
            HealthConstraints(), mealCount = 3,
        )
        assertTrue(req.system.contains("荤素搭配") && req.system.contains("主食"), "system 应含每餐荤素+主食软引导")
        // 荤菜标"荤"、主食标"主食"、素菜标"素"
        assertTrue(req.user.substringAfter("红烧肉").substringBefore("\n").contains("荤"), "荤菜候选应标『荤』")
        assertTrue(req.user.substringAfter("米饭").substringBefore("\n").contains("主食"), "主食候选应标『主食』")
        assertTrue(req.user.substringAfter("清炒油菜").substringBefore("\n").contains("素"), "素菜候选应标『素』")
    }

    @Test
    fun `R2_口味汇总常吃菜系喂system_偏新鲜不喂空不喂`() {
        // 有口味 + 非新鲜风格 → system 含"爱吃川菜"
        val withTaste = RecommendationPrompt.build(
            listOf(cand(1, "回锅肉")), HealthConstraints(), mealCount = 3,
            tasteCuisines = listOf("川菜", "粤菜"),
        )
        assertTrue(withTaste.system.contains("川菜"), "口味汇总应把常吃菜系喂给 system")
        // 偏『新鲜』风格 → 不喂口味(避免与"换口味"矛盾)
        val fresh = RecommendationPrompt.build(
            listOf(cand(1, "回锅肉")), HealthConstraints(), mealCount = 3,
            style = RecommendationStyle.FRESH, tasteCuisines = listOf("川菜"),
        )
        assertTrue(!fresh.system.contains("平时爱吃"), "偏新鲜不喂口味汇总")
        // 空口味 → 不喂(向后兼容)
        val empty = RecommendationPrompt.build(listOf(cand(1, "回锅肉")), HealthConstraints(), mealCount = 3)
        assertTrue(!empty.system.contains("平时爱吃"), "无口味历史不喂")
    }
}
