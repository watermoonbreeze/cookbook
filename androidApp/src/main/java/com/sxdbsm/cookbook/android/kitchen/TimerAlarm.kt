package com.sxdbsm.cookbook.android.kitchen

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.sxdbsm.cookbook.android.MainActivity

/**
 * @File : TimerAlarm
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 烹饪计时的系统级到点提醒（AlarmManager + 广播接收器 + 通知）
 * <p>
 * 把响铃从前台 Compose 协程搬到系统精确闹钟：开始计时按结束时刻(elapsedRealtime)注册闹钟，
 * 息屏/后台/被杀时系统也能到点唤醒接收器响铃并发通知。多个计时器各用 timerId 作唯一
 * requestCode/通知 id，互不覆盖、各自准时响。
 * <p>
 * [AI生成] 修复"息屏倒计时到点不响"：可靠的厨房计时器必须在人不看手机时也能提醒。
 **/
object TimerAlarm {
    const val CHANNEL_ID = "cooking_timer_alarm"
    const val ACTION_FIRE = "com.sxdbsm.cookbook.action.TIMER_FIRE"
    const val ACTION_STOP = "com.sxdbsm.cookbook.action.TIMER_STOP"
    const val EXTRA_TIMER_ID = "timer_id"
    const val EXTRA_NAME = "timer_name"
    const val EXTRA_RINGTONE = "timer_ringtone"

    /** 建通知渠道（幂等），App 启动或首次调度前调用。[AI生成] */
    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "烹饪计时提醒", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "烹饪倒计时到点提醒"
                setSound(null, null) // 铃声由 Ringtone 自播（可循环），避免通知再出一次声。
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
        }
    }

    /**
     * 注册到点闹钟。[AI生成]
     *
     * @param endAtElapsed 结束时刻，与 UI 一致用 `SystemClock.elapsedRealtime()` 基准。
     */
    fun schedule(context: Context, timerId: Long, name: String, ringtoneUri: String, endAtElapsed: Long) {
        ensureChannel(context)
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        val pi = firePendingIntent(context, timerId, name, ringtoneUri)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        runCatching {
            if (canExact) {
                am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAtElapsed, pi)
            } else {
                // A12+ 未授精确闹钟权限时降级为不精确（Doze 下可能晚几分钟）。
                am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, endAtElapsed, pi)
            }
        }
    }

    /** 取消到点闹钟（暂停/停止/删除/前台已响时）。[AI生成] */
    fun cancel(context: Context, timerId: Long) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.cancel(firePendingIntent(context, timerId, "", ""))
        // 若已响，顺带停铃与消通知。
        TimerAlarmReceiver.stop(context, timerId.toInt())
    }

    private fun firePendingIntent(context: Context, timerId: Long, name: String, ringtoneUri: String): PendingIntent {
        val intent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = ACTION_FIRE
            putExtra(EXTRA_TIMER_ID, timerId)
            putExtra(EXTRA_NAME, name)
            putExtra(EXTRA_RINGTONE, ringtoneUri)
        }
        return PendingIntent.getBroadcast(
            context,
            timerId.toInt(), // [AI生成] timerId 作唯一 requestCode，多计时器互不覆盖。
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}

