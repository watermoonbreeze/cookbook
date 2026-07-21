package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    // [AI修改] P1 家族化统一(§9.35)：控件区做"高频直出/低频折叠"——天数+生成直出、人数+风格收进「计划设置」弹层(与另两档「筛选」弹层同构)。
    var settingsOpen by remember { mutableStateOf(false) }
    // 非默认值(非"综合"风格)→设置入口加 ● 圆点提示,与另两档一致。
    val hasNonDefault = state.recommendStyle != com.sxdbsm.cookbook.ai.RecommendationStyle.BALANCED
    // [AI修改] 整个页面(控件+计划)放同一 LazyColumn：生成计划后上滑，控件整体滚走、给计划更大空间。
    LazyColumn(modifier = modifier.fillMaxSize()) {
        item {
            // —— 高频直出：天数快捷 chip ——
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("天数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("当天" to 1, "3天" to 3, "一周" to 7, "半月" to 15, "一月" to 30)) { (label, value) ->
                        DayPreset(label, value, state.days, vm::setDays)
                    }
                }
            }
            // 天数精调 Slider + 当前天数。
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
            // —— 高频直出：「计划设置」入口 + 「生成」——低频(人数/风格)收进弹层。
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { settingsOpen = true }) {
                    Text("计划设置" + if (hasNonDefault) " ●" else "")
                }
                Spacer(Modifier.weight(1f))
                com.sxdbsm.cookbook.android.ui.component.CapsuleButton(
                    text = if (state.loading) "生成中…" else "生成",
                    onClick = { vm.generate() },
                    enabled = !state.loading,
                )
            }
            Text(
                "${state.people} 人 · 正餐约 ${mainRange.first}~${mainRange.last} 菜",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // —— P2 营养线概览卡(生成结果首张·总卡·"总—分"层级在逐日卡之上)。domain 已算好,只呈现。
        val line = state.nutritionLine
        if (state.plan != null && line != null && line.dayCount > 0) {
            item { NutritionLineCard(line, state.nutritionAdvices) }
        }

        // —— R3：生成上下文条(季节/健康)独立成条,移出控件区(概览卡之后·逐日卡之前)。
        if (state.season.isNotBlank() && state.plan != null) {
            item {
                val partialRule = state.byAi && state.plan.days.any { d -> d.meals.any { it.fromRule } }
                Text(
                    buildString {
                        append(if (state.byAi) "AI 规划" else "规则规划")
                        if (partialRule) append("（部分餐次由规则补充，已在下方标注）")
                        append(" · 当前季节：${state.season}（应季优先）")
                        if (state.healthAware) append(" · 已参考健康档案（膳食指南口径整理，仅供参考）")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
        }

        when {
            state.loading -> item { Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
            state.error != null -> item { Text(state.error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 16.dp)) }
            state.plan == null -> item {
                // R4：空态给下一步(呼应营养线文案),对齐另两档居中提示。
                com.sxdbsm.cookbook.android.ui.component.EmptyState(
                    text = "还没安排这一周\n排上几天，就能看到这一周搭得均不均衡",
                    icon = "🗓",
                    modifier = Modifier.padding(top = 24.dp),
                )
            }
            else -> {
                items(state.plan.days, key = { it.dayIndex }) { day ->
                    // [AI生成] 每天标注明确日期(计划起始日 + dayIndex)，让"第N天"对应真实日期。
                    val dateLabel = state.planStartDate?.let { s ->
                        val d = com.sxdbsm.cookbook.util.DateTime.plusDays(s, day.dayIndex)
                        "${d.monthNumber}月${d.dayOfMonth}日"
                    }
                    DayCard(day, dateLabel, state.nutritionByDishId)
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    Text("仅为饮食建议参考，忌口与用量请以你的医嘱为准。", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    // —— 低频折叠：「计划设置」弹层(人数 + 推荐风格)。条件 emit·ModalBottomSheet 内无 early return(守崩溃红线)。
    if (settingsOpen) {
        ModalBottomSheet(onDismissRequest = { settingsOpen = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
            ) {
                Text("计划设置", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("用餐人数", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    com.sxdbsm.cookbook.android.ui.component.MiniStepper(
                        valueText = "${state.people} 人",
                        onMinus = { vm.setPeople(state.people - 1) },
                        onPlus = { vm.setPeople(state.people + 1) },
                        minusEnabled = state.people > 1,
                        plusEnabled = state.people < com.sxdbsm.cookbook.ai.MealPortion.MAX_PEOPLE,
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text("推荐风格", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                // [AI修改] Google审🟡:提取 styleIdx 复用(去重·与另两档 RecommendFilterSheet 对齐)。
                val styleIdx = RECOMMEND_STYLE_OPTIONS.indexOfFirst { it.second == state.recommendStyle }.coerceAtLeast(0)
                Text(
                    RECOMMEND_STYLE_OPTIONS[styleIdx].third,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                com.sxdbsm.cookbook.android.ui.component.SegmentedControl(
                    options = RECOMMEND_STYLE_OPTIONS.map { it.first },
                    selectedIndex = styleIdx,
                    onSelect = { idx -> vm.setStyle(RECOMMEND_STYLE_OPTIONS[idx].second) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayPreset(label: String, value: Int, current: Int, onSelect: (Int) -> Unit) {
    FilterChip(selected = current == value, onClick = { onSelect(value) }, label = { Text(label) })
}

/**
 * 「一周营养搭配」概览卡（营养线 P2·§9.35 总卡）。[AI生成]
 *
 * 把 domain 已算的整周营养线"盛出来"：整体均衡度色点(去红·复用 nutritionLevelColor)+已吃到的大类 + 跨天补充建议。
 * 守免责(膳食结构参考·非医嘱)、不制造焦虑(缺口用鼓励口吻·色点不上红)。
 */
@Composable
private fun NutritionLineCard(
    line: com.sxdbsm.cookbook.domain.NutritionLine,
    advices: List<com.sxdbsm.cookbook.domain.LineAdvice>,
) {
    // 均衡度 0~100 → 级别 0~4 → 去红中性色(与色系墙/膳食报告同 nutritionLevelColor·不上红)。
    val level = when {
        line.balanceScore >= 80 -> 4
        line.balanceScore >= 60 -> 3
        line.balanceScore >= 40 -> 2
        else -> 1
    }
    val dotColor = com.sxdbsm.cookbook.android.ui.component.nutritionLevelColor(level)
    val coveredText = com.sxdbsm.cookbook.domain.FoodGroup.Group.entries
        .filter { it in line.coveredGroups }.joinToString("·") { it.label }
    com.sxdbsm.cookbook.android.ui.component.InsetGroup(title = "一周营养搭配") {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(9.dp).clip(androidx.compose.foundation.shape.CircleShape).background(dotColor))
                Spacer(Modifier.width(8.dp))
                Text("整体搭配 ${line.balanceScore} 分", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            if (coveredText.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("这一周吃到了 $coveredText", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            }
            advices.forEach { adv ->
                Spacer(Modifier.height(6.dp))
                Text("· ${adv.text}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "按食材参考数据估算膳食结构，仅供了解，非医嘱。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun DayCard(
    day: DayPlan,
    dateLabel: String? = null,
    nutritionByDishId: Map<Long, com.sxdbsm.cookbook.android.ui.component.DishNutritionUi> = emptyMap(), // [AI生成] §9.36:每菜营养(整份热量+宏量·与AI推荐同款)
) {
    // [AI生成] 库存挂钩关→周期规划不显缺料/采购标注、缺料菜不变灰(与食历/详情同口径去噪)。
    val pantryHookOn by com.sxdbsm.cookbook.android.ui.component.rememberPantryHookEnabled()
    // [AI修改] P1 家族化(§9.35 R1)：灰卡 surfaceVariant→白卡 surface(与另两档结果卡同族)·卡内 14→16·卡间 Spacer(10)·卡头 primary→onSurface(强调色只给可交互)。
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "第 ${day.dayIndex + 1} 天" + (dateLabel?.let { " · $it" } ?: ""),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface,
            )
            day.meals.forEach { meal ->
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.mealName, style = MaterialTheme.typography.titleSmall)
                    if (meal.fromRule) {
                        // [AI修改] P1(§9.35 R6)：规则补充胶囊徽标→labelSmall 灰字(与另两档"纯文字浅色标注"语言统一)。
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "· 规则补充",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
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
                        // [AI生成] §9.36:每菜营养行(整份热量+宏量·热量数字受开关·钠偏高浅灰提示·缺数据"营养待完善")——与AI推荐逐菜同款。
                        com.sxdbsm.cookbook.android.ui.component.DishNutritionLine(nutritionByDishId[d.id])
                    }
                    if (pantryHookOn && d.purchaseNames.isNotEmpty()) {
                        // [AI修改] P1(§9.35 R6)：去装饰 emoji(纯文字标注·error 色保留=需注意信息)。
                        Text("　采购：${d.purchaseNames.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (pantryHookOn && d.shortageNames.isNotEmpty()) {
                        Text("　缺：${d.shortageNames.joinToString("、")}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}
