package com.sxdbsm.cookbook.android.ui.weekplan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * @File : WeekPlanScreen
 * @Time : 2026/07/14
 * @Author : SXD-AI
 * @Desc : 一周计划视图（B3）
 * <p>
 * 一周(周一~周日)整周概览：周导航 + 7 天卡片(含空日)，每天可编辑/复制/安排，today 高亮。
 * 面向"周末排下周饭"：切到下一周→逐天安排或从某天复制。
 * <p>
 * [AI生成] B3
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeekPlanScreen(
    onBack: () -> Unit,
    onEditMealDate: (LocalDate) -> Unit,
    onCopyMeal: (LocalDate) -> Unit,
    onOpenDish: (Long) -> Unit = {},
    vm: WeekPlanViewModel = koinViewModel(),
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()
    var deleteDate by remember { mutableStateOf<LocalDate?>(null) } // [AI生成] 待删除日期(确认)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("一周计划", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, contentDescription = "返回") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            // [AI生成] 周导航：上一周 / 周范围 + 本周 / 下一周。
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = vm::prevWeek) { Icon(Icons.Outlined.KeyboardArrowLeft, contentDescription = "上一周") }
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(weekRangeLabel(ui.weekStart, ui.weekEnd), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = vm::thisWeek, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("回本周") }
                }
                IconButton(onClick = vm::nextWeek) { Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "下一周") }
            }
            Divider()
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.days, key = { it.date.toString() }) { day ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // 星期几 · 今天 标注
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                weekdayLabel(day.date),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            if (day.isToday) {
                                Spacer(Modifier.width(8.dp))
                                // [AI修改] 苹果风格：今天用 accent 浅底小胶囊标签，去 Material 描边 chip。
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = androidx.compose.foundation.shape.RoundedCornerShape(50)) {
                                    Text(
                                        "今天",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    )
                                }
                            }
                        }
                        if (day.meals.isEmpty()) {
                            // 空日：一键安排
                            OutlinedButton(onClick = { onEditMealDate(day.date) }, modifier = Modifier.fillMaxWidth()) {
                                Text("＋ 安排这天")
                            }
                        } else {
                            DayMealCardView(
                                data = day,
                                onDishClick = { dish -> onOpenDish(dish.id) },
                                onEditClick = { onEditMealDate(day.date) },
                                onCopyClick = { onCopyMeal(day.date) },
                                onDeleteClick = { deleteDate = day.date }, // [AI修改] 补删除入口
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(60.dp)) }
            }
        }
    }

    deleteDate?.let { date ->
        AlertDialog(
            onDismissRequest = { deleteDate = null },
            title = { Text("删除餐食") },
            text = { Text("确认删除 ${weekdayLabel(date)} 的全部餐食？") },
            confirmButton = {
                TextButton(onClick = { vm.deleteDay(date); deleteDate = null }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { deleteDate = null }) { Text("取消") } },
        )
    }
}

/** 周范围文案，如 "7/14 - 7/20"。[AI生成] */
private fun weekRangeLabel(start: LocalDate, end: LocalDate): String =
    "${start.monthNumber}/${start.dayOfMonth} - ${end.monthNumber}/${end.dayOfMonth}"

/** 星期几文案，如 "周一 7/14"。[AI生成] */
private fun weekdayLabel(date: LocalDate): String {
    val wd = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> "周一"
        DayOfWeek.TUESDAY -> "周二"
        DayOfWeek.WEDNESDAY -> "周三"
        DayOfWeek.THURSDAY -> "周四"
        DayOfWeek.FRIDAY -> "周五"
        DayOfWeek.SATURDAY -> "周六"
        else -> "周日"
    }
    return "$wd ${date.monthNumber}/${date.dayOfMonth}"
}
