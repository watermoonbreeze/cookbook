package com.sxdbsm.cookbook.android.ui.addmeal

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.picker.DishPickerScreen
import com.sxdbsm.cookbook.android.ui.theme.ExtendedColorsHolder
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDayFoodScreen(
    onBack: () -> Unit,
    vm: AddMealViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    var pickerOpen by remember { mutableStateOf(false) }

    LaunchedEffect(state.done) {
        if (state.done) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("添加餐食", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = { vm.save() },
                        enabled = state.dishes.isNotEmpty() && !state.saving,
                    ) { Text(if (state.isPlan) "保存计划" else "保存") }
                    Spacer(Modifier.width(8.dp))
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            if (state.isPlan) {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Text(
                        "📌 这是 ${state.date} 的计划，未来才会真正吃",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }

            FieldLabel("日期")
            OutlinedTextField(
                value = state.date.toString(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            FieldLabel("餐次")
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.mealTypes.forEach { mt ->
                    FilterChip(
                        selected = mt.id == state.mealTypeId,
                        onClick = { vm.setMealType(mt.id) },
                        label = { Text(mt.name) },
                    )
                }
            }

            FieldLabel("用餐时间")
            OutlinedTextField(
                value = "${state.mealTime}",
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )

            FieldLabel("菜品")
            OutlinedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Column {
                    if (state.dishes.isEmpty()) {
                        Text(
                            "还没选菜品",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        state.dishes.forEach { d ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(d.name, modifier = Modifier.weight(1f))
                                IconButton(onClick = { vm.removeDish(d.id) }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "移除")
                                }
                            }
                            Divider()
                        }
                    }
                }
            }
            TextButton(
                onClick = { pickerOpen = true },
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("添加菜品", color = MaterialTheme.colorScheme.tertiary)
            }

            FieldLabel("备注")
            OutlinedTextField(
                value = state.note,
                onValueChange = vm::setNote,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text("（可选）") },
                minLines = 2,
            )
            Spacer(Modifier.height(80.dp))
        }
    }

    if (pickerOpen) {
        val mealName = state.mealTypes.firstOrNull { it.id == state.mealTypeId }?.name.orEmpty()
        DishPickerScreen(
            title = if (mealName.isBlank()) "添加菜品" else "添加到$mealName",
            multiSelect = true,
            initialSelected = emptyList(),
            excludeDishIds = state.dishes.map { it.id }.toSet(),
            showRecentChips = true,
            showAddNewButton = false,
            onDismiss = { pickerOpen = false },
            onConfirm = { selected -> vm.addDishes(selected) },
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 6.dp),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}
