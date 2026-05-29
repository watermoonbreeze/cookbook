package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.SectionHeader
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
    vm: HomeViewModel = koinViewModel(),
) {
    // [AI修改] collectAsStateWithLifecycle 会按 Android 生命周期订阅 StateFlow，避免后台页面继续无意义刷新。
    val ui by vm.uiState.collectAsStateWithLifecycle()
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // [AI修改] 页面 Scaffold 不再额外添加系统栏避让，配合透明状态栏形成沉浸式。
        topBar = {
            TopAppBar(
                title = { Text("今天吃什么", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                ),
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = "搜索",
                            tint = MaterialTheme.colorScheme.secondary, // [AI修改] 顶栏图标按暖杏规范使用辅助色。
                        )
                    }
                    IconButton(onClick = { /* 主题快捷切换：跳到我的页或直接切换三档 */ }) {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = "主题",
                            tint = MaterialTheme.colorScheme.secondary, // [AI修改] 顶栏图标按暖杏规范使用辅助色。
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

        item { Spacer(Modifier.height(16.dp)) }

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

        item { Spacer(Modifier.height(16.dp)) }

        // [AI修改] 只展示今天及未来真实存在的餐食记录；没有记录时整个计划区不显示。
        if (ui.plans.isNotEmpty()) {
            item { SectionHeader(title = "📅 计划", action = "全部 ▸", onActionClick = onOpenTimeline) }
            items(ui.plans, key = { it.date.toString() }) { card ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    DayMealCardView(
                        data = card,
                        onDishClick = { dish -> onOpenDish(dish.id) },
                        onEditClick = { onEditMealDate(card.date) },
                    )
                }
            }
        }
        item { Spacer(Modifier.height(80.dp)) } // [AI修改] 留底部 FAB 空间。
    }
    }
}
