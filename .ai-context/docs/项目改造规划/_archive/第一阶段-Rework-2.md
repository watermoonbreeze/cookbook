[toc]

# Project Graph Phase 1 收口修复实施蓝图

## PG-P1-R2 · Model Contract Finalization

**蓝图类型**：Phase 1 架构收口修复
**执行模型要求**：DeepSeek V4 Flash 可直接执行
**审核基准提交**：`6867a5fd79b006cc81fd5c470af13a45dc1ac94b`
**当前审核结论**：`REWORK-2`
**目标结论**：完成本蓝图后等待架构复审，不得自行进入 Phase 2
**影响范围**：仅 `.ai-context/project_graph/`
**生产代码影响**：无
**数据库影响**：无
**产品行为影响**：无

---

# 0. 本轮任务目标

当前 Project Graph Phase 1 的主体模型已经基本稳定。

本轮**禁止重新设计 Project Graph**。

只允许关闭以下剩余问题：

### 阻断项

* `PG-P1-B01` Feature ID 磁盘加载阶段重复声明仍可能被覆盖；
* `PG-P1-B02` yaml_lite 仍可能静默忽略未消费的尾部 Token；
* `PG-P1-B03` README Model Contract 与当前实现发生漂移；
* `PG-P1-B04` 正式 PoC 中仍存在虚假的 `K1g supersedes K1b` 关系。

### 同批收口项

* `PG-P1-S01` PoC CodeMapping 改为真实 repo-relative path；
* `PG-P1-S02` `SchemaError` 转换为 Project Graph 结构化错误，不允许裸 Python Stack Trace。

完成后：

> Phase 1 仍然保持 `draft`，提交并 STOP，等待架构复审。

---

# 1. 开始前强制检查

执行：

```bash
git status
git rev-parse HEAD
```

基准应为：

```text
6867a5fd79b006cc81fd5c470af13a45dc1ac94b
```

如果 HEAD 已经不是该提交：

* 不要 reset；
* 记录实际 HEAD；
* 检查新增提交是否只与本蓝图有关；
* 不要覆盖用户已有修改。

工作区如果存在用户未提交修改：

> 不得清理、reset、checkout 或覆盖。

---

# 2. 开始前必须读取

只读取与本轮有关的文件：

```text
.ai-context/project_graph/README.md

.ai-context/project_graph/project.yaml

.ai-context/project_graph/features/F-AI-MEAL.yaml

.ai-context/project_graph/schema/project-graph.schema.json

.ai-context/project_graph/tools/project_graph.py

.ai-context/project_graph/tools/yaml_lite.py

.ai-context/project_graph/tools/schema_checker.py

.ai-context/project_graph/tools/tests/test_validator.py
```

如测试目录还有 fixture/helper，一并读取。

不要重新扫描整个 Cookbook。

本轮不需要重新分析项目架构。

---

# 3. 修改总表

| ID        | 问题                      | 必须修改 |
| --------- | ----------------------- | ---- |
| PG-P1-B01 | Feature 重复声明可能被 dict 覆盖 | 是    |
| PG-P1-B02 | YAML 尾部 token 可被静默吞掉    | 是    |
| PG-P1-B03 | README 与最终 Contract 不一致 | 是    |
| PG-P1-B04 | PoC 存在虚假真实关系            | 是    |
| PG-P1-S01 | PoC CodeMapping 非真实路径   | 是    |
| PG-P1-S02 | SchemaError 可能裸异常       | 是    |

本轮除此之外：

> 不允许新增新的模型能力。

---

# 4. PG-P1-B01

# Feature 声明必须“先收集，后建索引”

## 4.1 当前问题

当前磁盘加载逻辑存在类似：

```python
self.features[fid] = fdata
self.feature_files[fid] = fpath
```

的问题。

如果两个磁盘文件都声明：

```yaml
id: F-A
```

第二个文件可能先覆盖第一个。

覆盖发生以后再执行唯一性校验：

> 已经丢失第一份 declaration 信息。

因此可能出现：

```text
features/F-A.yaml
features/F-A.yml
```

内容均为：

```yaml
id: F-A
```

