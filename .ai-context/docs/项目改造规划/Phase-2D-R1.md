[toc]

# Project Graph Phase 2D R1 Reconciliation 实施蓝图

## PG-P2D-R1 · Verification Coverage Repair + CFG Ownership Recheck + SESSION Transitional Contract

**任务类型**：Phase 2D 小范围返工 / Truth Reconciliation
**推荐执行模型**：GPT-5.6 Luna
**推荐 Reasoning**：Medium
**审核基准提交**：`fb378da412c0688cd89a5f05c71605748f46c2e5`
**Phase 2D 当前审核结论**：`REWORK`
**Phase 2E**：`NOT STARTED`
**Graph Mode**：保持 `draft`

---

# 0. 本轮目标

本轮不是重做 Phase 2D。

保留已经正确的 Verification Graph 主体，只修：

```text
R1
Source Verification 数量错误

R2
Deferred 数量错误

R3
SESSION 中错误 98/98

R4
E-CFG-01..06 → WorkItem ownership 证据不足

R5
E-K1G-01 legacy aggregate stale semantic

R6
冻结 SESSION Transitional Contract
```

完成后：

```text
Phase 2D
RECONCILED
WAITING FOR ARCHITECTURE REVIEW
```

不得写 ACCEPT。

---

# 1. 基线检查

执行：

```bash
git status
git rev-parse HEAD
git log --oneline -8
```

当前 HEAD 应至少包含：

```text
fb378da412c0688cd89a5f05c71605748f46c2e5
```

不要 reset / clean / checkout 用户修改。

---

# 2. 必须读取

```text
.ai-context/project_graph/migration/PHASE2C_TO_2D_HANDOFF.md

.ai-context/project_graph/migration/PHASE2D_INVENTORY.md

.ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md

.ai-context/project_graph/migration/PHASE2D_CONFLICTS.md

.ai-context/project_graph/features/F-AI-MEAL.yaml

.ai-context/project_graph/features/F-TOOLS.yaml

.ai-context/docs/context_memory/SESSION_交接.md

.ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

以及当前 Phase 2D 已选定的：

> AUTHORITATIVE_VERIFICATION_CHECKLIST

---

# ============================================================

# PART A — SOURCE COVERAGE RECOUNT

# ============================================================

# 3. 不使用现有 98 作为输入

必须重新从权威 Checklist 枚举。

不得：

```text
读取 PHASE2D_SOURCE_COVERAGE.md 的统计
→ 当作 Source Truth
```

Source Truth 是：

> 权威真机 Checklist 本身。

---

# 4. 重新计算 Verification Rows

已知审核发现：

```text
pending = 97
pass    = 17
```

但这两个数字只能作为：

> 审核锚点。

执行时仍必须从 Checklist 自己重新计数验证。

期望核实结果：

```text
Verification Rows = 114
```

如果实际重新枚举结果不是 114：

不得为了迎合审核手工改成 114。

必须报告：

```text
SOURCE_RECOUNT_MISMATCH
```

并给出实际行列表。

---

# 5. 重新计算现有 disposition 数量

当前已知 Graph 施工结果：

```text
MIGRATE_VERIFY = 43
UPDATE_EXISTING_VERIFY = 2
```

必须重新核实这 45 条实际 ID。

---

# 6. Deferred 必须按真实 ID 列表重新计数

当前 Coverage 中 Deferred 列表应逐项程序化计数。

审核发现当前实际列表为：

```text
69
```

不得人工写数字。

---

# 7. 正确数学 Gate

重新计算后必须满足：

```text
VERIFICATION_ROWS
=
KEEP_EXISTING_VERIFY
+
MIGRATE_VERIFY
+
UPDATE_EXISTING_VERIFY
+
DEFER_VERIFY_UNMAPPED
```

当前预期：

```text
114
=
0
+
43
+
2
+
69
```

如果 KEEP 实际非 0：

使用真实结果。

---

# 8. V2 17 条 PASS 不得消失

V2-1..V2-17 即使：

```text
DEFER_VERIFY_UNMAPPED
```

仍必须保留：

```text
SOURCE STATUS = pass
```

因为：

```text
Graph representation deferred
≠
verification result unknown
```

---

# 9. Deferred 状态统计

必须额外统计：

```text
Deferred source status:

pass:
<n>

pending:
<n>

fail:
<n>

blocked:
<n>

