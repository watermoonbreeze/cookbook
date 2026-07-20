package com.sxdbsm.cookbook.android.ui.picker

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import kotlinx.coroutines.launch
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.activity.compose.BackHandler
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.IngredientCard
import com.sxdbsm.cookbook.android.ui.component.rememberPantryHookEnabled
import com.sxdbsm.cookbook.domain.model.AdviceLevel
import com.sxdbsm.cookbook.domain.model.FoodCategory
import com.sxdbsm.cookbook.domain.model.Ingredient
import org.koin.androidx.compose.koinViewModel

/**
 * 食材选择 - 全屏 Compose Dialog。[AI修改]
 *
 * 左侧手风琴分类树 + 右侧食材网格 + 顶部搜索 + 底部完成。
 * [AI修改] 搜索面板 SearchResultsPanel 末尾统一常驻 SearchCreateRow"新建食材「x」"行(覆盖有结果/0结果两态)；
 *   selectionMode 收敛 if(有结果)/else if(有词)分叉为"有词即用面板"(修有结果时新建行消失)；Tab覆盖层0结果改"没找到「x」+新建行"。
 */
/** 隐藏"来源徽标"的纯来源浏览Tab(分类已隐含来源→去噪·§9.29)：常规/营养/调养(全预设)、家庭(全自建)。[AI生成] */
private val SOURCE_BADGE_HIDDEN_TABS = setOf(
    IngredientMainTab.GENERAL, IngredientMainTab.NUTRITION, IngredientMainTab.CARE, IngredientMainTab.CUSTOM,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientPickerScreen(
    excludeIngredientIds: Set<Long> = emptySet(),
    onDismiss: () -> Unit,
    onConfirm: (List<Ingredient>) -> Unit,
    asDialog: Boolean = true,
    selectionMode: Boolean = true,
    openDetailFor: Ingredient? = null, // [AI生成] 跨屏(首页搜索)跳来时直接开该食材详情,替代jumpToIngredient定位(自建食材不再跳错到常规)
    openCreateWithName: String? = null, // [AI生成] 跨屏(首页搜索"新建食材")跳来时按名开新增食材编辑器
    onComposeDish: ((List<Ingredient>) -> Unit)? = null, // [AI生成] 食材页:长按进多选,"组成菜品"回传选中食材(从食材出发生成菜品)
    vm: IngredientPickerViewModel = koinViewModel(),
) {
    // [AI生成] 食材页多选"组成菜品"态：仅浏览模式(非菜品选食材)+ onComposeDish 提供时可长按进入。
    var composeMode by remember { mutableStateOf(false) }
    val selecting = selectionMode || composeMode // 显勾选圈/选中态的统一开关
    // [AI修改] 选择状态统一来自 ViewModel；弹窗输入框内容使用 remember 保存临时值。
    val ui by vm.state.collectAsStateWithLifecycle()
    // [AI生成] 库存挂钩总开关(提到顶层，供 库存Tab/详情入库出库/搜索面板入库 统一 gate，防漏点)。
    val pantryHookOn by rememberPantryHookEnabled()
    var createDialogOpen by remember { mutableStateOf(false) }
    var createPrefillName by remember { mutableStateOf("") } // [AI生成] 搜索无结果"新建关键词"时预填的名称
    var editingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var deletingIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var categoryManageOpen by remember { mutableStateOf(false) } // [AI生成] 分类管理弹框开关。
    var categoryEditOpen by remember { mutableStateOf(false) } // [AI生成] 新增/编辑分类弹框开关。
    var categoryEditTarget by remember { mutableStateOf<FoodCategory?>(null) }
    var categoryDeleteTarget by remember { mutableStateOf<FoodCategory?>(null) }
    var categoryNameDraft by remember { mutableStateOf("") }
    var categoryParentIdDraft by remember { mutableStateOf<Long?>(null) }
    var selectedIngredient by remember { mutableStateOf<Ingredient?>(null) } // [AI修改] 食材详情统一通过底部弹层展示，首页和菜品选择共用。
    // [AI生成] 跨屏(首页搜索)跳来:直接开该食材详情(顶部有分类路径),不再靠 jumpToIngredient 定位网格(自建食材跳错常规)。
    LaunchedEffect(openDetailFor?.id) { openDetailFor?.let { selectedIngredient = it } }
    // [AI生成] 跨屏(首页搜索"新建食材")跳来:按名开新增食材编辑器,预填名。
    LaunchedEffect(openCreateWithName) {
        openCreateWithName?.takeIf { it.isNotBlank() }?.let {
            createPrefillName = it
            vm.clearCreateError()
            vm.loadIngredientEditor(null)
            createDialogOpen = true
        }
    }
    var selectedMenuOpen by remember { mutableStateOf(false) } // [AI生成] 底部“已选 X 项”跟随弹框开关。
    var recycleBinOpen by remember { mutableStateOf(false) } // [AI生成] 失效食材回收站弹框开关。
    // [AI生成] #4:Tab落地态搜索改右上角图标触发→展开全屏搜索覆盖层(弹窗选择态仍用顶栏整行搜索,不走此开关)。
    var searchOpen by remember { mutableStateOf(false) }
    // [AI生成] #4:搜索覆盖层可见时,系统返回键=关搜索(而非退出页面)。仅 Tab 落地态。
    BackHandler(enabled = !selectionMode && searchOpen) { searchOpen = false; vm.setKeyword("") }
    val context = androidx.compose.ui.platform.LocalContext.current // [AI生成] A8：入库即时反馈 Toast
    val appSnackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current // [AI生成] UX深挖#2/#13：出/入库改可撤销(§9.12)

    /**
     * 外部排除列表变化时刷新可选食材。[AI修改]
     */
    LaunchedEffect(excludeIngredientIds, selectionMode) {
        vm.configure(excludeIngredientIds, selectionMode)
    }
    // [AI修改] 修#3(切Tab状态丢失):仅**首次**进入 force 初始化主分类(建树);切底部Tab离开再回来时,
    //   composition 重建会让本 LaunchedEffect 重跑——若再 force 会把 selectedCategoryId 重置为"全部"、丢失用户已选分类。
    //   该 Tab 页状态由 Navigation saveState/restoreState 跨切换保留,故再入不该 force 重置分类(force 仅首次建树用)。
    //   rememberSaveable 保证"首次"标记跨再入留存(存进 nav 的 saved bundle);弹窗选择态每次新开=新 composition,标记自然重置。
    var mainTabInited by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!mainTabInited) {
            vm.selectMainTab(ui.mainTab, force = true)
            mainTabInited = true
        }
    }
    LaunchedEffect(selectedIngredient?.id, ui.lastSavedIngredientId) {
        selectedIngredient?.let { vm.loadIngredientDetail(it) }
    } // [AI修改] 编辑保存后（lastSavedIngredientId 变化）重新加载详情，修复详情弹层不实时刷新的问题。
    LaunchedEffect(ui.ingredients, ui.selectedIngredients, selectedIngredient?.id) {
        val currentId = selectedIngredient?.id ?: return@LaunchedEffect
        val refreshed = (ui.ingredients + ui.selectedIngredients).firstOrNull { it.id == currentId }
        selectedIngredient = refreshed ?: selectedIngredient
    } // [AI生成] 食材编辑保存后同步当前详情弹层中的名称、二级名称和图片，避免继续显示旧对象。
    ui.operationError?.let { error ->
        AlertDialog(
            onDismissRequest = { vm.clearOperationError() },
            title = { Text("操作失败") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = { vm.clearOperationError() }) {
                    Text("知道了")
                }
            },
        )
    }
    // [AI修改] B(2026-07-16)：Tab 落地页(selectionMode=false)统一大标题(与首页/菜品/我的一致)，下滑折叠；
    //   弹窗选择器(selectionMode=true)保持原紧凑顶栏(返回+整行搜索)不动。共享主体(搜索面板/主Tab/网格/底栏)两模式共用。
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val content: @Composable () -> Unit = {
        // 右上"添加食材"两模式共用。
        val addAction: @Composable () -> Unit = {
            IconButton(
                onClick = {
                    vm.clearCreateError()
                    vm.loadIngredientEditor(null)
                    createDialogOpen = true
                },
            ) {
                Icon(Icons.Outlined.Add, contentDescription = "添加食材")
            }
        }
        Surface(
            // Tab 模式给 Surface 挂 nestedScroll：右侧网格滚动驱动 LargeTopAppBar 折叠；弹窗模式不挂。
            modifier = Modifier.fillMaxSize()
                .then(if (!selectionMode) Modifier.nestedScroll(scrollBehavior.nestedScrollConnection) else Modifier),
            color = MaterialTheme.colorScheme.background, // [AI修改] B背景统一:以菜品页为准,食材页根背景由 surface(白)改 background(分组灰),两页一致。
        ) {
          Box(Modifier.fillMaxSize()) { // [AI生成] #4:包 Box 以叠放 Tab落地态搜索覆盖层
            Column(Modifier.fillMaxSize()) {
                if (selectionMode) {
                    // 弹窗选择器：返回 + 整行搜索 + 添加(原样保留，紧凑、无大标题、不折叠)。
                    TopAppBar(
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            navigationIconContentColor = MaterialTheme.colorScheme.primary,
                            actionIconContentColor = MaterialTheme.colorScheme.primary,
                        ),
                        title = {
                            AppSearchField(
                                value = ui.keyword,
                                onValueChange = vm::setKeyword,
                                placeholder = "搜索食材",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                            }
                        },
                        actions = { addAction() },
                    )
                } else {
                    // Tab 落地页：大标题「食材」下滑折叠 + 右上[搜索][添加]；#4 搜索改右上角图标触发覆盖层(不再常驻搜索行)。
                    LargeTopAppBar(
                        title = { Text("食材", fontWeight = FontWeight.Bold) },
                        scrollBehavior = scrollBehavior,
                        actions = {
                            // [AI修改] #4:搜索图标(左)+添加(右),与菜品页一致;点搜索展开全屏搜索覆盖层。
                            IconButton(onClick = { searchOpen = true }) {
                                Icon(Icons.Outlined.Search, contentDescription = "搜索食材")
                            }
                            addAction()
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            scrolledContainerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                            actionIconContentColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
                // [AI修改] #4:内联搜索面板仅弹窗选择态(整行搜索)用;Tab落地态搜索走覆盖层(见下方 IngredientSearchOverlay)。
                // [AI修改] 收敛原 if(有结果)/else if(有词)分叉：有搜索词就用 SearchResultsPanel，面板自带末尾"新建食材「x」"行，覆盖有结果/0结果两态(不再有"有结果时新建行消失"问题)。
                if (selectionMode && ui.keyword.trim().isNotBlank()) {
                    SearchResultsPanel(
                        results = ui.searchResults,
                        selectionMode = selectionMode,
                        selectedIds = ui.selectedIds,
                        pantryIds = ui.pantryIngredientIds,
                        categoryName = ui.searchCategoryName, // [AI生成] 2026-07-19:搜"蔬菜类"→按类目筛模式头部+不显新建行
                        onPick = {
                            // [AI修改] 搜索结果点击直接开详情(顶部显分类路径)——不再靠 jumpToIngredient 定位网格,
                            // 修"食材还没加载到当前页时点击没反应"。收起搜索下拉,详情弹层浮在上层。
                            vm.setKeyword("")
                            selectedIngredient = it
                        },
                        onToggleSelect = { vm.toggleSelection(it) },
                        // [AI修改] 阻断-1:库存挂钩关→搜索面板不显入库/出库(传 null)。
                        onTogglePantry = if (pantryHookOn) { ing ->
                            // [AI修改] UX深挖#2/#13：出/入库改可撤销(§9.12 Snackbar+撤销，替代 Toast/无反馈)。
                            if (ing.id in ui.pantryIngredientIds) {
                                vm.removeFromPantryUndoable(ing) { onUndo ->
                                    appSnackbar?.showUndo("已把「${ing.name}」移出库存", onUndo = onUndo)
                                }
                            } else {
                                vm.addToPantryUndoable(ing) { onUndo ->
                                    appSnackbar?.showUndo("已把「${ing.name}」入库 1 份", onUndo = onUndo)
                                }
                            }
                        } else null,
                        // [AI生成] 末尾常驻"新建食材「x」"行：搜到库里没有的直接新建(预填名称、大类按名自动预选)。
                        onCreateNew = {
                            createPrefillName = ui.keyword.trim()
                            vm.clearCreateError()
                            vm.loadIngredientEditor(null)
                            createDialogOpen = true
                        },
                        createKeyword = ui.keyword,
                    )
                }
                // [AI生成] 库存挂钩关→隐藏"库存"Tab(用户已明确不用库存，留灰占位是打脸式提醒)；若正停在该 Tab 则回落。
                androidx.compose.runtime.LaunchedEffect(pantryHookOn, ui.mainTab) {
                    if (!pantryHookOn && ui.mainTab == IngredientMainTab.PANTRY) vm.selectMainTab(IngredientMainTab.RECENT)
                }
                val visibleTabs = IngredientMainTab.values().filter { it != IngredientMainTab.PANTRY || pantryHookOn }
                // [AI修改] §9.18:主分类改共享件 PrimaryTabRow(与菜品页统一胶囊分段视觉);食材项数可变(≤6)→scrollable=true 横滚、项按内容宽。逻辑(selectMainTab/清详情)不动。
                com.sxdbsm.cookbook.android.ui.component.PrimaryTabRow(
                    options = visibleTabs.map { it.label },
                    selectedIndex = visibleTabs.indexOf(ui.mainTab).coerceAtLeast(0),
                    onSelect = { idx ->
                        selectedIngredient = null
                        vm.selectMainTab(visibleTabs[idx])
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
                    scrollable = true,
                )
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    // 左侧分类树（最近/库存平铺，无左树）
                    if (ui.mainTab != IngredientMainTab.RECENT && ui.mainTab != IngredientMainTab.PANTRY) {
                        LazyColumn(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                // [AI修改] 用户要求:食材二级分类(左栏)背景/样式与菜品菜系栏(CuisineRail)统一——改用同款柔和 surfaceVariant.copy(0.35)(非白),明暗都成立。
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                        ) {
                            item {
                                CategoryItem(
                                    label = "全部",
                                    level = 1,
                                    expanded = false,
                                    hasChildren = false,
                                    selected = ui.selectedCategoryId == -1L,
                                    onClick = {
                                        selectedIngredient = null
                                        vm.selectAll()
                                    },
                                )
                            }
                            if (ui.mainTab == IngredientMainTab.CUSTOM) {
                                item {
                                    TextButton(
                                        onClick = { categoryManageOpen = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    ) {
                                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("管理分类")
                                    }
                                }
                                item {
                                    // [AI生成] 回收站入口：查看/恢复/彻底删除失效的自定义食材。
                                    TextButton(
                                        onClick = {
                                            vm.loadInactiveIngredients()
                                            recycleBinOpen = true
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                    ) {
                                        Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("已失效")
                                    }
                                }
                            }
                            items(ui.tree, key = { "${it.category.id}-${it.level}" }) { node ->
                                CategoryItem(
                                    label = node.category.name,
                                    level = node.level,
                                    expanded = node.expanded,
                                    hasChildren = node.category.hasChildren,
                                    selected = ui.selectedCategoryId == node.category.id,
                                    onClick = {
                                        selectedIngredient = null
                                        if (node.category.hasChildren) {
                                            vm.toggleExpand(node)
                                        }
                                        vm.selectCategory(node) // [AI修改] 点击任意层级分类都立即筛选该分类及其子分类下的食材。
                                    },
                                )
                            }
                        }
                    }
                    // 右侧食材网格
                    // [AI修改] 调养 tab 选中病种分类后按 绿灯/黄灯/红灯 分组展示，剂量语义来自调养规则 advice_level。
                    val careGroups = if (ui.mainTab == IngredientMainTab.CARE && ui.selectedCategoryId != null) {
                        listOf(
                            AdviceLevel.RECOMMEND to "🟢 推荐", // [AI修改] 文案:术语统一"推荐/限量/避免"(与详情页/维度区一致,去"红绿灯/禁忌"另一套词)
                            AdviceLevel.LIMIT to "🟡 限量",
                            AdviceLevel.AVOID to "🔴 避免",
                        ).mapNotNull { (level, title) ->
                            val group = ui.ingredients.filter { it.adviceLevel == level }
                            if (group.isEmpty()) null else title to group
                        } + listOfNotNull(
                            ui.ingredients.filter { it.adviceLevel == null }
                                .takeIf { it.isNotEmpty() }
                                ?.let { "其他" to it },
                        )
                    } else {
                        emptyList()
                    }
                    val gridState = rememberLazyGridState()
                    val letterScope = rememberCoroutineScope() // [AI生成] ②b：字母定位条点击→滚动到该首字母首个食材。
                    // [AI生成] 搜索跳转后滚动到高亮食材（跳转固定在常规 Tab、无 careGroups 头，index 即列表位置）。
                    LaunchedEffect(ui.highlightIngredientId, ui.ingredients) {
                        val hid = ui.highlightIngredientId ?: return@LaunchedEffect
                        val idx = ui.ingredients.indexOfFirst { it.id == hid }
                        if (idx >= 0) gridState.animateScrollToItem(idx)
                    }
                    // [AI生成] 加载更多改为"滑到底部前自动预加载"(替代手动按钮，与搜索页一致)：
                    //   接近末尾(剩 6 项≈2 行)即续下一页。loadMoreIngredients 为内存分页(同步)、自带 canLoadMore 守卫，安全。
                    LaunchedEffect(gridState, ui.ingredients.size, ui.canLoadMoreIngredients) {
                        snapshotFlow {
                            val lastVisible = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            lastVisible to gridState.layoutInfo.totalItemsCount
                        }.collect { (lastVisible, total) ->
                            if (ui.canLoadMoreIngredients && total > 0 && lastVisible >= total - 6) {
                                vm.loadMoreIngredients()
                            }
                        }
                    }
                    // [AI生成] ②b 库存字母定位：把网格包进 Box 以在右侧叠放 LetterIndexBar(仅库存 Tab)。
                    Box(Modifier.weight(1f)) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        state = gridState,
                        // [AI修改] 库存 Tab 右侧留出字母条空间；其余 Tab 正常内边距。
                        contentPadding = PaddingValues(
                            start = 12.dp, top = 12.dp, bottom = 12.dp,
                            end = if (ui.mainTab == IngredientMainTab.PANTRY) 24.dp else 12.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        if (careGroups.isNotEmpty()) {
                            careGroups.forEach { (title, group) ->
                                item(key = "care-header-$title", span = { GridItemSpan(maxLineSpan) }) {
                                    Text(
                                        title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                                items(group, key = { it.id }) { ing ->
                                    IngredientCard(
                                        ingredient = ing,
                                        selected = ing.id in ui.selectedIds,
                                        highlighted = ing.id == ui.highlightIngredientId,
                                        showSourceBadge = ui.mainTab !in SOURCE_BADGE_HIDDEN_TABS, // [AI生成] §9.29 纯来源分类Tab去噪
                                        onClick = { selectedIngredient = ing },
                                        onToggleSelect = if (selecting) ({ vm.toggleSelection(ing) }) else null, // [AI生成] 点勾选圈直接选
                                        onLongClick = if (!selectionMode && onComposeDish != null) ({ composeMode = true; if (ing.id !in ui.selectedIds) vm.toggleSelection(ing) }) else null, // [AI生成] 长按进"组成菜品"多选
                                    )
                                }
                            }
                        } else if (ui.ingredients.isEmpty() && ui.keyword.isBlank()) {
                            // [AI生成] 网格空态：避免纯空白让用户以为出错，按 Tab 给"下一步"引导(苹果式永远给下一步)。
                            item(key = "empty-state", span = { GridItemSpan(maxLineSpan) }) {
                                IngredientGridEmptyState(tab = ui.mainTab)
                            }
                        } else {
                            items(ui.ingredients, key = { it.id }) { ing ->
                                // [AI生成] B-3：库存Tab 卡底就地加减份数(复用 MiniStepper，§9.14)，免开详情。
                                val pantryFooter: (@Composable () -> Unit)? =
                                    if (ui.mainTab == IngredientMainTab.PANTRY && !selectionMode) {
                                        val serving = ui.pantryServings[ing.id] ?: 0
                                        {
                                            com.sxdbsm.cookbook.android.ui.component.MiniStepper(
                                                valueText = "$serving 份",
                                                onMinus = { vm.setServings(ing.id, (serving - 1).coerceAtLeast(0)) },
                                                onPlus = { vm.addServings(ing.id, 1) },
                                                minusEnabled = serving > 0,
                                                modifier = Modifier.padding(horizontal = 4.dp),
                                            )
                                        }
                                    } else null
                                IngredientCard(
                                    ingredient = ing,
                                    selected = ing.id in ui.selectedIds,
                                    highlighted = ing.id == ui.highlightIngredientId,
                                    showSourceBadge = ui.mainTab !in SOURCE_BADGE_HIDDEN_TABS, // [AI生成] §9.29 纯来源分类Tab去噪
                                    onClick = {
                                        selectedIngredient = ing // [AI修改] 点击食材统一先打开详情，是否加入已选由详情顶部按钮决定。
                                    },
                                    onToggleSelect = if (selecting) ({ vm.toggleSelection(ing) }) else null, // [AI生成] 点勾选圈直接选(点卡仍看详情)
                                    onLongClick = if (!selectionMode && onComposeDish != null) ({ composeMode = true; if (ing.id !in ui.selectedIds) vm.toggleSelection(ing) }) else null, // [AI生成] 长按进"组成菜品"多选
                                    footer = pantryFooter,
                                )
                            }
                        }
                        // [AI修改] 手动"加载更多"按钮已由上方滑到底自动预加载替代；保留加载中占位(可选)。
                        if (ui.canLoadMoreIngredients) {
                            item(key = "load-more-spacer", span = { GridItemSpan(maxLineSpan) }) {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    Text("加载中…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    // [AI生成] ②b 库存字母定位条(拖动/点击滚到该首字母首个食材)。仅库存 Tab、超一屏(>12)且多字母才显。
                    if (ui.mainTab == IngredientMainTab.PANTRY) {
                        val pantryLetters = remember(ui.ingredients) {
                            ui.ingredients.map { com.sxdbsm.cookbook.android.ui.component.pinyinInitial(it.name) }.distinct()
                        }
                        if (pantryLetters.size > 1 && ui.ingredients.size > 12) {
                            com.sxdbsm.cookbook.android.ui.component.LetterIndexBar(
                                letters = pantryLetters,
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 2.dp),
                                onLetterSelected = { letter ->
                                    val idx = ui.ingredients.indexOfFirst {
                                        com.sxdbsm.cookbook.android.ui.component.pinyinInitial(it.name) == letter
                                    }
                                    if (idx >= 0) letterScope.launch { gridState.animateScrollToItem(idx) }
                                },
                            )
                        }
                    }
                    } // Box(字母定位叠放) 收尾
                }
                // [AI修改] 移除库存 Tab 底部"从现有食材中添加入库"冗余提示——空态已有引导，非空时也不需常驻这行。
                // 底部固定栏：仅选择模式且有已选时出现。
                if (selectionMode && ui.selectedIds.isNotEmpty()) {
                    SelectionBottomBar(
                        selectedCount = ui.selectedIds.size,
                        selectedIngredients = ui.selectedIngredients,
                        menuOpen = selectedMenuOpen,
                        onMenuOpenChange = { selectedMenuOpen = it },
                        onPickSelected = { selectedIngredient = it },
                        onConfirm = {
                            onConfirm(vm.confirmSelected())
                            onDismiss()
                        },
                    )
                }
                // [AI生成] 食材页"组成菜品"多选底栏:长按进入后出现,显已选+组成菜品(从食材出发生成菜品)+取消。
                if (composeMode && onComposeDish != null) {
                    ComposeDishBottomBar(
                        selectedCount = ui.selectedIds.size,
                        onCancel = { composeMode = false; vm.clearSelection() },
                        onCompose = {
                            val picked = ui.selectedIngredients
                            composeMode = false
                            vm.clearSelection()
                            onComposeDish(picked)
                        },
                    )
                }
            }
            // [AI生成] #4:Tab落地态全屏搜索覆盖层(右上搜索图标触发)——自带聚焦搜索框+取消,复用 SearchResultsPanel;
            //   空查询给提示、有输入0结果给"新建"、有结果列表。与菜品页 DishSearchOverlay 同范式。
            if (!selectionMode && searchOpen) {
                val searchFocus = remember { FocusRequester() }
                LaunchedEffect(Unit) { runCatching { searchFocus.requestFocus() } }
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background, tonalElevation = 2.dp) {
                    // [AI修改] #15 修bug:全屏覆盖层从 y=0 起绘,顶部搜索行须避让状态栏(否则钻到状态栏下显示半个);statusBarsPadding 只让内容下移、Surface 底色仍铺满状态栏区。
                    Column(Modifier.fillMaxSize().statusBarsPadding()) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AppSearchField(
                                value = ui.keyword,
                                onValueChange = vm::setKeyword,
                                placeholder = "搜索食材",
                                modifier = Modifier.weight(1f),
                                focusRequester = searchFocus,
                            )
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { searchOpen = false; vm.setKeyword("") }) { Text("取消") }
                        }
                        Divider()
                        when {
                            ui.keyword.isBlank() -> {
                                Column(
                                    Modifier.fillMaxWidth().padding(top = 48.dp, start = 24.dp, end = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text("🔎", style = MaterialTheme.typography.displaySmall)
                                    Text("输入食材名开始搜索", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            ui.searchResults.isNotEmpty() -> {
                                SearchResultsPanel(
                                    results = ui.searchResults,
                                    selectionMode = false,
                                    selectedIds = ui.selectedIds,
                                    pantryIds = ui.pantryIngredientIds,
                                    categoryName = ui.searchCategoryName, // [AI生成] 2026-07-19:搜"蔬菜类"→按类目筛模式头部+不显新建行
                                    fillHeight = true, // [AI生成] #4:全屏覆盖层铺满,不用 320dp 限高
                                    onPick = { searchOpen = false; vm.setKeyword(""); selectedIngredient = it },
                                    onToggleSelect = { vm.toggleSelection(it) },
                                    // [AI生成] 有结果末尾也常驻"新建食材「x」"行(与0结果同一视觉)。
                                    onCreateNew = {
                                        createPrefillName = ui.keyword.trim()
                                        vm.clearCreateError()
                                        vm.loadIngredientEditor(null)
                                        searchOpen = false
                                        createDialogOpen = true
                                    },
                                    createKeyword = ui.keyword,
                                    onTogglePantry = if (pantryHookOn) { ing ->
                                        // [AI修改] UX深挖#2/#13：与其余入口一致改可撤销(§9.12)，替代 Toast/无反馈。
                                        if (ing.id in ui.pantryIngredientIds) {
                                            vm.removeFromPantryUndoable(ing) { onUndo ->
                                                appSnackbar?.showUndo("已把「${ing.name}」移出库存", onUndo = onUndo)
                                            }
                                        } else {
                                            vm.addToPantryUndoable(ing) { onUndo ->
                                                appSnackbar?.showUndo("已把「${ing.name}」入库 1 份", onUndo = onUndo)
                                            }
                                        }
                                    } else null,
                                )
                            }
                            else -> {
                                // [AI修改] 0结果:改"🔎 + 没找到「x」说明 + SearchCreateRow 新建行"(替换原居中 CapsuleButton，与有结果末尾行同一视觉)。
                                Column(
                                    Modifier.fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Column(
                                        Modifier.fillMaxWidth().padding(top = 40.dp, start = 24.dp, end = 24.dp, bottom = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text("🔎", style = MaterialTheme.typography.displaySmall)
                                        // [AI修改] 2026-07-19:类目筛模式(搜"蔬菜类")用类目口径文案,非"没找到「」"。
                                        Text(
                                            if (ui.searchCategoryName != null) "「${ui.searchCategoryName}」类目下还没有食材" else "没找到「${ui.keyword.trim()}」",
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                    }
                                    // [AI修改] 2026-07-19:类目筛模式不显"新建食材"(搜类目名不是要新建叫这名的食材)；普通搜索保留。
                                    if (ui.searchCategoryName == null) {
                                        com.sxdbsm.cookbook.android.ui.component.SearchCreateRow(
                                            keyword = ui.keyword,
                                            entity = "食材",
                                            onClick = {
                                                createPrefillName = ui.keyword.trim()
                                                vm.clearCreateError()
                                                vm.loadIngredientEditor(null)
                                                searchOpen = false
                                                createDialogOpen = true
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
          } // Box(叠放层) 收尾
        }
    }
    if (asDialog) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
        ) {
            content()
        }
    } else {
        content()
    }

    selectedIngredient?.let { ingredient ->
        IngredientDetailSheet(
            ingredient = ingredient,
            selected = ingredient.id in ui.selectedIds,
            loading = ui.detailLoading && ui.detailIngredientId == ingredient.id,
            categories = ui.detailCategories,
            categoryPath = buildGeneralCategoryPath(ui.detailCategories, ui.allCategories), // [AI生成] 顶部分类路径
            detail = ui.detailInfo,
            careRules = ui.detailCareRules,
            crowdVerdicts = ui.detailCrowdVerdicts,
            dishMatches = ui.detailDishMatches,
            enabledCareCategoryIds = ui.enabledCareCategoryIds,
            onDismiss = { selectedIngredient = null },
            // [AI修改] 高风险重构：仅选择场景传 onToggleSelection(非空→组件显选择按钮)，浏览场景传 null。
            onToggleSelection = if (selectionMode) {
                {
                    vm.toggleSelection(ingredient) // [AI修改] 详情页右侧按钮按当前状态执行选择/取消选择。
                    selectedIngredient = null // [AI修改] 选择或取消选择后立即关闭详情弹层。
                }
            } else null,
            // [AI修改] N5：选择模式下也允许编辑「自建(user)」食材(预设仍不可直接编辑)；页面模式保持可编辑。
            onEdit = if (!selectionMode || ingredient.source == "user") {
                {
                    vm.loadIngredientEditor(ingredient)
                    editingIngredient = ingredient
                }
            } else {
                null
            },
            onDelete = if (ingredient.source == "user") {
                {
                    deletingIngredient = ingredient
                }
            } else {
                null
            },
            inPantry = ingredient.id in ui.pantryIngredientIds,
            // [AI修改] 阻断-1:库存挂钩关→详情弹层不显入库/出库/份数(回调置 null,能力显隐由回调决定,组件不改)。
            onTogglePantry = if (!selectionMode && pantryHookOn) ({
                // [AI修改] UX深挖#2：出库改可撤销(§9.12)。
                vm.removeFromPantryUndoable(ingredient) { onUndo ->
                    appSnackbar?.showUndo("已把「${ingredient.name}」移出库存", onUndo = onUndo)
                }
                selectedIngredient = null // [AI修改] 出库后关闭详情弹层(用户反馈:点出库后详情该退出，与"存为菜品"一致)
            }) else null, // [AI修改] 仅承担出库
            pantryRemaining = ui.pantryRemaining[ingredient.id] ?: 0,
            pantryServing = ui.pantryServings[ingredient.id] ?: 0,
            onAddServings = if (!selectionMode && pantryHookOn) ({ count -> vm.addServings(ingredient.id, count) }) else null,
            onSetServings = if (!selectionMode && pantryHookOn) ({ count -> vm.setServings(ingredient.id, count) }) else null,
            // [AI生成] 仅浏览模式(非选食材)给"存为菜品"：即食品直接吃场景一步建成同名单食材菜品。
            onSaveAsDish = if (!selectionMode) ({
                vm.saveIngredientAsDish(ingredient) { already ->
                    Toast.makeText(
                        context,
                        if (already) "已有同名菜品「${ingredient.name}」" else "已把「${ingredient.name}」存成一道菜，记餐时可直接选",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                selectedIngredient = null
            }) else null,
        )
    }

    if (recycleBinOpen) {
        InactiveIngredientsDialog(
            ingredients = ui.inactiveIngredients,
            onRestore = { vm.restoreIngredient(it) },
            onHardDelete = { vm.hardDeleteIngredient(it) },
            onDismiss = { recycleBinOpen = false },
        )
    }

    if (createDialogOpen) {
        IngredientEditorDialog(
            ingredient = null,
            initialName = createPrefillName, // [AI生成] 搜索无结果新建时预填名称
            ui = ui,
            canSaveAndContinue = true, // [AI生成] B-6：新建入口给"再记一个"连续录入(编辑入口不传)
            onDismiss = {
                createDialogOpen = false
                createPrefillName = ""
                vm.clearCreateError()
            },
            onAddCategory = {
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
                categoryEditOpen = true
            },
            onAddUnit = vm::addUnit,
            onSave = vm::saveIngredientEditor,
            onGuessNutrition = { n, cb -> vm.guessNutrition(n, cb) }, // [AI生成] 智能推演：新建按名预填营养
        )
    }

    editingIngredient?.let { ingredient ->
        IngredientEditorDialog(
            ingredient = ingredient,
            ui = ui,
            onDismiss = {
                editingIngredient = null
                vm.clearCreateError()
            },
            onAddCategory = {
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
                categoryEditOpen = true
            },
            onAddUnit = vm::addUnit,
            onSave = vm::saveIngredientEditor,
        )
    }

    LaunchedEffect(ui.lastSavedIngredientId) {
        if (ui.lastSavedIngredientId != null) {
            createDialogOpen = false
            editingIngredient = null
        }
    }

    deletingIngredient?.let { ingredient ->
        AlertDialog(
            onDismissRequest = { deletingIngredient = null },
            title = { Text("删除食材") },
            text = {
                Text(
                    "确定删除“${ingredient.displayNameText()}”吗？删除后该食材将标记为失效并从食材列表隐藏；已关联菜品中仍会保留并灰显，不影响原有菜品。可在“已失效”中恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // [AI修改] 删除=软失效保留：置失效并从列表隐藏，菜品引用灰显保留，可在“已失效”恢复。
                        vm.deleteIngredient(ingredient)
                        deletingIngredient = null
                        selectedIngredient = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingIngredient = null }) { Text("取消") }
            },
        )
    }

    if (categoryManageOpen) {
        CategoryManageDialog(
            categories = ui.allCategories.filter { it.isEditableUserGeneralCategory() },
            onDismiss = { categoryManageOpen = false },
            onAdd = {
                categoryManageOpen = false
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
                categoryEditOpen = true
            },
            onEdit = { category ->
                categoryManageOpen = false
                categoryEditTarget = category
                categoryNameDraft = category.name
                categoryParentIdDraft = category.parentId
                categoryEditOpen = true
            },
            onDelete = {
                categoryManageOpen = false
                categoryDeleteTarget = it
            },
        )
    }

    if (categoryEditOpen) {
        val editingCategory = categoryEditTarget
        CategoryEditDialog(
            editingCategory = editingCategory,
            categories = ui.allCategories,
            name = categoryNameDraft,
            parentId = categoryParentIdDraft,
            onNameChange = { categoryNameDraft = it },
            onParentChange = { categoryParentIdDraft = it },
            onDismiss = {
                categoryEditOpen = false
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
            },
            onSave = {
                if (editingCategory == null) {
                    vm.createCategory(categoryNameDraft, categoryParentIdDraft)
                } else {
                    vm.renameCategory(editingCategory, categoryNameDraft)
                }
                categoryEditOpen = false
                categoryEditTarget = null
                categoryNameDraft = ""
                categoryParentIdDraft = null
            },
        )
    }

    categoryDeleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { categoryDeleteTarget = null },
            title = { Text("删除分类") },
            text = {
                Text("确定删除“${category.name}”吗？分类会被软删除，已绑定的食材只解除分类关系，食材本身不会删除。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCategory(category)
                        categoryDeleteTarget = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryDeleteTarget = null }) { Text("取消") }
            },
        )
    }
}

/**
 * 食材页"组成菜品"多选底栏（已选 N · 组成菜品 · 取消）。[AI生成]
 *
 * 从食材出发生成菜品：长按进多选后出现，点"组成菜品"带选中食材跳新建菜品页。
 */
@Composable
private fun ComposeDishBottomBar(
    selectedCount: Int,
    onCancel: () -> Unit,
    onCompose: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) { Text("取消") }
            Text(
                "已选 $selectedCount 项",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(start = 8.dp),
            )
            com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                text = "组成菜品",
                onClick = onCompose,
                enabled = selectedCount > 0,
            )
        }
    }
}

/**
 * 选择模式底部固定栏（已选项 + 添加/找菜/完成）。[AI生成]
 *
 * 从主编排抽出，把"选择模式专属"的底栏收敛成命名组件，减少主体里散落的 selectionMode 分叉。
 */
@Composable
private fun SelectionBottomBar(
    selectedCount: Int,
    selectedIngredients: List<Ingredient>,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onPickSelected: (Ingredient) -> Unit,
    onConfirm: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                Text(
                    "已选 $selectedCount 项",
                    modifier = Modifier.clickable { onMenuOpenChange(true) }, // [AI生成] 点击已选数量打开左下跟随弹框。
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                DropdownMenu(expanded = menuOpen, onDismissRequest = { onMenuOpenChange(false) }) {
                    selectedIngredients.forEach { ingredient ->
                        DropdownMenuItem(
                            text = { Text(ingredient.displayNameText(), maxLines = 1) },
                            onClick = {
                                onMenuOpenChange(false)
                                onPickSelected(ingredient) // [AI生成] 从已选弹框点击食材时打开同一个详情弹层。
                            },
                        )
                    }
                }
            }
            // [AI修改] B-1：菜品编辑选食材时"找菜"无意义(已在建这道菜)，去掉；"添加食材"改由顶部＋。底栏只留完成。
            Button(onClick = onConfirm, enabled = selectedCount > 0) { Text("完成") }
        }
    }
}

/**
 * 全局搜索结果下拉面板。[AI生成]
 *
 * 紧贴搜索框下方弹出，跨全库匹配的食材列表；点某项跳到其所属分类并高亮。
 */
@Composable
private fun SearchResultsPanel(
    results: List<Ingredient>,
    selectionMode: Boolean, // [AI生成] true=菜品选食材(选择/已选)；false=食材管理(入库/出库)
    selectedIds: Set<Long>,
    pantryIds: Set<Long>,
    onPick: (Ingredient) -> Unit,
    onToggleSelect: (Ingredient) -> Unit,
    onTogglePantry: ((Ingredient) -> Unit)? = null, // [AI修改] 阻断-1:库存挂钩关时传 null→不显入库/出库按钮
    fillHeight: Boolean = false, // [AI生成] #4(Google审查🟡5):全屏搜索覆盖层里铺满高度;弹窗内联下拉仍限高 320dp。
    onCreateNew: (() -> Unit)? = null, // [AI生成] 传入即在列表末尾显示"新建食材「x」"行(能力由回调是否传入决定，非 mode 布尔)。
    createKeyword: String = "", // [AI生成] 新建行展示/回填的关键词(需 onCreateNew!=null 且非空白才渲染)。
    categoryName: String? = null, // [AI生成] 2026-07-19:非空=按类目筛模式(搜"蔬菜类")，顶部提示"「X」的食材 · N 种"、不显新建行。
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        LazyColumn(modifier = if (fillHeight) Modifier.fillMaxSize() else Modifier.heightIn(max = 320.dp)) {
            // [AI生成] 2026-07-19:按类目筛模式头部——淡主色条+"「蔬菜类」的食材 · N 种"，一眼区别普通名搜。
            if (categoryName != null) {
                item(key = "cat-header") {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.06f))
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "「$categoryName」的食材",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            " · ${results.size} 种",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Divider()
                }
            }
            items(results, key = { it.id }) { ing ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(ing) }
                        .padding(start = 16.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(ing.emoji.ifBlank { "🥗" }, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(12.dp))
                    // [AI修改] 左：食材名 +（预设/家庭）。
                    Text(ing.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                    Text(
                        "（${if (ing.source == "user") "家庭" else "预设"}）",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    // [AI修改] 右：菜品模式=选择/已选；管理模式=入库/出库。
                    val compact = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                    if (selectionMode) {
                        if (ing.id in selectedIds) {
                            FilledTonalButton(onClick = { onToggleSelect(ing) }, contentPadding = compact) { Text("已选") }
                        } else {
                            OutlinedButton(onClick = { onToggleSelect(ing) }, contentPadding = compact) { Text("选择") }
                        }
                    } else if (onTogglePantry != null) { // [AI修改] 阻断-1:库存挂钩关(回调null)→管理模式不显入库/出库
                        if (ing.id in pantryIds) {
                            OutlinedButton(onClick = { onTogglePantry(ing) }, contentPadding = compact) { Text("出库") }
                        } else {
                            Button(onClick = { onTogglePantry(ing) }, contentPadding = compact) { Text("入库") }
                        }
                    }
                }
                Divider()
            }
            // [AI修改] 2026-07-19:列表末尾常驻"新建食材「x」"行(覆盖有结果/0结果两态)。**类目筛模式不显**(搜"蔬菜类"不是要新建叫这名的食材)。
            if (onCreateNew != null && createKeyword.isNotBlank() && categoryName == null) {
                item(key = "search-create-row") {
                    com.sxdbsm.cookbook.android.ui.component.SearchCreateRow(
                        keyword = createKeyword,
                        entity = "食材",
                        onClick = onCreateNew,
                    )
                }
            }
        }
    }
}

/**
 * 食材网格空态：按 Tab 给出"下一步"引导，避免纯空白让用户以为出错。[AI生成]
 */
@Composable
private fun IngredientGridEmptyState(tab: IngredientMainTab) {
    val (emoji, title, hint) = when (tab) {
        IngredientMainTab.RECENT -> Triple("🍽️", "还没有最近用过的食材", "记一餐后，这里会显示你家常用的食材")
        IngredientMainTab.PANTRY -> Triple("🧊", "库存还是空的", "从搜索或分类里，把家里有的食材『入库』") // [AI修改] 文案:术语统一"库存"(不用"冰箱")
        IngredientMainTab.CUSTOM -> Triple("🥗", "还没有自建食材", "点右上角 ＋ 添加你家常用的食材")
        else -> Triple("🔍", "这个分类下暂无食材", "换个分类看看，或用上方搜索")
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 48.dp, bottom = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.displaySmall)
        Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
        Text(
            hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // [AI生成] 库存空态补充：餐次里缺/没有的食材可在采购清单统一添加(引导到集中入口)。
        if (tab == IngredientMainTab.PANTRY) {
            Text(
                "餐次里缺少或没有的食材，可在「我的 → 采购清单」中统一添加",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, start = 24.dp, end = 24.dp),
            )
        }
    }
}

/**
 * 拼食材的常规分类路径(常规›蔬菜类›叶菜)。[AI生成]
 *
 * 取该食材所挂的 general 维度分类里最深的一个，沿 parentId 链向上收集名，正序拼接。无 general 分类返回空。
 */
private fun buildGeneralCategoryPath(ingredientCategories: List<FoodCategory>, all: List<FoodCategory>): String {
    val byId = all.associateBy { it.id }
    fun depth(c: FoodCategory): Int {
        var d = 0; var cur = c; var g = 0
        while (cur.parentId != null && g++ < 12) { cur = byId[cur.parentId] ?: break; d++ }
        return d
    }
    val leaf = ingredientCategories.filter { it.dimension == "general" }.maxByOrNull { depth(it) } ?: return ""
    val chain = mutableListOf<String>()
    var cur: FoodCategory? = leaf
    var g = 0
    while (g++ < 12) {
        val c = cur ?: break
        chain.add(c.name)
        cur = c.parentId?.let { byId[it] }
    }
    return chain.asReversed().joinToString(" › ")
}

