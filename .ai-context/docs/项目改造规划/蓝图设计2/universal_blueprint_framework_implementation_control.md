# Universal Blueprint Framework 实施总控

> 文档身份：实施控制文档（Implementation Control）
> 状态：`M0/M1/M2 ACCEPT / CLOSED; M3 CORPUS+PROBE ACCEPT; M3 ANALYSIS H4_INSUFFICIENT_EVIDENCE; BAP-01 ACCEPT; FAMILY-B CELL-01 PRE-PAIR SEALED; CELL-02 IN REVIEW`
> 制定日期：2026-08-11
> Current UBF Stage: `M3 CALIBRATION — BLIND FAMILY-B CELL-02 / PAIR ACQUISITION`
> Current Review Result: `673cc9f1a0eb163058edf9fb7f467c429999cebf = ARCH ACCEPT / CELL-01 PRE-PAIR SEALED / CONSUMED BY CELL-02`
> M0 Accepted Review Target: `3489523db6508ba742ee835022d7e2a9a64f2c4f`
> M0→M1 Persistence Accepted Review Target: `eb1bdc846b3f746dde80e8a1fec234f6434b411f`
> M1 Accepted Review Target: `1723a4f9c050d4da47740d04164fa27d73ea9f2b`
> M1→M2 Persistence Accepted Review Target: `2054899ad93d9c2bc1353914c31a1ef3b96c15ac`
> M2 Preview/Start Accepted Review Target: `c72a19b257550de7bb75dc9361b9f939fc220cb9`
> M2 Accepted Review Target: `84cd8508e213e3664ec898cd2b9a783570b28de5`
> M2→M3 Persistence Accepted Review Target: `0cb6d95057485bebb088523a6fd44a7e5ef1c2a4`
> M3 Preview/Start Accepted Review Target: `c07e4d582a485739144a38ed06267473596cadee`
> M3 Corpus Work-01 Accepted Review Target: `1be1afa1185570e67d7d23e965f6f42ea38724df`
> M3 Calibration Analysis Work-01 Review Target: `b87726abc575a0c17cd1b76f663f242edbddc041` — execution fidelity ACCEPT; H4 preserved
> M3 Analysis Lifecycle Repair Accepted Review Target: `bbd8bbbd5c97a9faef62fde50971a586322e625d`
> M3 Evidence Gap Closure Preview/Start Accepted Review Target: `423d7382d56765e17ea9395e2b167454d5e1450f`
> M3 Evidence Gap Closure Work-01 Reviewed Target: `d43c73fe12cfe3abd3a5b5efa7b5492b0487beca` — CODE ACCEPT / SEMANTIC PASS / identity unresolved / Family-A matched reuse burned
> M3 Blind Acquisition Protocol Repair Reviewed Target: `15b3470703b3df0f1f7dcae8a815b3f660463f0c` — CODE fidelity ACCEPT / State identity hygiene repair required
> M3 BAP-01 State Identity Repair Accepted Review Target: `7c4c060dc5f6e86bcd9517da353cc8924e93818c`
> M3 Family-B Cell-01 Pre-Pair Seal Accepted Review Target: `673cc9f1a0eb163058edf9fb7f467c429999cebf`
> CookBook Project Graph: `Phase 3A EXECUTED / REWORK REQUIRED / PAUSED; Phase 3B NOT AUTHORIZED TO START`
> Process Revision: `R26 — blind Family-B Cell-02 capture; pair evidence remains sealed pending remote ARCH review`

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

Lifecycle result: `ACCEPT / CLOSED` at reviewed delivery `1723a4f9c050d4da47740d04164fa27d73ea9f2b`. The M1→M2 persistence was remotely accepted at `2054899ad93d9c2bc1353914c31a1ef3b96c15ac` and its handoff was consumed by the separately accepted M2 entry; this historical transition no longer carries a pending gate.

目标：把现行协议拆成真正的语义对象，而不是直接改文案。

产物：

- 当前条款 inventory；
- 每条条款的 `Identity / Contract / Lifecycle / Snapshot / View` 分类；
- Level、GC、FULL/LITE、coder role、review、promotion 的现状图；
- 与目标架构的 contradiction / gap / preserve matrix；
- 必须保留的 CookBook 专属规则清单。

