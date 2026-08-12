# Universal Blueprint Framework 实施总控

> 文档身份：实施控制文档（Implementation Control）
> 状态：`M0 ACCEPT / CLOSED; M0→M1 HANDOFF PERSISTED / PENDING REMOTE ARCH REVIEW`
> 制定日期：2026-08-11
> Current UBF Stage: `M0 — ACCEPT / CLOSED`
> Current Review Result: `3489523db6508ba742ee835022d7e2a9a64f2c4f = ACCEPT`
> M0 Accepted Review Target: `3489523db6508ba742ee835022d7e2a9a64f2c4f`
> CookBook Project Graph: `Phase 3A EXECUTED / REWORK REQUIRED / PAUSED; Phase 3B NOT AUTHORIZED TO START`
> Process Revision: `R6 — M0 End/Accept & M0→M1 Handoff Persistence`

## 1. 目标

在不重写历史、不建立第二套永久颗粒度系统、不提前启动 CookBook Phase 3 的前提下，把现有 CookBook 蓝图治理经验迁移为一个跨项目可复用的 Universal Blueprint Framework。

唯一第一性目标：

> Blueprint 应按目标 coder 的能力与任务容错边界，闭合不能安全下放的决策，同时保留 coder 能可靠承担并可用于改善架构的决策空间。

## 2. 本轮实施边界

### 2.1 本轮允许

- 审计现行 `blueprint_protocol`、Global Core 候选规则与 CookBook Project Overlay；
- 分解现有 L1～L7 主题和 GC-01～GC-48 的真实语义角色；
- 建立 Legacy → Universal 的非破坏性映射；
- 设计并校准 Universal Level、Task Profile、Capability Profile、Level Selector；
- 设计 FULL/LITE package profile、GC 权威与晋升/合并/废弃机制；
- 形成 canonical protocol 的精确 mutation blueprint；
- 经独立 challenge 后，由 Luna 按蓝图落盘并提交证据。

### 2.2 本轮禁止

- 启动或实现 CookBook Phase 3 的生产代码；
- 处理 `PHASE2E_VIEW_DRIFT.md` 中的 9 项 legacy view drift；
- 立即宣布 CookBook Legacy L7 等于某个 Universal Level；
- 立即决定 Universal Level 最终数量或预设 L8/L10；
- 重编号、删除或改写 GC-01～GC-48 的历史身份和既有审计证据；
- 让项目自行重定义 Universal Level 语义；
- 用 GC 数量、覆盖率、风险分数或文档长度定义 Level；
- 在未经校准和 challenge 的情况下直接替换 canonical protocol；
- 让 Luna依据讨论摘要自行设计架构或扩大范围。

## 3. 必须保持的语义边界

所有迁移工件都必须区分：

1. `Stable Identity`；
2. `Contract / Semantic`；
3. `Lifecycle State`；
4. `Acceptance Snapshot`；
5. `Generated View`。

对应规则：

- canonical protocol 的身份可以保持稳定，但其合同内容可通过受控版本变更演进；
- CookBook Legacy L7 与 GC-01～GC-48 的历史身份和历史验收证据保持稳定；
- `Active / Candidate / Merged / Superseded / Automated` 等属于可变 lifecycle state；
- 某次校准或验收看到的数量、映射和状态属于 acceptance snapshot；
- 汇总表、导航页、handoff 属于 generated view，不能成为独立 Truth。

## 4. 目标对象边界

