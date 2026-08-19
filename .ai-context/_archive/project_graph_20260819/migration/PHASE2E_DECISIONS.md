# Phase 2E Decisions — Cross-Reconcile

> 实施记录；Phase 2E 尚未获得外部架构 ACCEPT，不能替代 Accept Record。

## Truth hierarchy

```text
Runtime Truth          = code / DB / schema / runtime configuration
Project Truth          = Project Graph
Decision Truth         = formal Plan / Architecture Decision / Blueprint
Execution Extension    = BLUEPRINT_STATE CODE / ARCH / REVIEW
Handoff Context        = SESSION_交接
```

对于 Feature、WorkItem、Plan、Verification、Relation、CurrentWork，Project Graph 优先于 SESSION 摘要。SESSION 仅保留 handoff/context，不独立维护实体注册表或派生统计。`BLUEPRINT_STATE` 继续是 Cookbook execution extension，不进入 Core Schema。

## CurrentWork

已核对 `project.yaml.current`、SESSION 当前产品主线与 Graph WorkItem：

```yaml
feature: F-AI-MEAL
work_item: K1i
phase: verifying
blocker: ""
```

Phase 2E 是治理迁移阶段，不是产品 WorkItem；CurrentWork 不改。

## SESSION

SESSION 冻结为 `Handoff / Working Context View`。本轮移除 Phase 2D rework/等待审核的 stale 描述，将 Graph 统计与 Verification 详情改为指向 `PHASE2D_ACCEPT.md` / Phase 2E reconciliation ledger；不生成完整 SESSION view。

## BLUEPRINT_STATE

审计 K1g、K1i、K1a、L1 及当前 CODE/ARCH/REVIEW 链：未发现需要修正的 drift。Graph 的 `verifying` 与已完成 Plan、CODE/ARCH accepted 一致。其 `CODE/ARCH/REVIEW` 角色仍为 extension，未进入 Core Schema。

## Deferred Verification policy

69 条逐条保留 `source_status` 与最终 disposition：17 条 V2 已通过且不再绑定当前可执行 WorkItem，归为 `CLASSIFY_HISTORICAL_VERIFY`；52 条仍是当前权威清单中的 pending，缺少唯一 WorkItem 证据，归为 `RETAIN_DEFERRED_BLOCKED`，进入阻塞冲突台账。没有创建猜测性的 WorkItem 或 Verification。

## E-K1G-01

保留 Stable ID，`required: true`，状态改为 `not_required`，并补充明确 reason 与 Phase 2D/2E source refs。分类为 `ACCEPTED_LEGACY`，不再阻断当前 Closure。

## L3

保持 `primary = F-TOOLS` 与现有 6 条 `affects` 关系；本阶段关闭 Feature Split Candidate 为 `RESOLVED_NO_REGISTRY_CHANGE`。13 个 Feature Registry 不变。未来若形成独立 AI/Automation 平台，须另行 Architecture Change Proposal。

## Legacy View policy

Phase 2E 只识别并登记 legacy view drift，不手工重写 generated/human views；View 替换留给 Phase 3。SESSION 作为当前 handoff 文档是唯一允许在本轮清理的例外。

## R1 amendment

## R2 frozen decisions

### Verification Identity Layers

```text
Source Verification ID = authoritative checklist row identity.
Graph VerifyId = V1 Graph Verification entity identity.
Source ID is not renamed into Graph VerifyId; the ledger records a deterministic mapping.
```

### Source-to-Graph VerifyId Normalization

`F4-1 -> E-F4-01`, `P0-1 -> E-P0-01`, `D1 -> E-D-01`, `R6 -> E-R-06` mappings remain stable; IDs are not renamed, reused, or merged within either identity layer.

### WorkItem verifying semantic

```text
implementation proven complete + required acceptance pending/fail/blocked = verifying
```

`backlog` means implementation has not started. This round never auto-promotes `verifying` to `done`.

### Schema boundary

```text
Source Verification ID is migration/source identity.
Graph VerifyId is V1 graph identity.
No Core Schema field added in Phase 2.
```

### R2 ownership corrections

R1's K1d ownership for P0-2, P0-6, D11, F3-1, F3-2, and F3-3 is revoked. K1d is a not-yet-implemented cross-platform compatibility design; Schema/AI/JSON keywords are not ownership evidence. These six rows test the implemented AI meal parsing/preview path and map to K1g. D12, F5-1, and F5-2 remain K1g, supported by the meal specification and confirmation/commit path rather than CodeMapping alone.

K1f, BUG-AI-MEAL-002, and BUG-AI-MEAL-003 have implemented code with required device acceptance pending, so their status is `verifying`, not `backlog` or `done`.

初次 Phase 2E 结论中的 `52 RETAIN_DEFERRED_BLOCKED` 只是临时隔离状态，已由 R1 ownership evidence search 逐条替换：42 条映射既有 WorkItem，10 条由 3 个严格证据支持的 WorkItem 负责；`RETAIN_DEFERRED_BLOCKED=0`、`ARCHITECTURE_CHANGE_REQUIRED=0`。R1 详细证据见 `PHASE2E_VERIFICATION_RECONCILIATION.md`。