门禁：每个拟修改点必须标为 `PRESERVE / REDEFINE / MOVE / SPLIT / DEPRECATE-CANDIDATE`，并给出权威和证据。

### M2 — Legacy Asset Mapping

Lifecycle result: `ACCEPT / CLOSED` at reviewed delivery `84cd8508e213e3664ec898cd2b9a783570b28de5`. The M2→M3 handoff persisted by `UBF-M2-END-ACCEPT-01` remains pending its own remote ARCH review and does not start M3.

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

Work-01 frozen result:

- GC-01～GC-48 exactly once; total=48, unique=48, missing=0, duplicate=0;
- identity, provenance, recurrence and exact Legacy grouping preserved;
- every row carries Origin, Current Authority, Decision Category, Task Profile candidate, Closure Effect, Preserved Coder Discretion, Evidence Type, Legacy Level, Universal Mapping, Lifecycle and references;
- 48/48 Universal Mapping values are `UNRESOLVED`; no Universal ladder/count/name is introduced;
- M1 64-record decomposition is not reopened;
- M3 and CookBook Phase 3B remain not started/not authorized.

Accepted result:

- Work-01 remote delivery chain/scope/blobs/Preserve/State/whitespace/lifecycle all PASS;
- 48 total and unique records; missing=0; duplicate=0;
- Legacy counts L1=9, L2=8, L3=9, L4=7, L5=5, L6=4, L7=6;
- `UNRESOLVED=48` is accepted M2 uncertainty, not permission for CODE to infer a ladder;
- M2 is closed only by the architecture acceptance snapshot; M3 remains gated behind handoff acceptance and separate Preview/Start.

### M3 — Empirical Calibration Corpus

**Entry Status:** `PREVIEW/START ACCEPT / REMOTE ARCH REVIEWED / CONSUMED`

**Corpus Status:** `WORK-01 COMPLETE / PENDING REMOTE ARCH REVIEW`

**Entry Accepted Review Target:** `c07e4d582a485739144a38ed06267473596cadee`

**Entry Contract:** `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-PREVIEW-START.md`

**Preview/Start frozen result:**

- M2→M3 handoff acceptance is consumed only by the M3 stage-entry transaction;
- this entry creates empirical sample rows = `0`;
- future Work-01 must use the architecture-frozen corpus record schema, eligibility classes, evidence minimum, ARCH labeling/adjudication, UBEA-v2 defect attribution, compatibility classification, confound controls, coverage recount and acceptance gates;
- `EXECUTION_DEVIATION` is the only defect class that may support a negative coder capability signal, and only with independent evidence plus confound review;
- architecture-authored payload/Blueprint defects, soft compatibility, compatibility exhaustion and external truth change are not coder negative samples;
- linked revisions of one incident require cluster identity to avoid pseudo-replication;
- Legacy L1～L7, GC count/order, risk/novelty, FULL/LITE, mechanism class and Actor Routing remain observed orthogonal variables, never preselected Universal Level answers;
- raw corpus may not decide Universal Level count/name/threshold/envelope/mapping, final Task Profile, final Capability Profile or Level Selector;
- every corpus delivery requires machine recount, independent challenge and remote ARCH acceptance before any later calibration analysis.

**Corpus Work-01 start gate:** only a **separate architecture-authored package after remote ARCH ACCEPT of this Preview/Start delivery** may create empirical rows. CODE has no standing authorization to start corpus work from the presence of this entry document alone.

**Exit:** accepted corpus evidence and subsequent architecture analysis establish whether a monotonic/comparable closure ladder is empirically supportable; failure returns to architecture research instead of inventing levels.

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

当前唯一在途治理事务是 `UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03` 的 adjudicated-corpus delegated execution / remote review cycle：

