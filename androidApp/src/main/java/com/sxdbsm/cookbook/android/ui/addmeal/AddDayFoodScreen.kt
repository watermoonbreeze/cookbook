package com.sxdbsm.cookbook.android.ui.addmeal

import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.MealDishGrid
import com.sxdbsm.cookbook.android.ui.component.FormFieldLabel
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.domain.model.FavoriteCombo
import com.sxdbsm.cookbook.domain.model.MealType
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel

/**
 * 添加餐食页面。[AI修改]
 *
 * 支持一次创建某一天的多个餐食模块，每个模块对应一条餐食记录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDayFoodScreen(
    onBack: () -> Unit,
    onAddNewDish: () -> Unit,
    onOpenDish: (Long) -> Unit = {}, // [AI生成] F1：点餐次里的菜进详情
    copyFromDate: LocalDate? = null, // [AI生成] F8：食历复制来源日期→预填新建草稿
    editDate: LocalDate? = null,
    createdDishId: Long? = null,
    presetDishIds: List<Long> = emptyList(), // [AI生成] AI 推荐"选它"带入的菜品(从首页进入时新建整餐)。
    onCreatedDishConsumed: () -> Unit = {},
    onOpenAiForBlock: () -> Unit = {}, // [AI生成] 从某餐次块进入 AI 推荐。
    aiPickedDishIds: List<Long> = emptyList(), // [AI生成] AI 推荐回传给餐次块的菜品 id。
    onAiPickedConsumed: () -> Unit = {},
    vm: AddMealViewModel = koinViewModel(),
) {
    // [AI修改] 页面订阅 ViewModel 状态，任何字段变化都会触发相关 UI 重组。
    val state by vm.state.collectAsStateWithLifecycle()
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickingBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    var timeDialogBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comboPickerBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var saveComboBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comboNameDraft by rememberSaveable { mutableStateOf("") }
    var aiTargetBlockId by rememberSaveable { mutableStateOf<Long?>(null) } // [AI生成] 记录哪个餐次块发起了 AI 推荐。
    val snackbar = remember { SnackbarHostState() } // [AI生成] A6：移除菜品撤销提示
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current

    // [AI生成] AI 推荐从餐次块进入并"选它"回传：把菜品加入发起的那个餐次块。
    LaunchedEffect(aiPickedDishIds) {
        if (aiPickedDishIds.isEmpty()) return@LaunchedEffect
        val target = aiTargetBlockId ?: state.activeBlockId
        if (target != null) vm.addDishesByIds(target, aiPickedDishIds)
        aiTargetBlockId = null
        onAiPickedConsumed()
    }

    LaunchedEffect(Unit) {
        AppLogger.d("MealFlow", "AddDayFoodScreen enter: editDate=$editDate createdDishId=$createdDishId") // [AI生成] 记录添加/编辑餐食页入口参数，便于排查路由与状态是否匹配。
    }
    LaunchedEffect(state.done) {
        if (state.done) {
            AppLogger.d("MealFlow", "AddDayFoodScreen done: date=${state.date} blocks=${state.mealBlocks.size}") // [AI生成] 保存完成后记录返回前状态摘要。
            Toast.makeText(context, "已保存", Toast.LENGTH_SHORT).show() // [AI生成] A4：餐食保存成功轻提示。
            onBack()
        }
    }
    LaunchedEffect(editDate, presetDishIds, copyFromDate) {
        AppLogger.d("MealFlow", "configure by editDate effect: editDate=$editDate preset=$presetDishIds copyFrom=$copyFromDate currentDate=${state.date} blocks=${state.mealBlocks.size}") // [AI生成] 记录入口日期配置，排查返回后是否误重载旧餐食。
        if (copyFromDate != null) {
            vm.configureCopy(copyFromDate) // [AI生成] F8：食历复制→按来源日预填成新建草稿
        } else {
            vm.configure(editDate, presetDishIds) // [AI修改] 入口日期做初始化配置；带 AI 推荐预填时并入菜品。
        }
    }
    LaunchedEffect(createdDishId) {
        val dishId = createdDishId?.takeIf { it > 0 } ?: return@LaunchedEffect
        AppLogger.d("MealFlow", "created dish consumed: dishId=$dishId targetBlock=${pickingBlockId ?: state.activeBlockId} pickerOpenBefore=$pickerOpen") // [AI生成] 记录新建菜品回传后加入哪个餐食模块。
        vm.addCreatedDish(dishId, pickingBlockId ?: state.activeBlockId)
        pickerOpen = true // [AI修改] 返回后继续停留在菜品选择流程，并由 DishPicker 重新读取最新菜品列表。
        onCreatedDishConsumed()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        snackbarHost = { SnackbarHost(snackbar) }, // [AI生成] A6：移除撤销
        topBar = {
            TopAppBar(
                title = { Text("添加餐食", fontWeight = FontWeight.SemiBold) },
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
                    com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                        text = if (state.isPlan) "保存计划" else "保存",
                        onClick = { vm.save() },
                        enabled = state.canSave,
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
                .verticalScroll(rememberScrollState()),
        ) {
            state.errorMessage?.let { message ->
                Text(
                    text = message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            } // [AI生成] 保存失败时在表单顶部提示，用户可调整后重试。
            // [AI生成] F7：选到已有餐食的日期时提示(不切换过去)，选到空日期自动清除。
            state.dateWarning?.let { warning ->
                Text(
                    text = "⚠ $warning",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            FormFieldLabel("日期", startPadding = 16.dp)
            // [AI修改] N3：编辑既有某天的餐食时日期锁定不可改(防改日期导致数据错乱)；仅新增/复制可改。
            OutlinedButton(
                onClick = { dateDialogOpen = true },
                enabled = !state.isEditingExisting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Outlined.Event, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(state.date.toString(), modifier = Modifier.weight(1f))
                if (state.isEditingExisting) {
                    Text("编辑中不可改", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (state.isPlan) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        "这是 ${state.date} 的计划，未来才会真正吃",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            FormFieldLabel("餐次", startPadding = 16.dp)
            state.mealBlocks.forEach { block ->
                MealBlockCard(
                    block = block,
                    mealTypes = state.mealTypes,
                    canRemove = state.mealBlocks.size > 1,
                    onMealTypeChange = { mealTypeId -> vm.setMealType(block.id, mealTypeId) },
                    onPickTime = { timeDialogBlockId = block.id },
                    onAddDish = {
                        vm.setActiveBlock(block.id)
                        pickingBlockId = block.id
                        AppLogger.d("MealFlow", "open dish picker: blockId=${block.id} date=${state.date} mealTypeId=${block.mealTypeId} existingDishes=${block.dishes.map { it.id }}") // [AI生成] 记录打开菜品选择器时的餐次和已有菜品。
                        pickerOpen = true
                    },
                    onRemoveDish = { dishId ->
                        // [AI生成] A6：移除后弹 Snackbar 可撤销(误删家常事)，撤销即把该菜重新加回本块。
                        val removed = block.dishes.firstOrNull { it.id == dishId }
                        vm.removeDish(block.id, dishId)
                        if (removed != null) {
                            scope.launch {
                                val res = snackbar.showSnackbar(message = "已移除「${removed.name}」", actionLabel = "撤销", duration = SnackbarDuration.Short)
                                if (res == SnackbarResult.ActionPerformed) vm.addDishes(block.id, listOf(removed))
                            }
                        }
                    },
                    onOpenDish = onOpenDish, // [AI生成] F1：点菜进详情
                    onNoteChange = { vm.setNote(block.id, it) },
                    onRemoveBlock = { vm.removeMealBlock(block.id) },
                    onAiRecommend = { // [AI生成] 从该餐次块进入 AI 推荐，回传后加入本块。
                        vm.setActiveBlock(block.id)
                        aiTargetBlockId = block.id
                        onOpenAiForBlock()
                    },
                    onOpenCombos = { comboPickerBlockId = block.id },
                    onSaveCombo = {
                        comboNameDraft = comboNameForBlock(
                            date = state.date,
                            block = block,
                            mealTypes = state.mealTypes,
                        )
                        saveComboBlockId = block.id
                    },
                    hasCombos = state.favoriteCombos.isNotEmpty(),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = vm::addMealBlock) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加")
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    if (dateDialogOpen) {
        DatePickerDialogContent(
            initialDate = state.date,
            onDismiss = { dateDialogOpen = false },
            onConfirm = { date ->
                vm.setDate(date)
                dateDialogOpen = false
            },
        )
    }

    val timeBlock = state.mealBlocks.firstOrNull { it.id == timeDialogBlockId }
    if (timeBlock != null) {
        TimePickerDialogContent(
            initialTime = timeBlock.mealTime ?: LocalTime(12, 0),
            onDismiss = { timeDialogBlockId = null },
            onConfirm = { time ->
                vm.setMealTime(timeBlock.id, time)
                timeDialogBlockId = null
            },
        )
    }

    if (pickerOpen) {
        val blockId = pickingBlockId ?: state.activeBlockId
        val block = state.mealBlocks.firstOrNull { it.id == blockId }
        val mealName = state.mealTypes.firstOrNull { it.id == block?.mealTypeId }?.name.orEmpty()
        AppLogger.d("MealFlow", "compose dish picker: blockId=$blockId mealName=$mealName selected=${block?.dishes?.map { it.id }} pickerOpen=$pickerOpen") // [AI生成] 记录弹框组合时传入的已选菜品，排查勾选状态。
        DishPickerScreen(
            title = if (mealName.isBlank()) "添加菜品" else "添加到$mealName",
            multiSelect = true,
            initialSelected = block?.dishes ?: emptyList(),
            excludeDishIds = emptySet(), // [AI修改] 当前餐次已有菜品在弹框内以勾选态展示，避免新建菜品回填后被排除导致看不到。
            showRecentChips = true,
            showAddNewButton = true,
            onDismiss = { pickerOpen = false },
            onAddNewDish = { currentSelected ->
                // [AI修改] #51：去新建自定义菜品前，先把当前已勾选的菜提交到该餐次(合并)，
                // 这样返回重开选择器时(initialSelected=block.dishes)原选择保留，新建的也一并加入并选中。
                AppLogger.d("MealFlow", "navigate add new dish from picker: blockId=$blockId keepSelected=${currentSelected.map { it.id }}")
                if (blockId != null) vm.addDishes(blockId, currentSelected)
                pickerOpen = false // 跳转新建菜品前先关闭当前弹框；保存返回后重新打开并刷新菜品库。
                onAddNewDish()
            },
            onConfirm = { selected ->
                AppLogger.d("MealFlow", "dish picker confirm: blockId=$blockId selected=${selected.map { it.id }}") // [AI生成] 记录菜品选择确认结果。
                if (blockId != null) vm.addDishes(blockId, selected)
            },
        )
    }

    comboPickerBlockId?.let { blockId ->
        FavoriteComboPickerDialog(
            combos = state.favoriteCombos,
            onDismiss = { comboPickerBlockId = null },
            onPick = { combo ->
                vm.addComboDishes(blockId, combo)
                comboPickerBlockId = null
            },
        )
    }

    saveComboBlockId?.let { blockId ->
        SaveComboDialog(
            name = comboNameDraft,
            onNameChange = { comboNameDraft = it },
            onDismiss = { saveComboBlockId = null },
            onSave = {
                vm.saveCurrentBlockAsCombo(blockId, comboNameDraft)
                saveComboBlockId = null
            },
        )
    }
}

/**
 * 单个餐食模块卡片。[AI生成]
 *
 * 排版和首页/食历里的餐食卡片保持同类结构：上方餐次与时间，下方横向菜品。
 */
