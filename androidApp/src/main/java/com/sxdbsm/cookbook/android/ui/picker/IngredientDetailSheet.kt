package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.android.ui.component.StoredImage
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.DishIngredientMatch
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail

// [AI生成] 食材详情底部弹层（详情四区展示 + 底部操作按钮）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 食材基础详情底部弹层。[AI修改]
 *
 * 首页食材页和菜品选择食材共用同一套详情展示；只有选择模式才显示右侧“选择”按钮。
 */
@Composable
internal fun IngredientDetailSheet(
    ingredient: Ingredient,
    selectionMode: Boolean,
    selected: Boolean,
    loading: Boolean,
    categories: List<FoodCategory>,
    detail: IngredientDetail?,
    careRules: List<IngredientCareRule>,
    dishMatches: List<DishIngredientMatch>,
    onDismiss: () -> Unit,
    onToggleSelection: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    inPantry: Boolean = false, // [AI生成] 当前食材是否已在库存。
    onTogglePantry: (() -> Unit)? = null, // [AI生成] 加入/移出库存，仅管理模式提供。
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.35f)
                    .clickable { onDismiss() }, // [AI修改] 底部详情弹层外部空白支持点击关闭。
            )
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.65f),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp,
            ) {
                Column(Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("关闭")
                        }
                        Spacer(Modifier.weight(1f))
                        if (selectionMode) {
                            Button(
                                onClick = onToggleSelection,
                                colors = if (selected) {
                                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                } else {
                                    ButtonDefaults.buttonColors()
                                },
                            ) {
                                Text(if (selected) "取消选择" else "选择")
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        if (loading) {
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StoredImage(
                                imagePath = ingredient.imagePath,
                                thumbnailPath = ingredient.thumbnailPath,
                                fallbackText = ingredient.name.take(1),
                                fallbackEmoji = ingredient.emoji.ifBlank { "🥗" },
                                seedId = ingredient.id,
                                size = 64.dp,
                                corner = 12.dp,
                            ) // [AI修改] 图片预览入口统一放在详情弹层内。
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    ingredient.displayNameText(), // [AI修改] 食材展示名称按“名称(别名)”规则拼接。
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                if (ingredient.alias.isNotBlank()) {
                                    Text(
                                        "二级名称：${ingredient.alias}", // [AI修改] 食材详情文案同步新命名。
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }

                        DetailLine(label = "来源", value = if (ingredient.source == "user") "用户创建" else "预设食材")
                        ingredient.defaultUnitName.takeIf { it.isNotBlank() }?.let { unit ->
                            DetailLine(label = "默认单位", value = unit)
                        }
                        if (categories.isNotEmpty()) {
                            DetailLine(label = "分类", value = categories.joinToString("、") { it.name })
                        }
                        ingredient.adviceLevel?.let { level ->
                            val label = when (level) {
                                com.sxdbsm.cookbook.domain.model.AdviceLevel.RECOMMEND -> "推荐"
                                com.sxdbsm.cookbook.domain.model.AdviceLevel.LIMIT -> "限量"
                                com.sxdbsm.cookbook.domain.model.AdviceLevel.AVOID -> "避免"
                            }
                            DetailLine(label = "慢病建议", value = label)
                        }
                        if (ingredient.adviceReason.isNotBlank()) {
                            DetailLine(label = "建议原因", value = ingredient.adviceReason)
                        }
                        if (careRules.isNotEmpty()) {
                            DetailLine(
                                label = "调养建议",
                                value = careRules.joinToString("\n") { rule ->
                                    "${rule.categoryName}: ${rule.adviceLevel.label()}${rule.reason.takeIf { it.isNotBlank() }?.let { "，$it" }.orEmpty()}"
                                },
                            )
                        }
                        detail?.let { info ->
                            if (info.commonMethods.isNotBlank()) DetailLine("常见做法", info.commonMethods)
                            if (info.prepTips.isNotBlank()) DetailLine("处理建议", info.prepTips)
                            if (info.eatingNotes.isNotBlank()) DetailLine("食用注意", info.eatingNotes)
                            if (info.storageTips.isNotBlank()) DetailLine("保存建议", info.storageTips)
                            if (info.healthNote.isNotBlank()) DetailLine("健康说明", info.healthNote)
                        }
                        if (dishMatches.isNotEmpty()) {
                            // [AI修改] 相关菜品按烹饪方式分组：方式为一级标题、菜品为二级条目，默认平铺展开；
                            // 多烹饪方式的菜品在每个方式组下都出现，无烹饪方式的归入"其他"。
                            val groupedMatches = linkedMapOf<String, MutableList<DishIngredientMatch>>()
                            dishMatches.forEach { match ->
                                val methods = match.dish.cookingMethodNames
                                    .ifEmpty { listOfNotNull(match.dish.cookingMethodName) }
                                    .filter { it.isNotBlank() }
                                    .ifEmpty { listOf("其他") }
                                methods.forEach { method ->
                                    groupedMatches.getOrPut(method) { mutableListOf() }.add(match)
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("相关菜品", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                groupedMatches.forEach { (method, matches) ->
                                    Text(
                                        method,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    matches.forEach { match ->
                                        Text(
                                            "　${match.dish.name}：命中 ${match.matchCount}/${match.totalIngredientCount}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                    }
                                }
                            }
                        }

                        Divider()
                        Text("以上建议仅作为日常饮食记录参考。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    if (!selectionMode && (onEdit != null || onDelete != null || onTogglePantry != null)) {
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            onTogglePantry?.let { toggle ->
                                OutlinedButton(
                                    onClick = toggle,
                                    modifier = Modifier.weight(1f),
                                    colors = if (inPantry) {
                                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    } else {
                                        ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary)
                                    },
                                ) {
                                    Icon(
                                        if (inPantry) Icons.Outlined.Delete else Icons.Outlined.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (inPantry) "出库" else "入库")
                                }
                            }
                            onEdit?.let { edit ->
                                OutlinedButton(
                                    onClick = edit,
                                    modifier = Modifier.weight(1f),
                                ) {
                                    Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("编辑")
                                }
                            }
                            onDelete?.let { delete ->
                                OutlinedButton(
                                    onClick = delete,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                ) {
                                    Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("删除")
                                }
                            }
                        } // [AI生成] 管理模式下将编辑/删除固定在详情底部，替代原卡片长按菜单。
                    }
                }
            }
        }
    }
}


/**
 * 食材详情中的键值行。[AI生成]
 */
@Composable
internal fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

