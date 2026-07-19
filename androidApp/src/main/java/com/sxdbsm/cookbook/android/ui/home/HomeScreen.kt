package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.SectionHeader
import com.sxdbsm.cookbook.android.ui.component.ThemeModeDialog
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * 首页页面。[AI修改]
 *
 * `@Composable` 可以理解为 Compose 的“UI 函数”：输入状态和回调，输出界面树。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenTimeline: () -> Unit,
    onOpenDishes: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenDish: (Long) -> Unit,
    onEditMealDate: (LocalDate) -> Unit,
    onOpenTimelineAt: (LocalDate) -> Unit = {}, // [AI生成] 营养色系墙点色块→食历定位该日
    onCopyMeal: (LocalDate) -> Unit = {}, // [AI生成] A1：首页计划卡"复制"入口(与食历页一致，家庭高频"照着某天再吃一次")
    onOpenWeekPlan: () -> Unit = {}, // [AI生成] B3：一周计划入口
    onOpenAiRecommend: () -> Unit = {},
    vm: HomeViewModel = koinViewModel(),
) {
    // [AI修改] collectAsStateWithLifecycle 会按 Android 生命周期订阅 StateFlow，避免后台页面继续无意义刷新。
    val ui by vm.uiState.collectAsStateWithLifecycle()
    val mode by vm.themeMode.collectAsStateWithLifecycle()
    // [AI生成] 营养色系墙：功能设置开启时展示近 5 周每天营养级别热力图。
    val prefs = org.koin.compose.koinInject<com.sxdbsm.cookbook.data.repository.PreferenceRepository>()
    val nutritionColorEnabled by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.NUTRITION_COLOR_ENABLED, false)
    }.collectAsStateWithLifecycle(false)
    // [AI生成] 热量数值显示(与营养色系独立)：控制首页「今日营养」卡的数字呈现。
    val calorieNumberEnabled by remember(prefs) {
        prefs.observeFlag(com.sxdbsm.cookbook.domain.model.PreferenceKeys.CALORIE_NUMBER_ENABLED, false)
    }.collectAsStateWithLifecycle(false)
    val nutritionWall by vm.nutritionWall.collectAsStateWithLifecycle()
    val yearAverages by vm.yearAverages.collectAsStateWithLifecycle()
    val todayNutrition by vm.todayNutrition.collectAsStateWithLifecycle()
    val focusSwitcher by vm.focusSwitcher.collectAsStateWithLifecycle() // [AI生成] 多人关注:今日卡成员切换器
    var themeDialogOpen by remember { mutableStateOf(false) } // [AI生成] 首页主题图标直接控制弹框，不再跳转“我的”页。
    var wallExpanded by rememberSaveable { mutableStateOf(true) } // [AI生成] 营养色系墙折叠态：默认展开(整墙显示)，收起后标题右侧显示昨/今/明三色块。
    val appSnackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current // [AI生成] B-5：删整天撤销 Snackbar
    // [AI修改] 苹果风格：首页用大标题(Large Title)，下滑折叠为小标题。
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 页面 Scaffold 不再额外添加系统栏避让，配合透明状态栏形成沉浸式。
        topBar = {
            LargeTopAppBar(
                title = { Text("今天吃什么", fontWeight = FontWeight.Bold) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.primary, // [AI修改] 苹果风格：顶栏图标统一用 accent。
                        )
                    }
                    IconButton(onClick = { themeDialogOpen = true }) {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = "主题",
                            tint = MaterialTheme.colorScheme.primary, // [AI修改] 苹果风格：顶栏图标统一用 accent。
                        )
                    }
                },
            )
        },
    ) { padding ->
    LazyColumn(
        modifier = Modifier
            .padding(padding)
            .fillMaxSize(),
    ) {
        // [AI生成] AI 推荐下一餐入口卡。
        item {
            // [AI修改] 苹果风格：AI 入口去大色块，改白卡 + 小面积 accent(图标/标题)。
            Surface(
                onClick = onOpenAiRecommend,
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🤖", style = MaterialTheme.typography.headlineMedium)
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(
                            "AI 推荐",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            "用你现有的食材，帮你搭配今天吃什么",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline) // [AI修改] UX:统一列表 chevron 图标(替代文本"›",对齐苹果分组列表)
                }
            }
        }

        // [AI生成] 营养色系墙(功能设置开启营养色系时展示)：整年每天营养级别热力图，可折叠。
        if (nutritionColorEnabled) {
            item {
                // [AI修改] 自定义标题行：展开态只显示标题+收起按钮；折叠态显示"昨天今天明天"三色块+展开按钮。
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "营养色系墙", // [AI修改] 文案:去标题内嵌装饰emoji(苹果式图标与文字分离,右侧已有展开图标)
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    if (!wallExpanded) {
                        NutritionThreeDay(days = nutritionWall)
                        Spacer(Modifier.width(6.dp))
                    }
                    IconButton(onClick = { wallExpanded = !wallExpanded }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            if (wallExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                            contentDescription = if (wallExpanded) "收起色系墙" else "展开色系墙",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (wallExpanded) {
                item {
                    NutritionWall(
                        days = nutritionWall,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        onDayClick = onOpenTimelineAt, // [AI修改] 点色块→食历定位该日餐食(不再进编辑页)
                        yearAverages = yearAverages, // [AI生成] 往年平均色系(有往年数据才显示)
                    )
                }
            }
            // [AI生成] 3c：今日营养分配卡(有当天营养数据 且 开启"热量数值显示"才显示)。
            todayNutrition?.takeIf { calorieNumberEnabled }?.let { tn ->
                item {
                    NutritionTodayCard(
                        tn,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        switcher = focusSwitcher, // [AI生成] 多人关注:成员切换器
                        onSelectViewing = vm::setViewing,
                    )
                }
            }
            item { Spacer(Modifier.height(28.dp)) }
        }

        // [AI生成] UX深挖#7：今天独立成"今日"区，与"计划(未来)"分开——消除"今天混在计划里、语义模糊"。
        //   数据层 observeTodayPlusFuture 已把今天(isToday)与未来分开，这里仅在展示层拆分渲染，不改 VM 数据流。
        val todayCard = ui.plans.firstOrNull { it.isToday }
        val futureCards = ui.plans.filter { !it.isToday }
        item { SectionHeader(title = "今日") }
        if (todayCard == null) {
            // 今天没记：空态给下一步(§9.6)，比"混在计划里看不到今天"更清晰。
            item {
                EmptyState(
                    text = "今天还没记，随手记一餐吧",
                    icon = "🍽",
                    actionLabel = "记一餐",
                    onAction = { onEditMealDate(com.sxdbsm.cookbook.util.DateTime.today()) },
                )
            }
        } else {
            item(key = "today-${todayCard.date}") {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DayMealCardView(
                        data = todayCard,
                        onDishClick = { dish -> onOpenDish(dish.id) },
                        onEditClick = { onEditMealDate(todayCard.date) },
                        onCopyClick = { onCopyMeal(todayCard.date) },
                        onDeleteClick = {
                            val d = todayCard.date
                            vm.deleteDayUndoable(d) { onUndo -> appSnackbar?.showUndo("已删除 $d 的餐食", onUndo = onUndo) }
                        },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }

        // [AI修改] "计划"区只管未来(今天已独立到"今日"区，UX深挖#7)。
        item { SectionHeader(title = "计划", action = "全部 ▸", onActionClick = onOpenTimeline) } // [AI修改] 文案:去标题装饰emoji(与同页其余SectionHeader一致)
        // [AI生成] B3：一周计划入口——"周末排下周饭"整周概览 + 逐日安排。
        // [AI修改] 苹果风格：去描边按钮，改无边框白卡点击行 + chevron。
        item {
            Surface(
                onClick = onOpenWeekPlan,
                color = MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.medium,
                tonalElevation = 0.dp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            ) {
                Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("🗓", style = MaterialTheme.typography.titleMedium)
                    Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                        Text("一周计划", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("排下周饭：整周概览 + 逐日安排", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Outlined.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline) // [AI修改] UX:统一列表 chevron 图标(替代文本"›",对齐苹果分组列表)
                }
            }
        }
        // [AI生成] #2:说明计划卡只展示"下一个有安排的日期"(观 observeTodayPlusFuture 逻辑,今天已独立"今日"区);仅有未来计划时显示。
        if (futureCards.isNotEmpty()) {
            item {
                Text(
                    "只显示下一个有安排的日期，点「全部」看完整食历",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
        }
        if (futureCards.isEmpty()) {
            // [AI修改] UX深挖#7：未来空态给下一步(排一周计划)，比"暂无计划"更有引导。
            item { EmptyState(text = "接下来还没安排", icon = "📅", actionLabel = "排一周计划", onAction = onOpenWeekPlan) }
        } else {
            items(futureCards, key = { it.date.toString() }) { card ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DayMealCardView(
                        data = card,
                        onDishClick = { dish -> onOpenDish(dish.id) },
                        onEditClick = { onEditMealDate(card.date) },
                        onCopyClick = { onCopyMeal(card.date) }, // [AI生成] A1：复制该日为新建草稿(日期源+1可改)。
                        onDeleteClick = { // [AI修改] B-5/§9.12：可逆删除改软删+撤销 Snackbar，不再硬确认。
                            val d = card.date
                            vm.deleteDayUndoable(d) { onUndo -> appSnackbar?.showUndo("已删除 $d 的餐食", onUndo = onUndo) }
                        },
                    )
                }
            }
        }

        // [AI修改] 用户 2026-07-16：去除首页"🔥热门/⏱最近"发现区——菜品页(最近/喜爱Tab)已有相关推荐，
        //   首页聚焦"今天吃了啥/该吃啥"(今日卡+计划含今天)，更克制、少重复。
        item { Spacer(Modifier.height(80.dp)) } // 留底部 FAB 空间。
    }
    }

    if (themeDialogOpen) {
        ThemeModeDialog(
            current = mode,
            onSelect = {
                vm.setThemeMode(it)
                themeDialogOpen = false
            },
            onDismiss = { themeDialogOpen = false },
        )
    }

}
