[toc]

# Project Graph Phase 2A END → Phase 2B PREVIEW / START 实施蓝图

## PG-P2-HANDOFF-2A-2B · Feature Universe Freeze → Current WorkItem Bootstrap

**任务类型**：Project Graph 子阶段交接 + 下一阶段实施
**执行模型**：DeepSeek V4 Flash
**上一阶段**：Phase 2A — Feature Universe + Source Provenance + CodeMapping Bootstrap
**Phase 2A 审核结论**：`ACCEPT`
**Phase 2A 审核提交**：`b54246c1cbbdbfeb76c2ea7b51784a06c22bbab8`
**下一阶段**：Phase 2B — Current WorkItem Bootstrap
**Graph Mode**：继续保持 `draft`
**生产代码修改**：禁止
**数据库修改**：禁止
**Generated Views**：禁止开始
**Phase 2C+**：禁止开始

---

# 0. 本任务采用标准阶段交接模式

本任务分成两个连续但职责严格分离的部分：

```text
PART A
Phase 2A END
↓
将外部架构 ACCEPT 落库
↓
冻结 2A 结果
↓
生成 2A→2B 交接记录
↓
END Gate

PART B
Phase 2B PREVIEW / START
↓
读取交接记录
↓
建立迁移 Inventory / Conflict Ledger
↓
迁移 Current WorkItems
↓
测试
↓
提交
↓
STOP
```

以后 Project Graph 子阶段统一采用：

```text
Phase N END
+
Handoff Record
+
Phase N+1 PREVIEW / START
```

的模式。

---

# 1. 开始前检查

执行：

```bash
git status
git rev-parse HEAD
```

记录：

```text
Execution Baseline:
<完整 SHA>
```

当前 HEAD 必须至少包含：

```text
b54246c1cbbdbfeb76c2ea7b51784a06c22bbab8
```

如果有后续提交：

* 不 reset；
* 不 checkout；
* 不 clean；
* 不覆盖用户修改；
* 先确认后续提交内容。

---

# 2. 本任务执行模型

本任务默认：

```text
DeepSeek V4 Flash
```

无需切换 V4 Pro / Luna / Terra。

原因：

> 2B 工作量较大，但主要属于已经有明确映射规则的数据归类、去重和状态迁移，决策密度可以通过本蓝图压低。

执行 AI 不承担 Project Graph 架构重设计责任。

---

# ============================================================

# PART A

# Phase 2A END

# ============================================================

# 3. Phase 2A END 的目标

外部架构审核已经确认：

```text
Phase 2A = ACCEPT
```

但仓库需要正式记录：

```text
Feature Universe
= ACCEPTED

source_refs
= ACCEPTED INTO V1 CONTRACT

CodeMapping Bootstrap
= ACCEPTED

Known 2A Blockers
= 0
```

同时要建立：

```text
2A → 2B Handoff
```

避免 Phase 2B 重新判断 Feature ownership。

---

# 4. 允许修改的治理文件

Phase 2A END 部分允许：

```text
.ai-context/project_graph/README.md

.ai-context/project_graph/project.yaml

.ai-context/project_graph/migration/PHASE2A_REVIEW.md
```

新增：

```text
.ai-context/project_graph/migration/PHASE2A_ACCEPT.md

.ai-context/project_graph/migration/PHASE2A_TO_2B_HANDOFF.md
```

如文件命名与现有 migration 规范有轻微冲突，可采用同义名称。

但语义必须保留：

```text
2A immutable accept record
+
2A → 2B handoff record
```

---

# 5. PHASE2A_ACCEPT.md

新增：

```text
.ai-context/project_graph/migration/PHASE2A_ACCEPT.md
```

性质：

> Immutable Architecture Review Record。

不是动态 Truth Source。

---

# 5.1 必须记录

```text
Project Graph:
Phase 2A — Feature Universe Bootstrap

Status:
ACCEPT

Review Commit:
b54246c1cbbdbfeb76c2ea7b51784a06c22bbab8

Known Blockers:
0

Graph Mode:
draft
```

---

# 5.2 Phase 2A 已完成能力

记录：

```text
Feature Registry:
13 Features

Feature Shards:
13 / 13

CodeMapping:
Bootstrapped

Exact Code Paths:
Validated

Match Rules:
Smoke Validated

Source Provenance:
source_refs accepted

source_refs Contract:
FROZEN INTO V1
```

---

