package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.ImagePickerButton
import com.sxdbsm.cookbook.android.ui.component.IngredientCard
import com.sxdbsm.cookbook.android.ui.component.StoredImage
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.DishIngredientMatch
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.domain.model.MeasurementUnit
import org.koin.androidx.compose.koinViewModel

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

