package com.sxdbsm.cookbook.android.ui.kitchen

import android.Manifest
import android.app.Activity
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sxdbsm.cookbook.android.kitchen.CookingTimerService
import com.sxdbsm.cookbook.android.kitchen.TimerAlarm
import com.sxdbsm.cookbook.android.kitchen.TimerAlarmReceiver
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.data.repository.CookingTimerRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.CookingTimerTemplate
import com.sxdbsm.cookbook.domain.model.TimerSegment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * @File : CookingTimerScreen
 * @Time : 2026/06/12
 * @Author : SXD-AI
 * @Desc : 烹饪计时页面
 * <p>
 * 支持添加多个烹饪倒计时，按创建顺序稳定展示，可暂停或停止；运行期间禁止编辑。
 * <p>
 * [AI生成] 用户任务7要求在厨房小助手中新增烹饪计时独立页面。
 **/
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CookingTimerScreen(
    onBack: () -> Unit,
    repo: CookingTimerRepository = koinInject(),
    prefs: PreferenceRepository = koinInject(), // [AI生成] 持久化运行中计时器，App 被杀重开可恢复倒计时显示。
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var nextLocalId by remember { mutableLongStateOf(-1L) }
    var timers by remember { mutableStateOf<List<CookingTimerItem>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var pickingRingtoneTimerId by remember { mutableStateOf<Long?>(null) }
    var fullScreenGuideDismissed by remember { mutableStateOf(false) } // [AI生成] A14 全屏提醒引导本次会话内"知道了"后不再显示。
    // [AI修改] 响铃/停铃统一由 TimerAlarmReceiver 单一管理，页面不再持有本地 Ringtone。
    val ringtonePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val timerId = pickingRingtoneTimerId
        pickingRingtoneTimerId = null
        if (result.resultCode != Activity.RESULT_OK || timerId == null) return@rememberLauncherForActivityResult
        val pickedUri = result.data?.pickedRingtoneUri()
        timers = timers.map { timer ->
            if (timer.id == timerId) {
                timer.copy(
                    ringtoneUri = pickedUri?.toString().orEmpty(),
                    ringtoneTitle = getRingtoneTitle(context, pickedUri),
                ) // [AI生成] 系统铃声选择器返回 URI 后，只更新当前编辑中的计时器配置。
            } else {
                timer
            }
        }
    }
    val sortedTimers = remember(timers) { timers.sortedBy { it.sortOrder } }

    // [AI修改] 停铃统一走 TimerAlarmReceiver（单一真相源）：撤系统闹钟调度 + 停铃 + 消通知，并把该计时器复位显示态。
    fun stopAlarm(timerId: Long) {
        TimerAlarm.cancel(context, timerId) // cancel = 撤调度 + TimerAlarmReceiver.stop(停铃+消通知)
        timers = timers.map {
            if (it.id == timerId && it.status == TimerStatus.FINISHED) {
                it.copy(status = TimerStatus.IDLE, remainingSeconds = it.durationSeconds, endAtElapsed = null)
            } else {
                it
            }
        }
    }

    // [AI生成] 全屏/通知处停铃后，把已不再响铃的"完成态"计时器复位，保持页面与告警状态一致。
    val ringingAlerts by TimerAlarmReceiver.activeAlerts.collectAsState()
    LaunchedEffect(ringingAlerts, loaded) {
        if (!loaded) return@LaunchedEffect
        val ringingIds = ringingAlerts.map { it.first }.toSet()
        timers = timers.map {
            // [AI修改] 仅复位"单段/末段"的完成态(外部停铃后回 IDLE)；多段非末段完成态保留(等用户在应用内点 开始下一段/停止全部)。
            val atLastSegment = it.currentSegmentIndex >= it.runSegments.lastIndex
            if (it.status == TimerStatus.FINISHED && it.id.toInt() !in ringingIds && atLastSegment) {
                it.copy(status = TimerStatus.IDLE, currentSegmentIndex = 0, remainingSeconds = it.runSegments.first().seconds, endAtElapsed = null)
            } else {
                it
            }
        }
    }

    LaunchedEffect(Unit) {
        val loadedTimers = repo.listTemplates().map { it.toTimerItem() }
        // [AI生成] 恢复运行态：持久化存的是墙钟结束时刻，重开按它算剩余并重锚 elapsedRealtime；
        // 已过期的显示为完成（响铃由之前注册的系统闹钟处理，不重复响）。
        val running = parseRunningTimers(prefs.get(KEY_RUNNING_TIMERS))
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        timers = loadedTimers.map { t ->
            val persisted = running[t.id] ?: return@map t
            val endWall = persisted.first
            // [AI修改] 多段：恢复当前段下标(限在有效范围)，否则被杀重开会退到段0致接力/进度点错乱。
            val segIndex = persisted.second.coerceIn(0, t.runSegments.lastIndex)
            val remaining = (((endWall - nowWall) + 999) / 1000).toInt()
            if (remaining > 0) {
                t.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining, endAtElapsed = nowElapsed + remaining * 1000L, currentSegmentIndex = segIndex)
            } else {
                t.copy(status = TimerStatus.FINISHED, remainingSeconds = 0, endAtElapsed = null, currentSegmentIndex = segIndex)
            }
        }
        loaded = true
    }

    // [AI生成] 运行中计时器集合变化时（开始/暂停/停止/完成）持久化墙钟结束时刻；tick 每秒只改 remaining、不触发此保存。
    val runningSnapshot = timers.filter { it.status == TimerStatus.RUNNING && it.endAtElapsed != null }
    LaunchedEffect(loaded, runningSnapshot.map { Triple(it.id, it.endAtElapsed, it.currentSegmentIndex) }) {
        if (!loaded) return@LaunchedEffect
        val now = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        // [AI修改] 持久化格式 id:endWall:segIndex(段号)，多段被杀重开能恢复到正确段。
        val running = runningSnapshot.map { Triple(it.id, nowWall + (it.endAtElapsed!! - now), it.currentSegmentIndex) }
        prefs.set(KEY_RUNNING_TIMERS, running.joinToString(",") { "${it.first}:${it.second}:${it.third}" })
        // [AI生成] 同步前台服务：有运行中计时器则常驻通知+保活，全部结束/暂停则停止服务。
        CookingTimerService.sync(context, running.map { it.second })
    }

    // [AI生成] A13+ 请求通知权限（未授时铃声仍会响，只是不显示到点通知）。
    val notifPermLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notifPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(timers.any { it.status == TimerStatus.RUNNING }) {
        while (timers.any { it.status == TimerStatus.RUNNING }) {
            delay(1000)
            // [AI修改] 剩余时间按墙钟(endAtElapsed - now)计算，而非每秒 -1；
            // 息屏/后台时本循环虽被系统挂起，恢复后按真实流逝时间刷新，倒计时不再“停走”。
            val now = SystemClock.elapsedRealtime()
            // [AI修改] 收集本 tick 内所有归零的计时器(可能多个同秒到点)，逐个响铃，别只处理最后一个。
            val finishedTimers = mutableListOf<CookingTimerItem>()
            timers = timers.map { timer ->
                if (timer.status != TimerStatus.RUNNING) {
                    timer
                } else {
                    val end = timer.endAtElapsed
                    val nextRemaining = if (end != null) remainingFrom(end, now) else (timer.remainingSeconds - 1).coerceAtLeast(0)
                    if (timer.remainingSeconds > 0 && nextRemaining == 0) {
                        finishedTimers += timer.copy(remainingSeconds = 0, status = TimerStatus.FINISHED, endAtElapsed = null)
                    }
                    timer.copy(
                        remainingSeconds = nextRemaining,
                        status = if (nextRemaining == 0) TimerStatus.FINISHED else TimerStatus.RUNNING,
                        endAtElapsed = if (nextRemaining == 0) null else timer.endAtElapsed,
                    ) // [AI生成] 倒计时归零后进入完成告警态，等待用户点击记录或停止按钮确认。
                }
            }
            finishedTimers.forEach { timer ->
                // [AI修改] 前台到点：统一走 TimerAlarmReceiver.ring(响铃+通知+全屏意图+入 activeAlerts)，
                //   并撤掉重复的系统闹钟调度，避免二次触发。若系统闹钟已先响(该 id 已在 activeAlerts)则不重复 ring，
                //   避免打断正在响的铃。多计时器各自入 activeAlerts，全屏逐个显示、各自停止(修双响铃机制打架)。
                val idInt = timer.id.toInt()
                if (TimerAlarmReceiver.activeAlerts.value.none { it.first == idInt }) {
                    TimerAlarm.cancelSchedule(context, timer.id)
                    TimerAlarmReceiver.ring(context, idInt, timer.name, timer.ringtoneUri)
                }
            }
        }
    }

    // [AI生成] 用户提:计时中屏幕常亮不息屏，直到所有倒计时结束再放开(离开页面也放开)。
    val screenView = androidx.compose.ui.platform.LocalView.current
    val anyRunning = timers.any { it.status == TimerStatus.RUNNING }
    DisposableEffect(anyRunning) {
        screenView.keepScreenOn = anyRunning
        onDispose { screenView.keepScreenOn = false }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            // [AI修改] B-8(§9.15)：带返回二级页统一 AppTopBar 收敛。
            com.sxdbsm.cookbook.android.ui.component.AppTopBar(
                title = "烹饪计时",
                onBack = onBack,
                actions = {
                    IconButton(
                        onClick = {
                            timers = timers + CookingTimerItem(
                                id = nextLocalId--,
                                sortOrder = (timers.maxOfOrNull { it.sortOrder } ?: -1) + 1,
                                editing = true,
                            )
                        },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "添加计时")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item { Spacer(Modifier.height(6.dp)) }
            // [AI生成] A14+ 全屏提醒需授权：未授时引导用户去系统设置开启"全屏通知"，否则息屏到点可能只响铃不弹全屏。
            //   A13 及以下默认允许、不显示此引导；铃声+通知本就是兜底(不开也能响)。华为/鸿蒙还需另在系统"后台弹出界面"放行。
            val needsFullScreenGuide = !fullScreenGuideDismissed &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                context.getSystemService(NotificationManager::class.java)?.canUseFullScreenIntent() == false
            if (loaded && needsFullScreenGuide) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    ) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("到点全屏提醒未开启", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Android 14 起需手动允许「全屏通知」，否则息屏或锁屏时计时到点可能只响铃、不弹全屏。铃声和通知不受影响。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = { fullScreenGuideDismissed = true }) { Text("知道了") }
                                Spacer(Modifier.weight(1f))
                                Button(onClick = { openFullScreenIntentSettings(context) }) { Text("去开启") }
                            }
                        }
                    }
                }
            }
            if (!loaded) {
                item {
                    Text(
                        text = "加载计时模板中…",
                        modifier = Modifier.padding(18.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else if (sortedTimers.isEmpty()) {
                item {
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                    ) {
                        Text(
                            text = "点击右上角 + 添加烹饪计时",
                            modifier = Modifier.padding(18.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            items(sortedTimers, key = { it.id }) { timer ->
                if (timer.editing && timer.status == TimerStatus.IDLE) {
                    CookingTimerEditRow(
                        timer = timer,
                        onSave = { name, note, segs ->
                            // [AI修改] 多段：segs≥1 且每段>0；单段(size==1)存 segments=空(向后兼容)、durationSeconds=该段；多段存 segments。
                            if (segs.isNotEmpty() && segs.all { it.seconds > 0 }) {
                                val firstSecs = segs.first().seconds
                                val nextTimer = timer.copy(
                                    name = name.ifBlank { "未命名计时" },
                                    durationSeconds = firstSecs,
                                    remainingSeconds = firstSecs,
                                    note = note,
                                    segments = if (segs.size >= 2) segs else emptyList(),
                                    currentSegmentIndex = 0,
                                    editing = false,
                                )
                                timers = timers.map { if (it.id == timer.id) nextTimer else it }
                                scope.launch {
                                    val savedId = repo.saveTemplate(nextTimer.toTemplate())
                                    timers = timers.map {
                                        if (it.id == timer.id) it.copy(id = savedId) else it
                                    } // [AI生成] 新建模板保存后用数据库 id 替换本地临时负 id，后续编辑可直接 update。
                                }
                            }
                        },
                        onPickRingtone = {
                            pickingRingtoneTimerId = timer.id
                            ringtonePickerLauncher.launch(createRingtonePickerIntent(timer.ringtoneUri))
                        },
                        onCancel = {
                            timers = if (timer.name.isBlank()) {
                                timers.filterNot { it.id == timer.id }
                            } else {
                                timers.map { if (it.id == timer.id) it.copy(editing = false) else it }
                            }
                        },
                    )
                } else {
                    CookingTimerDisplayRow(
                        timer = timer,
                        onStart = {
                            TimerAlarm.cancel(context, timer.id) // [AI修改] 开始前清掉该计时器可能残留的响铃/通知，再重新调度。
                            val now = SystemClock.elapsedRealtime()
                            timers = timers.map {
                                if (it.id == timer.id) {
                                    // [AI修改] 剩余≤0(全新开始)取当前段时长；否则用剩余(继续暂停的当前段)。
                                    val segSecs = it.runSegments.getOrNull(it.currentSegmentIndex)?.seconds ?: it.durationSeconds
                                    val remaining = if (it.remainingSeconds <= 0) segSecs else it.remainingSeconds
                                    // [AI修改] 开始/继续时锚定墙钟结束时刻，供息屏后按真实时间算剩余；并注册系统精确闹钟到点响铃。
                                    val endAt = now + remaining * 1000L
                                    TimerAlarm.schedule(context, it.id, it.name, it.ringtoneUri, endAt)
                                    it.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining, endAtElapsed = endAt)
                                } else {
                                    it
                                }
                            }
                        },
                        onNextSegment = {
                            // [AI生成] 多段手动接力：停当前段铃，进入并启动下一段(用户点了才进，非自动链)。
                            // [AI生成] 时序契约：TimerAlarm.cancel 会触发 activeAlerts 变→对账 Effect 重跑；下面 timers=map 同步把本项置 RUNNING，
                            //   对账重跑时读到的已非 FINISHED 故不复位。**此二者之间禁插入挂起点/scope.launch**，否则对账会误复位。
                            TimerAlarm.cancel(context, timer.id)
                            val now = SystemClock.elapsedRealtime()
                            timers = timers.map {
                                if (it.id == timer.id) {
                                    val nextIdx = it.currentSegmentIndex + 1
                                    val seg = it.runSegments.getOrNull(nextIdx)
                                    if (seg == null) {
                                        // 越界防御：按钮仅在非末段可见，正常不会走到此分支。
                                        it.copy(status = TimerStatus.IDLE, currentSegmentIndex = 0, remainingSeconds = it.runSegments.first().seconds, endAtElapsed = null)
                                    } else {
                                        val endAt = now + seg.seconds * 1000L
                                        TimerAlarm.schedule(context, it.id, it.name, it.ringtoneUri, endAt)
                                        it.copy(status = TimerStatus.RUNNING, currentSegmentIndex = nextIdx, remainingSeconds = seg.seconds, endAtElapsed = endAt)
                                    }
                                } else {
                                    it
                                }
                            }
                        },
                        onPause = {
                            TimerAlarm.cancel(context, timer.id) // [AI生成] 暂停取消到点闹钟。
                            timers = timers.map {
                                if (it.id == timer.id) {
                                    // [AI修改] 暂停时按墙钟结算真实剩余（含息屏期间流逝），并清除结束时刻。
                                    val rem = it.endAtElapsed?.let { end -> remainingFrom(end, SystemClock.elapsedRealtime()) } ?: it.remainingSeconds
                                    it.copy(status = TimerStatus.PAUSED, remainingSeconds = rem, endAtElapsed = null)
                                } else {
                                    it
                                }
                            }
                        },
                        onStop = {
                            // [AI修改] 停止(多段=停止全部) = 撤调度 + 停铃 + 消通知 + 复位到第1段 IDLE，统一走 receiver。
                            TimerAlarm.cancel(context, timer.id)
                            timers = timers.map {
                                if (it.id == timer.id) it.copy(status = TimerStatus.IDLE, currentSegmentIndex = 0, remainingSeconds = it.runSegments.first().seconds, endAtElapsed = null) else it
                            }
                        },
                        onEdit = {
                            timers = timers.map { if (it.id == timer.id) it.copy(editing = true) else it }
                        },
                        onDelete = {
                            TimerAlarm.cancel(context, timer.id) // [AI修改] 删除时撤调度+停铃+消通知。
                            timers = timers.filterNot { it.id == timer.id }
                            if (timer.id > 0) {
                                scope.launch { repo.deleteTemplate(timer.id) }
                            }
                        },
                        onAcknowledgeAlarm = {
                            if (timer.status == TimerStatus.FINISHED) {
                                if (timer.isMultiSegment && timer.currentSegmentIndex < timer.runSegments.lastIndex) {
                                    TimerAlarm.cancel(context, timer.id) // [AI修改] 多段非末段:仅停铃,停在完成态等用户决定(开始下一段/停止全部)。
                                } else {
                                    stopAlarm(timer.id) // 单段/末段:停铃+复位。
                                }
                            }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

private const val MAX_TIMER_SEGMENTS = 6 // [AI生成] 多段上限：家庭烹饪流程罕见超过。

/** 分段编辑草稿：段名 + 分/秒文本，各自独立 Compose 状态。[AI生成] 多段倒计时 */
private class SegmentDraft(name: String, minText: String, secText: String) {
    var name by mutableStateOf(name)
    var minText by mutableStateOf(minText)
    var secText by mutableStateOf(secText)
    val seconds: Int? get() = parseDurationSeconds(minText, secText)
}

@Composable
private fun CookingTimerEditRow(
    timer: CookingTimerItem,
    onSave: (String, String, List<TimerSegment>) -> Unit,
    onPickRingtone: () -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(timer.id) { mutableStateOf(timer.name) }
    var note by remember(timer.id) { mutableStateOf(timer.note) }
    // [AI生成] 段草稿：多段用 segments；否则单段(用 durationSeconds)。默认单段，"添加下一段"转多段。
    val drafts = remember(timer.id) {
        mutableStateListOf<SegmentDraft>().apply {
            if (timer.segments.isNotEmpty()) {
                timer.segments.forEach { add(SegmentDraft(it.name, formatMinutes(it.seconds), formatSeconds(it.seconds))) }
            } else {
                add(SegmentDraft("", formatMinutes(timer.durationSeconds), formatSeconds(timer.durationSeconds)))
            }
        }
    }
    val multi = drafts.size >= 2
    val allValid = drafts.all { it.seconds != null }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("编辑计时", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onPickRingtone, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.Notifications, contentDescription = "选择铃声", modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "取消", modifier = Modifier.size(18.dp))
                }
            }
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("名称") },
                placeholder = { Text("蒸红薯") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            if (!multi) {
                // 单段：仅分/秒（不出现"段名"概念，段名即计时名）
                SegmentDurationInputs(drafts[0], showError = !allValid)
            } else {
                Text("分段计时", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                drafts.forEachIndexed { idx, d ->
                    SegmentEditRow(
                        index = idx + 1,
                        draft = d,
                        canDelete = drafts.size > 1,
                        showError = d.seconds == null,
                        onDelete = { drafts.removeAt(idx) },
                    )
                }
            }
            // 添加下一段（上限 MAX_TIMER_SEGMENTS 段）
            if (drafts.size < MAX_TIMER_SEGMENTS) {
                TextButton(
                    onClick = { drafts.add(SegmentDraft("", "", "")) },
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                    modifier = Modifier.align(Alignment.Start),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加下一段")
                }
            } else {
                Text("最多 $MAX_TIMER_SEGMENTS 段", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (allValid) "铃声：${timer.ringtoneTitle}" else "每段时长需大于 0，秒数 0-59",
                style = MaterialTheme.typography.bodySmall,
                color = if (allValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注") },
                placeholder = { Text("水开后计时") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = { onSave(name.trim(), note.trim(), drafts.map { TimerSegment(it.name.trim(), it.seconds ?: 0) }) },
                enabled = allValid,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存")
            }
        }
    }
}

/** 一段的分/秒输入对（单段与多段行复用）。[AI生成] */
@Composable
private fun SegmentDurationInputs(draft: SegmentDraft, showError: Boolean, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = draft.minText,
            onValueChange = { draft.minText = it.filter(Char::isDigit).take(3) },
            label = { Text("分") },
            placeholder = { Text("00") },
            isError = showError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
        )
        Text("分", color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = draft.secText,
            onValueChange = { draft.secText = it.filter(Char::isDigit).take(2) },
            label = { Text("秒") },
            placeholder = { Text("00") },
            isError = showError,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.weight(1f),
        )
        Text("秒", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** 多段中的一段编辑行：序号圆点 + 可选段名 + 分/秒 + 删除。[AI生成] */
@Composable
private fun SegmentEditRow(index: Int, draft: SegmentDraft, canDelete: Boolean, showError: Boolean, onDelete: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SegmentIndexBadge(index)
            Spacer(Modifier.width(8.dp))
            OutlinedTextField(
                value = draft.name,
                onValueChange = { draft.name = it },
                placeholder = { Text("焯水 / 慢炖 / 收汁") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f),
            )
            if (canDelete) {
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Outlined.Close, contentDescription = "删除该段", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SegmentDurationInputs(draft, showError = showError, modifier = Modifier.padding(start = 32.dp))
    }
}

/** 段序号小圆点 ①②③（编辑态，仅指示）。[AI生成] */
@Composable
private fun SegmentIndexBadge(index: Int) {
    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(24.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text("$index", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 运行态分段进度点 ●●○：已完成实心主色、当前(响铃红)、未开始空心。[AI生成] */
@Composable
private fun SegmentDots(total: Int, currentIndex: Int, isAlarming: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        for (i in 0 until total) {
            val color = when {
                i < currentIndex -> MaterialTheme.colorScheme.primary
                i == currentIndex -> if (isAlarming) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
            }
            val filled = i <= currentIndex
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .then(if (filled) Modifier.background(color) else Modifier.border(1.dp, color, CircleShape)),
            )
        }
    }
}

@Composable
private fun CookingTimerDisplayRow(
    timer: CookingTimerItem,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onStop: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAcknowledgeAlarm: () -> Unit,
    onNextSegment: () -> Unit,
) {
    // [AI修改] 进度按当前段时长算(多段每段各自进度)。
    val segSecs = timer.runSegments.getOrNull(timer.currentSegmentIndex)?.seconds ?: timer.durationSeconds
    val progress = if (segSecs <= 0) 0f else timer.remainingSeconds.toFloat() / segSecs.toFloat()
    val finished = timer.status == TimerStatus.FINISHED
    val isLastSegment = timer.currentSegmentIndex >= timer.runSegments.lastIndex
    val currentSegName = timer.runSegments.getOrNull(timer.currentSegmentIndex)?.name?.takeIf { it.isNotBlank() }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = finished) { onAcknowledgeAlarm() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (finished) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = timer.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = timer.note.ifBlank { "无备注" },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (finished) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = formatDuration(timer.remainingSeconds),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = when (timer.status) {
                        TimerStatus.RUNNING -> MaterialTheme.colorScheme.primary
                        TimerStatus.FINISHED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurface
                    },
                )
            }
            // [AI生成] 多段：段指示行(第N段·段名 + 进度点)。
            if (timer.isMultiSegment) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val segLabel = buildString {
                        append("第${timer.currentSegmentIndex + 1}段")
                        if (currentSegName != null) append("·$currentSegName")
                        if (finished) append(if (isLastSegment) "·全部完成" else "·完成")
                    }
                    Text(
                        text = segLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (finished) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    SegmentDots(total = timer.runSegments.size, currentIndex = timer.currentSegmentIndex, isAlarming = finished)
                }
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Spacer(Modifier.height(10.dp))
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                when {
                    timer.status == TimerStatus.RUNNING || timer.status == TimerStatus.PAUSED -> {
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (timer.isMultiSegment) "停止全部" else "停止")
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = if (timer.status == TimerStatus.RUNNING) onPause else onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(
                                if (timer.status == TimerStatus.RUNNING) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(if (timer.status == TimerStatus.RUNNING) "暂停" else "继续")
                        }
                    }
                    finished && timer.isMultiSegment && !isLastSegment -> {
                        // [AI生成] 多段非末段：停止全部(次) + 停铃·开始下一段(主·一键停铃并启动下一段)。
                        TextButton(onClick = onStop) { Text("停止全部", color = MaterialTheme.colorScheme.error) }
                        Spacer(Modifier.weight(1f))
                        Button(
                            onClick = onNextSegment,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("停铃·开始第${timer.currentSegmentIndex + 2}段")
                        }
                    }
                    finished -> {
                        // 单段/末段：停止 + 提示。
                        Button(
                            onClick = onStop,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        ) {
                            Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("停止")
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (timer.isMultiSegment) "全部完成，关闭铃声" else "计时结束，点击记录或停止按钮关闭铃声",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    else -> {
                        Button(
                            onClick = onStart,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("开始")
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(onClick = onDelete) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Outlined.Edit, contentDescription = "编辑")
                        }
                    }
                }
            }
        }
    }
}

private enum class TimerStatus {
    IDLE,
    RUNNING,
    PAUSED,
    FINISHED,
}

private data class CookingTimerItem(
    val id: Long,
    val name: String = "",
    val durationSeconds: Int = 0,
    val remainingSeconds: Int = durationSeconds,
    val note: String = "",
    val ringtoneUri: String = "",
    val ringtoneTitle: String = "系统默认铃声",
    val sortOrder: Int = 0,
    val status: TimerStatus = TimerStatus.IDLE,
    val editing: Boolean = false,
    // [AI生成] 运行中的目标结束时刻（elapsedRealtime，息屏也走时）；剩余时间按墙钟从它算，
    // 避免息屏时 delay 循环被系统挂起导致倒计时停走。仅 RUNNING 时有值。
    val endAtElapsed: Long? = null,
    // [AI生成] 连续多段：空=单段(用 durationSeconds)；≥1 段=按序手动接力(一段停止后才开始下一段)。[用户 2026-07-18]
    val segments: List<TimerSegment> = emptyList(),
    // [AI生成] 运行中当前段下标(0-based)；单段恒 0。remainingSeconds/endAtElapsed 跟随当前段。
    val currentSegmentIndex: Int = 0,
) {
    /** 实际运行的段序列：多段用 segments，否则退化为单段(用 durationSeconds)。[AI生成] */
    val runSegments: List<TimerSegment>
        get() = if (segments.isNotEmpty()) segments else listOf(TimerSegment("", durationSeconds))
    val isMultiSegment: Boolean get() = segments.size > 1
}

/**
 * 按墙钟(elapsedRealtime)计算剩余秒数，向上取整。[AI生成]
 */
private fun remainingFrom(endAtElapsed: Long, nowElapsed: Long): Int =
    (((endAtElapsed - nowElapsed) + 999) / 1000).toInt().coerceAtLeast(0)

private const val KEY_RUNNING_TIMERS = "cooking_timer_running" // [AI生成] 持久化运行中计时器的偏好 key。

/**
 * 解析持久化的运行中计时器：格式 "id:endWallMillis:segIndex,..."。[AI修改]
 *
 * segIndex 为多段当前段下标；旧格式("id:endWall"无段号)兼容为段 0。返回 id -> (endWall, segIndex)。
 */
private fun parseRunningTimers(raw: String?): Map<Long, Pair<Long, Int>> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        val id = parts.getOrNull(0)?.toLongOrNull()
        val endWall = parts.getOrNull(1)?.toLongOrNull()
        val segIndex = parts.getOrNull(2)?.toIntOrNull() ?: 0 // 旧格式无段号=段0
        if (id != null && endWall != null) id to (endWall to segIndex) else null
    }.toMap()
}

private fun parseDurationSeconds(minutesText: String, secondsText: String): Int? {
    val minutes = minutesText.ifBlank { "0" }.toIntOrNull() ?: return null
    val seconds = secondsText.ifBlank { "0" }.toIntOrNull() ?: return null
    if (minutes < 0 || seconds !in 0..59) return null
    val total = minutes * 60 + seconds
    return total.takeIf { it > 0 }
}

private fun formatMinutes(totalSeconds: Int): String = (totalSeconds.coerceAtLeast(0) / 60).toString().padStart(2, '0')

private fun formatSeconds(totalSeconds: Int): String = (totalSeconds.coerceAtLeast(0) % 60).toString().padStart(2, '0')

private fun formatDuration(totalSeconds: Int): String {
    val safeSeconds = totalSeconds.coerceAtLeast(0)
    val minutes = safeSeconds / 60
    val seconds = safeSeconds % 60
    return "$minutes:${seconds.toString().padStart(2, '0')}"
}

private fun CookingTimerTemplate.toTimerItem(): CookingTimerItem {
    val firstSeconds = if (segments.isNotEmpty()) segments.first().seconds else durationSeconds
    return CookingTimerItem(
        id = id,
        name = name,
        durationSeconds = durationSeconds,
        remainingSeconds = firstSeconds, // [AI修改] 多段时初显首段时长
        note = note,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        sortOrder = sortOrder,
        segments = segments,
    )
}

private fun CookingTimerItem.toTemplate(): CookingTimerTemplate =
    CookingTimerTemplate(
        id = id.takeIf { it > 0 } ?: 0,
        name = name,
        // [AI修改] 多段时 durationSeconds 存首段秒数(保证极老代码读它不崩、退化路径与首段一致)；单段=总时长。
        durationSeconds = if (segments.isNotEmpty()) segments.first().seconds else durationSeconds,
        note = note,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        sortOrder = sortOrder,
        segments = segments,
    )

/** 打开系统"全屏通知"授权设置(A14+)；失败兜底到应用详情页。[AI生成] 华为/A14 全屏引导 */
private fun openFullScreenIntentSettings(context: Context) {
    val pkg = Uri.parse("package:${context.packageName}")
    val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, pkg)
    } else {
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)
    }
    runCatching { context.startActivity(intent) }
        .onFailure { runCatching { context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, pkg)) } }
}

private fun createRingtonePickerIntent(currentUri: String): Intent {
    val existingUri = currentUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "选择倒计时铃声")
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
    }
}

private fun Intent.pickedRingtoneUri(): Uri? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }

private fun getRingtoneTitle(context: Context, uri: Uri?): String {
    if (uri == null) return "系统默认铃声"
    return runCatching {
        RingtoneManager.getRingtone(context.applicationContext, uri)?.getTitle(context.applicationContext)
    }.getOrNull().orEmpty().ifBlank { "系统默认铃声" }
}
