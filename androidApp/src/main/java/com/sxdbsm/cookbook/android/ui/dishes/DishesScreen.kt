package com.sxdbsm.cookbook.android.ui.dishes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.domain.model.DishMini
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

/**
 * 菜品列表页面。[AI修改]
 *
 * 头部固定显示“菜品 + 搜索 + 添加”，喜爱度横滑区随列表滚动，筛选 Tab 吸顶。[AI修改]
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DishesScreen(
    onAddDish: () -> Unit,
    onOpenDish: (Long) -> Unit,
    onEditDish: (Long) -> Unit,
    onCopyDish: (Long) -> Unit,
    vm: DishesViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var dropdownDish by remember { mutableStateOf<DishMini?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    var pullDistance by remember { mutableStateOf(0f) }
    val hotRankById = remember(ui.all) {
        ui.all
            .filter { it.preference > 0 }
            .sortedByDescending { it.preference }
            .take(3)
            .mapIndexed { index, dish -> dish.id to index + 1 }
            .toMap()
    } // [AI修改] 菜品 Item 右侧喜爱值前 3 名显示热度标识。
    val sections = remember(ui.all) {
        ui.all.groupBy { dishInitial(it.name) }.toSortedMap()
    }
    val showPopular = ui.popular.isNotEmpty() && ui.keyword.isBlank()
    val letterIndexMap = remember(sections, showPopular) {
        var index = 1 + if (showPopular) 2 else 0 // [AI修改] 首项为下拉刷新提示，需要计入索引偏移。
        index += 1 // TabRow sticky header
        buildMap {
            sections.forEach { (letter, dishes) ->
                put(letter, index)
                index += 1 + dishes.size
            }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                vm.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    } // [AI生成] 从新建/编辑菜品返回时主动刷新，补足非 Flow 搜索态列表。

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                ),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("菜品", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(12.dp))
                        AppSearchField(
                            value = ui.keyword,
                            onValueChange = vm::setKeyword,
                            placeholder = "搜索",
                            modifier = Modifier.weight(1f),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onAddDish) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加菜品")
                    }
                },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(ui.refreshing, listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
                    detectVerticalDragGestures(
                        onDragEnd = {
                            if (pullDistance > 80f && !ui.refreshing) vm.refresh()
                            pullDistance = 0f
                        },
                        onDragCancel = { pullDistance = 0f },
                    ) { _, dragAmount ->
                        val atTop = listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
                        if (atTop && dragAmount > 0f) pullDistance += dragAmount
                    }
                },
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                val refreshText = when {
                    ui.refreshing -> "刷新中..."
                    pullDistance > 80f -> "松开刷新"
                    else -> "下拉刷新"
                }
                Text(
                    text = refreshText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .padding(top = 6.dp),
                )
            }
            if (showPopular) {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("🔥 喜爱", style = MaterialTheme.typography.titleMedium)
                    }
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(ui.popular, key = { it.id }) { dish ->
                            DishMiniCard(dish = dish, onClick = { onOpenDish(dish.id) })
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            stickyHeader {
                TabRow(
                    selectedTabIndex = DishesSortTab.values().indexOf(ui.sortTab),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    listOf(
                        DishesSortTab.RECENT to "最近",
                        DishesSortTab.FAVORITE to "喜爱",
                        DishesSortTab.ALL to "全部",
                    ).forEach { (tab, label) ->
                        Tab(
                            selected = ui.sortTab == tab,
                            onClick = { vm.setSortTab(tab) },
                            text = { Text(label) },
                        )
                    }
                }
            }

            if (ui.all.isEmpty()) {
                item { EmptyState(text = "还没有菜品\n点击右上 + 号添加", icon = "🥗") }
            } else if (ui.sortTab == DishesSortTab.ALL) {
                sections.forEach { (letter, dishes) ->
                    item(key = "section-$letter") {
                        Text(
                            text = letter,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.background)
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                    }
                    items(dishes, key = { it.id }) { dish ->
                        DishRow(
                            dish = dish,
                            preferenceRank = hotRankById[dish.id],
                            onClick = { onOpenDish(dish.id) },
                            onLongClick = { dropdownDish = dish },
                        )
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            } else {
                itemsIndexed(ui.all, key = { _, dish -> dish.id }) { _, dish ->
                    DishRow(
                        dish = dish,
                        preferenceRank = hotRankById[dish.id],
                        onClick = { onOpenDish(dish.id) },
                        onLongClick = { dropdownDish = dish },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
            if (ui.sortTab == DishesSortTab.ALL && sections.isNotEmpty()) {
                LetterIndexBar(
                    letters = sections.keys.toList(),
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 4.dp),
                    onLetterSelected = { letter ->
                        letterIndexMap[letter]?.let { index ->
                            scope.launch { listState.animateScrollToItem(index) }
                        }
                    },
                )
            }
        }
    }

    dropdownDish?.let { d ->
        AlertDialog(
            onDismissRequest = { dropdownDish = null },
            title = { Text(d.name) },
            text = { Text("选择操作") },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    TextButton(onClick = {
                        dropdownDish = null
                        onEditDish(d.id)
                    }) { Text("编辑") }
                    TextButton(onClick = {
                        dropdownDish = null
                        onCopyDish(d.id)
                    }) { Text("基于此另存") }
                    TextButton(onClick = {
                        dropdownDish = null
                        vm.requestDeleteDish(d)
                    }) { Text("删除") }
                } // [AI修改] 弹框外部可关闭，不再额外展示“取消”；长按菜单补充编辑和删除。
            },
        )
    }

    val deleteState = ui.deleteState
    deleteState.warningDish?.let { dish ->
        AlertDialog(
            onDismissRequest = vm::dismissDeleteDialog,
            title = { Text("无法直接删除") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("“${dish.name}”已被以下餐食引用，请谨慎操作：")
                    deleteState.warningReferences.take(8).forEach { ref ->
                        Text("• ${ref.date} ${ref.mealName} ${ref.mealTime}")
                    }
                    if (deleteState.warningReferences.size > 8) {
                        Text("还有 ${deleteState.warningReferences.size - 8} 条引用未展示")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::dismissDeleteDialog) { Text("知道了") }
            },
        )
    }

    deleteState.pendingDish?.let { dish ->
        AlertDialog(
            onDismissRequest = vm::dismissDeleteDialog,
            title = { Text("删除菜品") },
            text = { Text("确认删除“${dish.name}”？删除后菜品列表中将不再展示。") },
            confirmButton = {
                TextButton(onClick = vm::confirmDeleteDish) {
                    Text(if (deleteState.checking) "删除中..." else "删除")
                }
            },
            dismissButton = {
                TextButton(onClick = vm::dismissDeleteDialog) { Text("取消") }
            },
        )
    }

    deleteState.errorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = vm::dismissDeleteDialog,
            title = { Text("操作失败") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = vm::dismissDeleteDialog) { Text("知道了") }
            },
        )
    }
}

@Composable
private fun LetterIndexBar(
    letters: List<String>,
    modifier: Modifier = Modifier,
    onLetterSelected: (String) -> Unit,
) {
    var active by remember { mutableStateOf<String?>(null) }
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f), MaterialTheme.shapes.small)
            .padding(vertical = 4.dp)
            .pointerInput(letters) {
                detectDragGestures(
                    onDragEnd = { active = null },
                    onDragCancel = { active = null },
                ) { change, _ ->
                    val index = ((change.position.y / size.height) * letters.size).toInt().coerceIn(0, letters.lastIndex)
                    active = letters[index]
                    onLetterSelected(letters[index])
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        letters.forEach { letter ->
            val selected = active == letter
            Text(
                text = letter,
                style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelSmall,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(if (selected) 28.dp else 22.dp)
                    .height(if (selected) 28.dp else 20.dp)
                    .clickable {
                        active = letter
                        onLetterSelected(letter)
                    },
            )
        }
    }
}
