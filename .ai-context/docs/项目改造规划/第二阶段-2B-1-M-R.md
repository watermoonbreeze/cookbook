[toc]

# Project Graph Phase 2B Migration Reconciliation 实施蓝图

## PG-P2B-R1 · Stable-ID Recovery + Source Coverage + Truth Reconciliation

**任务类型**：Phase 2B 返工 / 数据迁移对账
**执行模型**：DeepSeek V4 Flash
**审核基准提交**：`6152e8f373e1e006cbfcadbf7bbb9cec03de3e7e`
**Phase 2A**：`ACCEPT / CLOSED`
**Phase 2B 当前审核结论**：`REWORK`
**Phase 2C**：`NOT STARTED`
**Graph Mode**：继续 `draft`
**Schema 修改**：禁止
**Validator Contract 修改**：禁止
**生产代码修改**：禁止

---

# 0. 本轮任务性质

当前 Phase 2B 已经建立第一版 WorkItem Graph，但架构复审发现：

```text
结构合法
≠
迁移完整
≠
项目事实正确
```

当前 61 项 Project Graph tests 与 `pg check` 可以证明：

```text
Schema
References
Sharding
基础语义
```

合法。

但不能证明：

```text
所有源待办都被处理
所有已有 Stable ID 都被保存
WorkItem 状态完全正确
重复项没有丢 ID
Feature ownership 没有猜测
```

因此本轮只做：

> Phase 2B Migration Reconciliation。

不重新设计 Project Graph。

不重新迁移已正确的数据。

---

# 1. 已知必须修复的问题

本轮至少关闭：

| ID      | 问题                                   |
| ------- | ------------------------------------ |
| P2B-R01 | `FAM-AGE` Stable ID 被错误替换            |
| P2B-R02 | `FAM-MEAL` Stable ID 被错误替换           |
| P2B-R03 | `K15` 被错误合并进 `I7`                    |
| P2B-R04 | `J22` 被合并后 Stable ID 消失              |
| P2B-R05 | 新 `TODO-*` ID 与 `kind: feature` 不一致  |
| P2B-R06 | `K1c` 状态被错误迁为 `backlog`              |
| P2B-R07 | `L3` Feature ownership 处理违反 No-Guess |
| P2B-R08 | Source Coverage 不完整                  |
| P2B-R09 | “低优先级/二期规划 = 不迁”属于未经授权过滤规则           |
| P2B-R10 | Inventory 无法证明每个源事项都有唯一 disposition  |

以上全部属于：

> Migration Truth / Coverage 问题。

不是 Schema 问题。

---

# 2. 开始前检查

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

当前 HEAD 应至少包含：

```text
6152e8f373e1e006cbfcadbf7bbb9cec03de3e7e
```

如果 HEAD 已有后续提交：

* 不 reset；
* 不 checkout；
* 不 clean；
* 不覆盖用户修改；
* 先核实后续提交。

存在未提交用户修改：

> 不得清理或覆盖。

---

# 3. 本轮必须读取的 Project Graph 文件

必须先读取：

```text
.ai-context/project_graph/README.md

.ai-context/project_graph/project.yaml

.ai-context/project_graph/migration/PHASE1_FINAL_ACCEPT.md

.ai-context/project_graph/migration/PHASE2A_ACCEPT.md

.ai-context/project_graph/migration/PHASE2A_REVIEW.md

.ai-context/project_graph/migration/PHASE2A_TO_2B_HANDOFF.md

.ai-context/project_graph/migration/PHASE2B_INVENTORY.md

.ai-context/project_graph/migration/PHASE2B_CONFLICTS.md
```

然后读取：

```text
.ai-context/project_graph/features/*.yaml
```

重点是当前已迁入的 WorkItems。

---

# 4. 必须重新枚举全部 WorkItem Source

不得只读取上一版 Inventory。

必须重新扫描：

```bash
find .ai-context/docs/feature -maxdepth 1 -type f
```

找出所有：

```text
待办*.md
```

以及实际承担 WorkItem 台账功能的同级文件。

至少应覆盖当前实际存在的：

```text
待办索引.md
待办_Bug修复.md
待办_功能算法.md
待办_UI交互*.md
待办_数据健康*.md
待办_工程*.md
待办_合规*.md
待办_战略*.md
```

文件名以真实仓库为准。

禁止只按这里的示例名硬编码。

---

# 5. 当前状态高优先级 Truth

必须重新读取：

