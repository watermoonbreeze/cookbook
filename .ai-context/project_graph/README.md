# Project Graph（Cookbook 实施版 · Phase 1 — Model Contract）

> **Phase 1 — Model Contract**: FINAL ACCEPT / FROZEN
> **Phase 2A — Feature Universe**: ACCEPT / CLOSED
> **Phase 2B — Current WorkItem**: ACCEPT / CLOSED
> **Phase 2C — Plan + Relation + Deferred Semantics**: ACCEPT / CLOSED
> **Phase 2D — Verification Bootstrap**: RECONCILED / WAITING FOR ARCHITECTURE REVIEW
> **Graph Mode**: `draft`

> 本目录是 Project Graph 的 **数据真相源（Project Truth）**。Phase 1（Model Contract）**FINAL ACCEPT**、核心 Contract **FROZEN**；Phase 2A（Feature Universe）已 **ACCEPT / CLOSED**，13 个 Feature shard + `source_refs` provenance 已冻结；Phase 2B（Current WorkItem）已 **ACCEPT / CLOSED**；Phase 2C（Plan + Relation + Deferred Semantics）**已 ACCEPT / CLOSED**。Phase 2D R1 已完成 Verification Coverage Reconciliation：权威清单 114 条、Deferred 69 条、CFG ownership 已有证据、E-K1G-01 已标记 legacy aggregate；当前状态为 **RECONCILED / WAITING FOR ARCHITECTURE REVIEW**。**Graph 仍为 mode: draft，未正式启用**。
>
> 维护角色：**AI Maintained, Human Read-Only**。
>
> 阶段记录：Phase 1 → [`migration/PHASE1_FINAL_ACCEPT.md`](migration/PHASE1_FINAL_ACCEPT.md) ｜ Phase 2A → [`migration/PHASE2A_ACCEPT.md`](migration/PHASE2A_ACCEPT.md) ｜ Phase 2A→2B 交接 → [`migration/PHASE2A_TO_2B_HANDOFF.md`](migration/PHASE2A_TO_2B_HANDOFF.md) ｜ Phase 2B Accept → [`migration/PHASE2B_ACCEPT.md`](migration/PHASE2B_ACCEPT.md) ｜ Phase 2B→2C 交接 → [`migration/PHASE2B_TO_2C_HANDOFF.md`](migration/PHASE2B_TO_2C_HANDOFF.md) ｜ Phase 2C 决策 → [`migration/PHASE2C_DECISIONS.md`](migration/PHASE2C_DECISIONS.md) ｜ Phase 2C 冲突 → [`migration/PHASE2C_CONFLICTS.md`](migration/PHASE2C_CONFLICTS.md) ｜ Phase 2C Accept → [`migration/PHASE2C_ACCEPT.md`](migration/PHASE2C_ACCEPT.md) ｜ Phase 2C→2D 交接 → [`migration/PHASE2C_TO_2D_HANDOFF.md`](migration/PHASE2C_TO_2D_HANDOFF.md)。

## 0. Phase 1 状态与冻结契约（Status & Frozen Contract）

### 0.1 Phase Progress

```text
Phase 1  — Model Contract      : FINAL ACCEPT / FROZEN   （Final Review Commit 83623a3）
Phase 2A — Feature Universe    : ACCEPT / CLOSED          （Review Commit b54246c1）
Phase 2B — Current WorkItem    : ACCEPT / CLOSED          （Review Commit e2127176）
Phase 2C — Plan+Relation+Deferred : ACCEPT / CLOSED       （Review Commit ced5f13f）
Phase 2D — Verification Bootstrap : AUTHORIZED / NOT STARTED
Phase 2  — 整体                : IN PROGRESS（非 COMPLETE）
Graph Mode                     : draft
```

