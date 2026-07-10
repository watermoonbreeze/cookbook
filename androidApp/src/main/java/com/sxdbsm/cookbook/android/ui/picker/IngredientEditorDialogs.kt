package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sxdbsm.cookbook.android.ui.component.ImagePickerButton
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import com.sxdbsm.cookbook.domain.model.IngredientCareRule
import com.sxdbsm.cookbook.domain.model.IngredientDetail
import com.sxdbsm.cookbook.domain.model.MeasurementUnit

// [AI生成] 食材新增/编辑表单弹层及其子控件（分类选择器/调养规则/单位下拉等）
// 由 IngredientPickerScreen.kt 拆分而来（阶段1界面重构），保持同包同行为，不改逻辑。

/**
 * 完整食材新增/编辑弹层。[AI生成]
 *
 * 阶段 B 先以全屏 Dialog 承载完整表单，后续可平滑迁移为独立页面。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun IngredientEditorDialog(
    ingredient: Ingredient?,
    ui: IngredientPickerUiState,
    onDismiss: () -> Unit,
    onAddCategory: () -> Unit,
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
    var categoryPickerOpen by remember { mutableStateOf(false) } // [AI生成] 自定义食材分类选择器开关。
    val isPreset = ingredient?.source == "preset"
    val editableCustomCategories = ui.allCategories.filter { it.isEditableUserGeneralCategory() }
    val selectedCategoryNames = editableCustomCategories
        .filter { it.id in categoryIds }
        .joinToString("，") { it.name }

    LaunchedEffect(ingredient?.id, ui.editorLoading, ui.editorCategoryIds, ui.editorDetail, ui.editorCareRules) {
        if (ingredient == null || !ui.editorLoading) {
            categoryIds = if (ingredient?.source == "preset") emptySet() else ui.editorCategoryIds.filter { id ->
                ui.allCategories.firstOrNull { it.id == id }?.isEditableUserGeneralCategory() == true
            }.toSet()
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
                            onValueChange = { if (!isPreset) name = it },
                            label = { Text("食材名称 *") },
                            singleLine = true,
                            enabled = !isPreset,
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
                        if (!isPreset) {
                            UnitDropdown(
                                units = ui.availableUnits,
                                selectedUnitId = defaultUnitId,
                                onSelect = { defaultUnitId = it },
                            )
                        }
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

                    if (!isPreset) {
                        EditorSection("分类归属（可选）") {
                            Text(
                                // [AI修改] 分类改为可选：不选也能保存，在「自定义-全部」中查看。
                                selectedCategoryNames.ifBlank { "未选择分类（可不选，保存后在「全部」查看）" },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            OutlinedButton(onClick = { categoryPickerOpen = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("选择分类")
                            }
                        }

                        EditorSection("详情说明") {
                            DetailTextField("常见做法", commonMethods) { commonMethods = it }
                            DetailTextField("处理建议", prepTips) { prepTips = it }
                            DetailTextField("食用注意", eatingNotes) { eatingNotes = it }
                            DetailTextField("保存建议", storageTips) { storageTips = it }
                            DetailTextField("健康说明", healthNote) { healthNote = it }
                        }

                        // [AI修改] 恢复"食材界面改造2"重构时丢失的调养建议编辑区：自定义食材可编辑所有内容（含调养规则）。
                        CareRuleEditor(
                            categories = ui.allCategories.filter { (it.dimension == "crowd" || it.crowdTypeId != null) && !it.isCareGroupRoot() },
                            rules = careRules,
                            onRulesChange = { careRules = it },
                        )
                    }

                    ui.createError?.let { error ->
                        Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (categoryPickerOpen) {
        IngredientCategoryPickerDialog(
            categories = editableCustomCategories,
            selectedIds = categoryIds,
            onToggle = { categoryIds = categoryIds.toggle(it) },
            onAddCategory = {
                categoryPickerOpen = false
                onAddCategory()
            },
            onDismiss = { categoryPickerOpen = false },
        )
    }
}


/**
 * 编辑器分组容器。[AI生成]
 */
@Composable
internal fun EditorSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        content()
    }
}


/**
 * 默认单位下拉选择。[AI生成]
 */
@Composable
internal fun UnitDropdown(
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
 * 自定义食材分类选择器。[AI生成]
 */
@Composable
internal fun IngredientCategoryPickerDialog(
    categories: List<FoodCategory>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    onAddCategory: () -> Unit,
    onDismiss: () -> Unit,
) {
    var expandedIds by remember { mutableStateOf<Set<Long>>(emptySet()) }
    val rows = remember(categories, expandedIds) {
        buildCategoryPickerRows(categories, expandedIds)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("分类选择", modifier = Modifier.weight(1f))
                IconButton(onClick = onAddCategory) {
                    Icon(Icons.Outlined.Add, contentDescription = "新增分类")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (categories.isEmpty()) {
                    Text("暂无自定义分类，请先点击右上角 + 创建分类。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    rows.forEach { node ->
                        val hasChildren = categories.any { it.parentId == node.category.id }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onToggle(node.category.id) }
                                .padding(start = (8 + (node.level - 1) * 16).dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = node.category.id in selectedIds,
                                onCheckedChange = { onToggle(node.category.id) },
                            )
                            Text(node.category.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            if (hasChildren) {
                                IconButton(
                                    onClick = {
                                        expandedIds = if (node.category.id in expandedIds) {
                                            expandedIds - node.category.id
                                        } else {
                                            expandedIds + node.category.id
                                        }
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Text(if (node.category.id in expandedIds) "▾" else "▸")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
}


internal fun buildCategoryPickerRows(categories: List<FoodCategory>, expandedIds: Set<Long>): List<CategoryNode> {
    val result = mutableListOf<CategoryNode>()
    fun append(parentId: Long?, level: Int) {
        categories
            .filter { it.parentId == parentId }
            .sortedWith(compareBy<FoodCategory> { it.sortOrder }.thenBy { it.id })
            .forEach { category ->
                result += CategoryNode(category = category, level = level, expanded = category.id in expandedIds)
                if (category.id in expandedIds) append(category.id, level + 1)
            }
    }
    append(null, 1)
    return result
}


/**
 * 调养规则编辑区。[AI修改] 自"食材界面改造1"版本恢复，供自定义食材编辑调养建议。
 */
@Composable
internal fun CareRuleEditor(
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
internal fun CareCategoryDropdown(
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
internal fun AdviceLevelDropdown(
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
internal fun DetailTextField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
    )
}

