[toc]

# Project Graph Phase 2A 实施蓝图

## Feature Universe + Source Provenance + Code Mapping Bootstrap

**执行批次**：Phase 2A ONLY
**执行模型**：DeepSeek V4 Flash
**前置要求**：Phase 1 必须已经获得 `FINAL ACCEPT`
**禁止执行**：Phase 2B / 2C / 2D / 2E
**Graph mode**：保持 `draft`
**生产代码修改**：禁止

---

# 0. 本批目标

Phase 2A 只做三件事：

1. 冻结 Cookbook 第一版 Feature Universe；
2. 给每个 Feature 创建真实 Feature shard；
3. 增加最小 `source_refs` provenance 能力并建立 Feature 级 CodeMapping。

本批不迁移大量：

```text
Bug
Todo
Plan
Verification
```

F-AI-MEAL / F-MEAL 现有 PoC 数据保留，只做必要一致性调整。

---

# 1. 前置检查

执行：

```bash
git status
git rev-parse HEAD
```

确认：

1. 工作区无未知用户修改；
2. 当前 HEAD 已包含 Phase 1 最终修复；
3. `.ai-context/project_graph/features/F-AI-MEAL.yaml` 中：

```text
E-K1I-02
kind=device
status=pending
```

4. Project Graph tests PASS；
5. `pg check` PASS。

任何一条不满足：

> STOP，不开始 Phase 2A。

---

# 2. 必须读取

首先读取：

```text
.ai-context/project_graph/README.md
.ai-context/project_graph/project.yaml
.ai-context/project_graph/schema/project-graph.schema.json
.ai-context/project_graph/features/F-AI-MEAL.yaml
.ai-context/project_graph/features/F-MEAL.yaml
```

再读取静态项目地图：

```text
.ai-context/PROJECT.md
.ai-context/docs/projectReview/00_导读与索引.md
.ai-context/docs/功能路径索引.md
```

注意：

`功能路径索引.md`

本批只用于：

> 发现 Feature → Code 候选。

其中任何：

```text
B3完成
B4待做
测试数量
当前阶段
```

动态状态描述全部忽略。

动态状态不是本批工作。

---

# 3. Feature Registry 基线

当前 Registry：

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

本批原则：

> 默认保留全部现有 ID。

不得：

* rename；
* delete；
* merge；

除非发现明确结构性错误。

---

# 4. 新增 Feature 的严格条件

只有同时满足：

### C1

代表长期稳定产品/系统能力。

### C2

有独立代码边界或主要业务路径。

### C3

无法合理归入当前 13 个 Feature。

才允许新增。

不得因为：

```text
一个页面
一个ViewModel
一个Bug
一个方案
```

就创建 Feature。

---

# 5. 如果认为需要新增 Feature

不要直接添加。

先写入：

```text
.ai-context/project_graph/migration/PHASE2A_REVIEW.md
```

格式：

```text
PROPOSED_FEATURE

id:
F-XXX

name:
...

reason:
...

cannot_belong_to:
F-A / F-B

code_evidence:
...

source_evidence:
...
```

本批仍继续能完成的其他工作。

最终报告中标记：

```text
ARCH_REVIEW_REQUIRED
```

不得自行扩大 Feature Universe。

---

# 6. Feature Shard

为 Registry 中每个 Feature 创建：

```text
features/<FEATURE-ID>.yaml
```

例如：

```text
features/F-NUTRITION.yaml
```

最低内容：

```yaml
kind: feature
id: F-NUTRITION
name: 营养评估
lifecycle: active

match:
  - ...

code:
  domain:
    - ...
```

---

# 7. Feature lifecycle 判定

本批只判长期生命周期。

不要判断 activity。

规则：

```text
尚未实现，仅规划
→ planned

已有真实可用实现，仍持续发展
→ active

核心能力稳定成熟，以维护为主
→ mature

明确废弃
→ deprecated
```

禁止：

```text
verifying
developing
blocked
```

这些不是 lifecycle。

---

# 8. CodeMapping 施工规则

每个 Feature 至少建立：