但 Validator 最终：

```text
0 issues
```

这是错误行为。

---

# 4.2 目标行为

磁盘加载必须遵循：

```text
scan files
    ↓
parse all declarations
    ↓
preserve every declaration
    ↓
detect duplicate declarations
    ↓
only after validation build lookup index
```

禁止：

```text
parse
↓
immediately write dict by id
↓
overwrite
↓
later detect duplicates
```

---

# 4.3 推荐实现结构

增加一个内部 declaration 数据结构。

可使用：

```python
@dataclass
class FeatureDeclaration:
    declared_id: str
    path: Path
    data: dict
```

也允许使用 tuple/dict。

关键不是类名。

关键约束是：

> 同 ID 的多份声明在 duplicate validation 完成前必须全部存在内存中。

例如：

```python
self.feature_declarations: list[FeatureDeclaration]
```

读取：

```python
for feature_file in feature_files:
    data = load_yaml(feature_file)

    self.feature_declarations.append(
        FeatureDeclaration(
            declared_id=data["id"],
            path=feature_file,
            data=data,
        )
    )
```

之后：

```text
validate declaration uniqueness
```

确认无重复后，才能创建：

```python
self.features[id] = data
self.feature_files[id] = path
```

---

# 4.4 必须覆盖的重复类型

至少检测：

### Case A

```text
F-A.yaml
F-A.yml

both:
id: F-A
```

必须：

```text
PG-E-DUP_ID
```

---

### Case B

```text
one/F-A.yaml
two/F-B.yaml

both declare:
id: F-A
```

如果当前目录结构不允许嵌套 Feature 文件，则按实际扫描范围构造等价情况。

核心要求：

> 两个不同物理文件声明同一 Feature ID 必须失败。

---

### Case C

Feature ID 与其他全局实体 ID 存在禁止冲突时：

继续维持当前已有全局 ID 唯一规则。

不得因为本轮重构导致：

* WorkItem duplicate；
* Plan duplicate；
* Verification duplicate；

原有检测退化。

---

# 4.5 错误信息

不得只输出：

```text
duplicate
```

至少包含：

```text
PG-E-DUP_ID
entity: feature
id: F-A
declarations:
- path/to/F-A.yaml
- path/to/F-A.yml
```

具体排版可按当前 Validator 统一格式。

---

# 4.6 必须新增真实文件系统测试

不能只测试：

```python
ProjectGraph.from_data(...)
```

因为本问题发生在磁盘 `load()` 阶段。

必须使用临时目录真实创建：

```text
project.yaml
features/F-A.yaml
features/F-A.yml
```

两个 Feature 文件声明相同 ID。

调用和真实 `pg check` 一样的磁盘加载路径。

断言：

```text
PG-E-DUP_ID
```

---

# 4.7 验收

以下必须全部成立：

```text
same Feature ID in two files
→ FAIL

same Feature ID in .yaml + .yml
→ FAIL

unique Feature files
→ PASS
```

且原已有 duplicate entity 测试继续通过。

---

# 5. PG-P1-B02

# YAML Parser 必须消费完整输入

## 5.1 当前问题

当前 parser 顶层存在类似：

```python
value, _ = _parse_node(...)
return value
```

的问题。

第二个返回值代表：

> 已消费到哪个 token / line。

但没有检查：

```text
是否真的消费完整个输入。
```

因此：

```yaml
a: 1
- stray
```

可能得到：

```python
{"a": 1}
```

而：

```text
- stray
```

被静默忽略。

同样：

```yaml
- a
b: 2
```

可能只返回：

```python
["a"]
```

这是 Project Truth 不允许的。

---

# 5.2 核心原则

Project Graph YAML Parser 必须：

> Fail Closed.

即：

```text
合法且完全理解
→ PASS

存在任何未消费输入
→ FAIL

存在非法缩进
→ FAIL

存在当前 parser 不支持但无法可靠解释的结构
→ FAIL
```

绝对禁止：

```text
猜测
跳过
忽略
继续
```

---

# 5.3 修改要求

在顶层 parse 完成后取得：

```python
value, next_index = _parse_node(...)
```

