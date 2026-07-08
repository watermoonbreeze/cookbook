package com.sxdbsm.cookbook.android.ui.kitchen

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.sxdbsm.cookbook.android.kitchen.TimerAlarm
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.data.repository.CookingTimerRepository
import com.sxdbsm.cookbook.data.repository.PreferenceRepository
import com.sxdbsm.cookbook.domain.model.CookingTimerTemplate
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
    var activeAlarmTimerId by remember { mutableStateOf<Long?>(null) }
    var activeRingtone by remember { mutableStateOf<Ringtone?>(null) }
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

    fun stopActiveAlarm(resetTimerId: Long? = null) {
        activeRingtone?.stop()
        val resetTargetId = resetTimerId ?: activeAlarmTimerId
        resetTargetId?.let { TimerAlarm.cancel(context, it) } // [AI生成] 同时停系统闹钟铃声/消通知（含背景响铃后回 App 停止）。
        activeRingtone = null
        activeAlarmTimerId = null
        if (resetTargetId != null) {
            timers = timers.map {
                if (it.id == resetTargetId && it.status == TimerStatus.FINISHED) {
                    it.copy(status = TimerStatus.IDLE, remainingSeconds = it.durationSeconds, endAtElapsed = null)
                } else {
                    it
                }
            } // [AI生成] 用户点击完成告警记录或停止按钮时，同时停止铃声并恢复到显示态。
        }
    }

    DisposableEffect(activeRingtone) {
        val ringtoneToDispose = activeRingtone
        onDispose { ringtoneToDispose?.stop() } // [AI生成] 页面退出或 Ringtone 被替换时主动停止，避免离开界面后继续响铃。
    }

    LaunchedEffect(Unit) {
        val loadedTimers = repo.listTemplates().map { it.toTimerItem() }
        // [AI生成] 恢复运行态：持久化存的是墙钟结束时刻，重开按它算剩余并重锚 elapsedRealtime；
        // 已过期的显示为完成（响铃由之前注册的系统闹钟处理，不重复响）。
        val running = parseRunningTimers(prefs.get(KEY_RUNNING_TIMERS))
        val nowWall = System.currentTimeMillis()
        val nowElapsed = SystemClock.elapsedRealtime()
        timers = loadedTimers.map { t ->
            val endWall = running[t.id] ?: return@map t
            val remaining = (((endWall - nowWall) + 999) / 1000).toInt()
            if (remaining > 0) {
                t.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining, endAtElapsed = nowElapsed + remaining * 1000L)
            } else {
                t.copy(status = TimerStatus.FINISHED, remainingSeconds = 0, endAtElapsed = null)
            }
        }
        loaded = true
    }

    // [AI生成] 运行中计时器集合变化时（开始/暂停/停止/完成）持久化墙钟结束时刻；tick 每秒只改 remaining、不触发此保存。
    val runningSnapshot = timers.filter { it.status == TimerStatus.RUNNING && it.endAtElapsed != null }
    LaunchedEffect(loaded, runningSnapshot.map { it.id to it.endAtElapsed }) {
        if (!loaded) return@LaunchedEffect
        val now = SystemClock.elapsedRealtime()
        val nowWall = System.currentTimeMillis()
        val encoded = runningSnapshot.joinToString(",") { "${it.id}:${nowWall + (it.endAtElapsed!! - now)}" }
        prefs.set(KEY_RUNNING_TIMERS, encoded)
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
            var finishedTimer: CookingTimerItem? = null
            timers = timers.map { timer ->
                if (timer.status != TimerStatus.RUNNING) {
                    timer
                } else {
                    val end = timer.endAtElapsed
                    val nextRemaining = if (end != null) remainingFrom(end, now) else (timer.remainingSeconds - 1).coerceAtLeast(0)
                    if (timer.remainingSeconds > 0 && nextRemaining == 0) {
                        finishedTimer = timer.copy(remainingSeconds = 0, status = TimerStatus.FINISHED, endAtElapsed = null)
                    }
                    timer.copy(
                        remainingSeconds = nextRemaining,
                        status = if (nextRemaining == 0) TimerStatus.FINISHED else TimerStatus.RUNNING,
                        endAtElapsed = if (nextRemaining == 0) null else timer.endAtElapsed,
                    ) // [AI生成] 倒计时归零后进入完成告警态，等待用户点击记录或停止按钮确认。
                }
            }
            finishedTimer?.let { timer ->
                // [AI生成] 前台已检测到完成：取消/停系统闹钟，改由前台响铃，避免与闹钟双响。
                TimerAlarm.cancel(context, timer.id)
                activeRingtone?.stop()
                activeRingtone = playTimerFinishedSound(context, timer.ringtoneUri)
                activeAlarmTimerId = timer.id
            }
        }
    }

    Scaffold(
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("烹饪计时", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                    navigationIconContentColor = MaterialTheme.colorScheme.secondary,
                    actionIconContentColor = MaterialTheme.colorScheme.secondary,
                ),
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
            if (!loaded) {
                item {
                    Text(
                        text = "加载计时模板中...",
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
                        onSave = { name, minuteText, secondText, note ->
                            parseDurationSeconds(minuteText, secondText)?.let { seconds ->
                                val nextTimer = timer.copy(
                                    name = name.ifBlank { "未命名计时" },
                                    durationSeconds = seconds,
                                    remainingSeconds = seconds,
                                    note = note,
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
                            stopActiveAlarm()
                            val now = SystemClock.elapsedRealtime()
                            timers = timers.map {
                                if (it.id == timer.id) {
                                    val remaining = if (it.remainingSeconds <= 0) it.durationSeconds else it.remainingSeconds
                                    // [AI修改] 开始/继续时锚定墙钟结束时刻，供息屏后按真实时间算剩余；并注册系统精确闹钟到点响铃。
                                    val endAt = now + remaining * 1000L
                                    TimerAlarm.schedule(context, it.id, it.name, it.ringtoneUri, endAt)
                                    it.copy(status = TimerStatus.RUNNING, remainingSeconds = remaining, endAtElapsed = endAt)
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
                            TimerAlarm.cancel(context, timer.id) // [AI生成] 停止取消到点闹钟并停铃。
                            if (timer.status == TimerStatus.FINISHED) {
                                stopActiveAlarm(resetTimerId = timer.id)
                            }
                            timers = timers.map {
                                if (it.id == timer.id) it.copy(status = TimerStatus.IDLE, remainingSeconds = it.durationSeconds, endAtElapsed = null) else it
                            }
                        },
                        onEdit = {
                            timers = timers.map { if (it.id == timer.id) it.copy(editing = true) else it }
                        },
                        onDelete = {
                            TimerAlarm.cancel(context, timer.id) // [AI生成] 删除取消到点闹钟。
                            stopActiveAlarm()
                            timers = timers.filterNot { it.id == timer.id }
                            if (timer.id > 0) {
                                scope.launch { repo.deleteTemplate(timer.id) }
                            }
                        },
                        onAcknowledgeAlarm = {
                            if (timer.status == TimerStatus.FINISHED) {
                                stopActiveAlarm(resetTimerId = timer.id)
                            }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun CookingTimerEditRow(
    timer: CookingTimerItem,
    onSave: (String, String, String, String) -> Unit,
    onPickRingtone: () -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember(timer.id, timer.name) { mutableStateOf(timer.name) }
    var minuteText by remember(timer.id, timer.durationSeconds) { mutableStateOf(formatMinutes(timer.durationSeconds)) }
    var secondText by remember(timer.id, timer.durationSeconds) { mutableStateOf(formatSeconds(timer.durationSeconds)) }
    var note by remember(timer.id, timer.note) { mutableStateOf(timer.note) }
    val durationValid = parseDurationSeconds(minuteText, secondText) != null

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
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = minuteText,
                    onValueChange = { value -> minuteText = value.filter(Char::isDigit).take(3) },
                    label = { Text("分") },
                    placeholder = { Text("00") },
                    isError = !durationValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                Text("分", color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = secondText,
                    onValueChange = { value -> secondText = value.filter(Char::isDigit).take(2) },
                    label = { Text("秒") },
                    placeholder = { Text("00") },
                    isError = !durationValid,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f),
                )
                Text("秒", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                text = if (durationValid) "铃声：${timer.ringtoneTitle}" else "总时长需大于 0，秒数 0-59",
                style = MaterialTheme.typography.bodySmall,
                color = if (durationValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
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
                onClick = { onSave(name.trim(), minuteText.trim(), secondText.trim(), note.trim()) },
                enabled = durationValid,
                modifier = Modifier.align(Alignment.End),
            ) {
                Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("保存")
            }
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
) {
    val progress = if (timer.durationSeconds <= 0) {
        0f
    } else {
        timer.remainingSeconds.toFloat() / timer.durationSeconds.toFloat()
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = timer.status == TimerStatus.FINISHED) { onAcknowledgeAlarm() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.outlinedCardColors(
            containerColor = if (timer.status == TimerStatus.FINISHED) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
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
                        color = if (timer.status == TimerStatus.FINISHED) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
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
                if (timer.status == TimerStatus.RUNNING || timer.status == TimerStatus.PAUSED) {
                    Button(
                        onClick = onStop,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("停止")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = if (timer.status == TimerStatus.RUNNING) onPause else onStart,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                    ) {
                        Icon(
                            if (timer.status == TimerStatus.RUNNING) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(if (timer.status == TimerStatus.RUNNING) "暂停" else "继续")
                    }
                } else if (timer.status == TimerStatus.FINISHED) {
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
                        text = "计时结束，点击记录或停止按钮关闭铃声",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                } else {
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
)

/**
 * 按墙钟(elapsedRealtime)计算剩余秒数，向上取整。[AI生成]
 */
private fun remainingFrom(endAtElapsed: Long, nowElapsed: Long): Int =
    (((endAtElapsed - nowElapsed) + 999) / 1000).toInt().coerceAtLeast(0)

private const val KEY_RUNNING_TIMERS = "cooking_timer_running" // [AI生成] 持久化运行中计时器的偏好 key。

/**
 * 解析持久化的运行中计时器：格式 "id:endWallMillis,id:endWallMillis"。[AI生成]
 */
private fun parseRunningTimers(raw: String?): Map<Long, Long> {
    if (raw.isNullOrBlank()) return emptyMap()
    return raw.split(",").mapNotNull { entry ->
        val parts = entry.split(":")
        val id = parts.getOrNull(0)?.toLongOrNull()
        val endWall = parts.getOrNull(1)?.toLongOrNull()
        if (id != null && endWall != null) id to endWall else null
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

private fun CookingTimerTemplate.toTimerItem(): CookingTimerItem =
    CookingTimerItem(
        id = id,
        name = name,
        durationSeconds = durationSeconds,
        remainingSeconds = durationSeconds,
        note = note,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        sortOrder = sortOrder,
    )

private fun CookingTimerItem.toTemplate(): CookingTimerTemplate =
    CookingTimerTemplate(
        id = id.takeIf { it > 0 } ?: 0,
        name = name,
        durationSeconds = durationSeconds,
        note = note,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        sortOrder = sortOrder,
    )

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

private fun playTimerFinishedSound(context: Context, ringtoneUri: String): Ringtone? {
    return runCatching {
        val uri = ringtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: return null
        RingtoneManager.getRingtone(context.applicationContext, uri)?.also { ringtone ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.isLooping = true
            }
            ringtone.play()
        }
    }.getOrNull() // [AI生成] 保存 Ringtone 实例用于用户点击记录、停止按钮或退出页面时主动停止。
}
