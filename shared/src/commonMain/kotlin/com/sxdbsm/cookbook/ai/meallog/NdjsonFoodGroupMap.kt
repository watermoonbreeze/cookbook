package com.sxdbsm.cookbook.ai.meallog

import com.sxdbsm.cookbook.domain.FoodGroup

/**
 * @File : NdjsonFoodGroupMap
 * @Time : 2026/09/03
 * @Author : SXD-AI
 * @Desc : NDJSON 协议 food_group 词表 → FoodGroup.Group 枚举 映射（AUTOGEN-UNIFY 决策点1A）
 * <p>
 * 词表来源：AiMealPrompt.kt NDJSON_SYSTEM_PROMPT 的规则行
 * `food_group: meat/vegetable/staple/fruit/dairy/egg/bean/seafood/seasoning`（9 词）。
 * <p>
 * 映射策略（字段级「AI 优先、本地兜底」的合法性校验层）：
 * - **7 个精确词一一对应直用**：staple/vegetable/fruit/dairy/egg/bean/seafood——AI 只覆盖它「能说清」的类，
 *   误判面受词表硬约束（食材 CREATE 一次性+同名 dedup，无重复漂移问题）。
 * - **meat → null**：NDJSON 粗粒度（不分红/白肉），交本地 classify 全权细分（猪牛羊/鸡鸭鹅关键词表更强）。
 * - **seasoning → null**：FoodGroup 无调料枚举(classify 调料返 null)，且 AI 的 seasoning 提示不参与
 *   isSeasoning 判定(靠名字在库内调料集)——已知边界见蓝图决策点 1 注记，留待后续批次评估。
 * - 未知/空/大小写变体 → null（一律本地兜底，禁武断映射）。
 * <p>
 * [AI生成] AUTOGEN-UNIFY：修复 AI 判定的大类在 MealStreamDraftMapper→MultiDayRecorder 链上被丢弃的断链。
 **/
object NdjsonFoodGroupMap {

    private val MAP: Map<String, FoodGroup.Group> = mapOf(
        "staple" to FoodGroup.Group.STAPLE,
        "vegetable" to FoodGroup.Group.VEGETABLE,
        "fruit" to FoodGroup.Group.FRUIT,
        "dairy" to FoodGroup.Group.DAIRY,
        "egg" to FoodGroup.Group.EGG,
        "bean" to FoodGroup.Group.BEAN,
        "seafood" to FoodGroup.Group.FISH,
    )

    /** NDJSON food_group 原始词 → 枚举；meat/seasoning/未知/空 → null（本地 classify 兜底）。[AI生成] */
    fun map(raw: String?): FoodGroup.Group? = raw?.trim()?.lowercase()?.let { MAP[it] }
}
