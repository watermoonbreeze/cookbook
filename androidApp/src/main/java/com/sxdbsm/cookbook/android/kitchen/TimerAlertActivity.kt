package com.sxdbsm.cookbook.android.kitchen

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sxdbsm.cookbook.android.ui.theme.CookbookTheme
import com.sxdbsm.cookbook.domain.model.ThemeMode

/**
 * @File : TimerAlertActivity
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 烹饪计时到点全屏提醒（多计时器合并一个全屏，逐个停止）
 * <p>
 * 由通知的 fullScreenIntent 拉起：息屏/锁屏时自动亮屏并全屏显示所有"已到点"的计时器名称，
 * 每个后面一个"停止"按钮——点哪个停哪个对应的门铃(直接停，不必回倒计时项)。多个同时到点只弹这一个全屏。
 * <p>
 * [AI修改] 用户提：①点停止直接停对应门铃 ②多个同时到点合并一个全屏、各自停止。
 **/
class TimerAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [AI生成] 锁屏上方显示 + 点亮屏幕 + 常亮(响铃期间不息屏)。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        setContent {
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                // [AI修改] 修"第2个到点没进全屏、停不掉"：直接 collect 接收器的实时 activeAlerts，
                //   新到点自动加、停止后自动移除；不再依赖 onNewIntent(前台时后来的 fullScreenIntent 不触发它)。
                val liveAlerts by TimerAlarmReceiver.activeAlerts.collectAsState()
                // 首次进入时接收器可能还没 emit(极少数竞态)，用 intent 的单个兜底。
                val fallback = remember {
                    val id = intent.getLongExtra(TimerAlarm.EXTRA_TIMER_ID, -1L).toInt()
                    val name = intent.getStringExtra(TimerAlarm.EXTRA_NAME).orEmpty()
                    if (id >= 0) listOf(id to name) else emptyList()
                }
                val alerts = liveAlerts.ifEmpty { fallback }
                // 全部停完(实时集合空且兜底也空)→退出全屏。
                LaunchedEffect(liveAlerts) {
                    if (liveAlerts.isEmpty() && fallbackStopped) finish()
                }
                AlertContent(
                    alerts = alerts,
                    onStop = { id ->
                        stopRing(id) // 广播 ACTION_STOP → 接收器停铃+消通知+emit(集合自动移除该项)。
                        fallbackStopped = true
                        if (TimerAlarmReceiver.activeAlerts.value.isEmpty()) finish()
                    },
                )
            }
        }
    }

    // [AI生成] 兜底项(intent 单个)被点过停止后置真，避免实时集合本就空时首帧误退。
    private var fallbackStopped = false

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun stopRing(timerId: Int) {
        sendBroadcast(
            Intent(this, TimerAlarmReceiver::class.java).apply {
                action = TimerAlarm.ACTION_STOP
                putExtra(TimerAlarm.EXTRA_TIMER_ID, timerId.toLong())
            },
        )
    }
}

@Composable
private fun AlertContent(alerts: List<Pair<Int, String>>, onStop: (Int) -> Unit) {
    val capsule = RoundedCornerShape(percent = 50)
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("⏰", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                if (alerts.size > 1) "${alerts.size} 个计时结束" else "计时结束",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            if (alerts.size <= 1) {
                // [AI修改] 单条聚焦式(Apple-UX)：名称大字居中 + 全宽 64dp 大停止按钮。
                val (id, name) = alerts.firstOrNull() ?: return@Column
                Spacer(Modifier.height(8.dp))
                Text(
                    if (name.isBlank()) "烹饪计时" else name,
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = { onStop(id) },
                    shape = capsule,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                ) {
                    Text("停 止", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                }
            } else {
                // [AI修改] 多条卡片列表(Apple-UX)：名称 weight(1f)+单行省略，停止按钮固定宽不参与压缩→根治挤压。
                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    alerts.forEach { (id, name) ->
                        Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    if (name.isBlank()) "烹饪计时" else name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f).padding(end = 12.dp),
                                )
                                Button(
                                    onClick = { onStop(id) },
                                    shape = capsule,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError),
                                    modifier = Modifier.widthIn(min = 88.dp).heightIn(min = 48.dp),
                                ) {
                                    Text("停止", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