@Composable
private fun MealBlockCard(
    block: MealBlockUiState,
    mealTypes: List<MealType>,
    canRemove: Boolean,
    onMealTypeChange: (Long) -> Unit,
    onPickTime: () -> Unit,
    onAddDish: () -> Unit,
    onRemoveDish: (Long) -> Unit,
    onOpenDish: (Long) -> Unit, // [AI生成] F1：点菜品进详情
    onNoteChange: (String) -> Unit,
    onRemoveBlock: () -> Unit,
    onOpenCombos: () -> Unit,
    onSaveCombo: () -> Unit,
    onAiRecommend: () -> Unit,
    hasCombos: Boolean,
) {
    // [AI修改] 苹果风格：无阴影填充白卡，圆角 medium(12)。
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                MealTypeDropdown(
                    selectedId = block.mealTypeId,
                    mealTypes = mealTypes,
                    onMealTypeChange = onMealTypeChange,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                OutlinedButton(onClick = onPickTime) {
                    Icon(Icons.Outlined.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(block.mealTime?.formatForUi() ?: "选择时间")
                }
                if (canRemove) {
                    IconButton(onClick = onRemoveBlock) {
                        Icon(Icons.Outlined.Close, contentDescription = "删除餐食模块")
                    }
                }
            }

            if (block.mealTime == null) {
                Text(
                    "请选择用餐时间",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            if (block.dishes.isEmpty()) {
                Column {
                    TextButton(
                        onClick = onAddDish,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("添加菜品", color = MaterialTheme.colorScheme.primary) // [AI修改] 主动作 accent
                    }
                    TextButton(onClick = onAiRecommend, modifier = Modifier.fillMaxWidth()) { // [AI生成] 餐次块内 AI 推荐入口。
                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("AI 推荐", color = MaterialTheme.colorScheme.onSurfaceVariant) // [AI修改] 次动作中性
                    }
                    // [AI修改] A10：空块固定露出"从收藏组合选"入口(不再仅在有组合时才出现)——
                    // 无组合时点了给引导，让"整餐照搬"能力对新用户也可见。
                    TextButton(onClick = onOpenCombos, modifier = Modifier.fillMaxWidth()) {
                        Text(if (hasCombos) "从收藏组合选" else "从收藏组合选（先保存一餐为组合）", color = MaterialTheme.colorScheme.onSurfaceVariant) // [AI修改] 次动作中性
                    }
                }
            } else {
                // [AI修改] F2/F3：复用 MealDishGrid(4列+主食置顶+角标+点菜进详情)；右上角×移除由 overlay slot 注入。
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    MealDishGrid(
                        dishes = block.dishes,
                        onDishClick = { dish -> onOpenDish(dish.id) },
                        cellOverlay = { dish ->
                            IconButton(
                                onClick = { onRemoveDish(dish.id) },
                                modifier = Modifier.align(Alignment.TopEnd).size(24.dp),
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "移除菜品", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        },
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = onAddDish) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Text("添加菜品")
                        }
                        TextButton(onClick = onAiRecommend) { // [AI生成] 餐次块内 AI 推荐入口。
                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null)
                            Text("AI 推荐")
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (hasCombos) {
                        TextButton(onClick = onOpenCombos) { Text("选择组合") }
                    }
                    TextButton(onClick = onSaveCombo) { Text("保存组合") }
                }
            }

            OutlinedTextField(
                value = block.note,
                onValueChange = onNoteChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                placeholder = { Text("备注（可选）") },
                minLines = 1,
                maxLines = 2,
                shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
            )
        }
    }
}