1. M0/M1/M2 已 `ACCEPT / CLOSED`；M3 Preview/Start、Work-01、Work-02 已 REMOTE ARCH ACCEPT；
2. Work-02 reviewed delivery `318bbc27f4d485fa0f8de6c66b92c7dc14a3c821`，claim=`09e6f7590309ca6b97d70830982fe8baf8321cac`，accepted recount = 6 rows / 6 clusters / 4 eligible / 2 context / 2 positive / 2 negative / 2 neutral；combined Work-01+02 = 15 rows / 11 clusters / 9 eligible / 6 context / 6 positive / 3 negative / 6 neutral；
3. structured Q / correct STOP 仍为历史 coverage gaps；不得复制、重标或猜测旧案例来填空；
4. 因历史 coverage/confound 不足，calibration analysis 仍 `NOT STARTED / NOT AUTHORIZED`；
5. 本 Probe-01 仅授权 6 个 frozen scenario cards + 1 个 schema-bound runtime response；response 是 synthetic controlled evidence，不是预先 ACCEPT 的 corpus row；
6. carrier=`BLUEPRINT-LITE`，mechanism=`RUNTIME_DISCOVERY_REQUIRED`；静态 authoring response 会污染观测，因此只有 fixture/contract/static governance targets 预冻结；
7. CODE 只能选择 fixture 枚举动作并写简洁可审理由，不得自评正确性、eligibility、capability signal、模型排名或 Universal Level/Profile/Selector；
8. independent ARCH review 后才可决定是否进入 Work-03、是否把某些 probe observations 纳入 corpus、是否仍需更多 coverage；
9. user canonical、GC registry、routing、State ownership split、Project Graph、accepted Work-01/02 evidence 和 production assets Preserve；
10. M4、M5、CookBook Phase 3B 继续 `NOT STARTED / NOT AUTHORIZED`。

## 11. M1 Semantic Decomposition Work-01

- M1 Preview/Start final delivery `795d2b9c807fe3954f1ac5f4cda60392c7ff9cc9` has REMOTE ARCH **ACCEPT** and is consumed by this work batch.
- Architecture has directly read current user-level `blueprint_protocol.md`, `GLOBAL.md`, and root shared `MODEL_ROUTING.md`; repository persistence stores hashes + normalized semantics, not wholesale external file content.
- Work-01 persists the UBF-relevant clause inventory, five-kind classification, current-state map, contradiction/gap/preserve matrices, and CookBook overlay boundary.
- Legacy L1-L7 and GC-01..GC-48 remain unmapped to Universal Level; per-GC metadata/mapping is M2 and remains prohibited.
- No user-level canonical mutation, fallback synchronization, State compaction, Project Graph mutation, production change, M2 start, or CookBook Phase 3B start is authorized.
- Normal delivery returns `TURN=REVIEW` and waits for REMOTE ARCH review. If accepted, next is M1 End/Accept + M1→M2 Handoff persistence, not M2 execution.

## 12. Execution Architecture v2 Pointer

- Canonical implementation architecture for this batch: `Universal-Blueprint-Execution-Architecture-v2.md`.
- Execution Truth is the manifest plus exact claim/final/abort artifacts; Python/PowerShell/Bash/native file operations are adapters.
- Deterministic means unique target Git blobs and lifecycle result, not mandatory execution of one script.
- Compatibility failures follow only the package-preauthorized fallback graph; semantic/Truth failures remain Hard STOP.
- Byte identity PASS and architecture semantic truth PASS are separate gates.
- R1/R2/R3 package defects remain architecture-attributed and do not count as Luna capability negatives.

## 13. M3 Empirical Calibration Corpus Work-01

- Fixed Handoff Parent: `c07e4d582a485739144a38ed06267473596cadee` = M3 Preview/Start **ACCEPT**.
- Machine-readable corpus target: `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01.json`.
- Frozen recount: total=9, unique IDs=9, unique clusters=5, eligible=5, context-only=4, excluded=0.
- Outcome coverage: success=4, REWORK=3, incorrect STOP=1, blocked/external=1.
- Primary attribution coverage: NONE=4, ARCH_PAYLOAD_DEFECT=3, EXECUTION_DEVIATION=1, COMPATIBILITY_EXHAUSTED=1.
- Capability signal coverage: POSITIVE=4, NEGATIVE=1, NEUTRAL=4; forbidden negative/non-execution combination=0.
- All raw Universal Calibration Disposition values are UNRESOLVED.
- This is evidence construction, not Universal Level or routing inference.
- Work-01 final state is COMPLETE / PENDING REMOTE ARCH REVIEW and returns TURN=REVIEW.

