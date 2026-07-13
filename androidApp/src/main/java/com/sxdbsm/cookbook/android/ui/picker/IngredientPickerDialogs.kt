package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.domain.model.DishIngredientMatch
import com.sxdbsm.cookbook.domain.model.Ingredient

// [AI生成] 食材相关弹框（回收站/按食材找菜结果）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 失效食材回收站弹框。[AI生成]
 *
 * 列出失效（用户删除/后台下架）的自定义食材，可恢复为有效或彻底删除。
 * 彻底删除会一并清除该食材在菜品中的引用，操作前二次确认。
 */
@Composable
internal fun InactiveIngredientsDialog(
    ingredients: List<Ingredient>,
    onRestore: (Ingredient) -> Unit,
    onHardDelete: (Ingredient) -> Unit,
    onDismiss: () -> Unit,
) {
    var pendingHardDelete by remember { mutableStateOf<Ingredient?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("已失效食材") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                if (ingredients.isEmpty()) {
                    Text("暂无已失效的自定义食材", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    ingredients.forEach { ing ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (ing.alias.isBlank()) ing.name else "${ing.name}（${ing.alias}）",
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                if (ing.reason.isNotBlank()) {
                                    Text(ing.reason, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            TextButton(onClick = { onRestore(ing) }) { Text("恢复") }
                            TextButton(onClick = { pendingHardDelete = ing }) {
                                Text("彻底删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                        Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )

    pendingHardDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingHardDelete = null },
            title = { Text("彻底删除食材") },
            text = { Text("将永久删除「${target.name}」，并清除它在所有菜品中的引用，无法恢复。确定继续？") },
            confirmButton = {
                TextButton(onClick = {
                    onHardDelete(target)
                    pendingHardDelete = null
                }) { Text("彻底删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingHardDelete = null }) { Text("取消") } },
        )
    }
}


/**
 * 按食材找菜结果弹框。[AI生成]
 */
@Composable
internal fun DishMatchDialog(
    title: String,
    matches: List<DishIngredientMatch>,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // [AI生成] 说明"找菜"逻辑：按勾选食材反查用到它们的菜品，并标注齐全/差几项。
                Text(
                    "根据你勾选的食材，找出用到这些食材的菜品：命中越多的排在越前，并标注「食材齐全」或「还差几项」，帮你决定用现有食材做什么。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (matches.isEmpty()) {
                    Text("暂时没有匹配菜品", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    matches.forEach { match ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f))
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(match.dish.name, fontWeight = FontWeight.SemiBold)
                            Text(match.matchLabel(), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}


internal fun DishIngredientMatch.matchLabel(): String =
    when {
        missingCount == 0 -> "食材齐全"
        missingCount == 1 -> "差 1 项食材"
        else -> "已匹配 $matchCount 项，差 $missingCount 项"
    }

