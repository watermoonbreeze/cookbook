package com.sxdbsm.cookbook.android.ui.timeline

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sxdbsm.cookbook.android.ui.component.DayMealCardView
import com.sxdbsm.cookbook.android.ui.component.EmptyState
import com.sxdbsm.cookbook.util.DateTime
import kotlinx.datetime.LocalDate
import org.koin.androidx.compose.koinViewModel

/**
 * 食历页面。[AI修改]
 *
 * 用时间线方式展示历史餐食和未来计划。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodTimelineScreen(
    onEditMealDate: (LocalDate) -> Unit,
    onOpenDish: (Long) -> Unit,
    onCopyMeal: (LocalDate) -> Unit = {}, // [AI生成] F8：复制→按来源日期预填成新建草稿(日期可改)
    onBack: (() -> Unit)? = null,
    initialJumpDate: LocalDate? = null, // [AI生成] 营养色系墙点色块进入时的目标日期(空=默认今天)
    vm: TimelineViewModel = koinViewModel(),
) {
    // [AI修改] 页面只订阅 TimelineUiState，不直接访问 Repository。
    val state by vm.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var initialScrollDone by remember { mutableStateOf(false) }
    var jumpConsumed by remember { mutableStateOf(false) } // [AI生成] 保证外部日期只定位一次

    // [AI生成] 从营养色系墙进入时，待食历日期列表就绪后定位到目标日(该日有餐才有卡片)。
    LaunchedEffect(initialJumpDate, state.mealDates) {
        if (initialJumpDate != null && !jumpConsumed && initialJumpDate in state.mealDates) {
            vm.jumpToDate(initialJumpDate)
            jumpConsumed = true
        }
    }
    var calendarOpen by remember { mutableStateOf(false) }
    val appSnackbar = com.sxdbsm.cookbook.android.ui.component.LocalAppSnackbar.current // [AI生成] B-5：删整天撤销 Snackbar

    LaunchedEffect(state.scrollRequestVersion, state.scrollTargetIndex, state.pages.size) {
        if (state.scrollTargetIndex >= 0 && state.pages.isNotEmpty()) {
            // [AI修改] VM 根据“今天/未来最近/最大日期”计算目标，UI 只负责滚动到对应 item。
            listState.scrollToItem(state.scrollTargetIndex.coerceIn(0, state.pages.lastIndex))
            initialScrollDone = true
            vm.consumeScrollRequest()
        }
    }

    LaunchedEffect(state.prependCount, state.pages.size) {
        if (state.prependCount > 0 && state.pages.isNotEmpty()) {
            // [AI修改] 顶部前插历史日期后，把可见锚点顺延 prependCount，避免列表内容突然下跳。
            val index = (listState.firstVisibleItemIndex + state.prependCount).coerceAtMost(state.pages.lastIndex)
            val offset = listState.firstVisibleItemScrollOffset
            listState.scrollToItem(index, offset)
            vm.consumePrependCount()
        }
    }

    LaunchedEffect(
        listState,
        state.pages.size,
        state.loadingPrevious,
        state.loadingNext,
        initialScrollDone,
    ) {
        snapshotFlow {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            Triple(listState.firstVisibleItemIndex, lastVisible, listState.isScrollInProgress)
        }.collect { (firstVisible, lastVisible, isScrolling) ->
            if (!initialScrollDone || state.pages.isEmpty() || !isScrolling) return@collect
            // [AI修改] 顶部加载更早历史，底部加载未来计划；VM 内部有 loading 门禁防止重复触发。
            if (firstVisible <= 1 && !state.loadingPrevious) {
                vm.loadPrevious()
            }
            if (lastVisible >= state.pages.lastIndex - 1 && !state.loadingNext) {
                vm.loadNext()
            }
        }
    }

    LaunchedEffect(state.copyMessage, state.copyError) {
        val message = state.copyMessage ?: state.copyError ?: return@LaunchedEffect
        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
        vm.consumeCopyMessage()
    }

    Box(Modifier.fillMaxSize()) {
      Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("食历", fontWeight = FontWeight.SemiBold) },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background,
                titleContentColor = MaterialTheme.colorScheme.onBackground,
                navigationIconContentColor = MaterialTheme.colorScheme.primary,
                actionIconContentColor = MaterialTheme.colorScheme.primary,
            ),
            navigationIcon = {
                onBack?.let { back ->
                    IconButton(onClick = back) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                }
            },
            actions = {
                // [AI修改] "今天"移到右下角悬浮"今"按钮(见下方 Box)，不再占标题行——原来挤得"食历"标题换行。
                TextButton(onClick = { calendarOpen = true }) {
                    Icon(Icons.Outlined.CalendarMonth, contentDescription = "选择日期")
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = formatRange(state.rangeMin, state.rangeMax),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            },
        )

        if (state.pages.isEmpty() && !state.loading) {
            EmptyState(text = "还没有任何餐食记录\n中间 + 号开始记录", icon = "📅")
        } else {
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                items(state.pages, key = { it.date.toString() }) { card ->
                    DayMealCardView(
                        data = card,
                        onEditClick = { onEditMealDate(card.date) },
                        onCopyClick = { onCopyMeal(card.date) }, // [AI修改] F8：复制改为进入新建草稿(预填该日餐次+日期源+1可改)，不再直接"复用到今天/明天"
                        onDeleteClick = { // [AI修改] B-5/§9.12：可逆删除改软删+撤销 Snackbar。
                            val d = card.date
                            vm.deleteDayUndoable(d) { onUndo -> appSnackbar?.showUndo("已删除 ${DateTime.formatDate(d)} 的餐食", onUndo = onUndo) }
                        },
                        onDishClick = { dish -> onOpenDish(dish.id) }, // [AI修改] 食历餐食卡片内的菜品 block 点击进入菜品详情。
                    )
                }
                item {
                    // [AI修改] 移除旧“加载更多”按钮，底部只保留导航栏避让空间。
                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.height(80.dp))
                }
            }
        }
      }
      // [AI生成] "今"悬浮按钮：右下角，点击快速滚到今天(操作同原"今天"按钮)，不再占标题行挤"食历"。
      if (state.pages.isNotEmpty()) {
          SmallFloatingActionButton(
              onClick = { vm.jumpToDate(DateTime.today()) },
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .navigationBarsPadding()
                  .padding(end = 16.dp, bottom = 16.dp),
          ) {
              Text("今", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
          }
      }
    }

    if (calendarOpen) {
        TimelineCalendarDialog(
            mealDates = state.mealDates,
            onDismiss = { calendarOpen = false },
            onDateClick = { date ->
                vm.jumpToDate(date)
                calendarOpen = false
            },
        )
    }

}

/**
 * 食历顶部日期范围文案。[AI生成]
 */