## 14. M3 Empirical Calibration Corpus Work-02

- Fixed Handoff Parent: `1be1afa1185570e67d7d23e965f6f42ea38724df` = Work-01 **REMOTE ARCH ACCEPT**.
- Machine-readable Work-02 target: `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02.json`.
- Work-02 frozen recount: total=6, unique IDs=6, unique clusters=6, eligible=4, context-only=2, excluded=0.
- Capability signals: positive=2, negative=2, neutral=2; all negatives are `EXECUTION_DEVIATION`.
- Combined Work-01+02: total=15, unique IDs=15, unique clusters=11, eligible=9, context-only=6, positive=6, negative=3, neutral=6.
- Actor/model diversity is broadened but not balanced; GPT-5 rows are context-only and assisted-review confounds remain explicit.
- Structured Q and correct STOP are still missing coverage cells; missing cells are gaps, not permission to invent samples.
- All raw Universal Calibration dispositions remain `UNRESOLVED`.
- Work-02 is evidence construction only. Calibration analysis, Universal Level/Profile/Selector inference, M4/M5 and CookBook Phase 3B remain prohibited.
- Work-02 final state is `COMPLETE / PENDING REMOTE ARCH REVIEW` and returns `TURN=REVIEW`.

## 15. M3 Controlled Calibration Probe-01

- Fixed Handoff Parent: `318bbc27f4d485fa0f8de6c66b92c7dc14a3c821` = Work-02 **REMOTE ARCH ACCEPT**.
- Fixture: `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CONTROLLED-CALIBRATION-PROBE-01-Fixture.json`.
- Runtime evidence: `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CONTROLLED-CALIBRATION-PROBE-01-Response.json`.
- Carrier profile is BLUEPRINT-LITE: task card + exact allowlist + invariant table + test matrix; constraint strength is not reduced.
- Mechanism class is `RUNTIME_DISCOVERY_REQUIRED` because the CODE response is the observation and cannot be pre-authored without contaminating it.
- Six scenarios target underdetermined structured-Q choice, real Hard STOP recognition, optional-helper fallback, denied-path scope discipline, reasonable authorized divergence, and architecture challenge without frozen-Truth mutation.
- Response shape/non-inference is mechanically validated; semantic correctness is deliberately **not** package-validated. Independent ARCH owns adjudication.
- Probe completion does not create Work-03 corpus rows and does not start calibration analysis. Universal Level/Profile/Selector, routing, M4/M5 and CookBook Phase 3B remain prohibited.

## 16. M3 Empirical Calibration Corpus Work-03

- Fixed Handoff Parent: `2326a94e5ee261888be527a2303962219cf422a6` = Probe-01 **REMOTE ARCH ACCEPT**.
- Probe-01 claim: `5cb0744d8f5e748def22b1d00cafb7a9d1da4193`; final exact 7 paths; Response contains 6/6 semantically correct scenario actions after independent remote ARCH adjudication.
- Machine-readable Work-03 target: `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03.json`.
- Work-03 frozen recount: total=6, unique IDs=6, unique clusters=1, eligible=6, context-only=0, excluded=0, positive=6, negative=0, neutral=0.
- Combined Work-01+02+03: total=21, unique IDs=21, unique clusters=12, eligible=15, context-only=6, positive=12, negative=3, neutral=6.
- All six Work-03 rows share `M3-CONTROLLED-PROBE-01-CLUSTER`; later analysis must not treat them as six independent execution batches.
- Controlled structured-Q and correct-STOP cells are now represented, but production equivalents remain coverage gaps.
- Raw Response executor label conflicts with package/ledger identity. Work-03 preserves raw `GPT-5`, normalizes authoritative identity to `GPT-5.6 Luna`, and attributes missing cross-check to ARCH package validator hygiene; no negative capability signal is created.
- All raw Universal Calibration dispositions remain `UNRESOLVED`.
- Work-03 is evidence persistence only. Calibration analysis, model ranking/routing, Universal Level/Profile/Selector inference, M4/M5 and CookBook Phase 3B remain prohibited.
- Work-03 final state is `COMPLETE / PENDING REMOTE ARCH REVIEW` and returns `TURN=REVIEW`.


