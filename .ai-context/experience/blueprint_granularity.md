# CookBook Blueprint Granularity Experience

> Scope: CookBook only
> Global Protocol: ~/.ai-context/rules/blueprint_protocol.md
> Protocol Layer: Project-specific GC / experience
> Status: ACTIVE
> Initial Baseline: 2026-08-11

本文件不重新定义 `FULL/LITE`、`L1~LN`、`BLUEPRINT_READY`、`TURN`、`ROLE_CONTRACT`；以上全部继承 global `blueprint_protocol.md`。

## CookBook GC Baseline

以下条款均为存在性命题；不适用时必须在蓝图中写明 `N/A + 理由`。

### L1 — 决策与范围闭合

- `GC-CB-L1-01` REWORK 蓝图中存在 Verified Defect、Reopen Set、Preserve Set、Exact Repair、Regression Audit、STOP Gate。
- `GC-CB-L1-02` 蓝图中存在 IN SCOPE、OUT OF SCOPE、Allowed Files、Forbidden Files 或对应项 N/A + 理由。
- `GC-CB-L1-03` 涉及 FINAL/FROZEN 时，蓝图中存在 Frozen Matrix，逐项区分 Stable Identity、Contract、Lifecycle State、Acceptance Snapshot、Generated View。

### L2 — 证据闭合

- `GC-CB-L2-01` 每个非机械 ownership / migration / mapping 决策存在 Primary Evidence 字段。
- `GC-CB-L2-02` 使用代码路径、模块路径或 CodeMapping 作为证据时，蓝图中存在额外 ownership evidence；代码影响范围不是 sole ownership evidence。
- `GC-CB-L2-03` 涉及数量、覆盖率、ID 集合时，蓝图中存在 programmatic recount / audit command。
- `GC-CB-L2-04` source 未直接包含 Stable ID 时，蓝图中存在 semantic + formal responsibility + implementation evidence 的唯一责任链，或显式 blocker。

### L3 — 真相源闭合

- `GC-CB-L3-01` 涉及 Project Graph 时，蓝图中存在 Truth Hierarchy 或明确引用 frozen hierarchy：Runtime Truth、Project Truth、Decision Truth、Execution Extension、Handoff Context。
- `GC-CB-L3-02` Derived counts / acceptance snapshot 在蓝图中被显式标记为 Derived，不成为独立 Project Truth。
- `GC-CB-L3-03` SESSION / BLUEPRINT_STATE / Generated View 参与判断时，蓝图中存在 Truth Role 声明。
- `GC-CB-L3-04` 两个 source 冲突时，蓝图中存在 source priority 或 conflict disposition。

### L4 — 所有权与生命周期闭合

- `GC-CB-L4-01` 涉及 WorkItem status 时，蓝图中存在 implementation-state × verification-state 一致性检查。
- `GC-CB-L4-02` implementation complete 且 required Verification pending/fail/blocked 时，蓝图中存在 verifying 判定或例外理由。
- `GC-CB-L4-03` 蓝图中不存在 Plan completed → WorkItem done 的隐式推导；两者同时变化时存在独立 Closure Evidence。
- `GC-CB-L4-04` 涉及 Source Verification ID / Graph VerifyId 时，蓝图中存在 Identity Layer 声明及 1:1 mapping audit。
- `GC-CB-L4-05` CodeMapping 不作为 WorkItem Ownership sole evidence；蓝图中存在 formal responsibility evidence。

### L5 — 索引空间与集合投影闭合

- `GC-CB-L5-01` Source ID → Graph ID normalization 存在 deterministic mapping contract。
- `GC-CB-L5-02` 涉及 ID migration 时，存在 duplicate / dangling / collision audit。
- `GC-CB-L5-03` Generated View 中的实体集合存在 Project Graph source projection 定义，或明确 N/A。
- `GC-CB-L5-04` Feature/WorkItem/Verification counts 若出现在 ACCEPT 文档，标记 Derived Acceptance Snapshot。