```text
match
+
code
```

但不要求列出 Feature 的所有文件。

目标：

> 低 Token 的高价值导航。

---

# 8.1 code 只收关键路径

优先：

```text
entry
ui
viewmodel
domain
data
core
tests
other
```

每个 category：

只列：

> 关键入口 / 核心实现 / 代表性测试。

不要列整个目录几十个文件。

---

# 8.2 文件必须真实存在

每条：

```text
code.*
```

必须：

* repo-relative；
* 文件真实存在；
* 不用 `...`；
* 不用裸类名；
* 不用目录代替文件；
* 不用 CSV。

---

# 8.3 match 用于反向定位

match 应覆盖该 Feature 的主要代码区域。

例如：

```yaml
match:
  - shared/**/nutrition/**
  - androidApp/**/nutrition/**
```

但：

> 不要写过宽 glob。

例如禁止：

```text
shared/**
androidApp/**
```

这种会匹配半个项目的规则。

---

# 8.4 Feature 重叠

一个文件可能被多个 Feature 使用。

`code` 精确路径允许重复。

但 `match` 如产生大面积 Feature 重叠：

记录：

```text
MATCH_OVERLAP
```

在 PHASE2A_REVIEW 中。

不要擅自创建复杂 priority 机制。

---

# 9. source_refs Schema 最小加法

本批允许唯一核心 Schema 扩展：

```text
source_refs
```

---

# 9.1 增加到以下实体

```text
Feature
WorkItem
Plan
Verification
```

字段：

```json
"source_refs": {
  "type": "array",
  "items": {
    "type": "string",
    "minLength": 1
  }
}
```

可选。

---

# 9.2 source_refs 示例

```yaml
source_refs:
  - .ai-context/docs/feature/K1i_AI流式渐进展示_实施蓝图.md
```

或：

```yaml
source_refs:
  - .ai-context/docs/feature/待办_Bug修复.md#I8
```

---

# 9.3 source_refs 只放权威来源

不要：

```text
一个实体被20个文档提到
→ 全部写进去
```

通常：

```text
1~3 条
```

即可。

---

# 9.4 Source Ref Validator

Semantic Validator 增加轻量检查：

对于仓库内部 source ref：

1. 去除 `#anchor`；
2. path 不得绝对；
3. path 不得含 `..` 逃逸 repo；
4. 文件必须存在。

Anchor：

本阶段不验证。

---

# 9.5 错误码

增加：

```text
PG-E-SOURCE_REF
```

错误必须包含：

```text
entity
source_ref
reason
```

---

# 9.6 测试

至少：

```text
valid relative source
→ PASS

path#anchor
→ PASS

missing source
→ FAIL

absolute source
→ FAIL

../escape
→ FAIL
```

---

# 10. Feature source_refs

每个 Feature 可以有 1~2 个静态权威来源。

例如：

```text
功能路径索引
项目业务地图
```

但本批重点仍然是：

> Code。

source_refs 不需要为了凑数全部填写。

---

# 11. 本批禁止迁移大量 WorkItem

除现有：

```text
F-AI-MEAL
```

PoC 已经存在的：

```text
K1g
K1i
K1b
I8
```

外：

不要开始把：

```text
待办_Bug修复
待办_UI交互
待办_数据健康
...
```

逐条写入 Feature YAML。

那是 Phase 2B。

---

# 12. 本批禁止迁移 Verification

不要迁移：

```text
97项真机
17项已通过
```

不要新建：

```text
E-L1
E-B4
F1
P0
```

那是 Phase 2D。

---

# 13. 本批禁止迁移 Plan

除了为了保持现有 PoC 合法：

不要大量创建：

```text
PLAN-K1A
PLAN-K1I
PLAN-L1
```

那是 Phase 2C。

---

# 14. Migration Review 文件

允许创建：

```text
.ai-context/project_graph/migration/PHASE2A_REVIEW.md
```

只记录：

```text
Proposed Feature
Ambiguous Code Ownership
Match Overlap
Missing Code Path
Static Map Conflict
```

如果无问题：

仍可创建并写：