## M3 Calibration Analysis Preview/Start — UBF-M3-CALIBRATION-ANALYSIS-PREVIEW-START-01

- Architecture input: Work-03 `99dc95ddd682945bfa6936a7ca2391ff211393ec` = **ACCEPT / CONSUMED**.
- Evidence boundary: 21 rows / 12 root clusters / 15 eligible / 6 context / 12 positive / 3 negative / 6 neutral.
- Primary independence unit: Root Incident / Episode Cluster ID; row-count pseudo-replication is forbidden.
- Mandatory stratification/sensitivity: synthetic vs production, actor imbalance, assistance/reviewer confound, task-family/decision-axis crossover, coverage gaps and legacy-label contamination.
- Candidate outcomes: one-dimensional supported; one-dimensional core + secondary axes; multidimensional only; insufficient evidence.
- This Preview/Start creates **zero** Universal Level/Profile/Selector/model-ranking/routing decisions.
- Analysis Work-01 is **NOT STARTED / NOT AUTHORIZED** until remote ARCH accepts this entry.
- M4/M5 remain NOT STARTED; CookBook Phase 3B remains NOT AUTHORIZED.


## M3 Calibration Analysis Work-01 — UBF-M3-CALIBRATION-ANALYSIS-WORK-01

- Parent Preview/Start `5d6eda046be0b2a09f52059e438cb51f7db38e40` = **ACCEPT / CONSUMED**.
- Accepted inferential unit = root cluster; 15 eligible rows collapse to 9 eligible clusters.
- Cluster outcome = 6 positive-only / 2 negative-only / 1 mixed; synthetic controlled evidence = 1 cluster.
- Actor cluster coverage = GPT-5.6 Luna 5 / DeepSeek V4 Flash 3 / DeepSeek V4 Pro 1, with material task/assistance mismatch.
- Mandatory falsification result: pseudo-replication, synthetic-dependence, actor imbalance, assistance sensitivity and coverage gaps are material; crossover is present; negative-attribution purity and legacy-contamination guards pass.
- Hypothesis disposition: H1 NOT ESTABLISHED; H2/H3 PLAUSIBLE BUT NOT DISTINGUISHABLE; **H4 INSUFFICIENT EVIDENCE selected**.
- `H4` is not a Level design decision. Universal Level/Profile/Selector/model ranking/routing decision counters remain 0.
- Any subsequent M3 task requires a new remote ARCH decision after Work-01 review. M4/M5 and CookBook Phase 3B remain prohibited.


## M3 Calibration Analysis Work-01 — Remote ARCH review and lifecycle-view repair

- Reviewed delivery `b87726abc575a0c17cd1b76f663f242edbddc041` executed the architecture-authored Work-01 payload faithfully: State-only claim, exact 8-path final, H4 analysis persistence and return to REVIEW all match the frozen task.
- Remote ARCH therefore records **CODE EXECUTION FIDELITY = ACCEPT**.
- Independent review found an architecture payload defect in Generated View lifecycle propagation: the implementation-control header and M3 Preview header remained on older corpus/analysis-entry status after Analysis Work-01 was persisted.
- Classification: `ARCH_PAYLOAD_DEFECT / NON_CAPABILITY`. It must not become a negative coder/model sample.
- This repair changes only lifecycle/current-status views and acceptance bookkeeping. It does **not** alter the Work-01 analysis JSON, H1/H2/H3/H4 adjudication, corpus labels, root clusters, falsification verdicts, or zero-decision gates.
- After this repair is remotely accepted, M3 may proceed only to an architecture-authored evidence-gap-closure Preview/Start. M4/M5 and CookBook Phase 3B remain prohibited.


## M3 Calibration Evidence Gap Closure Preview/Start — UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-PREVIEW-START-01

