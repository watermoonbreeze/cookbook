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
import androidx.compose.material.icons.outlined.MoreVert
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
import com.sxdbsm.cookbook.android.ui.component.MealDateCalendarDialog
import com.sxdbsm.cookbook.android.ui.component.MealDateCalendarSelectionMode
import com.sxdbsm.cookbook.android.ui.component.ActionSheet // [AI生成] 餐次简洁化:低频操作收纳(§9.11)
import com.sxdbsm.cookbook.android.ui.component.SheetAction
import com.sxdbsm.cookbook.android.ui.component.FormFieldLabel
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView // [AI生成] D保存预览:复用首页/食历餐食卡渲染草稿
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.domain.model.MealDayContent
import com.sxdbsm.cookbook.domain.projection.MealDayCardProjector
import com.sxdbsm.cookbook.domain.model.DishMini
import com.sxdbsm.cookbook.domain.model.FavoriteCombo
import com.sxdbsm.cookbook.domain.model.MealSection // [AI生成] D保存预览
import com.sxdbsm.cookbook.platform.BusinessTrace
import com.sxdbsm.cookbook.platform.TraceId
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
/**
 * [AI生成] F#7:餐次名(MealType.name)→ ai.MealSlot.code。按 label 精确匹配 + 少量常见别名；
 * 无匹配/无餐次→空串(=全部·不预选·不误推)。放 UI 层做展示态映射，不污染 shared ai.MealSlot。
 */
private fun aiSlotCodeForMealType(name: String?): String {
    val n = name?.trim().orEmpty()
    if (n.isEmpty()) return ""
    com.sxdbsm.cookbook.ai.MealSlot.values().firstOrNull { it != com.sxdbsm.cookbook.ai.MealSlot.ALL && it.label == n }?.let { return it.code }
    return when (n) {
        "午餐", "中饭", "午饭" -> com.sxdbsm.cookbook.ai.MealSlot.LUNCH.code
        "早饭" -> com.sxdbsm.cookbook.ai.MealSlot.BREAKFAST.code
        "晚饭" -> com.sxdbsm.cookbook.ai.MealSlot.DINNER.code
        "夜宵" -> com.sxdbsm.cookbook.ai.MealSlot.NIGHT_SNACK.code
        else -> "" // 加餐等无精确对应→全部(不误推特定餐次)
    }
}

/**
 * 记菜保存反馈文案：命中慢病关注点时与"已保存"合并成一条(take Top-1·定性不显数字)。[AI生成] 运营#177 ③
 *
 * 门禁裁决：apple_ux_designer(合并一条·§9.30) + copywriter(定性句·落脚统一"可留意"·不点病名/不判决/不出百分比数字·守热量个人概念红线)。
 * 无命中→"已保存"；有命中→"已保存 · {定性句}"。忌口带当前查看成员名(中性告知归属·承认可能给别的家人吃)。
 */
