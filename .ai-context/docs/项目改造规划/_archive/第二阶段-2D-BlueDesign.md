[toc]

# Project Graph Phase 2D 实施蓝图

## PG-P2D · Verification Bootstrap + Stable-ID Reconciliation + Closure Audit

**任务类型**：Project Graph Phase 2D 正式施工
**推荐执行模型**：GPT-5.6 Luna
**推荐 Reasoning Effort**：Medium
**Phase 2C END / Handoff Commit**：`12984df3`
**Phase 2C**：`ACCEPT / CLOSED`
**Phase 2D 当前状态**：`AUTHORIZED / NOT STARTED`
**Phase 2D 本任务结束状态**：`IMPLEMENTED / WAITING FOR ARCHITECTURE REVIEW`
**Phase 2E**：严禁开始
**Graph Mode**：全过程保持 `draft`

---

# 0. 本阶段唯一目标

Phase 2D 只负责建立：

```text
Verification Project Truth
```

完整流程：

```text
找到执行时唯一权威 Verification Checklist
↓
重新计算真实 Verification 条目
↓
建立 Source Coverage
↓
盘点 Graph 已有 Verification
↓
Stable Verification ID Reconcile
↓
Verification → WorkItem Mapping
↓
Status / required / reason Reconcile
↓
写入正确 Feature shard
↓
Verification Closure Audit
↓
Tests
↓
pg check
↓
Commit
↓
Push
↓
STOP
```

---

# 1. 本阶段明确不做

禁止：

```text
Phase 2E CurrentWork Cross-Reconcile

L3 Feature Split Decision

Legacy View Replacement

AI_INDEX

功能路径索引生成

07_项目现状生成

Graph draft → active

Observed Store

pg begin
pg affected
pg verify
pg reconcile
pg render
pg finish

Git Hook

CI Guard
```

---

# 2. 不允许修改的核心

默认禁止修改：

```text
.ai-context/project_graph/schema/project-graph.schema.json

.ai-context/project_graph/tools/project_graph.py

.ai-context/project_graph/tools/yaml_lite.py

.ai-context/project_graph/tools/schema_checker.py
```

同时禁止修改生产代码：

```text
shared/
androidApp/
iosApp/
```

---

# 3. 开始前基线

执行：

```bash
git status
git rev-parse HEAD
git log --oneline -10
```

记录：

```text
Execution Baseline:
<完整 SHA>
```

当前 HEAD 应至少包含：

```text
12984df3
```

如果 HEAD 有后续提交：

* 不 reset；
* 不 checkout；
* 不 clean；
* 不覆盖；
* 记录真实 HEAD；
* 检查后续提交是否与本任务冲突。

存在用户未提交修改：

> 不得覆盖或清理。

---

# 4. 第一组必须读取：Phase Contract

按顺序读取：

```text
.ai-context/project_graph/README.md

.ai-context/project_graph/project.yaml

.ai-context/project_graph/migration/PHASE1_FINAL_ACCEPT.md

.ai-context/project_graph/migration/PHASE2B_ACCEPT.md

.ai-context/project_graph/migration/PHASE2C_ACCEPT.md

.ai-context/project_graph/migration/PHASE2C_TO_2D_HANDOFF.md
```

其中：

```text
PHASE2C_TO_2D_HANDOFF.md
```

是本阶段强制入口。

不得绕过。

---

# 5. 第二组必须读取：当前状态源

读取：

```text
.ai-context/docs/context_memory/SESSION_交接.md

.ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

用途：

> 辅助判断当前 WorkItem 状态与当前验证上下文。

它们不能重新定义 Verification Stable ID。

---

# 6. 第三组：发现所有真机验证清单

执行类似：

```bash
find .ai-context/docs/feature -maxdepth 1 -type f \
  | grep "真机.*验证.*清单"
```

PowerShell 可使用等价命令。

不要假定只有一个文件。

---

# 7. 唯一权威 Checklist 选择规则

必须确定：

```text
AUTHORITATIVE_VERIFICATION_CHECKLIST
```

只允许一个。

优先级：

```text
1. SESSION / 当前治理文档明确指向的“唯一/当前”验证清单

2. BLUEPRINT_STATE 或现行文档明确指向的当前验证清单