- Parent lifecycle repair `bbd8bbbd5c97a9faef62fde50971a586322e625d` = **ARCH ACCEPT / CONSUMED**.
- Analysis Work-01 remains **H4_INSUFFICIENT_EVIDENCE**; no Level/Profile/Selector/routing decision is created.
- Preview/Start creates 0 empirical rows and starts 0 evidence-acquisition runs.
- Gap closure uses two lanes: matched controlled evidence for actor/task/assistance confounds, and naturalistic production capture for production `STRUCTURED_Q` / correct `HARD_STOP`.
- Re-analysis remains unauthorized until the full re-entry gate in `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-PREVIEW-START-01.md` is independently ARCH-verified.
- Execution packages bind the abstract actor `CODER`; concrete model identity is runtime provenance only and model substitution does not require semantic repackaging.
- Evidence Gap Closure Work-01 is **NOT STARTED / NOT AUTHORIZED** until this entry receives remote ARCH ACCEPT.
- M4/M5 and CookBook Phase 3B remain prohibited.


## M3 Calibration Evidence Gap Closure Work-01 — Family-A Cell-01

- Evidence Gap Closure Preview/Start `423d7382d56765e17ea9395e2b167454d5e1450f` = **ARCH ACCEPT / CONSUMED**.
- Work-01 acquires exactly one raw Matched Controlled observation against immutable `UBF-M3-EGC-MC-FAMILY-A/R1` with SHA-256 `af32e947d7c21b4dce0ad9e012b0de28d865a0183f4aa53d13bc006ce45bf33b`.
- The Family Truth Capsule contains no answer key. CODE selects actions and rationale; semantic correctness is adjudicated only after remote review.
- This execution is **one candidate root cluster**, regardless of its four scenario responses.
- Raw Response and Runtime-Provenance are evidence only. `correctness / capability signal / corpus eligibility / normalized concrete actor` remain `PENDING_REMOTE_ARCH_REVIEW`.
- Work-01 creates **0 empirical corpus rows**. Existing analysis and Work-01/02/03 corpus JSONs remain unchanged.
- A later Family-A Cell-02 must reuse the same Family Truth Capsule identity/digest but be executed by a different ARCH-normalized concrete coder identity before it can form a matched pair.
- Family-A Cell-02, all Family-B cells, naturalistic production capture, re-analysis, M4/M5 and CookBook Phase 3B are **NOT AUTHORIZED** by this Work-01 execution.
- H4 remains `H4_INSUFFICIENT_EVIDENCE`.


## M3 Evidence Gap Closure — Blind Acquisition Protocol Repair

- Work-01 `d43c73fe12cfe3abd3a5b5efa7b5492b0487beca`: CODE fidelity **ACCEPT**, semantic actions **4/4 PASS**, non-negative.
- Work-01 cannot satisfy matched inference because concrete actor identity is unresolved and the raw Response is already visible in canonical history.
- Family-A is therefore **BURNED_BY_CANONICAL_RESPONSE_EXPOSURE** for future matched reuse. This is an acquisition-design `BLUEPRINT_DEFECT / NON_CAPABILITY`, not coder failure.
- BAP-01 requires future matched families to commit only cryptographic commitments while holding raw Response + Runtime-Provenance outside the repository until the same-family pair is complete.
- Qualifying matched matrix restarts with Family-B and Family-C; each requires two independently normalized concrete CODER identities under identical family truth.
- Concrete model names remain runtime evidence only; package authority remains abstract `CODER`.
- This repair creates 0 empirical corpus rows and runs 0 new acquisitions.
- H4 remains `H4_INSUFFICIENT_EVIDENCE`.
- Family-B Cell-01 is **NOT STARTED / NOT AUTHORIZED** until this repair receives remote ARCH ACCEPT.
- Re-analysis, M4/M5 and CookBook Phase 3B remain unauthorized.


## M3 BAP-01 State Model-Identity Hygiene Repair

