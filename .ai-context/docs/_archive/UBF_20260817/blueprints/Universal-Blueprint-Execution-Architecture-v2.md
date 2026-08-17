# Universal Blueprint Execution Architecture v2

Architecture ID: `UBEA-v2`

Date: `2026-08-12`

Status: `ARCHITECTURE FROZEN / R4 SELF-APPLICATION ACCEPTED`

Scope: Universal Blueprint 的执行架构；不重做 UBF M1 Work-01 semantic decomposition；不启动 M2；不启动 CookBook Phase 3B。

## 0. 决策摘要

Blueprint execution 的确定性定义为：**给定已验证的 Truth、固定 parent 与授权边界，最终 repository tree、commit chain、TURN 与 evidence outcome 唯一可判定。**

`Deterministic` 不再等价于“必须运行某个 Python 脚本”。执行架构固定分为：

1. `Authoritative Payload Layer`：定义必须得到的结果，是 Truth。
2. `Execution Adapter Layer`：定义如何在当前环境落盘，是可替换机制。
3. `Adapter-independent Evidence Contract`：证明结果与 Truth 相同，不依赖某个 adapter 的自述。

治理、文档、配置批次默认优先 `STATIC_TARGET_BUNDLE`。脚本默认是 `OPTIONAL_ACCELERATOR` 或 `OPTIONAL_VERIFIER`。只有任务本质上需要计算型 transform 时，执行程序才可成为必要机制。

## 1. 语义类别与 Mutation Declaration

| 类别 | v2 本批状态 | 说明 |
|---|---|---|
| Stable Identity | MUTABLE | 新增 `UBEA-v2`、Execution Mechanism Class、package/evidence schema identity。 |
| Contract / Semantic | MUTABLE → FROZEN | 冻结 payload/adapter/evidence 分层、fallback、STOP taxonomy、remote transaction 与 acceptance gate。 |
| Lifecycle State | N/A | 本文件不改变任何 CookBook/UBF 阶段状态。 |
| Acceptance Snapshot | MUTABLE | 记录 2026-08-12 R4 Self-Application 结果；它不是永久 Truth。 |
| Generated View | MUTABLE | placement matrix、检查表与摘要可由冻结合同再生成。 |

## 2. Truth Hierarchy

优先级从高到低：

1. Canonical semantic Truth / architecture adjudication。
2. Parent-bound `Authoritative Payload` manifest 与其 exact target artifacts/patch/transform specification。
3. Fixed repository parent tree 与明确标识的 external canonical raw bytes。
4. Verified resulting Git tree / blobs / commit chain。
5. Adapter-independent evidence record。
6. Adapter log、脚本输出、execution report 自述。
7. Fixture、示例、fake repository、聊天摘要。

约束：

- Adapter、script、report、fixture 均不得升级为 semantic Truth。
- `deterministic payload byte identity PASS` 只证明落盘忠实度，不证明 architecture semantic truth 正确。
- Repository preimage 必须绑定 `Handoff Parent` 的 Git tree；fake fixture 重建出的 SHA 只能标 `FIXTURE_ONLY`。
- External canonical file 的 RAW SHA-256 必须标明路径身份、采集时点与 direct/attested access boundary；不得伪装为 repository tree identity。

## 3. Authoritative Payload Contract

每个 writable execution package 必须含一个 canonical manifest；manifest 与其引用的文件共同构成 Authoritative Payload。

### 3.1 必填 manifest 字段