3. 同一系列清单中明确时间最新且内容声明为当前版本的文件
```

---

# 8. 不得单纯依赖文件修改时间

禁止：

```text
mtime 最新
→ 自动就是 Truth
```

原因：

> 老文档可能因为格式调整而产生较新 Modified Time。

优先读取文档内容和当前指针。

---

# 9. 如果出现两个都可能是当前 Truth 的清单

创建：

```text
CHECKLIST_SOURCE_CONFLICT
```

写入：

```text
.ai-context/project_graph/migration/PHASE2D_CONFLICTS.md
```

如果无法确定哪一个权威：

> 不得开始写 Verification Graph。

可以继续完成 Source Discovery 和冲突记录，但最终：

```text
Architecture Change Required / Source Conflict
```

必须报告。

---

# 10. 不硬编码历史文件名

过去的：

```text
真机待验证清单_202608082330.md
```

只能作为历史参考。

执行时必须重新确认当前唯一清单。

---

# ============================================================

# SOURCE MODEL

# ============================================================

# 11. Verification Row 定义

Checklist 中真正用于验收某个能力的独立条目，才是：

```text
Verification Row
```

例如：

```text
E-K1I-01 ...
E-K1I-02 ...
```

---

# 12. 以下不是独立 Verification Row

例如：

```text
章节标题
说明文字
统计数字
执行建议
验证方法说明
设备准备说明
汇总行
引用其它 Verification 的说明
```

这些应该：

```text
SKIP_NON_VERIFICATION_ROW
```

---

# 13. 建立 Phase 2D Inventory

新增：

```text
.ai-context/project_graph/migration/PHASE2D_INVENTORY.md
```

它是：

> Migration Working Record。

不是 Project Truth。

---

# 14. 建立 Source Coverage Ledger

新增：

```text
.ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md
```

这是本阶段最重要的 Audit Artifact。

目标：

```text
100% Source Disposition Coverage
```

---

# 15. 建立 Conflict Ledger

新增：

```text
.ai-context/project_graph/migration/PHASE2D_CONFLICTS.md
```

即使最终没有 Blocking Conflict：

> 文件仍然保留。

---

# ============================================================

# SOURCE COVERAGE

# ============================================================

# 16. 每一条 Checklist Row 必须有唯一 disposition

正式允许：

```text
KEEP_EXISTING_VERIFY

MIGRATE_VERIFY

UPDATE_EXISTING_VERIFY

SKIP_NON_VERIFICATION_ROW

DEFER_VERIFY_UNMAPPED
```

不得增加其它 disposition 名称。

---

# 17. KEEP_EXISTING_VERIFY

条件：

```text
Checklist Stable ID
=
Graph Existing Verification ID

AND

identity / work_item / kind / status / required / meaning
与权威清单一致
```

此时：

> 不修改 Graph。

---

# 18. MIGRATE_VERIFY

条件：

```text
Checklist 是真实 Verification Row
+
有 Stable Verification ID
+
能可靠映射 WorkItem
+
Graph 当前不存在该 ID
```

动作：

> 新建 Verification Entity。

---

# 19. UPDATE_EXISTING_VERIFY

条件：

Graph 已有同 Stable ID，但：

```text
status
reason
required
work_item
kind
```

任一与当前权威源不一致。

动作：

> 修正现有实体。

禁止：

```text
保留错误 ID
+
再创建新 ID
```

---

# 20. SKIP_NON_VERIFICATION_ROW

只用于：

> Checklist 中不是独立验收事项的行。

不得用于：

```text
低优先级 Verification
不知道怎么迁
工作量太大
```

---

# 21. DEFER_VERIFY_UNMAPPED

用于真实 Verification Row，但当前无法安全结构化。

允许 reason_code：

```text
WORKITEM_UNMAPPED

VERIFY_ID_MISSING

SOURCE_ID_CONFLICT

IDENTITY_AMBIGUOUS
```

---

# 22. DEFER 不等于静默遗漏

任何：

```text
DEFER_VERIFY_UNMAPPED
```

必须：

* 写入 Coverage；
* 写入 Conflict Ledger；
* 写具体原因；
* 保留原始 source text / source reference。

---

# 23. Source Coverage 每条建议格式

```text
SOURCE:
<authoritative checklist path>

SOURCE LOCATION:
<section / heading / line description>

RAW ID:
E-XXXX

RAW LABEL:
<source description>

ROW TYPE:
VERIFICATION

DISPOSITION:
MIGRATE_VERIFY

TARGET VERIFY:
verify:E-XXXX

WORKITEM:
work:K1i

FEATURE:
F-AI-MEAL

SOURCE STATUS:
pending

TARGET STATUS:
pending

REQUIRED:
true

MAPPING EVIDENCE:
<为什么属于该 WorkItem>

NOTES:
...
```

---

# 24. 非 Verification 行

```text
ROW TYPE:
NON_VERIFICATION

DISPOSITION:
SKIP_NON_VERIFICATION_ROW

