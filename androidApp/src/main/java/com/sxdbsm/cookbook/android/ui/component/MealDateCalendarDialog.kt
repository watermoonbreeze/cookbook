package com.sxdbsm.cookbook.android.ui.component

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate

/** [AI生成] 食历与添加餐食共用的轻量月历选择器，日期标记和选择态彼此独立。 */
enum class MealDateCalendarSelectionMode { MEAL_DATES_ONLY, ANY_VISIBLE_DATE }

@Composable
fun MealDateCalendarDialog(
    mealDates: Set<LocalDate>,
    initialDate: LocalDate,
    selectedDate: LocalDate? = null,
    selectionMode: MealDateCalendarSelectionMode,
    onDismiss: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    var monthStart by remember { mutableStateOf(LocalDate(initialDate.year, initialDate.monthNumber, 1)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = { monthStart = addMonths(monthStart, -1) }) { Text("上月") }
                Text("${monthStart.year}年${monthStart.monthNumber.toString().padStart(2, '0')}月", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { monthStart = addMonths(monthStart, 1) }) { Text("下月") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                        Text(label, modifier = Modifier.weight(1f), textAlign = TextAlign.Center, style = MaterialTheme.typography.labelMedium)
                    }
                }
                monthRows(monthStart).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            CalendarDayCell(
                                date = date,
                                visibleMonth = monthStart.monthNumber,
                                hasMeal = date in mealDates,
                                selected = date == selectedDate,
                                enabled = date.monthNumber == monthStart.monthNumber &&
                                    (selectionMode == MealDateCalendarSelectionMode.ANY_VISIBLE_DATE || date in mealDates),
                                onDateClick = onDateClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    visibleMonth: Int,
    hasMeal: Boolean,
    selected: Boolean,
    enabled: Boolean,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier,
) {
    Column(
        modifier = modifier
            .height(44.dp)
            .then(if (enabled) Modifier.clickable { onDateClick(date) } else Modifier)
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                date.dayOfMonth.toString(),
                color = when {
                    date.monthNumber != visibleMonth -> MaterialTheme.colorScheme.outline
                    selected -> MaterialTheme.colorScheme.onPrimaryContainer
                    hasMeal -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.size(2.dp))
        Box(Modifier.size(5.dp).background(if (hasMeal && date.monthNumber == visibleMonth) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape))
    }
}

private fun addMonths(date: LocalDate, delta: Int): LocalDate {
    val zeroBased = date.year * 12 + date.monthNumber - 1 + delta
    return LocalDate(zeroBased.floorDiv(12), zeroBased.mod(12) + 1, 1)
}

private fun monthRows(monthStart: LocalDate): List<List<LocalDate>> {
    val gridStart = DateTime.plusDays(monthStart, -monthStart.dayOfWeek.ordinal)
    return (0 until 6).map { week -> (0 until 7).map { day -> DateTime.plusDays(gridStart, week * 7 + day) } }
}
