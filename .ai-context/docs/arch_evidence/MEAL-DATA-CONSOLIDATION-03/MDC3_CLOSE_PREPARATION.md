# MDC3 R4 Close Preparation

批次：`COOKBOOK-MDC3-AND-FOUNDATION-OBSERVABILITY-BATCH`

状态：`AUTOMATED_GATES_PASS / PENDING_DEVICE_VERIFICATION`

本文件只记录自动证据与收口条件，不写入 `ACCEPTED`/`CLOSED`。

## Code Evidence

| 检查项 | 结果 | 事实依据 |
|---|---|---|
| MDC3 Trace 存在 | PASS | `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/MealDataTraceLogger.kt`；覆盖 Repository、Revision、Projection、UI |
| 三类 Revision 覆盖 | PASS | `MealRecordRepository.observeTimelineWindow` 监听 `meal_record_dish`、`dish_ingredient`、`dish` |
| AppLogger 统一出口 | PASS | `CookbookDiag -> CookbookLog -> installCookbookLogSink -> AppLogger` |
| 独立日志出口 | PASS | 业务源码未发现直接 `android.util.Log`；原生 Log 仅保留在 AppLogger sink 与初始化前 fallback |
| 业务/数据边界 | PASS | 本批未修改业务逻辑、schema、Repository API 或 Meal Data 架构 |

## Test Evidence

命令：

```text
scripts\build-cli.bat :shared:testDebugUnitTest :androidApp:assembleDebug
```

结果：`BUILD SUCCESSFUL`，2026-08-22；shared 单元测试与 Android debug 构建均通过。

## Runtime Evidence Pending

真机验证延后统一执行，不能用静态代码或已有普通 AppLogger 日志替代 MDC3 运行时证据。

必须在同一份导出 debug 日志中观察：

1. 修改 `meal_record_dish` 食用比例：`[MDC3][Revision] meal_record_dish`。
2. 修改 `dish` 菜品信息：`[MDC3][Revision] dish`。
3. 修改 `dish_ingredient` 食材信息：`[MDC3][Revision] dish_ingredient`。
4. 三项操作均出现 `[MDC3][Projection]` 和 `[MDC3][UI]`，且日志来自 AppLogger 文件链路。

当前结论：`OBS-NEXT-A/B=AUTOMATED_GATES_PASS`；`Runtime=PENDING_DEVICE_VERIFICATION`。真机证据按用户决定在末期统一执行，不阻断当前代码；不得写 `ARCH_ACCEPTED`/`ACCEPTED`/`CLOSED`。
