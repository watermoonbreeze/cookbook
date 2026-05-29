package com.sxdbsm.cookbook.android.ui.newdish

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.FormFieldLabel
import com.sxdbsm.cookbook.android.ui.component.ImagePickerButton
import com.sxdbsm.cookbook.android.ui.component.TagChip
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerScreen
import org.koin.androidx.compose.koinViewModel

/**
 * 新建/编辑菜品页面。[AI修改]
 *
 * 通过 `editingDishId` 进入编辑模式，通过 `importDishId` 进入复制导入模式。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewDishScreen(
    editingDishId: Long? = null,
    importDishId: Long? = null,
    onBack: () -> Unit,
    vm: NewDishViewModel = koinViewModel(),
) {
    // [AI修改] 表单状态来自 ViewModel，局部弹窗开关用 remember 存在当前 Composable 内。
    val state by vm.state.collectAsStateWithLifecycle()
    var tagInputOpen by remember { mutableStateOf(false) }
    var newTagText by remember { mutableStateOf("") }
    var importPickerOpen by remember { mutableStateOf(false) }
    var ingredientPickerOpen by remember { mutableStateOf(false) }
    var cookingMethodDialogOpen by remember { mutableStateOf(false) }
    var cookingMethodDraft by remember { mutableStateOf("") }

    /**
     * 进入编辑页时按 id 加载原菜品。[AI修改]
     */
    LaunchedEffect(editingDishId) {
        if (editingDishId != null && editingDishId > 0 && state.editingId != editingDishId) {
            vm.loadForEdit(editingDishId)
        }
    }
    /**
     * 从其它菜品复制导入时加载源菜品。[AI修改]
     */
    LaunchedEffect(importDishId) {
        if (importDishId != null && importDishId > 0) {
            vm.importFromDishId(importDishId)
        }
    }
    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            TopAppBar(
                title = { Text(if (state.editingId != null) "编辑菜品" else "新建菜品", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.secondary,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                ),
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
            FormFieldLabel("菜名 *")
            OutlinedTextField(
                value = state.name,
                onValueChange = vm::setName,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
            )

            FormFieldLabel("标签")
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

            FormFieldLabel("烹饪方式")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.cookingMethodInput.isNotBlank()) {
                    TagChip(state.cookingMethodInput)
                    IconButton(
                        onClick = { vm.clearCookingMethod() },
                        modifier = Modifier.size(20.dp),
                    ) {
                        Icon(Icons.Outlined.Close, contentDescription = "清空烹饪方式", modifier = Modifier.size(14.dp))
                    }
                }
                AssistChip(
                    onClick = {
                        cookingMethodDraft = state.cookingMethodInput
                        cookingMethodDialogOpen = true
                    },
                    label = { Text(if (state.cookingMethodInput.isBlank()) "+ 添加" else "修改") },
                )
            }

            FormFieldLabel("食材清单")
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface), // [AI修改] 表单卡片按新规范使用白底内容卡片。
            ) {
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
                                // [AI修改] 新增/编辑页食材列表不展示“主料”标识，保留内部数据用于排序或后续业务。
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

            FormFieldLabel("特殊说明")
            OutlinedTextField(
                value = state.specialNote,
                onValueChange = vm::setSpecialNote,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("如：少盐") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
            )

            FormFieldLabel("描述")
            OutlinedTextField(
                value = state.description,
                onValueChange = vm::setDescription,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("（可选，做法/心得）") },
                minLines = 2,
                shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
            )

            FormFieldLabel("图片")
            ImagePickerButton(
                imagePaths = decodeImagePaths(state.imagePath),
                onImagesChanged = { vm.setImagePath(encodeImagePaths(it)) },
                maxCount = 3,
                modifier = Modifier.fillMaxWidth(),
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
                    shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
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

    if (cookingMethodDialogOpen) {
        CookingMethodDialog(
            value = cookingMethodDraft,
            options = state.availableCookingMethods,
            onValueChange = { cookingMethodDraft = it },
            onSelect = { method ->
                vm.selectCookingMethod(method)
                cookingMethodDraft = method.name
                cookingMethodDialogOpen = false
            },
            onConfirm = {
                vm.setCookingMethodInput(cookingMethodDraft.trim())
                cookingMethodDialogOpen = false
            },
            onDismiss = { cookingMethodDialogOpen = false },
        )
    }
}

/**
 * 烹饪方式弹框。[AI生成]
 *
 * 当前数据库只支持一个 `cooking_method_id`，所以弹框提供“Spinner 下拉选择一个或手动输入一个”。
 */
@Composable
private fun CookingMethodDialog(
    value: String,
    options: List<com.sxdbsm.cookbook.domain.model.CookingMethod>,
    onValueChange: (String) -> Unit,
    onSelect: (com.sxdbsm.cookbook.domain.model.CookingMethod) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("烹饪方式") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = { expanded = true },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(if (value.isBlank()) "下拉选择烹饪方式" else value, modifier = Modifier.weight(1f))
                    Icon(Icons.Outlined.ExpandMore, contentDescription = null)
                }
                Box {
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        options.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.name) },
                                onClick = {
                                    onSelect(method)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    label = { Text("或手动输入") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = value.isNotBlank()) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/**
 * 简化版 FlowRow。[AI修改]
 *
 * 当前 Compose 版本没有直接使用官方 FlowRow，这里用自定义布局承载标签换行。
 */
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