| 字段 | 合同 |
|---|---|
| `schema_version` | 固定为可审计版本；本版建议 `ubea/v2`。 |
| `package_id` / `revision` | 单批、单 revision 稳定身份。 |
| `repository_identity` | 逻辑身份，如 `cookbook`；不得硬编码 provider/owner 作为 Truth。 |
| `target_ref` | 要验证和推送的配置 ref，如 `origin/master`。 |
| `handoff_parent` | 40 位固定 commit；claim 前 remote target 必须精确相等。 |
| `review_mode` | `WRITE_CAPABLE_ARCH` 或 `REMOTE_READ_ONLY_ARCH`。 |
| `mechanism_class` | 四种 class 之一。 |
| `turn_contract` | initial holder、delegation、claim target、return target、single-use boundary。 |
| `claim_artifact` | claim 后 `BLUEPRINT_STATE` exact target bytes、expected Git blob、claim allowlist。 |
| `final_artifacts` | 每个目标 path、action、exact bytes/patch/transform spec、expected Git blob。 |
| `preimages` | `COMMIT_TREE_BOUND` path/blob，或明确的 `EXTERNAL_RAW_SHA256`；必须带 hash basis。 |
| `allowlists` | claim、normal final、authorized abort/fallback 分开列出。 |
| `denyset` | preserve/zero-diff path/pattern。 |
| `commit_chain` | parent→claim→final/abort 的父子关系、固定 commit message 与禁止历史改写。 |
| `fallback_graph` | 预授权 adapter 顺序、触发条件、等价结果条件、耗尽后的结论。 |
| `evidence_contract` | 结构化 evidence 字段、semantic assertions、landing path。 |
| `non_scope` | 明确禁止的阶段、路径、语义决定。 |

### 3.2 Payload identity

Package publish 前必须同时满足：

- package 内每个文件有 content digest；
- exact target artifact 预计算 expected Git blob；
- manifest 自身 digest 与 package inventory 可核验；
- claim/final/abort targets 均为独立完整状态，不依赖执行时临场编辑；
- target path 使用 Git canonical path（`/`），filesystem path 仅属 adapter-local 表达；
- text encoding、BOM、newline policy 进入 artifact byte identity；
- 同 path 多阶段目标必须分别命名，禁止用“稍后再手改”表达状态变化。

## 4. Execution Mechanism Class

| Class | 适用条件 | Authoritative 内容 | Script 地位 | 禁止 |
|---|---|---|---|---|
| `STATIC_TARGET_BUNDLE` | 目标是有限文档/配置/代码文件，最终 bytes 可由 ARCH 预先给出 | exact claim/final/abort artifacts + manifest | OPTIONAL | coder 重写、补全文案、选择 merge 策略 |
| `PATCH_BUNDLE` | 必须保留大文件未改部分，且 patch 可安全绑定 preimage | parent-bound patch + exact preimage + expected result blobs | OPTIONAL | patch fuzz、错 parent 自动套用、冲突时自行解决 |
| `DETERMINISTIC_TRANSFORM` | 结果必须由真实输入执行计算、迁移、生成，不能合理预先携带全部结果 | transform spec + executable identity + input identity + expected invariants/result identity | REQUIRED only when computation is essential | 把普通文件替换包装成强制脚本；fixture hash 冒充真实输入 |
| `RUNTIME_DISCOVERY_REQUIRED` | 运行时/远程/环境事实未闭合，静态 authoring 会猜测 Truth | discovery schema + read-only commands + evidence boundary + architecture return gate | Discovery tool may be required | discovery 结果未经 ARCH adjudication直接进入 mutation |

Class 由 ARCH 在 package freeze 前唯一选择。FULL/LITE、Universal Level、GC coverage、actor capability 均不得代替 mechanism class。

## 5. Execution Adapter Contract

Adapter 只把 Authoritative Payload 映射为 repository filesystem/index/commit；没有语义修改权。

每个 adapter 必须声明：

- adapter ID 与支持平台；
- 输入 artifact 与输出 path 的机械映射；
- isolation method；
- encoding/newline preservation；
- tracked/untracked/index handling；
- 成功证据；
- soft compatibility failure codes；
- 不得捕获为 soft failure 的 hard STOP codes。

### 5.1 Host preservation

