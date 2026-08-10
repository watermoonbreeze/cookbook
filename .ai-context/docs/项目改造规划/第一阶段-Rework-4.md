[toc]

# Project Graph Phase 1 最终收口实施蓝图

## PG-P1-FINAL · E-K1I-02 Truth Correction + Final Gate

**任务类型**：Phase 1 最终收口
**执行模型**：DeepSeek V4 Flash
**当前阶段**：Project Graph Phase 1
**当前架构状态**：Model Contract 已通过审核
**本轮允许修改范围**：`.ai-context/project_graph/`
**生产代码修改**：禁止
**Schema 修改**：禁止
**Validator Contract 修改**：禁止
**Phase 2**：禁止开始

---

# 0. 本轮唯一目标

当前 Project Graph Phase 1 的：

* Entity Model；
* Feature Model；
* WorkItem Model；
* Plan Model；
* Verification Model；
* Relation Model；
* JSON Schema；
* YAML Fail Closed；
* Duplicate Detection；
* Feature Sharding；
* CodeMapping；
* Declared / Observed / Derived；
* Validator；
* README Contract；

均已经完成架构审核。

当前只剩最后一个明确的 Project Truth 错误：

```text
E-K1I-02
```

现在被错误写成：

```text
kind = build
status = pass
```

但 Cookbook 当前唯一真机验证清单中：

```text
E-K1I-02
```

已经有稳定、不可复用的真实语义：

> K1i 周期记流式同样生效的真机验证项。

因此正确事实必须恢复为：

```text
kind = device
status = pending
```

本轮只修这一个 Truth 错误，并执行 Phase 1 最终 Gate。

---

# 1. 开始前检查

执行：

```bash
git status
git rev-parse HEAD
```

记录：

```text
Baseline Commit:
<当前完整 SHA>
```

如果存在用户未提交修改：

* 不 reset；
* 不 checkout；
* 不 clean；
* 不覆盖；
* 不擅自 stash。

如果与本任务无关的修改存在：

> 停止涉及这些文件的修改，仅处理本蓝图授权范围。

---

# 2. 必须读取

只需读取：

```text
.ai-context/project_graph/features/F-AI-MEAL.yaml

.ai-context/project_graph/project.yaml

.ai-context/docs/context_memory/SESSION_交接.md

.ai-context/docs/context_memory/BLUEPRINT_STATE.md

最新唯一：
.ai-context/docs/feature/真机待验证清单_*.md
```

如存在多个真机验证清单：

> 使用时间戳最新、当前项目指定为唯一验证清单的那一份。

不得使用旧验证清单覆盖最新事实。

---

# 3. Truth Source 判定

本轮 `E-K1I-02` 的语义以：

```text
最新唯一真机验证清单
```

为最高优先级。

BLUEPRINT_STATE 中：

```text
K1i 构建命令通过
```

是一个真实事实。

但它表示：

> Build / Test Observed Fact。

它不能改变已经存在的：

```text
Verification ID = E-K1I-02
```

的真实语义。

核心规则：

```text
Stable Verification ID semantics are immutable.
```

已有稳定 ID：

> 不得因为另一个验证事实出现而重新利用。

---

# 4. 当前错误

检查：

```text
.ai-context/project_graph/features/F-AI-MEAL.yaml
```

当前预计存在类似：

```yaml
- id: E-K1I-02
  kind: build
  status: pass
  work_item: K1i
  reason: K1i 构建验证通过（BLUEPRINT_STATE 三命令复验全绿）
```

这属于错误 Project Fact。

---

# 5. 正确修改

将 `E-K1I-02` 恢复成真实 Verification。

目标结构：

```yaml
- id: E-K1I-02
  kind: device
  status: pending
  work_item: K1i
  reason: 周期记流式同样生效待真机验证
```

`reason` 可以根据最新唯一真机清单做简短、准确压缩。

建议表达：

```text
周期记流式同样生效待真机验证：周一/周三/周五多段渐进解析、日期归属及流式 Delta 表现。
```

无需把完整真机操作步骤复制进 Graph。

完整验证步骤继续由：

```text
真机待验证清单
```

保存。

---

