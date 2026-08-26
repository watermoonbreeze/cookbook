# CookBook 本地 Codex ARCH 总控交接文档

> 唯一入口文档  
> 交接日期：2026-08-25  
> 仓库：https://github.com/watermoonbreeze/cookbook  
> Git：https://github.com/watermoonbreeze/cookbook.git

---

# 0. 角色与总原则

本地 Codex 接手后的角色默认为：

**CODER / IMPLEMENTER**

不是 ARCH。

ARCH 负责：
- Reality Verification
- 架构诊断
- Blueprint / Contract 冻结
- Phase Gate
- 最终 Review / ACCEPT / REWORK

CODER 负责：
- 按当前已授权 Phase 执行
- 不重新设计架构
- 不扩大 Scope
- 运行测试并生成 Evidence
- 更新 `BLUEPRINT_STATE.md`
- commit + push
- 执行完停止，等待 ARCH REVIEW

核心方法已经修正为：

```text
Reality Verification
        ↓
Architecture Decision
        ↓
Blueprint
        ↓
Batch Roadmap
        ↓
Current Phase Execution
        ↓
ARCH REVIEW
```

**禁止“先假设一个漂亮理论，再围绕理论自证”。**

所有后续架构改造必须区分：

- FACT：来自代码、数据结构、测试、真实用户流程、运行证据
- HYPOTHESIS：架构推断、候选方案、未来演进判断

HYPOTHESIS 不得伪装成当前系统事实。

---

# 1. 当前权威 Git 状态

当前已审核基线链：

```text
8dc15767f768b40a75bf518ef300412bb02a169b
    Reality Verification ACCEPTED

        ↓

e67f62d87d56c5ebd1c4f575a61115bf6ca8f7d8
    Phase 1 Domain Boundary Foundation ACCEPTED

        ↓

54db71b9d92e4490cf1e79ca9ba95bc320381f9e
    Phase 2 UseCase Migration ACCEPTED

        ↓

93acdcdffed14fd91b99307f9c4b90d347fbc1e4
    Phase 3 Projection Migration ACCEPTED
```

当前 ARCH 认可的最新基线：

```text
93acdcdffed14fd91b99307f9c4b90d347fbc1e4
```

后续工作必须以远程最新树为准。

如果本地状态与远程不一致：
- 不回退已存在的新实现
- 不根据旧 package 中过期的 base 强制 reset
- 先记录真实状态
- 若存在语义/治理冲突，停止并交 ARCH
- 普通执行器环境差异不得被误判为领域 STOP

---

# 2. Meal Architecture 已完成的事实

## 2.1 Reality Verification 已确认

当前真实持久化事实是：

```text
meal_record
    │
    └── meal_record_dish
            │
            └── dish
                  │
                  └── ingredient
```

已确认：
- 当前 Planning、Recording、History 长期共享 `meal_record`
- 当前没有独立 `meal_plan` 持久化表
- “Recipe” 不是独立事实模型，当前大量语义由 `Dish` 承载
- AI 保存最终复用现有 MealRecord 保存链
- Canonical Domain 不应凭空替换真实存储世界

因此旧的“单一 Meal Aggregate 包打天下”设计已经废弃。

---

# 3. 当前冻结的 Meal 架构方向

Meal 领域拆成三个语义边界：

```text
                    AI Capability
                         │
                     Suggestion
                         │
                         ▼
       ┌────────────────────────────────┐
       │                                │
MealPlanning                      MealRecording
       │                                │
    MealPlan                         MealRecord
       │                                │
       └──── User Confirm / Convert ────┘

                  FoodKnowledge
                      │
                 Dish / Ingredient
```

冻结原则：

## 3.1 MealPlanning

负责：
- 未来计划
- 排期
- 尚未发生的饮食安排

核心模型：
- `MealPlan`
- `MealPlanId`

计划不等于实际发生。

---

## 3.2 MealRecording

负责：
- 实际发生的饮食记录
- 编辑后的记录
- 历史事实

核心模型：
- `MealRecord`
- `MealRecordId`

