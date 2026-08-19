# Phase 2A → 2B Handoff

> 本文件是 **Phase 2B 执行的强制入口**。Phase 2A 已 ACCEPT（`b54246c1`），Feature ownership 已冻结。
> Phase 2B 迁移时的所有权判断，以本文件 + Feature CodeMapping 为准（Truth Source Priority §13.3）。

---

## 1. Feature Universe（冻结，13 个）

```text
F-MEAL / F-AI-MEAL / F-TIMELINE / F-INGREDIENT / F-DISH / F-PANTRY /
F-RECOMMEND / F-NUTRITION / F-HEALTH / F-FAMILY / F-WEEKPLAN / F-SYNC / F-TOOLS
```

> **Phase 2B 默认禁止新增、删除、重命名、合并 Feature。**
> 若确实无法归类，仅记录 `FEATURE_SPLIT_CANDIDATE` 到 Conflict Ledger，不得自行拆 Feature。

## 2. Feature ownership 冻结决策（2A 已审核）

```text
库存 / Pantry                    → F-PANTRY
食材库 / Ingredient Metadata     → F-INGREDIENT

推荐 / 自由搭配 / 推荐引擎       → F-RECOMMEND
周计划 / 周期计划                → F-WEEKPLAN

家庭成员 / 健康档案数据          → F-FAMILY
健康规则 / 慢病评价 / 健康算法    → F-HEALTH

厨房工具 / 采购 / 设置 / 导航 / 主题 /
通用组件 / 工程入口等暂归          → F-TOOLS
```

> 完整决策表 + 边界裁量（F-PANTRY vs F-INGREDIENT、F-WEEKPLAN vs F-RECOMMEND、F-FAMILY vs F-HEALTH、F-TOOLS wide boundary、match overlap）见 `PHASE2A_REVIEW.md`，Phase 2B 不得擅自推翻。

## 3. F-TOOLS 特别规则

Phase 2B 不允许因为 F-TOOLS 很宽就擅自创建：

```text
F-SHOPPING / F-COOKING / F-SETTINGS / F-INFRA
```

若迁移过程中发现确实无法合理归类：

```text
FEATURE_SPLIT_CANDIDATE
```

放入 Conflict Ledger，等架构审核。

## 4. source_refs

Entity → `source_refs` 已可用于：

```text
WorkItem / Plan / Verification / Feature
```

Phase 2B 每个迁移 WorkItem 原则上提供 **1~3 个 authoritative source_refs**
（第一条=身份最权威来源，第二条=当前状态最权威来源）。

## 5. 下一阶段输入（Phase 2B）

- **迁移范围**：当前未完成（backlog~parked）+ 当前主线最近完成但解释现状 + 被 Plan/Verify/CurrentWork 引用；历史完成项按 §17 过滤。
- **已有稳定 ID**（I*/J*/K*/L*/AF-* 等）全保留，禁止重编号（如 K1g → TODO-AI-MEAL-001）。
- **状态以 SESSION + BLUEPRINT_STATE + 最新 Verification 为准**，禁止机械复制旧待办状态。
- **CODE+ARCH accepted + required device pending → verifying**（不是 done）。
- **不批量迁 Plan（Phase 2C）**、**不迁完整 Verification 清单（Phase 2D）**、**不伪造 Verification 让 done 通过**。

---

*Phase 2A → 2B Handoff · 2026-08-10。*
