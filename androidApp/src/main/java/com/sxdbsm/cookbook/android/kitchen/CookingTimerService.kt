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
import com.sxdbsm.cookbook.android.util.AppLogger
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
    private var lastEndWalls: List<Long> = emptyList() // [AI生成] 被系统重启且 intent 为 null 时的兜底数据。

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelfSafely()
            return START_NOT_STICKY
        }
        // [AI修改] 被系统重启(intent 可能为 null)且无数据时不自毁：读取上次 endWalls；仍为空才停止。
        val endWalls = intent?.getLongArrayExtra(EXTRA_END_WALLS)?.toList() ?: lastEndWalls
        lastEndWalls = endWalls
        AppLogger.d("TimerSvc", "onStartCommand endWalls=${endWalls.size}")
        ensureChannel(this)
        try {
            startForegroundCompat(buildNotification(endWalls))
            AppLogger.d("TimerSvc", "startForeground ok")
        } catch (t: Throwable) {
            AppLogger.e("TimerSvc", "startForeground failed", t) // [AI生成] 前台服务起步失败排查（如 FGS 类型/后台启动限制）。
            stopSelf()
            return START_NOT_STICKY
        }
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
        return START_REDELIVER_INTENT // [AI修改] 被杀后系统重启时重投最后一个带 endWalls 的 intent。
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
            // [AI修改] 强制立即显示：前台服务通知默认有最多 10 秒延迟显示，导致“点开始不立刻出现、离开界面才出现”。
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
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
                    .onFailure { AppLogger.e("TimerSvc", "stop startService failed", it) }
            } else {
                intent.putExtra(EXTRA_END_WALLS, endWalls.toLongArray())
                AppLogger.d("TimerSvc", "sync start FGS, endWalls=${endWalls.size}")
                runCatching { ContextCompat.startForegroundService(context, intent) }
                    .onFailure { AppLogger.e("TimerSvc", "startForegroundService failed", it) }
            }
        }
    }
}