---

## 3.3 FoodKnowledge

负责：
- `Dish`
- `Ingredient`
- 可复用食物知识

当前不要擅自增加独立 Recipe Domain。

如果未来要拆 Recipe，必须先 Reality Verification。

---

## 3.4 AI Boundary

冻结：

```text
AI Suggestion != Domain Truth
```

AI 是 Capability Provider，不是 Domain Owner。

目标链路：

```text
AI Suggestion
      ↓
Validation / User Confirmation
      ↓
UseCase
      ↓
MealPlan / MealRecord
      ↓
Adapter / Repository
      ↓
Storage
```

---

# 4. 已完成 Phase

# Phase 1 — Domain Boundary Foundation

状态：

**ACCEPTED**

已建立：
- MealPlan Domain
- MealRecord Domain
- FoodKnowledge Boundary
- Legacy MealRecord Adapter
- 生命周期边界
- Boundary Test

禁止回退或重新合并为一个万能 Meal。

---

# Phase 2 — UseCase Migration

状态：

**ACCEPTED**

目标结构已经开始建立：

```text
UI
 ↓
UseCase
 ↓
Domain
 ↓
Repository / Adapter
 ↓
Storage
```

关键实现包括：
- `MealRecordUseCase`
- UI create/save/query 逐步经过 UseCase
- 保留旧 `MealRecordRepository` 兼容 API
- 不改数据库 schema

ARCH 已接受“迁移期 Query 仍可返回 legacy edit projection”的策略。

不要在后续阶段为了“纯洁架构”提前删除兼容层。

---

# Phase 3 — Projection Migration

状态：

**ACCEPTED**

已经迁移的主要读取流：
- Home
- Timeline
- Search / Meal History

目标结构：

```text
UI
 ↓
Projection Boundary
 ↓
Repository
 ↓
Domain / Legacy Adapter
 ↓
Storage
```

已经建立只读 Projection Facade。

冻结：
- Projection 只负责展示
- Projection 不能修改 Domain Truth
- UI 应逐步减少直接依赖 storage / legacy record model

仍保留的 legacy read：
- edit/readback
- domain automation
- 部分阶段外消费者

这是有意的兼容策略，不是当前缺陷。

---

# 5. 当前授权执行：Phase 4

# Phase 4 — AI Flow Alignment

状态：

**NEXT / 当前可执行**

当前基线：

```text
93acdcdffed14fd91b99307f9c4b90d347fbc1e4
```

## 5.1 目标

把 AI 与 Domain 的交互收敛到明确边界。

禁止结构：

```text
AI
 ↓
Repository / Storage
```

目标结构：

```text
AI Suggestion
      ↓
User Confirmation / Validation
      ↓
UseCase
      ↓
MealPlan / MealRecord
      ↓
Repository / Adapter
      ↓
Storage
```

## 5.2 执行内容

先调查真实 AI 写入路径，再做最小必要迁移。

重点：
- AI 生成 / preview / confirm / commit 路径
- AI 是否存在绕过 UseCase 的直接写入
- confirmation 是否真实存在
- AI 结果如何映射为 MealPlan 或 MealRecord
- 保留已有生成算法和解析能力

允许：
- AI suggestion mapping
- confirmation boundary
- AI-facing UseCase 调整
- 测试
- Evidence
- DI 必要调整

禁止：
- AI 推荐算法重写
- 模型训练调整
- Prompt 体系大改
- 数据库 schema 修改
- Meal Domain 重新设计
- Recipe/Nutrition 扩展
- Phase 5 Repository Cleanup 提前执行

## 5.3 Phase 4 Acceptance Gate

必须证明：
- AI 不直接拥有 Domain Truth
- 用户确认 / validation 边界真实存在
- 非 AI 流程不被破坏
- 原 AI 算法行为不被无授权改变
- schema 无变化
- Evidence 完整

完成后：
- `CODE_COMPLETE`
- `TURN=REVIEW`
- commit + push
- 返回 commit hash
- 停止等待 ARCH

