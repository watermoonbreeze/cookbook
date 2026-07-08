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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * @Desc : 烹饪计时到点全屏提醒
 * <p>
 * 由通知的 fullScreenIntent 拉起：息屏/锁屏时自动亮屏并在锁屏上方全屏显示"计时结束 + 名称"，
 * 提供"停止"按钮（停铃 + 消通知）。
 * <p>
 * [AI生成] 满足"息屏到点自动亮屏、锁屏显示倒计时信息、可点关闭"。
 **/
class TimerAlertActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // [AI生成] 锁屏上方显示 + 点亮屏幕。
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }

        val timerId = intent.getLongExtra(TimerAlarm.EXTRA_TIMER_ID, -1L)
        val name = intent.getStringExtra(TimerAlarm.EXTRA_NAME).orEmpty()

        setContent {
            CookbookTheme(themeMode = ThemeMode.SYSTEM) {
                AlertContent(name = name, onStop = {
                    stopRing(timerId)
                    finish()
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun stopRing(timerId: Long) {
        // [AI生成] 通知同一接收器停铃 + 消通知。
        sendBroadcast(
            Intent(this, TimerAlarmReceiver::class.java).apply {
                action = TimerAlarm.ACTION_STOP
                putExtra(TimerAlarm.EXTRA_TIMER_ID, timerId)
            },
        )
    }
}

@Composable
private fun AlertContent(name: String, onStop: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(PaddingValues(32.dp)),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("⏰", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                "计时结束",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (name.isBlank()) "烹饪计时到点了" else "「$name」到点了",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Spacer(Modifier.height(40.dp))
            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                Text("停止", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
