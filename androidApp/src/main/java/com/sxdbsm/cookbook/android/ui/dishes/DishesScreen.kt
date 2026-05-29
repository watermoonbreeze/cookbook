package com.sxdbsm.cookbook.android.ui.dishes

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppSearchField
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.domain.model.DishMini
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
    onCopyDish: (Long) -> Unit,
    vm: DishesViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var dropdownDish by remember { mutableStateOf<DishMini?>(null) }
    val hotRankById = remember(ui.all) {
        ui.all
            .filter { it.preference > 0 }
            .sortedByDescending { it.preference }
            .take(3)
            .mapIndexed { index, dish -> dish.id to index + 1 }
            .toMap()
    } // [AI修改] 菜品 Item 右侧喜爱值前 3 名显示热度标识。

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
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            if (ui.popular.isNotEmpty() && ui.keyword.isBlank()) {
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
            }

            if (ui.all.isEmpty()) {
                item { EmptyState(text = "还没有菜品\n点击右上 + 号添加", icon = "🥗") }
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
    }

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
