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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import kotlinx.coroutines.Job
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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.MealDishGrid
import com.sxdbsm.cookbook.android.ui.component.FormFieldLabel
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView // [AI生成] D保存预览:复用首页/食历餐食卡渲染草稿
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.domain.model.DayMealCardData // [AI生成] D保存预览
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.FavoriteCombo
import com.sxdbsm.cookbook.domain.model.MealSection // [AI生成] D保存预览
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
    val frequentDishes by vm.frequentDishes.collectAsStateWithLifecycle() // [AI生成] part1：餐次块"常吃"一键 chips 候选
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickingBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    var timeDialogBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comboPickerBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var saveComboBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var comboNameDraft by rememberSaveable { mutableStateOf("") }
    var aiTargetBlockId by rememberSaveable { mutableStateOf<Long?>(null) } // [AI生成] 记录哪个餐次块发起了 AI 推荐。
    var previewOpen by rememberSaveable { mutableStateOf(false) } // [AI生成] D保存预览:点"保存计划"先弹预览sheet,确认再存(仅计划态,实录高频不加确认=守少操作)
    val snackbar = remember { SnackbarHostState() } // [AI生成] A6：移除菜品撤销提示
    val scope = rememberCoroutineScope()
    // [AI生成] part1/审查建议1(§9.12 红线)：撤销 Snackbar **单 job 串行化**——连点多 chip/连续操作时，
    //   cancel 前一个再 show，避免新提示无声挤掉旧撤销条(否则连加 A/B/C 只剩 C 可撤，A/B 撤销窗口被吞)。
    var undoJob by remember { mutableStateOf<Job?>(null) }
    val showUndoLocal: (String, () -> Unit) -> Unit = { message, onUndo ->
        undoJob?.cancel()
        undoJob = scope.launch {
            val res = snackbar.showSnackbar(message = message, actionLabel = "撤销", duration = SnackbarDuration.Short)
            if (res == SnackbarResult.ActionPerformed) onUndo()
        }
    }
    val context = androidx.compose.ui.platform.LocalContext.current
    // [AI生成] §9.17：未保存返回守卫(复用 UnsavedGuard)——厨房场景误触返回易丢这餐编辑。
    val requestBack = com.sxdbsm.cookbook.android.ui.component.rememberUnsavedGuard(
        isDirty = { vm.isDirty() },
        onConfirmLeave = onBack,
        dialogText = "你的改动还没保存，返回将丢失。", // [AI修改] 文案:统一到 UnsavedGuard 默认文案(守卫措辞全项目一致)
    )

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
            // [AI修改] B-8(§9.15)：带返回二级页统一 AppTopBar 收敛。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "添加餐食",
                onBack = requestBack, // [AI修改] §9.17：走未保存守卫
                actions = {
                    com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                        text = if (state.isPlan) "保存计划" else "保存",
                        // [AI修改] D保存预览:保存/编辑餐食均先弹预览(复用餐食卡)确认再存(用户要求:不只保存计划要预览)。
                        onClick = { previewOpen = true },
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
                        // [AI修改] 审查建议1：改走单 job 串行化 showUndoLocal(防连续操作挤丢撤销)。
                        val removed = block.dishes.firstOrNull { it.id == dishId }
                        vm.removeDish(block.id, dishId)
                        if (removed != null) {
                            showUndoLocal("已移除「${removed.name}」") { vm.addDishes(block.id, listOf(removed)) }
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
                    quickDishes = frequentDishes, // [AI生成] part1：常吃 chips 候选(组件内再排除本块已加)
                    onQuickAddDish = { dish ->
                        // [AI生成] part1：点 chip 即入餐 + Snackbar 撤销(误点高频，撤销优于确认，单 job 串行化防连点挤丢)。
                        vm.addDishes(block.id, listOf(dish))
                        showUndoLocal("已加入「${dish.name}」") { vm.removeDish(block.id, dish.id) }
                    },
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
            onConfirm = { combo, selectedIds ->
                vm.addComboDishes(blockId, combo, selectedIds)
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

    // [AI生成] D保存预览:点"保存计划"→底部 sheet 用首页/食历同款餐食卡渲染草稿→确认再存(§7 底部sheet承载确认,§9.12 撤销优于确认此处为"保存前核对"故用确认)。
    //   仅计划态触发(实录直存),按钮 enabled=canSave 已保证非空草稿,故此处不会弹空卡。
    if (previewOpen) {
        val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        // [AI修改] Google审查B1:预览过滤口径必须与 vm.save() 一致(用 it.canSave,含 mealTime!=null 那一维)——
        //   否则 mealTime 为 null 的餐次会"预览显示但保存被静默丢弃"。canSave 保证 mealTypeId/mealTime 非空,故映射内用 !! 不再兜底。
        //   B3:派生态 remember 缓存(对齐项目规范,避免 sheet 打开期反复重建)。
        val previewData = remember(state.mealBlocks, state.mealTypes, state.date, state.isPlan) {
            DayMealCardData(
                date = state.date,
                isToday = state.date == com.sxdbsm.cookbook.util.DateTime.today(),
                isPlanState = state.isPlan,
                meals = state.mealBlocks
                    .filter { it.canSave } // 与 save() 单一真相源一致:mealTypeId!=null && mealTime!=null && dishes 非空
                    .sortedBy { it.mealTime } // [AI生成] 用户反馈:添加时餐次顺序可能乱,预览按正常时间序展示(仅展示序,不影响保存)
                    .map { b ->
                        MealSection(
                            mealTypeId = b.mealTypeId!!,
                            mealName = state.mealTypes.firstOrNull { it.id == b.mealTypeId }?.name ?: "",
                            mealTime = b.mealTime!!,
                            dishes = b.dishes,
                            note = b.note,
                        )
                    },
            )
        }
        ModalBottomSheet(
            onDismissRequest = { previewOpen = false },
            sheetState = previewSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    "保存前预览",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                Text(
                    "确认无误后保存，也可返回修改",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                // 只读预览:不传编辑/复制/删除回调——能力显隐由回调决定(§9.3),预览态无这些操作。
                DayMealCardView(data = previewData)
                Spacer(Modifier.height(16.dp))
                com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                    text = "确定保存",
                    onClick = {
                        previewOpen = false
                        vm.save()
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(
                    onClick = { previewOpen = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    Text("返回修改", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/**
 * 单个餐食模块卡片。[AI生成]
 *
 * 排版和首页/食历里的餐食卡片保持同类结构：上方餐次与时间，下方横向菜品。
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class) // [AI生成] FilterChip 在 M3 1.1.2 为实验 API
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
    quickDishes: List<DishMini> = emptyList(), // [AI生成] part1：本块"常吃"一键 chips 候选(全局常吃菜，本组件再排除本块已加)
    onQuickAddDish: (DishMini) -> Unit = {}, // [AI生成] part1：点 chip 即把该菜加入本块(点后移出列表=天然反馈)
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

            // [AI修改] 移除"请选择用餐时间"死分支：newBlock/setMealType 恒给默认时间(固定餐次defaultTime/加餐nowTime)，mealTime 不会为 null。

            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(10.dp))

            // [AI生成] part1 餐次常吃 chips：排除本块已加菜后取前 8，点即入餐(少一步)。无候选则整行不渲染(不占位)。
            // [AI修改] 审查建议2：纯派生态用 remember 缓存(依赖候选与本块已选)，免每次重组重算。
            val quickChips = remember(quickDishes, block.dishes) {
                quickDishes.filter { d -> block.dishes.none { it.id == d.id } }.take(8)
            }

            if (block.dishes.isEmpty()) {
                Column {
                    FrequentDishChips(dishes = quickChips, onAdd = onQuickAddDish) // [AI生成] 空块:chips 在"添加菜品"上方
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
                    FrequentDishChips(dishes = quickChips, onAdd = onQuickAddDish) // [AI生成] 有菜块:chips 在已有网格下方、"添加菜品"上方
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

            // [AI修改] 备注默认收起(低频、多块时空备注框拉长页面)：点"+ 备注"才展开；已有备注则默认展开。
            var noteExpanded by remember(block.id) { mutableStateOf(block.note.isNotBlank()) }
            if (noteExpanded) {
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
            } else {
                TextButton(onClick = { noteExpanded = true }, modifier = Modifier.padding(top = 4.dp)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("备注")
                }
            }
        }
    }
}

/**
 * 餐次块内"常吃"一键 chips 行。[AI生成] part1 餐次常吃 chips
 *
 * 家庭高频记菜提速：点 chip 即把该常吃菜加入本块(免开选择器，4 步→1 步)。
 * 无候选(新用户/都已加)则整行不渲染(不占纵向空间)。点后该菜被上层过滤移出列表=天然"已加"反馈，撤销由调用方 Snackbar 承接。
 * 设计门禁偏离说明：未用 SectionHeader(其自带 16dp padding+titleSmall 会在卡内 16dp padding 下双重内缩/字号偏大)，
 *   改用克制内联小标签(labelMedium/onSurfaceVariant)，更贴卡内紧凑场景、更苹果式克制。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequentDishChips(
    dishes: List<DishMini>,
    onAdd: (DishMini) -> Unit,
) {
    if (dishes.isEmpty()) return // 无候选不占位
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Text(
            "常吃",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 6.dp),
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            dishes.forEach { dish ->
                FilterChip(
                    selected = false, // 点即入餐并移出列表，无常驻选中态
                    onClick = { onAdd(dish) },
                    label = {
                        Text(
                            dish.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.widthIn(max = 160.dp), // 超长菜名截断防撑满整行
                        )
                    },
                    leadingIcon = {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                )
            }
            Spacer(Modifier.width(8.dp)) // 末尾留白提示可横滚
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
    onConfirm: (FavoriteCombo, Set<Long>) -> Unit,
) {
    // [AI修改] 选组合时展开列出菜品，可全选/部分选后再加入(不再整组直接加)。
    var expandedId by remember { mutableStateOf<Long?>(combos.firstOrNull()?.id) }
    // 各组合的已选菜品(缺省=全选)；点某菜切换。
    val selected = remember { mutableStateMapOf<Long, Set<Long>>() }
    fun selOf(combo: FavoriteCombo): Set<Long> = selected[combo.id] ?: combo.dishes.map { it.id }.toSet()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择收藏组合") },
        text = {
            if (combos.isEmpty()) {
                Text("还没有收藏组合。\n先在某餐次添加好几道菜，点「保存组合」存成组合，之后就能一键整餐照搬。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                ) {
                    combos.forEach { combo ->
                        val expanded = expandedId == combo.id
                        val cur = selOf(combo)
                        // 组合标题行：点开/收起
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expandedId = if (expanded) null else combo.id }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("${combo.name}（${combo.dishes.size} 道菜）", modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                            Icon(
                                Icons.Outlined.ExpandMore,
                                contentDescription = if (expanded) "收起" else "展开",
                                modifier = Modifier.rotate(if (expanded) 180f else 0f),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (expanded) {
                            val allChecked = cur.size == combo.dishes.size && combo.dishes.isNotEmpty()
                            // 全选行
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selected[combo.id] = if (allChecked) emptySet() else combo.dishes.map { it.id }.toSet()
                                    }
                                    .padding(start = 8.dp, top = 2.dp, bottom = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Checkbox(checked = allChecked, onCheckedChange = {
                                    selected[combo.id] = if (allChecked) emptySet() else combo.dishes.map { it.id }.toSet()
                                })
                                Text("全选", style = MaterialTheme.typography.bodyMedium)
                            }
                            combo.dishes.forEach { dish ->
                                val checked = dish.id in cur
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected[combo.id] = if (checked) cur - dish.id else cur + dish.id
                                        }
                                        .padding(start = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(checked = checked, onCheckedChange = {
                                        selected[combo.id] = if (checked) cur - dish.id else cur + dish.id
                                    })
                                    Text(dish.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                }
                            }
                            Button(
                                onClick = { onConfirm(combo, cur) },
                                enabled = cur.isNotEmpty(),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 4.dp),
                            ) {
                                Text("添加所选（${cur.size} 道）")
                            }
                        }
                        InsetHairline()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

/** 组合项之间的细分隔。[AI生成] Material3 1.1.2 用 Divider(无 HorizontalDivider)。 */
@Composable
private fun InsetHairline() {
    Divider(
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
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