Phase 2A 已完成能力见 `migration/PHASE2A_ACCEPT.md`；Phase 2A→2B 执行强制入口见 `migration/PHASE2A_TO_2B_HANDOFF.md`；Phase 2B Accept 记录见 `migration/PHASE2B_ACCEPT.md`；Phase 2B→2C 强制入口见 `migration/PHASE2B_TO_2C_HANDOFF.md`；Phase 2C 决策/冲突见 `migration/PHASE2C_DECISIONS.md` / `migration/PHASE2C_CONFLICTS.md`；Phase 2C Accept 见 `migration/PHASE2C_ACCEPT.md`；Phase 2C→2D 强制入口见 `migration/PHASE2C_TO_2D_HANDOFF.md`。
**Phase 2B 已 ACCEPT / CLOSED（Review Commit `e2127176`）**：Migration Reconciliation（Rework）完成——Stable ID（FAM-AGE/FAM-MEAL/K15/J22）无损恢复、Source Coverage 100%（UNEXPLAINED=0）、TODO-* kind 全部修正、K1c 状态修正、L3 → F-TOOLS（FEATURE_SPLIT_CANDIDATE）。最终派生统计（From Graph）：Total 104 / Stable 51 / Generated 53。
**Phase 2C 已 ACCEPT / CLOSED（Review Commit `ced5f13f`）**：Plan lifecycle 冻结（Plan completed ≠ WorkItem done）、Observed vs Verification 冻结、BLUEPRINT_STATE Extension 边界冻结、FEAT-* 匿名 feature 约定结构化；Plan 迁移 PLAN-AI-NDJSON(completed)/PLAN-K1I(completed)/PLAN-K1A(completed)/PLAN-L1(completed)，K1g/K1i/K1a/L1 均保持 verifying；关系 K15↔I7、J22↔L2（related_to）、L3 affects 6 Feature 已建立；FEAT-AI-MEAL-001 / FEAT-RECOMMEND-001 已落位。**Phase 2D（Verification Bootstrap）已 AUTHORIZED 但未开始；执行入口见 `migration/PHASE2C_TO_2D_HANDOFF.md`。**

### 0.2 冻结契约（Phase 1 Frozen Contract）

以下 Phase 1 核心设计已冻结，后续 Phase 不得因迁移数据方便而随意改动：

- **核心实体**：Project / Feature / WorkItem / Plan / Verification / Relation / CodeMapping / CurrentWork
- **稳定 ID / Typed Reference / Feature Sharding**
- **Declared / Observed / Derived** 三分类契约
- **状态机**：Feature Lifecycle、WorkItem State Machine、Plan State Machine、Verification State Machine
- **Relation**：Canonical Direction、Semantic Matrix
- **Verification Closure Contract**
- **YAML Fail Closed / JSON Schema Validation / Semantic Validator / Duplicate Detection**

### 0.3 Frozen 的含义

Frozen **≠** 永远不可修改。Frozen 表示：

> 后续 Phase 不得因为迁移数据方便而随意改变核心 Contract。

只有满足以下**三者**才能修改 Frozen Contract：

1. 发现通用模型无法表达真实项目需求；
2. 形成明确 Architecture Change；
3. 经过独立架构复审。

**禁止**：`某模型发现某条数据不好迁 → 顺手改 Schema`。

### 0.4 mode: draft 与 Frozen 的区别

Contract = **Frozen** 与 Graph mode = **draft** 二者不冲突，同时成立：

- **Contract Frozen**：Project Graph 的**语言**已经稳定。
- **Graph draft**：Cookbook 的真实项目数据尚未完整 Bootstrap，Generated Views 尚未切换，Project Graph 尚未成为当前唯一 Project Truth 入口。

因此本阶段**绝对禁止**把 `mode` 改成 `active`。

### 0.5 Phase 2C 冻结语义（Plan / Observed / Extension / FEAT-*）

Phase 2C（Plan + Relation + Deferred Semantics）已实施并冻结以下治理语义，判据与来源见 `migration/PHASE2C_DECISIONS.md`：

