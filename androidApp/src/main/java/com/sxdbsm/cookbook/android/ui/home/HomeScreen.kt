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
    onOpenDish: (Long) -> Unit,
    onEditMealDate: (LocalDate) -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    // [AI修改] collectAsStateWithLifecycle 会按 Android 生命周期订阅 StateFlow，避免后台页面继续无意义刷新。
    val ui by vm.uiState.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        item {
            // [AI修改] 顶部栏：页面标题和主题入口。
            TopAppBar(
                title = { Text("今天吃什么", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = { /* 主题快捷切换：跳到我的页或直接切换三档 */ }) {
                        Icon(
                            Icons.Outlined.WbSunny,
                            contentDescription = "主题",
                            tint = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                },
            )
        }
        // [AI修改] 搜索框（点击调起菜品选择弹框，MVP 先做样式）。
        item {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "搜索菜品 / 标签 / 食材...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // [AI修改] 热门菜品横向列表。
        item { SectionTitle(title = "🔥 热门", action = "更多 ▸", onActionClick = onOpenDishes) }
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
        item { SectionTitle(title = "⏱ 最近", action = "更多 ▸", onActionClick = onOpenDishes) }
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

        // [AI修改] 今天/未来计划餐食卡片列表。
        item { SectionTitle(title = "📅 计划", action = "全部 ▸", onActionClick = onOpenTimeline) }
        if (ui.plans.isEmpty()) {
            item { EmptyState(text = "还没记录今天的餐食\n点中间 + 号开始记录", icon = "🍽") }
        } else {
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

/**
 * 首页模块标题行。[AI修改]
 */
@Composable
private fun SectionTitle(title: String, action: String? = null, onActionClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.weight(1f))
        if (action != null) {
            TextButton(onClick = onActionClick) { Text(action) }
        }
    }
}
