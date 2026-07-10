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
    enabledCareCategoryIds: Set<Long> = emptySet(), // [AI生成] 用户健康档案病种，忌口区置顶高亮。
    onDismiss: () -> Unit,
    onToggleSelection: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    inPantry: Boolean = false, // [AI生成] 当前食材是否已在库存。
    onTogglePantry: (() -> Unit)? = null, // [AI生成] 出库(移出库存)，仅管理模式提供。
    pantryRemaining: Int = 0, // [AI生成] 库存剩余份数(份数-今天及过去占用)。
    pantryServing: Int = 0, // [AI生成] 库存总份数(用户提供)。
    onAddServings: ((Int) -> Unit)? = null, // [AI生成] 入库/加份数(累加)。
    onSetServings: ((Int) -> Unit)? = null, // [AI生成] 设置份数(减份数用)。
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

                        // ① 🍳 做法：常见做法 + 相关菜品（按烹饪方式分组）
                        val methodsText = detail?.commonMethods?.takeIf { it.isNotBlank() }
                        if (methodsText != null || dishMatches.isNotEmpty()) {
                            SectionTitle("🍳 做法")
                            if (methodsText != null) DetailLine("常见做法", methodsText)
                            if (dishMatches.isNotEmpty()) {
                                // [AI修改] 相关菜品按烹饪方式分组：方式为一级标题、菜品为二级条目；多方式菜品每组都出现，无方式归"其他"。
                                val groupedMatches = linkedMapOf<String, MutableList<DishIngredientMatch>>()
                                dishMatches.forEach { match ->
                                    match.dish.cookingMethodNames
                                        .ifEmpty { listOfNotNull(match.dish.cookingMethodName) }
                                        .filter { it.isNotBlank() }
                                        .ifEmpty { listOf("其他") }
                                        .forEach { method -> groupedMatches.getOrPut(method) { mutableListOf() }.add(match) }
                                }
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("相关菜品", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    groupedMatches.forEach { (method, matches) ->
                                        Text(method, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                        matches.forEach { match ->
                                            Text("　${match.dish.name}：命中 ${match.matchCount}/${match.totalIngredientCount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }
                        }

                        // ② 🩺 忌口/宜忌：调养建议（用户健康档案病种置顶高亮）+ 当前病种建议
                        val sortedCare = careRules.sortedByDescending { it.categoryId in enabledCareCategoryIds }
                        if (careRules.isNotEmpty() || ingredient.adviceLevel != null) {
                            SectionTitle("🩺 忌口 / 宜忌")
                            ingredient.adviceLevel?.let { level ->
                                val lv = when (level) {
                                    AdviceLevel.RECOMMEND -> "推荐"; AdviceLevel.LIMIT -> "限量"; AdviceLevel.AVOID -> "避免"
                                }
                                DetailLine("慢病建议", lv + ingredient.adviceReason.takeIf { it.isNotBlank() }?.let { "，$it" }.orEmpty())
                            }
                            sortedCare.forEach { rule ->
                                CareRuleLine(rule, mine = rule.categoryId in enabledCareCategoryIds)
                            }
                        }

                        // ③ 🥗 属性：品类 / 营养 / 应季（按维度分组，统一走 groupByDimension/DimensionRows）
                        val dimensionGroups = categories.groupByDimension()
                        if (!dimensionGroups.isEmpty) {
                            SectionTitle("🥗 属性")
                            DimensionRows(dimensionGroups)
                        }

                        // ④ 📋 处理与保存 + 来源
                        detail?.let { info ->
                            if (listOf(info.prepTips, info.eatingNotes, info.storageTips, info.healthNote).any { it.isNotBlank() }) {
                                SectionTitle("📋 处理与保存")
                                if (info.prepTips.isNotBlank()) DetailLine("处理建议", info.prepTips)
                                if (info.eatingNotes.isNotBlank()) DetailLine("食用注意", info.eatingNotes)
                                if (info.storageTips.isNotBlank()) DetailLine("保存建议", info.storageTips)
                                if (info.healthNote.isNotBlank()) DetailLine("健康说明", info.healthNote)
                            }
                        }
                        DetailLine(label = "来源", value = if (ingredient.source == "user") "用户创建" else "预设食材")
                        ingredient.defaultUnitName.takeIf { it.isNotBlank() }?.let { unit ->
                            DetailLine(label = "默认单位", value = unit)
                        }

                        Divider()
                        Text("以上建议仅作为日常饮食记录参考。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                    // [AI生成] 库存份数管理区：未入库→选份数入库；已入库→显示剩余份数+加/减+出库。
                    if (!selectionMode && onAddServings != null) {
                        Divider()
                        PantryServingSection(
                            ingredientId = ingredient.id,
                            inPantry = inPantry,
                            remaining = pantryRemaining,
                            serving = pantryServing,
                            onAddServings = onAddServings,
                            onSetServings = onSetServings,
                            onRemove = onTogglePantry,
                        )
                    }
                    if (!selectionMode && (onEdit != null || onDelete != null)) {
                        Divider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
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
 * 库存份数管理区。[AI生成]
 *
 * 未入库：选份数(默认1)后「入库」；已入库：显示剩余份数 + 加/减份数 + 出库。
 * 份数=可做几次菜；剩余=份数-今天及过去占用(0仍在库、可继续加)。
 */
@Composable
private fun PantryServingSection(
    ingredientId: Long,
    inPantry: Boolean,
    remaining: Int,
    serving: Int,
    onAddServings: (Int) -> Unit,
    onSetServings: ((Int) -> Unit)?,
    onRemove: (() -> Unit)?,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (inPantry) {
            Text(
                buildString {
                    append("库存剩余 $remaining 份")
                    if (serving != remaining) append("（共 $serving 份）")
                },
                style = MaterialTheme.typography.titleSmall,
                color = if (remaining == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { onSetServings?.invoke((serving - 1).coerceAtLeast(0)) }, enabled = onSetServings != null && serving > 0) { Text("－") }
                OutlinedButton(onClick = { onAddServings(1) }) { Text("＋ 加1份") }
                Spacer(Modifier.weight(1f))
                onRemove?.let { TextButton(onClick = it) { Text("出库", color = MaterialTheme.colorScheme.error) } }
            }
        } else {
            var addCount by remember(ingredientId) { mutableStateOf(1) }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("入库份数", style = MaterialTheme.typography.bodyMedium)
                OutlinedButton(onClick = { if (addCount > 1) addCount-- }, enabled = addCount > 1) { Text("－") }
                Text("$addCount", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { if (addCount < 99) addCount++ }) { Text("＋") }
                Spacer(Modifier.weight(1f))
                Button(onClick = { onAddServings(addCount) }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("入库")
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

/**
 * 详情四区小标题。[AI生成]
 */
@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 4.dp),
    )
}

/**
 * 忌口/调养建议行；命中用户健康档案病种时置顶高亮。[AI生成]
 */
@Composable
private fun CareRuleLine(rule: IngredientCareRule, mine: Boolean) {
    val text = "${rule.categoryName}：${rule.adviceLevel.label()}" +
        rule.reason.takeIf { it.isNotBlank() }?.let { "，$it" }.orEmpty()
    if (mine) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "【我的】$text",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
    } else {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