- Reviewed BAP-01 delivery `15b3470703b3df0f1f7dcae8a815b3f660463f0c`: **CODE EXECUTION FIDELITY ACCEPT**.
- BAP-01 blind commitment/reveal protocol semantics remain accepted as the intended repair Truth.
- One architecture-authored State historical row repeated a concrete runtime/model label, violating the State abstract-role contract.
- Classification: `ARCH_PAYLOAD_DEFECT / NON_CAPABILITY`; no coder/model negative sample.
- Repair scope is strictly State identity hygiene + lifecycle/ledger truth.
- `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-BLIND-ACQUISITION-PROTOCOL.md`, Work-01 evidence, accepted analysis and corpus assets are Preserve.
- Family-A qualifying=0; Family-B=0/2; Family-C=0/2.
- new evidence runs=0; new empirical rows=0; H4=`H4_INSUFFICIENT_EVIDENCE`.
- Blind Family-B Cell-01 remains **NOT STARTED / NOT AUTHORIZED** until this repair receives remote ARCH ACCEPT.
- M4/M5 and CookBook Phase 3B remain prohibited.


## Blind Family-B Cell-01
- Parent `7c4c060dc5f6e86bcd9517da353cc8924e93818c` ARCH ACCEPT.
- Family SHA `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`.
- Canonical commitment only; raw response/model/nonce outside repo.
- rows=0; matched credit=0 before reveal; Cell-02 **NOT STARTED / NOT AUTHORIZED**; H4=`H4_INSUFFICIENT_EVIDENCE`.

## M3 Family-B Cell-01 Pre-Pair Seal

- Cell-01 final `bd96410bd20e3a41848ca61a98eb41875e7c8829` transaction fidelity = **ACCEPT**.
- R2 claim `e00cabe703aec65efbc60b18679e4b69fd6b2b56` is State-only; final canonical scope is 9 files.
- BAP-01 Commitment/Reveal hashes were independently recomputed by ARCH and match `4dc7307c6c3fc3529a4f77400d183cb84f3e7a2f39e3aadf89a2c4a6cf170227` / `8ac02e8747bb457ffbb344c11b99e5b75f9050751b6fdc8385dd4056337aa15f`.
- Cell-01 semantic adjudication, normalized concrete actor and capability result are intentionally **SEALED UNTIL FAMILY-B PAIR COMPLETE**.
- This seal does not persist raw Reveal, raw response, concrete actor or semantic result.
- The ledger formatting defect that concatenated the Cell-01 row to the prior row is `ARCH_PAYLOAD_DEFECT / NON_CAPABILITY` and is repaired here.
- Canonical matched credit remains deferred; this avoids exposing peer outcome before Cell-02.
- After this seal receives remote ARCH ACCEPT, Family-B Cell-02 becomes the only authorized matched-controlled next acquisition. Its operator-selected concrete coder must differ from the ARCH-sealed Cell-01 actor.
- Cell-01 Reveal must never be given to Cell-02 CODER.
- H4 remains `H4_INSUFFICIENT_EVIDENCE`; no new evidence run or empirical row is created.
- Family-C/naturalistic/re-analysis/M4/M5/Phase3B remain unauthorized.

## Blind Family-B Cell-02

- Cell-01 Pre-Pair Seal `673cc9f1a0eb163058edf9fb7f467c429999cebf` = **ARCH ACCEPT / CONSUMED**.
- Cell-02 reuses the exact byte-identical `UBF-M3-EGC-MC-FAMILY-B/R1` Truth with SHA-256 `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`.
- Package authority remains abstract `CODER`; concrete actor provenance is held only in the repo-external Reveal and normalized privately by ARCH.
- The operator must select a concrete coder different from the sealed peer actor. The package does not expose or name that peer actor.
- Canonical repository receives only the Cell-02 Commitment. Raw actions, rationales, nonce, concrete actor provenance and both-cell adjudication remain outside repository history until pair review completes.
- Cell-02 completion means only **PAIR ACQUISITION COMPLETE / PENDING ARCH REVEAL AND PAIR ADJUDICATION**. It does not itself establish actor distinctness, semantic correctness, capability signal, corpus eligibility or matched credit.
- This cell creates 0 empirical corpus rows and preserves `H4_INSUFFICIENT_EVIDENCE`.
- After final push, only remote ARCH may verify the reveal, normalize Cell-02 actor, check distinctness/blindness, adjudicate both cells and issue a later pair-canonicalization task.
- Family-C, naturalistic production capture, re-analysis, M4/M5 and CookBook Phase 3B remain unauthorized.