---

# 6. 后续已规划 Phase

这些 Phase 已纳入 Batch Roadmap，但 **当前不能越权一次性执行**。

---

# Phase 5 — Repository Boundary Cleanup

状态：

**PLANNED / WAIT**

前置：
- Phase 4 ACCEPT

目标：

Repository 最终收敛到：

```text
load
save
query
```

业务规则逐步移动到：
- Domain
- UseCase
- Policy

重点调查：
- Repository 内是否仍存在业务状态迁移
- Repository 是否承担过多 merge / validation / preference side effect
- 哪些行为属于存储兼容逻辑，哪些属于真正 Domain Logic

特别注意：

当前 `saveDayMeals()` 属于真实核心链，且存在：
- 按日替换
- 菜品关联
- eaten_ratio 回填
- preference side effect
- transaction boundary

**不能因为架构整洁而一次性重写。**

正确方式：
1. Reality map Repository responsibilities
2. 分类 Storage / Orchestration / Domain Policy
3. 逐步提取
4. 保持旧 API 兼容
5. 每次迁移有 regression evidence

禁止：
- 一次性 Repository 重写
- 数据库迁移
- 无事实依据的 DDD 纯化

---

# Phase 6 — Legacy Retirement Evaluation

状态：

**PLANNED / WAIT**

这不是“删除 Legacy”的授权。

任务是：

**评估删除条件。**

只有同时满足以下条件才可以提出 Retirement Blueprint：

- 所有关键调用方已迁移
- Home/Timeline/History/Edit/AI/Automation 等已完成边界迁移
- Legacy 与 Domain 的身份映射稳定
- 回归测试完整
- 真机流程无阻塞
- 数据兼容已验证
- 没有历史数据读取依赖被遗漏

未满足条件：

```text
LEGACY MUST REMAIN
```

Phase 6 应先输出：
- remaining caller inventory
- legacy dependency graph
- removal risk matrix
- deletion preconditions
- rollback strategy

然后由 ARCH 决定：
- RETAIN
- PARTIAL RETIREMENT
- FULL RETIREMENT BLUEPRINT

CODER 不得自行删除。

---

# 7. Meal Batch 完成后仍未关闭的项目级待办

Meal Batch 不是整个 CookBook 项目的终点。

下列事项仍需进入 Master Roadmap。

---

# 7.1 用户流程 Bug / 回归

优先级：P0 / P1

历史上需要持续验证：
- 手动添加 / 保存链路
- AI 保存链路
- 返回 / state restore / merge
- 移动日期写入的原子性和失败恢复
- 编辑后整日替换行为
- 保存后 UI 状态是否正确

Reality Verification 已提示：

移动日期流程存在两个 Repository 调用：

```text
保存目标日
      ↓
删除来源日
```

这不是单事务。

如果后续仍出现保存失败/状态恢复异常，应先用运行 Trace / 真实复现确认，而不是假设 Domain 模型有问题。

---

# 7.2 Device Verification Closure

状态：

**PENDING_DEVICE_VERIFICATION**

必须最终闭环：
- 手动新增
- 手动编辑
- 保存
- 返回
- Timeline
- History
- AI 生成 -> 确认 -> 保存
- state restore
- trace chain

自动测试 PASS 不能代替真机验证。

不得把：

```text
STATIC PASS
```

标成：

```text
DEVICE ACCEPTED
```

---

# 7.3 View / Activation 历史规划项

之前已经存在但尚未完整关闭的方向，需要在 Meal Batch 后重新 Reality Reassessment：

- Renderer Architecture
- Generated View Migration
- SESSION Thin View
- View Drift Detection
- AI Entry
- Draft -> Active Gate

这些项不能直接按旧方案执行。

原因：
Meal Architecture 和统一入口等基础已经变化。

正确流程：

```text
Existing Plan
      ↓
Reality Reassessment
      ↓
Retain / Modify / Drop
      ↓
New Blueprint
```

---

# 7.4 Dish / Recipe / Ingredient / Nutrition

当前明确：

