package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onAddMeal: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenDishes: () -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        // 顶部栏
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
        // 搜索框（点击调起菜品选择弹框，MVP 先做样式）
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

        // 热门
        SectionTitle(title = "🔥 热门", action = "更多 ▸", onActionClick = onOpenDishes)
        if (ui.popular.isEmpty()) {
            EmptyState(text = "还没记录餐食，热门会随着记录自动出现", icon = "🔥")
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.popular, key = { it.id }) { dish ->
                    DishMiniCard(dish = dish)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 最近
        SectionTitle(title = "⏱ 最近", action = "更多 ▸", onActionClick = onOpenDishes)
        if (ui.recent.isEmpty()) {
            EmptyState(text = "暂无最近餐食", icon = "⏱")
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.recent, key = { it.id }) { dish ->
                    DishMiniCard(dish = dish)
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // 计划
        SectionTitle(title = "📅 计划", action = "全部 ▸", onActionClick = onOpenTimeline)
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            if (ui.plans.isEmpty()) {
                EmptyState(text = "还没记录今天的餐食\n点中间 + 号开始记录", icon = "🍽")
            } else {
                ui.plans.forEach { card ->
                    DayMealCardView(data = card)
                }
            }
        }
        Spacer(Modifier.height(80.dp)) // 留底部 FAB 空间
    }
}

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
