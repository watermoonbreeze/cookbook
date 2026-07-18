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
import androidx.compose.runtime.mutableStateListOf
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

    // [AI生成] 当前全屏里列出的"已到点"计时器 (id, 名称)；新计时器到点(onNewIntent)会追加进来。
    private val alerts = mutableStateListOf<Pair<Int, String>>()

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

        refreshAlerts(intent)

        setContent {
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                AlertContent(
                    alerts = alerts,
                    onStop = { id ->
                        stopRing(id) // 直接停对应门铃(广播 ACTION_STOP → 停铃+消通知)。
                        alerts.removeAll { it.first == id }
                        if (alerts.isEmpty()) finish() // 全停完退出全屏。
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        refreshAlerts(intent) // 又有计时器到点：合并进当前全屏列表。
    }

    /** 从接收器的"正在响"集合刷新列表；集合空则退回本次 intent 的单个，仍空则关闭。[AI生成] */
    private fun refreshAlerts(intent: Intent) {
        val live = TimerAlarmReceiver.activeList()
        alerts.clear()
        if (live.isNotEmpty()) {
            alerts.addAll(live)
        } else {
            val id = intent.getLongExtra(TimerAlarm.EXTRA_TIMER_ID, -1L).toInt()
            val name = intent.getStringExtra(TimerAlarm.EXTRA_NAME).orEmpty()
            if (id >= 0) alerts.add(id to name)
        }
        if (alerts.isEmpty()) finish()
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(PaddingValues(32.dp)),
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
            Spacer(Modifier.height(24.dp))
            // 逐个列出到点的计时器：名称 + 各自"停止"。
            alerts.forEach { (id, name) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        if (name.isBlank()) "烹饪计时" else name,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                    Button(
                        onClick = { onStop(id) },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    ) {
                        Text("停止", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
