# Phase 2C Decisions — Frozen Semantics Ledger

> **Migration working record（迁移工作记录）——不是 Project Truth。**
> 本文件是 Phase 2C 冻结的语义决策台账。冻结语义同时写入 `../README.md`，此处记录判据与来源，供外部架构审核对照。
> 本文件不是动态 Truth，冻结后不得随意推翻（推翻需 Architecture Change）。

---

## 1. Plan Lifecycle（正式冻结）

| status | 含义 |
|---|---|
| `draft` | Plan 尚在编写。 |
| `reviewing` | Plan 正在设计评审 / 架构挑战 / 方案审查。 |
| `accepted` | Plan 已被冻结并可实施，但实施尚未开始。 |
| `implementing` | 编码/迁移已经开始，但 Plan 要求的实现步骤尚未全部交付。 |
| `completed` | **Plan 要求的代码/文档实施步骤完成 + 设计/架构接受条件完成。** |
| `superseded` | Plan 被另一正式 Plan 取代。 |

> **Plan completed ≠ WorkItem done**（正式冻结）。以下状态组合完全合法：
>
> ```text
> Plan: completed
> WorkItem: verifying
> Verification: device / pending
> ```
>
> `Plan completed` 只表示「实施方案自身已完成」，不触发 WorkItem done；WorkItem done 仍受 Verification Closure Contract 约束（`verifying` 是正确状态，不可机械复制为 done）。

## 2. PLAN-AI-NDJSON 最终裁决

```text
事实: B1–B6 主线实现 CODE + ARCH accepted（SESSION/BLUEPRINT_STATE，commit ad1c5878/d7240d6f 时代背景）。
仓库无更新证据表明 Plan 实现仍有未完成步骤。
裁决: PLAN-AI-NDJSON.status = completed
连带: K1g.status = verifying（required device verification E-K1G-01 pending）
禁止: Plan completed → K1g done
```

## 3. K1i 独立 Plan（PLAN-K1I）

```text
裁决: K1i 拥有独立 PLAN-K1I（禁止 implemented_by PLAN-AI-NDJSON）。
事实: K1i CODE + ARCH accepted；required device verification pending。
PLAN-K1I.status = completed（实现步骤已交付）
K1i.status = verifying（保持）
```

## 4. Observed vs Verification（正式冻结）

```text
Ordinary command execution（build / test / lint / pg check）= Observed Fact by default，不是 Verification Entity。
```

- 只有「稳定 Acceptance Semantic + 稳定 Verification ID + 明确验证对象」才成为 Verification Entity（如 `E-K1I-01`、`E-L1-01` 等真实验收项）。
- 禁止仅因 Gradle 成功新建 build Verification（如 `E-BUILD-K1I build/pass`）。
- Phase 2C **不实现 Observed Store**：无 Observed schema、无 observed.yaml、无 command-history database；命令结果只用于执行报告与后续 Phase 4。
- Phase 2C 只允许修复「明显因 Plan/Relation 语义需要触碰的已有 PoC Verification reference」，禁止 Phase 2D full verification migration。

## 5. BLUEPRINT_STATE Extension 边界（正式冻结）

```text
BLUEPRINT_STATE 的 CODE / ARCH / REVIEW 属于 Cookbook-specific Project Graph Extension / Current Truth Source，
不是 Core Generic Schema Semantic。
```

- 禁止新增 `code_status` / `arch_status` / `review_status` 到 WorkItem / Plan / Verification 的 Core Schema。
- Extension 可用于判断 Plan implementation 是否完成、WorkItem 是否已过 CODE/ARCH、是否进入 verifying，**但不替代 WorkItem status**。
- Phase 2C 不做完整 Extension Reconcile；如发现 `project.yaml extension` 与 `BLUEPRINT_STATE.md` 漂移，记录 `BLUEPRINT_STATE_DRIFT` 到 `PHASE2C_CONFLICTS.md`，Follow-up Phase 2E。
- 本轮核对：`project.yaml extensions.blueprint_state`（roles=CODE/ARCH/REVIEW、turn=USER、current_batch=L1+K1i）与 `docs/context_memory/BLUEPRINT_STATE.md` 当前内容一致，**未发现漂移**。

## 6. FEAT-* WorkItem ID Convention（正式冻结）

```text
Convention: FEAT-<FEATURE>-NNN
```

- 适用于：无已有 Stable ID 且确认为 `kind: feature` 的匿名功能项。
- 编号：扫描整个 Graph 的 `FEAT-<FEATURE>-*`，取 max + 1（不存在则 001），永不重新编号。
- 只是 Migration Stable ID Convention，**不是 Core Schema 增量**：不修改 JSON Schema、不新增 `id_type` 字段。
- 本批结构化两个匿名项：
  - `FEAT-AI-MEAL-001` — AI 对话生成菜品/餐食（primary F-AI-MEAL，backlog；affects F-DISH/F-MEAL）
  - `FEAT-RECOMMEND-001` — 放开 AI 推荐限制（primary F-RECOMMEND，parked 用户暂缓）
- 对应 `KIND_ID_CONVENTION_REQUIRED` 冲突 → `RESOLVED_IN_PHASE_2C`。

## 7. K15 / I7 关系

```text
work:K15 related_to work:I7

Reason:
both concern AI failure/fallback chain, but K15 additionally covers
segmentation/token truncation/control.
```

两个 Stable ID 保持独立；不使用 `supersedes`（无证据一方替代另一方）、不使用 `depends_on`。

## 8. J22 / L2 关系

```text
work:J22 related_to work:L2

Reason:
语义高度重叠（健康状态加脂肪肝 App 侧入口），当前只能确认重复、不能确认正式 replacement direction。
```

两个 Stable ID 保持独立；默认 `related_to`，只有发现明确「L2 replaces J22」式正式决策才允许 `supersedes`。本轮未发现 → `related_to`。

## 9. L3 跨 Feature 关系

```text
L3 Primary: F-TOOLS（保持，禁止迁回 F-AI-MEAL）
L3 affects F-INGREDIENT / F-DISH / F-MEAL / F-WEEKPLAN / F-PANTRY / F-NUTRITION
```

- `FEATURE_SPLIT_CANDIDATE` 继续保留，Follow-up Phase 2E。
- Phase 2C 不新增 `F-AI-PLATFORM` 或任何 Feature。
- L3 正式源（`待办_战略会商.md#L3`）明确覆盖 食材/菜品/餐次/计划/库存/营养 → 六个 `affects` 全部有源证据。

---

*Phase 2C Decisions · 2026-08-10。*