然后检查：

```python
if next_index != len(lines):
    raise YamlLiteError(...)
```

注意：

如果 parser 内部会过滤：

* 空行；
* 注释；

则这里比较的是：

> parser 实际 tokenized / normalized 后的有效 line 数。

不要把合法尾部空行和注释误判为错误。

---

# 5.4 错误信息

建议类似：

```text
YamlLiteError:
unexpected unconsumed YAML content at line N
```

至少包含：

* 出错行号；
* 当前无法消费的内容。

不要返回 Python IndexError 等内部异常。

---

# 5.5 必须新增测试

### YAML-T1

输入：

```yaml
a: 1
- stray
```

必须：

```text
YamlLiteError
```

---

### YAML-T2

输入：

```yaml
- a
b: 2
```

必须：

```text
YamlLiteError
```

---

### YAML-T3

合法：

```yaml
a: 1

# comment
```

必须 PASS。

---

### YAML-T4

合法 nested mapping / list。

使用当前 schema 中真实使用到的结构。

必须 PASS。

---

### YAML-T5

上一轮已经修复的非法 deeper indentation：

继续 FAIL。

不得回归。

---

# 5.6 验收

不存在任何：

```text
输入部分内容非法
+
parser只解析前半部分
+
validator仍PASS
```

的情况。

---

# 6. PG-P1-B03

# README 必须同步成最终 Model Contract

## 6.1 定位

`.ai-context/project_graph/README.md`

不是普通说明文档。

它属于：

> Project Graph Model Contract 的 AI入口。

因此它必须与：

* Schema；
* Validator；
* Feature YAML；

完全一致。

本轮只同步已有最终设计。

不得继续扩展新概念。

---

# 6.2 Relation canonical direction

README 必须明确：

```text
belongs_to
work → feature
```

```text
implemented_by
work → plan
```

```text
verified_by
work → verification
```

---

# 6.3 Shorthand 与 canonical relation 分开说明

例如：

```yaml
plan:
  work_items:
    - K1g
```

这是：

> Storage Shorthand。

它的 normalized graph semantic 必须是：

```text
work:K1g
--implemented_by-->
plan:PLAN-X
```

不是：

```text
plan → work
```

---

Verification 同理。

存储：

```yaml
verification:
  work_item: K1g
```

normalized：

```text
work:K1g
--verified_by-->
verify:E-X
```

---

# 6.4 Derived Facts

README 必须删除任何以下含义：

> `activity` / `health` 可由 AI 声明或覆盖。

最终 V1 Contract：

```text
Feature.lifecycle
= Declared

Feature.activity
= Derived

Feature.health
= Derived
```

核心 Feature YAML 中：

> 不存储 activity / health。

如未来真的需要 override：

> 属于未来 Extension 设计。

本阶段不实现。

---

# 6.5 CodeMapping

README 示例必须更新成数组。

禁止继续：

```yaml
domain: A,B,C
```

统一：

```yaml
code:
  ui:
    - path/to/A.kt

  viewmodel:
    - path/to/B.kt

  core:
    - path/to/C.kt

  tests:
    - path/to/CTest.kt
```

---

# 6.6 CodeMapping 两个方向

README 必须明确：

### Forward

```text
Feature
→ CodeMapping
→ 功能路径索引
```

### Reverse

```text
git diff changed files
→ match glob
→ affected Feature
```

---

# 6.7 Verification Done Contract

README 必须和代码一致：

```text
WorkItem status = done
```

要求：

```text
至少存在 Verification
```

且所有：

```text
required=true
```

Verification 都必须：

```text
pass
或
not_required + reason
```

任何 required：

```text
pending
fail
blocked
```

都禁止 WorkItem Done。

---

# 6.8 Feature shard Contract

README 必须明确：

```text
features/F-A.yaml
```

中的 WorkItem：

```text
primary feature = F-A
```

跨 Feature 影响使用：

```text
affects
depends_on
related_to
```

等 relation。

禁止把属于 F-B 的 WorkItem 放进 F-A shard。

---

# 6.9 README验收

完成后人工/AI搜索以下旧表达：

