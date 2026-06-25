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
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var deletingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var categoryManageOpen by remember { mutableStateOf(false) } // [AI生成] 分类管理弹框开关。
    var categoryEditOpen by remember { mutableStateOf(false) } // [AI生成] 新增/编辑分类弹框开关。
    var categoryEditTarget by remember { mutableStateOf<FoodCategory?>(null) }
    var categoryDeleteTarget by remember { mutableStateOf<FoodCategory?>(null) }
    var categoryNameDraft by remember { mutableStateOf("") }
    var categoryParentIdDraft by remember { mutableStateOf<Long?>(null) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) } // [AI修改] 食材详情统一通过底部弹层展示，首页和菜品选择共用。
    var selectedMenuOpen by remember { mutableStateOf(false) } // [AI生成] 底部“已选 X 项”跟随弹框开关。
    var filterOpen by remember { mutableStateOf(false) } // [AI生成] 食材筛选弹框开关。
    var dishMatchOpen by remember { mutableStateOf(false) } // [AI生成] 按已选食材找菜结果弹框。

    /**
     * 外部排除列表变化时刷新可选食材。[AI修改]
     */
    LaunchedEffect(excludeIngredientIds) {
        vm.configure(excludeIngredientIds)
    }
    LaunchedEffect(Unit) {
        vm.selectMainTab(ui.mainTab, force = true)
    } // [AI修改] 食材页和菜品选择食材统一启用同一套顶部主分类。
    LaunchedEffect(selectedIngredient?.id) {
        selectedIngredient?.let { vm.loadIngredientDetail(it) }
    }
    LaunchedEffect(ui.ingredients, ui.selectedIngredients, selectedIngredient?.id) {
        val currentId = selectedIngredient?.id ?: return@LaunchedEffect
        val refreshed = (ui.ingredients + ui.selectedIngredients).firstOrNull { it.id == currentId }
        selectedIngredient = refreshed ?: selectedIngredient
    } // [AI生成] 食材编辑保存后同步当前详情弹层中的名称、二级名称和图片，避免继续显示旧对象。
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
                        TextButton(onClick = { filterOpen = true }) {
                            Text(if (ui.filterCategoryIds.isEmpty()) "筛选" else "筛选(${ui.filterCategoryIds.size})")
                        }
                        if (!selectionMode) {
                            IconButton(
                                onClick = {
                                    vm.clearCreateError()
                                    vm.loadIngredientEditor(null)
                                    createDialogOpen = true
                                },
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = "添加食材")
                            }
                        }
                    },
                )
                ScrollableTabRow(
                    selectedTabIndex = IngredientMainTab.values().indexOf(ui.mainTab),
                    containerColor = MaterialTheme.colorScheme.surface,
                    edgePadding = 0.dp,
                ) {
                    IngredientMainTab.values().forEach { tab ->
                        Tab(
                            selected = ui.mainTab == tab,
                            onClick = {
                                selectedIngredient = null
                                vm.selectMainTab(tab)
                            },
                            text = {
                                Text(
                                    tab.label,
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            },
                            modifier = Modifier.widthIn(min = 72.dp),
                        )
                    }
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // 左侧分类树
                    if (ui.mainTab != IngredientMainTab.RECENT) {
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
                                    onClick = {
                                        selectedIngredient = null
                                        vm.selectAll()
                                    },
                                )
                            }
                            if (ui.mainTab == IngredientMainTab.CUSTOM) {
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
                            }
                            items(ui.tree, key = { "${it.category.id}-${it.level}" }) { node ->
                                CategoryItem(
                                    label = node.category.name,
                                    level = node.level,
                                    expanded = node.expanded,
                                    hasChildren = node.category.hasChildren,
                                    selected = ui.selectedCategoryId == node.category.id,
                                    onClick = {
                                        selectedIngredient = null
                                        if (node.category.hasChildren) {
                                            vm.toggleExpand(node)
                                        } else {
                                            vm.selectCategory(node)
                                        }
                                    },
                                )
                            }
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
                                onClick = {
                                    selectedIngredient = ing // [AI修改] 点击食材统一先打开详情，是否加入已选由详情顶部按钮决定。
                                },
                            )
                        }
                    }
                }
                // 底部固定栏
                if (selectionMode && ui.selectedIds.isNotEmpty()) {
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
                            Box(
                                modifier = Modifier
                                    .weight(1f),
                            ) {
                                Text(
                                    "已选 ${ui.selectedIds.size} 项",
                                    modifier = Modifier
                                        .clickable { selectedMenuOpen = true }, // [AI生成] 点击已选数量打开左下跟随弹框。
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                DropdownMenu(
                                    expanded = selectedMenuOpen,
                                    onDismissRequest = { selectedMenuOpen = false },
                                ) {
                                    ui.selectedIngredients.forEach { ingredient ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    ingredient.displayNameText(),
                                                    maxLines = 1,
                                                )
                                            },
                                            onClick = {
                                                selectedMenuOpen = false
                                                selectedIngredient = ingredient // [AI生成] 从已选弹框点击食材时打开同一个详情弹层。
                                            },
                                        )
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = {
                                    vm.clearCreateError()
                                    vm.loadIngredientEditor(null)
                                    createDialogOpen = true
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.tertiary),
                            ) { Text("+ 添加食材") }
                            Spacer(Modifier.width(8.dp))
                            OutlinedButton(
                                onClick = {
                                    vm.findDishesBySelectedIngredients()
                                    dishMatchOpen = true
                                },
                                enabled = ui.selectedIds.isNotEmpty(),
                            ) { Text("找菜") }
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

    selectedIngredient?.let { ingredient ->
        IngredientDetailSheet(
            ingredient = ingredient,
            selectionMode = selectionMode,
            selected = ingredient.id in ui.selectedIds,
            loading = ui.detailLoading && ui.detailIngredientId == ingredient.id,
            categories = ui.detailCategories,
            detail = ui.detailInfo,
            careRules = ui.detailCareRules,
            dishMatches = ui.detailDishMatches,
            onDismiss = { selectedIngredient = null },
            onToggleSelection = {
                vm.toggleSelection(ingredient) // [AI修改] 详情页右侧按钮按当前状态执行选择/取消选择。
                selectedIngredient = null // [AI修改] 选择或取消选择后立即关闭详情弹层。
            },
            onEdit = if (!selectionMode) {
                {
                    vm.loadIngredientEditor(ingredient)
                    editingIngredient = ingredient
                }
            } else {
                null
            },
            onDelete = if (!selectionMode && ingredient.source == "user") {
                {
                    deletingIngredient = ingredient
                }
            } else {
                null
            },
        )
    }

    if (filterOpen) {
        IngredientFilterDialog(
            categories = ui.allCategories,
            selectedIds = ui.filterCategoryIds,
            onToggle = vm::toggleFilterCategory,
            onClear = vm::clearFilters,
            onDismiss = { filterOpen = false },
        )
    }

    if (dishMatchOpen) {
        DishMatchDialog(
            title = "可做菜品",
            matches = ui.filterDishMatches,
            onDismiss = { dishMatchOpen = false },
        )
    }

    if (createDialogOpen) {
        IngredientEditorDialog(
            ingredient = null,
            ui = ui,
            onDismiss = {
                createDialogOpen = false
                vm.clearCreateError()
            },
            onSave = vm::saveIngredientEditor,
        )
    }

    editingIngredient?.let { ingredient ->
        IngredientEditorDialog(
            ingredient = ingredient,
            ui = ui,
            onDismiss = {
                editingIngredient = null
                vm.clearCreateError()
            },
            onSave = vm::saveIngredientEditor,
        )
    }

    LaunchedEffect(ui.lastSavedIngredientId) {
        if (ui.lastSavedIngredientId != null) {
            createDialogOpen = false
            editingIngredient = null
        }
    }

    deletingIngredient?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { deletingIngredient = null },
            title = { Text("删除食材") },
            text = {
                Text(
                    "确定删除“${ingredient.displayNameText()}”吗？删除后，已关联菜品中的该食材也会被移除。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // [AI修改] 删除用户自建食材前必须二次确认，避免级联删除菜品食材关联造成误删。
                        vm.deleteIngredient(ingredient)
                        deletingIngredient = null
                        selectedIngredient = null
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
 * 完整食材新增/编辑弹层。[AI生成]
 *
 * 阶段 B 先以全屏 Dialog 承载完整表单，后续可平滑迁移为独立页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientEditorDialog(
    ingredient: Ingredient?,
    ui: IngredientPickerUiState,
    onDismiss: () -> Unit,
    onSave: (
        Ingredient?,
        String,
        String,
        String,
        String,
        Long?,
        List<Long>,
        IngredientDetail,
        List<IngredientCareRule>,
    ) -> Unit,
) {
    var name by remember(ingredient?.id) { mutableStateOf(ingredient?.name.orEmpty()) }
    var alias by remember(ingredient?.id) { mutableStateOf(ingredient?.alias.orEmpty()) }
    var images by remember(ingredient?.id) { mutableStateOf(decodeImagePaths(ingredient?.imagePath.orEmpty())) }
    var thumbnails by remember(ingredient?.id) { mutableStateOf(decodeImagePaths(ingredient?.thumbnailPath.orEmpty())) }
    var defaultUnitId by remember(ingredient?.id) { mutableStateOf(ingredient?.defaultUnitId) }
    var categoryIds by remember(ingredient?.id) { mutableStateOf<Set<Long>>(emptySet()) }
    var commonMethods by remember(ingredient?.id) { mutableStateOf("") }
    var prepTips by remember(ingredient?.id) { mutableStateOf("") }
    var eatingNotes by remember(ingredient?.id) { mutableStateOf("") }
    var storageTips by remember(ingredient?.id) { mutableStateOf("") }
    var healthNote by remember(ingredient?.id) { mutableStateOf("") }
    var careRules by remember(ingredient?.id) { mutableStateOf<List<IngredientCareRule>>(emptyList()) }

    LaunchedEffect(ingredient?.id, ui.editorLoading, ui.editorCategoryIds, ui.editorDetail, ui.editorCareRules) {
        if (ingredient == null || !ui.editorLoading) {
            categoryIds = ui.editorCategoryIds
            val detail = ui.editorDetail
            commonMethods = detail?.commonMethods.orEmpty()
            prepTips = detail?.prepTips.orEmpty()
            eatingNotes = detail?.eatingNotes.orEmpty()
            storageTips = detail?.storageTips.orEmpty()
            healthNote = detail?.healthNote.orEmpty()
            careRules = ui.editorCareRules
        }
    }

    Dialog(
        onDismissRequest = { if (!ui.creatingIngredient) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(if (ingredient == null) "添加食材" else "编辑食材", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss, enabled = !ui.creatingIngredient) {
                            Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    },
                    actions = {
                        Button(
                            onClick = {
                                onSave(
                                    ingredient,
                                    name,
                                    alias,
                                    encodeImagePaths(images),
                                    encodeImagePaths(thumbnails),
                                    defaultUnitId,
                                    categoryIds.toList(),
                                    IngredientDetail(
                                        ingredientId = ingredient?.id ?: 0L,
                                        commonMethods = commonMethods,
                                        prepTips = prepTips,
                                        eatingNotes = eatingNotes,
                                        storageTips = storageTips,
                                        healthNote = healthNote,
                                    ),
                                    careRules,
                                )
                            },
                            enabled = name.isNotBlank() && !ui.creatingIngredient && !ui.editorLoading,
                            modifier = Modifier.padding(end = 12.dp),
                        ) {
                            Text(if (ui.creatingIngredient) "保存中" else "保存")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )

                if (ui.editorLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    EditorSection("基础信息") {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("食材名称 *") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        OutlinedTextField(
                            value = alias,
                            onValueChange = { alias = it },
                            label = { Text("二级名称") }, // [AI修改] 食材展示规则调整为“食材名称(二级名称)”。
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                        )
                        UnitDropdown(
                            units = ui.availableUnits,
                            selectedUnitId = defaultUnitId,
                            onSelect = { defaultUnitId = it },
                        )
                        ImagePickerButton(
                            imagePaths = images,
                            thumbnailPaths = thumbnails,
                            onImagesChanged = { nextImages, nextThumbnails ->
                                images = nextImages
                                thumbnails = nextThumbnails
                            },
                            maxCount = 3,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    EditorSection("分类归属") {
                        CategoryMultiSelect(
                            title = "常规分类",
                            categories = ui.allCategories.filter { it.dimension == "general" && it.crowdTypeId == null },
                            selectedIds = categoryIds,
                            onToggle = { categoryIds = categoryIds.toggle(it) },
                        )
                        CategoryMultiSelect(
                            title = "营养标签",
                            categories = ui.allCategories.filter { it.dimension in nutritionDimensionsForEditor && !it.isNutritionGroupRoot() },
                            selectedIds = categoryIds,
                            onToggle = { categoryIds = categoryIds.toggle(it) },
                        )
                    }

                    CareRuleEditor(
                        categories = ui.allCategories.filter { (it.dimension == "crowd" || it.crowdTypeId != null) && !it.isCareGroupRoot() },
                        rules = careRules,
                        onRulesChange = { careRules = it },
                    )

                    EditorSection("详情说明") {
                        DetailTextField("常见做法", commonMethods) { commonMethods = it }
                        DetailTextField("处理建议", prepTips) { prepTips = it }
                        DetailTextField("食用注意", eatingNotes) { eatingNotes = it }
                        DetailTextField("保存建议", storageTips) { storageTips = it }
                        DetailTextField("健康说明", healthNote) { healthNote = it }
                    }

                    ui.createError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

/**
 * 编辑器分组容器。[AI生成]
 */
@Composable
private fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}

/**
 * 默认单位下拉选择。[AI生成]
 */
@Composable
private fun UnitDropdown(
    units: List<MeasurementUnit>,
    selectedUnitId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = units.firstOrNull { it.id == selectedUnitId }?.name
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName ?: "默认单位（可选）", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text("不设置") },
                onClick = {
                    onSelect(null)
                    expanded = false
                },
            )
            units.forEach { unit ->
                DropdownMenuItem(
                    text = { Text(unit.name) },
                    onClick = {
                        onSelect(unit.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * 分类多选区。[AI生成]
 */
@Composable
private fun CategoryMultiSelect(
    title: String,
    categories: List<FoodCategory>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
) {
    if (categories.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        categories.forEach { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggle(category.id) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = category.id in selectedIds, onCheckedChange = { onToggle(category.id) })
                Text(category.displayWithParentHint(), modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * 调养规则编辑区。[AI生成]
 */
@Composable
private fun CareRuleEditor(
    categories: List<FoodCategory>,
    rules: List<IngredientCareRule>,
    onRulesChange: (List<IngredientCareRule>) -> Unit,
) {
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var level by remember { mutableStateOf(AdviceLevel.RECOMMEND) }
    var reason by remember { mutableStateOf("") }

    EditorSection("调养建议") {
        CareCategoryDropdown(categories, selectedCategoryId) { selectedCategoryId = it }
        AdviceLevelDropdown(level) { level = it }
        OutlinedTextField(
            value = reason,
            onValueChange = { reason = it },
            label = { Text("原因说明") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
        )
        OutlinedButton(
            onClick = {
                val category = categories.firstOrNull { it.id == selectedCategoryId } ?: return@OutlinedButton
                val next = rules.filterNot { it.categoryId == category.id } + IngredientCareRule(
                    ingredientId = 0L,
                    categoryId = category.id,
                    categoryName = category.name,
                    adviceLevel = level,
                    reason = reason,
                    source = "user",
                )
                onRulesChange(next)
                selectedCategoryId = null
                reason = ""
            },
            enabled = selectedCategoryId != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("添加调养建议")
        }
        rules.forEach { rule ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${rule.categoryName.ifBlank { categories.firstOrNull { it.id == rule.categoryId }?.name.orEmpty() }} / ${rule.adviceLevel.label()}")
                    if (rule.reason.isNotBlank()) {
                        Text(rule.reason, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
                IconButton(onClick = { onRulesChange(rules.filterNot { it.categoryId == rule.categoryId }) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "删除调养建议")
                }
            }
        }
    }
}

@Composable
private fun CareCategoryDropdown(
    categories: List<FoodCategory>,
    selectedCategoryId: Long?,
    onSelect: (Long?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selectedName ?: "选择调养分类", modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.displayWithParentHint()) },
                    onClick = {
                        onSelect(category.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun AdviceLevelDropdown(
    selected: AdviceLevel,
    onSelect: (AdviceLevel) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(selected.label(), modifier = Modifier.weight(1f))
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            listOf(AdviceLevel.RECOMMEND, AdviceLevel.LIMIT, AdviceLevel.AVOID).forEach { level ->
                DropdownMenuItem(
                    text = { Text(level.label()) },
                    onClick = {
                        onSelect(level)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DetailTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    )
}

/**
 * 食材基础详情底部弹层。[AI修改]
 *
 * 首页食材页和菜品选择食材共用同一套详情展示；只有选择模式才显示右侧“选择”按钮。
 */
@Composable
private fun IngredientDetailSheet(
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
                            DetailLine(
                                label = "相关菜品",
                                value = dishMatches.joinToString("\n") { match ->
                                    "${match.dish.name}：命中 ${match.matchCount}/${match.totalIngredientCount}"
                                },
                            )
                        }

                        Divider()
                        Text("以上建议仅作为日常饮食记录参考。", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
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
 * 食材展示名称。[AI生成]
 */
private fun Ingredient.displayNameText(): String =
    if (alias.isBlank()) name else "$name($alias)"

/**
 * 食材筛选弹框。[AI生成]
 */
@Composable
private fun IngredientFilterDialog(
    categories: List<FoodCategory>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("筛选食材") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                CategoryMultiSelect(
                    title = "常规分类",
                    categories = categories.filter { it.dimension == "general" && it.crowdTypeId == null },
                    selectedIds = selectedIds,
                    onToggle = onToggle,
                )
                CategoryMultiSelect(
                    title = "营养标签",
                    categories = categories.filter { it.dimension in nutritionDimensionsForEditor && !it.isNutritionGroupRoot() },
                    selectedIds = selectedIds,
                    onToggle = onToggle,
                )
                CategoryMultiSelect(
                    title = "调养分类",
                    categories = categories.filter { (it.dimension == "crowd" || it.crowdTypeId != null) && !it.isCareGroupRoot() },
                    selectedIds = selectedIds,
                    onToggle = onToggle,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
        dismissButton = {
            TextButton(onClick = onClear, enabled = selectedIds.isNotEmpty()) { Text("清空") }
        },
    )
}

/**
 * 按食材找菜结果弹框。[AI生成]
 */
@Composable
private fun DishMatchDialog(
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

private fun DishIngredientMatch.matchLabel(): String =
    when {
        missingCount == 0 -> "食材齐全"
        missingCount == 1 -> "差 1 项食材"
        else -> "已匹配 $matchCount 项，差 $missingCount 项"
    }

/**
 * 分类在编辑器中的展示名。[AI生成]
 */
private fun FoodCategory.displayWithParentHint(): String =
    icon.takeIf { it.isNotBlank() }?.let { "$it $name" } ?: name

/**
 * 建议等级展示文案。[AI生成]
 */
private fun AdviceLevel.label(): String = when (this) {
    AdviceLevel.RECOMMEND -> "推荐"
    AdviceLevel.LIMIT -> "限量"
    AdviceLevel.AVOID -> "避免"
}

/**
 * 多选集合切换。[AI生成]
 */
private fun Set<Long>.toggle(id: Long): Set<Long> =
    if (id in this) this - id else this + id

private val nutritionDimensionsForEditor = setOf("nutrition", "gi", "purine", "sodium", "fat", "sugar") // [AI生成] 编辑器营养分类维度集合。

// [AI生成] 编辑器/筛选器隐藏营养聚合根，只保留可直接归属的标签。
private fun FoodCategory.isNutritionGroupRoot(): Boolean =
    parentId == null && dimension == "nutrition" && name == "健康饮食"

// [AI生成] 编辑器/筛选器隐藏调养聚合根，只保留具体病种、人群或建议节点。
private fun FoodCategory.isCareGroupRoot(): Boolean =
    parentId == null && dimension == "crowd" && name == "人群分类"

/**
 * 食材详情中的键值行。[AI生成]
 */
@Composable
private fun DetailLine(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
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
            .padding(horizontal = (12 + (level - 1).coerceAtMost(4) * 12).dp, vertical = 12.dp), // [AI修改] 多级分类按层级缩进，避免深层过度挤压。
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = if (level == 1) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
            color = fg,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.weight(1f),
        )
        if (hasChildren) {
            Text(if (expanded) "▾" else "▸", color = fg)
        }
    }
}