- 宿主 worktree 只读保护；可以 dirty，HEAD 可以不是 Handoff Parent。
- 不得以宿主 dirty 或宿主 HEAD 不同作为 STOP。
- 真正执行必须从固定 Handoff Parent 建立安全隔离环境：临时 clone、detached worktree、等价受控 sandbox 均可。
- adapter 不得 reset/clean/checkout 覆盖宿主，不得把宿主 untracked 文件带入 target tree。

### 5.2 Static target adapter 等价条件

允许的 adapter 可包括 native file copy、PowerShell、Bash、Git plumbing、Python helper。任意 adapter 只有在以下结果全部相同时才等价：

- target path set 相同；
- target Git blobs 相同；
- deletions/modes 相同；
- claim/final/abort lifecycle 相同；
- evidence 字段相同；
- 无 allowlist 外 tracked/untracked/index residue。

## 6. Compatibility Fallback Contract

Package 必须在 freeze 时给出有向 fallback graph，例如：

```text
OPTIONAL_PRIMARY_HELPER
  → NATIVE_MECHANICAL_COPY
  → ALTERNATE_PREAUTHORIZED_ADAPTER
  → AUTHORIZED_ABORT_TARGET（仅 claim 已发布且所有 adapter 耗尽）
```

规则：

1. fallback 只能按 manifest 预授权边执行；CODE 不得设计新路径。
2. 每条边必须给出 trigger、允许输入、相同 expected blobs 与 evidence gate。
3. soft compatibility 出现时自动切换，不回 ARCH，不修改 manifest。
4. 任一 fallback 需要语义选择、patch conflict resolution 或内容重写时，立即升级 hard STOP。
5. claim 已推送但 final 无 adapter 可完成时，只能落预制 `AUTHORIZED_ABORT_TARGET`，返回 REVIEW，状态为 `COMPATIBILITY_BLOCKED / PENDING ARCH REVIEW`；abort 不是任务完成。
6. remote 已发生无法纳入固定 parent chain 的变化时，不得用 abort 覆盖远程。

## 7. Hard STOP / Soft Compatibility Taxonomy

### 7.1 Soft compatibility

默认 soft，且存在预授权等价 adapter 时自动 fallback：

- OS path separator / path presentation；
- Python version、launcher name、module availability；
- UTF-8/GBK console rendering；
- PowerShell/Bash command differences；
- optional tool missing；
- worktree/temp-clone path behavior；
- adapter startup/quoting behavior；
- optional verifier failure while independent evidence can still run。

Soft failure 只描述 mechanism 不兼容，不得改变 payload bytes 或 acceptance criteria。

### 7.2 Hard STOP

以下任一成立立即 STOP；不得自动设计 fallback：

- remote target ref 不再等于固定 Handoff Parent（claim 前），或 fixed chain 被其他 commit 插入；
- canonical continuity、source identity、authority 或 required Truth 无法建立；
- TURN/delegated authorization 不成立、已使用、parent 不匹配或 claim 超 State-only allowlist；
- 无法建立任何安全隔离执行环境；
- package/manifest/artifact identity 不成立；
- repository preimage 与 `COMMIT_TREE_BOUND` identity 不符；
- final/abort changed set 越 allowlist，或 denyset 非零；
- target Git blobs/modes/deletions 与 manifest 不一致；
- tracked + untracked + index 状态无法证明；
- claim/final parent chain 不成立；
- push 后 remote ref 无法验证为刚推送 commit；
- evidence 无法独立取得或 cross-document semantic assertion 失败；
- 出现 Blueprint 未闭合的语义选择；
- credential/sensitive ancestry、权限或网络问题使安全发布/验证不成立。

## 8. Adapter-independent Evidence Contract

Evidence 证明状态，不证明 adapter 本身“运行成功”。至少包含：

