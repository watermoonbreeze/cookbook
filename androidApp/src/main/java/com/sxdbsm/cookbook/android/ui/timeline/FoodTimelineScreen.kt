package com.sxdbsm.cookbook.android.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodTimelineScreen(vm: TimelineViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize()) {
        TopAppBar(title = { Text("食历", fontWeight = FontWeight.SemiBold) })

        // 时间轴顶部占位（MVP 简化：显示范围文字 + 月份刻度后续补）
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "◀  ${state.rangeMin ?: "—"}  ━●━  ${state.rangeMax ?: "—"}  ▶",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.pages.isEmpty() && !state.loading) {
            EmptyState(text = "还没有任何餐食记录\n中间 + 号开始记录", icon = "📅")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.pages, key = { it.date.toString() }) { card ->
                    DayMealCardView(data = card)
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = { vm.loadMore(state.pages.size) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("加载更多 ▾") }
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
    }
}
