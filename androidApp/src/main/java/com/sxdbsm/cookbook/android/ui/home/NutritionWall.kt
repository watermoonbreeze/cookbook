package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.firstOrNull
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.component.nutritionWallColor
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate

/**
 * @File : NutritionWall
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 每天营养色系墙（GitHub 式：纵向星期、横向月份对照；每列一周；点色块进那天）
 * <p>
 * 左侧固定"周一~周日"行标，顶部按月份变化标"N月"列标；色块=当天营养级别(与餐食卡片同基色满色)，
 * 横向可滚动看全部历史、默认到今天(最右)，今天格描边，点色块跳到那天。
 * <p>
 * [AI生成] 营养色系墙。
 **/
private val CELL = 22.dp // [AI修改] 放大以容纳格内日期数字
private val GAP = 5.dp
private val HEADER = 15.dp
private val MINI = 16.dp // 折叠态三色块(昨/明)
private val MINI_TODAY = 22.dp // 折叠态今天块(居中、带日期)

/**
 * 今日营养分配卡：热量(/目标·达标) + 三大宏量占比条。[AI生成] 3c
 */
@Composable
fun NutritionTodayCard(data: TodayNutrition, modifier: Modifier = Modifier) {
    val statusColor = when (data.status) {
        com.sxdbsm.cookbook.domain.model.CalorieStatus.ON -> MaterialTheme.colorScheme.primary
        com.sxdbsm.cookbook.domain.model.CalorieStatus.ABOVE -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    androidx.compose.material3.Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("今日营养", style = MaterialTheme.typography.titleSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text(
                    buildString {
                        append("🔥 ${data.kcal} 千卡")
                        if (data.target != null && data.status != null) append(" / 目标 ${data.target} · ${data.status.label}")
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = statusColor,
                )
            }
            if (data.target != null) {
                Spacer(Modifier.height(6.dp))
                androidx.compose.material3.LinearProgressIndicator(
                    progress = (data.kcal.toFloat() / data.target).coerceIn(0f, 1f),
                    color = statusColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                )
            }
            Spacer(Modifier.height(8.dp))
            // 三大宏量供能占比条(蛋白/脂肪/碳水)。
            val p = data.proteinG * 4
            val f = data.fatG * 9
            val c = data.carbG * 4
            if (p + f + c > 0) {
                Row(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))) {
                    if (p > 0) Box(Modifier.weight(p.toFloat()).fillMaxWidth().height(8.dp).background(Color(0xFF5C9A6A)))
                    if (f > 0) Box(Modifier.weight(f.toFloat()).fillMaxWidth().height(8.dp).background(Color(0xFFE0A23C)))
                    if (c > 0) Box(Modifier.weight(c.toFloat()).fillMaxWidth().height(8.dp).background(Color(0xFF6E9BD1)))
                }
                Spacer(Modifier.height(4.dp))
            }
            // 图例：色点 + 宏量克数，与占比条同色对应。
            Row(verticalAlignment = Alignment.CenterVertically) {
                MacroLegend(Color(0xFF5C9A6A), "蛋白 ${data.proteinG}g")
                Spacer(Modifier.width(10.dp))
                MacroLegend(Color(0xFFE0A23C), "脂肪 ${data.fatG}g")
                Spacer(Modifier.width(10.dp))
                MacroLegend(Color(0xFF6E9BD1), "碳水 ${data.carbG}g")
            }
        }
    }
}

/** 宏量图例项：小色点 + 文字(与占比条同色对应)。[AI生成] 3c */
@Composable
private fun MacroLegend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(color))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * 折叠态的「昨天·今天·明天」三色块。[AI生成]
 *
 * 今天居中、块内写日期数字并描边；昨天/明天略小、不写日期。用于色系墙折叠时的标题右侧预览。
 */
@Composable
fun NutritionThreeDay(days: List<DayNutrition>, modifier: Modifier = Modifier) {
    val today = DateTime.today()
    val byDate = days.associateBy { it.date }
    val order = listOf(DateTime.plusDays(today, -1), today, DateTime.plusDays(today, 1))
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        order.forEach { d ->
            val level = byDate[d]?.level ?: 0
            val c = nutritionWallColor(level)
            val isToday = d == today
            Box(
                modifier = Modifier
                    .size(if (isToday) MINI_TODAY else MINI)
                    .clip(RoundedCornerShape(4.dp))
                    .background(c)
                    .then(if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp)) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (isToday) {
                    val tc = if (c.luminance() > 0.5f) Color(0xFF3A352E) else Color.White
                    Text(d.dayOfMonth.toString(), style = MaterialTheme.typography.labelSmall, color = tc)
                }
            }
        }
    }
}

