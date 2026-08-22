# COOKBOOK 夜间批量执行记录（2026-08-22）

本文件记录 01→06 的可追溯执行事实；每项任务完成后追加，不替代 `BLUEPRINT_STATE.md`。

## 01 Observability Completion

- 状态：CODE_COMPLETE / PENDING_DEVICE_VERIFICATION。
- 改动：统一 `operation.error` 事件；失败操作自动关联 `trace_id`；保留 `operation.duration`；新增 shared 单测。
- 自动证据：shared TraceModelTest、LoggerTest 通过。
- 真机：E-OVN-01~03 已登记，未执行。

## 02 AI Recommend Diagnostic

- 状态：CODE_COMPLETE / PENDING_DEVICE_VERIFICATION。
- 事实：`Meal Edit -> AI Recommend` 使用 `meal_edit`；`Record Meal -> Manual Select -> AI Recommend` 使用 `record_meal_manual`。
- 改动：入口导航发射 `recommend.route` DataFlow，沿用入口 Action 的 `trace_id`；不记录输入、菜名或推荐结果内容。
- 自动证据：LoggerTest 覆盖两个入口代码可区分且路由值受控。
- 真机：E-OVN-04~05 已登记，未执行。

## 03 Trace Testing

- 状态：CODE_COMPLETE / PENDING_DEVICE_VERIFICATION。
- 改动：新增 `TraceEventContract` 稳定事件名清单，覆盖 UI、导航、操作、耗时、错误、系统与兼容事件；新增未知事件探针测试。
- 自动证据：`TraceEventContractTest`、既有 TraceModelTest/LoggerTest 通过。
- 真机：E-OVN-06 已登记，未执行。
