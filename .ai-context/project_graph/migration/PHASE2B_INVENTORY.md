# Phase 2B Migration Inventory

> **Migration working record（迁移工作记录）——不是 Project Truth。**
> 供 Phase 2B 施工与后续 2B END 审计使用；不得作为未来 AI 的 Project Truth 默认来源。
> Truth Source Priority 见 Handoff §5 / 实施蓝图 §13。

## 范围

- **扫描源**：`待办索引.md` / `待办_Bug修复.md` / `待办_功能算法.md` / `待办_UI交互.md` / `待办_数据健康.md` / `待办_工程合规.md` / `待办_战略会商.md` + `SESSION_交接.md` + `BLUEPRINT_STATE.md`。
- **迁移原则**：CURRENT PROJECT STATE。迁移「当前未完成（backlog~parked）+ 最近完成主线但仍解释现状（verifying）+ 被 Plan/Verify/CurrentWork 引用」；历史完成且无引用 → `SKIP_HISTORY`；二期规划（📌）/ 低优先级 → 不迁。

## 自检统计（Inventory 内部自检，§31）

```text
Candidate count        : 56（含现有 Graph 4 + 去重后待迁 48 + 记录 skipped 若干）
Duplicate count        : 4（I7+K15、L2+J22、K1d 双文件、B1-B6 并入 K1g）
Skip history count     : K1f/K1h/AG-REVIEW 等已完成且无当前引用 → SKIP_HISTORY
Conflict count         : 8（见 PHASE2B_CONFLICTS.md，多为已裁决 STATE_CONFLICT / FEATURE_OWNERSHIP）
New ID count           : 12（BUG-*/TODO-*/REFACTOR-*，见下）
Existing ID count      : 36（I3~I8、J2~J21、K1a/K1c/K1d/K1e/K9~K14、L1~L4、FAM-* 经新 ID 落地）
```

## 迁移映射（按 Feature）

> 说明：新 ID 均按 §19.1 格式 `KIND-<FEATURE>-NNN`；编号从 001 起（Graph 中无同类型同 Feature 既有 ID）。

### F-AI-MEAL（已有 K1g/K1i/K1b/I8，新增 11）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 功能算法 K1a + SESSION | K1a | feature | verifying | MIGRATE | K1a |
| 功能算法 K1c | K1c | feature | backlog | MIGRATE | K1c |
| 功能算法/工程合规 K1d | K1d | feature | backlog | MIGRATE（双文件同一项） | K1d |
| 功能算法 K1e | K1e | feature | cancelled | MIGRATE（DISCARDED 落 cancelled） | K1e |
| Bug修复 I3 | I3 | bug | backlog | MIGRATE | I3 |
| Bug修复 I4 | I4 | bug | backlog | MIGRATE | I4 |
| Bug修复 I5 | I5 | bug | backlog | MIGRATE | I5 |
| Bug修复 I6 | I6 | bug | backlog | MIGRATE | I6 |
| Bug修复 I7 + 功能算法 K15 | I7 | bug | backlog | MIGRATE（合并 K15） | I7 |
| Bug修复 K2语音 | — | bug | backlog | MIGRATE（新 ID） | BUG-AI-MEAL-001 |
| 战略 L3 | L3 | feature | backlog | MIGRATE | L3 |
| 功能算法 K1f / K1h / AG-REVIEW | — | — | — | SKIP_HISTORY（已完成无当前引用） | — |

### F-TIMELINE（新增 2）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| UI交互 J4 | J4 | feature | backlog | MIGRATE | J4 |
| Bug修复 J5 | J5 | feature | backlog | MIGRATE | J5 |

### F-INGREDIENT（新增 6）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| Bug修复 添加食材清空预填 | — | bug | backlog | MIGRATE（新 ID） | BUG-INGREDIENT-001 |
| 功能算法 K14 | K14 | feature | backlog | MIGRATE | K14 |
| UI交互 J9 | J9 | feature | backlog | MIGRATE | J9 |
| UI交互 J11 | J11 | feature | backlog | MIGRATE | J11 |
| 数据健康 J20 | J20 | bug | backlog | MIGRATE | J20 |
| 数据健康 食材库扩充阶段2 | — | feature | in_progress | MIGRATE（新 ID） | TODO-INGREDIENT-001 |

### F-DISH（新增 2）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 功能算法 编辑页统一 | — | refactor | backlog | MIGRATE（新 ID） | REFACTOR-DISH-001 |
| 战略 L4 | L4 | feature | ready | MIGRATE | L4 |

### F-PANTRY（新增 2）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 功能算法 K13 | K13 | feature | backlog | MIGRATE | K13 |
| 功能算法 J7 | J7 | feature | backlog | MIGRATE | J7 |

### F-RECOMMEND（新增 5）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 功能算法 K9~K12 | K9/K10/K11/K12 | feature | backlog | MIGRATE | K9~K12 |
| UI交互 首页推荐下一餐 v2 | — | feature | backlog | MIGRATE（新 ID） | TODO-RECOMMEND-001 |

### F-NUTRITION（新增 7）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 功能算法 J2 | J2 | feature | backlog | MIGRATE | J2 |
| UI交互 J21 | J21 | feature | backlog | MIGRATE | J21 |
| UI交互 J6 | J6 | todo | backlog | MIGRATE | J6 |
| 战略 J3 | J3 | research | backlog | MIGRATE | J3 |
| 数据健康 J13 | J13 | maintenance | backlog | MIGRATE | J13 |
| UI交互 营养走势折线 | — | feature | backlog | MIGRATE（新 ID） | TODO-NUTRITION-001 |
| 数据健康 营养表分页 | — | feature | backlog | MIGRATE（新 ID） | TODO-NUTRITION-002 |

### F-HEALTH（新增 3）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 工程合规 L2 + 数据健康 J22 | L2 | feature | backlog | MIGRATE（合并 J22） | L2 |
| UI交互 J10 | J10 | feature | backlog | MIGRATE | J10 |
| 功能算法 J14 | J14 | feature | backlog | MIGRATE | J14 |

### F-FAMILY（新增 5）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| Bug修复 成员不在场→今日卡消失 | — | bug | backlog | MIGRATE（新 ID） | BUG-FAMILY-001 |
| Bug修复 缺席微调 | — | bug | backlog | MIGRATE（新 ID） | BUG-FAMILY-002 |
| UI交互 成员>4滑块 | — | feature | backlog | MIGRATE（新 ID） | TODO-FAMILY-001 |
| 索引 FAM-AGE | — | feature | backlog | MIGRATE（新 ID） | TODO-FAMILY-002 |
| 索引 FAM-MEAL | — | feature | backlog | MIGRATE（新 ID） | TODO-FAMILY-003 |

### F-WEEKPLAN（新增 1）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 功能算法 J17 | J17 | feature | backlog | MIGRATE | J17 |

### F-TOOLS（新增 4）

| source | existing_id | kind | status | action | target_id |
|---|---|---|---|---|---|
| 工程合规 L1 + SESSION | L1 | compliance | verifying | MIGRATE（ACCEPTED+真机 pending） | L1 |
| 战略 J19 | J19 | research | backlog | MIGRATE | J19 |
| UI交互 J16 | J16 | feature | backlog | MIGRATE | J16 |
| UI交互 K2 | K2 | todo | backlog | MIGRATE | K2 |

### F-SYNC（0 新增）

双设备同步/备份为已归档完成项，无当前未完成 WorkItem → 不迁。

---

*Phase 2B Inventory · 2026-08-10。施工顺序见 实施蓝图 §32（F-MEAL→…→F-TOOLS）。*
