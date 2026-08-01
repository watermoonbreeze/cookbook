package com.sxdbsm.cookbook.domain.autogen

import com.sxdbsm.cookbook.domain.NutritionGuess
import kotlinx.datetime.LocalDate

/**
 * @File : AutoGenModels.kt
 * @Time : 2026/08/01
 * @Author : SXD-AI
 * @Desc : 自动化基础能力层数据模型——中立语义输入 + 预览 + 结果
 * <p>
 * 定义 Semantic* 中立输入（AI/分享/手输统一产出）、*Preview 预览态（只读·上层渲染确认页）、
 * AutoGenResult 结果（准确计数）。与 AI 来源无关，任何上层自动化共用同一套入库。
 * <p>
 * [AI生成] 自动化基础能力层 Phase 1。
 **/

// ═══════════════════════════════════════════════════
// 中立语义输入（上层统一产出，不依赖任何特定来源）
// ═══════════════════════════════════════════════════

/** 中立食材语义输入。[AI生成] */
data class SemanticIngredient(
    val name: String,
    val quantity: Double? = null,   // null/0 → commit 时用 SeasoningDefaults 兜默认克数
    val unit: String? = null,       // 单位名(如"g""个")·null → 解析默认
    val isMain: Boolean = false,
)

/** 中立菜品语义输入。[AI生成] */
data class SemanticDish(
    val name: String,
    val ingredients: List<SemanticIngredient> = emptyList(), // 空 → 走菜名推演
    val cookingMethods: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: String = "",
    val specialNote: String = "",
    val eatenRatio: Double? = null, // 餐次维度回填用
    val source: String = "auto",    // "ai"/"link"/"user"·标来源
)

/** 中立餐次语义输入。[AI生成] */
data class SemanticMeal(
    val mealTypeCode: String? = null,  // "BREAKFAST"等·null→回退午餐
    val mealTime: String? = null,      // "HH:mm"·null→餐次默认时间
    val note: String = "",
    val dishes: List<SemanticDish> = emptyList(),
)

/** 中立天语义输入。[AI生成] */
data class SemanticDay(
    val date: String? = null,       // "yyyy-MM-dd"优先
    val dateOffset: Int = 0,        // 相对 today
    val meals: List<SemanticMeal> = emptyList(),
)

// ═══════════════════════════════════════════════════
// 预览态（只读·resolution 明确 Reuse/Create·杜绝计数错误 D5）
// ═══════════════════════════════════════════════════

/** 解析类型。[AI生成] */
enum class ResolveKind { REUSE, CREATE }

/** 新建食材的 care 标记。[AI生成] */
enum class CareFlag {
    /** 归一命中库内已有食材——继承其 care·不自动断言忌口 */
    INHERITED,
    /** 全新食材——care 留待人工复核·不自动断言忌口（健康红线） */
    PENDING_REVIEW,
}

/** 食材预览——明确 REUSE/CREATE 决策 + 完整派生数据。[AI生成] */
data class IngredientPreview(
    val inputName: String,
    val normalizedName: String,     // 别名归一后
    val resolution: ResolveKind,
    val existingId: Long?,          // REUSE 时非空
    val groupLabel: String?,        // classify 大类名·可空
    val categoryId: Long?,          // commit 用
    val nutrition: NutritionGuess,  // 复用现有类型·含 source(Match/Group/None)
    val quantity: Double,           // 已兜默认克数
    val unitId: Long?,              // 已解析(null→saveDish 回填 gramUnit)
    val careFlag: CareFlag,
)

/** 菜品预览。[AI生成] */
data class DishPreview(
    val inputName: String,
    val resolution: ResolveKind,
    val existingId: Long?,
    val ingredients: List<IngredientPreview>,
    val source: String,
    /** 由营养估算×克数汇总·全缺料→null(显"营养待完善"非"约0") */
    val estimatedKcal: Double?,
)

/** 餐次预览。[AI生成] */
data class MealPreview(
    val mealTypeId: Long,
    val mealTime: String,
    val note: String,
    val dishes: List<DishPreview>,
)

/** 天预览。[AI生成] */
data class DayPreview(
    val date: LocalDate,
    val meals: List<MealPreview>,
    val hasExisting: Boolean,
)

/** 完整预览结果（上层渲染确认页）。[AI生成] */
data class AutoGenPreview(
    val days: List<DayPreview>,
    val warnings: List<String>,
)

// ═══════════════════════════════════════════════════
// 入库结果（准确计数·杜绝 D5"简化处理"）
// ═══════════════════════════════════════════════════

/** 合并模式。[AI生成] */
enum class MergeMode { REPLACE, APPEND, MERGE }

/** 自动生成入库结果。[AI生成] */
data class AutoGenResult(
    val daysSaved: Int,
    val mealsSaved: Int,
    val dishesCreated: Int,
    val dishesReused: Int,
    val ingredientsCreated: Int,
    val ingredientsReused: Int,
    val createdIngredientNames: List<String>,
    val createdDishNames: List<String>,
)