```text
.ai-context/docs/context_memory/SESSION_交接.md

.ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

以及：

> 当前最新唯一真机验证清单。

Phase 2B 不迁完整 Verification，但 WorkItem 当前状态判断可能需要验证事实。

---

# 6. Source Coverage 是本轮最高优先级 Gate

上一版最大的缺陷是：

> 部分源事项没有进入 Graph，也没有进入 Inventory。

本轮必须达到：

```text
SOURCE ACTIONABLE ITEMS
        ↓
100% Disposition
```

不允许任何可执行事项静默消失。

---

# 7. 新建 Source Coverage Ledger

新增：

```text
.ai-context/project_graph/migration/PHASE2B_SOURCE_COVERAGE.md
```

它属于：

> Migration Audit Artifact。

不是 Project Truth。

---

# 8. 什么叫 Actionable Source Item

以下属于 actionable：

```text
有明确 ID 的 Bug/Todo/WorkItem

没有 ID 但明确要求未来修复/实现的事项

明确标为：
未完成
待办
进行中
暂缓
二期
后续
低优先级
验证中
待设计
```

以下不属于 actionable：

```text
章节标题
说明性文字
方法论
历史背景描述
纯统计
引用其它事项但没有独立行动要求的文字
```

---

# 9. 每个 Actionable Source Item 必须有且只有一个 Disposition

只允许：

```text
MIGRATE_EXISTING_ID
```

已有 Stable ID，迁入/保留 Graph。

---

```text
MIGRATE_NEW_ID
```

无 Stable ID，但属于当前应管理事项，分配新 ID。

---

```text
KEEP_EXISTING_GRAPH
```

源文档再次描述一个 Graph 中已经正确存在的同一 WorkItem。

---

```text
MERGE_NO_ID_DUPLICATE
```

源事项没有 Stable ID，并且能够明确证明它只是已有 WorkItem 的重复描述。

---

```text
SKIP_HISTORY
```

仅允许：

> 已完成历史事项，并且不再解释当前状态、不被当前 Plan/Verify/Work 引用。

---

```text
DEFER_WITH_REASON
```

只有存在真正架构或 ownership 冲突、当前无法安全写入 Graph 时使用。

必须有明确 reason。

---

# 10. 禁止的 Disposition 逻辑

以下全部禁止：

```text
低优先级
→ 不迁
```

```text
二期
→ 不迁
```

```text
以后再做
→ 不迁
```

```text
暂时不重要
→ 不迁
```

这些事项只要仍然是未来真实工作：

> 就仍然属于 Current Project State。

---

# 11. 低优先级 / 二期事项应该如何处理

### 明确存在、未来仍计划实施

一般：

```text
backlog
```

---

### 明确已经决定暂缓，等待未来条件重新开启

使用：

```text
parked
```

---

### 只有明确取消

才能：

```text
cancelled
```

---

# 12. PHASE2B_SOURCE_COVERAGE.md 每条格式

建议：

```text
SOURCE:
.ai-context/docs/feature/待办_UI交互.md

SOURCE ITEM:
U1

TITLE:
...

STABLE ID:
U1

DISPOSITION:
MIGRATE_EXISTING_ID

TARGET:
work:U1

FEATURE:
F-...

STATUS:
backlog

EVIDENCE:
<当前状态依据>

NOTES:
...
```

无 ID：

```text
STABLE ID:
NONE

DISPOSITION:
MIGRATE_NEW_ID

TARGET:
TODO-TOOLS-003
```

---

# 13. 每个源文件必须有 Coverage Summary

例如：

```text
Source:
待办_UI交互.md

Actionable Items:
18

MIGRATE_EXISTING_ID:
7

MIGRATE_NEW_ID:
6

KEEP_EXISTING_GRAPH:
2

MERGE_NO_ID_DUPLICATE:
1

SKIP_HISTORY:
2

DEFER_WITH_REASON:
0

UNEXPLAINED:
0
```

核心门禁：

```text
UNEXPLAINED = 0
```

---

# 14. 全局 Coverage Summary

文件最后必须汇总：

```text
TOTAL ACTIONABLE ITEMS

TOTAL DISPOSITIONED ITEMS

