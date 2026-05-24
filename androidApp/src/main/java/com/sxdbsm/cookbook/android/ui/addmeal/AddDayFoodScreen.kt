package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Event
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
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
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
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
    editDate: LocalDate? = null,
    vm: AddMealViewModel = koinViewModel(),
) {
    // [AI修改] 页面订阅 ViewModel 状态，任何字段变化都会触发相关 UI 重组。
    val state by vm.state.collectAsStateWithLifecycle()
    var pickerOpen by rememberSaveable { mutableStateOf(false) }
    var pickingBlockId by rememberSaveable { mutableStateOf<Long?>(null) }
    var dateDialogOpen by rememberSaveable { mutableStateOf(false) }
    var timeDialogBlockId by rememberSaveable { mutableStateOf<Long?>(null) }

    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }
    LaunchedEffect(editDate) {
        editDate?.let { vm.setDate(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加餐食", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = { vm.save() },
                        enabled = state.canSave,
                    ) { Text(if (state.isPlan) "保存计划" else "保存") }
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
            FieldLabel("日期")
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

            FieldLabel("餐次")
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
                        pickerOpen = true
                    },
                    onRemoveDish = { dishId -> vm.removeDish(block.id, dishId) },
                    onNoteChange = { vm.setNote(block.id, it) },
                    onRemoveBlock = { vm.removeMealBlock(block.id) },
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
        DishPickerScreen(
            title = if (mealName.isBlank()) "添加菜品" else "添加到$mealName",
            multiSelect = true,
            initialSelected = emptyList(),
            excludeDishIds = block?.dishes?.map { it.id }?.toSet() ?: emptySet(),
            showRecentChips = true,
            showAddNewButton = true,
            onDismiss = { pickerOpen = false },
            onAddNewDish = onAddNewDish,
            onConfirm = { selected ->
                if (blockId != null) vm.addDishes(blockId, selected)
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
    onNoteChange: (String) -> Unit,
    onRemoveBlock: () -> Unit,
) {
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
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
                TextButton(
                    onClick = onAddDish,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("添加菜品", color = MaterialTheme.colorScheme.tertiary)
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    block.dishes.forEach { dish ->
                        Box {
                            DishMiniCard(dish = dish)
                            IconButton(
                                onClick = { onRemoveDish(dish.id) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(24.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "移除菜品",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }
                    TextButton(onClick = onAddDish) {
                        Icon(Icons.Outlined.Add, contentDescription = null)
                    }
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
            )
        }
    }
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

/**
 * 表单字段标题。[AI修改]
 */
@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

private fun LocalTime.formatForUi(): String {
    val hh = hour.toString().padStart(2, '0')
    val mm = minute.toString().padStart(2, '0')
    return "$hh:$mm"
}

private fun LocalDate.toUtcDateMillis(): Long =
    Instant.parse("${this}T00:00:00Z").toEpochMilliseconds()
