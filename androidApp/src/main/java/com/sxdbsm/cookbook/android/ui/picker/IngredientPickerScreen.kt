package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.IngredientCard
import com.sxdbsm.cookbook.domain.model.AdviceLevel
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
    var dishMatchOpen by remember { mutableStateOf(false) } // [AI生成] 按已选食材找菜结果弹框。
    var recycleBinOpen by remember { mutableStateOf(false) } // [AI生成] 失效食材回收站弹框开关。

    /**
     * 外部排除列表变化时刷新可选食材。[AI修改]
     */
    LaunchedEffect(excludeIngredientIds) {
        vm.configure(excludeIngredientIds)
    }
    LaunchedEffect(Unit) {
        vm.selectMainTab(ui.mainTab, force = true)
    } // [AI修改] 食材页和菜品选择食材统一启用同一套顶部主分类。
    LaunchedEffect(selectedIngredient?.id, ui.lastSavedIngredientId) {
        selectedIngredient?.let { vm.loadIngredientDetail(it) }
    } // [AI修改] 编辑保存后（lastSavedIngredientId 变化）重新加载详情，修复详情弹层不实时刷新的问题。
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
                // [AI生成] 全局搜索下拉：有输入且有匹配时紧贴搜索框下方弹出，点结果跳到分类并高亮。
                if (ui.searchResults.isNotEmpty()) {
                    SearchResultsPanel(
                        results = ui.searchResults,
                        onPick = {
                            selectedIngredient = null
                            vm.jumpToIngredient(it)
                        },
                    )
                }
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
                    // 左侧分类树（最近/库存平铺，无左树）
                    if (ui.mainTab != IngredientMainTab.RECENT && ui.mainTab != IngredientMainTab.PANTRY) {
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
                                item {
                                    // [AI生成] 回收站入口：查看/恢复/彻底删除失效的自定义食材。
                                    TextButton(
                                        onClick = {
                                            vm.loadInactiveIngredients()
                                            recycleBinOpen = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("已失效")
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
                                        }
                                        vm.selectCategory(node) // [AI修改] 点击任意层级分类都立即筛选该分类及其子分类下的食材。
                                    },
                                )
                            }
                        }
                    }
                    // 右侧食材网格
                    // [AI修改] 调养 tab 选中病种分类后按 绿灯/黄灯/红灯 分组展示，剂量语义来自调养规则 advice_level。
                    val careGroups = if (ui.mainTab == IngredientMainTab.CARE && ui.selectedCategoryId != null) {
                        listOf(
                            AdviceLevel.RECOMMEND to "🟢 绿灯推荐",
                            AdviceLevel.LIMIT to "🟡 黄灯限量",
                            AdviceLevel.AVOID to "🔴 红灯禁忌",
                        ).mapNotNull { (level, title) ->
                            val group = ui.ingredients.filter { it.adviceLevel == level }
                            if (group.isEmpty()) null else title to group
                        } + listOfNotNull(
                            ui.ingredients.filter { it.adviceLevel == null }
                                .takeIf { it.isNotEmpty() }
                                ?.let { "其他" to it },
                        )
                    } else {
                        emptyList()
                    }
                    val gridState = rememberLazyGridState()
                    // [AI生成] 搜索跳转后滚动到高亮食材（跳转固定在常规 Tab、无 careGroups 头，index 即列表位置）。
                    LaunchedEffect(ui.highlightIngredientId, ui.ingredients) {
                        val hid = ui.highlightIngredientId ?: return@LaunchedEffect
                        val idx = ui.ingredients.indexOfFirst { it.id == hid }
                        if (idx >= 0) gridState.animateScrollToItem(idx)
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        if (careGroups.isNotEmpty()) {
                            careGroups.forEach { (title, group) ->
                                item(key = "care-header-$title", span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                items(group, key = { it.id }) { ing ->
                                    IngredientCard(
                                        ingredient = ing,
                                        selected = ing.id in ui.selectedIds,
                                        highlighted = ing.id == ui.highlightIngredientId,
                                        onClick = { selectedIngredient = ing },
                                    )
                                }
                            }
                        } else {
                            items(ui.ingredients, key = { it.id }) { ing ->
                                IngredientCard(
                                    ingredient = ing,
                                    selected = ing.id in ui.selectedIds,
                                    highlighted = ing.id == ui.highlightIngredientId,
                                    onClick = {
                                        selectedIngredient = ing // [AI修改] 点击食材统一先打开详情，是否加入已选由详情顶部按钮决定。
                                    },
                                )
                            }
                        }
                        if (ui.canLoadMoreIngredients) {
                            item {
                                OutlinedButton(
                                    onClick = vm::loadMoreIngredients,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("加载更多")
                                }
                            }
                        }
                    }
                }
                // [AI修改] 库存 Tab 下方小提示：入库入口统一在食材详情，这里只作说明，不再放添加按钮。
                if (!selectionMode && ui.mainTab == IngredientMainTab.PANTRY) {
                    Text(
                        "从现有食材中添加入库",
                        style = MaterialTheme.typography.labelSmall, // [AI生成] 小两号提示字号。
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                    )
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
            enabledCareCategoryIds = ui.enabledCareCategoryIds,
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
            inPantry = ingredient.id in ui.pantryIngredientIds,
            onTogglePantry = if (!selectionMode) {
                {
                    if (ingredient.id in ui.pantryIngredientIds) vm.removeFromPantry(ingredient) else vm.addToPantry(ingredient)
                }
            } else {
                null
            },
        )
    }

    if (dishMatchOpen) {
        DishMatchDialog(
            title = "可做菜品",
            matches = ui.filterDishMatches,
            onDismiss = { dishMatchOpen = false },
        )
    }

    if (recycleBinOpen) {
        InactiveIngredientsDialog(
            ingredients = ui.inactiveIngredients,
            onRestore = { vm.restoreIngredient(it) },
            onHardDelete = { vm.hardDeleteIngredient(it) },
            onDismiss = { recycleBinOpen = false },
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
            onAddCategory = {
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
                categoryEditOpen = true
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
            onAddCategory = {
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
                categoryEditOpen = true
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
                    "确定删除“${ingredient.displayNameText()}”吗？删除后该食材将标记为失效并从食材列表隐藏；已关联菜品中仍会保留并灰显，不影响原有菜品。可在“已失效”中恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // [AI修改] 删除=软失效保留：置失效并从列表隐藏，菜品引用灰显保留，可在“已失效”恢复。
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
            categories = ui.allCategories.filter { it.isEditableUserGeneralCategory() },
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
 * 全局搜索结果下拉面板。[AI生成]
 *
 * 紧贴搜索框下方弹出，跨全库匹配的食材列表；点某项跳到其所属分类并高亮。
 */
@Composable
private fun SearchResultsPanel(
    results: List<Ingredient>,
    onPick: (Ingredient) -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
            items(results, key = { it.id }) { ing ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(ing) }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ing.emoji.ifBlank { "🥗" }, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(12.dp))
                    Text(
                        if (ing.alias.isBlank()) ing.name else "${ing.name}(${ing.alias})",
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        if (ing.source == "user") "自定义" else "预设",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Divider()
            }
        }
    }
}

