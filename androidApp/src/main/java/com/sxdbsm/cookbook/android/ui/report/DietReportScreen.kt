package com.sxdbsm.cookbook.android.ui.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.AppTopBar
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.android.ui.component.InsetGroup
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.android.ui.component.nutritionLevelColor
import com.sxdbsm.cookbook.domain.model.CountItem
import com.sxdbsm.cookbook.domain.model.DietReport
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

/**
 * @File : DietReportScreen
 * @Time : 2026/07/18
 * @Author : SXD-AI
 * @Desc : 饮食报告页（周/月 · 家庭/个人 · 回顾非考核 · 守免责）
 * <p>
 * 顶部周期/视角/期次切换；主体概览卡→要点卡→各维度卡；自绘轻量图(进度条/分段条/结构日历色块)，零三方库。
 * 营养维度守"仅供参考·非医嘱"。空态给"去记一餐"。
 * <p>
 * [AI生成] 报告模块 MVP（用户 2026-07-18 拍板）。
 **/
@Composable
fun DietReportScreen(
    onBack: () -> Unit,
    onGoAddMeal: (kotlinx.datetime.LocalDate) -> Unit,
    onGoWeekPlan: (kotlinx.datetime.LocalDate) -> Unit = {}, // [AI生成] 空周期→跳一周计划(带该周日期·以周为单位)
) {
    val vm: DietReportViewModel = koinViewModel()
    val st by vm.state.collectAsStateWithLifecycle()

    Scaffold(topBar = { AppTopBar(title = "饮食报告", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // 控制区：周期分段 + 视角切换。
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SegmentedControl(
                    options = listOf("周", "月"),
                    selectedIndex = if (st.period == ReportPeriod.WEEK) 0 else 1,
                    onSelect = { vm.setPeriod(if (it == 0) ReportPeriod.WEEK else ReportPeriod.MONTH) },
                    modifier = Modifier.width(140.dp),
                )
                Spacer(Modifier.weight(1f))
                SegmentedControl(
                    options = listOf("家庭", "个人"),
                    selectedIndex = if (st.personal) 1 else 0,
                    onSelect = { vm.setPersonal(it == 1) },
                    modifier = Modifier.width(140.dp),
                )
            }
            // [AI生成] 多人关注(§9.23):个人视角 ≥2 关注人时成员切换器(与今日卡共用指针·一处切两处同步)。≤4 均分 / >4 横滚。
            if (st.personal && st.focusMembers.size >= 2) {
                val names = st.focusMembers.map { it.name }
                val selIdx = st.focusMembers.indexOfFirst { it.id == st.viewingId }.coerceAtLeast(0)
                val onSel: (Int) -> Unit = { vm.setViewing(st.focusMembers[it].id) }
                Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
                    if (st.focusMembers.size <= 4) {
                        SegmentedControl(options = names, selectedIndex = selIdx, onSelect = onSel, modifier = Modifier.fillMaxWidth())
                    } else {
                        com.sxdbsm.cookbook.android.ui.component.PrimaryTabRow(
                            options = names, selectedIndex = selIdx, onSelect = onSel, scrollable = true, modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            // 期次翻页。
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                IconButton(onClick = { vm.prevPeriod() }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一期", tint = MaterialTheme.colorScheme.primary)
                }
                Text(st.periodLabel, style = MaterialTheme.typography.bodyMedium)
                IconButton(onClick = { vm.nextPeriod() }, enabled = st.canGoNewer) {
                    Icon(
                        Icons.Outlined.ChevronRight, contentDescription = "下一期",
                        tint = if (st.canGoNewer) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    )
                }
            }
            Divider()

            when {
                st.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                st.report?.hasData != true -> EmptyState(
                    // [AI修改] 用户2026-07-21:空周期统一跳"一周计划"(带该周日期·以周为单位·月则含月首日所在周)，从整周维度把这几天安排上,比单条记一餐更贴"规划"。
                    text = "这段时间还没记一餐\n去一周计划把这几天安排上",
                    icon = "🗓",
                    actionLabel = "去一周计划",
                    onAction = { onGoWeekPlan(st.weekJumpDate) },
                )
                else -> ReportBody(st, st.report!!)
            }
        }
    }
}

@Composable
private fun ReportBody(st: DietReportUiState, r: DietReport) {
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 24.dp)) {
        // 概览卡。
        item { OverviewCard(st, r) }
        // 要点卡。
        val tips = buildTips(st, r)
        if (tips.isNotEmpty()) item { TipsCard(tips) }
        // 记录概况。
        item {
            InsetGroup(title = "记录概况") {
                Column(Modifier.padding(14.dp)) {
                    StatRow("记录天数", r.coverageText)
                    LinearProgressIndicator(
                        progress = if (r.periodDays == 0) 0f else r.recordedDays.toFloat() / r.periodDays,
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).padding(top = 4.dp, bottom = 6.dp),
                    )
                    StatRow("总餐次数", "${r.mealCount} 餐")
                    StatRow("平均每天菜数", "${oneDecimal(r.avgDishesPerDay)} 道")
                }
            }
        }
        // 菜品。
        item {
            InsetGroup(title = "菜品") {
                Column(Modifier.padding(14.dp)) {
                    StatRow("不同菜品数", "${r.distinctDishes} 道")
                    if (r.topDishes.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("常吃菜品", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        r.topDishes.forEach { CountRow(it, "次") }
                    }
                }
            }
        }
        // 食材。
        item {
            InsetGroup(title = "食材") {
                Column(Modifier.padding(14.dp)) {
                    StatRow("用到食材种数", "${r.ingredientKinds} 种")
                    if (r.topIngredients.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text("常用食材", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        r.topIngredients.forEach { CountRow(it, "次") }
                    }
                }
            }
        }
        // 膳食结构。
        item {
            InsetGroup(title = "膳食结构") {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("本期膳食均衡度", modifier = Modifier.weight(1f))
                        Text("${oneDecimal(r.avgLevel)} 级", fontWeight = FontWeight.SemiBold, color = levelColor(r.avgLevel.roundToInt()))
                        Spacer(Modifier.width(8.dp))
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(levelColor(r.avgLevel.roundToInt())))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("结构日历", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    StructureCalendar(r.perDayLevels)
                    if (r.structureGaps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        // [AI修改] 文案审校🔴1:nutritionGaps 返回纯名词(优质蛋白/主食…)，报告里补成完整句才读得懂、且鼓励非责备。
                        r.structureGaps.forEach { TipRow(com.sxdbsm.cookbook.android.ui.theme.LocalExtendedColors.current.warning, "这段时间${it}吃得偏少，可以多安排点") } // [AI修改] 提示色收敛语义色(待改进=warning)
                    } else {
                        Spacer(Modifier.height(8.dp))
                        TipRow(com.sxdbsm.cookbook.android.ui.theme.LocalExtendedColors.current.success, "五大类基本吃到，结构均衡") // [AI修改] 提示色收敛语义色(均衡=success)
                    }
                }
            }
        }
        // 营养摄入(仅个人视角且有数据)。
        r.personal?.let { p ->
            item {
                InsetGroup(title = "营养摄入（${st.memberName.ifBlank { "个人" }}）") {
                    Column(Modifier.padding(14.dp)) {
                        StatRow("日均热量", if (p.targetKcal != null) "${p.avgKcal} / 目标 ${p.targetKcal} 千卡" else "${p.avgKcal} 千卡")
                        if (p.targetKcal != null) {
                            StatRow("在目标内", "${p.onTargetDays} / ${r.recordedDays} 天")
                            Text("即当天热量落在你设定的目标范围内", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("蛋白·脂肪·碳水占比", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        MacroBar(p.proteinPct, p.fatPct, p.carbPct)
                        Spacer(Modifier.height(6.dp))
                        StatRow("日均钠", "${p.avgSodiumMg} mg")
                        StatRow("日均钾", "${p.avgPotassiumMg} mg")
                        StatRow("日均膳食纤维", "${p.avgFiberG} g")
                        Spacer(Modifier.height(6.dp))
                        // [AI修改] 文案审校🔴3:免责补"估算·来自食材参考数据"(慢病敏感数值别被当权威摄入量)。
                        Text("· 营养按你的饭量估算，来自食材参考数据，仅供了解，非医嘱", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
        if (st.personal && !st.hasFocusMember) {
            item {
                InsetGroup {
                    Text(
                        "还没关注家人，去家庭档案关注一位，就能看 TA 的营养摄入",
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun OverviewCard(st: DietReportUiState, r: DietReport) {
    val p = r.personal
    val summary = buildSummary(st, r)
    InsetGroup {
        Column(Modifier.padding(16.dp)) {
            Text(summary, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                if (st.personal && p != null) {
                    StatBig(Modifier.weight(1f), "${p.avgKcal}", "日均千卡")
                    StatBig(Modifier.weight(1f), "${p.onTargetDays}", "在目标内")
                } else {
                    StatBig(Modifier.weight(1f), "${r.recordedDays}", "记录天")
                    StatBig(Modifier.weight(1f), "${r.distinctDishes}", "不同菜")
                }
                StatBig(Modifier.weight(1f), oneDecimal(r.avgLevel), "膳食均衡", levelColor(r.avgLevel.roundToInt()))
            }
        }
    }
}

@Composable
private fun StatBig(modifier: Modifier, value: String, label: String, color: Color = MaterialTheme.colorScheme.onSurface) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TipsCard(tips: List<Pair<Color, String>>) {
    InsetGroup(title = "本期发现") {
        Column(Modifier.padding(14.dp)) { tips.forEach { TipRow(it.first, it.second) } }
    }
}

@Composable
private fun TipRow(dot: Color, text: String) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun CountRow(item: CountItem, unit: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
        Text(item.name, modifier = Modifier.weight(1f))
        Text("${item.count} $unit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 结构日历：每天一个色块(级别色，没记=浅灰)。[AI生成] */
// [AI修改] 用户2026-07-21:月视图~30天单行会横向溢出(30×17dp≈527dp>屏宽)→改 FlowRow 自动换行:
//   周(7块)仍单行、月(~30块)约两行·响应式不溢出。色块尺寸/间距/级别色不变(复用已确立 FlowRow 范式)。
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun StructureCalendar(levels: List<Int>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        levels.forEach { lv ->
            Box(
                Modifier.size(14.dp).clip(RoundedCornerShape(3.dp))
                    .background(if (lv < 0) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f) else levelColor(lv)),
            )
        }
    }
}

/** 三大宏量供能比分段条。[AI生成] */
@Composable
private fun MacroBar(p: Int, f: Int, c: Int) {
    // [AI修改] 宏量色收敛到单一来源 ExtendedColors(原硬编码 Material 系已漂移·与 FamilyStats/首页色系墙不一致)→逐像素对齐权威色+深色适配。
    val ext = com.sxdbsm.cookbook.android.ui.theme.LocalExtendedColors.current
    Spacer(Modifier.height(4.dp))
    Row(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(7.dp))) {
        if (p > 0) Box(Modifier.weight(p.toFloat()).fillMaxSize().background(ext.macroProtein))
        if (f > 0) Box(Modifier.weight(f.toFloat()).fillMaxSize().background(ext.macroFat))
        if (c > 0) Box(Modifier.weight(c.toFloat()).fillMaxSize().background(ext.macroCarb))
    }
    Spacer(Modifier.height(4.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        LegendDot(ext.macroProtein, "蛋白 $p%")
        LegendDot(ext.macroFat, "脂肪 $f%")
        LegendDot(ext.macroCarb, "碳水 $c%")
    }
}

@Composable
private fun LegendDot(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 膳食均级(0~4)→色：中性灰→琥珀→黄绿→浅绿→绿(**去红**·合"不制造焦虑"健康克制准则)；没记=灰(调用方 lv<0 另处理)。
 * [AI修改] D3 收敛到色系墙单一来源 [nutritionLevelColor]，与首页色系墙/餐食卡片同色板(防级别色漂移)。
 */
private fun levelColor(level: Int): Color = nutritionLevelColor(level)

private fun oneDecimal(v: Double): String {
    val r = (v * 10).roundToInt()
    return "${r / 10}.${r % 10}"
}

/** 概览一句话总结(鼓励基调，copywriter 后续可审)。[AI生成] */
private fun buildSummary(st: DietReportUiState, r: DietReport): String {
    val periodWord = if (st.period == ReportPeriod.WEEK) "这周" else "这月"
    val p = r.personal
    if (st.personal && p != null) {
        val dab = if (p.targetKcal != null) "，${p.onTargetDays} 天在目标内" else ""
        return "$periodWord 日均 ${p.avgKcal} 千卡$dab"
    }
    val struct = when {
        r.avgLevel >= 3.5 -> "结构挺均衡 👍"
        r.avgLevel >= 2.5 -> "结构还不错"
        else -> "可以更均衡些"
    }
    return "$periodWord 记了 ${r.recordedDays} 天，吃到 ${r.distinctDishes} 种菜，$struct"
}

/** 从报告派生 2~3 条本期发现(鼓励非责备)。[AI生成] */
private fun buildTips(st: DietReportUiState, r: DietReport): List<Pair<Color, String>> {
    val tips = mutableListOf<Pair<Color, String>>()
    // [AI修改] D3 收敛到色系墙同色板(nutritionLevelColor)：正向=绿(4)、待补=琥珀(1)、中性信息=中性灰(0)，去红防漂移。
    val green = nutritionLevelColor(4); val amber = nutritionLevelColor(1); val gray = nutritionLevelColor(0)
    if (r.avgLevel >= 3.5) tips += green to "膳食结构较均衡，继续保持"
    r.structureGaps.take(2).forEach { tips += amber to "${it}可以再多安排些" }
    r.topDishes.firstOrNull()?.let { if (it.count >= 2) tips += gray to "最常吃${it.name}，出现 ${it.count} 次" }
    return tips.take(4)
}