| 对象 | 只回答的问题 | 不得承担的职责 |
|---|---|---|
| Universal Level | coder 还可决定多少，Blueprint 已闭合多少委托决策 | 不表示风险、文档长度、GC 数量或项目成熟度 |
| Task Profile | 任务涉及哪些决策域与上下文 | 不生成第二套数字颗粒度 |
| Risk / Novelty | 哪些决策不能容忍独立试错 | 不定义 Level 语义 |
| Coder Capability Profile | coder 在给定条件下能可靠承担哪些剩余决策 | 不把 coder 永久标成单一 Lk |
| Level Selector | 为本次委托选择最低安全 Level | 不创造独立 Granularity Score |
| FULL / LITE | 用什么 package 表达和证明所需 closure | 不降低或重新定义 closure |
| GC | 验证 closure 与适用规则是否真正落入 Blueprint | 不以覆盖率定义 Level |
| Project Overlay | 保存项目 Truth、约束、验证和历史模式 | 不重定义 Universal Level |
| Challenge Mode | 在 freeze 前挑战候选架构、freeze 后结构化反馈 | 不是新的 Level 或 capability 总分 |

## 5. 实施阶段

### M0 — Migration Control & Truth Lock

目标：建立独立迁移工作流并锁定可审计输入。

产物：

- 本实施总控；
- 仓库 Truth Pack；
- `Design Baseline / Execution Parent / Review Target` 记录；
- canonical file inventory 与 SHA-256；
- Phase 3 非启动声明；
- 现行权威层级与文件角色表。

门禁：Truth Pack 完整、commit 与工作区状态明确之前，不进入语义重构。

### M1 — Current-State Semantic Decomposition

目标：把现行协议拆成真正的语义对象，而不是直接改文案。

产物：

- 当前条款 inventory；
- 每条条款的 `Identity / Contract / Lifecycle / Snapshot / View` 分类；
- Level、GC、FULL/LITE、coder role、review、promotion 的现状图；
- 与目标架构的 contradiction / gap / preserve matrix；
- 必须保留的 CookBook 专属规则清单。

门禁：每个拟修改点必须标为 `PRESERVE / REDEFINE / MOVE / SPLIT / DEPRECATE-CANDIDATE`，并给出权威和证据。

### M2 — Legacy Asset Mapping

目标：非破坏性映射 CookBook Legacy L7 与 GC-01～GC-48。

每条 GC 至少增加候选元数据：

- Origin；
- Current Authority；
- Decision Category；
- Applicable Task Profile；
- Closure Effect；
- Preserved Coder Discretion；
- Evidence Type；
- Legacy Level；
- Universal Level Mapping（初始允许 `UNRESOLVED`）；
- Lifecycle Status。

门禁：不得仅凭旧 Level 或 checklist 位置推断 Universal Level。

### M3 — Empirical Calibration Corpus

目标：用真实 Blueprint 与执行结果确定“一维 Universal Level”是否可形成单调、可比较的 closure ladder。

样本至少覆盖：

- 不同 task family；
- 不同风险与复杂度；
- 成功、REWORK、Q/STOP、越界和 Blueprint defect；
- Luna 实际补推理的位置；
- coder challenge 改善架构的案例；
- FULL 与 LITE 的不同表达载体。

每个样本必须标注：

- Blueprint 已闭合的决策；
- coder 剩余的决策；
- 合格 coder 可能产生的合理分歧；
- 哪些分歧会造成不可接受结果；
- 最终执行是否需要隐藏帮助；
- evidence 是否具有真实因果性；
- defect attribution。

门禁：只有当候选 closure envelopes 满足可检验的嵌套关系时，才允许冻结线性 Level；否则必须回到架构研究，不得用历史主题顺序伪造单调性。

### M4 — Universal Contracts

目标：在校准证据基础上形成候选 Global Core。

产物：

- Blueprint 第一性定义；
- Universal Level closure contracts；
- 各 Level 的 coder discretion allow/deny contract；
- 通用 Q/STOP 与 structured feedback；
- Task Profile schema；
- Capability Profile schema；
- Minimum Safe Level Selector；
- FULL/LITE package rules；
- GC validation 与 promotion lifecycle；
- Delegation Preservation 与 over-design gate。

门禁：任何 Level 条款必须指出“关闭了什么真实决策、保留了什么自由、如何验证”，不能只列工件或 GC。

