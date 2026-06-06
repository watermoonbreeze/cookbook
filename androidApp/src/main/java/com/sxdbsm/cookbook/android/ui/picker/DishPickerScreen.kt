package com.sxdbsm.cookbook.android.ui.picker

import com.sxdbsm.cookbook.android.util.AppLogger
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.SectionHeader
import com.sxdbsm.cookbook.domain.model.DishMini
import org.koin.androidx.compose.koinViewModel

/**
 * 菜品选择全屏弹窗。[AI修改]
 *
 * 可配置为单选或多选，常用于添加餐食、导入菜品等场景。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishPickerScreen(
    title: String,
    multiSelect: Boolean,
    initialSelected: List<DishMini>,
    excludeDishIds: Set<Long>,
    showRecentChips: Boolean,
    showAddNewButton: Boolean,
    onDismiss: () -> Unit,
    onAddNewDish: () -> Unit = {},
    onConfirm: (List<DishMini>) -> Unit,
    vm: DishPickerViewModel = koinViewModel(),
) {
    // [AI修改] 选择器内部状态由 ViewModel 管理，外部只传初始选中和排除列表。
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        AppLogger.d("DishPickerFlow", "open picker: title=$title multi=$multiSelect initial=${initialSelected.map { it.id }} exclude=$excludeDishIds showAdd=$showAddNewButton") // [AI生成] 记录菜品选择器入口参数。
    }
    /**
     * 外部参数变化时重新配置选择器。[AI修改]
     */
    LaunchedEffect(excludeDishIds, initialSelected) {
        AppLogger.d("DishPickerFlow", "configure picker: initial=${initialSelected.map { it.id }} exclude=$excludeDishIds") // [AI生成] 记录选择器配置变化，排查已选菜品是否传入。
        vm.configure(excludeDishIds, initialSelected)
    }

    /**
     * 弹窗每次重新进入组合时刷新列表。[AI修改]
     *
     * 从“添加菜品”跳到新建菜品并返回后，刚创建的菜品会按创建时间出现在列表前方。
     */
    LaunchedEffect(Unit) {
        AppLogger.d("DishPickerFlow", "refresh picker on open: title=$title") // [AI生成] 记录每次打开选择器强制刷新。
        vm.refresh(force = true) // [AI修改] 每次重新打开菜品库都强制读取数据库，确保刚新建的菜品立即出现在列表中。
    }

    DisposableEffect(lifecycleOwner, showAddNewButton) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && showAddNewButton) {
                AppLogger.d("DishPickerFlow", "refresh picker on resume: title=$title selected=${state.selected.map { it.id }}") // [AI生成] 从新建菜品返回等恢复场景强制刷新。
                vm.refresh(force = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
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
                    ), // [AI修改] 菜品选择弹窗顶栏按暖杏规范使用背景一体化样式。
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        if (multiSelect) {
                            Button(
                                onClick = {
                                    AppLogger.d("DishPickerFlow", "confirm picker: title=$title selected=${vm.confirmSelected().map { it.id }}") // [AI生成] 记录确认返回给上层的菜品 id。
                                    onConfirm(vm.confirmSelected())
                                    onDismiss()
                                },
                                enabled = state.selected.isNotEmpty(),
                            ) { Text("完成") }
                            Spacer(Modifier.width(8.dp))
                        }
                    },
                )

                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = vm::setKeyword,
                    placeholder = { Text("搜索菜名 / 标签 / 食材...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium, // [AI修改] 输入框圆角按新暖杏规范统一为 12dp。
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                if (showRecentChips && state.recent.isNotEmpty() && state.keyword.isBlank()) {
                    SectionHeader(title = "最近常吃", compact = true)
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.recent.take(8).forEach { dish ->
                            FilterChip(
                                selected = state.selected.any { it.id == dish.id },
                                onClick = {
                                    AppLogger.d("DishPickerFlow", "toggle recent chip: dishId=${dish.id} multi=$multiSelect") // [AI生成] 记录最近菜品点击。
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                                label = { Text(dish.name) },
                            )
                        }
                    }
                }

                if (state.popular.isNotEmpty() && state.keyword.isBlank()) {
                    SectionHeader(title = "喜爱", compact = true)
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.popular, key = { it.id }) { dish ->
                            DishMiniCard(
                                dish = dish,
                                onClick = {
                                    AppLogger.d("DishPickerFlow", "toggle popular block: dishId=${dish.id} multi=$multiSelect") // [AI生成] 记录喜爱菜品点击。
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (state.dishes.isEmpty()) {
                    EmptyState(text = "没有找到菜品", icon = "🥗")
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(state.dishes, key = { it.id }) { dish ->
                            DishRow(
                                dish = dish,
                                showCheckbox = multiSelect,
                                checked = state.selected.any { it.id == dish.id },
                                onCheckedChange = { vm.toggle(dish, multiSelect) },
                                onClick = {
                                    AppLogger.d("DishPickerFlow", "toggle row: dishId=${dish.id} multi=$multiSelect") // [AI生成] 记录列表菜品点击。
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                }

                if (showAddNewButton) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(
                            onClick = {
                                AppLogger.d("DishPickerFlow", "add new dish click: title=$title selected=${state.selected.map { it.id }}") // [AI生成] 记录从菜品选择器跳转新建菜品。
                                onAddNewDish()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("添加菜品", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}
