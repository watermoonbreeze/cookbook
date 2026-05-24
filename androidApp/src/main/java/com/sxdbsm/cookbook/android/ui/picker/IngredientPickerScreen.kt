package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Search
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
import com.sxdbsm.cookbook.android.ui.component.placeholderBg
import com.sxdbsm.cookbook.android.ui.component.placeholderFg
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.Ingredient
import org.koin.androidx.compose.koinViewModel

/**
 * 食材选择 - 全屏 Compose Dialog。
 * 左侧手风琴分类树 + 右侧食材网格 + 顶部搜索 + 底部完成。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerScreen(
    excludeIngredientIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (List<Ingredient>) -> Unit,
    vm: IngredientPickerViewModel = koinViewModel(),
) {
    val ui by vm.state.collectAsStateWithLifecycle()
    var createDialogOpen by remember { mutableStateOf(false) }
    var newIngredientName by remember { mutableStateOf("") }
    var newIngredientAlias by remember { mutableStateOf("") }

    LaunchedEffect(excludeIngredientIds) {
        vm.configure(excludeIngredientIds)
    }
    LaunchedEffect(ui.lastCreatedIngredientId) {
        if (ui.lastCreatedIngredientId != null && createDialogOpen) {
            createDialogOpen = false
            newIngredientName = ""
            newIngredientAlias = ""
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        OutlinedTextField(
                            value = ui.keyword,
                            onValueChange = vm::setKeyword,
                            placeholder = { Text("搜索食材...") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // 左侧分类树
                    LazyColumn(
                        modifier = Modifier
                            .width(120.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        item {
                            CategoryItem(
                                label = "全部",
                                level = 1,
                                expanded = false,
                                hasChildren = false,
                                selected = ui.selectedCategoryId == -1L,
                                onClick = { vm.selectAll() },
                            )
                        }
                        items(ui.tree, key = { "${it.category.id}-${it.level}" }) { node ->
                            CategoryItem(
                                label = node.category.name,
                                level = node.level,
                                expanded = node.expanded,
                                hasChildren = node.category.hasChildren,
                                selected = ui.selectedCategoryId == node.category.id,
                                onClick = {
                                    if (node.level == 1 && node.category.hasChildren) {
                                        vm.toggleExpand(node)
                                    } else {
                                        vm.selectCategory(node)
                                    }
                                },
                            )
                        }
                    }
                    // 右侧食材网格
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        items(ui.ingredients, key = { it.id }) { ing ->
                            IngredientCell(
                                ingredient = ing,
                                selected = ing.id in ui.selectedIds,
                                onClick = { vm.toggleSelection(ing) },
                            )
                        }
                    }
                }
                // 底部固定栏
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "已选 ${ui.selectedIds.size} 项",
                            modifier = Modifier.weight(1f),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                vm.clearCreateError()
                                createDialogOpen = true
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                        ) { Text("+ 添加食材") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onConfirm(vm.confirmSelected())
                                onDismiss()
                            },
                            enabled = ui.selectedIds.isNotEmpty(),
                        ) { Text("完成") }
                    }
                }
            }
        }
    }

    if (createDialogOpen) {
        AlertDialog(
            onDismissRequest = {
                if (!ui.creatingIngredient) {
                    createDialogOpen = false
                    newIngredientName = ""
                    newIngredientAlias = ""
                    vm.clearCreateError()
                }
            },
            title = { Text("添加食材") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newIngredientName,
                        onValueChange = {
                            newIngredientName = it
                            vm.clearCreateError()
                        },
                        label = { Text("食材名称") },
                        singleLine = true,
                        enabled = !ui.creatingIngredient,
                    )
                    OutlinedTextField(
                        value = newIngredientAlias,
                        onValueChange = { newIngredientAlias = it },
                        label = { Text("别名（可选）") },
                        singleLine = true,
                        enabled = !ui.creatingIngredient,
                    )
                    ui.createError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.createUserIngredient(newIngredientName, newIngredientAlias)
                    },
                    enabled = newIngredientName.isNotBlank() && !ui.creatingIngredient,
                ) {
                    Text(if (ui.creatingIngredient) "添加中" else "添加")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        createDialogOpen = false
                        newIngredientName = ""
                        newIngredientAlias = ""
                        vm.clearCreateError()
                    },
                    enabled = !ui.creatingIngredient,
                ) { Text("取消") }
            },
        )
    }
}

@Composable
private fun CategoryItem(
    label: String,
    level: Int,
    expanded: Boolean,
    hasChildren: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = if (level == 1) 12.dp else 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (level == 1) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (level == 1 && hasChildren) {
            Text(if (expanded) "▾" else "▸", color = fg)
        }
    }
}

@Composable
private fun IngredientCell(
    ingredient: Ingredient,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ext = ExtendedColorsHolder.current
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface

    OutlinedCard(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.outlinedCardColors(containerColor = bg),
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
    ) {
        Box(modifier = Modifier.padding(8.dp)) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(placeholderBg(ingredient.id)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        ingredient.name.take(1),
                        color = placeholderFg(ingredient.id),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    ingredient.name,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
            // 人群分类时的角标
            ingredient.adviceLevel?.let { level ->
                val (color, label) = when (level) {
                    AdviceLevel.RECOMMEND -> ext.success to "✓"
                    AdviceLevel.LIMIT -> ext.warning to "⚠"
                    AdviceLevel.AVOID -> ext.danger to "✕"
                }
                Surface(
                    color = color,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.align(Alignment.TopEnd),
                ) {
                    Text(
                        label,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}