# 5.3 source_refs 状态

必须明确：

```text
source_refs
```

是 Phase 2A 唯一经审核批准的 V1 Contract 增量。

从 Phase 2B 开始：

> 视为 Frozen Contract 一部分。

禁止 Phase 2B 因迁移方便再次修改其 Schema 结构。

---

# 6. README 状态更新

README 增加 Phase Progress。

建议：

```text
Phase 1
FINAL ACCEPT / FROZEN

Phase 2A
ACCEPT / CLOSED

Phase 2B
AUTHORIZED / STARTING

Graph Mode
draft
```

---

# 6.1 不要误写成整个 Phase 2 完成

禁止：

```text
Phase 2 = COMPLETE
```

正确：

```text
Phase 2A = ACCEPT

Phase 2
IN PROGRESS
```

---

# 7. project.yaml 治理注释

不得增加新的 Schema 字段。

只允许更新注释。

例如顶部：

```text
Phase 1 Model Contract: FINAL ACCEPT / FROZEN
Phase 2A Feature Universe: ACCEPT / CLOSED
Current Bootstrap Stage: Phase 2B
```

实际字段：

```yaml
mode: draft
```

保持不变。

---

# 8. PHASE2A_REVIEW.md 状态

在现有 Review 顶部或尾部增加：

```text
Architecture Review:
ACCEPT

Review Commit:
b54246c...

Status:
CLOSED
```

不得删除原来的边界判断。

尤其必须保留：

```text
F-PANTRY vs F-INGREDIENT
F-WEEKPLAN vs F-RECOMMEND
F-FAMILY vs F-HEALTH
F-TOOLS wide boundary
match overlap
```

这些是 Phase 2B Feature ownership 的输入。

---

# 9. 新增 Phase 2A → 2B Handoff

新增：

```text
.ai-context/project_graph/migration/PHASE2A_TO_2B_HANDOFF.md
```

这是以后 AI 执行 2B 的强制入口。

---

# 9.1 Handoff 必须包含 Feature Universe

完整记录：

```text
F-MEAL
F-AI-MEAL
F-TIMELINE
F-INGREDIENT
F-DISH
F-PANTRY
F-RECOMMEND
F-NUTRITION
F-HEALTH
F-FAMILY
F-WEEKPLAN
F-SYNC
F-TOOLS
```

并注明：

> Phase 2B 默认禁止新增、删除、重命名、合并 Feature。

---

# 9.2 Feature ownership frozen decisions

必须把 2A 已审核边界压缩写入 Handoff。

至少：

```text
库存 / Pantry
→ F-PANTRY

食材库 / Ingredient Metadata
→ F-INGREDIENT
```

```text
推荐 / 自由搭配 / Recommendation Engine
→ F-RECOMMEND

周计划 / 周期计划
→ F-WEEKPLAN
```

```text
家庭成员 / 健康档案数据
→ F-FAMILY

健康规则 / 慢病评价 / 健康算法
→ F-HEALTH
```

```text
厨房工具 / 采购 / 设置 / 导航 / 主题 /
通用组件 / 工程入口等暂归
→ F-TOOLS
```

---

# 9.3 F-TOOLS 特别规则

Phase 2B 不允许因为：

```text
F-TOOLS 很宽
```

就擅自创建：

```text
F-SHOPPING
F-COOKING
F-SETTINGS
F-INFRA
```

如果 WorkItem 迁移过程中发现确实无法合理归类：

记录：

```text
FEATURE_SPLIT_CANDIDATE
```

放入 Conflict Ledger。

不得自行拆 Feature。

---

# 9.4 Handoff 中记录 source_refs

必须注明：

```text
Entity → source_refs
```

已可用于：

```text
WorkItem
Plan
Verification
Feature
```

Phase 2B 每个迁移 WorkItem 原则上应提供：

```text
1~3 个 authoritative source_refs
```

---

# 10. Phase 2A END Gate

以下全部满足后 PART A 才完成。

### END-2A-01

Phase 2A ACCEPT 已落库。

### END-2A-02

Review Commit 正确：

```text
b54246c1cbbdbfeb76c2ea7b51784a06c22bbab8
```

### END-2A-03

source_refs 标记为 V1 Frozen Contract。

### END-2A-04

PHASE2A_REVIEW 状态 CLOSED。

### END-2A-05

新增 PHASE2A_ACCEPT。

### END-2A-06