- **Plan Lifecycle**（状态集合，非严格单向链）：`draft`、`reviewing`、`accepted`、`implementing`、`completed`、`superseded`。**`completed` 必须表示「Plan 要求的代码/文档实施步骤完成 + 设计/架构接受条件完成」**；`superseded` 是替换状态（当前 Plan 被另一正式 Plan 取代），**不要求以 `completed` 为前态**。
- **Plan completed ≠ WorkItem done**：`Plan: completed + WorkItem: verifying + Verification: device/pending` 完全合法。`completed` 只表示实施方案自身完成；WorkItem done 仍受 Verification Closure Contract 约束。
- **Observed vs Verification**：普通命令执行（build / test / lint / pg check）= **Observed Fact by default**，不是 Verification Entity。只有「稳定 Acceptance Semantic + 稳定 Verification ID + 明确验证对象」才成为 Verification（如 `E-K1I-01`）。禁止仅因 Gradle 成功新建 build Verification。Phase 2C **不实现 Observed Store**。
- **BLUEPRINT_STATE Extension 边界**：BLUEPRINT_STATE 的 `CODE/ARCH/REVIEW` 属于 **Cookbook-specific Project Graph Extension**，不是 Core Schema Semantic。禁止新增 `code_status` / `arch_status` / `review_status` 到 WorkItem/Plan/Verification Core Schema。
- **FEAT-* ID Convention**：匿名且确认为 `kind: feature` 的 WorkItem 使用 `FEAT-<FEATURE>-NNN`（max+1，永不重编号）。只是 Migration Stable ID Convention，不修改 JSON Schema。本批：`FEAT-AI-MEAL-001`、`FEAT-RECOMMEND-001`。

## 1. 这是什么

Project Graph 是 AI 开发时代的「项目结构化记忆层」。它回答：

> 项目现在发生了什么。

三种 Truth 各司其职：

```
Code                = Runtime Truth   （系统实际怎么运行）
Project Graph       = Project Truth    （项目当前处于什么状态）
Plan / ADR / 专项方案 = Decision Truth   （为什么这么设计）
```

本目录的 Markdown（README 之外不生成视图，Phase 1 不做）属于 View，可由 `pg render`（未来）重新生成，不作为真相源。

## 2. 目录结构

```
.ai-context/project_graph/
├── README.md                       # 本文件（设计说明）
├── project.yaml                    # Project 根：graph_version/mode/current/feature registry/extensions
├── features/
│   ├── F-AI-MEAL.yaml              # PoC 完整样例（WorkItem/Plan/Verification/Relation/CodeMapping）
│   └── F-MEAL.yaml                 # 简单样例（仅 match/code）
├── schema/
│   └── project-graph.schema.json   # JSON Schema（通用，不含业务字段）
└── tools/
    ├── yaml_lite.py                 # 零依赖 YAML 子集解析器
    ├── schema_checker.py            # 迷你 JSON Schema 校验器（零依赖）
    ├── project_graph.py             # 语义校验器 + `pg check` CLI
    └── tests/
        └── test_validator.py        # 覆盖 §28 全部场景的测试套件
```

## 3. 核心实体

| 实体 | 说明 | ID 形式 |
|------|------|---------|
| **Project** | 根节点：graph_version / mode / current / feature registry / extensions | — |
| **Feature** | 长期稳定的产品/系统能力（不是 Screen/VM/Class/阶段） | `F-XXX` |
| **WorkItem** | Bug/Todo/功能/重构等的统一项，用 `kind` 区分；保留现有 I/J/K/L 编号 | `K1i` `AF-B456-05` |
| **Plan** | WorkItem 的实施设计；一个 Plan → 多个 WorkItem（`work_items`） | `PLAN-XXX` |
| **Verification** | 闭环验证，关联 WorkItem | `E-XXX` |
| **Relation** | 正式关系概念，Typed Reference 表达 | `work:K1i` |
| **CodeMapping** | Feature↔Code 双向映射（`match` glob + `code` 路径） | — |
| **CurrentWork** | 当前 feature/work_item/phase/blocker 单一入口 | — |

