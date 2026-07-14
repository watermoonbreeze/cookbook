package com.sxdbsm.cookbook.android.ui.newdish

import android.widget.Toast
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
    var newTagText by remember { mutableStateOf("") }
    var importPickerOpen by remember { mutableStateOf(false) }
    var ingredientPickerOpen by remember { mutableStateOf(false) }
    var cookingMethodDialogOpen by remember { mutableStateOf(false) }
    var cookingMethodDraft by remember { mutableStateOf("") }
    var stepTemplateSheetOpen by remember { mutableStateOf(false) } // [AI生成] #2 "选择步骤"模板弹层开关
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
                        enabled = state.name.isNotBlank() && !state.saving && !state.loading && state.errorMessage == null,
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
                    onClick = {
                        cookingMethodDraft = ""
                        cookingMethodDialogOpen = true
                    },
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
                Text("添加食材", color = MaterialTheme.colorScheme.tertiary)
            }

            OperationStepsEditor(
                steps = state.steps,
                onAddStep = vm::addStep,
                onUpdateStepText = vm::updateStepText,
                onUpdateStepImages = { index, images, thumbnails ->
                    vm.updateStepImages(index, encodeImagePaths(images), encodeImagePaths(thumbnails))
                },
                onRemoveStep = vm::removeStep,
                onMoveStep = vm::moveStep,
                showStepNumber = stepModeEnabled,
                onPickTemplate = { vm.loadStepTemplates(); stepTemplateSheetOpen = true },
                onStepFocused = { focusedStepIndex = it },
            )

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
                thumbnailPaths = decodeImagePaths(state.thumbnailPath),
                onImagesChanged = { images, thumbnails ->
                    vm.setImages(encodeImagePaths(images), encodeImagePaths(thumbnails))
                },
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

    if (stepTemplateSheetOpen) {
        StepTemplatePickerDialog(
            templates = state.stepTemplates,
            canSaveCurrent = state.steps.any { it.text.isNotBlank() },
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
            onSaveCurrent = { name -> vm.saveCurrentStepsAsTemplate(name) },
            onDelete = { id -> vm.deleteStepTemplate(id) },
            onDismiss = { stepTemplateSheetOpen = false },
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
 * 食材克数步进器 −N+。[AI生成]
 *
 * #55：每味食材剂量以克为单位，±5g，最小 0。
 */
@Composable
private fun GramStepper(grams: Int, onDelta: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GramBtn("−") { onDelta(-5) }
        Text(
            "$grams g",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        GramBtn("＋") { onDelta(5) }
    }
}

@Composable
private fun GramBtn(label: String, onClick: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = MaterialTheme.shapes.small,
        modifier = Modifier.size(28.dp),
        onClick = onClick,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
        }
    }
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
            Text("添加步骤", color = MaterialTheme.colorScheme.tertiary)
        }
    }
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
    canSaveCurrent: Boolean,
    multiStep: Boolean, // [AI生成] #2 来自"我的-功能设置-分步执行"：开=分步插入，关=合并一条
    onApply: (com.sxdbsm.cookbook.domain.model.StepTemplate) -> Unit,
    onSaveCurrent: (String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    var saveNameOpen by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择步骤模板") },
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
                if (canSaveCurrent) {
                    item {
                        OutlinedButton(onClick = { newName = ""; saveNameOpen = true }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("把当前步骤存为模板")
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )

    if (saveNameOpen) {
        AlertDialog(
            onDismissRequest = { saveNameOpen = false },
            title = { Text("存为步骤模板") },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("模板名（如 我的红烧做法）") },
                    shape = MaterialTheme.shapes.medium,
                )
            },
            confirmButton = {
                TextButton(
                    enabled = newName.isNotBlank(),
                    onClick = { onSaveCurrent(newName.trim()); saveNameOpen = false },
                ) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { saveNameOpen = false }) { Text("取消") } },
        )
    }
}

/**
 * 烹饪方式弹框。[AI生成]
 *
 * 支持一次添加一个烹饪方式，外层表单可重复点击“+ 添加”形成多个方式。[AI修改]
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