新增 PHASE2A_TO_2B_HANDOFF。

### END-2A-07

13 个 Feature ownership 已传递到 Handoff。

### END-2A-08

Graph mode 仍：

```text
draft
```

### END-2A-09

Schema 无修改。

### END-2A-10

Validator 无修改。

---

# ============================================================

# PART B

# Phase 2B PREVIEW / START

# Current WorkItem Bootstrap

# ============================================================

# 11. Phase 2B 目标

Phase 2B 负责：

> 将 Cookbook 当前真正需要项目管理的 WorkItem 结构化迁入对应 Feature shard。

包括：

```text
Bug
Todo
Feature Work
Tech Debt
Refactor
Compliance
Research
Maintenance
```

重点：

```text
CURRENT PROJECT STATE
```

不是把所有历史 Markdown 全量搬进 YAML。

---

# 12. Phase 2B 开始前必须读取

第一组：Project Graph Contract

```text
.ai-context/project_graph/README.md

.ai-context/project_graph/project.yaml

.ai-context/project_graph/migration/PHASE1_FINAL_ACCEPT.md

.ai-context/project_graph/migration/PHASE2A_ACCEPT.md

.ai-context/project_graph/migration/PHASE2A_REVIEW.md

.ai-context/project_graph/migration/PHASE2A_TO_2B_HANDOFF.md
```

---

第二组：当前高优先级状态

```text
.ai-context/docs/context_memory/SESSION_交接.md

.ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

---

第三组：WorkItem Source

必须扫描：

```text
.ai-context/docs/feature/待办索引.md

.ai-context/docs/feature/待办_Bug修复.md

.ai-context/docs/feature/待办_功能算法.md
```

以及实际存在的其他：

```text
待办_UI*
待办_数据*
待办_工程*
待办_合规*
待办_战略*
```

不要假定文件名。

使用：

```bash
find .ai-context/docs/feature -maxdepth 1 -type f
```

或等价命令确认。

---

第四组：

对当前 ACTIVE / VERIFYING / PARKED WorkItem：

必须按需读取对应正式专项 Plan / 蓝图。

不要一次把所有 Plan 全部加载。

---

# 13. Truth Source Priority

Phase 2B 必须严格执行。

---

## 13.1 WorkItem 身份

回答：

> 这个 ID 到底是什么？

优先：

```text
正式专项蓝图 / 原始 WorkItem 定义
>
专项待办
>
待办索引
>
SESSION 简称
```

---

## 13.2 当前状态

回答：

> 现在处于什么阶段？

优先：

```text
SESSION
+
BLUEPRINT_STATE
+
最新 Verification 状态
>
专项当前记录
>
旧待办
>
旧索引
```

---

## 13.3 Feature ownership

优先：

```text
PHASE2A_TO_2B_HANDOFF
+
Feature CodeMapping
>
旧功能路径索引分类
```

---

# 14. 绝对禁止机械复制旧状态

例如旧待办：

```text
K1g B3待编码
```

而 SESSION：

```text
B1-B6 CODE+ARCH accepted
真机 pending
```

必须迁为：

```text
K1g = verifying
```

不能复制旧状态。

---

# 15. Phase 2B Migration Inventory

新增：

```text
.ai-context/project_graph/migration/PHASE2B_INVENTORY.md
```

先 Inventory，再迁移。

---

# 15.1 Inventory 不是 Truth

它只是：

> migration working record。

不得让未来 AI 默认读取它作为 Project Truth。

---

# 15.2 Inventory 每项格式

建议：

```text
SOURCE ITEM

source:
待办_Bug修复.md

source_label:
xxx

existing_id:
I8 / NONE

candidate_feature:
F-AI-MEAL

kind:
bug

candidate_status:
backlog

action:
MIGRATE / SKIP_HISTORY / MERGE_DUPLICATE / CONFLICT

target_id:
I8