Feature 按「文件分片」组织（`features/<id>.yaml`），AI 处理任务时只需读 `project.yaml` + 当前 Feature 文件，降低 Token。

**Feature shard Contract**：`features/F-A.yaml` 中的 WorkItem 的 **primary feature 必须等于 F-A**（`work_item.feature` 字段，语义校验器强制）；跨 Feature 影响使用 `affects` / `depends_on` / `related_to` 等 relation 表达。**禁止把属于 F-B 的 WorkItem 放进 F-A shard。**

## 4. 状态机（enum，禁止自由字符串）

| 实体 | 取值 |
|------|------|
| Graph mode | `draft` `active` |
| Feature lifecycle | `planned` `active` `mature` `deprecated` |
| Feature activity（Derived） | `idle` `developing` `reviewing` `verifying` `blocked` |
| Feature health（Derived） | `green` `yellow` `red` |
| WorkItem status | `backlog` `ready` `in_progress` `blocked` `review` `verifying` `done` `parked` `cancelled` |
| Plan status | `draft` `reviewing` `accepted` `implementing` `completed` `superseded` |
| Verification status | `pending` `pass` `fail` `blocked` `not_required` |
| WorkItem kind | `feature` `bug` `todo` `tech_debt` `refactor` `compliance` `research` `maintenance` |
| Relation type | `belongs_to` `implemented_by` `verified_by` `depends_on` `blocks` `affects` `supersedes` `related_to` |

## 5. Declared / Observed / Derived（§19）

同一事实只维护一次；能 Derive 的绝不要求 AI 手工重复同步。

| 类别 | 来源 | 示例 |
|------|------|------|
| **Declared** | AI 维护 | `status` / feature relation / plan relation / priority / intent |
| **Observed** | 工具采集 | commit / changed files / test exit code / timestamp |
| **Derived** | 程序计算 | open bug count / pending verify count / activity / health / affected features |

- **Final V1 Contract**：`Feature.lifecycle = Declared`；`Feature.activity = Derived`；`Feature.health = Derived`。核心 Feature YAML 中**不存储 activity/health**（Schema 的 Feature 无此字段，AI 不得声明或覆盖）。`project_graph.py` 已实现 `derive_activity` / `derive_health` 作为推导契约演示。
- 如未来确需 override activity/health，属于 Extension 设计，**本阶段不实现**。

## Source Provenance（Phase 2A · source_refs）

Entity → `source_refs` → authoritative repository artifact：

```yaml
source_refs:
  - .ai-context/docs/feature/AI记一餐_周期记_NDJSON流式开发规范.md
  - .ai-context/docs/功能路径索引.md#记录
```

- `source_refs` 可挂在 **Feature / WorkItem / Plan / Verification** 上（可选，通常 1~3 条），指向**权威来源**（功能路径索引 / 项目业务地图 / 专项方案），不是罗列所有提到该实体的文档。
- 语义校验器（PG-P2A-A01）轻量检查：去掉 `#anchor` 后，path 不得绝对、不得含 `..` 逃逸仓库、文件必须存在（相对仓库根解析）。Anchor 本身本阶段不验证。错误码 `PG-E-SOURCE_REF`。
- **`source_refs` 不改变 Truth Source 优先级，只记录证据来源**：Project Graph 仍是 Project Truth；Code / SESSION / BLUEPRINT_STATE / 项目地图 的现有优先级不变。

## 6. Typed Reference 与 Relation Canonical Direction（§18）

内部引用标准化为带类型：

```
feature:F-AI-MEAL   work:K1i   plan:PLAN-AI-NDJSON   verify:E-K1I-01
```

### Relation canonical direction