### M5 — CookBook Overlay Migration Design

目标：把项目事实与 Universal Core 分离，同时保持历史连续性。

产物：

- CookBook Project Overlay；
- Legacy L7 双标签迁移状态；
- GC-01～GC-48 authority mapping；
- project closure floor 候选；
- 项目 validation commands 与 defect patterns；
- 旧入口、registry 与 generated views 的 pointer migration。

门禁：Overlay 可以规定最低安全 floor，但不得修改 Universal Level 的含义。

### M6 — Canonical Mutation Blueprint & Challenge

目标：形成可机械落盘的精确变更包。

完整执行包必须包含：

- Execution Parent；
- 文件 allowlist / denylist；
- 每文件 create/replace/edit 动作；
- 精确目标内容或可判定 patch contract；
- Preserve Set / Reopen Set；
- registry 与 pointer 更新；
- validation commands；
- expected diff；
- STOP/Q 条件；
- 回报模板；
- rollback / repair boundary。

Freeze 前必须由独立视角完成 canonical-requirement challenge；挑战结果逐项裁决后才可标 `BLUEPRINT_READY`。

### M7 — Luna Mechanical Execution

目标：Luna 只执行已冻结变更包。

Luna 可以：

- 按 allowlist 写入完整目标文档；
- 执行规定的脚本、lint、grep、测试与 diff 检查；
- 遇到矛盾时提交 `Q / CONTRADICTION / IMPLEMENTABILITY / OPTIMIZATION`；
- 提交 commit 并返回结构化证据。

Luna 不可以：

- 自行重新解释 Universal Level；
- 新增未授权文件、registry、评分轴或 fallback；
- 为了“顺便统一”扩大当前批次；
- 静默修正 Blueprint；
- 启动 CookBook Phase 3 代码实现。

### M8 — Review, Acceptance & Handoff

目标：对 Review Target 独立重跑并形成阶段交接。

结果只能是：

- `ACCEPT`；
- `REWORK`（必须有最小 Reopen Set / Preserve Set / Exact Repair）；
- `BLOCKED`（缺失外部 Truth 或权限）。

每阶段必须先完成 End/Accept 状态落库和 handoff，再进入下一阶段 Preview/Start。

## 6. 角色与交付方式

### 6.1 架构模型（本会话）

负责：

- 读取 Truth Source；
- 完成架构判断、语义拆解与校准；
- 产出完整目标文档；
- 产出可直接交给 Luna 的自包含执行包；
- challenge、验收、缺陷归因和最小 REWORK；
- 决定是否 ACCEPT，不把架构裁决下放给 Luna。

### 6.2 Luna

负责：

- 在全新上下文中读取单批执行包；
- 核对 Execution Parent 与工作区；
- 机械写入指定文件；
- 执行指定验证；
- 返回 commit hash、diff summary、测试输出、哈希与 Q/STOP；
- 不承担未显式授权的体系设计。

### 6.3 用户

负责：

- 授权每个阶段启动；
- 在本会话与 Luna 之间传递执行包和结果；
- 提供本会话无法直接读取的 repository Truth；
- 不需要手工拼接或改写我产出的正式文档。

## 7. 推荐的 Luna 循环

每一批都使用同一个闭环：

1. 本会话产出完整执行包；
2. 用户让 Luna `/clear`；
3. 用户原样粘贴执行包，不追加架构解释；
4. Luna 先核对 parent、status、allowlist；
5. Luna 写入并验证；
6. Luna 返回 commit hash 与规定证据；
7. 用户把完整回报带回本会话；
8. 本会话独立复核并给出 ACCEPT / 最小 REWORK；
9. ACCEPT 落库及 handoff 完成后，才开始下一批。

`/clear` 不会破坏连续性，因为连续性由 canonical Truth、commit、执行包和结构化 evidence 承担，而不是依赖 Luna 的聊天记忆。