| Evidence ID | 必证事实 |
|---|---|
| `EV-PARENT` | fetch 后 remote target ref = fixed Handoff Parent；记录 40 位值。 |
| `EV-HOST` | 宿主未被执行 adapter 修改；宿主 dirty/HEAD 仅记录，不作为 preimage。 |
| `EV-ISOLATION` | 隔离环境从 fixed parent 创建，初始 tracked/untracked/index changed set 为空。 |
| `EV-PACKAGE` | manifest/package/artifact digest 全部匹配。 |
| `EV-CLAIM-SET` | claim staged/committed changed set 精确等于 claim allowlist，State blob 等于 claim target。 |
| `EV-CLAIM-REMOTE` | claim push 后 remote ref = claim commit；claim parent = Handoff Parent。 |
| `EV-FINAL-SET` | final staged/committed changed set精确等于 normal/abort allowlist。 |
| `EV-BLOBS` | 每个 target Git blob/mode/deletion 与 manifest 一致。 |
| `EV-RESIDUE` | Git porcelain `-z` 语义下 tracked、untracked、index 无未授权 residue。 |
| `EV-DENY` | preserve/deny set 相对 Execution Parent 为 zero-diff。 |
| `EV-CHAIN` | final/abort parent = claim；禁止 amend/rebase/force/history rewrite。 |
| `EV-SEMANTIC` | manifest 定义的 cross-document lifecycle/truth assertions 全 PASS。 |
| `EV-REMOTE` | final/abort push 后 remote ref = delivered commit。 |
| `EV-ATTRIBUTION` | outcome 分类为 execution、compatibility、Blueprint/ARCH-payload defect；证据明确。 |

Git path parsing 规则：

