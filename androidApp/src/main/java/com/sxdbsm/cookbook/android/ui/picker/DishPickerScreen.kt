package com.sxdbsm.cookbook.android.ui.picker

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
import com.sxdbsm.cookbook.ai.MealSlot // [AI生成] v28:记一餐按餐次预筛
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl // [AI生成] C深度:分类Tab
import com.sxdbsm.cookbook.android.ui.dishes.DishesSortTab // [AI生成] C深度:复用分类Tab枚举
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.TagChip
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.SectionHeader
import com.sxdbsm.cookbook.domain.model.DishMini
import org.koin.androidx.compose.koinViewModel

/**
 * 菜品选择全屏弹窗。[AI修改]
 *
 * 可配置为单选或多选，常用于添加餐食、导入菜品等场景。
 * [AI修改] 底部常驻新建行有搜索词时改用统一 SearchCreateRow("新建菜品「X」")，空词仍显"添加菜品"。
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
    mealSlot: MealSlot? = null, // [AI生成] v28:记一餐传入当前餐次→按餐次预筛(默认只看适合,可切全部);其他入口 null 不预筛
    onDismiss: () -> Unit,
    onAddNewDish: (List<DishMini>) -> Unit = {}, // [AI修改] 带出当前已勾选，供上层先保留再去新建
    onConfirm: (List<DishMini>) -> Unit,
    vm: DishPickerViewModel = koinViewModel(),
) {
    // [AI修改] 选择器内部状态由 ViewModel 管理，外部只传初始选中和排除列表。
    val state by vm.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    // [AI生成] 修复"选菜品搜索无结果→添加菜品未带搜索词":去新建前把当前搜索关键词写入预填总线(与全局搜索一致)。
    val prefillBus = org.koin.compose.koinInject<com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefillBus>()

    /**
     * 外部参数变化时重新配置选择器。[AI修改]
     */
    LaunchedEffect(excludeDishIds, initialSelected, mealSlot) {
        vm.configure(excludeDishIds, initialSelected, mealSlot)
    }

    /**
     * 弹窗每次重新进入组合时刷新列表。[AI修改]
     *
     * 从“添加菜品”跳到新建菜品并返回后，刚创建的菜品会按创建时间出现在列表前方。
     */
    LaunchedEffect(Unit) {
        vm.refresh(force = true) // [AI修改] 每次重新打开菜品库都强制读取数据库，确保刚新建的菜品立即出现在列表中。
    }

    DisposableEffect(lifecycleOwner, showAddNewButton) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && showAddNewButton) {
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
                // [AI修改] §9.30 P0-2:新建入口与选食材对齐——无词=顶栏➕、有词=结果列表末尾 SearchCreateRow(带入搜索词)。
                //   onAddNew 提到此处供顶栏➕与列表末尾行共用(原只在底部 Surface 内)。
                val onAddNew = {
                    if (state.keyword.isNotBlank()) {
                        prefillBus.request(com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefill(name = state.keyword.trim()))
                    }
                    onAddNewDish(vm.confirmSelected()) // 带出当前已勾选，去新建前先保留
                }
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.primary,
                        actionIconContentColor = MaterialTheme.colorScheme.primary,
                    ), // [AI修改] 菜品选择弹窗顶栏按暖杏规范使用背景一体化样式。
                    // [AI修改] N6：搜索框融入标题栏(标题栏放下 添加到餐次·搜索框·完成)；原搜索框位置改放已选菜品。
                    title = {
                        AppSearchField(
                            value = state.keyword,
                            onValueChange = vm::setKeyword,
                            placeholder = if (title.isBlank()) "搜索菜品" else "$title·搜索菜品",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        // [AI修改] §9.30 P1(用户2026-07-20#3真机反馈):顶栏只留新建➕(无词时),"完成"下移底部(与选食材统一·治顶栏挤)。
                        if (showAddNewButton && state.keyword.isBlank()) {
                            IconButton(onClick = onAddNew) {
                                Icon(Icons.Outlined.Add, contentDescription = "新建菜品")
                            }
                        }
                    },
                )

                // [AI修改] N6：原搜索框位置改为展示"已选菜品"(第一行菜名、第二行标签，横向滚动)。
                if (multiSelect && state.selected.isNotEmpty()) {
                    SelectedDishesBar(selected = state.selected, onRemove = { vm.toggle(it, true) })
                }

                // [AI生成] v28:记一餐按当前餐次预筛入口——一枚可切 chip "只看适合早餐 (N) / 全部"(告知不替决定,不硬隐藏)。仅传入餐次且无搜索时显。
                if (state.mealSlot != null && state.keyword.isBlank()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterChip(
                            selected = state.mealSlotOnly,
                            onClick = { vm.toggleMealSlotOnly() },
                            label = { Text("只看适合${state.mealSlot!!.label} (${state.mealSlotMatchCount})") },
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (state.mealSlotOnly) "点一下看全部" else "已显示全部",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                // [AI生成] C深度:选择菜品加分类导航(与菜品页/选择食材统一操作逻辑)。搜索时隐藏(搜索是全局的);菜系用横向chip(弹窗宽度受限,不用左竖栏)。
                if (state.keyword.isBlank()) {
                    val pickerTabs = listOf(
                        DishesSortTab.RECENT to "最近",
                        DishesSortTab.FAVORITE to "喜爱",
                        DishesSortTab.ALL to "菜系",
                        DishesSortTab.HOME to "家庭",
                    )
                    SegmentedControl(
                        options = pickerTabs.map { it.second },
                        selectedIndex = pickerTabs.indexOfFirst { it.first == state.sortTab }.coerceAtLeast(0),
                        onSelect = { idx -> vm.setSortTab(pickerTabs[idx].first) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                    if (state.sortTab == DishesSortTab.ALL && state.availableCuisines.isNotEmpty()) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(bottom = 4.dp),
                        ) {
                            item(key = "cuisine-all") {
                                FilterChip(selected = state.selectedCuisine == null, onClick = { vm.selectCuisine(null) }, label = { Text("全部") })
                            }
                            items(state.availableCuisines, key = { it }) { c ->
                                FilterChip(
                                    selected = state.selectedCuisine == c,
                                    onClick = { vm.selectCuisine(if (state.selectedCuisine == c) null else c) },
                                    label = { Text(c) },
                                )
                            }
                        }
                    }
                }

                // [AI修改] 2026-07-19(用户反馈):移除"最近"Tab 下的"最近常吃"+"喜爱"两个快捷区——主分类 Tab 已有"最近"和"喜爱",此处重复冗余。
                if (state.dishes.isEmpty()) {
                    // [AI修改] C深度审查🟡:空态按 Tab/搜索给具体引导(苹果式空态给下一步),而非泛化"没有找到菜品"。
                    val emptyText = when {
                        state.keyword.isNotBlank() -> "没有找到「${state.keyword.trim()}」"
                        state.sortTab == DishesSortTab.HOME -> "还没有自建菜\n点右上角 ＋ 新建一道" // [AI修改] UX走查M7:入口已改顶栏➕(§9.30),指引对齐
                        state.sortTab == DishesSortTab.FAVORITE -> "还没有喜爱的菜\n给菜品评分后会出现在这里"
                        state.sortTab == DishesSortTab.ALL && state.selectedCuisine != null -> "这个菜系下暂无菜品"
                        else -> "还没有菜品"
                    }
                    EmptyState(text = emptyText, icon = "🥗")
                    // [AI修改] §9.30 P0-2:0结果+有搜索词→紧接空态给"新建菜品「词」"末尾行(与选食材覆盖层一致)。
                    if (showAddNewButton && state.keyword.trim().isNotBlank()) {
                        com.sxdbsm.cookbook.android.ui.component.SearchCreateRow(
                            keyword = state.keyword,
                            entity = "菜品",
                            onClick = onAddNew,
                        )
                    }
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(state.dishes, key = { it.id }) { dish ->
                            DishRow(
                                dish = dish,
                                showCheckbox = multiSelect,
                                checked = state.selected.any { it.id == dish.id },
                                onCheckedChange = { vm.toggle(dish, multiSelect) },
                                onClick = {
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                            )
                        }
                        // [AI修改] §9.30 P0-2:有搜索词→结果列表末尾常驻"新建菜品「词」"行(§9.19·带入词·替代原底部 Surface)。
                        if (showAddNewButton && state.keyword.trim().isNotBlank()) {
                            item(key = "dish-create-row") {
                                com.sxdbsm.cookbook.android.ui.component.SearchCreateRow(
                                    keyword = state.keyword,
                                    entity = "菜品",
                                    onClick = onAddNew,
                                )
                            }
                        }
                    }
                }
                // [AI生成] §9.30 P1(用户2026-07-20#3真机):多选"完成"下移底部(与选食材 SelectionBottomBar 统一·顶栏不再放完成防挤)。
                if (multiSelect) {
                    Surface(tonalElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { onConfirm(vm.confirmSelected()); onDismiss() },
                            enabled = state.selected.isNotEmpty(),
                            modifier = Modifier
                                .navigationBarsPadding()
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        ) { Text(if (state.selected.isEmpty()) "完成" else "完成（已选 ${state.selected.size}）") }
                    }
                }
            }
        }
    }
}

/**
 * 已选菜品横滑条。[AI生成] N6
 *
 * 每个已选菜一列：第一行菜名、第二行标签；点右上角×取消选择；整体横向滚动。
 */
@Composable
private fun SelectedDishesBar(selected: List<DishMini>, onRemove: (DishMini) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selected.forEach { dish ->
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, end = 2.dp, top = 2.dp, bottom = 2.dp)) {
                        Column {
                            Text(dish.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer, maxLines = 1)
                            if (dish.tags.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    dish.tags.take(3).forEach { tag -> TagChip(tag) }
                                }
                            }
                        }
                        IconButton(onClick = { onRemove(dish) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Outlined.Close, contentDescription = "取消选择", modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
            }
        }
    }
}
