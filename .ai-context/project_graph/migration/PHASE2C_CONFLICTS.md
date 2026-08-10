# Phase 2C Conflict Ledger

> 迁移冲突台账（遵循 No Guess Rule §77）。已解决标记 `RESOLVED`，未解决明列于"开放冲突"。
> 本阶段允许的冲突类型：`PLAN_SOURCE_MISSING` / `PLAN_SCOPE_UNCERTAIN` / `RELATION_UNCERTAIN` / `BLUEPRINT_STATE_DRIFT` / `ARCH_CHANGE_REQUIRED`。
>
> **交接注记（Phase 2C END · ACCEPT / CLOSED · Review Commit `ced5f13f`）**：本台账随 Phase 2C 外部架构审核 ACCEPT 收口；唯一开放项 `L3 FEATURE_SPLIT_CANDIDATE` 为允许的架构冲突，Follow-up Phase 2E（见 `PHASE2C_ACCEPT.md` §8）。Phase 2D 边界见 `PHASE2C_TO_2D_HANDOFF.md`。

## 已裁决冲突

### RESOLVED · KIND_ID_CONVENTION_REQUIRED（AI对话生成菜品/餐食）

```text
Convention: FEAT-<FEATURE>-NNN
实际 ID  : FEAT-AI-MEAL-001（primary F-AI-MEAL，backlog）
来源     : .ai-context/docs/feature/待办_功能算法.md + AI对话生成菜品餐食方案.md
关系     : affects F-DISH / F-MEAL
```

- 状态：`RESOLVED_IN_PHASE_2C`。

### RESOLVED · KIND_ID_CONVENTION_REQUIRED（放开AI推荐限制）

```text
Convention: FEAT-<FEATURE>-NNN
实际 ID  : FEAT-RECOMMEND-001（primary F-RECOMMEND，parked 用户暂缓）
来源     : .ai-context/docs/feature/待办_功能算法.md
```

- 状态：`RESOLVED_IN_PHASE_2C`。

### RESOLVED · RELATION_PENDING_2C（K15 / I7）

```text
实施蓝图 §49 裁决: work:K15 related_to work:I7（两个 Stable ID 保持独立）
理由: both concern AI failure/fallback chain, but K15 additionally covers segmentation/token truncation/control。
不使用 supersedes / depends_on。
声明位置: features/F-AI-MEAL.yaml relations
```

- 状态：RESOLVED（2C 已建 related_to）。

### RESOLVED · DUPLICATE_RELATION_PENDING_2C（J22 / L2）

```text
实施蓝图 §51 裁决: work:J22 related_to work:L2（默认；无明确 supersedes evidence）
理由: 语义高度重叠但不确认 replacement direction。
声明位置: features/F-HEALTH.yaml relations
```

- 状态：RESOLVED（2C 已建 related_to）。

### RESOLVED · L3 FEATURE_SPLIT_CANDIDATE 跨 Feature 影响

```text
L3 Primary: F-TOOLS（保持）
L3 affects F-INGREDIENT / F-DISH / F-MEAL / F-WEEKPLAN / F-PANTRY / F-NUTRITION（6 条，源证据 = 待办_战略会商.md#L3）
声明位置: features/F-TOOLS.yaml relations
```

- 状态：RELATION RESOLVED（2C 已建 affects）；`FEATURE_SPLIT_CANDIDATE` 本身继续保留（Follow-up Phase 2E 架构复核），不属本阶段可解。

### 核查无冲突 · BLUEPRINT_STATE Extension

```text
project.yaml extensions.blueprint_state（roles=CODE/ARCH/REVIEW、turn=USER、current_batch=L1+K1i）
与 docs/context_memory/BLUEPRINT_STATE.md 当前内容一致 → 未发现漂移。
```

- 状态：NO_DRIFT（无 BLUEPRINT_STATE_DRIFT 记录）。

### 核查无冲突 · Plan Source 判定

```text
PLAN-AI-NDJSON / PLAN-K1I / PLAN-K1A / PLAN-L1 均有正式 Blueprint（见 PHASE2C_PLAN_INVENTORY.md）。
K1b 蓝图 DRAFT·PARKED（非正式可实施 Plan）→ 不创建 PLAN-K1B（NOT CREATED，非冲突，见 Inventory）。
其余 WorkItem 无满足 §28 判据的正式 Plan → 不迁移。
无 PLAN_SOURCE_MISSING / PLAN_SCOPE_UNCERTAIN 遗留。
```

- 状态：NO_PLAN_SOURCE_CONFLICT。

## 开放冲突

```text
FEATURE_SPLIT_CANDIDATE : L3（临时 F-TOOLS，Phase 2E 架构复核）
```

> 唯一开放项为允许的架构冲突（Phase 2E follow-up），不污染 Stable ID、有 temporary safe representation。Source Coverage 冲突仍为 0；KIND_ID_CONVENTION_REQUIRED 两项已在本阶段解决。

---

*Phase 2C Conflict Ledger · 2026-08-10。*