REASON:
summary/header/instruction/...
```

---

# 25. 全局 Coverage 数学关系

必须计算：

```text
TOTAL_SOURCE_ROWS
=
VERIFICATION_ROWS
+
NON_VERIFICATION_ROWS
```

并且：

```text
VERIFICATION_ROWS
=
KEEP_EXISTING_VERIFY
+
MIGRATE_VERIFY
+
UPDATE_EXISTING_VERIFY
+
DEFER_VERIFY_UNMAPPED
```

还必须：

```text
UNEXPLAINED = 0
```

---

# 26. Source Coverage 100% 的含义

不是：

```text
100% 都必须进入 Graph
```

而是：

> 每一条源 Row 都必须有明确 disposition。

因此允许：

```text
DEFER_VERIFY_UNMAPPED > 0
```

但：

```text
UNEXPLAINED
```

必须：

```text
0
```

---

# ============================================================

# STABLE VERIFICATION ID

# ============================================================

# 27. Stable Verification ID 是不可变 Identity

Phase 2D 最重要规则：

```text
Verification Stable ID semantics are immutable.
```

---

# 28. 已有 ID 不得重新解释

例如：

```text
E-K1I-02
```

已经定义了真实设备验证语义。

禁止：

```text
E-K1I-02
→ build verification
```

禁止：

```text
E-K1I-02
→ 另一个无关设备测试
```

---

# 29. ID 与语义冲突时谁优先

权威顺序：

```text
最新唯一真实 Verification Checklist
>
Graph 当前旧实体
>
旧 Migration 文档
>
旧摘要
```

如果 Graph 错：

> 修 Graph。

不得修改 Stable ID。

---

# 30. Checklist 内部重复 ID 检查

必须扫描：

```text
duplicate verification IDs
```

---

# 31. 同 ID 出现多次

如果后面的行只是：

> 汇总 / 引用同一个 Verification

则：

```text
SKIP_NON_VERIFICATION_ROW
```

作为 reference row。

---

# 32. 如果同一个 Stable ID 真正定义了两个不同 Verification

记录：

```text
SOURCE_DUPLICATE_VERIFY_ID
```

这是 Blocking Conflict。

这两个条目都不得写入 Graph，直到语义冲突解决。

---

# 33. Graph Existing Verification Inventory

在写任何 YAML 前：

扫描：

```text
features/*.yaml
```

列出当前全部 Verification：

```text
ID
Feature shard
WorkItem
kind
status
required
reason
source_refs
```

写入：

```text
PHASE2D_INVENTORY.md
```

---

# 34. 已有 PoC Verification 必须先 Reconcile

尤其明确检查：

```text
E-K1I-01
E-K1I-02
```

顺序：

```text
读取 Checklist
↓
确认 Stable Identity
↓
确认 WorkItem
↓
确认 Status
↓
确认 required
↓
确认 reason
↓
确认 source_refs
↓
KEEP / UPDATE
```

然后才开始批量新增其它 Verification。

---

# ============================================================

# VERIFICATION → WORKITEM

# ============================================================

# 35. 每一个 Verification 必须映射真实 WorkItem

Verification 存储：

```text
work_item: <ID>
```

代表 normalized semantic：

```text
work
--verified_by-->
verification
```

不要再额外创造重复 relation。

---

# 36. WorkItem Mapping Priority

优先级：

```text
1. Verification Row 明确写出 WorkItem Stable ID

2. Row 所在 section 明确属于某个 Stable WorkItem

3. Checklist 明确 cross-reference 某专项 WorkItem / Blueprint

4. 当前 Project Graph + Phase 2B Source Coverage 能唯一证明对应关系
```

---

# 37. 禁止 fuzzy guess

禁止：

```text
标题看起来像 K1g
→ 就挂 K1g
```

禁止：

```text
属于 AI
→ 就挂 F-AI-MEAL 最近的 WorkItem
```

---

# 38. Section Ownership 允许使用

例如：

```text
## K1i
...
E-K1I-01
E-K1I-02
```

如果文档结构明确表示下面 Verification 属于 K1i：

可作为可靠 mapping evidence。

---

# 39. 无法唯一映射

使用：

```text
DEFER_VERIFY_UNMAPPED
```

Reason：

```text
WORKITEM_UNMAPPED
```

并进入：

```text
PHASE2D_CONFLICTS.md
```

---

# 40. 不得为了 Verification 强建假 WorkItem

禁止：

```text
找不到 WorkItem
→ 自己创建 TODO-XXX
```

---

# 41. 如果 Checklist 暴露 Phase 2B 真漏项

记录：

```text
WORKITEM_MISSING_FOR_VERIFY
```

到 Conflict Ledger。

只有满足全部：

```text
Checklist 明确存在真实 WorkItem
+
身份语义明确
+
Stable ID 明确
或
能够严格按 2B ID convention 创建
```

才允许补 WorkItem。

---

# 42. 本阶段补 WorkItem 属于异常路径

正常期望：

```text
0
```

如果需要补：

最终报告必须单独列出。

---

# ============================================================

# FEATURE SHARD

# ============================================================

# 43. Verification 属于 WorkItem 所在 Feature shard

如果：

```text
work:K1i
feature: F-AI-MEAL
```

则：

```text
verify:E-K1I-XX
```

应写在：

```text
features/F-AI-MEAL.yaml
```

---

# 44. Verification 不独立选择 Feature

禁止：

```text
Verification 看起来像 Health
但 WorkItem 属于 Family
→ 放 Health shard
```

Verification shard 跟随：

```text
WorkItem Primary Feature
```

---

# 45. 跨 Feature 验证

仍只绑定 Primary WorkItem。

其它 Feature 影响：

> 不通过复制 Verification 表达。

---

# ============================================================

# VERIFICATION KIND

# ============================================================

# 46. 真机验证清单产生的 Verification

默认：

```text
kind: device
```

因为 Source 本身就是：

> 真机 Verification Checklist。

---

# 47. 不从该清单创造 build Verification

禁止：

```text
kind: build
```

除非权威 Verification Source 本身明确把该 Stable ID 定义成正式 build acceptance item。

普通：

```text
Gradle build
unit test
lint
pg check
```

仍属于：

```text
Observed Fact
```

---

# 48. 不发明新 kind

如果当前 Schema 没有某种 kind：

> 不修改 Schema。

记录 Conflict。

---

# ============================================================

# STATUS RECONCILIATION

# ============================================================

# 49. Status 必须依据“真实验证结果”

不能依据：

```text
条目新增时间
emoji 分类
标题语气
代码已经提交
Plan completed
CODE accepted
ARCH accepted
```

---

# 50. pass

只有明确存在真实验证结果：

```text
验证通过
实际通过
真机通过
✅ 且该 ✅ 明确属于结果字段
```

才：

```text
status: pass
```

---

# 51. pending

以下：

```text
待验证
尚未验证
无实际结果
只描述检查方法
只标记新增/修改
```

统一：

```text
status: pending
```

---

# 52. fail

只有：

> 实际执行过验证，并明确结果失败 / 未通过。

才：

```text
status: fail
```

---

# 53. blocked

只有明确表示：

> 当前因为设备、环境、权限、外部条件等无法完成验证。

才：

```text
status: blocked
```

---

# 54. not_required

仅当权威源明确：

```text
不再需要验证
N/A
无需验证
已被正式替代
```

并有 reason。

才：

```text
status: not_required
```

---

# 55. UI emoji 规则

以下：

```text
⬜
🔧
🆕
```

默认不代表结果。

如果没有实际结果：

```text
pending
```

---

# 56. ✅ 也不能盲目机械映射

必须判断它是否属于：

```text
Actual Verification Result
```

而不是：

```text
“已加入清单”
“代码完成”
“文档完成”
```

只有前者：

```text
pass
```

---

# 57. ❌ 同理

如果 ❌ 表示：

```text
验证执行失败
```

→ fail。

如果只是：

```text
功能尚未实现
不支持
```

需要结合上下文判断是否：

```text
pending / blocked / fail
```

不得机械处理。

---

# ============================================================

# REQUIRED

# ============================================================

# 58. required 默认

沿用 Frozen Contract：

```text
required = true
```

---

# 59. 如果当前 YAML style 允许默认省略

可以不显式写：

```yaml
required: true
```

但 Migration Inventory / Coverage 中必须按：

```text
true
```

统计。

---

# 60. required:false

只有源明确：

```text
optional
非阻断验收
补充检查
```

才能：

```text
required: false
```

---

# 61. 禁止为了 Closure 降 required

绝对禁止：

```text
WorkItem 想变 done
→ required:false
```

---

# ============================================================

# REASON / SOURCE_REFS

# ============================================================

# 62. reason

Verification `reason`：

> 使用简洁、准确、可理解的验收语义。

不要复制整段 Checklist。

---

# 63. reason 应表达“验证什么”

例如：

```text
周期记流式在周一/周三/周五多段场景下保持渐进解析、日期归属和 Delta 表现正确。
```

而不是：

```text
pending
```

---

# 64. 不在 reason 里塞状态统计

状态由：

```text
status
```

表达。

---

# 65. source_refs 第一条

所有迁移 / 更新 Verification：

第一条必须：

```text
AUTHORITATIVE_VERIFICATION_CHECKLIST
```

可带：

```text
#section
```

---

# 66. 第二条 source_ref

如果 WorkItem mapping 依赖某正式 Blueprint：

可增加：

```text
formal blueprint
```

---

# 67. 第三条

仅当确实必要才使用。

不要堆所有提过它的文档。

---

# ============================================================

# WORKITEM STATUS & CLOSURE

# ============================================================

# 68. Phase 2D 默认不重新迁 WorkItem 状态

Phase 2B 的 WorkItem 状态已经 ACCEPT。

因此：

> Verification migration 不等于重新做 WorkItem Migration。

---

# 69. 唯一允许主动修正 WorkItem status 的情况

Verification 写入后如果出现：

```text
WorkItem.status = done

AND

required Verification
= pending / fail / blocked
```

这违反 Frozen Closure。

此时必须修正。

---

# 70. Closure repair 默认目标

如果没有更明确的高优先级状态证据：

```text
done
→ verifying
```

并记录：

```text
CLOSURE_STATUS_REPAIR
```

---

# 71. 不得自动从 verifying → done

即使：

```text
所有 migrated required Verification = pass
```

Phase 2D 也：

> 不自动把 WorkItem 改成 done。

原因：

```text
Phase 2E
```

才做完整 Cross-Reconcile。

---

# 72. 例外

只有 WorkItem 当前已经：

```text
done
```

且 Closure 完全合法：

> 保持 done。

---

# 73. fail 不自动决定 WorkItem 回到 in_progress

如果 Verification：

```text
fail
```

但高优先级源没有明确说明 WorkItem 已重新进入编码：

保持当前非-done WorkItem status。

如果需要重新判断：

记录：

```text
WORK_STATUS_REVIEW_REQUIRED
```

交 Phase 2E。

---

# 74. blocked 同理

Verification blocked：

> 不自动把 WorkItem 改为 blocked。

除非 Current Truth 明确说整个 WorkItem 被阻塞。

---

# ============================================================

# CURRENTWORK / PLAN / RELATIONS BOUNDARY

# ============================================================

# 75. CurrentWork

本阶段：

```text
不修改
```

---

# 76. Plan

本阶段：

```text
不修改 Plan lifecycle/status
```

除非发现 Verification source 暴露明显数据引用错误。

遇到这种情况：

> 记录 Conflict，不顺手修 Plan。

---

# 77. Relation

本阶段：

```text
不新增业务 Relation
```

Verification `work_item` 是已有 storage shorthand。

不要创建重复：

```text
verified_by
```

relation。

---

# ============================================================

# EXISTING GRAPH RECONCILIATION

# ============================================================

# 78. 写数据前必须生成 Existing Verify Inventory

至少统计：

```text
Existing Verification Count

By Feature

By Status

By kind

Duplicate IDs

Missing WorkItem refs
```

---

# 79. Existing Stable ID 与 Source 交集

计算：

```text
Existing IDs ∩ Source IDs
```

分类：

```text
KEEP
UPDATE
```

---

# 80. Existing ID 不在当前 Checklist

不能立即删除。

必须判断：

```text
它是否来自其它正式 Verification Truth Source？
```

---

# 81. 本阶段 Source 是“最新唯一真机清单”

因此 Graph 中已有：

```text
device Verification
```

如果不在当前唯一真机清单：

记录：

```text
EXISTING_VERIFY_NOT_IN_CURRENT_CHECKLIST
```

到 Conflict Ledger。

---

# 82. 不自动删除 orphan existing Verification

原因：

> 它可能来自其它正式 acceptance source，或者当前清单存在遗漏。

Phase 2D 不猜。

---

# 83. build / non-device existing Verification

如果 Graph 里存在合法、历史正式 Verification：

> 不因为它不在真机清单而删除。

但普通 build/test 假 Verification 如果已经被之前 Phase 修掉：

不要重新创建。

---

# ============================================================

# SOURCE ID AUDIT

# ============================================================

# 84. Source Stable ID 全量提取

必须输出：

```text
Source Verification IDs
```

按排序记录到 Inventory。

---

# 85. 检查：

```text
blank ID
duplicate ID
malformed ID
same ID multiple semantics
```

---

# 86. 不强制所有 Verification ID 都是 E- 前缀

已有 stable source ID：

> 原样保留。

禁止因为 convention 不统一重新编号。

---

# 87. 无 ID Verification

真实 Verification Row 但没有 Stable ID：

```text
DEFER_VERIFY_UNMAPPED
reason_code:
VERIFY_ID_MISSING
```

本阶段：

> 不自行创造新的 Verification ID convention。

---

# ============================================================

# WORKITEM MAPPING AUDIT

# ============================================================

# 88. Mapping 统计

最终必须统计：

```text
Verification Rows:
<n>

Mapped:
<n>

Deferred Unmapped:
<n>
```

---

# 89. Mapping Coverage

要求：

```text
Mapped + Deferred
=
Verification Rows
```

---

# 90. Unmapped 可以不为 0

但：

```text
UNKNOWN / UNEXPLAINED
=
0
```

---

# 91. 每个 Deferred 必须进入 Conflicts

例如：

```text
VERIFY_UNMAPPED:
E-XXXX

reason:
...

source:
...

candidate:
<if any>

decision:
deferred, no guess
```

---

# ============================================================

# PHASE2D_CONFLICT TYPES

# ============================================================

# 92. 正式允许记录

```text
CHECKLIST_SOURCE_CONFLICT

SOURCE_DUPLICATE_VERIFY_ID

VERIFY_ID_MISSING

WORKITEM_UNMAPPED

WORKITEM_MISSING_FOR_VERIFY

IDENTITY_AMBIGUOUS

EXISTING_VERIFY_NOT_IN_CURRENT_CHECKLIST

CLOSURE_STATUS_REPAIR

WORK_STATUS_REVIEW_REQUIRED

ARCH_CHANGE_REQUIRED
```

---

# 93. Blocking 与 Non-Blocking

Blocking：

```text
无法确定唯一权威 Checklist

Stable ID 同时表示两个不同 Verification

需要修改 Frozen Schema 才能表达
```

---

# 94. Non-Blocking

例如：

```text
少数 Verification 无法映射 WorkItem
```

可以：

```text
DEFER_VERIFY_UNMAPPED
```

后继续迁其它条目。

---

# ============================================================

# WRITE GRAPH

# ============================================================

# 95. 写入顺序

必须按：

```text
1. Existing Verification Reconcile

2. Stable-ID Updates

3. New Verification Migration

4. WorkItem Closure Audit

5. Counts / Coverage Audit
```

---

# 96. 禁止“读一条写一条”

先完成：

```text
Inventory
+
Coverage Draft
+
Mapping
```

再写 Feature YAML。

这样可以避免：

```text
重复 Verification
错误 WorkItem
半途中改变规则
```

---

# 97. Feature 施工顺序

建议按：

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

只是施工顺序。

不代表优先级。

---

# ============================================================

# KNOWN AUDIT ANCHORS

# ============================================================

# 98. 必须显式审计

至少：

```text
E-K1I-01
E-K1I-02
```

最终报告必须列出：

```text
source meaning
work_item
kind
status
required
action
```

---

# 99. K1i

如果当前最新清单仍显示设备验证未完成：

```text
K1i
=
verifying
```

保持。

---

# 100. K1g / K1a / L1

这些 WorkItems 当前都属于：

```text
Plan completed
WorkItem verifying
```

Phase 2D 必须检查：

> 对应 required device Verification 是否已经完整迁入。

不能因为 Plan completed 自动 pass。

---

# ============================================================

# VERIFICATION STATISTICS

# ============================================================

# 101. 不使用历史 97 / 17 作为目标

必须重新计算。

---

# 102. Source Status Statistics

从权威 Checklist 计算：

```text
Verification Rows

pass
pending
fail
blocked
not_required
```

---

# 103. Graph Statistics

迁移后计算：

```text
Total Verification Entities

By Status

By Kind

By Feature

By WorkItem
```

---

# 104. Source vs Graph Reconcile

对于：

```text
KEEP
MIGRATE
UPDATE
```

对应行：

Graph status 必须与权威源一致。

---

# 105. Deferred 项

不应该出现在 Graph 新实体中。

因此：

```text
Source Verification Rows
≠
Graph Newly Represented Rows
```

在存在 Deferred 时是正常的。

---

# 106. 正确公式

```text
Verification Rows
=
Graph Represented Source Rows
+
Deferred Verification Rows
```

---

# ============================================================

# CLOSURE AUDIT

# ============================================================

# 107. 对所有 WorkItems 运行 Closure Audit

不仅检查本次新增 Verification 对应的 WorkItem。

至少全 Graph 扫描：

```text
status = done
```

的 WorkItems。

---

# 108. done 必须满足

```text
at least one Verification
```

并且：

```text
all required Verification
=
pass / not_required
```

---

# 109. required pending

如果出现：

```text
done + pending
```

→ Closure violation。

---

# 110. required fail

```text
done + fail
```

→ Closure violation。

---

# 111. required blocked

```text
done + blocked
```

→ Closure violation。

---

# 112. optional pending

如果：

```text
required:false
status:pending
```

不阻断 done。

---

# 113. Closure repair 记录

任何 Phase 2D 自动修的：

```text
done → verifying
```

必须写入：

```text
PHASE2D_CONFLICTS.md
```

类别：

```text
CLOSURE_STATUS_REPAIR
```

---

# ============================================================

# MIGRATION DOCUMENTS

# ============================================================

# 114. PHASE2D_INVENTORY.md 最终结构

至少：

```text
Authoritative Checklist

Checklist Selection Evidence

Existing Graph Verification Inventory

Source Verification ID Inventory

Existing / Source Intersection

Migration Summary

WorkItem Mapping Summary

Status Summary

Closure Summary
```

---

# 115. PHASE2D_SOURCE_COVERAGE.md

必须：

```text
每条 source row
+
唯一 disposition
```

以及：

```text
TOTAL_SOURCE_ROWS
VERIFICATION_ROWS
NON_VERIFICATION_ROWS
KEEP
MIGRATE
UPDATE
DEFER
SKIP
UNEXPLAINED
```

---

# 116. PHASE2D_CONFLICTS.md

顶部：

```text
Phase:
2D

Status:
OPEN DURING IMPLEMENTATION
```

最终写：

```text
Blocking:
<n>

Non-Blocking:
<n>
```

---

# ============================================================

# README / PROJECT ROOT

# ============================================================

# 117. README 最终阶段状态

本任务完成后：

```text
Phase 1
FINAL ACCEPT / FROZEN

Phase 2A
ACCEPT / CLOSED

Phase 2B
ACCEPT / CLOSED

Phase 2C
ACCEPT / CLOSED

Phase 2D
IMPLEMENTED
WAITING FOR ARCHITECTURE REVIEW

Phase 2E
NOT STARTED

Graph Mode
draft
```

---

# 118. 不写 Phase 2D ACCEPT

禁止：

```text
Phase 2D ACCEPT
```

只有外部架构审核后才能写。

---

# 119. project.yaml

只允许治理注释更新为：

```text
Current Bootstrap Stage:
Phase 2D review
```

实际：

```yaml
mode: draft
```

不变。

---

# 120. CurrentWork

```text
0 changes
```

除非为了 Closure 有 unavoidable WorkItem repair。

但：

```yaml
current:
```

仍不得修改。

---

# ============================================================

# VALIDATION

# ============================================================

# 121. Project Graph Tests

运行当前完整测试套件。

记录：

```text
Command:
<实际>

Total:
<n>

Pass:
<n>

Fail:
0
```

---

# 122. pg check

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

# 123. Stable ID Duplicate Audit

必须确认：

```text
Duplicate Verification IDs:
0
```

除非 source 本身存在 Blocking Conflict，此时对应项不能写 Graph。

---

# 124. Missing WorkItem Ref

必须：

```text
Graph Verification with missing work_item:
0
```

---

# 125. Source Ref Audit

所有新建/更新 Verification：

```text
source_refs valid
```

---

# 126. Kind Audit

所有从当前真机 Checklist 迁入的新 Verification：

默认：

```text
kind=device
```

---

# 127. Build Verification Audit

执行搜索确认没有因为本阶段新增类似：

```text
build verification
unit-test verification
pg-check verification
```

实体。

---

# ============================================================

# PHASE 2D GATES

# ============================================================

# 128. GATE-2D-01

唯一权威 Checklist 已明确。

---

# 129. GATE-2D-02

Checklist Selection Evidence 已记录。

---

# 130. GATE-2D-03

历史 97/17 未硬编码。

---

# 131. GATE-2D-04

真实 Source Verification 数量已重新计算。

---

# 132. GATE-2D-05

PHASE2D_INVENTORY.md 已创建。

---

# 133. GATE-2D-06

PHASE2D_SOURCE_COVERAGE.md 已创建。

---

# 134. GATE-2D-07

PHASE2D_CONFLICTS.md 已创建。

---

# 135. GATE-2D-08

所有 Source Rows 有 disposition。

---

# 136. GATE-2D-09

UNEXPLAINED = 0。

---

# 137. GATE-2D-10

所有 Stable Verification IDs 被保留。

---

# 138. GATE-2D-11

Stable ID 没有被重新解释。

---

# 139. GATE-2D-12

Source duplicate ID 已审计。

---

# 140. GATE-2D-13

Existing Verification 先 Reconcile，再迁新增。

---

# 141. GATE-2D-14

E-K1I-01 已显式核对。

---

# 142. GATE-2D-15

E-K1I-02 已显式核对。

---

# 143. GATE-2D-16

Verification → WorkItem mapping 有证据。

---

# 144. GATE-2D-17

无法映射项没有猜测。

---

# 145. GATE-2D-18

DEFER_VERIFY_UNMAPPED 全部有原因。

---

# 146. GATE-2D-19

Verification shard 跟随 WorkItem Primary Feature。

---

# 147. GATE-2D-20

真机清单迁入 Verification 默认 kind=device。

---

# 148. GATE-2D-21

普通 build/test 未成为 Verification。

---

# 149. GATE-2D-22

status 基于真实验证结果。

---

# 150. GATE-2D-23

⬜/🔧/🆕 未机械映射成 pass/fail。

---

# 151. GATE-2D-24

required 默认 true。

---

# 152. GATE-2D-25

required:false 只有明确 optional evidence。

---

# 153. GATE-2D-26

source_refs 指向权威 Checklist。

---

# 154. GATE-2D-27

reason 保留真实 Verification 语义。

---

# 155. GATE-2D-28

WorkItem statuses 未被大规模重新迁移。

---

# 156. GATE-2D-29

所有 done WorkItems Closure 合法。

---

# 157. GATE-2D-30

Closure repair 全部有记录。

---

# 158. GATE-2D-31

没有自动 verifying → done。

---

# 159. GATE-2D-32

Plan statuses 0 修改。

---

# 160. GATE-2D-33

CurrentWork 0 修改。

---

# 161. GATE-2D-34

业务 Relations 0 新增。

---

# 162. GATE-2D-35

Schema 0 修改。

---

# 163. GATE-2D-36

Validator Contract 0 修改。

---

# 164. GATE-2D-37

Observed Store NOT IMPLEMENTED。

---

# 165. GATE-2D-38

Production Code 0 修改。

---

# 166. GATE-2D-39

Graph mode = draft。

---

# 167. GATE-2D-40

Tests PASS。

---

# 168. GATE-2D-41

pg check PASS / 0 issue。

---

# 169. GATE-2D-42

Phase 2D：

```text
IMPLEMENTED
WAITING FOR ARCH REVIEW
```

---

# 170. GATE-2D-43

Phase 2E：

```text
NOT STARTED
```

---

# ============================================================

# DIFF BOUNDARY

# ============================================================

# 171. 正常允许修改

```text
.ai-context/project_graph/features/*.yaml

.ai-context/project_graph/README.md

.ai-context/project_graph/project.yaml
  仅治理注释

.ai-context/project_graph/migration/PHASE2D_INVENTORY.md

.ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md

.ai-context/project_graph/migration/PHASE2D_CONFLICTS.md
```

---

# 172. 可能允许修改 WorkItem status

仅限：

```text
Closure required repair
```

例如：

```text
done → verifying
```

必须有 Audit Record。

---

# 173. 正常不应修改

```text
PHASE1_FINAL_ACCEPT.md
PHASE2A_ACCEPT.md
PHASE2B_ACCEPT.md
PHASE2C_ACCEPT.md
```

这些都是 Immutable Review Records。

---

# 174. 绝对禁止修改

```text
schema/
tools/
shared/
androidApp/
iosApp/
```

---

# ============================================================

# COMMIT

# ============================================================

# 175. 本阶段只做一个实施 Commit

建议：

```text
feat(project-graph): bootstrap verification truth phase 2d
```

---

# 176. Push 后 STOP

Commit + Push 后：

> 绝对停止。

不得开始：

```text
Phase 2D END

PHASE2D_ACCEPT.md

PHASE2D_TO_2E_HANDOFF.md

Phase 2E Cross-Reconcile
```

这些必须等外部架构审核。

---

# ============================================================

# FINAL REPORT

# ============================================================

# 177. 最终汇报模板

严格输出：

```text
Project Graph:
Phase 2D — Verification Bootstrap

Execution Baseline:
<sha>

Completion Commit:
<sha>

================================
AUTHORITATIVE SOURCE
================================

Checklist:
<path>

Selection Evidence:
<说明>

Other Candidate Checklists:
<count/list>

Source Conflict:
NO / YES

================================
SOURCE COVERAGE
================================

Total Source Rows:
<n>

Verification Rows:
<n>

Non-Verification Rows:
<n>

KEEP_EXISTING_VERIFY:
<n>

MIGRATE_VERIFY:
<n>

UPDATE_EXISTING_VERIFY:
<n>

DEFER_VERIFY_UNMAPPED:
<n>

SKIP_NON_VERIFICATION_ROW:
<n>

UNEXPLAINED:
0

Coverage Ledger:
.ai-context/project_graph/migration/PHASE2D_SOURCE_COVERAGE.md

================================
STABLE VERIFICATION IDS
================================

Source Stable IDs:
<n>

Duplicate Source IDs:
0 / <details>

Existing Graph Verification IDs:
<n>

Stable IDs Updated:
<n>

Stable IDs Newly Migrated:
<n>

Stable IDs Lost:
0

Identity Conflicts:
0 / <details>

================================
KNOWN ANCHORS
================================

E-K1I-01:
Disposition:
<...>
WorkItem:
K1i
Kind:
device
Status:
<...>
Required:
<...>

E-K1I-02:
Disposition:
<...>
WorkItem:
K1i
Kind:
device
Status:
<...>
Required:
<...>

================================
WORKITEM MAPPING
================================

Mapped Verification Rows:
<n>

Deferred Unmapped:
<n>

WORKITEM_UNMAPPED:
<n>

VERIFY_ID_MISSING:
<n>

WORKITEM_MISSING_FOR_VERIFY:
<n>

New WorkItems Created:
0 / <list + justification>

================================
VERIFICATION GRAPH
================================

Total Verification Entities:
<n>

By Status:
pending:
<n>
pass:
<n>
fail:
<n>
blocked:
<n>
not_required:
<n>

By Kind:
device:
<n>
...

By Feature:
F-MEAL:
<n>
F-AI-MEAL:
<n>
F-TIMELINE:
<n>
F-INGREDIENT:
<n>
F-DISH:
<n>
F-PANTRY:
<n>
F-RECOMMEND:
<n>
F-NUTRITION:
<n>
F-HEALTH:
<n>
F-FAMILY:
<n>
F-WEEKPLAN:
<n>
F-SYNC:
<n>
F-TOOLS:
<n>

================================
CLOSURE AUDIT
================================

Done WorkItems Audited:
<n>

Closure Violations Found:
<n>

CLOSURE_STATUS_REPAIR:
<n>

WorkItems Changed done → verifying:
<list / NONE>

Auto verifying → done:
0

WORK_STATUS_REVIEW_REQUIRED:
<n>

================================
OBSERVED BOUNDARY
================================

Build/Test/Lint/PgCheck Verification Created:
NO

Observed Store Implemented:
NO

================================
CONFLICTS
================================

Conflict Ledger:
.ai-context/project_graph/migration/PHASE2D_CONFLICTS.md

Blocking Conflicts:
<n>

Non-Blocking Conflicts:
<n>

Open Conflicts:
<list / NONE>

Architecture Change Required:
NO / <details>

================================
BOUNDARIES
================================

Schema Changed:
NO

Validator Contract Changed:
NO

Plan Status Changed:
NO

CurrentWork Changed:
NO

Business Relations Added:
NO

Production Code Changed:
NO

Graph Mode:
draft

Phase 2E:
NOT STARTED

================================
VALIDATION
================================

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

Duplicate Verification IDs:
0

Missing Verification WorkItem Refs:
0

Deviations:
NONE / <details>

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 178. 本阶段最终正确状态

本任务完成后仓库只能表达：

```text
Phase 1
FINAL ACCEPT / FROZEN

Phase 2A
ACCEPT / CLOSED

Phase 2B
ACCEPT / CLOSED

Phase 2C
ACCEPT / CLOSED

Phase 2D
IMPLEMENTED
WAITING FOR ARCHITECTURE REVIEW

Phase 2E
NOT STARTED

Graph Mode
draft
```

---

# 179. Phase 2D 成功的真正标准

不是：

```text
迁进多少条 Verification
```

而是：

```text
每一条 Verification Source Row
都知道去了哪里

每一个 Stable Verification ID
都没有被重新解释

每一个 Graph Verification
都知道验证哪个 WorkItem

每一个 status
都有真实验证结果依据

每一个无法确定的项
都明确隔离，而不是猜

Verification Closure
保持自洽
```

---

# 180. 最后执行原则

本阶段遵循：

```text
Inventory first

Coverage second

Mapping third

Graph write fourth

Closure audit fifth

Validation last
```

禁止：

```text
边读 Checklist
边随手写 YAML
```

这条是 Phase 2D 最重要的施工纪律之一。