### 7.1 R4 — Remote Review Visibility & Issue Disposition Contract

1. Review role and repository write capability are separate dimensions.
2. `WRITE_CAPABLE_ARCH`: architecture pushes `REVIEW → CODE` before task release.
3. `REMOTE_READ_ONLY_ARCH`: the blueprint grants a one-task delegated claim; Coder first pushes a claim commit containing only `BLUEPRINT_STATE.md`.
4. Remote visibility is the exit gate; every safely publishable `COMPLETE`, `PARTIAL`, `Q`, validation failure or implementation blocker is written to the fixed report and pushed.
5. Outcomes are `COMPLETE / PENDING REMOTE ARCH REVIEW`, `PARTIAL / PENDING REMOTE ARCH REVIEW`, or `BLOCKED_FOR_REVIEW / PENDING REMOTE ARCH REVIEW`.
6. Every remote-read-only blueprint defines normal and fallback allowlists, PARTIAL rules, commit messages, Return TURN and `NON_PUBLISHABLE_STOP`.
7. Each issue records stable ID, classification, expected/actual, path/line, redacted evidence, `NONE — AWAITING ARCH DISPOSITION`, and delivery impact.
8. Coder does not decide whether an unanticipated issue should be repaired.
9. The next architecture task gives each Issue ID exactly one `ACCEPT_AS_IS / REPAIR / DEFER / REJECT` disposition and exact boundary.
10. Only credential/sensitive ancestry, unisolatable scope, parent/remote/non-fast-forward mismatch, network/permission failure, or unprovable Git contents can use `NON_PUBLISHABLE_STOP`.
11. No outcome authorizes amend, reset, rebase, force push, history rewrite or allowlist expansion.
12. After a safely pushed outcome, Luna returns only the full final commit hash; the user forwards only that hash.
13. Future task documents and reports identify the repository only as `cookbook` or `the current cookbook repository`.
14. No task document hard-codes a hosting provider, account owner, organization or namespace.
15. Coder uses the current worktree's configured `origin`; the task does not change it to match an architecture mirror.
16. Preflight validates only that `origin` exists and its final path component, ignoring case and optional `.git`, is `cookbook`.
17. The full origin URL is local-validation-only and is absent from task documents, reports and chat output.
18. Reports use `Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED`.
19. `origin/master` is a permitted Git ref, not a hosting/ownership identity claim.

This R4 subsection supersedes conflicting failure-report and local-only STOP wording in older §§6–7 for all future REMOTE_READ_ONLY_ARCH writable batches.

### 7.2 Model Execution Capability Evidence Contract

1. Every writable CODE blueprint declares the actual execution model and includes `.ai-context/docs/experience/14_模型执行力评估.md` in its normal delivery allowlist.
2. The current Coder appends exactly one task row with Task ID, CODE role, actual model, task family/complexity, package profile, blueprint granularity, outcome, rework/STOP facts and validation summary.
3. Concrete model names belong only in the capability ledger and execution report; `BLUEPRINT_STATE.md` keeps abstract role+machine identifiers.
4. Because the final commit does not exist when its row is written, the Coder records `待 ARCH 依据最终远程 commit hash 回填` and must not amend or add a follow-up commit merely to self-fill it.
5. Remote architecture review verifies the final hash and produces the ARCH assessment; the next writable architecture task explicitly authorizes backfilling the previous row's commit and ARCH comment.
6. Historical rows are append-preserved; a current task may edit a prior row only with an explicit architecture backfill instruction naming that row and verified commit.
7. A single batch is evidence, not a model-routing conclusion; the ledger's minimum-sample rule remains authoritative.

## 8. 第一批所需 Truth Pack

在进入 M1 前，需要从基线仓库提供只读材料。优先提供以下文件及命令结果：

### 8.1 文件

