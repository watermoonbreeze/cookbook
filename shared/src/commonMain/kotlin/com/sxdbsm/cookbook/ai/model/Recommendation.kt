package com.sxdbsm.cookbook.ai.model

/**
 * @File : Recommendation
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 「按在手食材推荐下一餐」的数据契约（平台无关）
 * <p>
 * 规则层(HealthRuleEngine)与模型层(Orchestrator)之间的输入/输出模型。规则层先筛出
 * 安全可做的候选(DishCandidate)，模型只在候选集里搭组合(MealSuggestion)。
 * <p>
 * [AI生成] S0：先把契约与纯规则引擎立起来，不依赖模型、可单测。
 **/

/** 食材在菜品中的角色。[AI生成] */
enum class IngredientRole {
    MAIN, // 主料：决定“可做性”，必须在手
    SECONDARY, // 辅料：决定做法，可缺
    SEASONING, // 调料：默认常备，不影响可做性
}

/** 规则引擎输入的菜品（已把食材按角色标好）。[AI生成] */
data class RuleDish(
    val id: Long,
    val name: String,
    val ingredients: List<RuleDishIngredient>,
)

data class RuleDishIngredient(
    val ingredientId: Long,
    val name: String,
    val role: IngredientRole,
)

/** 健康硬约束：由代码强校验，模型无权绕过。[AI生成] */
data class HealthConstraints(
    val avoidIngredientIds: Set<Long> = emptySet(), // 忌口(避免)：含则剔除
    val limitIngredientIds: Set<Long> = emptySet(), // 限量：保留但降权+提示
    val labels: List<String> = emptyList(), // 粗约束标签，喂给模型(如 "忌高嘌呤","低钠")
)

/**
 * 规则筛过的安全候选菜（可做 + 不犯忌）。[AI生成]
 *
 * 主料一定在手（否则已被剔除）；辅料在手/缺用于模型给做法建议。
 */
data class DishCandidate(
    val id: Long,
    val name: String,
    val mainOnHand: List<String>, // 在手主料
    val secondaryOnHand: List<String>, // 在手辅料 → 决定做法
    val secondaryMissing: List<String>, // 缺的辅料 → 可提示/换做法
    val limitHits: List<String>, // 命中“限量”的食材(保留但提示)
    val isRecent: Boolean, // 最近吃过(去重降权)
    val score: Double, // 规则打分，降序排列
)

/** 模型输出：3 个下一餐组合，每餐 2~3 菜。[AI生成] */
data class RecommendationDraft(
    val suggestions: List<MealSuggestion>,
)

data class MealSuggestion(
    val dishIds: List<Long>, // 这一餐的菜(2~3 个)；必须来自候选集
    val reason: String, // 一句人话理由
    val cookingHint: String? = null, // 按在手辅料给的做法建议
)
