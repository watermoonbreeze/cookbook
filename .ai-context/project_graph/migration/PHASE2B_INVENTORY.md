# Phase 2B Migration Inventory

> **Migration working record（迁移工作记录）——不是 Project Truth。**
> 供 Phase 2B 施工与后续 2B END 审计使用；不得作为未来 AI 的 Project Truth 默认来源。
> Truth Source Priority 见 Handoff §5 / 实施蓝图 §13。
>
> **状态：`RECONCILED`**（2026-08-10 · Phase 2B Rework）

## 范围

- **扫描源**：`待办索引.md` / `待办_Bug修复.md` / `待办_功能算法.md` / `待办_UI交互.md` / `待办_数据健康.md` / `待办_工程合规.md` / `待办_战略会商.md` / `待办总览.md` / `工程优化待办.md` / `UX深挖审计与待办.md` + `SESSION_交接.md` + `BLUEPRINT_STATE.md`。
- **迁移原则**：CURRENT PROJECT STATE。迁移「当前未完成（backlog~parked）+ 最近完成主线但仍解释现状（verifying）+ 被 Plan/Verify/CurrentWork 引用」；历史完成且无引用 → `SKIP_HISTORY`。

> ⚠️ **旧规则废弃（Rework §53）**：~~「低优先级 → 不迁」~~、~~「二期规划 → 不迁」~~ 已删除。
> 现规则：**Low Priority: does not affect migration eligibility**；**Phase-2/Future Planned: migrate as backlog/parked according to actual state**。
> 100% disposition 由 `PHASE2B_SOURCE_COVERAGE.md` 证明（UNEXPLAINED = 0）。

## Reconciliation 摘要

```text
Original Migration:
52 WorkItems

Reconciliation（Rework）:
+ 恢复 Stable ID        : FAM-AGE, FAM-MEAL, K15, J22（I7/L2 不再吞并）
+ 移除错误替代 ID       : TODO-FAMILY-002, TODO-FAMILY-003（已恢复为 FAM-AGE/FAM-MEAL）
+ 修正 kind             : TODO-* 全部 → kind: todo（原误标 feature）
+ 修正状态              : K1c: backlog → in_progress（源 🔄）
+ 修正 Feature ownership : L3: F-AI-MEAL → F-TOOLS（临时 primary；FEATURE_SPLIT_CANDIDATE）
+ 补齐 Source Coverage  : 从 ~52 → 101 WorkItems（含 39 个既有 Stable ID + 62 个新增迁移）
+ 明确 Defer            : 推演类接入AI增强、P5 Shared VM（DEFER_WITH_REASON）
+ 记录 KIND 约定待定    : AI对话生成菜品/餐食、放开AI推荐限制（KIND_ID_CONVENTION_REQUIRED）
```

## 迁移映射（按 Feature）

> 说明：新 ID 均按 §19.1 格式 `KIND-<FEATURE>-NNN`；编号从 001 起。**Stable ID（I/J/K/L/FAM/U/AF-*）一律保留原名，禁止重编号。**
> 完整逐条 disposition 见 `PHASE2B_SOURCE_COVERAGE.md`（本 Inventory 只列关键差异与计数）。

### F-AI-MEAL

| ID | kind | status | 说明 |
|---|---|---|---|
| K1g / K1i / K1b / K1a | feature | verifying / parked | 既有（SESSION 状态优先） |
| K1c | feature | **in_progress** | **状态修正（Rework）** |
| K1d / K1e | feature | backlog / cancelled | 既有 |
| I3-I8 | bug | backlog | 既有；I7 不再吞并 K15 |
| **K15** | feature | backlog | **恢复独立（Rework）** |
| BUG-AI-MEAL-001 | bug | backlog | K2 语音 |
| TODO-AI-MEAL-002 / 003 / 004 | todo | backlog | AI S4 端侧 / U5 连接测试 / AI营养补全 |

### F-TOOLS

| ID | kind | status | 说明 |
|---|---|---|---|
| L1 | compliance | verifying | 既有 |
| J19 / J16 / K2 | research/feature/todo | backlog | 既有 |
| **L3** | feature | backlog | **从 F-AI-MEAL 迁入（Rework）；FEATURE_SPLIT_CANDIDATE 记 Conflict** |
| I-Mine / I-About / U1-U5 | todo | backlog | **恢复 Stable ID（Rework）** |
| TODO-TOOLS-006~018 | todo | backlog/parked | 工程/性能/战略/UX 聚合新迁 |

### F-FAMILY

| ID | kind | status | 说明 |
|---|---|---|---|
| BUG-FAMILY-001 / 002 | bug | backlog | 既有 |
| TODO-FAMILY-001 | todo | backlog | kind 修正（原 feature） |
| **FAM-AGE / FAM-MEAL** | feature | backlog | **恢复 Stable ID（Rework）** |
| U4 / TODO-FAMILY-005 | todo | backlog | 临时成员 / 饭量模型 |

### F-HEALTH

| ID | kind | status | 说明 |
|---|---|---|---|
| L2 | feature | backlog | 既有；不再吞并 J22 |
| **J22** | feature | backlog | **恢复独立（Rework）** |
| J10 / J14 | feature | backlog | 既有 |
| TODO-HEALTH-001 / 002 | todo | backlog | 忌口补漏 / 生命阶段 |

### F-NUTRITION / F-RECOMMEND / F-INGREDIENT / F-WEEKPLAN / F-MEAL / F-PANTRY / F-TIMELINE / F-DISH / F-SYNC

- 既有 Stable ID（J2/J6/J13/J17/K9-K14/J3/J19/L4/J4/J5/J7/K13/J20/J21 等）全部保留。
- 新增迁移见 `PHASE2B_SOURCE_COVERAGE.md`（S3/S5/S7/S8/S11 明细）。
- 关键：`TODO-NUTRITION-001/002`、`TODO-INGREDIENT-001`、`TODO-RECOMMEND-001`、`TODO-FAMILY-001` 的 kind 均修正为 `todo`（原误标 feature）。

## 自检统计（Inventory 内部自检）

```text
Candidate count        : 104（含既有 Graph + 本轮恢复 + 新增迁移 + 记录 skipped/deferred 若干）
Restored Stable IDs    : 4（FAM-AGE, FAM-MEAL, K15, J22）
Removed wrong IDs      : 2（TODO-FAMILY-002, TODO-FAMILY-003）
Duplicate count        : 0（双 Stable ID 重复项 K15/I7、J22/L2 均 KEEP BOTH，关系待 2C）
Skip history count     : 若干（K1h/K1f/AG-REVIEW 等已完成且无当前引用 → SKIP_HISTORY）
Conflict count         : 更新至 PHASE2B_CONFLICTS.md（见其"开放冲突"）
New ID count           : 62（BUG-*/TODO-*/REFACTOR-*，kind 与 prefix 全部一致）
Existing ID count      : 39（I*/J*/K*/L*/FAM-*/U*/I-Mine/I-About 等 Stable ID 全保留）
Deferred               : 2（推演类接入AI增强 / P5 Shared VM，DEFER_WITH_REASON）
KIND_ID_CONVENTION     : 2（AI对话生成菜品 / 放开AI推荐限制，暂不迁移）
```

---

*Phase 2B Inventory · 2026-08-10（Rework）· 100% Source Coverage 见 `PHASE2B_SOURCE_COVERAGE.md`。*
