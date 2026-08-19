# Phase 2D → Phase 2E Handoff

> Phase 2D 已 `ACCEPT / CLOSED`。本文件是 Phase 2E 的强制入口；Graph 继续保持 `mode: draft`。

## Required inputs

读取：

```text
PHASE1_FINAL_ACCEPT.md
PHASE2A_ACCEPT.md
PHASE2B_ACCEPT.md
PHASE2C_ACCEPT.md
PHASE2D_ACCEPT.md
PHASE2A_TO_2B_HANDOFF.md
PHASE2B_TO_2C_HANDOFF.md
PHASE2C_TO_2D_HANDOFF.md
PHASE2D_TO_2E_HANDOFF.md
project.yaml
SESSION_交接.md
BLUEPRINT_STATE.md
PHASE2B_CONFLICTS.md
PHASE2C_CONFLICTS.md
PHASE2D_CONFLICTS.md
```

## Phase 2E scope

```text
Cross-Reconcile + Bootstrap Freeze
```

需要将 Feature、WorkItem、Plan、Verification、CurrentWork、SESSION、BLUEPRINT_STATE 与 migration conflict ledgers 放在同一份 Project Truth 上核对。Phase 2E 不是新一轮批量迁移，不得默认创建大量 WorkItem 或 Verification。

### CurrentWork

比较 `project.yaml.current`、SESSION 与 BLUEPRINT_STATE 的当前事实；不能机械地令任一文件单方面胜出。完成后，`project.yaml.current` 应成为当前 Project Truth 的 CurrentWork。当前交接明确 `0 changes`，本项留给 Phase 2E 正式处理。

### SESSION / BLUEPRINT_STATE

- SESSION 继续是 Transitional Current-State / Handoff View，不是长期 Project Truth；Phase 2E 只清理冲突、已关闭 blocker、过期阶段描述、重复 derived truth 与验证数量快照。
- BLUEPRINT_STATE 的 `CODE/ARCH/REVIEW` 继续是 Cookbook Extension，不得写入 Core WorkItem Schema；Phase 2E 检查其与 Graph WorkItem/Plan 的 drift。

### Deferred and legacy items

- 69 条 deferred 必须逐条处理：可映射到已有 WorkItem、在严格证据下创建缺失 WorkItem、保留并写明原因、归类 legacy 或 non-current。
- 17 条 deferred pass 必须保持 pass。
- `E-K1G-01` 只能在 2E 最终裁决为 legacy / not_required / superseded 等安全表示，2D END 不得删除。

### Architecture and conflicts

- 评估 `L3 FEATURE_SPLIT_CANDIDATE` 是否继续以 `F-TOOLS` 为 temporary primary，或提出 `FEATURE_REGISTRY_CHANGE_PROPOSAL`；未经 Architecture Review 不得新增 Feature。
- 扫描 PHASE2B/2C/2D conflict ledger，每项最终归类为 `RESOLVED`、`DEFERRED_TO_PHASE_3+`、`ACCEPTED_LEGACY` 或 `ARCHITECTURE_CHANGE_REQUIRED`，不得保留 unknown/forgotten。
- 识别 Legacy View Drift；Phase 2E 不重写 generated views，替换/生成留给 Phase 3。

## Explicit exclusions

Phase 2E 不实现 AI_INDEX、功能路径/07/Todo/Bug/Verify view generator、derived activity/health、Graph active、pg lifecycle、CI guard，也不改 schema、validator 或生产代码，除非另有正式架构决策。

## Target state

```text
Phase 1  FINAL ACCEPT / FROZEN
Phase 2A ACCEPT / CLOSED
Phase 2B ACCEPT / CLOSED
Phase 2C ACCEPT / CLOSED
Phase 2D ACCEPT / CLOSED
Phase 2E AUTHORIZED / NOT STARTED
Graph Mode draft
```
