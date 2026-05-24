package com.sxdbsm.cookbook.android.ui.dishes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.domain.model.DishMini
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishesScreen(
    onAddDish: () -> Unit,
    onOpenDish: (Long) -> Unit,
    onCopyDish: (Long) -> Unit,
    vm: DishesViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var dropdownDish by remember { mutableStateOf<DishMini?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("菜品", fontWeight = FontWeight.SemiBold) },
                actions = {
                    IconButton(onClick = onAddDish) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加菜品")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            // 搜索框
            OutlinedTextField(
                value = ui.keyword,
                onValueChange = vm::setKeyword,
                placeholder = { Text("搜索菜名 / 标签 / 食材...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )

            // 顶部热度横滑
            if (ui.popular.isNotEmpty() && ui.keyword.isBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🔥 热度", style = MaterialTheme.typography.titleMedium)
                }
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

            // 排序 tab
            TabRow(
                selectedTabIndex = DishesSortTab.values().indexOf(ui.sortTab),
                containerColor = MaterialTheme.colorScheme.surface,
            ) {
                listOf(
                    DishesSortTab.RECENT to "最近",
                    DishesSortTab.FAVORITE to "喜爱",
                    DishesSortTab.PINYIN to "拼音",
                    DishesSortTab.ALL to "全部",
                ).forEach { (tab, label) ->
                    Tab(
                        selected = ui.sortTab == tab,
                        onClick = { vm.setSortTab(tab) },
                        text = { Text(label) },
                    )
                }
            }

            // 列表
            if (ui.all.isEmpty()) {
                EmptyState(text = "还没有菜品\n点击右上 + 号添加", icon = "🥗")
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(ui.all, key = { it.id }) { dish ->
                        DishRow(
                            dish = dish,
                            onClick = { onOpenDish(dish.id) },
                            onLongClick = { dropdownDish = dish },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    // 长按弹出菜单（简易实现：用 AlertDialog 代替）
    dropdownDish?.let { d ->
        AlertDialog(
            onDismissRequest = { dropdownDish = null },
            title = { Text(d.name) },
            text = { Text("选择操作") },
            confirmButton = {
                TextButton(onClick = {
                    dropdownDish = null
                    onCopyDish(d.id)
                }) { Text("基于此另存") }
            },
            dismissButton = {
                TextButton(onClick = { dropdownDish = null }) { Text("取消") }
            },
        )
    }
}