UNEXPLAINED ITEMS
```

必须：

```text
TOTAL ACTIONABLE ITEMS
=
TOTAL DISPOSITIONED ITEMS
```

且：

```text
UNEXPLAINED = 0
```

---

# 15. Stable ID 的定义升级

Stable ID 不限于：

```text
I*
J*
K*
L*
AF-*
```

任何已经在原始项目台账中明确用作事项标识、并可稳定引用的 ID，都视为 Stable ID。

包括：

```text
FAM-AGE
FAM-MEAL
U1
U2
...
```

以及仓库实际存在的其它稳定标签。

---

# 16. Stable ID Preservation 绝对规则

已有 Stable ID：

> 永远不得被 Project Graph 新生成 ID 替代。

例如禁止：

```text
FAM-AGE
↓
TODO-FAMILY-002
```

---

# 17. P2B-R01 — 恢复 FAM-AGE

定位当前对应：

```text
TODO-FAMILY-002
```

或实际 Graph 中用于替代 FAM-AGE 的节点。

核实它与源：

```text
FAM-AGE
```

确实是同一个事项。

如果确认：

执行：

```text
删除错误生成 ID 节点
恢复 Stable ID：
FAM-AGE
```

保留：

* title；
* feature；
* kind；
* status；
* source_refs；

但必须按真实源重新确认。

---

# 18. P2B-R02 — 恢复 FAM-MEAL

同理：

```text
TODO-FAMILY-003
```

如对应：

```text
FAM-MEAL
```

则恢复：

```text
FAM-MEAL
```

不得继续保留替代 ID。

---

# 19. 修改 ID 前必须搜索全 Graph 引用

对：

```text
TODO-FAMILY-002
TODO-FAMILY-003
```

执行全仓 Graph 搜索：

```bash
rg "TODO-FAMILY-002|TODO-FAMILY-003" .ai-context/project_graph
```

如有：

* Plan refs；
* Verify refs；
* Relation refs；
* Current refs；

必须同步更新。

正常情况下 Phase 2B 尚未迁 Plan/Verify，不应很多。

但必须搜索，不能假定。

---

# 20. Stable ID 重复项的新规则

这是上一版没有定义清楚的地方。

存在：

```text
两个源事项
都有 Stable ID
但语义高度重叠
```

时：

> 不允许合并后让一个 Stable ID 消失。

---

# 21. 双 Stable ID 重复项处理

例如：

```text
L2
J22
```

或：

```text
I7
K15
```

如果两者都有 Stable ID：

必须：

```text
KEEP BOTH IDs
```

Phase 2B 阶段不删除其中一个。

---

# 22. 为什么不能直接 Merge

因为：

```text
Stable ID
```

不仅表示当前内容。

还可能被：

* 历史 commit；
* Plan；
* Verification；
  -旧文档；
* AI上下文；

长期引用。

删除其中一个会破坏身份连续性。

---

# 23. 重复关系交给 Phase 2C

Phase 2B 只记录：

```text
DUPLICATE_RELATION_PENDING_2C
```

到 Conflict Ledger。

Phase 2C 再根据真实语义使用：

```text
related_to
supersedes
```

等 Relation。

不得 Phase 2B 提前创造新 Relation。

---

# 24. P2B-R03 — K15 与 I7

必须恢复：

```text
K15
```

独立 WorkItem。

禁止：

```text
K15
→ merge into I7
→ K15消失
```

---

# 24.1 K15 与 I7 的已知差异

审核发现：

```text
I7
```

核心主要涉及：

> AI 失败后的静默 fallback / 降级问题。

而：

```text
K15
```

还包含：

> 按天 / 餐次分段、防 token 截断、可控降级。

因此：

> 两者不能视为完全同一个 WorkItem。

---

# 24.2 实施

从原始源重新读取：

```text
I7
K15
```

分别建立/保留：

```text
work:I7
work:K15
```

分别：

* title；
* kind；
* status；
* source_refs。

不要因为共享 Plan 就合并 WorkItem。

---

# 25. P2B-R04 — J22 与 L2

必须同时保留：

```text
J22
L2
```

两个 Stable IDs。

即使它们都描述：

> 脂肪肝 App 入口

Phase 2B 也不能让其中一个 ID 消失。

---

# 25.1 Primary Feature

本蓝图冻结：

```text
J22
L2
→ F-HEALTH
```

因为它们的主要交付对象属于健康/疾病相关能力。

不要迁到：

```text
F-TOOLS
```

---

# 25.2 Relation

Phase 2B：

> 不建立 duplicate/supersedes relation。

在：

```text
PHASE2B_CONFLICTS.md
```

记录：

```text
DUPLICATE_RELATION_PENDING_2C:
J22
L2
```

Phase 2C 处理。

---

# 26. 只有“无 Stable ID 的重复描述”允许 Merge

例如：

```text
源文档 A:
“家庭年龄显示有误”