/**
 * 收藏组合选择弹框。[AI生成]
 */
@Composable
private fun FavoriteComboPickerDialog(
    combos: List<FavoriteCombo>,
    onDismiss: () -> Unit,
    onPick: (FavoriteCombo) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择收藏组合") },
        text = {
            Column {
                if (combos.isEmpty()) {
                    // [AI修改] A10：空态给可操作引导——告诉用户怎么创建组合。
                    Text("还没有收藏组合。\n先在某餐次添加好几道菜，点「保存组合」存成组合，之后就能一键整餐照搬。", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    combos.forEach { combo ->
                        TextButton(
                            onClick = { onPick(combo) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("${combo.name}（${combo.dishes.size} 道菜）", modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/**
 * 保存当前餐食模块为收藏组合。[AI生成]
 */
@Composable
private fun SaveComboDialog(
    name: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("保存组合") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("组合名称") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = name.isNotBlank()) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun comboNameForBlock(date: LocalDate, block: MealBlockUiState, mealTypes: List<MealType>): String {
    val mealName = mealTypes.firstOrNull { it.id == block.mealTypeId }?.name ?: "餐食"
    val timeText = block.mealTime?.formatForUi() ?: "未定时间"
    return "${date} ${mealName} ${timeText}" // [AI修改] 默认组合名只保留日期、餐次、时间，菜品数量由弹框内容表达。
}

/**
 * 餐次下拉框。[AI生成]
 */
@Composable
private fun MealTypeDropdown(
    selectedId: Long?,
    mealTypes: List<MealType>,
    onMealTypeChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = mealTypes.firstOrNull { it.id == selectedId }?.name ?: "选择餐次"

    Box(modifier = modifier) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        ) {
            Text(
                selectedName,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(Icons.Outlined.ExpandMore, contentDescription = null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            mealTypes.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.name) },
                    onClick = {
                        onMealTypeChange(type.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

/**
 * 日期选择弹窗。[AI生成]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogContent(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
) {
    val zone = TimeZone.UTC
    // [AI修改] #2：Material3 1.1.2 无 SelectableDates，改在 setDate 里校验(已有餐食/早于下限则提示不切换)。
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initialDate.toUtcDateMillis(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val millis = state.selectedDateMillis ?: return@TextButton
                    onConfirm(Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone).date)
                },
            ) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    ) {
        DatePicker(state = state)
    }
}

/**
 * 时间选择弹窗。[AI生成]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialTime.hour,
        initialMinute = initialTime.minute,
        is24Hour = true,
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择用餐时间") },
        text = {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TimePicker(state = state)
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(LocalTime(state.hour, state.minute)) }) { Text("确定") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
    )
}

private fun LocalTime.formatForUi(): String {
    val hh = hour.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

private fun LocalDate.toUtcDateMillis(): Long =
    Instant.parse("${this}T00:00:00Z").toEpochMilliseconds()