notes:
...
```

---

# 16. 迁移范围

必须迁移：

### A. 当前未完成

```text
backlog
ready
in_progress
blocked
review
verifying
parked
```

---

### B. 当前主线最近完成，但仍解释当前状态

例如：

```text
L1
K1a
K1g
K1i
B1-B6
```

实际按现有 WorkItem ID结构决定。

---

### C. 当前 Verification / Plan 仍会引用

必须迁。

---

### D. 当前 SESSION / BLUEPRINT_STATE 明确引用

必须迁。

---

# 17. 不强制迁移

历史完成且：

* 不被当前 Plan 引用；
* 不被当前 Verify 引用；
* 不解释当前 Feature 状态；
* 不被 CurrentWork 引用；
* 没有架构意义；

可以：

```text
SKIP_HISTORY
```

---

# 18. Existing Stable IDs

已有：

```text
I*
J*
K*
L*
AF-*
其他仓库既有稳定 ID
```

全部保留。

禁止：

```text
K1g → TODO-AI-MEAL-001
```

这种重编号。

---

# 19. 无 ID WorkItem

只有确定：

> 当前仍需管理，且不存在已有稳定 ID。

才补 ID。

---

# 19.1 ID格式

Bug：

```text
BUG-<FEATURE>-NNN
```

Todo：

```text
TODO-<FEATURE>-NNN
```

Tech Debt：

```text
TECH-<FEATURE>-NNN
```

Refactor：

```text
REFACTOR-<FEATURE>-NNN
```

Compliance：

```text
COMP-<FEATURE>-NNN
```

Research：

```text
RESEARCH-<FEATURE>-NNN
```

Maintenance：

```text
MAINT-<FEATURE>-NNN
```

---

# 19.2 FEATURE部分

去掉：

```text
F-
```

例如：

```text
F-FAMILY
→ BUG-FAMILY-001
```

```text
F-AI-MEAL
→ TODO-AI-MEAL-001
```

---

# 19.3 编号

扫描整个 Graph。

同类型 + Feature：

```text
max(existing) + 1
```

禁止：

> 每次迁移重新从 001 排。

---

# 20. 去重是强制步骤

创建任何新 ID 前：

必须搜索：

```text
现有 Graph ID
现有 Graph title
待办索引
专项待办
Bug台账
SESSION
```

同一个问题可能被多个旧文档重复描述。

---

# 20.1 重复判定

如果以下核心语义相同：

```text
同 Feature
+
同问题对象
+
同预期结果
```

通常视为同 WorkItem。

---

# 20.2 不因标题不同就拆两个

例如：

```text
家庭成员年龄异常
```

和：

```text
家庭档案年龄计算错误
```

不能只因标题不同就创建两个 Bug。

必须读具体描述。

---

# 21. WorkItem kind 映射

严格按：

```text
产品/业务 Bug
→ bug

新能力
→ feature

普通产品待办
→ todo

架构/技术债
→ tech_debt

纯重构
→ refactor

隐私/合规
→ compliance

探索/调研
→ research

工程维护
→ maintenance
```

无法判断：

```text
todo
```

并记录 Inventory note。

不要擅自用：

```text
feature
```

扩大范围。

---

# 22. WorkItem 状态映射

这是 2B 核心。

---

## backlog

明确存在，但未进入实施。

---

## ready

Plan / Blueprint 已 accepted，可立即实施，但尚未编码。

---

## in_progress

正在实施。

---

## blocked

明确被外部条件阻塞。

注意：

```text
暂时不优先
```

不等于 blocked。

---

## review

代码已交，当前等待 CODE / ARCH review。

---

## verifying

实现与必要 review 已完成，但 required verification 未闭环。

这是当前 Cookbook 很重要的状态。

---

## done

必须：

```text
全部 required Verification
= pass / not_required
```

Phase 2B 尚未迁完整 Verification 时：

> 不得仅凭旧文档“已完成”就大量写 done。

---

## parked

明确暂缓、后续再拾起。

例如真正的：

```text
DRAFT·PARKED
```

---

## cancelled

明确不再做。

---

# 23. CODE+ARCH ACCEPTED 特别规则

```text
CODE ACCEPTED
+
ARCH ACCEPTED
+
required device pending
```

统一：

```text
verifying
```

不是：

```text
done
```

---

# 24. Phase 2B 与 Verification 的边界

Phase 2D 才进行完整 Verification Bootstrap。

因此 2B：

> 不迁移完整真机清单。

---

# 24.1 已存在 PoC Verification

例如：

```text
F-AI-MEAL
```

中现有验证继续保留。

---

# 24.2 新 WorkItem status 判断需要 Verification 时

可以通过最新验证清单判断其状态。

但：

> 不要把所有 E-* 条目提前写进 Graph。

只决定 WorkItem status。

---

# 24.3 如果 done 无法满足现有 Closure Validator

不要为了过 Validator：

```text
伪造 Verification
```

正确处理：

如果缺少 required Verification 结构化数据：

```text
status = verifying
```

Phase 2D 再完成闭环。

---

# 25. Phase 2B 与 Plan 的边界

Phase 2C 才全量迁 Plan。

所以：

> 2B 不大量创建 Plan。

---

# 25.1 已存在 Plan

保持。

---

# 25.2 WorkItem 明确已有正式 Plan 但 Graph 尚未迁

WorkItem 不需要为了表达计划而临时创建假 Plan。

使用：

```text
source_refs
```

指向正式蓝图。

Phase 2C 再创建 Plan node。

---

# 26. source_refs 施工规则

每个迁移 WorkItem 尽量写：

```yaml
source_refs:
  - ...