- Dish：真实存在
- Ingredient：真实存在
- Recipe：独立 Domain 尚无事实支撑
- Nutrition：存在计算/估算逻辑，但尚未完成领域级重构判断

因此：

不要直接创建：
- Recipe Aggregate
- Nutrition Aggregate

未来需要分别做 Reality Verification：

## Dish / Recipe Reality
回答：
- Dish 是否已经等价承担 Recipe
- 是否真实需要版本化 Recipe
- UI/DB/AI 中 Recipe 语义是否已经出现

## Nutrition Reality
回答：
- 营养事实来源
- Snapshot vs 实时计算
- 历史数据一致性
- Dish nutrition 与 Meal nutrition 的边界

---

# 7.5 Data Architecture

尚未系统完成：

- persistence boundary
- migration governance
- cache semantics
- offline strategy
- sync/conflict

不要在 Meal Phase 4–6 中顺手展开。

应作为独立大阶段 Reality First。

---

# 7.6 AI Capability 第二阶段

Phase 4 只是 AI 与 Meal Domain 的边界对齐。

它不等于 AI 产品能力已经完成。

未来大类包括：
- 推荐能力
- 推荐理由 / Explainability
- 用户反馈闭环
- 偏好学习
- AI Memory
- AI workflow orchestration

所有这些都必须基于现有真实功能和用户目标再规划。

---

# 7.7 Architecture Quality Automation

已有基础：
- architecture quality check
- evidence discipline
- domain boundary tests

后续可扩展：
- forbidden dependency checks
- lifecycle transition checks
- projection mutation checks
- repository boundary checks
- AI direct-write checks
- legacy remaining-callers audit

自动检查应验证已经冻结的真实 Contract。

不要用自动规则反过来创造不存在的架构。

---

# 8. 当前已知技术观察项

以下不是当前 Phase 4 blocker，但需要保留。

## OBS-01

部分 shared test clean recompilation 曾被已有无关符号问题阻塞，例如：

- `cookingHeaviness`
- `isWesternCuisine`
- `parseDecimalInput`
- `toProjection`
- `installCookbookLogSink`

Phase 3 Evidence 中已记录：
- Android assemble PASS
- production/shared main 可编译
- 不将无关旧测试问题误归类为 Phase 3 coder defect

后续应独立建立 Test Hygiene / Test Debt 修复任务，不应混入 Meal Domain 演进。

---

## OBS-02

UseCase 当前仍依赖 `MealRecordRepository`。

这是迁移期允许状态。

目标 Phase：
- Phase 5 Repository Boundary

不是 Phase 4。

---

## OBS-03

部分 edit/readback 仍返回 legacy projection。

这是阶段性兼容。

不要在未验证完整 edit flow 前强行替换。

---

# 9. Master Priority 建议

当前顺序：

```text
1. Phase 4 AI Flow Alignment
        ↓
2. ARCH REVIEW
        ↓
3. Phase 5 Repository Boundary Cleanup
        ↓
4. ARCH REVIEW
        ↓
5. Phase 6 Legacy Retirement Evaluation
        ↓
6. ARCH REVIEW
        ↓
7. 真机验证 / 用户流程 Bug Closure
        ↓
8. CookBook Master Reality Reassessment
        ↓
9. View / Activation 历史方案重评
        ↓
10. Dish/Recipe/Nutrition Reality Verification
        ↓
11. Data Architecture
        ↓
12. AI Capability 第二阶段
        ↓
13. Architecture Quality Automation 强化
```

注意：

如果 Phase 4–6 中发现真实用户流程 blocker，ARCH 可以插入 Repair Gate。

但不能因为出现新想法任意改变主线。

---

# 10. BLUEPRINT_STATE / TURN 规则

每个执行阶段必须：

开始：

```text
TURN=CODE
Holder=CODER
```

完成：

```text
CODE_COMPLETE
TURN=REVIEW
Holder=ARCH
```

必须记录：
- Phase
- 基线
- implementation commit
- evidence path
- test result
- schema diff
- open issues
- next action