# 6. 不要保留错误 Build Verification

删除原先借用：

```text
E-K1I-02
```

表达 Build PASS 的语义。

不要把：

```text
kind: build
status: pass
```

移动到另一个随意创造的新 ID。

例如禁止未经设计直接新增：

```text
E-K1I-BUILD-01
VERIFY-K1I-BUILD
BUILD-K1I-01
```

原因：

当前 Phase 1/2 已经确定：

> 普通 Gradle Build/Test 的单次执行结果默认属于 Observed Fact，而不是自动创建 Verification Entity。

Observed 自动采集属于后续 AI lifecycle 阶段。

---

# 7. 不修改 K1i WorkItem 状态

本轮只修 Verification Fact。

不得顺手修改：

```text
K1i.status
K1i.title
K1i.plan
K1i.relations
```

除非现有 Graph 因 `E-K1I-02` 修正后违反已经冻结的 Verification Closure Contract。

如果发生 Validator 冲突：

先判断现有 K1i status。

---

# 8. Verification Closure 检查

当前 Contract：

如果：

```text
WorkItem.status = done
```

则所有：

```text
required = true
```

Verification 必须为：

```text
pass
或
not_required + reason
```

如果 K1i 当前还有：

```text
E-K1I-01 = pending
E-K1I-02 = pending
```

则：

```text
K1i
```

不能是：

```text
done
```

应当是符合当前真实状态的：

```text
verifying
```

---

## 8.1 只有在实际发现冲突时才修

如果当前：

```text
K1i.status = verifying
```

则：

> 不修改。

如果当前错误为：

```text
K1i.status = done
```

且 required device verification 仍 pending：

允许仅将：

```text
K1i.status
```

修正为：

```text
verifying
```

并在最终汇报中说明。

不得修改为其他状态。

---

# 9. required 字段

如果当前：

```text
E-K1I-02
```

没有显式：

```yaml
required:
```

则遵循当前 Schema 的默认语义。

如果 Schema 当前约定：

```text
required default = true
```

不需要为了显式而强行增加字段。

不要为了让 Validator 通过把它改成：

```yaml
required: false
```

真机验证是否 required 必须服从现有 Contract 和真实验收要求。

---

# 10. 不允许修改的内容

本轮绝对禁止修改：

```text
schema/project-graph.schema.json
```

禁止修改：

```text
tools/project_graph.py
tools/yaml_lite.py
tools/schema_checker.py
```

禁止修改 Relation Model。

禁止修改状态机。

禁止修改 README Contract。

禁止修改：

```text
F-MEAL.yaml
```

除非只是格式化工具自动产生无语义 diff；如果发生，撤销无关 diff。

---

# 11. 禁止修改旧状态文档

本轮只读取，不修改：

```text
SESSION_交接.md
BLUEPRINT_STATE.md
真机待验证清单
待办索引
待办_Bug修复.md
待办_功能算法.md
07_项目现状.md
功能路径索引.md
```

即使发现这些文档之间仍有旧状态漂移：

> 不处理。

旧 View 的自动替换属于 Phase 3。

---

# 12. 禁止生产代码修改

不得修改：

```text
shared/
androidApp/
iosApp/
```

不得修改：

* Kotlin；
* SQLDelight；
* Gradle业务配置；
* 数据库；
* 产品逻辑；
* AI Runtime。

本轮是纯 Project Graph Truth 修正。

---

# 13. 修改完成后的人工检查

重新打开：

```text
F-AI-MEAL.yaml
```

确认：

```text
E-K1I-02
```

只有一份声明。

必须满足：

```text
id      = E-K1I-02
kind    = device
status  = pending
work    = K1i
```

不得仍存在：

```text
build
pass
三命令复验全绿
```

等原错误语义。

---

# 14. 同时检查 E-K1I-01

本轮不修改 E-K1I-01，除非发现明显事实错误。

仅检查：

```text
E-K1I-01
```

和：

```text
E-K1I-02
```

不会出现：

* ID重复；
* 两个完全相同验证项；
* 一个被错误当 build；
* 错误关联其他 WorkItem。

如果 E-K1I-01 当前符合唯一真机清单：

