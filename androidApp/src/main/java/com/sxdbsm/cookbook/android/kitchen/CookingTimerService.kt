package com.sxdbsm.cookbook.android.kitchen

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.sxdbsm.cookbook.android.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * @File : CookingTimerService
 * @Time : 2026/07/08
 * @Author : SXD-AI
 * @Desc : 烹饪计时前台服务
 * <p>
 * 只要有计时器在运行就常驻一条"进行中"通知（不管 App 前台/后台/关闭都在通知栏可见、可点进 App），
 * 并借前台服务保活进程，改善"关闭 App 后到点闹钟不响"。响铃仍由 TimerAlarm 的系统闹钟触发；
 * 本服务只负责常驻状态展示与保活，所有运行中的计时器都过期后自动停止。
 * <p>
 * [AI生成] 满足"开启倒计时就需要长驻通知"，并提升后台/被杀时的可靠性。
 **/
class CookingTimerService : Service() {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        val endWalls = intent?.getLongArrayExtra(EXTRA_END_WALLS)?.toList().orEmpty()
        ensureChannel(this)
        startForegroundCompat(buildNotification(endWalls))
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val future = endWalls.filter { it > now }
                if (future.isEmpty()) {
                    stopSelfSafely()
                    break
                }
                notify(this@CookingTimerService, buildNotification(future))
                delay(1000)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        tickJob?.cancel()
        super.onDestroy()
    }

    private fun startForegroundCompat(notification: android.app.Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun stopSelfSafely() {
        tickJob?.cancel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
        stopSelf()
    }

    private fun buildNotification(endWalls: List<Long>): android.app.Notification {
        val now = System.currentTimeMillis()
        val nearest = endWalls.filter { it > now }.minOrNull()
        val text = if (nearest != null) {
            val remaining = ((nearest - now + 999) / 1000).toInt().coerceAtLeast(0)
            val n = endWalls.count { it > now }
            "最近 ${remaining / 60}:${(remaining % 60).toString().padStart(2, '0')} 后完成" + if (n > 1) "（共 $n 个进行中）" else ""
        } else {
            "计时进行中"
        }
        val openPi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("🍳 烹饪计时进行中")
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openPi)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val ACTION_STOP = "com.sxdbsm.cookbook.action.TIMER_SERVICE_STOP"
        const val EXTRA_END_WALLS = "end_walls"
        private const val NOTIF_ID = 424242
        private const val CHANNEL_ID = "cooking_timer_ongoing"

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(CHANNEL_ID, "烹饪计时进行中", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "计时进行中的常驻状态"
                    setShowBadge(false)
                }
                context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(channel)
            }
        }

        private fun notify(context: Context, notification: android.app.Notification) {
            runCatching { context.getSystemService(NotificationManager::class.java)?.notify(NOTIF_ID, notification) }
        }

        /**
         * 根据当前运行中计时器的墙钟结束时刻同步前台服务：非空则启动/刷新，空则停止。[AI生成]
         */
        fun sync(context: Context, endWalls: List<Long>) {
            val intent = Intent(context, CookingTimerService::class.java)
            if (endWalls.isEmpty()) {
                intent.action = ACTION_STOP
                runCatching { context.startService(intent) }
            } else {
                intent.putExtra(EXTRA_END_WALLS, endWalls.toLongArray())
                runCatching { ContextCompat.startForegroundService(context, intent) }
            }
        }
    }
}