源文档 B:
“成员年龄计算错误”

其中 A 无 ID
B = FAM-AGE
```

如果读完整描述后确定完全同一事项：

允许：

```text
MERGE_NO_ID_DUPLICATE
→ FAM-AGE
```

无 ID 那一条不需要再创造 ID。

---

# 27. 新生成 ID 与 kind 一致性

Phase 2B 新 ID 是迁移 convention，不是核心 Schema。

但必须保持：

```text
ID Prefix
↔
kind
```

一致。

---

# 28. 当前 `TODO-* + kind: feature` 错误

至少重新核实当前 Graph 中：

```text
TODO-INGREDIENT-001

TODO-RECOMMEND-001

TODO-NUTRITION-001

TODO-NUTRITION-002

TODO-FAMILY-001
```

以及所有其它：

```text
TODO-*
```

节点。

---

# 29. 默认规则

对于匿名旧待办：

如果只是：

```text
UI改进
数据补齐
体验增强
现有功能完善
现有流程优化
```

使用：

```text
kind: todo
```

即使源文档把它放在“功能”章节。

---

# 30. 什么情况下才使用 kind: feature

只有明确表示：

> 新增一个此前不存在的产品/业务能力。

并且它不是已有 Stable ID WorkItem。

如果无法明确判断：

```text
kind: todo
```

并写 source_refs。

---

# 31. 本轮不创造新的 FEATURE-* ID convention

为了避免继续扩大迁移规则：

> 本轮不要为匿名事项创造新的 FEATURE-* / FEAT-* 编号规则。

如果发现一个匿名事项确实必须是：

```text
kind: feature
```

但没有 Stable ID：

记录：

```text
KIND_ID_CONVENTION_REQUIRED
```

到 Conflict Ledger。

暂不迁移该项。

---

# 32. P2B-R05 验收

所有：

```text
TODO-*
```

新生成 ID：

必须：

```text
kind: todo
```

除非它是 Phase 2B 开始前就已经存在的历史稳定 ID。

---

# 33. P2B-R06 — K1c 状态

当前 Graph：

```text
K1c = backlog
```

审核发现源：

```text
待办_功能算法.md
```

标记：

```text
K1c 🔄
```

同时：

```text
SESSION
BLUEPRINT_STATE
```

没有发现更高优先级的新状态覆盖 K1c。

因此本蓝图直接冻结：

```text
K1c.status = in_progress
```

---

# 34. K1c 不得自行改成其它状态

本轮除非执行时发现：

> 当前 master 中已经新增了比审核时更新且更高优先级的明确状态记录。

否则：

```text
K1c
→ in_progress
```

---

# 35. P2B-R07 — L3 Feature Ownership

审核发现：

```text
L3
```

定义是：

> 全 App 基础功能自动化进阶。

覆盖：

```text
食材
菜品
餐次
计划
库存
营养
```

因此：

```text
L3
```

显然不是单纯：

```text
F-AI-MEAL
```

---

# 36. L3 本轮 Primary Feature 决策

在当前冻结的 13 Feature Universe 下，本蓝图决定：

```text
L3
→ F-TOOLS
```

作为：

> 当前阶段的 Cross-Cutting / App-wide Automation Primary Owner。

原因：

* L3 横跨多个产品 Feature；
* 当前没有通用 AI Platform Feature；
* 不允许 2B 自行新增 Feature；
* F-TOOLS 当前承担通用 App / 工具 / 横切能力边界。

---

# 37. L3 同时记录 Feature Split Candidate

在：

```text
PHASE2B_CONFLICTS.md
```

记录：

```text
FEATURE_SPLIT_CANDIDATE

work:
L3

temporary_primary:
F-TOOLS

reason:
跨食材/菜品/餐次/计划/库存/营养的全App自动化，
当前13 Feature中无独立AI Platform Feature。

decision:
Phase 2B 使用 F-TOOLS 作为 Primary Feature，
不得新增 Feature。