- 用户级或项目级 `GLOBAL.md`；
- 当前 canonical `blueprint_protocol.md`；
- `MODEL_ROUTING.md`；
- 项目 `PROJECT.md`；
- `BLUEPRINT_STATE.md`；
- `project.yaml`；
- GC-01～GC-48 所在 registry / experience 文件；
- Phase 3 control / handoff 文档；
- `PHASE2E_VIEW_DRIFT.md`；
- 当前治理文档入口与 registry/index。

不存在的文件必须明确回报 `ABSENT`，不能用猜测代替。

### 8.2 命令结果

```text
git rev-parse HEAD
git status --short
git log -5 --oneline
git diff --stat
git diff --name-only
```

另需治理目录的文件清单；目录位置若不确定，先只做 discovery，不创建新目录。

## 9. 第一批 Luna 指令（只读采集，不修改）

以下内容可直接作为 `UBF-M0` 指令交给 Luna：

```text
任务：UBF-M0 Truth Pack 只读采集

目的：为 Universal Blueprint Framework 迁移建立可审计输入。本批只读，不修改文件、不格式化、不提交、不启动 CookBook Phase 3。

已知基线候选：598daf4e5083d62038adfe39b1635993a7d90fa4
Phase 3 状态：AUTHORIZED / NOT STARTED

步骤：
1. 运行并原样回报：
   - git rev-parse HEAD
   - git status --short
   - git log -5 --oneline
   - git diff --stat
   - git diff --name-only
2. 只读定位并回报下列文件的实际路径；不存在则标 ABSENT：
   - GLOBAL.md
   - blueprint_protocol.md
   - MODEL_ROUTING.md
   - PROJECT.md
   - BLUEPRINT_STATE.md
   - project.yaml
   - GC-01～GC-48 registry / experience files
   - Phase 3 control / handoff
   - PHASE2E_VIEW_DRIFT.md
   - governance index / registry entries
3. 输出相关治理目录的文件清单。不要创建新的 inventory 文件。
4. 对已定位文件回报 SHA-256、行数和完整内容；若单次输出受限，按文件分段，保持原文，不做摘要。
5. 回报以下审计字段：
   - Design Baseline candidate
   - Current HEAD
   - Working tree clean/dirty
   - Interposed commits, if any
   - File path
   - Truth role（仅引用文件自述；无法确定则 UNKNOWN）
   - SHA-256

禁止：
- 禁止写文件或提交；
- 禁止修复发现的问题；
- 禁止把 Generated View 自动判为 Truth；
- 禁止推断 CookBook L7 的 Universal Level；
- 禁止处理 9 项 legacy view drift；
- 禁止启动 Phase 3。

STOP：
- HEAD 与已知基线不一致时不要 checkout/reset，只报告差异；
- 工作区非 clean 时不要清理，只报告文件；
- 文件权威冲突时不要裁决，只报告候选与自述角色。
```

## 10. 当前启动判定

- UBF M0 已由远程架构对 R5 reviewed delivery `3489523db6508ba742ee835022d7e2a9a64f2c4f` 独立复核并判定 `ACCEPT / CLOSED`。
- `UBF-M0-END-ACCEPT-01` 只负责持久化该既有架构裁决、回填 R5 模型台账并创建 M0→M1 handoff；不重新打开 R2/R3/R4/R5 已验收内容。
- M0→M1 handoff 在本批正常交付后状态为 `PERSISTED / PENDING REMOTE ARCH REVIEW`；本批不得自我 ACCEPT。
- 只有本批 persistence delivery 获得远程架构 `ACCEPT` 后，才允许另发独立的 M1 Preview/Start 蓝图。
- 当前 M1 为 `NOT STARTED / NOT YET AUTHORIZED`；不得在本批执行 M1 Current-State Semantic Decomposition。
- CookBook Phase 3B 继续 `NOT AUTHORIZED TO START`；production code、tests、build/configuration 与 Project Graph mutation 均不在本批范围内。