@Composable
fun NutritionWall(
    days: List<DayNutrition>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
    yearAverages: List<YearNutrition> = emptyList(), // [AI生成] 往年平均色(块内年份后两位)，空则整行不显示
) {
    if (days.isEmpty()) return
    val today = DateTime.today()
    val weeks = days.chunked(7)
    val listState = rememberLazyListState()
    // [AI修改] 固定本公历年，定位到今天所在周并**居中**(不再靠左/滚到年末)。
    LaunchedEffect(weeks.size) {
        if (weeks.isEmpty()) return@LaunchedEffect
        val idx = weeks.indexOfFirst { wk -> wk.any { it.date == today } }
            .let { if (it >= 0) it else weeks.indexOfFirst { wk -> wk.any { d -> d.date >= today } }.coerceAtLeast(0) }
        listState.scrollToItem(idx)
        // 居中：scrollToItem 同帧 layoutInfo 还没重排，等目标列真正被布局出来再按视口中央 scrollBy(内容不足自动 clamp)。
        val info = snapshotFlow { listState.layoutInfo }
            .firstOrNull { it.visibleItemsInfo.any { item -> item.index == idx } } ?: return@LaunchedEffect
        val item = info.visibleItemsInfo.first { it.index == idx }
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        val itemCenter = item.offset + item.size / 2f
        listState.scrollBy(itemCenter - viewportCenter)
    }
    Column(modifier = modifier) {
        // [AI生成] 往年平均色系(标题下方)：仅有记录的往年才显示；无则整行不显示。块内为年份后两位。
        if (yearAverages.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("年份", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(end = 6.dp))
                yearAverages.forEach { yr ->
                    val yc = nutritionWallColor(yr.level)
                    val ytc = if (yc.luminance() > 0.5f) Color(0xFF3A352E) else Color.White
                    Box(
                        modifier = Modifier.size(CELL).clip(RoundedCornerShape(4.dp)).background(yc),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(yr.yy, style = MaterialTheme.typography.labelSmall, color = ytc)
                    }
                    Spacer(Modifier.width(GAP))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
        Row {
            // 左侧固定：星期行标(与格子行对齐)。
            Column(verticalArrangement = Arrangement.spacedBy(GAP), modifier = Modifier.padding(end = 6.dp)) {
                Box(Modifier.height(HEADER)) // 对齐顶部月份标
                listOf("一", "二", "三", "四", "五", "六", "日").forEach { wd ->
                    Box(Modifier.height(CELL), contentAlignment = Alignment.CenterStart) {
                        Text(wd, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            // 右侧可滚动：按周成列，顶部月份变化时标"N月"。
            LazyRow(
                state = listState,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(GAP),
            ) {
                itemsIndexed(weeks) { index, week ->
                    val first = week.first().date
                    val prevMonth = if (index == 0) -1 else weeks[index - 1].first().date.monthNumber
                    val monthLabel = if (first.monthNumber != prevMonth) "${first.monthNumber}月" else ""
                    Column(verticalArrangement = Arrangement.spacedBy(GAP)) {
                        Box(Modifier.height(HEADER)) {
                            if (monthLabel.isNotEmpty()) {
                                Text(monthLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        week.forEach { d ->
                            val isToday = d.date == today
                            val cellColor = nutritionWallColor(d.level)
                            val darkBg = cellColor.luminance() > 0.5f
                            // 格内日期数字：按底色明暗选深/浅文字；普通日调淡避免密集刺眼，今天用足强度突出。
                            val dayTextColor = when {
                                isToday && darkBg -> Color(0xFF3A352E)
                                isToday -> Color.White
                                darkBg -> Color(0xFF3A352E).copy(alpha = 0.5f)
                                else -> Color.White.copy(alpha = 0.7f)
                            }
                            Box(
                                modifier = Modifier
                                    .size(CELL)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(cellColor)
                                    .then(
                                        if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                        else Modifier,
                                    )
                                    // [AI修改] 只有有记录(有色块)的日子才可点跳转食历，空日不可点。
                                    .then(if (d.level > 0) Modifier.clickable { onDayClick(d.date) } else Modifier),
                                contentAlignment = Alignment.Center,
                            ) {
                                // [AI修改] 有记录的日子填日期数字；空日留白减少密集感——但今天无论有无餐食都显示日期。
                                if (d.level > 0 || isToday) {
                                    Text(
                                        d.date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = dayTextColor,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        // 图例：较单一 → 均衡。
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("较单一", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(6.dp))
            (1..4).forEach { lv ->
                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(3.dp)).background(nutritionWallColor(lv)))
                Spacer(Modifier.width(3.dp))
            }
            Spacer(Modifier.width(3.dp))
            Text("均衡 · 点色块看当天食历 · 左右滑看全年", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
