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
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.domain.model.Ingredient
import org.koin.androidx.compose.koinViewModel

/**
 * 食材选择 - 全屏 Compose Dialog。[AI修改]
 *
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
    // [AI修改] 选择状态统一来自 ViewModel；弹窗输入框内容使用 remember 保存临时值。
    val ui by vm.state.collectAsStateWithLifecycle()
    var createDialogOpen by remember { mutableStateOf(false) }
    var newIngredientName by remember { mutableStateOf("") }
    var newIngredientAlias by remember { mutableStateOf("") }
    var newIngredientImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var newIngredientThumbnails by remember { mutableStateOf<List<String>>(emptyList()) }
    var newIngredientCategoryId by remember { mutableStateOf<Long?>(null) }
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var deletingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var editIngredientName by remember { mutableStateOf("") }
    var editIngredientAlias by remember { mutableStateOf("") }
    var editIngredientImages by remember { mutableStateOf<List<String>>(emptyList()) }
    var editIngredientThumbnails by remember { mutableStateOf<List<String>>(emptyList()) }

    /**
     * 外部排除列表变化时刷新可选食材。[AI修改]
     */
    LaunchedEffect(excludeIngredientIds) {
        vm.configure(excludeIngredientIds)
    }
    /**
     * 自定义食材创建成功后关闭创建弹窗。[AI修改]
     */
    LaunchedEffect(ui.lastCreatedIngredientId) {
        if (ui.lastCreatedIngredientId != null && createDialogOpen) {
            createDialogOpen = false
            newIngredientName = ""
            newIngredientAlias = ""
            newIngredientImages = emptyList()
            newIngredientThumbnails = emptyList()
            newIngredientCategoryId = null
        }
    }
    ui.operationError?.let { error ->
        AlertDialog(
            onDismissRequest = { vm.clearOperationError() },
            title = { Text("操作失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { vm.clearOperationError() }) {
                    Text("知道了")
                }
            },
        )
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.secondary,
                        actionIconContentColor = MaterialTheme.colorScheme.secondary,
                    ), // [AI修改] 食材选择弹窗顶栏按暖杏规范使用背景一体化样式。
                    title = {
                        AppSearchField(
                            value = ui.keyword,
                            onValueChange = vm::setKeyword,
                            placeholder = "搜索食材...",
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
                            .background(MaterialTheme.colorScheme.secondaryContainer), // [AI修改] 左侧分类区使用规范中的分组栏底色。
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
                            IngredientCard(
                                ingredient = ing,
                                selected = ing.id in ui.selectedIds,
                                onClick = { vm.toggleSelection(ing) },
                                onEdit = {
                                    editingIngredient = ing
                                    editIngredientName = ing.name
                                    editIngredientAlias = ing.alias
                                    editIngredientImages = decodeImagePaths(ing.imagePath)
                                    editIngredientThumbnails = decodeImagePaths(ing.thumbnailPath)
                                },
                                onDelete = { deletingIngredient = ing },
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
                    newIngredientImages = emptyList()
                    newIngredientCategoryId = null
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
                        shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
                    )
                    OutlinedTextField(
                        value = newIngredientAlias,
                        onValueChange = { newIngredientAlias = it },
                        label = { Text("别名（可选）") },
                        singleLine = true,
                        enabled = !ui.creatingIngredient,
                        shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
                    )
                    CategoryDropdown(
                        categories = ui.tree.map { it.category },
                        selectedCategoryId = newIngredientCategoryId,
                        onSelect = { newIngredientCategoryId = it },
                        enabled = !ui.creatingIngredient,
                    )
                    ImagePickerButton(
                        imagePaths = newIngredientImages,
                        thumbnailPaths = newIngredientThumbnails,
                        onImagesChanged = { images, thumbnails ->
                            newIngredientImages = images
                            newIngredientThumbnails = thumbnails
                        },
                        maxCount = 3,
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
                        vm.createUserIngredient(
                            name = newIngredientName,
                            alias = newIngredientAlias,
                            imagePath = encodeImagePaths(newIngredientImages),
                            thumbnailPath = encodeImagePaths(newIngredientThumbnails),
                            categoryId = newIngredientCategoryId,
                        )
                    },
                    enabled = newIngredientName.isNotBlank() && newIngredientCategoryId != null && !ui.creatingIngredient,
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
                        newIngredientImages = emptyList()
                        newIngredientThumbnails = emptyList()
                        newIngredientCategoryId = null
                        vm.clearCreateError()
                    },
                    enabled = !ui.creatingIngredient,
                ) { Text("取消") }
            },
        )
    }

    editingIngredient?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { editingIngredient = null },
            title = { Text("编辑食材") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editIngredientName,
                        onValueChange = { editIngredientName = it },
                        label = { Text("食材名称") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
                    )
                    OutlinedTextField(
                        value = editIngredientAlias,
                        onValueChange = { editIngredientAlias = it },
                        label = { Text("别名（可选）") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
                    )
                    ImagePickerButton(
                        imagePaths = editIngredientImages,
                        thumbnailPaths = editIngredientThumbnails,
                        onImagesChanged = { images, thumbnails ->
                            editIngredientImages = images
                            editIngredientThumbnails = thumbnails
                        },
                        maxCount = 3,
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.updateIngredient(
                            ingredient = ingredient,
                            name = editIngredientName,
                            alias = editIngredientAlias,
                            imagePath = encodeImagePaths(editIngredientImages),
                            thumbnailPath = encodeImagePaths(editIngredientThumbnails),
                        )
                        editingIngredient = null
                    },
                    enabled = editIngredientName.isNotBlank(),
                ) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editingIngredient = null }) { Text("取消") }
            },
        )
    }

    deletingIngredient?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { deletingIngredient = null },
            title = { Text("删除食材") },
            text = {
                Text(
                    "确定删除“${ingredient.name}”吗？删除后，已关联菜品中的该食材也会被移除。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // [AI修改] 删除用户自建食材前必须二次确认，避免级联删除菜品食材关联造成误删。
                        vm.deleteIngredient(ingredient)
                        deletingIngredient = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingIngredient = null }) { Text("取消") }
            },
        )
    }
}

/**
 * 新建食材时选择分类。[AI生成]
 */
@Composable
private fun CategoryDropdown(
    categories: List<com.sxdbsm.cookbook.domain.model.FoodCategory>,
    selectedCategoryId: Long?,
    onSelect: (Long) -> Unit,
    enabled: Boolean,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name.orEmpty()
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (selectedName.isBlank()) "选择分类 *" else selectedName, modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * 左侧分类项。[AI修改]
 */
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
