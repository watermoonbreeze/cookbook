[toc]

# Project Graph Phase 1 Final Data Cleanup 实施蓝图

## PG-P1-FDC · Contract Frozen / Data Truth Cleanup

**蓝图类型**：Phase 1 最终数据收口
**执行模型**：DeepSeek V4 Flash 可直接执行
**审核基准提交**：`7aa8c3d138ef226c30ab2c4ad83b744f231ff733`
**当前架构状态**：`CONTRACT ACCEPTED`
**本轮目标**：清除 Draft PoC Graph 中的事实错误，使 Phase 1 达到 `FINAL ACCEPT` 条件
**允许修改范围**：仅 `.ai-context/project_graph/` 内的 PoC 数据及必要测试
**Schema 修改**：禁止
**Validator 架构修改**：禁止
**生产代码修改**：禁止
**Phase 2**：禁止开始

---

# 0. 本轮任务性质

当前 Project Graph 的：

* 核心实体模型；
* JSON Schema；
* Relation Model；
* Typed Reference；
* 状态机；
* Verification Closure；
* Feature Sharding；
* YAML Fail Closed；
* Duplicate Detection；
* CodeMapping 数据结构；
* Declared / Observed / Derived；
* Validator 架构；

已经通过架构审核。

因此：

> 本轮不是架构重构。

本轮只处理：

> Draft Project Graph 中已经写入但与 Cookbook 当前真实项目状态不一致的 PoC 数据。

核心原则：

```text
Schema Correct
≠
Project Fact Correct
```

Phase 1 最后一步必须保证：

> 即使 Graph 还是 `mode: draft`，其中出现的 Cookbook 实体和事实也必须真实可信。

---

# 1. 开始前检查

执行：

```bash
git status
git rev-parse HEAD
```

审核基准应为：

```text
7aa8c3d138ef226c30ab2c4ad83b744f231ff733
```

如果实际 HEAD 已有后续提交：

* 不 reset；
* 不 checkout 覆盖；
* 记录实际 baseline；
* 检查后续提交是否与本任务相关。

如果存在用户未提交修改：

> 不得清理、覆盖、stash 或 reset。

---

# 2. 本轮必须读取的 Truth Source

本轮不需要重新全仓扫描。

必须读取：

```text
.ai-context/project_graph/project.yaml
.ai-context/project_graph/features/F-AI-MEAL.yaml
.ai-context/project_graph/features/F-MEAL.yaml
```

同时读取 Cookbook 当前高优先级状态源：

```text
.ai-context/docs/context_memory/SESSION_交接.md
.ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

读取与 K1g / K1i 相关的真实专项方案：

```text
在 .ai-context/docs/feature/ 中搜索：
K1g
K1i
NDJSON
流式运行时
真实委托
周期记
```

并读取实际匹配到的：

* K1g / B1-B6 方案；
* K1i 方案；
* 必要的待办原始定义。

还需要读取：

```text
.ai-context/docs/feature/待办_功能算法.md
```

如果当前项目已有更新后的统一 WorkItem 索引，则优先读取最新高优先级状态源。

---

# 3. Truth Source 优先级

本轮判断事实时按以下优先级：

```text
真实代码
>
SESSION_交接 / BLUEPRINT_STATE 当前状态
>
专项正式方案 / 蓝图
>
最新待办状态
>
旧索引 / 历史描述
```

注意：

> 不允许因为某个旧待办文档状态过期，就直接把其中所有内容判为无效。

WorkItem 的“身份和业务含义”仍可参考旧文档；

WorkItem 的“当前状态”必须以当前高优先级状态源为准。

---

# 4. 本轮修改项总览

只处理以下 5 项。

| ID        | 内容                            | 类型 |
| --------- | ----------------------------- | -- |
| PG-FDC-01 | F-MEAL CodeMapping 改真实路径      | 必修 |
| PG-FDC-02 | 修正 K1g 的真实业务含义                | 必修 |
| PG-FDC-03 | 修正 K1g 真机 Verification 状态     | 必修 |
| PG-FDC-04 | 核实并修正 PLAN-AI-NDJSON 与 K1i 关系 | 必修 |
| PG-FDC-05 | 对 F-AI-MEAL PoC 做一次事实一致性审计    | 必修 |

不得超出这些范围。

---

# 5. PG-FDC-01

# F-MEAL CodeMapping 改为真实 repo-relative path

## 5.1 当前问题

当前：

```text
.ai-context/project_graph/features/F-MEAL.yaml
```

中仍可能存在：

```yaml
code:
  ui:
    - AddDayFoodScreen
  viewmodel:
    - AddMealViewModel
  data:
    - MealRecordRepository
  domain:
    - MealRecord