所有 relation 都有**唯一 canonical 方向**（本蓝图确认的最终 Contract）：

```text
belongs_to       work → feature
implemented_by   work → plan
verified_by      work → verification
```

即：WorkItem 属于 Feature（belongs_to）、被 Plan 实现（implemented_by）、被 Verification 验证（verified_by）——**三者都以 work 为起点**。不得按书写顺序反向理解。

### Shorthand 与 canonical relation 分开说明

为编辑友好，YAML 中 shorthand 字段用裸 ID（由字段上下文确定类型）。**shorthand 只是存储形态，normalized graph semantic 必须回到 canonical direction**：

```yaml
work_items:
  - id: K1i
    feature: F-AI-MEAL        # shorthand → normalized: work:K1i --belongs_to--> feature:F-AI-MEAL
plans:
  - id: PLAN-X
    work_items:
      - WORK-X               # storage shorthand → normalized: work:WORK-X --implemented_by--> plan:PLAN-X
verifications:
  - id: E-K1I-01
    work_item: K1i            # shorthand → normalized: work:K1i --verified_by--> verify:E-K1I-01
```

> 注意：`plan.work_items: [WORK-X]` 是 **Storage Shorthand**——它表示 `work:WORK-X --implemented_by--> plan:PLAN-X`，**不是** `plan → work`。Verification 同理（`work:WORK-Y --verified_by--> verify:E-Y`，不是 `verify → work`）。

跨切关系（depends_on / blocks / affects / supersedes / related_to）用 `relations` 显式声明带类型引用：

```yaml
relations:
  - source: work:WORK-X
    type: depends_on
    target: work:WORK-Y
```

每种关系**唯一声明源**，不重复：belongs_to 在 work_item.feature、implemented_by 在 plan.work_items、verified_by 在 verification.work_item，其余在 relations。

## 7. Code Mapping 双向用途（§21）

```yaml
match:                 # 文件 glob，支持 Code → Feature 反向定位
  - shared/**/ai/meallog/**
code:                  # 关键路径（真实 repo-relative path 数组），支持 Feature → Code 生成功能路径索引
  ui:
    - androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputSheet.kt
  viewmodel:
    - androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/ai/AiMealInputViewModel.kt
  domain:
    - shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealSession.kt
    - shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealParser.kt
  tests:
    - shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/ai/meallog/StreamingMealParserTest.kt
```

- **Forward（Feature → Code）**：`Feature → CodeMapping → 功能路径索引`（AI 代码定位）。
- **Reverse（Code → Feature）**：`git diff changed files → 匹配 match glob → affected Feature`（未来 `pg affected`）。

`code` 键只允许：`entry` `ui` `viewmodel` `domain` `data` `core` `tests` `other`（语义校验器强制）。**路径必须相对仓库 root、用 `/` 分隔；禁止 `...` 缩略、类名代替路径、逗号拼接 CSV。**

## 8. Affected 契约（§22，Phase 1 仅定义不实现）

未来流水线：

```
Changed Files → Code Mapping(match) → Direct Features → Dependents/Related
```

设计上保证 `match` 能反向定位，不会出现「未来无法反向定位的 Code Mapping」。

## 9. Extensions（§24）

核心不写死角色/平台/业务。Cookbook 现有 `BLUEPRINT_STATE` 的 CODE/ARCH/REVIEW/TURN 通过 `extensions` 承载：

```yaml
extensions:
  blueprint_state:
    roles: [CODE, ARCH, REVIEW]
    turn: USER
```

语义校验器检查 extension 键不侵入核心顶层字段（`kind`/`graph_version`/`mode`/`project`/`current`/`features`/`extensions`）。

## 10. Schema 标准（§25）

```
YAML Data → yaml_lite 解析 → JSON 对象 → JSON Schema(schema_checker) → 语义校验器(project_graph)
```