private fun saveResultSnackbarText(hint: com.sxdbsm.cookbook.ai.MealHealthHint?): String {
    val tail = when (hint?.kind) {
        com.sxdbsm.cookbook.domain.MealConcernKind.SODIUM -> "这餐钠偏高，可留意"
        com.sxdbsm.cookbook.domain.MealConcernKind.HIGH_GI -> "这餐升糖较快，可留意"
        com.sxdbsm.cookbook.domain.MealConcernKind.HIGH_PURINE -> "这餐嘌呤偏高，可留意"
        com.sxdbsm.cookbook.domain.MealConcernKind.HIGH_FAT -> "这餐油脂偏高，可留意"
        com.sxdbsm.cookbook.domain.MealConcernKind.AVOID ->
            hint.memberName?.takeIf { it.isNotBlank() }?.let { "这道在${it}的忌口清单里，可留意" } ?: "这道有人忌口，可留意"
        null -> return "已保存"
    }
    return "已保存 · $tail"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDayFoodScreen(
    onBack: () -> Unit,
    onAddNewDish: () -> Unit,
    onOpenDish: (Long) -> Unit = {}, // [AI生成] F1：点餐次里的菜进详情
    onOpenWeekPlan: (LocalDate) -> Unit = {},
    copyFromDate: LocalDate? = null, // [AI生成] F8：食历复制来源日期→预填新建草稿
    editDate: LocalDate? = null,
    createdDishId: Long? = null,
    presetDishIds: List<Long> = emptyList(), // [AI生成] AI 推荐"选它"带入的菜品(从首页进入时新建整餐)。
    onCreatedDishConsumed: () -> Unit = {},
    onOpenAiForBlock: (String) -> Unit = {}, // [AI生成] 从某餐次块进入 AI 推荐。[AI修改] F#7:带该块餐次的 ai.MealSlot.code(空=全部)。
    aiPickedDishIds: List<Long> = emptyList(), // [AI生成] AI 推荐回传给餐次块的菜品 id。
    lifecycleTraceId: String? = null,
    onAiPickedConsumed: () -> Unit = {},
    vm: AddMealViewModel = koinViewModel(),
    embedded: Boolean = false,
) {
    // [AI修改] 页面订阅 ViewModel 状态，任何字段变化都会触发相关 UI 重组。
    val state by vm.state.collectAsStateWithLifecycle()
    val mealDates by vm.mealDates.collectAsStateWithLifecycle()
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
    // [AI修改] 输入入口统一（用户2026-08-29）：AI 快捷记已收口到首页"+"统一入口，编辑/复制进入的
    //   添加餐食页不再提供 AI 快捷记（aiSheetOpen 等四态与顶栏入口随 AiMealInputSheet 调用一并移除）。
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
    // [AI修改] 家族化 P4/§1.4:保存成功统一走 Snackbar。保存后 onBack 立即离页→用全局 LocalAppSnackbar(MainScaffold 级·跨导航存活),
    //   在返回后的目标页(食历/首页)显示,替代原离页即散的 Toast。本地 snackbar 只用于留页的撤销条。
    val appSnackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current
    // [AI生成] §9.17：未保存返回守卫(复用 UnsavedGuard)——厨房场景误触返回易丢这餐编辑。
    val requestBack = if (embedded) {
        onBack
    } else {
        com.sxdbsm.cookbook.android.ui.component.rememberUnsavedGuard(
            isDirty = { vm.isDirty() },
            onConfirmLeave = onBack,
            dialogText = "你的改动还没保存，返回将丢失。", // [AI修改] 文案:统一到 UnsavedGuard 默认文案(守卫措辞全项目一致)
        )
    }

    // [AI生成] AI 推荐从餐次块进入并"选它"回传：把菜品加入发起的那个餐次块。
    LaunchedEffect(aiPickedDishIds) {
        if (aiPickedDishIds.isEmpty()) return@LaunchedEffect
        val target = aiTargetBlockId ?: state.activeBlockId
        // [AI生成] RESTORE：SavedStateHandle 已恢复子页面结果；Navigation 成功不等于状态恢复成功。
        val traceId = lifecycleTraceId?.let(TraceId::fromValue)
        traceId?.let {
            BusinessTrace.stateRestore(
                restoreSource = "ai_recommend",
                restoreResult = if (target != null) "success" else "missing_target",
                restoredFields = "ai_picked_dishes_active_block",
                traceId = it,
            )
        }
        if (target != null) {
            vm.addDishesByIds(target, aiPickedDishIds) { merged ->
                // [AI生成] 只有父 ViewModel 实际完成草稿合并后才记 merge 终态。
                traceId?.let { BusinessTrace.stateMergeResult("add_meal", "editing", "ai_picked_dishes", if (merged) "editing_dishes" else "merge_failed", it) }
                onAiPickedConsumed()
            }
        } else {
            onAiPickedConsumed()
        }
        aiTargetBlockId = null
    }

    LaunchedEffect(Unit) {
        AppLogger.d("MealFlow", "add_meal_screen_entered")
    }
    LaunchedEffect(state.done) {
        if (state.done) {
            AppLogger.d("MealFlow", "add_meal_completed block_count=${state.mealBlocks.size}")
            // [AI修改] 运营#177 ③：保存反馈与慢病轻提示合并成一条(有命中→"已保存 · {定性句}")。全局宿主·返回后目标页可见·弹完即走天然一次性。
            appSnackbar?.showMessage(saveResultSnackbarText(state.careHint))
            vm.consumeCareHint() // 消费一次性提示，避免重组重复。
            onBack()
        }
    }
    LaunchedEffect(editDate, presetDishIds, copyFromDate) {
        AppLogger.d("MealFlow", "add_meal_configure_requested preset_count=${presetDishIds.size}")
        if (copyFromDate != null) {
            vm.configureCopy(copyFromDate) // [AI生成] F8：食历复制→按来源日预填成新建草稿
        } else {
            vm.configure(editDate, presetDishIds) // [AI修改] 入口日期做初始化配置；带 AI 推荐预填时并入菜品。
        }
    }
    LaunchedEffect(createdDishId) {
        val dishId = createdDishId?.takeIf { it > 0 } ?: return@LaunchedEffect
        // [AI生成] RESTORE：新建菜品结果已从子页面回到当前餐食流程。
        val traceId = lifecycleTraceId?.let(TraceId::fromValue)
        traceId?.let { BusinessTrace.stateRestore("new_dish", "success", "created_dish_active_block", it) }
        AppLogger.d("MealFlow", "created_dish_consumed")
        vm.addCreatedDish(dishId, pickingBlockId ?: state.activeBlockId) { merged ->
            // [AI生成] 只有菜品读取并加入父草稿后才记 merge 成功。
            traceId?.let { BusinessTrace.stateMergeResult("add_meal", "editing", "created_dish", if (merged) "editing_dishes" else "merge_failed", it) }
            onCreatedDishConsumed()
        }
        pickerOpen = true // [AI修改] 返回后继续停留在菜品选择流程，并由 DishPicker 重新读取最新菜品列表。
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        snackbarHost = { SnackbarHost(snackbar) }, // [AI生成] A6：移除撤销
        topBar = {
            if (!embedded) {
            // [AI修改] B-8(§9.15)：带返回二级页统一 AppTopBar 收敛。[AI修改] 家族化 P3/§9.13:保存 CTA 从顶栏右上下移底部 FormBottomBar。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "添加餐食",
                onBack = requestBack, // [AI修改] §9.17：走未保存守卫
                // [AI修改] 输入入口统一（用户2026-08-29）：删除顶栏"AI快捷记"入口——AI 快捷记已
                //   统一到首页"+"（记录饮食统一入口），编辑/复制场景只需调整菜品。
            )
            }
        },
        // [AI修改] 家族化 P3(§9.13/基调§一.1):保存/主 CTA 永远在底部胶囊常驻。
        //   navBarPadding=false——本页是 MainScaffold 无底栏路由,已在 NavHost 层加过 navigationBarsPadding(见 MainScaffold:175),此处不再消费防双下边距。
        bottomBar = {
            com.sxdbsm.cookbook.android.ui.component.FormBottomBar(
                primaryText = if (state.isPlan) "保存计划" else "保存",
                // [AI修改] D-07(用户2026-07-18二次确认):所有保存餐食(记一餐/实录 与 计划)都先弹预览确认再存。
                onPrimary = { previewOpen = true },
                primaryEnabled = state.canSave,
                navBarPadding = false,
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
            // [AI修改] N3：编辑既有某天餐食时日期锁定不可改；[UX打磨]编辑态改**纯文本行**(不做成禁用按钮外观·免用户反复误点)，仅新增/复制可点改。
            if (state.isEditingExisting) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(state.date.toString(), modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("编辑中不可改", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                OutlinedButton(
                    onClick = { dateDialogOpen = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Outlined.Event, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(state.date.toString(), modifier = Modifier.weight(1f))
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
                        AppLogger.d("MealFlow", "dish_picker_opened selected_count=${block.dishes.size}")
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
                        // [AI生成] F#7:把本块餐次映射成 ai.MealSlot.code 带给 AI 推荐(按餐次名精确/别名匹配·无匹配→空=全部,不误推)。
                        onOpenAiForBlock(aiSlotCodeForMealType(state.mealTypes.firstOrNull { it.id == block.mealTypeId }?.name))
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
        MealDateCalendarDialog(
            mealDates = mealDates,
            initialDate = state.date,
            selectedDate = state.date,
            selectionMode = MealDateCalendarSelectionMode.ANY_VISIBLE_DATE,
            onDismiss = { dateDialogOpen = false },
            onDateClick = { date -> vm.setDate(date); dateDialogOpen = false },
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
        val mealType = state.mealTypes.firstOrNull { it.id == block?.mealTypeId }
        val mealName = mealType?.name.orEmpty()
        // [AI生成] v28:按当前餐次预筛选菜(默认只看适合该餐次,可切全部)。加餐(SNACK)/未知→ALL→不预筛。
        val pickerMealSlot = mealType?.code?.let { com.sxdbsm.cookbook.ai.MealSlot.fromCode(it) }
            ?.takeIf { it != com.sxdbsm.cookbook.ai.MealSlot.ALL }
        AppLogger.d("MealFlow", "dish_picker_composed selected_count=${block?.dishes?.size ?: 0} open=$pickerOpen")
        DishPickerScreen(
            title = if (mealName.isBlank()) "添加菜品" else "添加到$mealName",
            multiSelect = true,
            initialSelected = block?.dishes ?: emptyList(),
            excludeDishIds = emptySet(), // [AI修改] 当前餐次已有菜品在弹框内以勾选态展示，避免新建菜品回填后被排除导致看不到。
            showRecentChips = true,
            showAddNewButton = true,
            mealSlot = pickerMealSlot, // [AI生成] v28:记一餐按餐次预筛
            onDismiss = { pickerOpen = false },
            onAddNewDish = { currentSelected ->
                // [AI修改] #51：去新建自定义菜品前，先把当前已勾选的菜提交到该餐次(合并)，
                // 这样返回重开选择器时(initialSelected=block.dishes)原选择保留，新建的也一并加入并选中。
                AppLogger.d("MealFlow", "dish_picker_new_dish_navigation selected_count=${currentSelected.size}")
                if (blockId != null) vm.addDishes(blockId, currentSelected)
                pickerOpen = false // 跳转新建菜品前先关闭当前弹框；保存返回后重新打开并刷新菜品库。
                onAddNewDish()
            },
            onConfirm = { selected ->
                AppLogger.d("MealFlow", "dish_picker_confirmed selected_count=${selected.size}")
                if (blockId != null) vm.addDishes(blockId, selected)
            },
        )
    }

    comboPickerBlockId?.let { blockId ->
        FavoriteComboPickerSheet(
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
            MealDayCardProjector.project(
                MealDayContent(
                    date = state.date,
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
                ),
                com.sxdbsm.cookbook.util.DateTime.today(),
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

    // [AI修改] 输入入口统一（用户2026-08-29）：AI 快捷记 Sheet 打开块整体移除——AI 快捷记
    //   已统一到首页"+"（UnifiedAddMealScreen），编辑/复制进入的本页不再内嵌 AI 输入。
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
            // [AI修改] 餐次简洁化①③:头部 = 餐次 + 时间 + ⋯溢出菜单(低频操作收进 ActionSheet·§9.11)。
            var menuOpen by remember { mutableStateOf(false) }
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
                // [AI修改] 餐次简洁化①:删本块从头部常驻×收进 ⋯,与选/存组合同归低频菜单(去杂乱)。
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = "更多操作")
                }
            }
            if (menuOpen) {
                // [AI生成] 餐次简洁化①:选组合/存组合/删本块收纳。存组合仅本块有菜可选;删本块仅可删时出现。
                val actions = buildList {
                    add(SheetAction("从收藏组合选") { onOpenCombos() })
                    if (block.dishes.isNotEmpty()) add(SheetAction("存为组合") { onSaveCombo() })
                    if (canRemove) add(SheetAction("删除这个餐次", destructive = true) { onRemoveBlock() })
                }
                ActionSheet(
                    actions = actions,
                    onDismiss = { menuOpen = false },
                    title = mealTypes.firstOrNull { it.id == block.mealTypeId }?.name,
                )
            }

            // [AI修改] 移除"请选择用餐时间"死分支:newBlock/setMealType 恒给默认时间,mealTime 不会为 null。

            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(12.dp))

            // [AI生成] part1 餐次常吃 chips:排除本块已加菜后取前 8,点即入餐(少一步)。无候选则整行不渲染。
            // [AI修改] 审查建议2:纯派生态用 remember 缓存(依赖候选与本块已选)。
            val quickChips = remember(quickDishes, block.dishes) {
                quickDishes.filter { d -> block.dishes.none { it.id == d.id } }.take(8)
            }

            // [AI修改] 餐次简洁化③:展示区(上)——有菜显"已选N道"小标题+菜格(§9.12 ×可撤销移除);空块显轻引导。
            if (block.dishes.isNotEmpty()) {
                Text(
                    "已选 ${block.dishes.size} 道",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                // [AI修改] F2/F3:复用 MealDishGrid(4列+主食置顶+角标+点菜进详情);右上角×移除由 overlay slot 注入。
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
                Spacer(Modifier.height(12.dp))
                // [AI生成] 餐次简洁化③:半透 hairline 分隔"展示/操作"两区(视觉更一目了然)。
                Divider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))
            } else {
                Text(
                    "还没加菜",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 10.dp),
                )
            }

            // [AI修改] 餐次简洁化③④:操作区(下)——首行常吃 chips,其下加菜按钮(主=添加菜品accent/次=AI推荐中性)。选/存组合已移入 ⋯ 菜单。
            FrequentDishChips(dishes = quickChips, onAdd = onQuickAddDish)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(onClick = onAddDish, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加菜品") // 主动作:TextButton 默认 primary accent
                }
                TextButton(onClick = onAiRecommend, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("AI 推荐", color = MaterialTheme.colorScheme.onSurfaceVariant) // 次动作:中性
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
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(4.dp))
                    Text("备注", color = MaterialTheme.colorScheme.onSurfaceVariant) // [AI修改] 低频·中性(不与添加菜品 accent 争视觉)
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
 * 收藏组合选择器。[AI修改] UX深挖#11：AlertDialog→ModalBottomSheet(§7)——多选空间更大、可下滑关闭；
 * 底部常驻单 CTA(反映当前展开组合的选中数)，替代原每组内嵌多个添加按钮；空态给下一步(§9.6)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FavoriteComboPickerSheet(
    combos: List<FavoriteCombo>,
    onDismiss: () -> Unit,
    onConfirm: (FavoriteCombo, Set<Long>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // [AI修改] 选组合时展开列出菜品，可全选/部分选后再加入(不再整组直接加)。
    // [AI修改] 审查建议4：key 用 combos，防 combos 异步填充时 expandedId 恒 null 致 CTA 卡"展开一个组合"。
    var expandedId by remember(combos) { mutableStateOf<Long?>(combos.firstOrNull()?.id) }
    // 各组合的已选菜品(缺省=全选)；点某菜切换。
    val selected = remember { mutableStateMapOf<Long, Set<Long>>() }
    fun selOf(combo: FavoriteCombo): Set<Long> = selected[combo.id] ?: combo.dishes.map { it.id }.toSet()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
        ) {
            Text(
                "选择收藏组合",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            if (combos.isEmpty()) {
                com.sxdbsm.cookbook.android.ui.component.EmptyState(
                    text = "还没有收藏组合，先在某餐次加好几道菜、点「保存组合」，以后就能一键整餐照搬",
                    icon = "🍱",
                )
            } else {
                Text(
                    "挑一个组合，可只选其中几道加入这一餐",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
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
                        }
                        InsetHairline()
                    }
                }
                // 底部常驻单 CTA(§9.13)：反映当前展开组合的选中数；无展开组合则提示先展开。
                val expandedCombo = combos.firstOrNull { it.id == expandedId }
                val curSel = expandedCombo?.let { selOf(it) } ?: emptySet()
                Spacer(Modifier.height(16.dp))
                com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                    text = if (expandedCombo == null) "展开一个组合来选菜" else "添加所选（${curSel.size} 道）",
                    onClick = { expandedCombo?.let { onConfirm(it, curSel) } },
                    enabled = expandedCombo != null && curSel.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
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
