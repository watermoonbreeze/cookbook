[toc]

# Project Graph Phase 1 — Architecture Rework

审核基准：

`581a391`

当前结论：

`REWORK`

禁止进入 Phase 2。

本次只修 Phase 1 Model Contract 与 Validator，不迁移 Cookbook 全量数据，不生成 Views，不修改生产代码。

## 必须修复

### PG-R1 — 全局 ID 唯一性

重构 ID declaration/index 逻辑。

必须能够发现：

* 同一 Feature 文件内重复 WorkItem ID；
* 同一文件内重复 Plan ID；
* 同一文件内重复 Verification ID；
* 跨文件重复 ID；
* 两个 Feature 文件内部声明相同 Feature ID；
* 不同实体类型之间发生不允许的 ID 冲突。

不得先放入 dict 覆盖后再检测。

应先记录 declaration：

`type / id / file / location`

再构建索引。

新增对应测试。

---

### PG-R2 — Feature 文件契约

要求：

`features/F-XXX.yaml`

内部：

`id: F-XXX`

文件名和 Feature ID 必须一致。

`mode: draft` 时：

允许 registry 中只有 ID、暂时没有 Feature 文件。

`mode: active` 时：

registry 中每个 Feature 必须存在对应文件，且不得出现未注册 Feature 文件。

---

### PG-R3 — Feature Sharding

WorkItem 的 primary Feature 必须等于所在 Feature 文件。

例如：

`features/F-A.yaml`

中：

`work_item.feature` 必须为 `F-A`。

跨 Feature 影响必须使用 relation 表达，不允许通过把 WorkItem 放在错误 shard 中实现。

Verification 所属 Feature 由其 WorkItem 推导；其声明文件应与 WorkItem primary Feature 一致。

Plan 可以跨 Feature 引用多个 WorkItem。

Plan 的 declaration owner 为它所在 Feature 文件；必须在 README 中明确该规则。

---

### PG-R4 — Relation Semantic Matrix

定义并实现 Relation 端点类型约束。

至少：

* `belongs_to`: work -> feature
* `implemented_by`: work -> plan
* `verified_by`: work -> verify
* `depends_on`: 同类合理依赖，至少支持 work -> work
* `blocks`: work -> work
* `affects`: work|plan -> feature
* `supersedes`: same-kind -> same-kind
* `related_to`: any valid entity -> any valid entity

明确：

`plan.work_items` 是 shorthand 存储。

Normalize 后语义必须是：

`work --implemented_by--> plan`

而不是 `plan --implemented_by--> work`。

`verification.work_item` 同理：

Normalize 后：

`work --verified_by--> verify`

Validator 必须拒绝例如：

`work --implemented_by--> feature`

新增测试。

---

### PG-R5 — Verification Closure

Verification 增加：

`required`

语义默认：

`true`

WorkItem `status=done` 必须满足：

所有 required Verification 都为：

* `pass`
* `not_required`

只要任意 required Verification 为：

* `pending`
* `fail`
* `blocked`

则 Done 非法。

至少必须存在一个 Verification；免验证使用：

`status: not_required`
+
`reason`

可选 Verification：

`required: false`

不得阻止 WorkItem Done。

增加：

* pass + pending
* pass + fail
* pass + optional pending
* all pass
* not_required + reason

测试。

---

### PG-R6 — Derived Facts

从 Feature 持久化数据中移除：

* `activity`
* `health`

它们必须由：

`derive_activity`
`derive_health`

或未来 Deriver 计算。

不得继续允许 AI 在核心 Feature YAML 中覆盖 Derived Fact。

如未来需要特殊覆盖，通过 extension 设计，不进入核心 V1。

PoC 中现有 `activity` / `health` 删除。

---

### PG-R7 — Code Mapping 结构

禁止：

`domain: A,B,C`

这种 CSV 字符串。

Code Mapping 改成结构化数组。

例如：

```yaml
code:
  ui:
    - androidApp/.../AiMealInputSheet.kt
  viewmodel:
    - androidApp/.../AiMealInputViewModel.kt
  core:
    - shared/.../StreamingMealSession.kt
    - shared/.../StreamingMealParser.kt
  tests:
    - shared/.../StreamingMealParserTest.kt
```

优先使用 repo-relative 真实路径。

Schema 对 code category 使用 array[string]。

保持：

`entry/ui/viewmodel/domain/data/core/tests/other`

分类。

`match` 保持 glob array。

---

### PG-R8 — graph_version

Phase 1 Schema 必须明确支持版本。

当前 V1：

`graph_version: "1"`

其他未知 major/version 必须校验失败。

不得只检查非空字符串。

---

### PG-R9 — CurrentWork 一致性

如果同时存在：

`current.feature`
`current.work_item`

则必须保证：

`current.work_item.feature == current.feature`

否则 PG-E-CURRENT。

---

### PG-R10 — YAML Fail Closed

`yaml_lite` 禁止遇到非法缩进或未支持结构后静默 skip。

所有不能可靠解释的输入：

立即抛 `YamlLiteError`。

必须增加 malformed indentation 测试。

Project Truth 的解析原则：

`unknown / malformed => fail`

不得：

`unknown => ignore`

---

### PG-R11 — Schema Checker Fail Closed

当前 schema_checker 对未知 JSON Schema keyword 不得静默忽略。

增加 Schema keyword 自检。

如果正式 schema 使用 fallback checker 不支持的 keyword：

立即失败并给出明确错误。

可以继续保持零依赖 fallback。

无需本阶段强制引入第三方 jsonschema。

---

### PG-R12 — PoC 数据真实性

不要在正式：

`project.yaml`
`features/*.yaml`

中使用“真实 Cookbook ID + 为演示而虚构的关系”。

例如演示 Relation 应迁入 test fixture，使用：

`F-SAMPLE-*`
`W-SAMPLE-*`
`PLAN-SAMPLE-*`

正式 draft Graph 中如果保留 Cookbook 实体，只允许写当前真实可信事实。

删除任何纯演示性质的真实 ID relation。

---

## 新增测试至少覆盖

1. same-file duplicate WorkItem
2. same-file duplicate Plan
3. duplicate Feature ID across files
4. filename != feature id
5. active registry missing file
6. WorkItem stored in wrong Feature shard
7. invalid Relation endpoint types
8. implemented_by canonical direction
9. done + required pending
10. done + required fail
11. done + optional pending
12. malformed YAML indentation
13. unsupported JSON Schema keyword
14. unsupported graph_version
15. current feature/work mismatch
16. code mapping array schema

## 保持不变

不得：

* 迁移全部 Cookbook；
* 生成 AI_INDEX；
* 重写功能路径索引；
* 重写 07；
* 实现 begin/affected/reconcile/render/finish；
* 加 Git Hook；
* 加 CI；
* 修改 Kotlin；
* 修改 DB；
* 修项目 Bug。

## 完成要求

运行原有测试 + 新增测试。

运行真实 draft Graph smoke test。

Phase 1 Rework 完成后：

commit + push。

输出：

* baseline commit
* completion commit
* changed files
* fixed blockers
* tests / results
* remaining architecture questions
* Phase 2 = NOT STARTED

完成后 STOP，等待架构复审。