```text
plan → work implemented_by
verify → work verified_by
activity可覆盖
health可覆盖
CSV CodeMapping
```

不得残留旧 Contract。

---

# 7. PG-P1-B04

# 删除正式 Graph 中的虚假关系

## 7.1 当前问题

正式 PoC：

```text
features/F-AI-MEAL.yaml
```

仍存在类似：

```yaml
source: work:K1g
type: supersedes
target: work:K1b
```

该关系不是当前 Cookbook 的真实项目事实。

K1g 与 K1b 是两项不同工作。

不能因为测试 relation 功能而把虚构关系写入正式 Graph。

---

# 7.2 修改

删除：

```text
K1g supersedes K1b
```

不得替换成另一条猜测关系。

如果当前没有确认的真实 Relation：

> 不写。

---

# 7.3 Relation 测试数据

Relation matrix、supersedes 等能力：

只能放进：

```text
tests
fixtures
```

使用虚构测试 ID：

```text
F-SAMPLE-A
F-SAMPLE-B

W-SAMPLE-1
W-SAMPLE-2

PLAN-SAMPLE-1

VERIFY-SAMPLE-1
```

禁止：

> 使用 Cookbook 真实实体 ID 来构造纯测试关系。

---

# 7.4 正式 draft Graph 原则

即使：

```text
mode: draft
```

其中出现的 Cookbook 数据仍必须：

> 真实可信。

Draft 的含义是：

> 尚未成为正式 Project Truth。

不是：

> 可以写虚构事实。

---

# 8. PG-P1-S01

# PoC CodeMapping 使用真实 repo-relative path

## 8.1 目标

当前 PoC 中如存在：

```text
androidApp/.../AiMealInputSheet.kt
shared/.../StreamingMealParser.kt
```

这种缩写路径：

改为真实：

> repository-relative path。

---

# 8.2 查找方式

在仓库中定位实际文件。

可使用：

```bash
find .
```

或：

```bash
rg --files
```

结合文件名搜索。

不得猜路径。

---

# 8.3 格式

例如实际路径确认后：

```yaml
code:
  ui:
    - androidApp/src/.../AiMealInputSheet.kt

  viewmodel:
    - androidApp/src/.../AiMealInputViewModel.kt

  core:
    - shared/src/.../StreamingMealSession.kt
    - shared/src/.../StreamingMealParser.kt

  tests:
    - shared/src/.../StreamingMealParserTest.kt
```

这里的示例不是实际路径要求。

执行 AI 必须使用仓库中真正存在的相对路径。

---

# 8.4 路径规则

必须：

* 相对仓库 root；
* `/` 分隔；
* 不使用绝对路径；
* 不使用 `...`；
* 不写类名代替文件路径；
* 不写逗号拼接字符串。

---

# 8.5 可选增强

如果实现成本很低，可以让 Validator：

> 对 `code.*` 中的精确路径检查文件是否存在。

但：

本轮这不是强制要求。

不要因此扩展大量逻辑。

`match` glob 也不要求每条必须当前命中。

---

# 9. PG-P1-S02

# SchemaError 转为结构化 Project Graph 错误

## 9.1 当前问题

`schema_checker.py` 已经正确改成：

> 未支持 JSON Schema keyword → Fail Closed。

这个方向保持。

但如果 `SchemaError` 没被上层接住，可能出现：

```text
Traceback ...
SchemaError ...
```

---

# 9.2 目标

所有用户/AI执行：

```text
pg check
```

时的预期输入/Schema问题：

都应该转为 Project Graph 标准错误。

---

# 9.3 修改要求

在合适边界捕获：

```python
SchemaError
```

转换成：

```text
PG-E-SCHEMA
```

例如：

```text
PG-E-SCHEMA
file: schema/project-graph.schema.json
reason: unsupported JSON Schema keyword: minItems
```

具体字段遵循当前 issue/error model。

---

# 9.4 不得吞异常

注意：

只捕获预期：

```text
SchemaError
YamlLiteError
ValidationError
```

等领域异常。

不要：

```python
except Exception:
    ...
```