```

通常 1~3 条。

---

# 26.1 推荐顺序

第一条：

> WorkItem 身份最权威来源。

第二条：

> 当前状态最权威来源。

例如：

```yaml
source_refs:
  - .ai-context/docs/feature/待办_功能算法.md#K1g
  - .ai-context/docs/context_memory/SESSION_交接.md
```

---

# 26.2 不要堆文档

禁止：

```text
所有提到这个 WorkItem 的文件全部塞进去。
```

---

# 27. Feature Shard 规则

WorkItem 必须存放：

```text
features/<PRIMARY-FEATURE>.yaml
```

并且：

```yaml
feature: <PRIMARY-FEATURE>
```

一致。

---

# 27.1 跨 Feature 事项

选择一个 Primary Feature。

判断：

> 哪个 Feature 的用户/业务能力是该 WorkItem 的主要交付对象？

其它 Feature：

Phase 2C 使用：

```text
affects
related_to
depends_on
```

表达。

2B 不为了跨 Feature 建重复 WorkItem。

---

# 28. Feature ownership Decision Table

Phase 2A 已冻结以下典型规则：

```text
食材属性、分类、care
→ F-INGREDIENT

库存、Pantry
→ F-PANTRY

菜品数据与菜品维护
→ F-DISH

记餐核心
→ F-MEAL

AI快捷记餐
→ F-AI-MEAL

时间轴
→ F-TIMELINE

推荐
→ F-RECOMMEND

周计划
→ F-WEEKPLAN

营养统计/评估
→ F-NUTRITION

健康规则/慢病判断
→ F-HEALTH

家庭成员/健康档案
→ F-FAMILY

备份/恢复/同步
→ F-SYNC

