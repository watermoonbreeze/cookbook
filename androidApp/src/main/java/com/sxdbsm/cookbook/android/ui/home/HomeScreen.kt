package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
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
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
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
    val nutritionWall by vm.nutritionWall.collectAsStateWithLifecycle()
    var themeDialogOpen by remember { mutableStateOf(false) } // [AI生成] 首页主题图标直接控制弹框，不再跳转“我的”页。
    var deleteDate by remember { mutableStateOf<LocalDate?>(null) } // [AI生成] 待删除计划餐食的日期(确认弹窗)。
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
                    Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }

        // [AI生成] 营养色系墙(功能设置开启营养色系时展示)：近5周每天营养级别热力图。
        if (nutritionColorEnabled) {
            item { SectionHeader(title = "🎨 营养色系墙") }
            item {
                NutritionWall(
                    days = nutritionWall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onDayClick = onEditMealDate, // [AI生成] 点色块跳到那天(编辑/查看)
                )
            }
            item { Spacer(Modifier.height(28.dp)) }
        }

        // [AI修改] 热门菜品横向列表。
        item { SectionHeader(title = "🔥 热门", action = "更多 ▸", onActionClick = onOpenDishes) }
        if (ui.popular.isEmpty()) {
            item { EmptyState(text = "还没记录餐食，热门会随着记录自动出现", icon = "🔥") }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ui.popular, key = { it.id }) { dish ->
                        DishMiniCard(dish = dish, onClick = { onOpenDish(dish.id) })
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        // [AI修改] 最近菜品横向列表。
        item { SectionHeader(title = "⏱ 最近", action = "更多 ▸", onActionClick = onOpenDishes) }
        if (ui.recent.isEmpty()) {
            item { EmptyState(text = "暂无最近餐食", icon = "⏱") }
        } else {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(ui.recent, key = { it.id }) { dish ->
                        DishMiniCard(dish = dish, onClick = { onOpenDish(dish.id) })
                    }
                }
            }
        }

        item { Spacer(Modifier.height(28.dp)) }

        // [AI修改] 计划标题始终展示；没有计划时内容区显示“暂无计划”。
        item { SectionHeader(title = "📅 计划", action = "全部 ▸", onActionClick = onOpenTimeline) }
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
                    Text("›", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        if (ui.plans.isEmpty()) {
            item { EmptyState(text = "暂无计划", icon = "📅") }
        } else {
            items(ui.plans, key = { it.date.toString() }) { card ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DayMealCardView(
                        data = card,
                        onDishClick = { dish -> onOpenDish(dish.id) },
                        onEditClick = { onEditMealDate(card.date) },
                        onCopyClick = { onCopyMeal(card.date) }, // [AI生成] A1：复制该日为新建草稿(日期源+1可改)。
                        onDeleteClick = { deleteDate = card.date }, // [AI生成] 删除该日计划餐食(带确认)。
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) } // [AI修改] 留底部 FAB 空间。
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

    deleteDate?.let { date ->
        AlertDialog(
            onDismissRequest = { deleteDate = null },
            title = { Text("删除餐食") },
            text = { Text("确定删除 $date 的全部餐食吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteDay(date)
                    deleteDate = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteDate = null }) { Text("取消") } },
        )
    }
}
