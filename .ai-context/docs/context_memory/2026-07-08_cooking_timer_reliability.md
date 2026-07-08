# 烹饪计时可靠性改造（息屏/后台/多计时/前台服务/全屏提醒）· 2026-07-08

[AI生成] 从"息屏倒计时停走"一路做到系统级可靠计时。多个提交，部分已 push，末尾数个待验证后 push。

## 根因与三层修复

1. **息屏倒计时停走**（97fac70）：原用 `while(running){delay(1000);remaining--}` 每秒递减，息屏时 Compose 协程被挂起。改为**墙钟计时**：`CookingTimerItem.endAtElapsed`(SystemClock.elapsedRealtime 结束时刻)，tick 按 `remainingFrom(endAt-now)` 算剩余；start 锚定、pause 结算、stop/finish 清空。

2. **系统级到点响铃**（AlarmManager + 接收器 + 通知）：`TimerAlarm`(object scheduler + `TimerAlarmReceiver`)。start 时 `setExactAndAllowWhileIdle(ELAPSED_REALTIME_WAKEUP, endAt, pi)`；息屏/后台/被杀系统唤醒接收器播铃(ALARM循环)+通知。**多计时器**各用 timerId 作唯一 requestCode/通知id，Ringtone 用 `Map<Int,Ringtone>` 分别持有分别停。前台完成走 in-app 响铃并 cancel 闹钟去重。

3. **前台服务 + 全屏提醒**：
   - `CookingTimerService`(FGS, specialUse 类型/A14)：有计时运行就常驻"进行中"通知(点进App)，保活；全部过期自停。`sync(context, endWalls)` 按运行集启停。
   - `TimerAlertActivity`(showWhenLocked+turnScreenOn)：fullScreenIntent 拉起，锁屏全屏"计时结束"+停止。
   - 运行态持久化：user_preferences KEY `cooking_timer_running` 存 {id:墙钟结束}，重开恢复显示。

## 实测踩坑与修（2ae9025）

- **前台服务通知默认最多 10 秒延迟显示**（防短命服务）→ 现象"点开始不立刻出现、离开界面才出现"。修：`setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)`。
- 划掉应用系统重启服务时 intent 为 null 会自毁 → `START_REDELIVER_INTENT` + `lastEndWalls` 兜底。
- 到点唤醒屏幕 → 接收器加 WakeLock(ACQUIRE_CAUSES_WAKEUP，旧版本有效)。

## 已知限制 / 待办

- **Android 14+ `USE_FULL_SCREEN_INTENT` 默认关闭**（非闹钟类 App）→ 全屏提醒降级为横幅、不亮屏。需引导用户到"设置→应用→通知→允许全屏通知"，或代码用 `ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT` 引导。**待用户确认设备 Android 版本后补**。
- 划掉应用后常驻通知消失/闹钟不响：部分国产 ROM"划掉即强杀"，纯代码绕不过；响铃靠系统闹钟不受影响，需用户开自启动/后台。
- CookingTimerService/sync 里有 AppLogger 排查日志（tag `TimerSvc`），稳定后可精简。

## 相关文件
`ui/kitchen/CookingTimerScreen.kt`、`kitchen/TimerAlarm.kt`、`kitchen/TimerAlarmReceiver`、`kitchen/CookingTimerService.kt`、`kitchen/TimerAlertActivity.kt`、`AndroidManifest.xml`。