把程序自身 Bug 伪装成 Graph 校验错误。

未知程序异常应该继续暴露，方便修复工具 Bug。

---

# 9.5 必须新增测试

构造一个 fallback checker 不支持的 Schema keyword。

调用实际 Project Graph validation 边界。

断言：

```text
PG-E-SCHEMA
```

且：

```text
不存在裸 traceback
```

单元测试可以检查 exception→issue 转换，不一定必须启动 subprocess。

---

# 10. 不得修改的最终 Contract

本轮不得调整以下已经通过审核的设计。

---

## 10.1 Feature

长期稳定产品/系统能力。

ID：

```text
F-XXX
```

---

## 10.2 WorkItem

统一：

```text
feature
bug
todo
tech_debt
refactor
compliance
research
maintenance
```

现有：

```text
I*
J*
K*
L*
```

保持原编号。

---

## 10.3 WorkItem 状态

保持：

```text
backlog
ready
in_progress
blocked
review
verifying
done
parked
cancelled
```

不得新增同义状态。

---

## 10.4 Plan 状态

保持：

```text
draft
reviewing
accepted
implementing
completed
superseded
```

---

## 10.5 Verification 状态

保持：

```text
pending
pass
fail
blocked
not_required
```

---

## 10.6 Feature lifecycle

保持：

```text
planned
active
mature
deprecated
```

---

## 10.7 Relation

保持当前已经实现并审核通过的 Relation Model。

只修 README 和测试。

不得重新设计一套。

---

## 10.8 graph_version

继续：

```yaml
graph_version: "1"
```

未知版本必须失败。

---

# 11. 测试实施蓝图

完成修改后，现有全部测试必须 PASS。

然后新增以下测试。

---

## T-01 Duplicate Feature Disk Declaration

真实临时目录：

```text
features/F-A.yaml
features/F-A.yml
```

both：

```yaml
id: F-A
```

期望：

```text
PG-E-DUP_ID
```

---

## T-02 Duplicate Feature Disk Declaration Same Extension

如测试工具方便，增加：

```text
a/F-A.yaml
b/F-A-copy.yaml
```

两者声明：

```yaml
id: F-A
```

如果正式 Feature scanner 不递归：

可使用两个不同合法候选文件名实现等价检测。

核心是：

> load path duplicate declaration 被真实测试。

---

## T-03 YAML Map Then Sequence

```yaml
a: 1
- stray
```

期望：

```text
YamlLiteError
```

---

## T-04 YAML Sequence Then Map

```yaml
- a
b: 2
```

期望：

```text
YamlLiteError
```

---

## T-05 YAML Legal Trailing Comment

合法 YAML + 尾部注释：

PASS。

---

## T-06 Schema Unsupported Keyword

给 fallback checker 一个它未实现的 Schema keyword。

通过实际 Graph validation boundary。

期望：

```text
PG-E-SCHEMA
```

---

## T-07 Relation Contract Regression

确认：

```text
work --implemented_by--> plan
```

PASS。

确认：

```text
plan --implemented_by--> work
```

如果 explicit relation matrix 不允许该方向：

FAIL。

---

## T-08 Verification Regression

保持已有：

```text
done + required pass
→ PASS

done + required pending
→ FAIL

done + optional pending
→ PASS
```

---

## T-09 Shard Regression

保持：

```text
F-A shard
work.feature = F-B
→ FAIL
```

---

## T-10 CurrentWork Regression

保持：

```text
current.feature = F-A
current.work belongs F-B
→ FAIL
```

---

# 12. 测试执行要求

先执行 Project Graph 专项测试。

使用当前仓库已经存在的正式命令。

如果当前测试通过：

```bash
python -m unittest ...
```

继续沿用。

不得为了好看重建测试框架。

---

## 12.1 必须记录

```text
测试命令
测试总数
通过数
失败数
```

例如：

```text
Project Graph Validator:
55 tests
55 passed
0 failed
```

数字以实际结果为准。

不得预填。

---

## 12.2 Smoke Test

运行真实 draft Graph：

```text
pg check
```

或项目当前等价命令。

要求：

```text
PASS
```

---