not_required:
<n>
```

当前审核锚点：

```text
pass = 17
pending = 52
```

但必须程序化重算。

---

# 10. 更新 PHASE2D_SOURCE_COVERAGE.md

修正：

```text
VERIFICATION_ROWS
DEFER_VERIFY_UNMAPPED
TOTAL
```

以及所有 Derived Summary。

必须：

```text
UNEXPLAINED = 0
```

---

# 11. 更新 PHASE2D_INVENTORY.md

旧：

```text
Actionable source rows = 98
Historical rows without unique WorkItem evidence = 53
```

不得继续作为 Current Final Statistics。

改成实际重算结果。

旧统计如保留历史：

标记：

```text
SUPERSEDED BY R1 RECOUNT
```

---

# ============================================================

# PART B — E-CFG OWNERSHIP RECHECK

# ============================================================

# 12. 不预设 E-CFG 属于 K1g

重新读取：

```text
E-CFG-01
...
E-CFG-06
```

对应的权威 Verification source。

然后读取 CFG 的正式实施蓝图 / 交付记录。

---

# 13. 必须回答

对 `E-CFG-01..06`：

```text
它们在验证哪个 Stable WorkItem？
```

只能根据正式证据判断。

---

# 14. WorkItem mapping priority

```text
1. 正式蓝图明确 WorkItem ID
2. 专项台账明确 Stable ID
3. Checklist section 明确 Stable ID
4. Graph + source history 能唯一证明
```

---

# 15. 三种允许结果

### A. 唯一证明属于 K1a

则：

```text
E-CFG-01..06
work_item = K1a
```

并迁到 K1a 所在 F-AI-MEAL shard。

---

### B. 唯一证明属于 K1g

保持：

```text
work_item = K1g
```

但必须在 Coverage 中记录具体 Mapping Evidence。

不能只写：

```text
same AI meal mainline
```

---

### C. 无法唯一证明

则：

```text
DEFER_VERIFY_UNMAPPED
reason:
WORKITEM_UNMAPPED
```

并从 Graph 中移除这 6 个本阶段新迁的 Verification Entity。

注意：

> 这是本阶段新迁实体，不是 Stable ID 被删除。

Stable ID 仍存在于 Source Coverage。

---

# 16. 禁止第四种做法

不得：

```text
为了少改 YAML
→ 继续挂 K1g
```

也不得：

```text
觉得 K1a 比较像
→ 改 K1a
```

必须有证据。

---

# 17. 更新 Conflict Ledger

如果 ownership 确认：

记录：

```text
CFG_WORKITEM_MAPPING
RESOLVED
```

包括证据。

如果无法确认：

记录：

```text
WORKITEM_UNMAPPED
E-CFG-01..06
```

---

# ============================================================

# PART C — E-K1G-01 LEGACY AGGREGATE

# ============================================================

# 18. 不删除 E-K1G-01

当前：

```text
E-K1G-01
```

是已有 Stable Verification ID。

本轮禁止直接删除。

---

# 19. 明确标记它不属于当前 authoritative checklist

在：

```text
PHASE2D_CONFLICTS.md
```

记录：

```text
EXISTING_VERIFY_NOT_IN_CURRENT_CHECKLIST

verification:
E-K1G-01

classification:
LEGACY_AGGREGATE

follow_up:
Phase 2E
```

---

# 20. stale reason 修正

如果 `E-K1G-01.reason` 当前明确把：

```text
E-K1A-01
```

错误归入 K1g aggregate：

允许做最小语义修正。

但只能写成不再错误归属的 legacy 描述，例如：

```text
历史 K1g 聚合验证节点；当前权威真机清单已拆分为独立稳定 Verification，
该聚合节点不再作为当前 Verification Coverage 统计来源。
```

---

# 21. 不改变它的 Identity

保持：

```text
id = E-K1G-01
work_item = K1g
```

除非 Phase 2E 后续正式决定 supersede/deprecate。

---

# ============================================================

# PART D — SESSION ERROR REPAIR

# ============================================================

# 22. SESSION 本轮只修事实错误

当前：

```text
Source Coverage 98/98
```

必须删除或修正。

---

# 23. 推荐 SESSION 表达

不要继续硬编码大段 Derived Statistics。

改成：

```text
Phase 2D:
REWORK / reconciliation in progress

Verification Source Coverage:
see .ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md
```

如果确实需要数字：

必须写：

```text
Snapshot (derived from authoritative checklist):
114 verification rows
```

并明确：

```text
derived / snapshot
```

---

# ============================================================

# SESSION TRANSITIONAL CONTRACT

# ============================================================

# 24. 本轮冻结过渡规范，但不重构 SESSION

在：

```text
SESSION_交接.md
```

加入一个简洁 Governance Note。

不要大规模改章节结构。

---

# 25. SESSION 当前角色

写清：

```text
SESSION_交接.md
=
Transitional Current-State / Handoff Document
```

它目前仍用于 AI 接手。

但：

```text
NOT long-term Project Truth
```

---

# 26. SESSION 可以记录

只记录：

```text
当前主线