- machine gate 使用 Git `-z` 输出或等价 Git plumbing，不按控制台换行/制表符拆路径；
- Git canonical path 始终按 `/` 比较；Windows `\` 只在 adapter filesystem 层归一化；
- human-readable `git status`/`name-status` 只作展示，不是唯一 machine proof；
- expected blobs 从 exact artifact bytes 计算，不从 fake checkout 重建 source Truth。

## 9. REMOTE_READ_ONLY_ARCH Transaction Contract

固定事务：

```text
TURN=REVIEW / remote=Handoff Parent
→ Blueprint 授权 single-task delegated claim
→ fixed parent 隔离环境
→ exact State-only claim target
→ claim commit + push + remote verify
→ claim = Execution Parent / TURN=CODE
→ exact final target artifacts
→ CODE→REVIEW
→ final commit + push + remote verify
→ CODE STOP
→ remote ARCH independent audit
```

补充约束：

- delegation 绑定 package ID、revision、parent、target ref、single use；不能从历史包类推。
- claim target 必须由 ARCH 预制，CODE 不编辑 State 文案。
- final/abort target必须由 ARCH 预制；Execution Parent 永远是已远程验证的 claim commit。
- allowlist diff 只相对 Execution Parent 判 final scope；Design Baseline/Handoff Parent 不能代替。
- `/clear` 可以发生在包交付前；claim 与 final 事务间不得丢失 package/claim identity。
- normal/abort 安全推送后 CODE 只返回完整 final hash；不得继续下一批。

## 10. Package Self-Application / Architecture Payload Acceptance Gate

Architecture package 自身是独立审计对象。freeze 前必须逐项 PASS：

1. source identity / authority discovery；
2. hash basis 标签：`COMMIT_TREE_BOUND` / `EXTERNAL_RAW_SHA256` / `FIXTURE_ONLY`；
3. TURN delegation 与 single-use parent binding；
4. host preservation；
5. isolated execution 可行性；
6. claim/normal/abort allowlist；
7. tracked + untracked + index changed-set；
8. Git porcelain `-z` / plumbing parsing；
9. Windows-style filesystem path 与 Git path 分层；
10. payload/manifest/expected blob identity；
11. lifecycle/truth cross-document consistency；
12. preserve/deny set；
13. remote parent/branch assumption；
14. fake fixture 不冒充 repository Truth；
15. compatibility failure 与 semantic STOP 分类；
16. adapter fallback graph 完整；
17. evidence 能脱离 primary adapter 独立重跑；
18. semantic truth gate 独立于 deterministic byte gate；
19. package defect attribution 不污染 coder capability ledger；
20. non-scope（M2/Phase 3B 等）可机械验证。

Acceptance 结论只能是：

- `PACKAGE_ACCEPTED`；
- `PACKAGE_REWORK_REQUIRED`（package/architecture defect）；
- `BLOCKED_MISSING_AUTHORITATIVE_TRUTH`；
- `BLOCKED_EXECUTION_AUTHORITY_OR_REMOTE_STATE`。

未经 `PACKAGE_ACCEPTED` 不得交 CODE。

## 11. Defect Attribution Contract

| 分类 | 判据 | 能否计入 coder 能力负样本 |
|---|---|---|
| `EXECUTION_DEVIATION` | payload、adapter options、evidence gate 已闭合且可执行，CODE 未遵守 | 可以，需远程证据 |
| `SOFT_COMPATIBILITY` | 环境机制不兼容但有等价 fallback | 不可以 |
| `COMPATIBILITY_EXHAUSTED` | 所有预授权 adapter 失败，语义 Truth 仍完整 | 不可以；返回预制 abort（若安全） |
| `BLUEPRINT_DEFECT` | 合格 coder 遵守包仍可能得到错误/多解结果 | 不可以 |
| `ARCH_PAYLOAD_DEFECT` | exact payload 自身含错误、假 Truth、错误 hash/path/状态 | 不可以 |
| `EXTERNAL_TRUTH_CHANGE` | remote/canonical/authority 在固定 parent 外变化 | 不可以 |

R1～R3 依本合同均归 `ARCH_PAYLOAD_DEFECT` 或 package compatibility defect；三轮正确 STOP 不计 GPT-5.6 Luna 能力负样本。

## 12. Canonical Placement Decision

### 12.1 未来用户级 `blueprint_protocol.md`

应进入的跨项目 C 档语义合同：

- Authoritative Payload / Adapter / Evidence 三层分离；
- WHAT frozen、HOW compatible；
- 四种 Execution Mechanism Class 与选择规则；
- manifest 的最低语义字段；
- preimage hash basis 与 fixture 禁冒充 Truth；
- Compatibility Fallback Contract；
- Hard STOP / Soft Compatibility taxonomy；
- adapter-independent evidence 最低集合；
- Package Self-Application / Acceptance Gate；
- Blueprint/ARCH-payload defect 不计 coder 负样本；
- byte identity PASS 与 semantic truth PASS 分离。

### 12.2 用户级 `GLOBAL.md`

跨机器运行/握手 owner 应保留或升级在 GLOBAL，而不在 protocol 建第二 Truth：

- `WRITE_CAPABLE_ARCH` 与 `REMOTE_READ_ONLY_ARCH` 的 TURN 交接差异；
- single-task delegated claim；
- host worktree 只读、fixed-parent isolated execution；
- claim→final/abort→REVIEW 事务；
- push/remote verification 与 history rewrite 禁令。

`blueprint_protocol.md` 只引用该 operation contract，并补 C 档 package 约束。

### 12.3 UBF implementation architecture / project overlay

只留实现细节：

- `ubea/v2` manifest 的具体 JSON/YAML key、目录命名与 package inventory；
- Cookbook branch/ref、State path、commit messages、report/ledger paths；
- R4 exact target artifacts、expected blobs、preimages 与 semantic assertions；
- Windows/PowerShell/Bash/native Git 具体 adapter recipes；
- fixture cases 与 CI/lint 实现；
- 当前 UBF stage、M1 Work-01、M2/Phase 3B gates。

### 12.4 `MODEL_ROUTING.md`

不因 v2 修改角色层级语义。旗舰/主力/快速/研究是 Actor/Capability Routing；Execution Mechanism Class 与 Universal Level 均是正交对象。根级与 `codex/MODEL_ROUTING.md` 的关系须经 Source Identity Discovery 判定，不能以 hash 不同直接称 drift。

## 13. R4 Self-Application Result（Acceptance Snapshot）

Target: `UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01 R4 — STATIC_TARGET_BUNDLE`

Fixed Handoff Parent: `795d2b9c807fe3954f1ac5f4cda60392c7ff9cc9`

Remote observation at 2026-08-12: `origin/master = 795d2b9c807fe3954f1ac5f4cda60392c7ff9cc9`。

### 13.1 Verified available inputs

| Input | Result |
|---|---|
| repository fixed parent | PASS |
| `~/.ai-context/rules/blueprint_protocol.md` RAW SHA-256 | `c2c8332eb12d545ca89fca4c80a15dba7e2acf5faf7703a8cfe6815a0b5f0eb3` |
| `~/.ai-context/GLOBAL.md` RAW SHA-256 | `73cf5c049585542b0f82ea216eab55ee4864399b71bfb6131efaeec254e540d0` |
| root `~/.ai-context/MODEL_ROUTING.md` RAW SHA-256 | `86b3dec955420552cbe7bcf5bc147478af06b67a6a95c0bf09cd80639bf636be` |
| M0/M1 Preview/Start repository sources | PASS |
| M2 / CookBook Phase 3B non-start boundary | PASS |

### 13.2 Authoritative input restoration

初次 Self-Application 时，当前会话与固定 parent 均未包含以下 exact bytes：

1. 已冻结的 64 条 semantic records 正文；
2. 已冻结 current-state map 正文；
3. 已冻结 contradiction / gap / preserve matrix 正文；
4. R1/R2/R3 的 exact target artifact set / manifest；
5. 如 Work-01 记录依赖其正文而非仅登记 gap，`~/.ai-context/codex/MODEL_ROUTING.md` 的 source-identity evidence/body。

聊天交接只证明“这些架构判断已完成”，不能证明逐条记录的 exact content。重新从 canonical source 推导 64 条会构成未经授权的 semantic decomposition redo，并可能制造第二套 frozen result。

用户随后提供原始 `UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01 R3` package。其 `MANIFEST.sha256` 4/4 PASS，`Architecture_Result_R3.md` 完整包含 64 条 frozen semantic records、current-state map、contradiction/ambiguity matrix、gap matrix、preserve matrix 与 CookBook overlay boundary。R4 对该正文执行 byte-preserving migration，不重新分析或改写 64 条记录。因此原 `BLOCKED_MISSING_AUTHORITATIVE_TRUTH` 已解除。

Source Identity Discovery 当前能冻结的唯一结论：

| Object | Reviewed identity | Result |
|---|---|---|
| root `~/.ai-context/MODEL_ROUTING.md` | `SHARED_ROUTING_CORE` candidate；正文直接审阅 | `DIRECTLY_REVIEWED` |
| `~/.ai-context/codex/MODEL_ROUTING.md` | `CODEX_ROUTING_ADAPTER_OR_OVERLAY` candidate；M0 仅保存历史 SHA/15 行，正文未入仓 | `HISTORICALLY_ATTESTED / CURRENT BODY ABSENT` |
| sibling relation | `GLOBAL.md` 要求先 shared root、再读 Codex-specific routing | `UNRESOLVED PENDING BODY/PROVENANCE` |
| semantic drift | 不同 hash 不能证明同一 Source Identity 或共享条款冲突 | `NOT ESTABLISHED` |

如 frozen 64-record artifact 对 codex routing 只登记 `UNKNOWN/UNRESOLVED`，正文缺失可作为 gap 保留；如它已作具体 authority/drift 裁决，则缺正文是额外 hard STOP。

### 13.2.1 Fixed repository preimages available for R4

| Path at Handoff Parent | COMMIT_TREE_BOUND Git blob | Raw bytes SHA-256 |
|---|---|---|
| `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` | `d9731304433a80b679d923a56367691a47755087` | `dad54586fcb4e764b8cb5c9b6bde692dc0bf782d2091b94c3345e5d54600cdf8` |
| `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-PREVIEW-START.md` | `2d57412c0d47829ab2d782bb5fe5df8b556487b5` | `cfc1fe7753281d98eae03eedcbb4b9d9de4f668897ba3b2b22c59a74b56a5836` |
| `.ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md` | `78e8222eb11c2e5b733f1154c3e768ac15b265ec` | `bad33cc1f1b02e711a96b16338e89d90c38f04f5e85720d14f61a2359b0ec186` |
| `.ai-context/docs/experience/14_模型执行力评估.md` | `c1dc250466eafb21e6b48e0eadbd8974ef518343` | `064e89095723d93ebd7f09205547e5740e5e7d0a9e81c2493beed27f4cabc216` |

### 13.2.2 R4 normal scope once exact Work-01 artifacts are restored

Canonical Registry Discovery disposition for the v2 project document: `CREATE_NEW_NO_OVERLAP`，因为它是 implementation architecture，受总控管理，不替代用户级 protocol。

| Action | Path |
|---|---|
| A | `.ai-context/docs/项目改造规划/蓝图设计2/Universal-Blueprint-Execution-Architecture-v2.md` |
| A | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01.md` |
| A | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01_Luna_Execution_Blueprint.md` |
| A | `.ai-context/docs/UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01-Execution-Report.md` |
| M | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-PREVIEW-START.md` |
| M | `.ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md` |
| M | `.ai-context/docs/experience/14_模型执行力评估.md` |
| M | `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` |