# 13. Diff 检查

完成后执行：

```bash
git diff --stat
git diff
```

确认修改仅属于：

```text
.ai-context/project_graph/
```

预计主要修改：

```text
README.md
features/F-AI-MEAL.yaml
tools/project_graph.py
tools/yaml_lite.py
tools/schema_checker.py
tools/tests/...
```

允许根据实际实现略有不同。

---

# 14. 绝对禁止

本次禁止修改：

```text
shared/
androidApp/
iosApp/
数据库 Schema
```

禁止修改现有产品 Bug。

禁止修改：

```text
07_项目现状.md
功能路径索引.md
待办索引
Bug索引
真机验证清单
SESSION_交接.md
BLUEPRINT_STATE.md
```

除非只是本轮蓝图文件本身被仓库规范要求登记；如无明确要求，不修改。

---

# 15. 禁止提前进入后续 Phase

完成后不得实现：

```text
Phase 2 Bootstrap
```

不得：

* 全量迁移 Feature；
* 全量迁移 I/J/K/L；
* 迁移 97 项真机验证；
* 创建正式 AI_INDEX；
* 自动重写功能路径索引；
* 自动重写 07；
* 实现 `pg begin`；
* 实现 `pg affected`；
* 实现 `pg reconcile`；
* 实现 `pg render`；
* 实现 `pg finish`；
* 加 Git Hook；
* 加 GitHub CI。

这些全部属于后续阶段。

---

# 16. 完成门禁

以下全部满足，才可提交。

### GATE-01

磁盘 Feature duplicate 测试真实 FAIL。

### GATE-02

YAML 未消费尾部输入真实 FAIL。

### GATE-03

README 与当前 Schema/Validator Contract 一致。

### GATE-04

正式 Graph 中无虚构 `K1g supersedes K1b`。

### GATE-05

正式 PoC CodeMapping 不存在 `...` 缩略路径。

### GATE-06

SchemaError 返回结构化 `PG-E-SCHEMA`。

### GATE-07

所有原测试 PASS。

### GATE-08

所有新增测试 PASS。

### GATE-09

真实 draft Graph `pg check` PASS。

### GATE-10

没有修改产品代码。

---

# 17. 提交

全部 Gate 通过后：

```bash
git status
git diff
```

然后提交。

建议 Commit：

```text
fix(project-graph): close phase 1 contract review blockers
```

如果 Cookbook 有更严格的 commit 格式，沿用项目现有规范。

Push 到当前远程分支。

---

# 18. 完成后必须 STOP

提交完成后：

> 不得自行执行任何新的 Project Graph 工作。

不得询问用户是否需要顺手继续 Phase 2 后就直接执行。

只输出结果。

---

# 19. 最终汇报格式

严格按以下模板：

```text
Project Graph Phase:
Phase 1 — Model Contract Finalization

Review Baseline:
6867a5fd79b006cc81fd5c470af13a45dc1ac94b

Completion Commit:
<完整SHA>

Fixed:

PG-P1-B01:
<具体修改>

PG-P1-B02:
<具体修改>

PG-P1-B03:
<具体修改>

PG-P1-B04:
<具体修改>

PG-P1-S01:
<具体修改>

PG-P1-S02:
<具体修改>

Files Changed:
<列表>

Tests Added:
<列表>

Tests:
Command:
<实际命令>

Result:
<实际测试数 / PASS / FAIL>

Draft Graph Smoke Check:
<命令>
<结果>

Production Code Changed:
NO

Phase 2:
NOT STARTED

Known Remaining Issues:
<没有则 NONE>

Deviations From Blueprint:
<没有则 NONE>

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 20. 执行优先级

本蓝图优先级：

```text
正确性
>
Fail Closed
>
Contract一致性
>
测试覆盖
>
代码简洁
>
实现速度
```

不要为了抽象漂亮增加新框架。

不要为了减少几行代码改变已经稳定的 Model Contract。

本轮最终目的只有一个：

> 将 Phase 1 从“主体正确但存在边界漏洞”收口为“可以安全承载 Phase 2 全量迁移”的稳定 Model Contract。

完成后停止。


