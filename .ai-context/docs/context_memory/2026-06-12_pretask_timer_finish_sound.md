# 任务前上下文快照：烹饪计时结束铃声

- 时间：2026-06-12
- 用户需求：烹饪倒计时结束后，需要有铃声响起。
- 任务类型：Feature 补充。
- 执行深度：轻量。
- 交互模式：常规。
- 计划角色：主线程模拟 DEV_UI/DEV_CODE 实现声音反馈，DEV_TEST 编译验证。
- 已知项目状态：任务7已新增 `CookingTimerScreen`，当前计时器为页面内状态，倒计时归零后回到显示态。
- 预计涉及文件：`androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/kitchen/CookingTimerScreen.kt`。
- 主要风险：系统铃声 URI 可能不存在；响铃不能重复触发；离开页面后的后台响铃不在本次范围。
- 待验证项：倒计时归零时只播放一次系统 alarm/notification 声音，编译通过。
