package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    val mainRange = com.sxdbsm.cookbook.ai.MealPortion.mainRange(state.people)
    // [AI修改] 整个页面(控件+计划)放同一 LazyColumn：生成计划后上滑，控件整体滚走、给计划更大空间。
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            // 规划天数：标签 + 周期快捷 整合成一行(可横滑)。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("天数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("当天" to 1, "3天" to 3, "一周" to 7, "半月" to 15, "一月" to 30)) { (label, value) ->
                        DayPreset(label, value, state.days, vm::setDays)
                    }
                }
            }
            // [AI修改] 天数显示补回：进度条右侧显示当前"N 天"。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = state.days.toFloat(),
                    onValueChange = { vm.setDays(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 28,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "${state.days} 天",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            // 用餐人数(MiniStepper) 与「生成」同一行，生成按钮在右侧。
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("用餐人数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                com.sxdbsm.cookbook.android.ui.component.MiniStepper(
                    valueText = "${state.people} 人",
                    onMinus = { vm.setPeople(state.people - 1) },
                    onPlus = { vm.setPeople(state.people + 1) },
                    minusEnabled = state.people > 1,
                    plusEnabled = state.people < com.sxdbsm.cookbook.ai.MealPortion.MAX_PEOPLE,
                )
                Spacer(Modifier.weight(1f))
                com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                    text = if (state.loading) "生成中…" else "生成",
                    onClick = { vm.generate() },
                    enabled = !state.loading,
                )
            }
            Text(
                "正餐约 ${mainRange.first}~${mainRange.last} 菜",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            // [AI生成] 推荐风格(与AI推荐共用)：影响规划的新颖/健康/搭配/去重权重；已有计划则改风格即重生成。
            Spacer(Modifier.height(6.dp))
            Text(
                RECOMMEND_STYLE_OPTIONS[RECOMMEND_STYLE_OPTIONS.indexOfFirst { it.second == state.recommendStyle }.coerceAtLeast(0)].third,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
                options = RECOMMEND_STYLE_OPTIONS.map { it.first },
                selectedIndex = RECOMMEND_STYLE_OPTIONS.indexOfFirst { it.second == state.recommendStyle }.coerceAtLeast(0),
                onSelect = { idx -> vm.setStyle(RECOMMEND_STYLE_OPTIONS[idx].second) },
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.season.isNotBlank() && state.plan != null) {
                Spacer(Modifier.height(6.dp))
                val partialRule = state.byAi && state.plan.days.any { d -> d.meals.any { it.fromRule } }
                Text(
                    buildString {
                        append(if (state.byAi) "AI 规划" else "规则规划") // [AI修改] 文案:去标题装饰emoji(文字本身已表意)
                        if (partialRule) append("（部分餐次由规则补充，已在下方标注）")
                        append(" · 当前季节：${state.season}（应季优先）")
                        if (state.healthAware) append(" · 已参考健康档案（膳食指南口径整理，仅供参考）") // [AI修改] 文案:去伪精确健康断言"利健康≥80%"(守免责·非医嘱)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
        }

        when {
            state.loading -> item { Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            state.error != null -> item { Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
            state.plan == null -> item { Text("选好天数，点「生成计划」。", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 16.dp)) }
            else -> {
                items(state.plan.days, key = { it.dayIndex }) { day ->
                    // [AI生成] 每天标注明确日期(计划起始日 + dayIndex)，让"第N天"对应真实日期。
                    val dateLabel = state.planStartDate?.let { s ->
                        val d = com.sxdbsm.cookbook.util.DateTime.plusDays(s, day.dayIndex)
                        "${d.monthNumber}月${d.dayOfMonth}日"
                    }
                    DayCard(day, dateLabel)
                }
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
private fun DayCard(day: DayPlan, dateLabel: String? = null) {
    // [AI生成] 库存挂钩关→周期规划不显缺料/采购标注、缺料菜不变灰(与食历/详情同口径去噪)。
    val pantryHookOn by com.sxdbsm.cookbook.android.ui.component.rememberPantryHookEnabled()
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                "第 ${day.dayIndex + 1} 天" + (dateLabel?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
            )
            day.meals.forEach { meal ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.mealName, style = MaterialTheme.typography.titleSmall)
                    if (meal.fromRule) {
                        // [AI生成] 该餐 AI 未覆盖、由规则补充，标注区分。
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(
                                "规则补充",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            )
                        }
                    }
                }
                meal.dishes.forEach { d ->
                    Spacer(Modifier.height(2.dp))
                    // [AI生成] 库存不足(缺料/采购)的菜半透明灰显 + 标注，与食历缺料样式一致。库存挂钩关则不判(去噪)。
                    val lack = pantryHookOn && (d.shortageNames.isNotEmpty() || d.purchaseNames.isNotEmpty())
                    Column(modifier = if (lack) Modifier.alpha(0.6f) else Modifier) { // [AI修改] 0.45→0.6, 缺料菜也保持可读
                        // [AI修改] 菜名显式用 onSurface 深色 + Medium 字重，避免在 surfaceVariant 卡片上默认淡色看不清。
                        Text(
                            "· ${d.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (d.reason.isNotBlank()) {
                            Text("　${d.reason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    if (pantryHookOn && d.purchaseNames.isNotEmpty()) {
                        Text("　🛒 采购：${d.purchaseNames.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (pantryHookOn && d.shortageNames.isNotEmpty()) {
                        Text("　⚠ 缺：${d.shortageNames.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