Final lifecycle target：

```text
M0: ACCEPT / CLOSED
M0→M1 Handoff: ARCH ACCEPTED / CONSUMED BY M1 PREVIEW/START-01
M1 Preview/Start: ARCH ACCEPT / CONSUMED BY WORK-01
M1 Work-01: COMPLETE / PENDING REMOTE ARCH REVIEW
M2+: NOT STARTED
CookBook Phase 3B: NOT AUTHORIZED TO START
TURN: REVIEW
```

`UBF-M0-to-M1-Handoff.md` 不在 R4 scope；它已被 Preview/Start 消费，Work-01 不得二次消费。项目 GC registry、Project Graph、M0 assets、用户级 canonical 与生产代码全部进入 zero-diff deny set。

STATIC target artifact 不写执行时才知道的 claim/final hash 占位符。commit identity 用结构性 parent contract 表达（claim parent = Handoff Parent；final parent = claim），实际 40 位 hash 只进入 Git evidence；否则 package 不再是 exact static target。

### 13.3 Gate result

| Gate | Result |
|---|---|
| Source identity | PASS；R3 frozen result restored；root/codex routing relation remains explicitly unresolved |
| Payload identity | PASS |
| STATIC target blobs | PASS / recorded in R4 manifest |
| Semantic truth gate | PASS；64 records preserved；M2/Phase 3B gates retained |
| Adapter/fallback design | PASS at universal architecture level |
| R4 package release | **PACKAGE_ACCEPTED** |

原阻断归因保持为正确的 hard Truth STOP；它在 exact R3 package 到达后通过 source restoration 正常解除，而不是通过 Luna 补写、R3 Python fallback 或重新分解绕过。

### 13.4 Restored input identity

Restored package ZIP SHA-256：`06443e2bd49ebe601d93f0af0bc42b80e7e3722caba69f095e8d028c9ea172f0`。

R3 package manifest entries：apply script、execution blueprint、bootstrap、architecture result，全部校验 `OK`。R4 只继承 architecture result 的 frozen semantic payload；R3 Python/bootstrap 不再是 execution Truth。

## 14. 当前执行口令状态

`R4 EXECUTION: AUTHORIZED ONLY THROUGH THE ACCEPTED STATIC_TARGET_BUNDLE`。

Luna 必须使用 R4 manifest + exact claim/final/abort artifacts；R3 script 仅为历史来源，不得作为 primary execution mechanism。R4 正常交付后 STOP，等待 remote ARCH review；不启动 M1 End/Accept、M2 或 CookBook Phase 3B。
