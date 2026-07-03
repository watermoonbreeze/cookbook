# 任务8 烹饪计时铃声可控结果

- 时间：2026-06-12
- 用户需求：倒计时编辑重新调整为“分 秒”输入；编辑模式增加铃声图标并关联系统铃声选择；倒计时结束时界面红色提示并响铃，点击记录或停止按钮可停止铃声，退出界面不继续响。
- 修改文件：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/kitchen/CookingTimerScreen.kt`。
- 实现内容：编辑模式总时长拆为分钟和秒两个输入框，新建默认显示 `00` 分、`00` 秒，时长必须大于 0 且秒数为 0-59。
- 实现内容：编辑行新增铃声图标，点击后打开系统 `ACTION_RINGTONE_PICKER`，选择结果保存为当前计时器的铃声 URI 和显示名称。
- 实现内容：倒计时归零后进入 `FINISHED` 告警态，计时卡片使用 `errorContainer` 红色状态展示，并显示停止提示。
- 实现内容：响铃改为保存 `Ringtone` 实例，用户点击完成记录或“停止”按钮时会停止铃声并恢复显示态。
- 实现内容：页面退出或铃声实例替换时通过 `DisposableEffect` 主动停止旧铃声，避免离开界面后仍响。
- 边界说明：当前仍是页面内前台计时；后台/锁屏提醒和通知属于后续扩展。
- 验证：`./gradlew :androidApp:compileDebugKotlin` 成功。
- 验证：`./gradlew :androidApp:assembleDebug` 成功。