private fun formatRange(start: LocalDate?, end: LocalDate?): String =
    if (start == null || end == null) "暂无记录" else "$start - $end"

/**
 * 食历日期选择月历。[AI生成]
 *
 * Material3 当前使用版本不支持给 DatePicker 日期格子添加小圆点，因此这里实现轻量月历。
 */
@Composable
private fun TimelineCalendarDialog(
    mealDates: Set<LocalDate>,
    onDismiss: () -> Unit,
    onDateClick: (LocalDate) -> Unit,
) {
    var monthStart by remember { mutableStateOf(firstDayOfMonth(DateTime.today())) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
            ) {
                TextButton(onClick = { monthStart = addMonths(monthStart, -1) }) { Text("上月") }
                Text(
                    "${monthStart.year}年${monthStart.monthNumber.toString().padStart(2, '0')}月",
                    fontWeight = FontWeight.SemiBold,
                )
                TextButton(onClick = { monthStart = addMonths(monthStart, 1) }) { Text("下月") }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    listOf("一", "二", "三", "四", "五", "六", "日").forEach { label ->
                        Text(
                            label,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                monthRows(monthStart).forEach { week ->
                    Row(Modifier.fillMaxWidth()) {
                        week.forEach { date ->
                            CalendarDayCell(
                                date = date,
                                visibleMonth = monthStart.monthNumber,
                                hasMeal = date in mealDates,
                                onDateClick = onDateClick,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("关闭") }
        },
    )
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    visibleMonth: Int,
    hasMeal: Boolean,
    onDateClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabled = hasMeal && date.monthNumber == visibleMonth
    Column(
        modifier = modifier
            .height(44.dp)
            .then(if (enabled) Modifier.clickable { onDateClick(date) } else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            date.dayOfMonth.toString(),
            color = when {
                date.monthNumber != visibleMonth -> MaterialTheme.colorScheme.outline
                enabled -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurface
            },
            style = MaterialTheme.typography.bodyMedium,
        )
        Box(
            modifier = Modifier
                .padding(top = 2.dp)
                .size(5.dp)
                .background(
                    color = if (hasMeal && date.monthNumber == visibleMonth) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    shape = CircleShape,
                ),
        )
    }
}

private fun firstDayOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.monthNumber, 1)

private fun addMonths(date: LocalDate, delta: Int): LocalDate {
    val zeroBased = date.year * 12 + (date.monthNumber - 1) + delta
    val year = zeroBased.floorDiv(12)
    val month = zeroBased.mod(12) + 1
    return LocalDate(year, month, 1)
}

private fun monthRows(monthStart: LocalDate): List<List<LocalDate>> {
    val firstOffset = monthStart.dayOfWeek.ordinal // [AI修改] kotlinx-datetime 当前版本无 isoDayNumber；枚举从周一开始。
    val gridStart = DateTime.plusDays(monthStart, -firstOffset)
    return (0 until 6).map { week ->
        (0 until 7).map { day -> DateTime.plusDays(gridStart, week * 7 + day) }
    }
}