工具/采购/厨房/设置/通用App能力
→ F-TOOLS
```

---

# 29. Conflict Ledger

新增：

```text
.ai-context/project_graph/migration/PHASE2B_CONFLICTS.md
```

即使最后无冲突也保留。

---

# 29.1 允许的 Conflict 类型

```text
STATE_CONFLICT
```

例如 SESSION 和高优先级 Blueprint 真正冲突。

---

```text
DUPLICATE_UNCERTAIN
```

不知道两条旧待办是否同一个问题。

---

```text
FEATURE_OWNERSHIP_UNCERTAIN
```

两个 Feature 都合理。

---

```text
ID_COLLISION
```

已有 ID 语义疑似冲突。

---

```text
FEATURE_SPLIT_CANDIDATE
```

现有 13 Feature 无法合理承载。

---

```text
SOURCE_MISSING
```

旧索引提到事项，但找不到足够原始定义。

---

# 29.2 No Guess Rule

出现 Conflict：

> 不允许静默猜。

如果该项不影响其他迁移：

记录 Conflict，继续其它事项。

如果会污染稳定 ID：

跳过该 Item。

---

# 30. Phase 2B Inventory 完成后才允许写 Graph

流程严格：

```text
Scan
↓
Inventory
↓
Deduplicate
↓
Resolve obvious ownership
↓
Create Conflict Ledger
↓
Review inventory internally
↓
Write Feature YAML
```

禁止：

```text
读一条
写一条
```

否则容易重复。

---

# 31. Inventory 内部自检

开始写 Graph 前至少检查：

```text
candidate count
duplicate count
skip history count
conflict count
new ID count
existing ID count
```

记录到 PHASE2B_INVENTORY.md。

---

# 32. 迁移顺序

为了降低跨文件冲突，按 Feature 顺序迁移：

```text
1  F-MEAL
2  F-AI-MEAL
3  F-TIMELINE
4  F-INGREDIENT
5  F-DISH
6  F-PANTRY
7  F-RECOMMEND
8  F-NUTRITION
9  F-HEALTH
10 F-FAMILY
11 F-WEEKPLAN
12 F-SYNC
13 F-TOOLS
```

这只是施工顺序。

不代表优先级。

---

# 33. F-AI-MEAL 特别要求

已有：

```text
K1g
K1i
K1b
I8
```

不得重复创建。

先以现有 Graph 为基础。

---

# 33.1 K1b

如果当前 Truth 仍：

```text
DRAFT·PARKED
```

保持：

```text
parked
```

---

# 33.2 K1g / K1i

按当前高优先级状态源迁移。

如果仍 required device pending：

```text
verifying
```

不要回退成旧待办状态。

---

# 34. 历史完成项过滤

对：

```text
done
```

历史项：

只有以下任一成立才迁：

```text
current state explanation
current Plan references it
current Verification references it
current WorkItem depends on it
major architecture significance
SESSION/BLUEPRINT explicitly references it
```

否则：

```text
SKIP_HISTORY
```

---

# 35. 不修改旧待办

即使发现：

```text
待办索引
```

过期：

本阶段也不修。

只把正确事实迁进 Graph。

旧动态 View Phase 3 再替换。

---

# 36. Phase 2B 不允许修改 Frozen Schema

禁止修改：

```text
project-graph.schema.json
```

禁止修改：

```text
WorkItem enums
Relation enums
Feature lifecycle
Verification states
source_refs schema
```

---

# 36.1 如果真实 WorkItem 无法表达

记录：

```text
ARCH_CHANGE_REQUIRED
```

停止该 Item。

其它可继续。

最终报告。

不要自己修改 Schema。

---

# 37. Validator

正常情况下：

> Phase 2B 不修改 Validator。

如果数据暴露 Validator 真 Bug：

不要顺手修。

记录：

```text
VALIDATOR_BUG_CANDIDATE
```

最终报告。

等待架构审核。

---

# 38. Migration Test

迁移完成后运行：

```text
全部 Project Graph tests
```

必须 PASS。

---

# 39. pg check

执行：

```bash
python .ai-context/project_graph/tools/project_graph.py check
```

要求：

```text
PASS
0 issue
```

---

# 40. WorkItem统计

写一个临时统计脚本或直接使用 Python。

最终报告：

```text
Total WorkItems

By Feature

By Kind

By Status

Existing-ID Migrated

New-ID Assigned

Skipped Historical

Conflicts
```

不得在 Graph 中存这些 Count。

这是 Derived / migration summary。

---

# 41. 稳定 ID 冲突检查

必须检查：

```text
全 Graph WorkItem ID uniqueness
```

Validator已有部分能力。

再确认：

> 新生成 ID 没覆盖现有 I/J/K/L。

---

# 42. source_refs smoke

所有新迁 WorkItem：

至少检查：

```text
source_refs path exists
```

由 Validator 完成。

---

# 43. Phase 2B Gate

以下全部通过才能提交。

### GATE-2B-01

2A END 已落库。

### GATE-2B-02

2A→2B Handoff 已创建。

### GATE-2B-03

Inventory 已完成。

### GATE-2B-04

Conflict Ledger 已建立。

### GATE-2B-05

当前重要 WorkItem 已迁移。

### GATE-2B-06

已有稳定 ID 全保留。

### GATE-2B-07

无 ID 当前事项按规则分配稳定 ID。

### GATE-2B-08

没有同一问题重复建 WorkItem。

### GATE-2B-09

WorkItem primary Feature 与 shard 一致。

### GATE-2B-10

Phase 2A ownership 决策未被擅自推翻。

### GATE-2B-11

旧状态没有机械复制覆盖新状态。

### GATE-2B-12

CODE+ARCH accepted + device pending → verifying。

### GATE-2B-13

没有伪造 Verification 让 done 通过。

### GATE-2B-14

没有提前批量迁 Plan。

### GATE-2B-15

没有提前迁完整 Verification 清单。

### GATE-2B-16

source_refs 使用正确。

### GATE-2B-17

历史噪音没有全量搬入 Graph。

### GATE-2B-18

Schema 未修改。

### GATE-2B-19

Validator Contract 未修改。

### GATE-2B-20

Graph tests PASS。

### GATE-2B-21

pg check PASS。

### GATE-2B-22

生产代码 0 修改。

### GATE-2B-23

Graph mode 仍 draft。

### GATE-2B-24

Phase 2C NOT STARTED。

---

# 44. 本次提交策略

本任务允许 **一个执行会话、两个逻辑提交**。

推荐：

---

## Commit A — Phase 2A END / Handoff

先完成 PART A。

Commit：

```text
docs(project-graph): close phase 2a and hand off phase 2b
```

---

## Commit B — Phase 2B Bootstrap

完成 PART B。

Commit：

```text
feat(project-graph): bootstrap current workitems phase 2b
```

这样 Git 历史中：

```text
2A结束
↓
清晰交接
↓
2B数据迁移
```

可独立追踪。

---

# 44.1 为什么要求两个 Commit

因为以后需要能够明确回答：

```text
Phase 2A 到底在哪个 commit 正式关闭？

