package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient

// [AI生成] 食材选择器共享扩展函数（显示名/分类提示/调养等级/可编辑判定等）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 食材展示名称。[AI生成]
 */
internal fun Ingredient.displayNameText(): String =
    if (alias.isBlank()) name else "$name($alias)"


/**
 * 分类在编辑器中的展示名。[AI生成]
 */
internal fun FoodCategory.displayWithParentHint(): String =
    icon.takeIf { it.isNotBlank() }?.let { "$it $name" } ?: name


/**
 * 建议等级展示文案。[AI生成]
 */
internal fun AdviceLevel.label(): String = when (this) {
    AdviceLevel.RECOMMEND -> "推荐"
    AdviceLevel.LIMIT -> "限量"
    AdviceLevel.AVOID -> "避免"
}


/**
 * 多选集合切换。[AI生成]
 */
internal fun Set<Long>.toggle(id: Long): Set<Long> =
    if (id in this) this - id else this + id

// [AI生成] 编辑器/筛选器隐藏调养聚合根，只保留具体病种、人群或建议节点。

internal fun FoodCategory.isCareGroupRoot(): Boolean =
    parentId == null && dimension == "crowd" && name == "人群分类"


/**
 * 当前分类是否属于方案 A 的可维护范围。[AI生成]
 */
internal fun FoodCategory.isEditableUserGeneralCategory(): Boolean =
    source == "user" && dimension == "general" && crowdTypeId == null