```

这些是：

> 类名 / 符号名。

不是当前 Project Graph Contract 要求的：

> 真实 repo-relative file path。

---

# 5.2 修改原则

必须在真实仓库中查找对应文件。

优先使用：

```bash
rg --files | rg "AddDayFoodScreen|AddMealViewModel|MealRecordRepository|MealRecord"
```

或等价命令。

不得根据类名猜路径。

---

# 5.3 CodeMapping 格式

最终应类似：

```yaml
code:
  ui:
    - androidApp/src/.../AddDayFoodScreen.kt

  viewmodel:
    - androidApp/src/.../AddMealViewModel.kt

  data:
    - shared/src/.../MealRecordRepository.kt

  domain:
    - shared/src/.../MealRecord.kt
```

以上只是格式示例。

实际必须使用仓库真实存在的路径。

---

# 5.4 路径规则

每条必须满足：

* 相对 repository root；
* 使用 `/`；
* 不使用绝对路径；
* 不使用 `...`；
* 不使用单纯类名；
* 不使用逗号拼接多个文件；
* 文件必须真实存在。

---

# 5.5 不要过度扩展

本轮不是重新建立 F-MEAL 完整 Code Map。

只需要：

> 把现有 PoC 中已经声明的 CodeMapping 修成真实路径。

如果发现某个现有类名实际已经不存在：

* 不猜新类；
* 根据当前真实功能路径找到替代实现；
* 如果无法可靠判断，删除该条 PoC mapping，并在最终汇报说明。

---

# 5.6 验收

Graph 内：

```text
F-MEAL.code.*
```

不得残留：

```text
AddMealViewModel
MealRecord
...
```

这种无路径裸符号。

---

# 6. PG-FDC-02

# 修正 K1g 的真实业务含义

## 6.1 当前问题

当前 F-AI-MEAL PoC 中，K1g 可能被描述为：

```text
SwitchableAiRuntime.stream() 真实委托
```

这与当前 Cookbook WorkItem 身份不一致。

当前已有资料显示：

```text
K1g
```

主要对应：

> AI 记餐周期记 + NDJSON 流式解析/协议改造，以及 B1-B6 主线。

而：

```text
K1i
```

才对应：

> AI 输出流式运行时 / 真实委托相关能力。

---

# 6.2 必须重新核实

执行 AI 必须通过真实文档确认：

```text
K1g 的正式标题
K1g 的业务范围
K1i 的正式标题
K1i 的业务范围
```

不得只照本蓝图中的概括直接写。

本蓝图给出的是：

> 审核发现的方向。

最终字段必须以仓库真实蓝图为准。

---

# 6.3 修改范围

只修改 K1g PoC 节点中：

* title；
* brief / intent；
* 可能存在的错误 Plan relation；
* 与真实业务含义直接相关的 PoC 字段。

---

# 6.4 禁止

不得：

* 改 K1g ID；
* 改 K1i ID；
* 合并 K1g/K1i；
* 创建新 WorkItem 替代；
* 顺手迁移 B1-B6；
* 把整个真实 K1g 内容全量迁进 Graph。

Phase 2 才做全量迁移。

---

# 6.5 目标

PoC 中至少保证：

```text
K1g 的名称
```

不会再误写成：

```text
K1i 的真实委托工作
```

---

# 7. PG-FDC-03

# 修正 K1g 真机 Verification

## 7.1 当前问题

当前 F-AI-MEAL PoC 可能存在：

```yaml
E-K1G-01:
  kind: device
  status: pass
  work_item: K1g
