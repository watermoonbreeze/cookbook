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

## 04 UBF Experience Governance

- 状态：GOVERNANCE_COMPLETE / PENDING ARCH REVIEW。
- 事实：记录本次 `GPT-5 / Codex / 标准级 / 常规五阶段执行`；推理等级未由系统提供。
- 评价边界：仅记录事实，不依据单批结果形成能力结论。

## 05 Trace Diagnostic

- 状态：DOCUMENTATION_COMPLETE / PENDING ARCH REVIEW。
- 交付：`experience/15_Trace诊断模板.md`，包含阶段时间线、失败节点分类、证据与结论格式。
- 边界：只做诊断辅助，不自动修改业务代码；模板要求脱敏。

## 06 Blueprint Standard

- 状态：GOVERNANCE_COMPLETE / PENDING ARCH REVIEW。
- 交付：`experience/16_蓝图任务包标准.md`，固化六件套、最小模板、旧包兼容和真机证据门禁。

## 07 Actor Capability Routing

- 状态：GOVERNANCE_COMPLETE / PENDING ARCH REVIEW。
- 交付：`experience/17_ACTOR_CAPABILITY_ROUTING.md`；能力维度与历史证据逐行映射。
- 边界：不替代 `MODEL_ROUTING`，不修改模型配置；小样本只保留观察事实。

## 08 Blueprint Level Standard

- 状态：GOVERNANCE_COMPLETE / PENDING ARCH REVIEW。
- 交付：`experience/18_BLUEPRINT_LEVEL_STANDARD.md`，引用 canonical protocol 并映射 L1→L7。
- 边界：Level 不是风险/模型等级；不新增 L8、不下调 L7 基线。

## 09 Architecture Quality Governance

- 状态：GOVERNANCE_COMPLETE / PENDING ARCH REVIEW。
- 交付：`architecture_quality_check.py` + unittest，检查 KMP 依赖边界和 Trace 契约存在性。
- 验证：静态检查应在当前项目通过；测试含 Android import 反例。
