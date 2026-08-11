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