follow_up:
Phase 2E architecture reconcile
```

---

# 38. 为什么允许 L3 迁入 F-TOOLS

这是本蓝图的架构决策。

Flash 不需要再判断：

```text
F-AI-MEAL？
F-TOOLS？
新增 Feature？
```

直接执行：

```text
L3 → F-TOOLS
```

Phase 2C 再通过：

```text
affects
```

表达它对多个 Feature 的影响。

---

# 39. Source Coverage 重新扫描重点

上一轮明确发现遗漏风险。

本轮必须特别查找：

```text
I-Mine
I-About
U1
U2
U3
U4
U5
```

以及：

```text
AI S4
端侧 OCR
工程性能优化
EER
食物交换份
维生素补充
全库忌口补漏
```

这些只是：

> 审核锚点。

不是完整列表。

---

# 40. 不能只补审核点

禁止：

```text
把上面列出的十几项补进去
→ 就认为 Source Coverage 完成
```

必须重新枚举：

```text
所有待办 Source
→ 所有 Actionable Items
→ 100% disposition
```

---

# 41. 当前未完成事项必须迁移

只要源事项当前是：

```text
待做
进行中
二期
低优先级
后续
暂缓
```

且没有明确取消：

必须：

```text
Graph WorkItem
```

或：

```text
DEFER_WITH_REASON
```

---

# 42. “二期规划”状态规则

如果只是表示：

> 第二阶段再实施。

而仍然是明确计划：

使用：

```text
backlog
```

如果明确：

> 当前暂停，等待未来重新启动。

才使用：

```text
parked
```

---

# 43. “低优先级”状态规则

低优先级：

> 不是 status。

仍按真实阶段判断：

```text
未开始
→ backlog
```

priority 如当前 Schema 有可表达字段：

按已有 Contract 使用。

没有：

> 不新增 Schema 字段。

---

# 44. Priority 不得决定是否迁移

禁止：

```text
priority=low
→ skip
```

---

# 45. Source Coverage 与旧“待办索引”的关系

`待办索引.md`：

主要用于：

```text
发现 WorkItem
已有 ID
关联专项台账
```

但如果专项待办内容更完整：

> 专项待办用于身份/内容。

当前状态仍按照高优先级 Truth。

---

# 46. 状态 Truth Priority

严格：

```text
SESSION
+
BLUEPRINT_STATE
+
最新 Verification Fact

>

专项当前状态

>

待办台账

>

待办索引
```

---

# 47. 状态冲突必须记录

如果：

```text
SESSION
```

与：

```text
BLUEPRINT_STATE
```

真的互相矛盾：

记录：

```text
STATE_CONFLICT
```

不得任选一个然后不记录。

---

# 48. Emoji 状态不能机械映射全部场景

例如：

```text
🔄
```

通常：

```text
in_progress
```

但仍需确认有没有更高优先级状态覆盖。

---

# 49. CODE + ARCH ACCEPTED

继续执行冻结规则：

```text
CODE accepted
+
ARCH accepted
+
required device pending
=
verifying
```

绝不是：

```text
done
```

---

# 50. Phase 2D 边界继续保持

本轮不得把完整真机清单迁入 Graph。

WorkItem 状态可以读取验证清单判断。

但不要新建大量：

```text
E-*
```

节点。

---

# 51. Phase 2C 边界继续保持

本轮不得批量创建 Plan。

已有 PoC Plan 保持。

对于已有正式蓝图但 Graph 尚无 Plan：

使用：

```text
source_refs
```

即可。

Phase 2C 创建 Plan Node。

---

# 52. 更新 PHASE2B_INVENTORY.md

上一版 Inventory 不删除。

更新为：

```text
RECONCILED
```

并补充：

```text
Original Migration:
52 WorkItems

Reconciliation:
<新增>
<恢复Stable IDs>
<修正状态>
<修正kind>
<deferred>
```

---

# 53. Inventory 必须明确移除旧错误规则

删除或明确废弃：

```text
低优先级 → 不迁
二期规划 → 不迁
```

改成：

```text
Low Priority:
does not affect migration eligibility

Phase-2/Future Planned:
migrate as backlog/parked according to actual state
```

---

# 54. 更新 PHASE2B_CONFLICTS.md

必须至少处理/记录：

### K15 / I7

```text
KEEP_BOTH_STABLE_IDS
RELATION_PENDING_2C
```

---

### J22 / L2

```text
KEEP_BOTH_STABLE_IDS
DUPLICATE_RELATION_PENDING_2C
```

---

### L3

```text
FEATURE_SPLIT_CANDIDATE
temporary primary = F-TOOLS
follow-up = 2E
```

---

# 55. 不允许已经解决的问题继续写 Open

例如：

```text
FAM-AGE 被替代
```

修复后：

标记：

```text
RESOLVED
```

不要继续算 Open Conflict。

---

# 56. 每个 WorkItem source_refs

新补迁的 WorkItem：

必须至少有：

```text
1 个身份来源
```

如果状态来源和身份来源不同：

建议：

```text
2 个 source_refs
```

例如：

```yaml
source_refs:
  - .ai-context/docs/feature/待办_功能算法.md
  - .ai-context/docs/context_memory/SESSION_交接.md
