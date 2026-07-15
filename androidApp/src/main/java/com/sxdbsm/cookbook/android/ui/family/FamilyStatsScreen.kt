package com.sxdbsm.cookbook.android.ui.family

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.sxdbsm.cookbook.android.ui.component.SegmentedControl
import com.sxdbsm.cookbook.domain.model.CalorieStatus
import org.koin.androidx.compose.koinViewModel

/**
 * @File : FamilyStatsScreen
 * @Time : 2026/07/15
 * @Author : SXD-AI
 * @Desc : 膳食统计页（家庭/成员切换：今日 + 近7天）
 * <p>
 * [AI生成] 多人家庭档案 P2。个人视图为"全家餐×饭量系数份额"的摄入估算，非精确。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyStatsScreen(
    onBack: () -> Unit,
    vm: FamilyStatsViewModel = koinViewModel(),
) {
    val members by vm.members.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()

    // 切换选项：家庭 + 各成员。
    val options = listOf<Pair<Long?, String>>(null to "全家") + members.map { it.id to it.name }
    val selectedIndex = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = { AppTopBar(title = "膳食统计", onBack = onBack) },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // [AI修改] 成员≤4用分段控件；更多用可横滚 chip 行(SegmentedControl 项多会挤压/截断)。
            if (options.size in 2..4) {
                SegmentedControl(
                    options = options.map { it.second },
                    selectedIndex = selectedIndex,
                    onSelect = { vm.select(options[it].first) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else if (options.size > 4) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    options.forEachIndexed { i, opt ->
                        androidx.compose.material3.FilterChip(
                            selected = i == selectedIndex,
                            onClick = { vm.select(opt.first) },
                            label = { Text(opt.second) },
                        )
                    }
                }
            }

            // 今日卡
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    val statusColor = when (stats.status) {
                        CalorieStatus.ON -> MaterialTheme.colorScheme.primary
                        CalorieStatus.ABOVE -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("今日 · ${stats.memberName}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text(
                            buildString {
                                append("🔥 ${stats.todayKcal} 千卡")
                                if (stats.target != null && stats.status != null) append(" / 目标 ${stats.target} · ${stats.status!!.label}")
                            },
                            style = MaterialTheme.typography.labelMedium, color = statusColor,
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Legend(Color(0xFF5C9A6A), "蛋白 ${stats.proteinG}g")
                        Spacer(Modifier.width(10.dp))
                        Legend(Color(0xFFE0A23C), "脂肪 ${stats.fatG}g")
                        Spacer(Modifier.width(10.dp))
                        Legend(Color(0xFF6E9BD1), "碳水 ${stats.carbG}g")
                    }
                    if (!stats.isFamily) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "个人摄入按饭量系数从全家餐估算，非精确、非医嘱，仅供参考。",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            // 家庭视图：各成员今日摄入拆分(按饭量系数)
            if (stats.breakdown.isNotEmpty()) {
                Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("今日各成员摄入(按饭量系数估算)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text("点成员名可标「没吃」，其份额分给其余在场成员(仅本次查看，不保存)。", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Spacer(Modifier.height(8.dp))
                        stats.breakdown.forEach { mi ->
                            Row(
                                Modifier.fillMaxWidth()
                                    .clickable { vm.togglePresent(mi.id) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    mi.name + if (!mi.present) " · 没吃" else "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (mi.present) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                                )
                                Spacer(Modifier.weight(1f))
                                Text(
                                    if (mi.present) "${mi.kcal} 千卡" else "—",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(Modifier.width(8.dp))
                                // 明确可点的在场/没吃胶囊
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (mi.present) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                ) {
                                    Text(
                                        if (mi.present) "在场" else "没吃",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (mi.present) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 近7天
            Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("近 7 天", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Text("日均 ${stats.weekAvgKcal} 千卡", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(Modifier.height(10.dp))
                    if (stats.dailyKcal.all { it == 0 }) {
                        Text("近 7 天暂无餐食记录", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                    // 简易柱状：每日热量相对最大值。
                    val maxKcal = (stats.dailyKcal.maxOrNull() ?: 0).coerceAtLeast(1)
                    Row(
                        modifier = Modifier.fillMaxWidth().height(80.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        stats.dailyKcal.forEach { k ->
                            val frac = (k.toFloat() / maxKcal).coerceIn(0.04f, 1f)
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth().height((80 * frac).dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (k > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.7f) else MaterialTheme.colorScheme.surfaceVariant),
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(7.dp).height(7.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