/**
 * 计时到点/停止的广播接收器（Manifest 注册）。[AI生成]
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(TimerAlarm.EXTRA_TIMER_ID, -1L).toInt()
        if (id < 0) return
        when (intent.action) {
            TimerAlarm.ACTION_STOP -> stop(context, id)
            else -> fire(
                context,
                id,
                intent.getStringExtra(TimerAlarm.EXTRA_NAME).orEmpty(),
                intent.getStringExtra(TimerAlarm.EXTRA_RINGTONE).orEmpty(),
            )
        }
    }

    private fun fire(context: Context, id: Int, name: String, ringtoneUri: String) {
        TimerAlarm.ensureChannel(context)
        // [AI生成] 尽力点亮屏幕（配合全屏提醒），旧版本有效；新版本主要靠 fullScreenIntent。
        runCatching {
            val pm = context.getSystemService(android.os.PowerManager::class.java)
            @Suppress("DEPRECATION")
            val wl = pm?.newWakeLock(
                android.os.PowerManager.FULL_WAKE_LOCK or android.os.PowerManager.ACQUIRE_CAUSES_WAKEUP or android.os.PowerManager.ON_AFTER_RELEASE,
                "cookbook:timer_alert",
            )
            wl?.acquire(10_000L)
        }
        // 1) 播放铃声（ALARM 流，循环），按 id 持有以便分别停止。
        runCatching {
            val uri = ringtoneUri.takeIf { it.isNotBlank() }?.let(Uri::parse)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = uri?.let { RingtoneManager.getRingtone(context.applicationContext, it) }
            if (ringtone != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) ringtone.isLooping = true
                ringtone.play()
                putActive(id, ringtone, name) // [AI修改] 入 active + emit StateFlow(全屏实时增)
            }
        }
        // 2) 高优先级通知（带“停止”动作）。未授通知权限时静默失败，不影响铃声。
        val stopIntent = Intent(context, TimerAlarmReceiver::class.java).apply {
            action = TimerAlarm.ACTION_STOP
            putExtra(TimerAlarm.EXTRA_TIMER_ID, id.toLong())
        }
        val stopPi = PendingIntent.getBroadcast(
            context,
            id + STOP_REQUEST_OFFSET,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // [AI生成] 点击通知进 App；全屏意图拉起锁屏全屏提醒(自动亮屏)。
        val openPi = PendingIntent.getActivity(
            context,
            id + CONTENT_REQUEST_OFFSET,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_TIMER, true) // [AI生成] 点击进烹饪计时页。
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val fullScreenPi = PendingIntent.getActivity(
            context,
            id + FULLSCREEN_REQUEST_OFFSET,
            Intent(context, TimerAlertActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(TimerAlarm.EXTRA_TIMER_ID, id.toLong())
                putExtra(TimerAlarm.EXTRA_NAME, name)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, TimerAlarm.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("计时结束")
            .setContentText(if (name.isBlank()) "烹饪计时到点了" else "「$name」到点了")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(false)
            .setOngoing(true) // 长驻，点“停止”或回 App 停止后才消失。
            .setContentIntent(openPi) // 点击通知进入 App。
            .setFullScreenIntent(fullScreenPi, true) // 息屏/锁屏时弹全屏提醒并点亮屏幕。
            .addAction(0, "停止", stopPi)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(id, notification) }
    }

    companion object {
        private const val STOP_REQUEST_OFFSET = 100000 // [AI生成] 停止 PendingIntent 的 requestCode 偏移，避开 fire 的 timerId。
        private const val CONTENT_REQUEST_OFFSET = 200000 // [AI生成] 点击进 App 的 requestCode 偏移。
        private const val FULLSCREEN_REQUEST_OFFSET = 300000 // [AI生成] 全屏提醒的 requestCode 偏移。
        // [AI修改] 用户提:全屏要列出所有已到时计时器+各自停止。active 带名字；
        //   [AI修改] 修"第2个到点没加进全屏、停不掉":全屏 Activity 已在前台时后来的 fullScreenIntent 不再 onNewIntent，
        //   故改用 **StateFlow 实时驱动**——fire/stop 变更 active 即 emit，Activity collect 自动增删，不依赖 intent 刷新。
        private data class ActiveAlert(val ringtone: Ringtone, val name: String)
        private val active = mutableMapOf<Int, ActiveAlert>() // timerId -> (正在响的铃声, 名称)
        private val _activeAlerts = kotlinx.coroutines.flow.MutableStateFlow<List<Pair<Int, String>>>(emptyList())

        /** 当前所有正在响的计时器 (id, 名称)，实时。全屏提醒 collect 它自动增删。[AI生成] */
        val activeAlerts: kotlinx.coroutines.flow.StateFlow<List<Pair<Int, String>>> = _activeAlerts

        private fun emitActive() {
            _activeAlerts.value = active.entries.map { it.key to it.value.name }.sortedBy { it.first }
        }

        internal fun putActive(id: Int, ringtone: Ringtone, name: String) {
            active[id]?.ringtone?.stop()
            active[id] = ActiveAlert(ringtone, name)
            emitActive()
        }

        /** 停止某计时器的铃声并消通知。[AI生成] */
        fun stop(context: Context, id: Int) {
            runCatching { active.remove(id)?.ringtone?.stop() }
            runCatching { NotificationManagerCompat.from(context).cancel(id) }
            emitActive()
        }
    }
}
