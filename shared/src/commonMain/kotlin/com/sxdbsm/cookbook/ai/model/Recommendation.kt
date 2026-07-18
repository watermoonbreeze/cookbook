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

/** 推荐取材范围。[AI生成] */
enum class RecommendMode {
    PANTRY, // 通过库存：只从在手食材出发
    RANDOM, // 随机推荐：从整个食材库出发(相当于都可做)
}

/** 食材在菜品中的角色。[AI生成] */
enum class IngredientRole {
    MAIN, // 主料：决定“可做性”，必须在手
    SECONDARY, // 辅料：决定做法，可缺
    SEASONING, // 调料：默认常备，不影响可做性
}

/** 取数层聚合好的规则引擎完整输入。[AI生成] */
data class RecommendationInput(
    val dishes: List<RuleDish>,
    val pantryIngredientIds: Set<Long>,
    val constraints: HealthConstraints,
    val recentDishIds: Set<Long>,
    val shortageIngredientIds: Set<Long> = emptySet(), // [AI生成] 在库但可用份数≤0的食材：含它的菜仍推荐但排后+标"库存不足"
    val recentDishDaysAgo: Map<Long, Int> = emptyMap(), // [AI生成] B2：去重窗口内吃过的菜→距今天数，用于排最后+标注"N天前吃过"
    // [AI生成] 增长型推荐 P2：从用户历史/收藏/营养派生的画像信号 + 推荐风格(权重预设)。
    val preferenceScores: Map<Long, Double> = emptyMap(), // 每菜偏好画像[0,1]：常做(preference)+收藏
    val nutritionBalanceScores: Map<Long, Double> = emptyMap(), // 每菜与近期已吃的营养互补度[-1,1]
    val mainRepeatCounts: Map<Long, Int> = emptyMap(), // 每菜主料近期重复次数(去重成长)
    val style: com.sxdbsm.cookbook.ai.RecommendationStyle = com.sxdbsm.cookbook.ai.RecommendationStyle.DEFAULT, // 推荐风格→权重
    // [AI生成] 慢病数值软约束:已登记病种(空=不触发软降)+名→GI(仅糖尿病需)。仅营养风格生效。
    val conditions: Set<com.sxdbsm.cookbook.domain.HealthCondition> = emptySet(),
    val giByName: Map<String, Double> = emptyMap(),
    // [AI生成] 算法3项：口味画像(菜系/做法/主料偏好频次) + 每菜距上次做天数(不限窗口,仅偏新鲜时间衰减用)。
    val tasteProfile: com.sxdbsm.cookbook.ai.TasteProfile = com.sxdbsm.cookbook.ai.TasteProfile.EMPTY,
    val lastCookedDaysAgo: Map<Long, Int> = emptyMap(),
)

/** 规则引擎输入的菜品（已把食材按角色标好）。[AI生成] */
data class RuleDish(
    val id: Long,
    val name: String,
    val ingredients: List<RuleDishIngredient>,
    val cuisine: String = "", // [AI生成] 菜系(家常/川菜…)：供 MMR 多样性菜系维度打散。
    val cookingMethodNames: List<String> = emptyList(), // [AI生成] 做法(红烧/清蒸…)：供 MMR 做法维度打散。
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
    val recommendIngredientIds: Set<Long> = emptySet(), // [AI生成] 调养推荐：含则加分(利于健康档案的菜靠前)
    val labels: List<String> = emptyList(), // 粗约束标签，喂给模型(如 "忌高嘌呤","低钠")
)

/**
 * 规则筛过的安全候选菜（可做 + 不犯忌）。[AI生成]
 *
 * 方案A：可做性=所有非调料食材在手（调料默认常备）。主料/辅料名仅用于展示；
 * 在手调料/香辛料的丰富度用于给模型做“做法”建议（有葱姜蒜酱→红烧/爆炒，只有盐油→清蒸/白灼）。
 */
data class DishCandidate(
    val id: Long,
    val name: String,
    val mainNames: List<String>, // 主料(展示)
    val secondaryNames: List<String>, // 辅料(展示)
    val seasoningsOnHand: List<String>, // 在手的调料/香辛料 → 做法建议依据
    val limitHits: List<String>, // 命中“限量”的食材(保留但提示)
    val recommendHits: List<String>, // [AI生成] 命中调养“推荐”的食材(利于健康，加分+展示)
    val isRecent: Boolean, // 最近吃过(去重降权)
    val score: Double, // 规则打分，降序排列
    val shortageNames: List<String> = emptyList(), // [AI生成] 库存不足的食材名(份数用尽)：非空则本菜排后并标"库存不足"
    val missingNames: List<String> = emptyList(), // [AI生成] 缺的非调料食材名(主料/辅料不在库)：列出让用户知道少什么、可自行采购
    val onHandNames: List<String> = emptyList(), // [AI生成] 用到你库存的非调料食材名(物尽其用高亮)
    val avoidNames: List<String> = emptyList(), // [AI生成] 命中健康档案"忌口(avoid)"的食材名(仅非调料)：不再隐藏，改为排到最后并标红警示
    val cookingCautions: List<String> = emptyList(), // [AI生成] 调料命中忌口/限量→转做法提示(少盐/少糖/少油)，不判菜品忌口
    val recentDaysAgo: Int? = null, // [AI生成] B2：去重窗口内吃过则=距今天数(0今天/1昨天…)，非空→排最后并标注；null=窗口内没吃过
    val frequent: Boolean = false, // [AI生成] 3b：偏好画像高(常做/收藏)，供逐菜"推荐理由"展示
    val complementary: Boolean = false, // [AI生成] 3b：能补近期缺的宏量(营养互补)，供"推荐理由"展示
    val cuisine: String = "", // [AI生成] 菜系：供 MMR 菜系维度打散(避免一批全川菜)。
    val cookingMethodNames: List<String> = emptyList(), // [AI生成] 做法：供 MMR 做法维度打散(避免一批全红烧)。
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

/** 推荐结果来源：模型 / 规则兜底 / 无候选。[AI生成] */
enum class RecommendationSource { MODEL, RULE_FALLBACK, EMPTY }

/** 推荐最终结果（建议 + 规则候选 + 来源）。[AI生成] */
data class RecommendationResult(
    val suggestions: List<MealSuggestion>,
    val candidates: List<DishCandidate>,
    val source: RecommendationSource,
)
