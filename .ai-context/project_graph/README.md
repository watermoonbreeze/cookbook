# Project Graph（Cookbook 实施版 · Phase 1 — Model Contract）

> 本目录是 Project Graph 的 **数据真相源（Project Truth）**。Phase 1 只定义协议与验证骨架，**mode: draft，未正式启用**；现有 Truth（Code / SESSION / BLUEPRINT_STATE / 项目地图）优先级不变。
>
> 维护角色：**AI Maintained, Human Read-Only**。

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

- `Feature.activity` / `health` 原则上由 WorkItem 状态、blocker、失败验证推导（本目录 `project_graph.py` 已实现 `derive_activity` / `derive_health` 作为推导契约演示）。
- Schema 中 `activity`/`health` 为可选声明覆盖，**不是必填**——禁止把它做成需手工同步的必填项。

## 6. Typed Reference（§18）

内部引用标准化为带类型：

```
feature:F-AI-MEAL   work:K1i   plan:PLAN-AI-NDJSON   verify:E-K1I-01
```

为编辑友好，YAML 中 shorthand 字段用裸 ID（由字段上下文确定类型）：

```yaml
work_items:
  - id: K1i
    feature: F-AI-MEAL        # → belongs_to work:K1i → feature:F-AI-MEAL
plans:
  - id: PLAN-AI-NDJSON
    work_items: [K1g, K1i]    # → implemented_by plan → work
verifications:
  - id: E-K1I-01
    work_item: K1i            # → verified_by verify → work
```

跨切关系（depends_on / blocks / affects / supersedes / related_to）用 `relations` 显式声明带类型引用：

```yaml
relations:
  - source: work:K1i
    type: depends_on
    target: work:K1g
```

每种关系**唯一声明源**，不重复：belongs_to 在 work_item.feature、implemented_by 在 plan.work_items、verified_by 在 verification.work_item，其余在 relations。

## 7. Code Mapping 双向用途（§21）

```yaml
match:                 # 文件 glob，支持 Code → Feature 反向定位
  - shared/**/ai/meallog/**
code:                  # 关键路径，支持 Feature → Code 生成功能路径索引
  ui: AiMealInputSheet
  viewmodel: AiMealInputViewModel
  domain: StreamingMealSession,StreamingMealParser
  tests: StreamingMealParserTest
```

- **Feature → Code**：未来生成 `功能路径索引`（AI 代码定位）。
- **Code → Feature**：未来 `pg affected` —— git diff → 匹配 `match` → Direct Features → Dependents。

`code` 键只允许：`entry` `ui` `viewmodel` `domain` `data` `core` `tests` `other`（语义校验器强制）。

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

附加语义：Done 须有 `pass`/`not_required` 验证（§41）；`not_required` 须有 `reason`（§16）。

## 12. 错误输出格式（§32）

避免裸 stack trace，结构化输出：

```
PG-E-RELATION
file: features/F-AI-MEAL.yaml
source: work:K1g
target: plan:PLAN-X
reason: target does not exist
```

错误码：`PG-E-LOAD` `PG-E-SCHEMA` `PG-E-GRAPH_VERSION` `PG-E-DUP_ID` `PG-E-UNKNOWN_FEATURE` `PG-E-WORK_FEATURE` `PG-E-PLAN_REF` `PG-E-VERIFY_REF` `PG-E-RELATION_SOURCE` `PG-E-RELATION_TARGET` `PG-E-SELF_REF` `PG-E-CYCLE` `PG-E-CODE_MAPPING` `PG-E-CURRENT` `PG-E-EXTENSION` `PG-E-DONE_NO_VERIFY` `PG-E-VERIFY_REASON` `PG-E-REGISTRY_MISMATCH`。

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
- [x] Validator 有充分测试（27 项，覆盖 §28 全部 14 场景 + 额外语义 + 解析 + 真实数据）
- [x] 没有迁移全量 Cookbook（仅 F-AI-MEAL + F-MEAL 两个样例）
- [x] 没有修改产品运行行为（无生产代码/DB 变更）

## 20. 门禁（§44）

完成 Phase 1 后 **STOP**。不继续：迁移 Cookbook / 生成 AI_INDEX / 重写 07 / 重写功能路径索引 / 自动维护生命周期 / Git Hook / CI。**等待架构审核。**