> 保持原样。

---

# 15. Project Graph 测试

运行当前仓库既有 Project Graph 全部测试。

必须使用项目当前正式测试命令。

记录：

```text
Command:
<实际命令>

Total:
<实际数量>

Passed:
<实际数量>

Failed:
0
```

不得预填测试数量。

---

# 16. pg check

运行当前正式：

```text
pg check
```

或仓库中 Project Graph 工具对应等价命令。

结果必须：

```text
0 issue
PASS
```

如果失败：

> 不绕过 Validator。

先检查是不是 K1i WorkItem closure 状态与 pending Verification 冲突。

只允许按照本蓝图第 8 节修正。

---

# 17. Git Diff Gate

执行：

```bash
git diff --stat
git diff
```

理想修改范围：

```text
.ai-context/project_graph/features/F-AI-MEAL.yaml
```

如果出现其他文件：

逐个判断。

无关修改必须撤销。

---

# 18. Phase 1 Final Gate

以下全部通过才能提交。

## GATE-P1-FINAL-01

`E-K1I-02`：

```text
kind = device
```

---

## GATE-P1-FINAL-02

`E-K1I-02`：

```text
status = pending
```

---

## GATE-P1-FINAL-03

`E-K1I-02.work_item = K1i`

---

## GATE-P1-FINAL-04

不存在把 Build PASS 继续绑定 `E-K1I-02` 的描述。

---

## GATE-P1-FINAL-05

没有新建未经批准的 Build Verification ID。

---

## GATE-P1-FINAL-06

K1i WorkItem 与 required pending Verification 的 closure 状态一致。

---

## GATE-P1-FINAL-07

Project Graph 全部测试 PASS。

---

## GATE-P1-FINAL-08

`pg check` PASS。

---

## GATE-P1-FINAL-09

Schema 0 修改。

---

## GATE-P1-FINAL-10

Validator Contract 0 修改。

---

## GATE-P1-FINAL-11

README Contract 0 修改。

---

## GATE-P1-FINAL-12

生产代码 0 修改。

---

## GATE-P1-FINAL-13

旧项目文档 0 修改。

---

## GATE-P1-FINAL-14

Phase 2：

```text
NOT STARTED
```

---

# 19. Commit

全部 Gate 通过后提交。

建议：

```text
fix(project-graph): correct K1i device verification truth
```

或遵循仓库当前 Commit 规范的等价标题。

Push 到远程。

---

# 20. 完成后 STOP

Commit + Push 后：

> 必须停止。

不得开始：

```text
Phase 2A
```

不得：

* 创建更多 Feature；
* 增加 source_refs；
* 迁移 WorkItem；
* 迁移 Plan；
* 迁移 Verification；
* 修改 Graph mode；
* 生成 AI_INDEX。

等待架构审核。

---

# 21. 最终汇报模板

严格输出：

```text
Project Graph Phase:
Phase 1 — Final Closure

Baseline Commit:
<完整SHA>

Completion Commit:
<完整SHA>

E-K1I-02 Before:
kind: build
status: pass

E-K1I-02 After:
kind: device
status: pending
work_item: K1i

Truth Source:
<最新唯一真机验证清单完整repo-relative路径>

K1i WorkItem Status:
<当前值>

Closure Contract:
PASS

Files Changed:
<列表>

Schema Changed:
NO

Validator Contract Changed:
NO

README Contract Changed:
NO

Production Code Changed:
NO

Legacy Docs Changed:
NO

Tests:
Command:
<实际命令>

Result:
<实际总数>/<通过数> PASS
0 FAIL

PG Check:
Command:
<命令>

Result:
PASS

Phase 2:
NOT STARTED

Deviations:
NONE / <说明>

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 22. 最终目标

本轮完成后 Phase 1 应达到：

```text
Model Contract
= Stable

Validator
= Stable

Draft Graph
= Incomplete but Truthful

Known Phase 1 Blockers
= 0
```

只有收到外部架构审核：

```text
PHASE 1 = FINAL ACCEPT
```

以后，才能进入：

```text
Phase 2A — Feature Universe Bootstrap
```

完成后停止。