```

但当前项目高优先级状态源明确表明：

> AI 记一餐相关主线虽然已经完成 CODE/ARCH 审核，但仍存在大量未完成真机验证。

因此：

```text
device = pass
```

不能作为真实 Project Fact 保留。

---

# 7.2 必须核实

读取：

```text
SESSION_交接.md
BLUEPRINT_STATE.md
最新真机验证清单
```

如果当前高优先级资料仍表明：

> K1g / B1-B6 没有完成对应真机验证，

则：

```yaml
status: pending
```

或按照当前 Verification Model 中与真实事实一致的状态。

---

# 7.3 不允许猜测 Verification ID 与粒度

注意：

当前 `E-K1G-01` 本身可能只是 PoC ID。

本轮不要求把真实 B1-B6 所有真机条目迁进 Graph。

因此两种合法处理方式：

### 方案 A — 保留 PoC Verification

如果它能明确表示：

> K1g 的“真机闭环尚未完成”

则：

```yaml
status: pending
```

可以保留。

---

### 方案 B — 删除不真实的 PoC Verification

如果：

```text
E-K1G-01
```

无法对应任何明确真实验证事实，

则可以直接删除这个 PoC Verification。

但必须确保：

> 删除后不会因为 K1g.status=done 与当前 Verification Closure Contract 冲突。

---

# 7.4 特别注意 K1g status

如果当前 Graph 中：

```yaml
K1g:
  status: done
```

但真实必需 device verification 仍 pending，

那么根据当前已经冻结的 Contract：

> `done` 与 required pending verification 不兼容。

因此执行 AI 必须重新核实：

### Project Graph 中的 `done` 含义

是否表示：

```text
开发实现完成
```

还是：

```text
WorkItem 全闭环完成
```

当前 Phase 1 Contract 已经明确：

> WorkItem `done` 必须完成所有 required Verification。

所以如果真机是 required 且 pending：

```text
K1g.status
```

不能继续是：

```text
done
```

应根据真实阶段设置为：

```text
verifying
```

或其它当前 Contract 中合法且符合事实的状态。

---

# 7.5 决策规则

优先：

```text
真实当前状态
+
现有 WorkItem 状态机
```

不得为了让 Validator 通过而伪造：

```text
device pass
```

也不得为了保持：

```text
K1g done
```

而把真实 pending verification 标成 optional。

---

# 7.6 required 的判断

只有当当前正式方案明确：

> 某项 Verification 不阻断 WorkItem 完成，

才能：

```yaml
required: false
```

不得为了通过 closure validator 随意改 optional。

---

# 7.7 验收

最终不得存在：

```text
当前资料明确未真机验证
+
Graph 写 device PASS
```

这种事实冲突。

---

# 8. PG-FDC-04

# 核实 PLAN-AI-NDJSON 与 K1i 的关系

## 8.1 当前疑点

当前可能存在：

```yaml
PLAN-AI-NDJSON:
  work_items:
    - K1g
    - K1i
```

按照当前 canonical relation：

```text
work --implemented_by--> plan
```

这意味着：

```text
K1g implemented_by PLAN-AI-NDJSON
K1i implemented_by PLAN-AI-NDJSON
```

但当前项目资料表明：

* K1g / B1-B6 有自己的 NDJSON / 周期记方案；
* K1i 有独立的流式运行时委托方案。

所以：

> `K1i → PLAN-AI-NDJSON` 很可能是错误关联。

---

# 8.2 必须读取真实方案

在：

```text
.ai-context/docs/feature/
```

查找：

```text
K1g
K1i
NDJSON
流式
真实委托
Runtime
```

确认：

### Q1

`PLAN-AI-NDJSON` 实际覆盖哪些 WorkItem？

### Q2

K1i 是否有独立正式 Plan？

### Q3

K1i 是否只是依赖 K1g，而不是由同一个 Plan 实现？

---

# 8.3 决策规则

如果真实资料确认：

```text
PLAN-AI-NDJSON
```

只实现 K1g：

则修改为：

```yaml
work_items:
  - K1g