```

---

# 57. source_refs 不验证 anchor

沿用 Phase 2A Frozen Contract。

不要修改 Validator。

---

# 58. Feature shard 规则继续冻结

WorkItem 必须：

```text
文件：
features/F-X.yaml

feature:
F-X
```

一致。

---

# 59. 不重复建立跨 Feature WorkItem

例如 L3：

只有：

```text
work:L3
```

一份。

不要在：

```text
F-INGREDIENT
F-DISH
F-WEEKPLAN
...
```

分别复制 L3。

Phase 2C 用 Relation。

---

# 60. Source Coverage 自检脚本

允许写：

> 临时、不提交的 Python 脚本。

用于检查：

```text
Coverage Ledger
```

的：

```text
Actionable
Disposition
Unexplained
```

总数。

本轮不要求把该脚本加入 Project Graph Tool。

---

# 61. WorkItem Graph 统计

完成后重新统计：

```text
Total WorkItems

By Feature
By Kind
By Status

Stable Existing IDs
New Generated IDs

Restored IDs
Removed Wrong Generated IDs

Skipped Historical
Deferred
```

这些统计：

> 不写进 Graph 字段。

---

# 62. Generated ID audit

检查所有本 Phase 新生成：

```text
BUG-*
TODO-*
TECH-*
REFACTOR-*
COMP-*
RESEARCH-*
MAINT-*
```

确认：

```text
prefix
↔
kind
```

一致。

---

# 63. 已有 Stable ID 不受 prefix-kind 规则影响

例如：

```text
K1g
L3
FAM-AGE
U1
```

其 ID 本身不编码 kind。

只按真实语义填写 kind。

---

# 64. 对 TODO-* 的强制审计

执行：

```bash
rg "id: TODO-" .ai-context/project_graph/features
```

逐个核实：

```text
kind: todo
```

全部一致。

---

# 65. ID replacement audit

执行全仓搜索确保：

```text
TODO-FAMILY-002
TODO-FAMILY-003
```

如果已经确定是错误替代：

不得继续作为独立节点存在。

---

# 66. Stable ID coverage audit

在 source coverage ledger 中所有：

```text
STABLE ID != NONE
```

且当前事项未历史关闭者：

必须满足：

```text
Graph contains that exact ID
```

除非：

```text
DEFER_WITH_REASON
```

且 reason 属于架构阻断。

---

# 67. 双 Stable ID duplicate audit

所有：

```text
两个已有 Stable IDs 被判断重复
```

必须确保：

```text
两个 ID 都存在
```

不能只有 canonical 一个。

---

# 68. SKIP_HISTORY 严格门禁

只有满足全部：

```text
status = historically completed

AND
not referenced by current WorkItem

AND
not referenced by current Plan

AND
not referenced by current Verification

AND
not needed to explain current Feature state

AND
not referenced by SESSION/BLUEPRINT
```

才允许：

```text
SKIP_HISTORY
```

---

# 69. DEFER_WITH_REASON 严格门禁

只能用于：

```text
Feature ownership truly unresolved

Stable ID collision truly unresolved

Source identity missing

Architecture Change required
```

不得用于：

```text
工作量太大
低优先级
以后再做
```

---

# 70. Phase 2B Completion Definition

本轮后 Phase 2B 的数据层必须满足：

```text
Current actionable WorkItems
= covered

Stable IDs
= preserved

Anonymous current work
= assigned IDs

Statuses
= reconciled

Feature ownership
= explicit

Unexplained source items
= 0
```

---

# 71. 不要求 Conflict = 0

允许少量：

```text
architecture conflict
```

例如：

```text
L3 Feature split candidate
```

但必须：

* 不污染 Stable ID；
* 有 temporary safe representation；
* 有明确 follow-up phase。

---

# 72. 不允许 Source Coverage Conflict

以下必须为：

```text
0
```

```text
UNEXPLAINED SOURCE ITEM

LOST STABLE ID