```text
NO OPEN ISSUE
```

这是迁移审计文档。

不是 Project Truth。

---

# 15. 代码路径核验

完成 Feature files 后运行一个临时路径检查。

对所有：

```text
features/*.yaml
code.*
```

确认：

```text
missing exact path = 0
```

临时脚本不必提交。

---

# 16. Match Smoke Check

对每个 Feature：

至少人工/脚本确认：

```text
match
```

能匹配到一个预期代码文件。

如果某 Feature 当前：

```text
planned
```

且确实没有代码：

可以没有 match/code。

但必须在 Review 中说明。

---

# 17. Registry 完整性

Phase 2A 完成后：

```text
project.yaml features registry
```

中的每个 Feature：

必须存在：

```text
features/<id>.yaml
```

虽然：

```text
mode: draft
```

仍然如此。

---

# 18. Schema 不得顺手扩展其他能力

除了：

```text
source_refs
```

禁止新增：

```text
owner
tags
status_description
progress
history
updated
created
milestone
team
percentage
```

本批不要重新设计 Graph。

---

# 19. README 更新

因为新增通用：

```text
source_refs
```

README 必须增加一小节：

```text
Source Provenance
```

说明：

```text
Entity
→ source_refs
→ authoritative repository artifact
```

同时明确：

> source_refs 不改变 Truth Source 优先级，只记录证据来源。

不要大改 README 其他 Contract。

---

# 20. 测试

必须运行：

```text
全部 Project Graph tests
```

以及：

```text
pg check
```

必须 0 issue。

---

# 21. Phase 2A Gate

### GATE-2A-01

Registry 每个 Feature 都有 shard。

### GATE-2A-02

现有 Feature ID 未擅自 rename/delete。

### GATE-2A-03

新增 Feature 如有，必须只提案、不擅自落地。

### GATE-2A-04

所有 code exact path 真实存在。

### GATE-2A-05

不存在裸类名 CodeMapping。

### GATE-2A-06

不存在 `...` 缩略 path。

### GATE-2A-07

每个 active/mature Feature 至少有可用 CodeMapping 或明确解释。

### GATE-2A-08

match 不存在明显全仓过宽规则。

### GATE-2A-09

source_refs Schema/Validator/tests 完成。

### GATE-2A-10

除 source_refs 外 Schema 无新增能力。

### GATE-2A-11

没有批量迁移 WorkItem。

### GATE-2A-12

没有迁移 Verification。

### GATE-2A-13

没有批量迁移 Plan。

### GATE-2A-14

Graph mode 仍为 draft。

### GATE-2A-15

全部 tests PASS。

### GATE-2A-16

pg check PASS。

### GATE-2A-17

生产代码 0 修改。

### GATE-2A-18

Phase 2B NOT STARTED。

---

# 22. Commit

建议：

```text
feat(project-graph): bootstrap phase 2a feature universe
```

提交后 push。

---

# 23. 最终汇报

严格输出：

```text
Project Graph:
Phase 2A — Feature Universe Bootstrap

Baseline:
<sha>

Completion:
<sha>

Feature Registry:
<count>

Feature Files:
<list>

Existing Feature IDs Changed:
NONE / <details>

Proposed New Features:
NONE / <details>

Source Refs:
Schema:
<summary>

Validator:
<summary>

Tests Added:
<summary>

Code Mapping:
Exact Paths:
<count>

Missing Paths:
0

Match Smoke:
<summary>

Migration Review:
<path>
Open Issues:
NONE / <list>

Schema Changes:
source_refs ONLY

WorkItem Bulk Migration:
NOT STARTED

Plan Bulk Migration:
NOT STARTED

Verification Migration:
NOT STARTED

Production Code Changed:
NO

Tests:
<command>
<result>

PG Check:
<command>
<result>

Graph Mode:
draft

Phase 2B:
NOT STARTED

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 24. STOP

完成后必须 STOP。

禁止因为已经读取：

```text
待办
蓝图
验证清单
```

就继续把它们迁进 Graph。

Phase 2B 必须等待独立架构复审。