Phase 2B 从哪里正式开始？
```

不能把：

```text
上一阶段接受记录
+
下一阶段大量迁移
```

全部揉进一个 commit。

---

# 45. Push

两个 commit 都完成以后统一 push 可以接受。

但 Git history 必须仍然有两个独立 commit。

---

# 46. Diff Gate

最终执行：

```bash
git status
git diff <BASELINE>..HEAD --stat
```

确认：

允许范围主要为：

```text
.ai-context/project_graph/**
```

禁止生产代码。

---

# 47. Phase 2B 最终汇报格式

严格输出：

```text
Project Graph Transition:
Phase 2A END
→
Phase 2B START

Execution Baseline:
<sha>

Phase 2A End Commit:
<sha>

Phase 2B Completion Commit:
<sha>

====================
PHASE 2A END
====================

Phase 2A Status:
ACCEPT / CLOSED

Review Commit:
b54246c1cbbdbfeb76c2ea7b51784a06c22bbab8

Feature Universe:
13

source_refs:
ACCEPTED / FROZEN

Handoff:
.ai-context/project_graph/migration/PHASE2A_TO_2B_HANDOFF.md

====================
PHASE 2B
====================

Inventory:
<path>

Conflict Ledger:
<path>

Candidate WorkItems:
<count>

Migrated WorkItems:
<count>

Existing IDs:
<count>

New IDs:
<count>

Skipped Historical:
<count>

Conflicts:
<count>

By Feature:
F-MEAL:
<count>
...

By Kind:
bug:
...
todo:
...

By Status:
backlog:
...
verifying:
...

Open Conflicts:
NONE / <list>

Feature Split Candidates:
NONE / <list>

Architecture Change Required:
NO / <details>

Schema Changed:
NO

Validator Contract Changed:
NO

Plan Bulk Migration:
NOT STARTED

Verification Bulk Migration:
NOT STARTED

Production Code Changed:
NO

Graph Mode:
draft

Tests:
Command:
<actual>

Result:
<actual>

PG Check:
Command:
python .ai-context/project_graph/tools/project_graph.py check

Result:
PASS / 0 issue

Phase 2C:
NOT STARTED

Deviations:
NONE / <details>

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 48. 完成后 STOP

完成 PART A + PART B 两个 commit 并 push 后：

> STOP。

绝对禁止继续：

```text
Phase 2C
```

即使：

* 已发现所有 Plan；
* 已打开所有蓝图；
* 已看见 Verification；
* Conflict = 0。

也不得继续。

---

# 49. 后续标准交接模式

从本任务开始，后续固定采用：

```text
Phase 2B END
+
2B→2C Handoff
+
Phase 2C PREVIEW / START
```

然后：

```text
Phase 2C END
+
2C→2D Handoff
+
Phase 2D PREVIEW / START
```

然后：

```text
Phase 2D END
+
2D→2E Handoff
+
Phase 2E PREVIEW / START
```

每个交接：

1. 落库上一阶段 ACCEPT；
2. 冻结上一阶段已确认决策；
3. 创建 Handoff；
4. 明确下一阶段输入；
5. 下一阶段施工；
6. STOP；
7. 外部审核。

---

# 50. 本任务最终目标

完成后仓库必须表达：

```text
Phase 1
FINAL ACCEPT / FROZEN

Phase 2A
ACCEPT / CLOSED

Phase 2B
IMPLEMENTED
WAITING FOR ARCHITECTURE REVIEW

Phase 2C
NOT STARTED

Graph Mode
draft
```

且 Project Graph 中：

```text
Feature Universe
+
Current WorkItems
```

已经形成第一版可信 Project Truth 数据基础。