UNKNOWN DISPOSITION
```

---

# 73. Schema Freeze

本轮绝对禁止修改：

```text
.ai-context/project_graph/schema/project-graph.schema.json
```

---

# 74. Validator Freeze

禁止修改：

```text
tools/project_graph.py
tools/yaml_lite.py
tools/schema_checker.py
```

以及现有 Contract 测试语义。

---

# 75. 如果发现 Validator 真 Bug

记录：

```text
VALIDATOR_BUG_CANDIDATE
```

不要本轮修。

---

# 76. 如果发现 Schema 无法表达真实 WorkItem

记录：

```text
ARCH_CHANGE_REQUIRED
```

跳过该项写入。

继续其它事项。

最终报告。

---

# 77. README

本轮不把：

```text
Phase 2B
```

标记为 ACCEPT/CLOSED。

因为本轮完成后仍要外部架构审核。

可以将当前说明更新为：

```text
Phase 2B
RECONCILED
WAITING FOR ARCHITECTURE REVIEW
```

如果 README 当前有 Phase Progress 区域。

不要写：

```text
Phase 2B ACCEPT
```

---

# 78. project.yaml

Graph：

```yaml
mode: draft
```

保持。

CurrentWork：

正常情况下不修改。

只有发现当前值与最高优先级 Truth 明显冲突时：

> 不自行修改。

记录到：

```text
PHASE2B_CONFLICTS.md
```

Phase 2E 统一 Reconcile。

---

# 79. 测试

运行全部现有：

```text
Project Graph tests
```

不得减少测试。

记录真实：

```text
Command
Total
Pass
Fail
```

---

# 80. pg check

执行：

```bash
python .ai-context/project_graph/tools/project_graph.py check
```

必须：

```text
PASS
0 issue
```

---

# 81. Source Coverage Gate

必须额外报告：

```text
Source Files:
<count>

Actionable Items:
<count>

Dispositioned:
<count>

Unexplained:
0
```

---

# 82. Stable ID Gate

必须报告：

```text
Existing Stable IDs Discovered:
<count>

Present In Graph:
<count>

Deferred With Reason:
<count>

Lost:
0
```

---

# 83. Generated ID Gate

报告：

```text
New IDs:
<count>