- JSON Schema（`project-graph.schema.json`）负责：类型 / 必填 / enum / ID 格式 / 基本结构。**通用，不含 `AI记一餐`/`Kotlin`/`营养`/`Android` 等业务约束。**
- Python Semantic Validator 负责：跨文件引用、ID 唯一、Relation 合法、depends_on 循环、Code Mapping 类型、CurrentWork、Extension、Done 闭环等。

## 11. Validator 检查项（§27）

`pg check` 至少执行：

1. YAML 可解析
2. JSON Schema 校验通过
3. Graph 版本合法
4. ID 唯一
5. Feature 引用存在（registry）
6. WorkItem.feature 存在
7. Plan 引用存在
8. Verification 引用存在
9. Relation source 存在
10. Relation target 存在
11. 状态合法（enum）
12. 非法自引用
13. depends_on 明显循环
14. Code Mapping 类型合法
15. CurrentWork 引用合法
16. Extension 不破坏核心 Schema

附加语义（**Verification Done Contract**，与代码 `_check_done_rule` 一致）：
- WorkItem `status = done` 要求**至少存在一个 Verification**；
- 且所有 `required = true` 的 Verification 都必须为 `pass` 或 `not_required + reason`；
- 任何 `required = true` 的 Verification 处于 `pending` / `fail` / `blocked` → **禁止 Done**；
- `required = false`（可选）的 Verification 不阻止 Done；`not_required` 必须带 `reason`（§16）。

## 12. 错误输出格式（§32）

避免裸 stack trace，结构化输出：

```
PG-E-RELATION
file: features/F-AI-MEAL.yaml
source: work:K1g
target: plan:PLAN-X
reason: target does not exist
```

错误码：`PG-E-LOAD` `PG-E-SCHEMA` `PG-E-GRAPH_VERSION` `PG-E-DUP_ID` `PG-E-UNKNOWN_FEATURE` `PG-E-WORK_FEATURE` `PG-E-PLAN_REF` `PG-E-VERIFY_REF` `PG-E-RELATION_SOURCE` `PG-E-RELATION_TARGET` `PG-E-SELF_REF` `PG-E-CYCLE` `PG-E-CODE_MAPPING` `PG-E-CURRENT` `PG-E-EXTENSION` `PG-E-DONE_NO_VERIFY` `PG-E-VERIFY_REASON` `PG-E-REGISTRY_MISMATCH` `PG-E-SOURCE_REF`。

## 13. Tool Contract（§31）

Phase 1 真正可用：`pg check`。其余为未来 CLI 预定义，**本阶段不实现**。

| 命令 | 职责 | Phase 1 |
|------|------|---------|
| `pg check` | schema + 语义 + relation 校验 | ✅ 可用 |
| `pg begin` | 读 AI_INDEX、定位 Feature/WorkItem、写 CurrentWork | 未实现 |
| `pg affected` | changed files → affected features | 未实现 |
| `pg verify` | 执行验证命令、记录 Observed Facts | 未实现 |
| `pg reconcile` | git diff ↔ graph 一致性 | 未实现 |
| `pg render` | 生成 Views（deterministic） | 未实现 |
| `pg finish` | check pass 才允许宣布完成 | 未实现 |

## 14. 使用

```bash
# 校验 Project Graph（默认本目录）
python .ai-context/project_graph/tools/project_graph.py check

# 校验指定目录
python .ai-context/project_graph/tools/project_graph.py check <graph_dir>

# 运行测试套件（零依赖，unittest）
cd .ai-context/project_graph/tools/tests
python -m unittest test_validator -v
```

CLI 退出码：0 = 通过，1 = 有 issue，2 = 用法错误。

## 15. 设计原则（§33，成熟思想来源）

不照搬大型平台，吸收其思想：

| 来源 | 吸收 |
|------|------|
| Backstage | Stable Entity / Typed Relation |
| GitOps | Declarative / Versioned / Reconciliation |
| Nx Project Graph | Code Mapping / Affected Detection |
| GitHub Projects | One Data Model, Multiple Views |
| C4 | Different Zoom Levels / Different Audiences |
| JSON Schema | Standard Schema Validation |