```

---

如果真实资料确认：

K1i 有独立方案，但 Phase 1 PoC 当前没有创建该 Plan：

> 不要求本轮新增完整 K1i Plan 节点。

可以只移除错误的：

```text
K1i → PLAN-AI-NDJSON
```

Phase 2 再完整迁移。

---

如果真实资料确实证明：

同一个 Plan 同时正式覆盖 K1g + K1i：

则可以保留。

但最终汇报必须引用所依据的仓库文档路径。

---

# 8.4 不允许

不得因为：

```text
K1i depends_on K1g
```

就推导：

```text
K1i implemented_by K1g 的 Plan
```

Dependency 和 implementation plan 是两个不同关系。

---

# 9. PG-FDC-05

# F-AI-MEAL PoC 事实一致性审计

这是本轮最重要的收尾动作。

不是全量迁移。

只审计当前：

```text
F-AI-MEAL.yaml
```

已经存在的每一条 Cookbook 真实实体数据。

---

# 9.1 对 WorkItem 逐条检查

对当前文件已有：

```text
K1g
K1i
K1b
I8
...
```

实际以文件内容为准。

逐项验证：

```text
ID
title
kind
status
feature
priority（若有）
plan（若有）
depends_on
blocks
relations
```

是否与当前 Truth Source 一致。

---

# 9.2 对 Plan 逐条检查

确认：

```text
Plan ID
Plan status
Plan file
Plan.work_items
```

均真实存在。

如果 Plan file 不存在：

> 删除错误路径或修正真实路径。

不得用虚构路径占位。

---

# 9.3 对 Verification 逐条检查

确认：

```text
Verification kind
status
required
work_item
reason
```

均符合实际。

特别检查：

```text
device pass
```

不得凭空存在。

---

# 9.4 对 Relation 逐条检查

每条 explicit relation 都回答：

> “当前仓库有什么真实资料支持这条关系？”

如果不能回答：

> 删除。

Draft PoC 不需要为了展示 Relation 功能而保留假关系。

Relation 功能已经由自动测试证明。

---

# 9.5 对 CodeMapping 逐条检查

要求：

* 文件真实存在；
* repo-relative；
* 不使用 `...`；
* 不使用裸类名；
* category 基本合理。

本轮不要求做到全功能覆盖。

只保证已有声明真实。

---

# 9.6 对 match glob 检查

当前已有：

```yaml
match:
  - ...
```

需要保证它不是明显虚构模式。

可以：

```bash
rg --files
```

或简单 glob 验证是否能匹配预期文件。

本轮不要求实现工具级 glob validator。

---

# 9.7 Draft Graph 的原则

最终：

```text
mode: draft
```

继续保留。

但所有写入的数据必须符合：

```text
Incomplete
but
Truthful
```

允许：

> 数据不完整。

禁止：

> 为了 PoC 完整而写假数据。

---

# 10. Schema / Validator 冻结门禁

本轮明确禁止修改：

```text
schema/project-graph.schema.json
```

除非当前修改无法在现有 Contract 下表达真实数据。

正常情况下：

> 不应修改。

---

同样禁止重构：

```text
tools/project_graph.py
tools/yaml_lite.py
tools/schema_checker.py
```

除非：

> 仅为了新增本轮数据真实性测试，而且完全不改变 Contract 行为。

优先：

> 不改工具。

---

# 11. 测试要求

本轮首先运行现有全部 Project Graph 测试。

当前基准上一轮为：

```text
54 / 54 PASS
```

本轮实际数字以执行结果为准。

---

# 11.1 必须运行

使用当前项目已有正式测试命令。

记录：

```text
Command
Tests
Pass
Fail
```

---

# 11.2 Draft Graph Smoke Test

运行真实：

```text
pg check
```

或项目当前等价命令。

必须：

```text
PASS
```

---

# 11.3 Path Existence Check

对：

```text
F-MEAL.code.*
F-AI-MEAL.code.*
```

逐条检查精确路径存在。

可写一个临时脚本检查。

无需把临时脚本提交。

最终必须：

```text
missing path = 0
```

如果 match glob 不属于精确路径，则不进入该统计。

---

# 12. 建议新增一个轻量 Truth Fixture 测试

如果实现很简单，可以增加测试：

> 正式 PoC Feature 的 `code.*` 精确路径不得包含 `...`，不得是无 `/` 的裸类名。

例如：

```text
F-MEAL
F-AI-MEAL
```

当前 PoC 都必须通过。

但这属于：

> 建议项。

如果需要大幅改 Validator 才能实现：

> 不做。

本轮优先保持 Contract 冻结。

---

# 13. 禁止事项

绝对禁止：

### 不进入 Phase 2

不得：

* 扫描全部 Feature；
* 迁移全部 Bug；
* 迁移全部 Todo；
* 迁移全部 I/J/K/L；
* 迁移全部 Plan；
* 迁移全部 97 项真机验证；
* 构建正式 CurrentWork。

---

### 不进入 Phase 3

不得：

* 创建正式 AI_INDEX；
* 重写功能路径索引；
* 重写 07；
* 生成 Bug/Todo/Plan/Verify View。

---

### 不进入 Phase 4

不得实现：

```text
pg begin
pg affected
pg verify
pg reconcile
pg render
pg finish
```

---

### 不进入 Phase 5

不得：

* Git Hook；
* CI；
* required check。

---

# 14. 不得修改的项目文件

禁止修改：

```text
shared/
androidApp/
iosApp/
```

禁止修改：

```text
数据库
Kotlin生产代码
Gradle业务逻辑
```

禁止修改现有：

```text
07_项目现状
功能路径索引
待办列表
Bug列表
真机验证清单
SESSION_交接
BLUEPRINT_STATE
```

这些在本轮只能读取，不能同步修复。

---

# 15. 本轮 Gate

所有 Gate 必须满足才能提交。

## GATE-FDC-01

`F-MEAL.code.*`

全部为真实 repo-relative path。

---

## GATE-FDC-02

K1g 的 title / intent 不再误用 K1i 的“真实流式委托”含义。

---

## GATE-FDC-03

不存在：

```text
未真机验证
+
device = pass
```

事实冲突。

---

## GATE-FDC-04

K1g WorkItem status 与 required Verification 状态符合 Closure Contract。

---

## GATE-FDC-05

`PLAN-AI-NDJSON → K1i`

已经根据真实方案明确：

```text
KEEP
或
REMOVE
```

不能继续保持“未核实”。

---

## GATE-FDC-06

F-AI-MEAL 中所有 explicit Relation 都有真实项目依据。

---

## GATE-FDC-07

正式 PoC 中不存在纯演示关系。

---

## GATE-FDC-08

所有精确 CodeMapping path 均真实存在。

---

## GATE-FDC-09

Project Graph 全部自动测试 PASS。

---

## GATE-FDC-10

真实 Draft Graph：

```text
pg check
```

PASS。

---

## GATE-FDC-11

没有修改 Schema Contract。

---

## GATE-FDC-12

没有修改产品代码。

---

## GATE-FDC-13

Phase 2：

```text
NOT STARTED
```

---

# 16. Git Diff 审核

完成后执行：

```bash
git diff --stat
git diff
```

理想情况下主要只修改：

```text
.ai-context/project_graph/features/F-MEAL.yaml
.ai-context/project_graph/features/F-AI-MEAL.yaml
```

如果还修改测试：

必须能够解释：

> 为什么这个测试是为了防止 PoC Truth 再次漂移。

如果修改：

```text
Schema
Validator核心
README Contract
```

视为偏离蓝图。

必须停止并报告，不要擅自提交架构变更。

---

# 17. 提交要求

全部 Gate 通过后提交。

建议 Commit：

```text
fix(project-graph): align phase 1 draft facts with cookbook truth
```

或遵循仓库已有规范的等价提交。

Push 后停止。

---

# 18. 最终汇报模板

严格输出：

```text
Project Graph Phase:
Phase 1 — Final Data Cleanup