任何 coder 不得自行标：

```text
ARCH_ACCEPTED
```

只有 ARCH 可以。

---

# 11. Git / Push 规则

所有阶段完成后必须：
- commit
- push remote

即使 STOP / BLOCKED：
- 也应尽可能提交可审核的 execution report / evidence / stop reason
- 不允许只在聊天中返回一句错误而没有仓库证据

禁止：
- amend 历史已审核 commit
- rewrite 已存在 accepted chain
- 为适配旧 task package 强制回退远程实现

---

# 12. STOP 分类

Hard STOP 只用于真正语义/治理风险：

- remote accepted parent 发生无法解释的变化
- TURN / authorization 不成立
- 无法建立安全执行边界
- Blueprint 与真实代码发生重大冲突
- changed set 无法控制
- 数据/业务语义存在不可逆风险

以下不应自动升级为领域 STOP：
- 脚本适配失败
- 平台路径差异
- Python/PowerShell/Bash 不一致
- 非必要 accelerator 失败
- 宿主 dirty 但可隔离执行

原则：

Authoritative Payload / Architecture Truth
与
Execution Adapter
分层。

---

# 13. 本地 Codex 当前立即执行话术

将以下作为当前 Phase 4 的启动提示：

```text
继续 CookBook ARCH 演进执行。

仓库：
https://github.com/watermoonbreeze/cookbook.git

当前 ARCH 已接受基线：
93acdcdffed14fd91b99307f9c4b90d347fbc1e4

请先完整阅读：
COOKBOOK_LOCAL_CODEX_ARCH_HANDOFF_20260825.md

你的角色：
CODER / IMPLEMENTER

不是 ARCH。

当前唯一授权执行：
COOKBOOK_MEAL_ARCHITECTURE_EVOLUTION
Phase 4 — AI Flow Alignment

不要执行 Phase 5 / Phase 6。
不要重新设计 Meal Domain。
不要重写 AI 算法。
不要修改数据库 schema。

先基于真实代码定位 AI preview / confirmation / commit / storage 链，
然后按本文 Phase 4 Contract 做最小必要边界迁移。

必须保持：
AI Suggestion != Domain Truth
MealPlan != MealRecord
Dish != MealRecord

完成后：
1. 运行相关测试
2. 生成 Evidence
3. 更新 BLUEPRINT_STATE.md
4. git commit
5. push remote

最终状态：
CODE_COMPLETE
TURN=REVIEW

返回：
- commit hash
- changed files
- tests
- evidence summary
- Blueprint/Reality conflict（如有）

完成后停止，等待 ARCH REVIEW。
```

---

# 14. 本地 Codex 后续推进规则

本地 Codex 可以持有这份长期路线，但不能自动连续跑完所有 Phase。

每阶段：

```text
执行
 ↓
push commit
 ↓
ARCH REVIEW
 ↓
ACCEPT
 ↓
下一 Phase 授权
```

原因不是拆任务，而是保留架构 Gate。

规划是批量的。

执行授权是分阶段的。

---

# 15. 当前总控一句话状态

```text
CookBook Meal Architecture 已完成
Reality Verification + Domain Boundary + UseCase Migration + Projection Migration。

当前进入 Phase 4 AI Flow Alignment。

Phase 5 Repository Boundary Cleanup 和 Phase 6 Legacy Retirement Evaluation 已规划，
但必须分别经过 ARCH Gate。

Meal Batch 完成后，再回到项目级用户流程、真机验证、View/Activation、Dish/Recipe/Nutrition、Data、AI 第二阶段和 Quality Automation 总路线。
```

---

# 16. 对本地 Codex 的最终原则

不要追求“架构看起来最漂亮”。

优先级：

```text
真实业务正确
>
现有行为兼容
>
边界可演进
>
代码结构整洁
```

任何架构判断都要先问：

> 这个问题在当前真实 CookBook 代码、数据和用户流程里真的存在吗？

如果没有事实证据：

标记为 HYPOTHESIS。

不要直接实现。
