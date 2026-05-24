package com.sxdbsm.cookbook.android.ui.newdish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.TagChip
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerScreen
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDishScreen(
    editingDishId: Long? = null,
    importDishId: Long? = null,
    onBack: () -> Unit,
    vm: NewDishViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var tagInputOpen by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var importPickerOpen by remember { mutableStateOf(false) }
    var ingredientPickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(editingDishId) {
        if (editingDishId != null && editingDishId > 0 && state.editingId != editingDishId) {
            vm.loadForEdit(editingDishId)
        }
    }
    LaunchedEffect(importDishId) {
        if (importDishId != null && importDishId > 0) {
            vm.importFromDishId(importDishId)
        }
    }
    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingId != null) "编辑菜品" else "新建菜品", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    OutlinedButton(
                        onClick = { importPickerOpen = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                    ) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("导入")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { vm.save() },
                        enabled = state.name.isNotBlank() && !state.saving,
                    ) { Text("保存") }
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            FieldLabel("菜名 *")
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::setName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            FieldLabel("标签")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.tags.forEach { tag ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TagChip(tag)
                        IconButton(
                            onClick = { vm.removeTag(tag) },
                            modifier = Modifier.size(20.dp),
                        ) {
                            Icon(Icons.Outlined.Close, contentDescription = "删除标签", modifier = Modifier.size(14.dp))
                        }
                    }
                }
                AssistChip(onClick = { tagInputOpen = true }, label = { Text("+ 添加") })
            }

            FieldLabel("烹饪方式")
            OutlinedTextField(
                value = state.cookingMethodName.orEmpty(),
                onValueChange = {},
                readOnly = true,
                placeholder = { Text("（可选）") },
                modifier = Modifier.fillMaxWidth(),
            )

            FieldLabel("食材清单")
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column {
                    if (state.ingredients.isEmpty()) {
                        Text(
                            "还没加食材",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.ingredients.forEach { ing ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(ing.ingredient.name, modifier = Modifier.weight(1f))
                                val qty = ing.quantity?.let { "$it ${ing.unitName}" } ?: "适量"
                                Text(qty, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.width(8.dp))
                                if (ing.isMain) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = MaterialTheme.shapes.extraSmall,
                                    ) {
                                        Text(
                                            "主料",
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        )
                                    }
                                }
                                IconButton(onClick = { vm.removeIngredient(ing.ingredient.id) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
            TextButton(
                onClick = { ingredientPickerOpen = true },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("添加食材", color = MaterialTheme.colorScheme.tertiary)
            }

            FieldLabel("特殊说明")
            OutlinedTextField(
                value = state.specialNote,
                onValueChange = vm::setSpecialNote,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：少盐") },
                singleLine = true,
            )

            FieldLabel("描述")
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("（可选，做法/心得）") },
                minLines = 2,
            )
            Spacer(Modifier.height(80.dp))
        }
    }

    if (importPickerOpen) {
        DishPickerScreen(
            title = "导入菜品",
            multiSelect = false,
            initialSelected = emptyList(),
            excludeDishIds = state.editingId?.let { setOf(it) } ?: emptySet(),
            showRecentChips = false,
            showAddNewButton = false,
            onDismiss = { importPickerOpen = false },
            onConfirm = { selected ->
                selected.firstOrNull()?.let { vm.importFromDishId(it.id) }
            },
        )
    }

    if (ingredientPickerOpen) {
        IngredientPickerScreen(
            excludeIngredientIds = state.ingredients.map { it.ingredient.id }.toSet(),
            onDismiss = { ingredientPickerOpen = false },
            onConfirm = { selected ->
                selected.forEach { vm.addIngredient(it) }
            },
        )
    }

    if (tagInputOpen) {
        AlertDialog(
            onDismissRequest = { tagInputOpen = false; newTagText = "" },
            title = { Text("添加标签") },
            text = {
                OutlinedTextField(
                    value = newTagText,
                    onValueChange = { newTagText = it },
                    singleLine = true,
                    label = { Text("标签名（如 家常 / 快手 / 少盐）") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addTag(newTagText.trim())
                    newTagText = ""
                    tagInputOpen = false
                }) { Text("添加") }
            },
            dismissButton = {
                TextButton(onClick = { tagInputOpen = false; newTagText = "" }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRow(
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    androidx.compose.foundation.layout.FlowRow(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
        content = { content() },
    )
}
