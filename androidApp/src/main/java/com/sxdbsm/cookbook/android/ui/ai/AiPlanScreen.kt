package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.model.DayPlan
import org.koin.androidx.compose.koinViewModel

/**
 * @File : AiPlanScreen
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划页（天数1~30 + 快捷 + 按天展示 + 保存为计划）
 * <p>
 * [AI生成] 从整个食材库规划 N 天菜谱：应季优先/营养维度均衡/不重复/有档案≥80%利健康，可保存到未来日期。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlanScreen(
    onBack: () -> Unit,
    vm: AiPlanViewModel = koinViewModel(),
) {
    val state = vm.state
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.saved) {
        if (state.saved) snackbar.showSnackbar("已保存到未来 ${state.plan?.days?.size ?: 0} 天计划")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("周期规划") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") } },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            if (state.plan != null && state.plan.days.isNotEmpty()) {
                Surface(tonalElevation = 3.dp) {
                    Button(
                        onClick = { vm.save() },
                        enabled = !state.saving,
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                    ) { Text(if (state.saving) "保存中…" else "保存为未来 ${state.plan.days.size} 天计划") }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            // 快捷天数
            Text("规划天数：${state.days} 天", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayPreset("当天", 1, state.days, vm::setDays)
                DayPreset("3天", 3, state.days, vm::setDays)
                DayPreset("一周", 7, state.days, vm::setDays)
                DayPreset("半月", 15, state.days, vm::setDays)
                DayPreset("一月", 30, state.days, vm::setDays)
            }
            Slider(
                value = state.days.toFloat(),
                onValueChange = { vm.setDays(it.toInt()) },
                valueRange = 1f..30f,
                steps = 28,
            )
            Button(onClick = { vm.generate() }, enabled = !state.loading, modifier = Modifier.fillMaxWidth()) {
                Text(if (state.loading) "生成中…" else "生成计划")
            }
            if (state.season.isNotBlank() && state.plan != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    buildString {
                        append("当前季节：${state.season}（应季优先）")
                        if (state.healthAware) append(" · 已结合健康档案（利健康占比≥80%，参考膳食指南整理）")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))

            when {
                state.loading -> Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 24.dp))
                state.plan == null -> Text("选好天数，点「生成计划」。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 24.dp))
                else -> LazyColumn(modifier = Modifier.weight(1f)) {
                    items(state.plan.days, key = { it.dayIndex }) { day -> DayCard(day) }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("仅为饮食建议参考，忌口与用量请以你的医嘱为准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPreset(label: String, value: Int, current: Int, onSelect: (Int) -> Unit) {
    FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label) })
}

@Composable
private fun DayCard(day: DayPlan) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text("第 ${day.dayIndex + 1} 天", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            day.meals.forEach { meal ->
                Spacer(Modifier.height(8.dp))
                Text(meal.mealName, style = MaterialTheme.typography.titleSmall)
                meal.dishes.forEach { d ->
                    Spacer(Modifier.height(2.dp))
                    Text("· ${d.name}", style = MaterialTheme.typography.bodyMedium)
                    if (d.reason.isNotBlank()) {
                        Text("　${d.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