Prefix/Kind Mismatch:
0
```

---

# 84. Known audit anchors

最终至少显式确认：

```text
FAM-AGE
FAM-MEAL
K15
I7
J22
L2
K1c
L3
I-Mine
I-About
U1
U2
U3
U4
U5
```

每一个都有 disposition。

---

# 85. 2B Rework Gate

全部通过才允许提交。

### GATE-2B-R01

`FAM-AGE` Stable ID 恢复。

### GATE-2B-R02

`FAM-MEAL` Stable ID 恢复。

### GATE-2B-R03

错误替代 ID 不再独立存在。

### GATE-2B-R04

`K15` 独立存在。

### GATE-2B-R05

`I7` 独立存在。

### GATE-2B-R06

`J22` 独立存在。

### GATE-2B-R07

`L2` 独立存在。

### GATE-2B-R08

双 Stable ID duplicate 不丢 ID。

### GATE-2B-R09

所有 `TODO-*` → `kind: todo`。

### GATE-2B-R10

`K1c = in_progress`，除非发现新的高优先级 Truth。

### GATE-2B-R11

`L3 → F-TOOLS`。

### GATE-2B-R12

L3 记录 `FEATURE_SPLIT_CANDIDATE`。

### GATE-2B-R13

没有再使用“低优先级不迁”。

### GATE-2B-R14

没有再使用“二期规划不迁”。

### GATE-2B-R15

全部 Source Actionable Items 有 disposition。

### GATE-2B-R16

`UNEXPLAINED = 0`。

### GATE-2B-R17

所有当前未完成事项已迁或明确 defer。

### GATE-2B-R18

Stable ID Lost = 0。

### GATE-2B-R19

Generated ID Prefix/Kind mismatch = 0。

### GATE-2B-R20

没有重复迁同一无-ID问题。

### GATE-2B-R21

历史完成噪音没有全量迁入。

### GATE-2B-R22

没有提前批量迁 Plan。

### GATE-2B-R23

没有提前批量迁 Verification。

### GATE-2B-R24

Schema 0 修改。

### GATE-2B-R25

Validator Contract 0 修改。

### GATE-2B-R26

生产代码 0 修改。

### GATE-2B-R27

Graph mode = draft。

### GATE-2B-R28

Project Graph tests PASS。

### GATE-2B-R29

pg check PASS。

### GATE-2B-R30

Phase 2C NOT STARTED。

---

# 86. Diff Gate

执行：

```bash
git diff --stat
git diff
```

允许主要修改：

```text
.ai-context/project_graph/features/*.yaml

.ai-context/project_graph/migration/PHASE2B_INVENTORY.md

.ai-context/project_graph/migration/PHASE2B_CONFLICTS.md

.ai-context/project_graph/migration/PHASE2B_SOURCE_COVERAGE.md

.ai-context/project_graph/README.md
```

正常情况下：

> 不需要修改任何 tools/schema 文件。

---

# 87. Commit

本轮建议只做一个 Rework Commit：

```text
fix(project-graph): reconcile phase 2b workitem migration
```

如果仓库有更严格规范：

使用等价格式。

---

# 88. Push

Commit 后 push 当前远程分支。

---

# 89. 完成后 STOP

Push 后：

> 必须停止。

禁止继续：

```text
Phase 2B END
2B → 2C Handoff
Phase 2C
```

因为：

> Phase 2B Rework 还需要外部架构审核。

下一轮只有得到：

```text
Phase 2B = ACCEPT
```

才生成：

```text
Phase 2B END
+
Phase 2B → 2C Handoff
+
Phase 2C PREVIEW / START
```

---

# 90. 最终汇报格式

严格输出：

```text
Project Graph:
Phase 2B — Migration Reconciliation

Review Baseline:
6152e8f373e1e006cbfcadbf7bbb9cec03de3e7e

Completion Commit:
<完整 SHA>

========================
KNOWN REVIEW FIXES
========================

FAM-AGE:
<result>

FAM-MEAL:
<result>

K15:
<result>

I7:
<result>

J22:
<result>

L2:
<result>

K1c:
<result>

L3:
<result>

TODO Prefix/Kind Audit:
<result>

========================
SOURCE COVERAGE
========================

Source Files:
<count>

Actionable Items:
<count>

Dispositioned:
<count>

MIGRATE_EXISTING_ID:
<count>

MIGRATE_NEW_ID:
<count>

KEEP_EXISTING_GRAPH:
<count>

MERGE_NO_ID_DUPLICATE:
<count>

SKIP_HISTORY:
<count>

DEFER_WITH_REASON:
<count>

UNEXPLAINED:
0

Coverage Ledger:
.ai-context/project_graph/migration/PHASE2B_SOURCE_COVERAGE.md

========================
STABLE IDS
========================

Stable IDs Discovered:
<count>

Present In Graph:
<count>

Deferred With Reason:
<count>

Lost:
0

Restored:
<list>

Duplicate Stable IDs Kept:
<list>

========================
WORKITEM GRAPH
========================

Total WorkItems:
<count>

By Feature:
F-MEAL: ...
F-AI-MEAL: ...
F-TIMELINE: ...
F-INGREDIENT: ...
F-DISH: ...
F-PANTRY: ...
F-RECOMMEND: ...
F-NUTRITION: ...
F-HEALTH: ...
F-FAMILY: ...
F-WEEKPLAN: ...
F-SYNC: ...
F-TOOLS: ...

By Kind:
bug: ...
todo: ...
feature: ...
tech_debt: ...
refactor: ...
compliance: ...
research: ...
maintenance: ...

By Status:
backlog: ...
ready: ...
in_progress: ...
blocked: ...
review: ...
verifying: ...
done: ...
parked: ...
cancelled: ...

Generated IDs:
<count>

Prefix/Kind Mismatch:
0

Skipped Historical:
<count>

========================
CONFLICTS
========================

Conflict Ledger:
.ai-context/project_graph/migration/PHASE2B_CONFLICTS.md

Open Architecture Conflicts:
<count>

L3 Feature Split Candidate:
RECORDED

K15/I7 Relation:
PENDING PHASE 2C

J22/L2 Relation:
PENDING PHASE 2C

Architecture Change Required:
NO / <details>

========================
BOUNDARIES
========================

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

Phase 2C:
NOT STARTED

========================
VALIDATION
========================

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

Deviations:
NONE / <details>

Status:
WAITING FOR ARCHITECTURE REVIEW
```

---

# 91. 本轮最终目标

完成后必须达到：

```text
Phase 2A
ACCEPT / CLOSED

Phase 2B
MIGRATION RECONCILED
WAITING FOR ARCH REVIEW

Stable IDs
LOSSLESS

Current Source Coverage
100%

Unexplained Source Items
0

Schema
FROZEN

Validator Contract
FROZEN

Graph Mode
draft

Phase 2C
NOT STARTED
```

本轮成功的标准不是：

> WorkItem 数量越多越好。

而是：

> 每一个当前真实 WorkItem 都能解释“它来自哪里、为什么存在、为什么属于这个 Feature、为什么是这个状态”，并且任何已有 Stable ID 都没有在迁移过程中消失。