当前 WorkItem

当前阶段

最近阶段审核状态

当前 Blocker

下一步动作

关键 Truth Source 指针

必要短期上下文
```

---

# 27. SESSION 不应独立维护

禁止把以下作为手工独立 Truth：

```text
WorkItem 总数

Verification 总数

pending/pass 数量

Plan 总数

Feature 数量

完整 Stable ID Registry
```

如果需要展示：

必须注明：

```text
snapshot / derived from <source>
```

---

# 28. SESSION 不得重新定义 Stable ID

例如 SESSION 不得定义：

```text
E-K1I-02 = ...
```

如果和正式 Checklist / Graph 不一致。

Identity Truth 由：

```text
Project Graph
formal blueprint
authoritative verification source
```

承担。

---

# 29. SESSION 不得覆盖 Frozen Project Graph Decisions

包括但不限于：

```text
Feature Universe

Plan lifecycle

Stable IDs

Relation semantics

Verification Closure

Observed vs Verification

FEAT-* convention
```

---

# 30. SESSION 长期重构 deferred

在 Governance Note 中明确：

```text
Structural SESSION redesign:
Phase 2E defines reconciliation role
Phase 3 converts it toward a thin/generated handoff view
```

本轮：

> 不重构。

---

# ============================================================

# PART E — CLOSURE RECHECK

# ============================================================

# 31. E-CFG 调整后重新 Closure Audit

扫描全 Graph：

```text
status = done
```

确认 required Verification Closure 合法。

---

# 32. 不自动 verifying → done

继续：

```text
0
```

---

# 33. 如果 E-CFG 被 Deferred

这不会自动改变：

```text
K1a
K1g
```

当前 WorkItem status。

它们仍按 Phase 2B ACCEPT / 当前 Truth 保持。

---

# ============================================================

# DOCUMENT STATUS

# ============================================================

# 34. README

保持：

```text
Phase 2D
IMPLEMENTED / WAITING FOR ARCHITECTURE REVIEW
```

或改成更准确：

```text
Phase 2D
RECONCILED / WAITING FOR ARCHITECTURE REVIEW
```

禁止：

```text
ACCEPT
CLOSED
```

---

# 35. project.yaml

```yaml
mode: draft
```

不变。

CurrentWork 不变。

---

# ============================================================

# VALIDATION

# ============================================================

# 36. Programmatic count requirement

本轮对以下数字：

```text
Verification Rows
Deferred
Pass
Pending
Graph Verification Count
```

必须通过脚本 / parser 计算。

不要人工数 range。

临时脚本：

> 可以使用，不提交。

---

# 37. Tests

运行完整 Project Graph tests。

要求：

```text
FAIL = 0
```

---

# 38. pg check

```bash
python .ai-context/project_graph/tools/project_graph.py check
```

要求：

```text
PASS
0 issue
```

---

# ============================================================

# R1 GATES

# ============================================================

# 39. GATE-R1-01

Authoritative checklist 未改变。

---

# 40. GATE-R1-02

Verification Rows 已程序化重算。

---

# 41. GATE-R1-03

Coverage 数学完全自洽。

---

# 42. GATE-R1-04

UNEXPLAINED = 0。

---

# 43. GATE-R1-05

Deferred 列表数量与统计一致。

---

# 44. GATE-R1-06

V2-1..17 仍记录 source status = pass。

---

# 45. GATE-R1-07

Deferred pass 未从统计消失。

---

# 46. GATE-R1-08

SESSION 不再包含错误 `98/98`。

---

# 47. GATE-R1-09

SESSION Transitional Contract 已记录。

---

# 48. GATE-R1-10

SESSION 未进行结构性重构。

---

# 49. GATE-R1-11

E-CFG-01..06 ownership 有明确证据或全部 defer。

---

# 50. GATE-R1-12

不存在“猜测式 CFG mapping”。

---

# 51. GATE-R1-13

E-K1G-01 未删除。

---

# 52. GATE-R1-14

E-K1G-01 被标为 legacy/current-checklist orphan。

---

# 53. GATE-R1-15

E-K1G-01 不再错误聚合 K1a Verification。

---

# 54. GATE-R1-16

Stable Verification ID loss = 0。

---

# 55. GATE-R1-17

WorkItem statuses 未大规模改动。

---

# 56. GATE-R1-18

Auto verifying → done = 0。

---

# 57. GATE-R1-19

Plan status 0 修改。

---

# 58. GATE-R1-20

CurrentWork 0 修改。

---

# 59. GATE-R1-21

Schema 0 修改。

---

# 60. GATE-R1-22

Validator Contract 0 修改。

---

# 61. GATE-R1-23

Production code 0 修改。

---

# 62. GATE-R1-24

Graph mode = draft。

---

# 63. GATE-R1-25

Tests PASS。

---

# 64. GATE-R1-26

pg check PASS / 0 issue。

---

# 65. GATE-R1-27

Phase 2E NOT STARTED。

---

# ============================================================

# DIFF BOUNDARY

# ============================================================

# 66. 允许修改

```text
.ai-context/project_graph/features/F-AI-MEAL.yaml
  仅 E-CFG / E-K1G-01 必要调整

