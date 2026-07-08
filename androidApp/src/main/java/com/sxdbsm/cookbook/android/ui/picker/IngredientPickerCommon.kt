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

// ========== 食材维度集中定义（品类/营养/应季/调养） ==========
// [AI生成] 维度分类的判定与分组统一收敛在此，供 VM(营养 Tab 树)、详情属性区、后续筛选面板共用，
// 避免各处各写一份 dimension 判断（历史上 VM 与详情各有一份且不一致）。

/** 营养族维度（低GI/嘌呤/钠/脂/糖等）。[AI生成] */
internal val NUTRITION_DIMENSIONS = setOf("nutrition", "gi", "purine", "sodium", "fat", "sugar")

/** 应季维度。[AI生成] */
internal const val SEASON_DIMENSION = "season"

/** 营养 Tab 左树覆盖的维度集（营养族 + 应季）。[AI生成] */
internal val NUTRITION_TAB_DIMENSIONS = NUTRITION_DIMENSIONS + SEASON_DIMENSION

/** 食材分类按维度分组（品类/营养/应季）。[AI生成] */
internal data class DimensionGroups(
    val pinlei: List<FoodCategory>,
    val nutrition: List<FoodCategory>,
    val season: List<FoodCategory>,
) {
    val isEmpty: Boolean get() = pinlei.isEmpty() && nutrition.isEmpty() && season.isEmpty()
}

/** 把某食材的分类拆成品类/营养/应季三组（调养走 care rule，不在此）。[AI生成] */
internal fun List<FoodCategory>.groupByDimension(): DimensionGroups = DimensionGroups(
    pinlei = filter { it.dimension == "general" },
    nutrition = filter { it.dimension in NUTRITION_DIMENSIONS },
    season = filter { it.dimension == SEASON_DIMENSION },
)

/**
 * 维度分行展示（品类/营养/应季）通用组件——详情属性区用，将来筛选面板可复用。[AI生成]
 */
@Composable
internal fun DimensionRows(groups: DimensionGroups) {
    if (groups.pinlei.isNotEmpty()) DetailLine("品类", groups.pinlei.joinToString("、") { it.name })
    if (groups.nutrition.isNotEmpty()) DetailLine("营养", groups.nutrition.joinToString("·") { it.name })
    if (groups.season.isNotEmpty()) DetailLine("应季", groups.season.joinToString("、") { it.name })
}

