package com.sxdbsm.cookbook.android.ui.ai

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate

/**
 * @File : WeekStrip
 * @Time : 2026/08/06
 * @Author : SXD-AI
 * @Desc : B4 周期记 7 天日期段选择器（签名元素）。
 * <p>
 * 7 格均分，连续范围选取（端点点击扩缩），默认全选当前周。
 * primaryContainer 实底 + onSurface 文字 = 选中；surfaceVariant 底 + onSurfaceVariant 文字 = 未选。
 * 连续选中格无缝连成色条（头尾圆角 10dp，中间圆角 0）。
 * <p>
 * [AI生成] B4 输入 UI 改造：Apple UX 设计师交互方案。
 */
@Composable
fun WeekStrip(
    weekMonday: LocalDate,
    selectedRange: IntRange,
    onRangeChange: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayLabels = listOf("一", "二", "三", "四", "五", "六", "日")
    val today = DateTime.today()

    Column(modifier = modifier.fillMaxWidth()) {
        // 7 天横条
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            dayLabels.forEachIndexed { index, label ->
                val date = DateTime.plusDays(weekMonday, index)
                val inRange = index in selectedRange
                val isEdgeLeft = index == selectedRange.first
                val isEdgeRight = index == selectedRange.last
                val onlyOne = selectedRange.first == selectedRange.last && inRange
                val isToday = date == today

                val bgColor by animateColorAsState(
                    targetValue = if (inRange) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant,
                    animationSpec = tween(300),
                    label = "weekBg",
                )
                val textColor by animateColorAsState(
                    targetValue = when {
                        isToday -> MaterialTheme.colorScheme.primary
                        inRange -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = tween(300),
                    label = "weekText",
                )

                val shape = when {
                    onlyOne -> RoundedCornerShape(10.dp)
                    isEdgeLeft -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                    isEdgeRight -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                    else -> RoundedCornerShape(0.dp)
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                        .padding(horizontal = 1.5.dp)
                        .clip(shape)
                        .background(bgColor)
                        .clickable {
                            val newRange = when {
                                // 点击选中段左端 → 缩 1 天
                                index == selectedRange.first && selectedRange.first != selectedRange.last ->
                                    (selectedRange.first + 1)..selectedRange.last
                                // 点击选中段右端 → 缩 1 天
                                index == selectedRange.last && selectedRange.first != selectedRange.last ->
                                    selectedRange.first..(selectedRange.last - 1)
                                // 点击仅剩的 1 天 → 无操作
                                selectedRange.first == selectedRange.last && inRange ->
                                    selectedRange
                                // 点击未选中天（相邻于当前范围）→ 扩展
                                !inRange && (index == selectedRange.first - 1 || index == selectedRange.last + 1) -> {
                                    val newFirst = minOf(index, selectedRange.first)
                                    val newLast = maxOf(index, selectedRange.last)
                                    newFirst..newLast
                                }
                                // 点击未选中天（不连续）→ 重置为仅该天
                                !inRange -> index..index
                                // 点击选中段中间 → 无操作
                                else -> selectedRange
                            }
                            onRangeChange(newRange)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            color = textColor,
                            fontWeight = if (inRange || isToday) FontWeight.SemiBold else FontWeight.Normal,
                        )
                        Text(
                            text = "${date.dayOfMonth}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            fontWeight = if (inRange || isToday) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // 范围描述行
        val dayCount = selectedRange.last - selectedRange.first + 1
        Text(
            text = "已选 $dayCount 天",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}