.ai-context/project_graph/migration/PHASE2D_INVENTORY.md

.ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md

.ai-context/project_graph/migration/PHASE2D_CONFLICTS.md

.ai-context/project_graph/README.md
  仅状态文字如有必要

.ai-context/docs/context_memory/SESSION_交接.md
  仅修错误数据 + Transitional Contract
```

---

# 67. 正常不应修改

```text
其它 Feature YAML
project.yaml
BLUEPRINT_STATE.md
```

除非 E-CFG 最终唯一证明属于不同 Feature shard 所在 WorkItem。

即便如此，只做 Verification placement 必要修改。

---

# 68. 禁止修改

```text
schema/
tools/
shared/
androidApp/
iosApp/
```

---

# ============================================================

# COMMIT

# ============================================================

# 69. Commit

只做一个 R1 commit：

```text
fix(project-graph): reconcile phase 2d verification coverage
```

---

# 70. Push 后 STOP

完成后：

> STOP。

禁止创建：

```text
PHASE2D_ACCEPT.md

PHASE2D_TO_2E_HANDOFF.md

Phase 2E artifacts
```

---

# ============================================================

# FINAL REPORT

# ============================================================

# 71. 最终汇报模板

```text
Project Graph:
Phase 2D R1 — Verification Reconciliation

Baseline:
fb378da412c0688cd89a5f05c71605748f46c2e5

Completion Commit:
<sha>

================================
SOURCE RECOUNT
================================

Authoritative Checklist:
<path>

Verification Rows:
<n>

Source Status:
pass:
<n>
pending:
<n>
fail:
<n>
blocked:
<n>
not_required:
<n>

Disposition:
KEEP:
<n>
MIGRATE:
<n>
UPDATE:
<n>
DEFER:
<n>

Math Check:
PASS

UNEXPLAINED:
0

================================
DEFERRED
================================

Deferred Total:
<n>

Deferred Source Pass:
<n>

Deferred Source Pending:
<n>

Deferred Source Fail:
<n>

Deferred Source Blocked:
<n>

================================
CFG OWNERSHIP
================================

E-CFG-01..06:
K1a / K1g / DEFERRED

Evidence:
<exact source>

Graph Action:
<details>

Guess Used:
NO

================================
LEGACY VERIFY
================================

E-K1G-01:
RETAINED

Classification:
LEGACY_AGGREGATE /
EXISTING_VERIFY_NOT_IN_CURRENT_CHECKLIST

Current Coverage Source:
NO

Stale Aggregate Semantic:
FIXED / <details>

================================
SESSION
================================

Incorrect 98/98:
REMOVED / FIXED

SESSION Role:
TRANSITIONAL HANDOFF DOCUMENT

Manual Derived Truth:
DISALLOWED

Stable ID Redefinition:
DISALLOWED

Frozen Graph Decision Override:
DISALLOWED

Structural Redesign:
DEFERRED TO PHASE 2E / 3

================================
CLOSURE
================================

Done WorkItems Audited:
<n>

Violations:
<n>

Auto verifying → done:
0

================================
BOUNDARIES
================================

Stable Verification IDs Lost:
0

Plan Status Changed:
NO

CurrentWork Changed:
NO

Schema Changed:
NO

Validator Changed:
NO

Production Changed:
NO

Graph Mode:
draft

Phase 2E:
NOT STARTED

================================
VALIDATION
================================

Tests:
<command>
<result>

PG Check:
python .ai-context/project_graph/tools/project_graph.py check

Result:
PASS / 0 issue

Deviations:
NONE / <details>

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 72. 本轮最终目标

必须达到：

```text
Phase 2D Verification Graph
主体保留

Source Coverage
数学自洽

Verification Source Truth
不再因 Deferred 丢失

CFG Mapping
有证据 / 不猜

SESSION
事实正确
角色边界已冻结
但尚未重构

Phase 2E
NOT STARTED
```