## 16. 不做（§34）

Phase 1 明确不引入：Backstage 平台 / Nx 工具本身 / Neo4j / 图数据库 / GitHub Projects 作 Truth / Web Dashboard / 后台服务 / Event Sourcing。

保持：**Git-native + File-based + AI-native**。

## 17. 跨切质量要求

- **Git-native（§35）**：所有数据可 `git diff` / `git review` / `git revert`，禁止只有本地数据库。
- **Token 友好（§36）**：按 Feature 分文件，普通任务只读 `project.yaml` + 当前 Feature 文件，不加载整个 Graph。
- **Deterministic（§37）**：同样输入 → 同样输出；未来 `pg render` 两次执行须 0 diff。
- **零依赖**：工具仅用 Python 标准库（无 PyYAML/jsonschema 也能运行，适配本仓库无网络环境）。`yaml_lite` 是受约束子集解析器；正式环境可换 PyYAML（接口兼容）。

## 18. 现有状态冲突（§41）

Phase 1 若发现 SESSION / 07 / 待办 / 方案 / 功能路径互相冲突，**只记录**（`MIGRATION_NOTE`），不在本阶段顺手修复——属于 Phase 2 Bootstrap。

## 19. Phase 1 验收对照（§39）

- [x] Schema 能表达 Cookbook 现有主要工作模型（Feature/WorkItem/Plan/Verification/Relation/CodeMapping/CurrentWork/Extension）
- [x] Schema 不依赖 Cookbook 业务（无业务字段）
- [x] Feature 是稳定长期实体
- [x] 现有 I/J/K/L 编号无损接入（PoC 用 K1g/K1i/K1b/I8/AF-* 形式）
- [x] WorkItem 统一模型成立
- [x] Plan 可关联多个 WorkItem
- [x] Verification 可闭环 WorkItem
- [x] Typed Relation 模型成立
- [x] Declared/Observed/Derived 明确
- [x] Code Mapping 支持正向和反向
- [x] JSON Schema 实际可运行
- [x] Semantic Validator 实际可运行
- [x] Validator 有充分测试（`tools/tests/test_validator.py`，覆盖 §28 全部场景 + 额外语义 + 解析 + 真实数据 + Rework 回归，运行：`python -m unittest test_validator`）
- [x] 没有迁移全量 Cookbook（仅 F-AI-MEAL + F-MEAL 两个样例）
- [x] 没有修改产品运行行为（无生产代码/DB 变更）

### Phase 1 验收最终状态

- Architecture Review: **FINAL ACCEPT**
- Final Review Commit: `83623a3`
- Known Blockers: **0**
- Contract: **FROZEN**

## 20. 门禁（§44）

Phase 1 实施时执行 **STOP 门禁**：完成 Phase 1 后 STOP，不继续迁移 Cookbook / 生成 AI_INDEX / 重写 07 / 重写功能路径索引 / 自动维护生命周期 / Git Hook / CI。

该门禁已完成：**Phase 1 已于最终审核提交 `83623a3` 后获得 FINAL ACCEPT，核心 Contract 已 FROZEN**（Phase 1 → Phase 2 完整承接见 `migration/PHASE1_FINAL_ACCEPT.md`）。

**当前阶段状态**：

```text
Phase 1
FINAL ACCEPT / FROZEN

Phase 2A
ACCEPT / CLOSED

Phase 2B
ACCEPT / CLOSED

Phase 2C
ACCEPT / CLOSED

Current Stage:
Phase 2D — Verification Bootstrap

Phase 2D Started:
YES — waiting for architecture review

Graph Mode:
draft
```

每一批独立 commit / push / architecture review，禁止连续自动执行。**禁止提前把 `mode` 切到 `active`。**
