package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
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
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.domain.model.FoodCategory
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
    asDialog: Boolean = true,
    selectionMode: Boolean = true,
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
    var categoryManageOpen by remember { mutableStateOf(false) } // [AI生成] 分类管理弹框开关。
    var categoryEditOpen by remember { mutableStateOf(false) } // [AI生成] 新增/编辑分类弹框开关。
    var categoryEditTarget by remember { mutableStateOf<FoodCategory?>(null) }
    var categoryDeleteTarget by remember { mutableStateOf<FoodCategory?>(null) }
    var categoryNameDraft by remember { mutableStateOf("") }
    var categoryParentIdDraft by remember { mutableStateOf<Long?>(null) }

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
    val content: @Composable () -> Unit = {
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!selectionMode) {
                                Text("食材", fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.width(12.dp))
                            }
                            AppSearchField(
                                value = ui.keyword,
                                onValueChange = vm::setKeyword,
                                placeholder = "搜索食材...",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    },
                    navigationIcon = {
                        if (selectionMode) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                            }
                        }
                    },
                    actions = {
                        if (!selectionMode) {
                            IconButton(
                                onClick = {
                                    vm.clearCreateError()
                                    createDialogOpen = true
                                },
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = "添加食材")
                            }
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
                        item {
                            CategoryItem(
                                label = "最近使用",
                                level = 1,
                                expanded = false,
                                hasChildren = false,
                                selected = ui.selectedCategoryId == -2L,
                                onClick = { vm.selectRecentlyUsed() },
                            )
                        }
                        item {
                            TextButton(
                                onClick = { categoryManageOpen = true },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("管理分类")
                            }
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
                                selected = selectionMode && ing.id in ui.selectedIds,
                                onClick = {
                                    if (selectionMode) {
                                        vm.toggleSelection(ing)
                                    } else {
                                        editingIngredient = ing
                                        editIngredientName = ing.name
                                        editIngredientAlias = ing.alias
                                        editIngredientImages = decodeImagePaths(ing.imagePath)
                                        editIngredientThumbnails = decodeImagePaths(ing.thumbnailPath)
                                    }
                                },
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
                if (selectionMode) {
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
    }
    if (asDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            content()
        }
    } else {
        content()
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
                        categories = ui.allCategories.filter { it.dimension == "general" && it.crowdTypeId == null },
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

    if (categoryManageOpen) {
        CategoryManageDialog(
            categories = ui.allCategories,
            onDismiss = { categoryManageOpen = false },
            onAdd = {
                categoryManageOpen = false
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
                categoryEditOpen = true
            },
            onEdit = { category ->
                categoryManageOpen = false
                categoryEditTarget = category
                categoryNameDraft = category.name
                categoryParentIdDraft = category.parentId
                categoryEditOpen = true
            },
            onDelete = {
                categoryManageOpen = false
                categoryDeleteTarget = it
            },
        )
    }

    if (categoryEditOpen) {
        val editingCategory = categoryEditTarget
        CategoryEditDialog(
            editingCategory = editingCategory,
            categories = ui.allCategories,
            name = categoryNameDraft,
            parentId = categoryParentIdDraft,
            onNameChange = { categoryNameDraft = it },
            onParentChange = { categoryParentIdDraft = it },
            onDismiss = {
                categoryEditOpen = false
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
            },
            onSave = {
                if (editingCategory == null) {
                    vm.createCategory(categoryNameDraft, categoryParentIdDraft)
                } else {
                    vm.renameCategory(editingCategory, categoryNameDraft)
                }
                categoryEditOpen = false
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
            },
        )
    }

    categoryDeleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryDeleteTarget = null },
            title = { Text("删除分类") },
            text = {
                Text("确定删除“${category.name}”吗？分类会被软删除，已绑定的食材只解除分类关系，食材本身不会删除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCategory(category)
                        categoryDeleteTarget = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryDeleteTarget = null }) { Text("取消") }
            },
        )
    }
}

/**
 * 食材分类管理弹框。[AI生成]
 *
 * 方案 A 只允许用户自建的普通分类被编辑/删除，预设分类在这里作为只读参考显示。
 */
@Composable
private fun CategoryManageDialog(
    categories: List<FoodCategory>,
    onDismiss: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (FoodCategory) -> Unit,
    onDelete: (FoodCategory) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("管理分类") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                categories.forEach { category ->
                    val editable = category.isEditableUserGeneralCategory()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (editable) 0.72f else 0.34f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${if (category.parentId == null) "" else "  "}${category.icon.ifBlank { "□" }} ${category.name}",
                            modifier = Modifier.weight(1f),
                            color = if (editable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (editable) {
                            IconButton(onClick = { onEdit(category) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Edit, contentDescription = "编辑分类")
                            }
                            IconButton(onClick = { onDelete(category) }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Delete, contentDescription = "删除分类")
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onAdd) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("新增分类")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

/**
 * 新增/编辑食材分类弹框。[AI生成]
 */
@Composable
private fun CategoryEditDialog(
    editingCategory: FoodCategory?,
    categories: List<FoodCategory>,
    name: String,
    parentId: Long?,
    onNameChange: (String) -> Unit,
    onParentChange: (Long?) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    val topGeneralCategories = categories.filter {
        it.parentId == null && it.dimension == "general" && it.crowdTypeId == null
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingCategory == null) "新增分类" else "编辑分类") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("分类名称") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                if (editingCategory == null) {
                    CategoryParentDropdown(
                        categories = topGeneralCategories,
                        selectedParentId = parentId,
                        onSelect = onParentChange,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

/**
 * 新增分类时选择父级分类。[AI生成]
 */
@Composable
private fun CategoryParentDropdown(
    categories: List<FoodCategory>,
    selectedParentId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedParentId }?.name
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName ?: "一级分类", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("一级分类") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
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
 * 新建食材时选择分类。[AI生成]
 */
@Composable
private fun CategoryDropdown(
    categories: List<FoodCategory>,
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
 * 当前分类是否属于方案 A 的可维护范围。[AI生成]
 */
private fun FoodCategory.isEditableUserGeneralCategory(): Boolean =
    source == "user" && dimension == "general" && crowdTypeId == null

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
