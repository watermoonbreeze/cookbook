package com.sxdbsm.cookbook.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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

@Composable
fun NutritionWall(
    days: List<DayNutrition>,
    modifier: Modifier = Modifier,
    onDayClick: (LocalDate) -> Unit = {},
) {
    if (days.isEmpty()) return
    val today = DateTime.today()
    val weeks = days.chunked(7)
    val listState = rememberLazyListState()
    LaunchedEffect(weeks.size) {
        if (weeks.isNotEmpty()) listState.scrollToItem(weeks.lastIndex)
    }
    Column(modifier = modifier) {
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
                            // 格内日期数字：按底色明暗选深/浅文字并调淡，避免整墙数字密集刺眼。
                            val dayTextColor = if (cellColor.luminance() > 0.5f) Color(0xFF3A352E).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f)
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
                                // [AI修改] 只有有记录(有色块)的日子才填日期数字，空日留白，减少整墙密集感。
                                if (d.level > 0) {
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
            Text("均衡 · 点色块看当天食历 · 可左滑看更早", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