### L6 — 用户可见副作用闭合

- `GC-CB-L6-01` 修改用户或 AI 直接读取的 Generated/Handoff View 时，蓝图中存在可见变化列表；完全不涉及时标 N/A + 理由。
- `GC-CB-L6-02` SESSION 角色发生变化时，蓝图中存在 Human/AI read behavior 和 Truth authority 声明。

### L7 — 脚本可勾销闭合

- `GC-CB-L7-01` Project Graph 治理批存在真实 test command + actual result。
- `GC-CB-L7-02` Project Graph 治理批存在 project_graph.py check + actual result。
- `GC-CB-L7-03` 涉及 ID / mapping / closure 时，存在 programmatic audit result。
- `GC-CB-L7-04` Validation Evidence 被持久化到正式 acceptance / reconciliation record，不能只存在于聊天报告。
- `GC-CB-L7-05` 蓝图存在明确 STOP Gate：Implementation → Tests → Commit → Push → STOP。

## Historical Lessons

### EXP-CB-001 — Manual Count Drift

Observed Pattern：人工统计 Verification 集合导致 source count 不闭合。

Lesson：涉及 ID 集合/覆盖率必须 programmatic recount。GC：`GC-CB-L2-03`、`GC-CB-L7-03`。

### EXP-CB-002 — Over-No-Guess

Observed Pattern：Source 未显式写 WorkItem ID，条目被统一判 blocked。

Lesson：没有显式 ID 不等于没有可证明 ownership；需要 semantic + formal responsibility + implementation evidence。GC：`GC-CB-L2-04`。

### EXP-CB-003 — Fuzzy Ownership

Observed Pattern：因 Schema、文件路径或 CodeMapping 相似而把 Verification 挂到错误 WorkItem。

Lesson：影响范围不等于责任所有权。GC：`GC-CB-L2-02`、`GC-CB-L4-05`。

### EXP-CB-004 — Implemented Work Marked Backlog

Observed Pattern：Runtime 已实现但 required device acceptance pending，WorkItem 仍是 backlog。

Lesson：implementation complete + required pending 默认必须 reconcile 为 verifying。GC：`GC-CB-L4-01`、`GC-CB-L4-02`。

### EXP-CB-005 — Identity Layer Confusion

Observed Pattern：Source Verification ID 与 Graph VerifyId 被误解释为 rename。

Lesson：存在两层 identity 时必须显式建模并做 1:1 audit。GC：`GC-CB-L4-04`、`GC-CB-L5-01`、`GC-CB-L5-02`。

### EXP-CB-006 — Broad Freeze Wording

Observed Pattern：FINAL ACCEPT 中使用“stable IDs and statuses frozen”，把 status contract 冻结错误扩大为当前 status value 永久冻结。

Lesson：Stable Identity / Contract / Lifecycle State / Acceptance Snapshot 必须分开声明。GC：`GC-CB-L1-03`、`GC-CB-L5-04`。

### EXP-CB-007 — REWORK Scope Expansion Risk

Observed Pattern：局部缺陷容易导致执行模型重新打开已接受决定。

Lesson：REWORK 必须显式 Reopen Set + Preserve Set。GC：`GC-CB-L1-01`。

## GC Feedback Registry

| GC ID | Theme | Origin | Recurrence Count | Review Required | Automation Candidate | Last Updated |
|---|---|---|---:|---|---|---|
| GC-CB-L1-01 ~ GC-CB-L7-05 | CookBook blueprint governance | EXP-CB-001 ~ EXP-CB-007 | 0 | No | No | 2026-08-11 |

首次形成规则不计作复发：Origin 是第一次形成 GC 的历史事故，Recurrence 是 GC 已存在后又发生同类错误。阈值继承 global protocol：Recurrence = 1 → Review Required；Recurrence >= 2 → Automation Candidate。
