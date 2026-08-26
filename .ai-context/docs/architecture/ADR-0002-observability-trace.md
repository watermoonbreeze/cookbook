# ADR-0002：结构化 Trace 统一契约与脱敏边界

- 状态：`AUTOMATED_GATES_PASS / PENDING_DEVICE_VERIFICATION`
- 日期：2026-08-22
- 范围：`shared/.../platform/TraceModel.kt`、`Logger.kt`、Android sink
- 关联：`experience/15_Trace诊断模板.md`、真机清单 E-OVN-01~06、`fc842a41`、`4c0bdd48`、`606ac9c5`

## 决策

结构化事件统一带 envelope 与可选 `trace_id`；操作失败发射 `operation.error`，终态耗时发射 `operation.duration`；错误字段只保留类型/代码标识，不记录异常 message、用户输入、饮食明细、密钥或完整用户标识。事件名必须登记在 `TraceEventContract`。

## 证据

shared TraceModelTest、LoggerTest、TraceEventContractTest 已通过；E-OVN-01~06 的运行时链路仍待真机，不以静态/自动证据替代。

## 当前收口口径

- `OBS-NEXT-A/B`：`AUTOMATED_GATES_PASS`；自动化测试/构建与静态契约证据已具备。
- `Runtime`：`PENDING_DEVICE_VERIFICATION`；真机验证按用户决定在末期统一执行，不阻断当前代码批次。

## 不决策

不在本 ADR 中接入远程监控、不改变业务流程、不把真机待验证项标记为 PASS。
