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
@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun IngredientEditorDialog(
    ingredient: Ingredient?,
    ui: IngredientPickerUiState,
    onDismiss: () -> Unit,
    initialName: String = "", // [AI生成] 新建时预填名称(搜索无结果直达新建用)
    onAddCategory: () -> Unit,
    onAddUnit: (String, (Long?) -> Unit) -> Unit, // [AI生成] 单位库：手填新单位入库并回选。
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
        com.sxdbsm.cookbook.domain.model.IngredientNutrition?,
        String, // [AI生成] A1：营养大类(FoodGroup.Group 名，空=未选)
    ) -> Unit,
) {
    var name by remember(ingredient?.id) { mutableStateOf(ingredient?.name ?: initialName) } // [AI修改] 新建时可预填名称
    var alias by remember(ingredient?.id) { mutableStateOf(ingredient?.alias.orEmpty()) }
    var images by remember(ingredient?.id) { mutableStateOf(decodeImagePaths(ingredient?.imagePath.orEmpty())) }
    var thumbnails by remember(ingredient?.id) { mutableStateOf(decodeImagePaths(ingredient?.thumbnailPath.orEmpty())) }
    var defaultUnitId by remember(ingredient?.id) { mutableStateOf(ingredient?.defaultUnitId) }
    var categoryIds by remember(ingredient?.id) { mutableStateOf<Set<Long>>(emptySet()) }
    // [AI生成] A1：营养大类(必选，默认按名/已有分类预选)——决定归到主食/鱼肉蛋等分类树 + 色系/均衡统计。
    var selectedGroup by remember(ingredient?.id) { mutableStateOf<com.sxdbsm.cookbook.domain.FoodGroup.Group?>(null) }
    var groupTouched by remember(ingredient?.id) { mutableStateOf(false) }
    var commonMethods by remember(ingredient?.id) { mutableStateOf("") }
    var prepTips by remember(ingredient?.id) { mutableStateOf("") }
    var eatingNotes by remember(ingredient?.id) { mutableStateOf("") }
    var storageTips by remember(ingredient?.id) { mutableStateOf("") }
    var healthNote by remember(ingredient?.id) { mutableStateOf("") }
    var careRules by remember(ingredient?.id) { mutableStateOf<List<IngredientCareRule>>(emptyList()) }
    var categoryPickerOpen by remember { mutableStateOf(false) } // [AI生成] 自定义食材分类选择器开关。
    // [AI生成] Item4：自定义食材营养素(每100g)录入，预填已有；营养色系开时给"影响哪些统计"提示。
    fun fmtNum(v: Double?): String = v?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
    var nKcal by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.energyKcal)) }
    var nProtein by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.proteinG)) }
    var nFat by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.fatG)) }
    var nCarb by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.carbG)) }
    var nFiber by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.fiberG)) }
    var nSodium by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.sodiumMg)) }
    var nPotassium by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.potassiumMg)) }
    var nCalcium by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.calciumMg)) }
    var nGi by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.gi)) }
    var nPurine by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.purineMg)) }
    var nPiece by remember(ingredient?.id, ui.editorNutrition) { mutableStateOf(fmtNum(ui.editorNutrition?.pieceGram)) }
    val prefs = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
    // [AI修改] 营养录入提示：营养色系或热量数值任一开启即提示(两者都吃这份营养数据)。
    val nutritionColorFlag by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_COLOR_ENABLED, false)
    }.collectAsState(false)
    val calorieNumberFlag by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.CALORIE_NUMBER_ENABLED, false)
    }.collectAsState(false)
    val nutritionColorOn = nutritionColorFlag || calorieNumberFlag
    fun buildNutrition() = com.sxdbsm.cookbook.domain.model.IngredientNutrition(
        ingredientId = ingredient?.id ?: 0L,
        energyKcal = nKcal.toDoubleOrNull(), proteinG = nProtein.toDoubleOrNull(), fatG = nFat.toDoubleOrNull(),
        carbG = nCarb.toDoubleOrNull(), fiberG = nFiber.toDoubleOrNull(), sodiumMg = nSodium.toDoubleOrNull(),
        potassiumMg = nPotassium.toDoubleOrNull(), calciumMg = nCalcium.toDoubleOrNull(),
        gi = nGi.toDoubleOrNull(), purineMg = nPurine.toDoubleOrNull(), pieceGram = nPiece.toDoubleOrNull(),
    )
    val isPreset = ingredient?.source == "preset"
    // [AI生成] A1：营养大类可选项(9个)——distinct 分类名 + 代表 Group + 该顶层分类 id(存在才可选)。
    val groupOptions = remember(ui.allCategories) {
        com.sxdbsm.cookbook.domain.FoodGroup.CATEGORY_NAME.entries.distinctBy { it.value }.mapNotNull { e ->
            ui.allCategories.firstOrNull { it.parentId == null && it.name == e.value }?.let { Triple(e.key, e.value, it.id) }
        }
    }
    // 预选：编辑现有食材优先用其已挂的顶层大类；否则按名猜(未手动改过时随名跟随)。
    LaunchedEffect(name, ui.editorCategoryIds, groupOptions, ui.editorLoading) {
        if (ingredient != null && ui.editorLoading) return@LaunchedEffect
        if (groupTouched) return@LaunchedEffect
        val existing = groupOptions.firstOrNull { (_, _, catId) -> catId in ui.editorCategoryIds }?.first
        selectedGroup = existing ?: com.sxdbsm.cookbook.domain.FoodGroup.classify(name)
    }
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
                                    buildNutrition(), // [AI生成] Item4：自定义营养(空则VM侧不写)
                                    selectedGroup?.name ?: "", // [AI生成] A1：营养大类
                                )
                            },
                            // [AI修改] A1：营养大类必选(预设无此要求,预设不显该区)。
                            enabled = name.isNotBlank() && (isPreset || selectedGroup != null) && !ui.creatingIngredient && !ui.editorLoading,
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
                                onAddUnit = { newName -> onAddUnit(newName) { id -> if (id != null) defaultUnitId = id } },
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
                        // [AI生成] A1：营养大类(必选)——归到主食/鱼肉蛋等分类树，并让色系/均衡按它统计。默认按名预选。
                        EditorSection("营养大类（必选）") {
                            Text(
                                "决定这个食材归到「主食/蔬菜/鱼肉蛋…」哪一类——用于分类浏览和营养均衡统计。已按名字自动选好，可改。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            androidx.compose.foundation.layout.FlowRow(
                                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                            ) {
                                groupOptions.forEach { (group, catName, _) ->
                                    val sel = selectedGroup?.let { com.sxdbsm.cookbook.domain.FoodGroup.CATEGORY_NAME[it] } == catName
                                    androidx.compose.material3.FilterChip(
                                        selected = sel,
                                        onClick = { selectedGroup = group; groupTouched = true },
                                        label = { Text(catName) },
                                    )
                                }
                            }
                            if (selectedGroup == null) {
                                Text("请选择一个营养大类", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        EditorSection("其它分类（可选）") {
                            Text(
                                // [AI修改] 分类改为可选：不选也能保存，在「自定义-全部」中查看。
                                selectedCategoryNames.ifBlank { "未选择其它分类（营养维度/自建分类等，可不选）" },
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

                        // [AI生成] Item4：营养素录入(每100g，选填)——填了这些，自定义食材就能像预设一样进营养/热量统计。
                        EditorSection("营养素（每100g，选填）") {
                            if (nutritionColorOn) {
                                Text(
                                    "填了这些值，这个食材就会计入统计：热量→每日千卡与达标；蛋白/脂肪/碳水→宏量均衡；" +
                                        "选好上方分类→搭配多样性；钠/GI/嘌呤→高血压/糖尿病/痛风指标。不填也能用，随时可补。",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.height(4.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("热量kcal", nKcal, Modifier.weight(1f)) { nKcal = it }
                                NutrientField("蛋白g", nProtein, Modifier.weight(1f)) { nProtein = it }
                                NutrientField("脂肪g", nFat, Modifier.weight(1f)) { nFat = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("碳水g", nCarb, Modifier.weight(1f)) { nCarb = it }
                                NutrientField("纤维g", nFiber, Modifier.weight(1f)) { nFiber = it }
                                NutrientField("钠mg", nSodium, Modifier.weight(1f)) { nSodium = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("钾mg", nPotassium, Modifier.weight(1f)) { nPotassium = it }
                                NutrientField("钙mg", nCalcium, Modifier.weight(1f)) { nCalcium = it }
                                NutrientField("GI", nGi, Modifier.weight(1f)) { nGi = it }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                NutrientField("嘌呤mg", nPurine, Modifier.weight(1f)) { nPurine = it }
                                NutrientField("单件克重", nPiece, Modifier.weight(1f)) { nPiece = it }
                                Spacer(Modifier.weight(1f))
                            }
                            Text(
                                "单件克重：按「个/根/片」等计件单位买时，一件约多少克(如1个鸡蛋≈50)，用于把用量折算成克。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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
    onAddUnit: (String) -> Unit = {}, // [AI生成] 单位库：手填新单位入库并回选。
) {
    var expanded by remember { mutableStateOf(false) }
    var addingCustom by remember { mutableStateOf(false) }
    var customText by remember { mutableStateOf("") }
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
            // [AI生成] 单位库：填新单位，保存到单位库(source=user)后即选中、下次可复用。
            DropdownMenuItem(
                text = { Text("＋ 自定义单位…", color = MaterialTheme.colorScheme.primary) },
                onClick = {
                    addingCustom = true
                    expanded = false
                },
            )
        }
    }
    if (addingCustom) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { addingCustom = false; customText = "" },
            title = { Text("自定义单位") },
            text = {
                androidx.compose.material3.OutlinedTextField(
                    value = customText,
                    onValueChange = { customText = it.take(8) }, // 单位名短，限 8 字
                    label = { Text("单位名（如 碗/把/罐）") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    enabled = customText.isNotBlank(),
                    onClick = {
                        onAddUnit(customText.trim())
                        addingCustom = false
                        customText = ""
                    },
                ) { Text("保存并选用") }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { addingCustom = false; customText = "" }) { Text("取消") }
            },
        )
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

/** 营养数值输入(仅数字，最多一个小数点)。[AI生成] Item4 */
@Composable
private fun NutrientField(label: String, value: String, modifier: Modifier = Modifier, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = { s ->
            val f = s.filter { it.isDigit() || it == '.' }
            onValueChange(if (f.count { it == '.' } <= 1) f else value)
        },
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        singleLine = true,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
    )
}

