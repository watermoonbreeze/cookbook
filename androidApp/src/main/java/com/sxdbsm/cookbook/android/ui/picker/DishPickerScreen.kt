package com.sxdbsm.cookbook.android.ui.picker

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DishMiniCard
import com.sxdbsm.cookbook.android.ui.component.DishRow
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.domain.model.DishMini
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishPickerScreen(
    title: String,
    multiSelect: Boolean,
    initialSelected: List<DishMini>,
    excludeDishIds: Set<Long>,
    showRecentChips: Boolean,
    showAddNewButton: Boolean,
    onDismiss: () -> Unit,
    onAddNewDish: () -> Unit = {},
    onConfirm: (List<DishMini>) -> Unit,
    vm: DishPickerViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(excludeDishIds, initialSelected) {
        vm.configure(excludeDishIds, initialSelected)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnClickOutside = false),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(Modifier.fillMaxSize()) {
                TopAppBar(
                    title = { Text(title, fontWeight = FontWeight.SemiBold) },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Outlined.Close, contentDescription = "关闭")
                        }
                    },
                    actions = {
                        if (multiSelect) {
                            Button(
                                onClick = {
                                    onConfirm(vm.confirmSelected())
                                    onDismiss()
                                },
                                enabled = state.selected.isNotEmpty(),
                            ) { Text("完成") }
                            Spacer(Modifier.width(8.dp))
                        }
                    },
                )

                OutlinedTextField(
                    value = state.keyword,
                    onValueChange = vm::setKeyword,
                    placeholder = { Text("搜索菜名 / 标签 / 食材...") },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                )

                if (showRecentChips && state.recent.isNotEmpty() && state.keyword.isBlank()) {
                    SectionLabel("最近常吃")
                    Row(
                        modifier = Modifier
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.recent.take(8).forEach { dish ->
                            FilterChip(
                                selected = state.selected.any { it.id == dish.id },
                                onClick = {
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                                label = { Text(dish.name) },
                            )
                        }
                    }
                }

                if (state.popular.isNotEmpty() && state.keyword.isBlank()) {
                    SectionLabel("热度")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.popular, key = { it.id }) { dish ->
                            DishMiniCard(
                                dish = dish,
                                onClick = {
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                if (state.dishes.isEmpty()) {
                    EmptyState(text = "没有找到菜品", icon = "🥗")
                } else {
                    LazyColumn(Modifier.weight(1f)) {
                        items(state.dishes, key = { it.id }) { dish ->
                            DishRow(
                                dish = dish,
                                showCheckbox = multiSelect,
                                checked = state.selected.any { it.id == dish.id },
                                onCheckedChange = { vm.toggle(dish, multiSelect) },
                                onClick = {
                                    vm.toggle(dish, multiSelect)
                                    if (!multiSelect) {
                                        onConfirm(vm.confirmSelected())
                                        onDismiss()
                                    }
                                },
                            )
                        }
                    }
                }

                if (showAddNewButton) {
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        TextButton(
                            onClick = onAddNewDish,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("添加菜品", color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}
