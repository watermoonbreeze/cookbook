package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.ai.model.DayPlan

/**
 * @File : AiPlanScreen
 * @Time : 2026/07/09
 * @Author : SXD-AI
 * @Desc : 周期规划内容体（内嵌于 AI 推荐页的「周期计划」档，无独立 Scaffold）
 * <p>
 * [AI修改] 从独立页改为 AI 推荐页的一个档：天数1~30 + 快捷 + 生成 + 按天菜谱；保存按钮由宿主底栏承载。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiPlanBody(vm: AiPlanViewModel, modifier: Modifier = Modifier) {
    val state = vm.state
    Column(modifier = modifier) {
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
                    append(if (state.byAi) "🤖 AI 规划" else "📋 规则规划")
                    append(" · 当前季节：${state.season}（应季优先）")
                    if (state.healthAware) append(" · 已结合健康档案（利健康≥80%，参考膳食指南整理）")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))

        when {
            state.loading -> Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp))
            state.plan == null -> Text("选好天数，点「生成计划」。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp))
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