Review Baseline:
7aa8c3d138ef226c30ab2c4ad83b744f231ff733

Completion Commit:
<完整SHA>

PG-FDC-01 F-MEAL CodeMapping:
<修改摘要>

PG-FDC-02 K1g Identity:
<真实标题/业务含义>
Truth Source:
<文件路径>

PG-FDC-03 K1g Verification:
Before:
<状态>

After:
<状态>

Reason:
<依据>

PG-FDC-04 PLAN-AI-NDJSON / K1i:
Decision:
KEEP / REMOVE

Reason:
<依据>

Truth Source:
<文件路径>

PG-FDC-05 F-AI-MEAL Truth Audit:
WorkItems:
<结果>

Plans:
<结果>

Verifications:
<结果>

Relations:
<结果>

CodeMapping:
<结果>

Files Changed:
<列表>

Schema Changed:
NO

Validator Contract Changed:
NO

Production Code Changed:
NO

Tests:
Command:
<命令>

Result:
<实际结果>

Draft Graph Check:
Command:
<命令>

Result:
<结果>

Exact Code Paths Checked:
<count>

Missing Paths:
0

Known Remaining Draft Gaps:
<允许不完整，但列出尚未迁移的内容>

Deviations From Blueprint:
NONE / <说明>

Phase 2:
NOT STARTED

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 19. 结束门禁

完成 Commit + Push 后：

> STOP。

不要主动开始 Cookbook Bootstrap。

不要生成 Phase 2 数据。

不要因为发现旧文档状态漂移就顺手修改它们。

本轮唯一目标：

> 让 Phase 1 Draft Graph 的“语言正确”与“示例事实正确”同时成立。

最终应达到：

```text
Model Contract = Stable
Validator = Stable
Draft Graph = Incomplete but Truthful
```

只有达到这个状态，才能安全进入 Phase 2 全量迁移。

