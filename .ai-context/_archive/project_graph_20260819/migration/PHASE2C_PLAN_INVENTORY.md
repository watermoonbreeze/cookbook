# Phase 2C Plan Inventory

> **Migration working record（迁移工作记录）——不是 Project Truth。**
> Phase 2C 的 Plan 迁移逐项台账：每项给出 WorkItem → Formal Plan Source → Plan ID → Plan Status → WHY → ACTION。
> 遵循 No Guess Rule（实施蓝图 §77）：无法确定 formal Plan 的不创造。
> 遵循 §78：迁 Plan 不追求数量，只迁「当前真实 Formal Plans」。

---

## 已迁移 Plan

### PLAN-AI-NDJSON

```text
WORKITEM:
K1g（周期记 + NDJSON 流式解析改造 · verifying）

FORMAL PLAN SOURCE:
.ai-context/docs/feature/AI记一餐_周期记_NDJSON流式开发规范.md

EXISTING PLAN ID:
PLAN-AI-NDJSON（既有，保留不改名）

TARGET PLAN ID:
PLAN-AI-NDJSON

PLAN STATUS:
completed

WHY:
B1-B6 主线实现已 CODE + ARCH accepted（SESSION/BLUEPRINT_STATE），Plan 要求的实现步骤已全部交付；
仓库无更新证据表明仍有未完成实施步骤 → 按实施蓝图 §35 判定 completed。
K1g 仍 verifying（E-K1G-01 device pending），Plan completed ≠ WorkItem done。

ACTION:
UPDATE（implementing → completed；补 source_refs 首条=正式 Blueprint）
```

### PLAN-K1I

```text
WORKITEM:
K1i（全App AI输出流式/渐进显示兼容 · verifying）

FORMAL PLAN SOURCE:
.ai-context/docs/feature/K1i_AI流式渐进展示_实施蓝图.md

EXISTING PLAN ID:
NONE（蓝图文档无既有稳定 Plan ID）

TARGET PLAN ID:
PLAN-K1I

PLAN STATUS:
completed

WHY:
K1i CODE + ARCH accepted（required device verification pending）；正式 Plan 实现步骤已交付。
禁止 K1i 复用 PLAN-AI-NDJSON（实施蓝图 §37）；K1i 有独立 Verification（E-K1I-01/02），保持 verifying。

ACTION:
CREATE
```

### PLAN-K1A

```text
WORKITEM:
K1a（AI预览确认页营养热量展示统一化 · verifying）

FORMAL PLAN SOURCE:
.ai-context/docs/feature/AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md

EXISTING PLAN ID:
NONE

TARGET PLAN ID:
PLAN-K1A

PLAN STATUS:
completed

WHY:
K1a 正式专项实施蓝图存在且 ARCH 已复核通过（BLUEPRINT_STATE：K1a ACCEPTED，真机 E-K1A-01 pending）。
Plan 实现步骤已交付 → completed；K1a 保持 verifying。

ACTION:
CREATE
```

### PLAN-L1

```text
WORKITEM:
L1（用户协议免责声明 + AI 功能开启前弹窗告知 · verifying）

FORMAL PLAN SOURCE:
.ai-context/docs/feature/L1_云端AI首启同意与合规免责_实施蓝图.md

EXISTING PLAN ID:
NONE

TARGET PLAN ID:
PLAN-L1

PLAN STATUS:
completed

WHY:
L1 正式专项实施蓝图存在且 ARCH 已复核通过（BLUEPRINT_STATE：L1 ACCEPTED，真机 E-L1-01~12 pending）。
Plan 实现步骤已交付 → completed；L1 保持 verifying。

ACTION:
CREATE
```

---

## 已核查·不迁移（No Guess / 非正式 Plan）

### K1b —— NOT CREATED

```text
WORKITEM:
K1b（膳食健康评价逐成员化 · parked · DRAFT）

FORMAL PLAN SOURCE:
.ai-context/docs/feature/AI记一餐_K1b膳食健康评价逐成员化_实施蓝图.md（DRAFT · 未 ACCEPT）

EXISTING PLAN ID:
NONE

TARGET PLAN ID:
NONE（NOT CREATED）

PLAN STATUS:
N/A

WHY:
K1b 蓝图处于 DRAFT·PARKED，尚未被架构接受为正式可实施 Plan；
实施蓝图 §41 未要求迁 K1b；§77 No Guess Rule + §78 不追求数量 → 不创建 PLAN-K1B。
等 K1b 蓝图推进到 BLUEPRINT_READY/ACCEPTED 再迁（后续阶段）。

ACTION:
NOT CREATED（记录观察项，非冲突）
```

### 其余 WorkItem —— NOT CREATED

```text
范围:
除 K1g/K1i/K1a/L1 外，其余 WorkItems 无「当前 WorkItem 使用 / 已完成但仍解释现状 /
后续 Relation·Verification 引用 / SESSION·BLUEPRINT_STATE 明确引用」的正式实施蓝图（多为 backlog 待办 + 方案/设计文档），
不满足实施蓝图 §28 迁移判据 → 不迁移、不猜测。

ACTION:
NOT CREATED
```

---

*Phase 2C Plan Inventory · 2026-08-10。*
