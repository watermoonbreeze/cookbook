package com.sxdbsm.cookbook.android.ui.newdish

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.FormFieldLabel
import com.sxdbsm.cookbook.android.ui.component.ImagePickerButton
import com.sxdbsm.cookbook.android.ui.component.decodeImagePaths
import com.sxdbsm.cookbook.android.ui.component.encodeImagePaths
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.android.ui.picker.IngredientPickerScreen
import com.sxdbsm.cookbook.domain.model.Ingredient
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
    onSavedDish: ((Long) -> Unit)? = null,
    vm: NewDishViewModel = koinViewModel(),
) {
    // [AI修改] 表单状态来自 ViewModel，局部弹窗开关用 remember 存在当前 Composable 内。
    val state by vm.state.collectAsStateWithLifecycle()
    // [AI生成] 分步执行开关(功能设置)：关闭时步骤不显示"步骤N"序号。
    // [AI修改] observeFlag 用 remember 缓存，避免每次重组新建 Flow 反复订阅查库。
    val prefs = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
    val stepModeEnabled by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.STEP_MODE_ENABLED, false)
    }.collectAsStateWithLifecycle(false)
    var tagInputOpen by remember { mutableStateOf(false) }
    var importPickerOpen by remember { mutableStateOf(false) }
    var ingredientPickerOpen by remember { mutableStateOf(false) }
    var cookingMethodDialogOpen by remember { mutableStateOf(false) }
    var stepTemplateSheetOpen by remember { mutableStateOf(false) } // [AI生成] #2 "选择步骤"模板弹层开关
    var ingredientGroupSheetOpen by remember { mutableStateOf(false) } // [AI生成] B5 "配料组"弹层开关
    var groupEditorOpen by remember { mutableStateOf(false) } // [AI生成] 需求2 全屏配料组编辑器开关
    var focusedStepIndex by remember { mutableStateOf<Int?>(null) } // [AI生成] #2 当前定位(聚焦)的步骤下标：模板插入到这一步
    var duplicateIngredientNames by remember { mutableStateOf<List<String>>(emptyList()) } // [AI生成] 选择食材后用于提示已存在的食材名称。
    var pendingNewIngredients by remember { mutableStateOf<List<Ingredient>>(emptyList()) } // [AI生成] 用户确认重复提示后实际追加的新增食材。
    val context = LocalContext.current

    /**
     * 页面入口统一交给 ViewModel 处理，避免编辑、新建、导入在同一实例中串状态。[AI修改]
     */
    LaunchedEffect(editingDishId, importDishId) {
        AppLogger.d("NewDishEdit", "screen start effect: editingDishId=$editingDishId importDishId=$importDishId") // [AI生成] 记录页面收到的导航参数。
        vm.start(editingDishId, importDishId)
    }
    LaunchedEffect(state.done) {
        if (state.done) {
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show() // [AI生成] A4：保存成功轻提示，家庭用户对"存没存上"敏感。
            state.savedDishId?.let { onSavedDish?.invoke(it) } // [AI生成] 从添加餐食页新建菜品后，把新菜品 id 回传给上一层路由。
            onBack()
        }
    }
    LaunchedEffect(state.editProbeToastSerial) {
        val message = state.editProbeToastMessage ?: return@LaunchedEffect
        AppLogger.d("NewDishEdit", "show edit probe toast: serial=${state.editProbeToastSerial} message=$message") // [AI生成] 记录一次性 Toast 是否被 UI 消费。
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        vm.consumeEditProbeToast()
    }
    LaunchedEffect(state.editingId, state.loading, state.name, state.tags.size, state.ingredients.size, state.errorMessage) {
        AppLogger.d(
            "NewDishEdit",
            "ui state snapshot: editingId=${state.editingId} loading=${state.loading} name=${state.name} tags=${state.tags.size} ingredients=${state.ingredients.size} error=${state.errorMessage}",
        ) // [AI生成] 记录 Compose 实际收到的表单状态，排查“Toast 成功但界面空白”是否为状态覆盖或渲染问题。
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            TopAppBar(
                title = { Text(if (editingDishId != null || state.editingId != null) "编辑菜品" else "新建菜品", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.primary,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    // [AI修改] 苹果风格：导入降为纯文字次操作，避免与主 CTA 争视觉；保存为胶囊主按钮。
                    TextButton(onClick = { importPickerOpen = true }) {
                        Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("导入")
                    }
                    Spacer(Modifier.width(4.dp))
                    com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                        text = "保存",
                        onClick = { vm.save() },
                        enabled = state.name.isNotBlank() && !state.saving && !state.loading && state.errorMessage == null,
                    )
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
            if (state.loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = "加载菜品信息中...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }
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
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = tag,
                                textAlign = TextAlign.Center,
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { vm.removeTag(tag) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "删除标签", modifier = Modifier.size(14.dp))
                            }
                        },
                        modifier = Modifier.height(32.dp), // [AI修改] 标签项高度与“+ 添加”按钮保持一致。
                    )
                }
                AssistChip(
                    onClick = { tagInputOpen = true },
                    label = {
                        Text(
                            "+ 添加",
                            textAlign = TextAlign.Center,
                        )
                    },
                    modifier = Modifier.height(32.dp),
                )
            }

            FormFieldLabel("烹饪方式")
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                state.cookingMethodNames.forEach { method ->
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                text = method,
                                textAlign = TextAlign.Center,
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { vm.removeCookingMethod(method) },
                                modifier = Modifier.size(20.dp),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "删除烹饪方式", modifier = Modifier.size(14.dp))
                            }
                        },
                        modifier = Modifier.height(32.dp), // [AI修改] 已添加项高度与“+ 添加”按钮保持一致。
                    )
                }
                AssistChip(
                    onClick = { cookingMethodDialogOpen = true },
                    label = {
                        Text(
                            "+ 添加",
                            textAlign = TextAlign.Center,
                        )
                    },
                    modifier = Modifier.height(32.dp),
                )
            }

            // [AI修改] #3：自建/编辑菜品不再提供菜系选择——菜系是预设菜的分类维度，用户自建菜不纳入。
            // DB 的 cuisine 列与预设菜的菜系保留不变；编辑预设菜时其原菜系原样带回保存(state.cuisine 不动)。

            // [AI修改] B5：食材清单标题右侧加"配料组"入口——套用常用配料组(如基础调料包)一键加入。
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                FormFieldLabel("食材清单")
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { vm.loadIngredientGroups(); ingredientGroupSheetOpen = true }) {
                    Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("配料组")
                }
            }
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
                                // [AI修改] N4：展示"食材名-二级名称(别名)"。
                                val nameText = if (ing.ingredient.alias.isBlank()) ing.ingredient.name else "${ing.ingredient.name}-${ing.ingredient.alias}"
                                Text(nameText, modifier = Modifier.weight(1f))
                                // [AI修改] #55：克数剂量 −N+(±5，最小0)。
                                val grams = ing.quantity?.toInt() ?: 100
                                GramStepper(grams = grams, onDelta = { d -> vm.changeIngredientGrams(ing.ingredient.id, d) })
                                Spacer(Modifier.width(4.dp))
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
                Text("添加食材", color = MaterialTheme.colorScheme.primary)
            }

            OperationStepsEditor(
                steps = state.steps,
                onAddStep = vm::addStep,
                onUpdateStepText = vm::updateStepText,
                onUpdateStepImages = { index, images, thumbnails ->
                    vm.updateStepImages(index, encodeImagePaths(images), encodeImagePaths(thumbnails))
                },
                // [AI修改] A-1 修复：删除/移动步骤后步骤索引会错位，复位 focusedStepIndex，
                // 让模板套用回退到"末步"语义，避免并入错误步骤 + Toast 报错步号。
                onRemoveStep = { vm.removeStep(it); focusedStepIndex = null },
                onMoveStep = { i, toStart -> vm.moveStep(i, toStart); focusedStepIndex = null },
                showStepNumber = stepModeEnabled,
                onPickTemplate = { vm.loadStepTemplates(); stepTemplateSheetOpen = true },
                onStepFocused = { focusedStepIndex = it },
            )

            // [AI修改] A7：把可选的"特殊说明/描述/图片"收进"更多信息(可选)"，默认折叠，降低录入压迫感；
            // 有内容时自动展开(编辑既有菜品/已填过不至于藏起来)。快速记一道菜只需菜名+食材即可。
            val hasMore = state.specialNote.isNotBlank() || state.description.isNotBlank() ||
                state.imagePath.isNotBlank() || state.thumbnailPath.isNotBlank()
            var moreExpanded by rememberSaveable { mutableStateOf(false) }
            LaunchedEffect(hasMore) { if (hasMore) moreExpanded = true }
            TextButton(onClick = { moreExpanded = !moreExpanded }) {
                Icon(
                    if (moreExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                )
                Spacer(Modifier.width(4.dp))
                Text("更多信息（可选）", color = MaterialTheme.colorScheme.primary)
            }
            if (moreExpanded) {
                FormFieldLabel("特殊说明")
                OutlinedTextField(
                    value = state.specialNote,
                    onValueChange = vm::setSpecialNote,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("如：少盐") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )

                FormFieldLabel("描述")
                OutlinedTextField(
                    value = state.description,
                    onValueChange = vm::setDescription,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("（可选，做法/心得）") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                )

                FormFieldLabel("图片")
                ImagePickerButton(
                    imagePaths = decodeImagePaths(state.imagePath),
                    thumbnailPaths = decodeImagePaths(state.thumbnailPath),
                    onImagesChanged = { images, thumbnails ->
                        vm.setImages(encodeImagePaths(images), encodeImagePaths(thumbnails))
                    },
                    maxCount = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
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
            excludeIngredientIds = emptySet(), // [AI修改] 不再过滤当前菜品已有食材，确保保存后进入“最近使用”的食材在再次打开选择器时可见；重复添加由 ViewModel 兜底。
            onDismiss = { ingredientPickerOpen = false },
            onConfirm = { selected ->
                val existingIds = state.ingredients.map { it.ingredient.id }.toSet()
                val duplicates = selected.filter { it.id in existingIds }
                val additions = selected.filterNot { it.id in existingIds }
                if (duplicates.isNotEmpty()) {
                    duplicateIngredientNames = duplicates.map { it.displayNameText() }
                    pendingNewIngredients = additions // [AI修改] 确认后只追加新增食材，已有食材的用量/备注不被覆盖。
                } else {
                    additions.forEach { vm.addIngredient(it) }
                }
            },
        )
    }

    if (duplicateIngredientNames.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = {
                duplicateIngredientNames = emptyList()
                pendingNewIngredients = emptyList()
            },
            title = { Text("食材已存在") },
            text = {
                Text("食材 ${duplicateIngredientNames.joinToString("、")} 已经在菜品中存在。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingNewIngredients.forEach { vm.addIngredient(it) }
                        duplicateIngredientNames = emptyList()
                        pendingNewIngredients = emptyList()
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        duplicateIngredientNames = emptyList()
                        pendingNewIngredients = emptyList()
                    },
                ) {
                    Text("取消")
                }
            },
        )
    }

    if (tagInputOpen) {
        // [AI修改] T3：标签弹窗改为库选择器——展示标签库可直接选，输入并添加会存进库，可编辑删除自建标签。
        LibraryPickerDialog(
            title = "标签",
            chips = state.availableTags.map { LibChip(it.id, it.name, it.preset) },
            selectedNames = state.tags.toSet(),
            inputLabel = "新标签（如 家常 / 快手 / 少盐）",
            onToggle = { if (it in state.tags) vm.removeTag(it) else vm.addTag(it) },
            onAddNew = { vm.saveAndAddTag(it) },
            onDelete = { id ->
                val name = state.availableTags.firstOrNull { it.id == id }?.name
                vm.deleteTagFromLibrary(id)
                if (name != null) Toast.makeText(context, "已从标签库删除「$name」", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { tagInputOpen = false },
        )
    }

    if (ingredientGroupSheetOpen) {
        IngredientGroupPickerDialog(
            groups = state.ingredientGroups,
            onApply = { group ->
                vm.applyIngredientGroup(group)
                ingredientGroupSheetOpen = false
                Toast.makeText(context, "已加入「${group.name}」的 ${group.items.size} 味配料", Toast.LENGTH_SHORT).show()
            },
            onAddNew = { ingredientGroupSheetOpen = false; groupEditorOpen = true }, // [AI修改] 需求2：打开全屏编辑器
            onDelete = { id -> vm.deleteIngredientGroup(id) },
            onDismiss = { ingredientGroupSheetOpen = false },
        )
    }

    if (groupEditorOpen) {
        // [AI生成] 需求2：全屏配料组编辑器——从食材库真实选、克数可调，与编辑菜品配料一致。
        IngredientGroupEditorScreen(
            initialItems = state.ingredients, // 有当前食材则带过来
            onSave = { name, items ->
                vm.createIngredientGroup(name, items)
                groupEditorOpen = false
                Toast.makeText(context, "已保存配料组「$name」", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { groupEditorOpen = false },
        )
    }

    if (stepTemplateSheetOpen) {
        StepTemplatePickerDialog(
            templates = state.stepTemplates,
            currentItems = state.steps.map { it.text.trim() }.filter { it.isNotBlank() },
            multiStep = stepModeEnabled, // [AI生成] #2 由"我的-功能设置-分步执行"控制插入方式
            onApply = { template ->
                vm.applyStepTemplate(template, focusedStepIndex, stepModeEnabled)
                stepTemplateSheetOpen = false
                val msg = when {
                    stepModeEnabled -> "已把「${template.name}」的 ${template.steps.size} 步分别加入"
                    state.steps.isEmpty() -> "已把「${template.name}」加入新步骤"
                    else -> "已把「${template.name}」并入第 ${(focusedStepIndex?.plus(1)) ?: state.steps.size} 步"
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            },
            onCreate = { name, texts ->
                vm.createStepTemplate(name, texts)
                Toast.makeText(context, "已保存步骤模板「$name」", Toast.LENGTH_SHORT).show()
            },
            onDelete = { id -> vm.deleteStepTemplate(id) },
            onDismiss = { stepTemplateSheetOpen = false },
        )
    }

    if (cookingMethodDialogOpen) {
        // [AI修改] T4：烹饪方式弹窗——库选择器，输入并添加会存进烹饪库，可编辑删除自建方式。
        LibraryPickerDialog(
            title = "烹饪方式",
            chips = state.availableCookingMethods.map { LibChip(it.id, it.name, it.preset) },
            selectedNames = state.cookingMethodNames.toSet(),
            inputLabel = "新烹饪方式（如 白灼 / 焗 / 生腌）",
            onToggle = { if (it in state.cookingMethodNames) vm.removeCookingMethod(it) else vm.addCookingMethod(it) },
            onAddNew = { vm.saveAndAddCookingMethod(it) },
            onDelete = { id ->
                val name = state.availableCookingMethods.firstOrNull { it.id == id }?.name
                vm.deleteCookingMethodFromLibrary(id)
                if (name != null) Toast.makeText(context, "已从烹饪库删除「$name」", Toast.LENGTH_SHORT).show()
            },
            onDismiss = { cookingMethodDialogOpen = false },
        )
    }
}

/**
 * 食材克数步进器 −N+。[AI生成]
 *
 * #55：每味食材剂量以克为单位，±5g，最小 0。
 */
// [AI修改] 苹果风格：克数步进器改用统一的 MiniStepper(−/＋ 缩小成可点标签)。
@Composable
private fun GramStepper(grams: Int, onDelta: (Int) -> Unit) {
    com.sxdbsm.cookbook.android.ui.component.MiniStepper(
        valueText = "$grams g",
        onMinus = { onDelta(-5) },
        onPlus = { onDelta(5) },
        minusEnabled = grams > 0,
    )
}

/**
 * 菜品操作步骤编辑区。[AI生成]
 *
 * 每一步支持文字和多张过程图，保存时由 ViewModel 转成 shared 层 `DishStep`。
 */
@Composable
private fun OperationStepsEditor(
    steps: List<com.sxdbsm.cookbook.domain.model.DishStep>,
    onAddStep: () -> Unit,
    onUpdateStepText: (Int, String) -> Unit,
    onUpdateStepImages: (Int, List<String>, List<String>) -> Unit,
    onRemoveStep: (Int) -> Unit,
    onMoveStep: (Int, Boolean) -> Unit, // [AI生成] (index, toStart) 上移/下移
    showStepNumber: Boolean, // [AI生成] 分步执行开启时才显示"步骤N"序号
    onPickTemplate: () -> Unit, // [AI生成] #2 打开"选择步骤"模板弹层
    onStepFocused: (Int) -> Unit, // [AI生成] #2 某步输入框获焦→记录为当前定位步(模板插入目标)
) {
    // [AI修改] #2：标题右侧加"选择步骤"入口——套用预设/自建步骤模板。
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        FormFieldLabel("操作步骤")
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onPickTemplate) {
            Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("选择步骤")
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (steps.isEmpty()) {
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Text(
                    "还没添加步骤",
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        steps.forEachIndexed { index, step ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    // [AI生成] 上移/下移始终可用；"步骤N"序号仅在分步执行开启时显示（用户不希望强制编号）。
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        if (showStepNumber) {
                            Text(
                                "步骤 ${index + 1}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onMoveStep(index, true) }, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { onMoveStep(index, false) }, enabled = index < steps.size - 1, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(20.dp))
                        }
                    }
                    OutlinedTextField(
                        value = step.text,
                        onValueChange = { onUpdateStepText(index, it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { if (it.isFocused) onStepFocused(index) }, // [AI生成] #2 记录当前定位步
                        placeholder = { Text("如：热锅冷油，放入蒜末爆香") },
                        minLines = 2,
                        shape = MaterialTheme.shapes.medium,
                    )
                    ImagePickerButton(
                        imagePaths = decodeImagePaths(step.imagePath),
                        thumbnailPaths = decodeImagePaths(step.thumbnailPath),
                        onImagesChanged = { images, thumbnails -> onUpdateStepImages(index, images, thumbnails) },
                        maxCount = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = { onRemoveStep(index) }) {
                            Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("删除")
                        }
                    } // [AI修改] 删除入口移到步骤卡片右下角，避免顶部空占位压缩输入区域。
                }
            }
        }
        TextButton(onClick = onAddStep) {
            Icon(Icons.Outlined.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text("添加步骤", color = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * 常用配料组弹框。[AI生成] B5
 *
 * 列出预设+自建配料组，"应用"把组内食材加入食材清单(按名解析)；自建可删；也可把当前食材存为配料组。
 */
@Composable
private fun IngredientGroupPickerDialog(
    groups: List<com.sxdbsm.cookbook.domain.model.IngredientGroup>,
    onApply: (com.sxdbsm.cookbook.domain.model.IngredientGroup) -> Unit,
    onAddNew: () -> Unit, // [AI修改] 需求2：打开全屏配料组编辑器(真实选食材+克数)
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        // [AI修改] 需求2：标题右侧常驻"+添加"——打开全屏编辑器(从食材库真实选、克数可调)。
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("常用配料组", modifier = Modifier.weight(1f))
                TextButton(onClick = onAddNew) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("添加")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (groups.isEmpty()) {
                    item { Text("还没有配料组", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium) }
                }
                items(groups, key = { it.id }) { g ->
                    OutlinedCard(shape = MaterialTheme.shapes.medium) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(g.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (g.isPreset) {
                                    AssistChip(onClick = {}, label = { Text("预设") }, modifier = Modifier.height(28.dp))
                                } else {
                                    IconButton(onClick = { onDelete(g.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Outlined.Close, contentDescription = "删除配料组", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(g.items.joinToString("、") { it.name }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onApply(g) }) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("加入")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/**
 * 选择步骤模板弹框。[AI生成] #2
 *
 * 列出预设+自建步骤模板，点"应用"把该模板步骤追加到当前步骤；自建模板可删除；
 * 也可把当前已填的步骤"存为模板"供其他菜品复用。
 */
@Composable
private fun StepTemplatePickerDialog(
    templates: List<com.sxdbsm.cookbook.domain.model.StepTemplate>,
    currentItems: List<String>, // [AI修改] bug2：当前菜的步骤文字，"+添加"编辑器预填
    multiStep: Boolean, // [AI生成] #2 来自"我的-功能设置-分步执行"：开=分步插入，关=合并一条
    onApply: (com.sxdbsm.cookbook.domain.model.StepTemplate) -> Unit,
    onCreate: (String, List<String>) -> Unit, // [AI修改] bug2：编辑器保存(名称, 步骤文字列表)
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var editorOpen by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        // [AI修改] bug2：标题右侧常驻"+添加"——点开编辑器(有当前步骤则带过来、没有则新增)。
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("选择步骤模板", modifier = Modifier.weight(1f))
                TextButton(onClick = { editorOpen = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(2.dp))
                    Text("添加")
                }
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // [AI生成] #2 提示当前插入方式(由"分步执行"设置决定)：开=每步单独成条；关=合并进当前定位步。
                item {
                    Text(
                        if (multiStep) "分步执行已开：模板每一步将单独成为一条步骤" else "模板将合并写入当前所在步骤（无步骤则新建一条）；如需分步，可在「我的-功能设置-分步执行」开启",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (templates.isEmpty()) {
                    item {
                        Text("还没有可用模板", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                items(templates, key = { it.id }) { t ->
                    OutlinedCard(shape = MaterialTheme.shapes.medium) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(t.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                if (t.isPreset) {
                                    AssistChip(onClick = {}, label = { Text("预设") }, modifier = Modifier.height(28.dp))
                                } else {
                                    IconButton(onClick = { onDelete(t.id) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Outlined.Close, contentDescription = "删除模板", modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            // 步骤预览：编号列出，避免弹层过高只显示前若干步。
                            t.steps.take(6).forEachIndexed { i, s ->
                                Text("${i + 1}. $s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (t.steps.size > 6) {
                                Text("…共 ${t.steps.size} 步", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Spacer(Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                                TextButton(onClick = { onApply(t) }) {
                                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("应用")
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )

    if (editorOpen) {
        StepTemplateEditorDialog(
            showStepNumber = multiStep, // [AI修改] 需求1：编号跟随"分步执行"设置
            initialSteps = currentItems, // 有当前步骤则带过来，没有则新增
            onSave = { name, items -> onCreate(name, items) },
            onDismiss = { editorOpen = false },
        )
    }
}

/**
 * 步骤模板编辑器。[AI生成] 需求1：与菜品编辑的操作步骤保持一致(步骤卡+"步骤N"编号随分步执行+上下移+多行文字)，仅无拍照。
 */
@Composable
private fun StepTemplateEditorDialog(
    showStepNumber: Boolean,
    initialSteps: List<String>,
    onSave: (String, List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val steps = remember { mutableStateListOf<String>().apply { addAll(initialSteps.ifEmpty { listOf("") }) } }
    fun move(index: Int, toStart: Boolean) {
        val target = if (toStart) index - 1 else index + 1
        if (index in steps.indices && target in steps.indices) { val t = steps[index]; steps[index] = steps[target]; steps[target] = t }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("新建步骤模板") },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 460.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = name, onValueChange = { name = it }, singleLine = true,
                        label = { Text("模板名（如 我的红烧做法）") }, shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                itemsIndexed(steps) { index, text ->
                    OutlinedCard(shape = MaterialTheme.shapes.large, colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                if (showStepNumber) Text("步骤 ${index + 1}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.weight(1f))
                                IconButton(onClick = { move(index, true) }, enabled = index > 0, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.KeyboardArrowUp, contentDescription = "上移", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { move(index, false) }, enabled = index < steps.size - 1, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = "下移", modifier = Modifier.size(20.dp))
                                }
                                IconButton(onClick = { if (steps.size > 1) steps.removeAt(index) else steps[index] = "" }, modifier = Modifier.size(36.dp)) {
                                    Icon(Icons.Outlined.Close, contentDescription = "删除", modifier = Modifier.size(18.dp))
                                }
                            }
                            OutlinedTextField(
                                value = text, onValueChange = { steps[index] = it },
                                modifier = Modifier.fillMaxWidth(), minLines = 2,
                                placeholder = { Text("如：热锅冷油，放入蒜末爆香") }, shape = MaterialTheme.shapes.medium,
                            )
                        }
                    }
                }
                item {
                    OutlinedButton(onClick = { steps.add("") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp)); Text("添加步骤")
                    }
                }
            }
        },
        confirmButton = {
            val valid = name.isNotBlank() && steps.any { it.isNotBlank() }
            TextButton(enabled = valid, onClick = { onSave(name.trim(), steps.map { it.trim() }.filter { it.isNotBlank() }); onDismiss() }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

/**
 * 全屏配料组编辑器。[AI生成] 需求2
 *
 * 与编辑菜品的食材清单一致：从食材库**真实选择**食材(而非手输)，每味带**克数 −N+**可调；
 * 有当前菜的食材则带过来。保存为自建配料组(含克数)，套用到别的菜时克数一并带过来。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientGroupEditorScreen(
    initialItems: List<com.sxdbsm.cookbook.domain.model.DishIngredient>,
    onSave: (String, List<com.sxdbsm.cookbook.domain.model.IngredientGroupItem>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    val items = remember { mutableStateListOf<com.sxdbsm.cookbook.domain.model.DishIngredient>().apply { addAll(initialItems) } }
    var pickerOpen by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("新建配料组", fontWeight = FontWeight.SemiBold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") } },
                    actions = {
                        val valid = name.isNotBlank() && items.isNotEmpty()
                        com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                            text = "保存",
                            enabled = valid,
                            onClick = {
                                onSave(name.trim(), items.map {
                                    com.sxdbsm.cookbook.domain.model.IngredientGroupItem(it.ingredient.name, it.isMain, it.quantity)
                                })
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name, onValueChange = { name = it }, singleLine = true,
                    label = { Text("配料组名（如 我的火锅调料）") }, shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                )
                FormFieldLabel("食材（从食材库选择，克数可调）")
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column {
                        if (items.isEmpty()) {
                            Text("还没加食材", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            items.forEachIndexed { index, ing ->
                                Row(
                                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(ing.ingredient.name, modifier = Modifier.weight(1f))
                                    val grams = ing.quantity?.toInt() ?: 100
                                    GramStepper(grams = grams, onDelta = { d ->
                                        items[index] = ing.copy(quantity = (grams + d).coerceAtLeast(0).toDouble())
                                    })
                                    Spacer(Modifier.width(4.dp))
                                    IconButton(onClick = { items.removeAt(index) }) {
                                        Icon(Icons.Outlined.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
                                    }
                                }
                                Divider()
                            }
                        }
                    }
                }
                TextButton(onClick = { pickerOpen = true }) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加食材", color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    }
    if (pickerOpen) {
        IngredientPickerScreen(
            excludeIngredientIds = items.map { it.ingredient.id }.toSet(),
            onDismiss = { pickerOpen = false },
            onConfirm = { selected ->
                selected.filter { s -> items.none { it.ingredient.id == s.id } }
                    .forEach { items.add(com.sxdbsm.cookbook.domain.model.DishIngredient(ingredient = it, isMain = false, quantity = 100.0)) }
            },
        )
    }
}

/** 库选择弹窗的一项(标签/烹饪方式通用)。[AI生成] T3/T4 */
private data class LibChip(val id: Long, val name: String, val preset: Boolean)

/**
 * 可复用「库选择」弹窗：标签库 / 烹饪方式库通用。[AI生成] T3/T4
 *
 * - 直接展示库内容为可选 chip，点击选中/取消(加到本菜)；
 * - 底部输入 + 「添加」：新内容加到本菜并存进库(下次可选)；
 * - 标题右侧无边框「编辑」文字按钮：进入编辑态后自建项显示删除，点击从库中删除(预设不可删)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryPickerDialog(
    title: String,
    chips: List<LibChip>,
    selectedNames: Set<String>,
    inputLabel: String,
    onToggle: (String) -> Unit,
    onAddNew: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var editing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(title, modifier = Modifier.weight(1f))
                if (chips.any { !it.preset }) {
                    // 无边框「编辑/完成」文字按钮，管理自建项。
                    Text(
                        if (editing) "完成" else "编辑",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .clickable { editing = !editing }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (chips.isEmpty()) {
                    Text("暂无，可在下方输入添加", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        chips.forEach { c ->
                            if (editing && !c.preset) {
                                AssistChip(
                                    onClick = { onDelete(c.id) },
                                    label = { Text(c.name) },
                                    trailingIcon = { Icon(Icons.Outlined.Close, contentDescription = "删除", modifier = Modifier.size(16.dp)) },
                                )
                            } else {
                                FilterChip(
                                    selected = c.name in selectedNames,
                                    onClick = { if (!editing) onToggle(c.name) },
                                    label = { Text(c.name) },
                                )
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    label = { Text(inputLabel) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        TextButton(onClick = { if (input.isNotBlank()) { onAddNew(input.trim()); input = "" } }, enabled = input.isNotBlank()) {
                            Text("添加")
                        }
                    },
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

/**
 * 食材展示名称。[AI生成]
 */
private fun Ingredient.displayNameText(): String =
    if (alias.isBlank()) name else "$name($alias)"

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
