package com.sxdbsm.cookbook.android.ui.dishes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.SourceBadge
import androidx.compose.material3.Surface
import androidx.compose.material3.Divider
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
    val prefillBus = org.koin.compose.koinInject<com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefillBus>() // [AI生成] 搜索无结果新建菜品预填
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var dropdownDish by remember { mutableStateOf<DishMini?>(null) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val lifecycleOwner = LocalLifecycleOwner.current
    val hotRankById = remember(ui.popular) {
        // [AI修改] 热度前3取自全局 popular(preference DESC)，而非受"最近30/筛选"缩窄的 ui.all，保持全局语义。
        ui.popular
            .filter { it.preference > 0 }
            .sortedByDescending { it.preference }
            .take(3)
            .mapIndexed { index, dish -> dish.id to index + 1 }
            .toMap()
    } // 菜品 Item 右侧喜爱值前 3 名显示热度标识。
    val sections = remember(ui.all) {
        ui.all.groupBy { dishInitial(it.name) }.toSortedMap()
    }
    val hasFilterRow = ui.availableMethods.isNotEmpty() || ui.availableTags.isNotEmpty()
    val isCuisineTab = ui.sortTab == DishesSortTab.ALL // [AI修改] 第三档=菜系(左二级栏+右菜品)
    // [AI修改] B-7：菜系档列表头部现有 4 个 item(搜索/Tab/计数/筛选)在字母 section 之前，字母跳转索引须从 4 起算。
    // 红线：新增/条件插入 item 必须同步偏移量并纳入 remember key，否则跳转偏位。
    val letterHeaderCount = 4
    val letterIndexMap = remember(sections, letterHeaderCount) {
        var index = letterHeaderCount
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
                vm.loadFavorites() // [AI修改] 详情页收藏/取消后返回列表即时刷新置顶。
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    } // [AI生成] 从新建/编辑菜品返回时主动刷新，补足非 Flow 搜索态列表。

    // [AI修改] B-7(§9.15)：Tab 落地页改大标题(下滑折叠)；搜索行/Tab/计数/筛选下沉进列表头部作折叠燃料。
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 避免页面 Scaffold 和根 Scaffold 重复避让系统栏。
        topBar = {
            LargeTopAppBar(
                title = { Text("菜品", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                actions = {
                    IconButton(onClick = onAddDish) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加菜品")
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { padding ->
        // [AI修改] 空态文案按原因：筛选无果 / 喜爱页无评分 / 真无菜(引导添加)。
        val filtersActive = ui.selectedMethod != null || ui.selectedTag != null ||
            (isCuisineTab && ui.selectedCuisine != null) || ui.keyword.isNotBlank()
        val emptyText = when {
            filtersActive -> "没有符合筛选的菜品"
            ui.sortTab == DishesSortTab.FAVORITE -> "还没有喜爱的菜品\n给菜品评分后会出现在这里"
            else -> "还没有菜品\n点击右上角 + 添加"
        }
        // [AI修改] B-7：Tab/计数下沉进列表头部(dishHeaderItems)作大标题折叠燃料，与搜索行同随内容滚走(§9.15)。
        val sortTabs = listOf(
            DishesSortTab.RECENT to "最近",
            DishesSortTab.FAVORITE to "喜爱",
            DishesSortTab.ALL to "菜系",
            DishesSortTab.HOME to "家庭",
        )
        val tabCount = when (ui.sortTab) {
            DishesSortTab.RECENT -> ui.recentCount
            DishesSortTab.FAVORITE -> ui.favoriteCount
            DishesSortTab.ALL -> ui.allCount
            DishesSortTab.HOME -> ui.homeCount
        }
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (isCuisineTab) {
                // 菜系档：左侧二级分类栏 | 右侧列表(搜索/Tab/计数/筛选/字母段均在右侧列表内，滚动驱动大标题折叠)。
                Row(Modifier.fillMaxSize()) {
                    CuisineRail(cuisines = ui.availableCuisines, selected = ui.selectedCuisine, onSelect = vm::selectCuisine)
                    Box(Modifier.weight(1f).fillMaxSize()) {
                        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                            dishHeaderItems(ui, vm, sortTabs, tabCount)
                            // 无条件加 filters item(即使 DishFilterChips 空返回也占一个 0 高度槽位)——这是 letterHeaderCount=4 的前提，勿改为条件加，否则字母跳转偏位。
                            item(key = "dish-filters") { DishFilterChips(ui, vm) }
                            if (ui.all.isEmpty()) {
                                item(key = "dish-empty") { EmptyState(text = emptyText, icon = "🥗") }
                            } else {
                                sections.forEach { (letter, dishes) ->
                                    item(key = "section-$letter") {
                                        Text(
                                            text = letter,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.fillMaxWidth()
                                                .background(MaterialTheme.colorScheme.background)
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                        )
                                    }
                                    items(dishes, key = { it.id }) { dish ->
                                        DishRow(dish = dish, preferenceRank = hotRankById[dish.id], favorite = dish.id in ui.favoriteIds, onClick = { onOpenDish(dish.id) }, onLongClick = { dropdownDish = dish })
                                    }
                                }
                                item { Spacer(Modifier.height(80.dp)) }
                            }
                        }
                        if (sections.isNotEmpty()) {
                            LetterIndexBar(
                                letters = sections.keys.toList(),
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
                                onLetterSelected = { letter ->
                                    letterIndexMap[letter]?.let { index -> scope.launch { listState.animateScrollToItem(index) } }
                                },
                            )
                        }
                    }
                }
            } else {
                // 最近/喜爱/家庭：搜索/Tab/计数/筛选均在列表头部随内容滚(驱动大标题折叠)，其后列表。
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
                    dishHeaderItems(ui, vm, sortTabs, tabCount)
                    if (hasFilterRow) item(key = "dish-filters") { DishFilterChips(ui, vm) }
                    if (ui.all.isEmpty()) {
                        item { EmptyState(text = emptyText, icon = "🥗") }
                    } else {
                        itemsIndexed(ui.all, key = { _, dish -> dish.id }) { _, dish ->
                            DishRow(dish = dish, preferenceRank = hotRankById[dish.id], favorite = dish.id in ui.favoriteIds, onClick = { onOpenDish(dish.id) }, onLongClick = { dropdownDish = dish })
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
            // [AI生成] 搜索弹框：搜索框有内容时覆盖在列表上，展示搜索结果(名+烹饪方式+预设/自建+详情)。
            if (ui.keyword.isNotBlank()) {
                DishSearchOverlay(
                    results = ui.searchResults,
                    keyword = ui.keyword,
                    onKeywordChange = vm::setKeyword,
                    onOpen = { dish ->
                        vm.openFromSearch(dish) // 跳到该菜的菜系分类 + 关闭弹框
                        onOpenDish(dish.id) // 同时打开详情
                    },
                    onCreateNew = {
                        // [AI生成] 搜索无结果"＋新建菜品「x」":预填菜名进新建菜品页(与食材搜索无结果统一)。
                        prefillBus.request(com.sxdbsm.cookbook.android.ui.newdish.NewDishPrefill(name = ui.keyword.trim()))
                        vm.setKeyword("")
                        onAddDish()
                    },
                    onClose = { vm.setKeyword("") },
                )
            }
        }
    }

    dropdownDish?.let { d ->
        // [AI修改] 苹果风格：长按操作改底部 Action Sheet(破坏项红字、取消置底)，替代 AlertDialog 竖排反模式。
        val actions = buildList {
            add(com.sxdbsm.cookbook.android.ui.component.SheetAction(
                label = if (d.id in ui.favoriteIds) "取消收藏" else "⭐ 收藏置顶",
                onClick = { vm.toggleFavorite(d.id) },
            ))
            if (d.source != "preset") {
                add(com.sxdbsm.cookbook.android.ui.component.SheetAction("编辑", onClick = { onEditDish(d.id) }))
            }
            add(com.sxdbsm.cookbook.android.ui.component.SheetAction("基于此另存", onClick = { onCopyDish(d.id) }))
            // [AI修改] 删除只对自建(user)菜品显示——预设菜不可删(原来预设也显删除、点了才提示"无法删除"，不合理)。
            if (d.source == "user") {
                add(com.sxdbsm.cookbook.android.ui.component.SheetAction("删除", destructive = true, onClick = { vm.requestDeleteDish(d) }))
            }
        }
        com.sxdbsm.cookbook.android.ui.component.ActionSheet(
            title = d.name,
            actions = actions,
            onDismiss = { dropdownDish = null },
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
                    // [AI修改] 苹果风格：删除为破坏性操作，恒用 error 红字。
                    Text(if (deleteState.checking) "删除中..." else "删除", color = MaterialTheme.colorScheme.error)
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

/**
 * 菜品列表头部三件套(搜索行 / 四Tab / 计数)下沉进列表——作大标题折叠燃料。[AI生成] B-7(§9.15)
 *
 * 三件套随内容一起上滑折叠(不吸顶)；筛选行/字母段头由调用处按档吸顶。
 */
private fun LazyListScope.dishHeaderItems(
    ui: DishesUiState,
    vm: DishesViewModel,
    sortTabs: List<Pair<DishesSortTab, String>>,
    tabCount: Int,
) {
    item(key = "dish-search") {
        AppSearchField(
            value = ui.keyword,
            onValueChange = vm::setKeyword,
            placeholder = "搜菜名 / 烹饪方式",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
        )
    }
    item(key = "dish-tabs") {
        SegmentedControl(
            options = sortTabs.map { it.second },
            selectedIndex = sortTabs.indexOfFirst { it.first == ui.sortTab }.coerceAtLeast(0),
            onSelect = { idx -> vm.setSortTab(sortTabs[idx].first) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
    item(key = "dish-count") {
        Text(
            "共 $tabCount 道",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 4.dp),
        )
    }
}

/**
 * 菜品搜索结果弹框覆盖层。[AI生成]
 *
 * 仿食材搜索：单独弹框展示匹配菜品——左侧菜名+下方烹饪方式+旁边预设/自建，右侧详情按钮；
 * 点某项跳到该菜所在菜系分类下并同时打开详情。
 */
@Composable
private fun DishSearchOverlay(
    results: List<DishMini>,
    keyword: String,
    onKeywordChange: (String) -> Unit, // [AI生成] B-7：覆盖层自带搜索框改词(列表内搜索行被本层盖住)
    onOpen: (DishMini) -> Unit,
    onCreateNew: () -> Unit,
    onClose: () -> Unit,
) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
        Column(Modifier.fillMaxSize()) {
            // [AI生成] B-7：搜索行下沉进列表后会被本覆盖层盖住——覆盖层自带搜索框(进入即聚焦)让搜索时仍可改词，iOS Mail 式。
            val searchFocus = remember { FocusRequester() }
            LaunchedEffect(Unit) { runCatching { searchFocus.requestFocus() } }
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppSearchField(
                    value = keyword,
                    onValueChange = onKeywordChange,
                    placeholder = "搜菜名 / 烹饪方式",
                    modifier = Modifier.weight(1f),
                    focusRequester = searchFocus,
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onClose) { Text("取消") }
            }
            Text(
                "搜索结果 ${results.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Divider()
            if (results.isEmpty()) {
                // [AI修改] 无结果给"＋新建菜品「x」"直达(与食材搜索统一),不再只是空态。
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🔎", style = MaterialTheme.typography.displaySmall)
                    Text("未找到「${keyword.trim()}」", style = MaterialTheme.typography.titleSmall)
                    com.sxdbsm.cookbook.android.ui.component.CapsuleButton(text = "＋ 新建菜品「${keyword.trim()}」", onClick = onCreateNew)
                }
            } else {
                LazyColumn(Modifier.fillMaxSize()) {
                    items(results, key = { it.id }) { dish -> DishSearchRow(dish = dish, onClick = { onOpen(dish) }) }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

/** 搜索结果行：左侧菜名+烹饪方式+预设/自建，右侧详情按钮。[AI生成] */
@Composable
private fun DishSearchRow(dish: DishMini, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(dish.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                val method = dish.cookingMethodNames.ifEmpty { dish.cookingMethodName?.let(::listOf).orEmpty() }.joinToString(" / ")
                if (method.isNotBlank()) {
                    Text(method, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                SourceBadge(dish.source)
            }
        }
        TextButton(onClick = onClick) { Text("详情") }
    }
    Divider(color = MaterialTheme.colorScheme.outlineVariant)
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

/**
 * 烹饪方式 / 标签筛选横滑条。[AI生成]
 *
 * 从当前列表派生可选项，再点同一项取消；菜系已移到左侧一级分类栏，此处只留烹饪方式与标签。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DishFilterChips(ui: DishesUiState, vm: DishesViewModel) {
    if (ui.availableMethods.isEmpty() && ui.availableTags.isEmpty()) return
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background),
    ) {
        items(ui.availableMethods, key = { "m-$it" }) { m ->
            FilterChip(selected = ui.selectedMethod == m, onClick = { vm.toggleMethodFilter(m) }, label = { Text(m) })
        }
        items(ui.availableTags, key = { "t-$it" }) { t ->
            FilterChip(selected = ui.selectedTag == t, onClick = { vm.toggleTagFilter(t) }, label = { Text("#$t") })
        }
        // [AI生成] 叠了筛选时给"清除"一键回全量,免逐个再点取消。
        if (ui.selectedMethod != null || ui.selectedTag != null || ui.selectedCuisine != null || ui.keyword.isNotBlank()) {
            item(key = "clear-filters") {
                FilterChip(
                    selected = false,
                    onClick = { vm.clearFilters() },
                    label = { Text("清除") },
                    leadingIcon = { Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(16.dp)) },
                )
            }
        }
    }
}

/**
 * 左侧菜系一级分类栏。[AI生成]
 *
 * 竖向固定列表：全部(不筛) + 家常菜 + 八大菜系等；选中项高亮，右侧菜品只显示该菜系。
 */
@Composable
private fun CuisineRail(cuisines: List<String>, selected: String?, onSelect: (String?) -> Unit) {
    // [AI修改] 菜系分类:全部(null)恒在最前，其后只列"有菜的菜系"(cuisines，去徽菜/西餐等稀疏死胡同)。
    val items: List<Pair<String, String?>> =
        listOf("全部" to null) + cuisines.map { it to it }
    Column(
        modifier = Modifier
            .width(60.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        items.forEach { (label, value) ->
            val isSel = selected == value
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(value) }
                    .background(if (isSel) MaterialTheme.colorScheme.background else Color.Transparent)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSel) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
