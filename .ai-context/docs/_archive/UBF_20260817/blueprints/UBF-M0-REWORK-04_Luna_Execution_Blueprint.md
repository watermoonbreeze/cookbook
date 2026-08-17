# UBF-M0-REWORK-04 — Isolated M0 Governance Repair and Evidence Closure

> Document Role: Luna Mechanical Execution Blueprint
> Status: `READY FOR EXECUTION`
> Task ID: `UBF-M0-REWORK-04`
> Blueprint Revision: `R1`
> Review Operation Mode: `REMOTE_READ_ONLY_ARCH`
> Execution Worktree Mode: `ISOLATED_DETACHED_CLEAN`
> Package Profile: `FULL`
> CookBook Legacy Granularity: `L7`
> Handoff Parent: `2a5567193c688bbd0e30f323699a68aab1ffeb34`
> Execution Parent: `CLAIM_COMMIT_RESOLVED_AT_RUNTIME`
> Expected Initial TURN: `REVIEW`
> Execution TURN: `CODE`
> Return TURN: `REVIEW`
> Turn Transfer Actor: `CODE_DELEGATED_CLAIM`
> Target Branch: `master`
> Target Repository: `cookbook`
> User-designated Execution Model: `GPT-5.6 Luna`
> Date: `2026-08-12`

## 0. Execution contract

Read this entire document in a fresh context before acting. This document is the only execution authority for this batch.

This is a narrow UBF M0 governance-document REWORK. It does not authorize architecture redesign, UBF M1, CookBook Phase 3B, production code, tests, build files, Project Graph lifecycle changes, user-level protocol changes, cleanup of the user's original worktree, or history rewriting.

The repository-carried R3 blueprint is a frozen repair specification incorporated by §3. It is evidence and a referenced patch contract, not a second task authority. Where R3 runtime metadata conflicts with this R4 blueprint, R4 controls.

Repository naming contract:

- identify the target only as `cookbook` or `the current cookbook repository`;
- do not record a hosting provider, account owner, organization or namespace;
- use the configured `origin` without changing it;
- `origin/master` is a Git ref and is permitted;
- do not copy the full origin URL into committed documents or chat output.

Remote-review visibility contract:

- after a successful claim push, every safely publishable result must be committed and pushed;
- `COMPLETE`, `PARTIAL` and `BLOCKED_FOR_REVIEW` mean remote review input, not architecture acceptance;
- an unanticipated issue receives a stable ID and is returned for architecture disposition;
- only §18 `NON_PUBLISHABLE_STOP` may leave the task without a remote review-input push.

On a successful final push, reply only:

```text
COMMIT_HASH: <40-character final remote commit hash>
```

## 1. Overall status and authorized sequence

| Layer | Current state at Handoff Parent | Meaning | Authorized next action |
|---|---|---|---|
| Universal Blueprint Framework | `M0 — Migration Control & Truth Lock` | M0 is not accepted | Execute only `UBF-M0-REWORK-04` |
| R3 review target | `2a5567193c688bbd0e30f323699a68aab1ffeb34` | `BLOCKED_FOR_REVIEW / VALID REVIEW INPUT / REWORK REQUIRED` | Preserve the valid fallback and repair the unresolved M0 artifacts in an isolated clean worktree |
| R3 implementation result | Four-file fallback transaction valid; original repairs closed `0/10` | The fallback is evidence, not completion | Backfill its final hash and ARCH review, correct its report, then execute the ten repairs |
| M0 completion | `NOT ACCEPTED` | Coder outcome is not architecture ACCEPT | R4 must return to independent remote review |
| M0 → M1 | `NOT AUTHORIZED` | No automatic continuation | Only after R4 ACCEPT may a separate M0 End/Accept + M0→M1 Handoff persistence blueprint be issued; that batch must also be reviewed before a separate M1 Preview/Start |
| CookBook Phase 3A | `EXECUTED / REWORK REQUIRED / PAUSED` | Separate lifecycle | Do not change |
| CookBook Phase 3B | `NOT AUTHORIZED TO START` | Separate lifecycle | Do not start or modify |

Required sequence:

```text
R4 isolated clean-worktree execution
→ remote architecture review
→ if REWORK: next narrow repair blueprint
→ if ACCEPT: separate M0 End/Accept + M0→M1 Handoff persistence batch
→ remote review of persisted handoff
→ separate M1 Preview/Start
```

No step after R4 review is automatic.

## 2. Architecture disposition ledger

### 2.1 Prior repair issues still open

| Issue ID | Status at `2a556719` | Disposition | R4 boundary |
|---|---|---|---|
| `UBF-M0-R2-02` | Supplement completion/status and integrity metadata remain stale | `REPAIR` | Apply the Supplement contract in §§10–11 |
| `UBF-M0-R2-05` | R2 report remains materially inaccurate | `REPAIR` | Replace it using §12 |
| `UBF-M0-R2-07` | Control R4 text remains encoding-corrupted/incomplete | `REPAIR` | Apply R3 §13 with R4 metadata substitutions in §13 |
| `UBF-M0-R2-08` | Supplement hash basis and external-state labels remain stale | `REPAIR` | Apply the fixed values in §10 |
| `UBF-M0-R3-01` | Repository R2 blueprint remains stale R1 | `REPAIR` | Restore exact official R2 file using §12 |
| `UBF-M0-R3-02` | Supplement provenance/current integrity list remain stale | `REPAIR` | Apply §§10–11 |
| `UBF-M0-R3-03` | R2 report still contains contradicted self-assessment and identity detail | `REPAIR` | Replace using §12 |
| `UBF-M0-R3-04` | State was corrected only enough for fallback; M0 repairs remain open | `REPAIR` | Create truthful current R4 and reviewed R3 records using §8/§16 |
| `UBF-M0-R3-05` | Current UBF stage/transition gate are not yet repaired in Control/Supplement | `REPAIR` | Apply §§10–13 |
| `UBF-M0-R3-06` | R3 ledger row lacks final commit and ARCH review; current governance evidence incomplete | `REPAIR` | Backfill R3 and append R4 exactly as §14 |

Accepted evidence remains preserved:

- `UBF-M0-R2-01`: three embedded bodies are 479/55/448 lines and byte-equal;
- `UBF-M0-R2-03`: Truth Pack §A/§J consistency;
- `UBF-M0-R2-04`: R2 whitespace gate;
- `UBF-M0-R2-06`: R2 claim/return transaction;
- R3 blueprint and four-file fallback scope;
- R3 claim/final direct-parent chain and final `TURN=REVIEW`.

### 2.2 Issues discovered in R3 remote review

| Issue ID | Verified fact | Disposition | Exact R4 result |
|---|---|---|---|
| `UBF-M0-R3-EXEC-01` | Original worktree had pre-existing allowlisted modifications, including deleted Control and modified Supplement | `REPAIR` | Execute from a clean linked worktree created from the Handoff Parent; do not touch the original worktree |
| `UBF-M0-R4-01` | R3 claim State omitted R2 as the immediately previous batch until final delivery and used `CLAIM_COMMIT_PENDING` instead of the required literal `PENDING CLAIM COMMIT` | `REPAIR` | Record the historical deviation in corrected R3 report; do not rewrite commits; use the required literal in the new R4 claim State |
| `UBF-M0-R4-02` | R3 report aggregated R3-01~06 and did not give one evidence row per issue | `REPAIR` | Replace R3 report with one row per R2/R3 repair issue and one row per R4 review issue |
| `UBF-M0-R4-03` | R3 report omitted the three fixed retained-source LF hashes | `REPAIR` | Add all three path/hash/line/byte-compare rows to corrected R3 report |
| `UBF-M0-R4-04` | R3 report summarized dirty entries instead of preserving every exact path | `REPAIR` | Record only recoverable verified facts: Control was pre-deleted, Supplement pre-modified, additional paths were not preserved and cannot be reconstructed from remote Git; never invent path names |

Every Issue ID above has exactly one disposition. Do not create a second disposition or reinterpret a verified fact.

## 3. Frozen repair specification and precedence

```text
<R3_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-03_Luna_Execution_Blueprint.md
```

The frozen file must be:

```text
Line count after LF normalization: 690
SHA-256 after LF normalization: 815c0344698baad56797a863eabd4d8202e6f981fa28df34017a0fa21a5596ef
```

Do not modify `<R3_BLUEPRINT>`.

The following R3 sections are incorporated as exact content repair specifications:

- R3 §8: official R2 blueprint restoration;
- R3 §§9–10: Supplement provenance, hashes, status and integrity;
- R3 §11: corrected R2 historical report;
- R3 §13: Control R4 contract and model evidence contract;
- R3 §15: content validation fixed values.

R4 overrides the incorporated text only as follows:

1. Current task is R4, not R3.
2. Handoff Parent is `2a5567193c688bbd0e30f323699a68aab1ffeb34`.
3. Current Execution Parent is the R4 claim commit.
4. Current review result is the R3 result defined in §1.
5. Normal delivery is the nine-file R4 allowlist in §4.
6. Current report is `<R4_REPORT>`; `<R3_REPORT>` becomes corrected historical evidence.
7. State, ledger backfill/append, outcome, commit and transition rules come from this R4 blueprint.
8. `REWORK-03` references that describe the historical R3 event remain R3 and are not renamed.

If an incorporated R3 clause conflicts with these eight overrides, apply the R4 override. Do not otherwise broaden or redesign the frozen repairs.

## 4. Paths and allowlists

```text
<SUPPLEMENT> = .ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md
<CONTROL> = .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md
<R2_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
<R2_REPORT> = .ai-context/docs/UBF-M0-REWORK-02-Execution-Report.md
<R3_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-03_Luna_Execution_Blueprint.md
<R3_REPORT> = .ai-context/docs/UBF-M0-REWORK-03-Execution-Report.md
<R4_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-04_Luna_Execution_Blueprint.md
<R4_REPORT> = .ai-context/docs/UBF-M0-REWORK-04-Execution-Report.md
<MODEL_LEDGER> = .ai-context/docs/experience/14_模型执行力评估.md
<STATE> = .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Claim commit allowlist, exactly one file:

```text
M  <STATE>
```

Normal `COMPLETE` or publishable `PARTIAL` final allowlist, exactly nine files:

```text
M  <SUPPLEMENT>
M  <CONTROL>
M  <R2_BLUEPRINT>
M  <R2_REPORT>
M  <R3_REPORT>
A  <R4_BLUEPRINT>
A  <R4_REPORT>
M  <MODEL_LEDGER>
M  <STATE>
```

`BLOCKED_FOR_REVIEW` fallback allowlist, exactly four files:

```text
A  <R4_BLUEPRINT>
A  <R4_REPORT>
M  <MODEL_LEDGER>
M  <STATE>
```

In fallback, safely attributable task-owned edits to other paths remain unstaged and are listed in `<R4_REPORT>`.

## 5. Preserve set and denylist

Preserve without modification:

- Git history through the Handoff Parent;
- the user's original worktree, index, dirty/untracked files and contents;
- `<R3_BLUEPRINT>` byte-for-byte;
- Truth Pack in full;
- all three Supplement §D embedded source bodies;
- canonical GC/protocol/Project Graph/Phase state source files;
- all model-ledger rows except the explicitly authorized R3 backfill and one R4 append;
- Phase 3A, Phase 3B and `GOV-BP-P3-01` source semantics.

Do not modify or stage:

- `.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`;
- user-level `GLOBAL.md`, `MODEL_ROUTING.md` or `rules/blueprint_protocol.md`;
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`;
- `.ai-context/docs/experience/INDEX.md`;
- `.ai-context/project_graph/README.md`;
- `project.yaml`, Phase 3 migration/control/audit/acceptance/handoff files or generated views;
- any production code, test, build, dependency or configuration file;
- any file outside the selected outcome allowlist.

Do not run `pull`, `merge`, `rebase`, `reset`, `clean`, `stash`, amend, force push or history rewrite. Do not remove a worktree during this task.

## 6. Isolated clean-worktree bootstrap

Run this section from the existing cookbook working copy before editing any file.

1. Resolve the repository top level and confirm its configured `origin` exists.
2. Use `git remote get-url origin` only for local validation. Do not output or commit the value.
3. Fetch `refs/heads/master` without merging it into any local branch.
4. Require `origin/master` to equal the full Handoff Parent.
5. Require the final remote path component, ignoring case and optional `.git`, to equal `cookbook`.
6. Do not require the original worktree to be clean. Do not read changed-file contents. Its dirty state belongs to the user.
7. Create a sibling linked worktree from the exact Handoff Parent using detached HEAD.

Preferred sibling directory name:

```text
cookbook-ubf-m0-rework-04-clean
```

If that name already exists or is registered, choose the first unused suffix `-01` through `-20`. Do not delete, overwrite, prune or reuse an existing directory/worktree.

Equivalent Git operation:

```text
git worktree add --detach <ISOLATED_WORKTREE> 2a5567193c688bbd0e30f323699a68aab1ffeb34
```

After creation, run all remaining commands inside `<ISOLATED_WORKTREE>`. Do not copy any repository file from the original worktree. The user-provided R4 blueprint may be written directly to `<R4_BLUEPRINT>` under §9.

The isolated worktree must start with:

```text
HEAD = 2a5567193c688bbd0e30f323699a68aab1ffeb34
origin/master = 2a5567193c688bbd0e30f323699a68aab1ffeb34
branch = detached HEAD
index = empty
worktree = clean
```

Record in reports only `Execution Worktree Mode: ISOLATED_DETACHED_CLEAN`; do not commit an absolute filesystem path.

Use `NON_PUBLISHABLE_STOP` if an isolated clean worktree cannot be created without touching existing user files, or if all 20 candidate names are occupied.

## 7. Isolated preflight

Inside the isolated worktree, run and record sanitized results in `<R4_REPORT>`:

```text
git rev-parse --show-toplevel
git rev-parse HEAD
git branch --show-current
git remote get-url origin
git ls-remote origin refs/heads/master
git status --short --untracked-files=all
git diff --cached --name-status
git show HEAD:<STATE>
git rev-list --parents -n 5 2a5567193c688bbd0e30f323699a68aab1ffeb34
git diff-tree --check 838136d645b7ac73c200f08305d052d6b93cad33 2a5567193c688bbd0e30f323699a68aab1ffeb34
```

Report remote identity exactly as:

```text
Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED
```

All must be true:

- HEAD and remote `master` equal the Handoff Parent;
- detached HEAD is expected and allowed;
- index and isolated worktree are clean;
- all existing normal-delivery targets are present and clean;
- `<R4_BLUEPRINT>` and `<R4_REPORT>` do not exist;
- `<STATE>` says `TURN=REVIEW` with abstract CODE/ARCH names;
- Handoff Parent has direct parent `838136d645b7ac73c200f08305d052d6b93cad33`;
- that claim commit has direct parent `c3c7b812272344935f2bb48f96a890d84081b5d3`;
- the claim changes only `<STATE>`;
- Handoff Parent changes exactly the four R3 fallback files;
- `<R3_BLUEPRINT>` matches the fixed line count and hash in §3;
- no preflight staged file exists.

## 8. Delegated R4 turn claim

Modify only `<STATE>`.

Create current batch:

```text
UBF-M0-REWORK-04 — Isolated M0 Governance Repair and Evidence Closure
```

Required current fields:

- status `AUTHORIZED / IN PROGRESS`;
- `TURN=CODE`;
- `CODE=Coder@当前机`, `ARCH=架构师@主力机`;
- Review mode `REMOTE_READ_ONLY_ARCH`;
- Worktree mode `ISOLATED_DETACHED_CLEAN`;
- Handoff Parent full `2a5567193c688bbd0e30f323699a68aab1ffeb34`;
- Execution Parent exactly `PENDING CLAIM COMMIT`;
- scope: all repair Issue IDs in §2;
- UBF Stage `M0 / REWORK BEFORE ACCEPTANCE`;
- next: execute only R4, then return to remote review; M1 and Phase 3B remain unauthorized.

Preserve R3 as the immediately previous batch and update its reviewed status to:

```text
BLOCKED_FOR_REVIEW / REMOTE ARCH REVIEWED / REWORK REQUIRED
```

Its reviewed delivery commit is `2a5567193c688bbd0e30f323699a68aab1ffeb34`; its unresolved issues are the ten open repair issues plus `UBF-M0-R3-EXEC-01` and `UBF-M0-R4-01 ~ UBF-M0-R4-04`.

Preserve R2 below R3. Do not write a concrete model name into `<STATE>`.

Stage only `<STATE>`. Require exactly one `M` entry and `git diff --cached --check` PASS.

Commit message:

```text
chore(governance): claim UBF-M0-REWORK-04
```

The claim commit must be a direct child of the Handoff Parent and change only `<STATE>`. Push normally with `git push origin HEAD:master`, verify `origin/master` equals it, and record the full hash as `<CLAIM_COMMIT>`.

## 9. Preserve this R4 blueprint

Save the exact user-provided contents of this document at `<R4_BLUEPRINT>`. Do not summarize, rewrite or alter its metadata.

## 10. Repair Supplement provenance, hashes and current status

Do not alter Supplement §D or any text inside its three four-tilde bodies.

Apply the substantive repair contract of R3 §§9–10, with this R4 header metadata:

```text
Document Role: Supplemental Evidence and Errata
Status: <OUTCOME> / PENDING REMOTE ARCH REVIEW
Original Capture HEAD: b7fc77e4d442364e6f5db790b374ece4c5da409d
Prior Repair Tasks: UBF-M0-REWORK-02, UBF-M0-REWORK-03
Current Repair Task: UBF-M0-REWORK-04
R2 Handoff Parent: b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
R3 Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
R3 Reviewed Delivery: 2a5567193c688bbd0e30f323699a68aab1ffeb34
R4 Handoff Parent: 2a5567193c688bbd0e30f323699a68aab1ffeb34
R4 Execution Parent: <CLAIM_COMMIT>
```

Required §C values, labeled `Repository LF-normalized SHA-256`:

| Path | SHA-256 | Lines |
|---|---|---:|
| `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | `0a58da55219ce134095c3a15881205124174b74994e47b58168a91c1f402c827` | 479 |
| `.ai-context/docs/experience/INDEX.md` | `e81a8e26866ba2db3347e998b79bba9833b189ef39e098f9bd023e045aa17241` | 55 |
| `.ai-context/project_graph/README.md` | `2e8ee6833d5cf672bc62118c41938436fcbc66ea24efad500838294b489a4677` | 448 |

Required §E evidence-snapshot values and basis:

| Source | Required value/basis |
|---|---|
| `.ai-context/project_graph/project.yaml` | `2c756ce240c129e72276d7a97842c953580c006b768227bb06c086c270ca2f0f` |
| `.ai-context/project_graph/migration/PHASE3A_AUDIT.md` | `c78dfbcb08b35ffbc26e165d139cd3929c9aae895bcc70e3acddb40fcb215a52` |
| `.ai-context/project_graph/migration/PHASE3A_BLUEPRINT.md` | `7269fab1a893212fce068367454051ae26c0d643452fac41b19b926c1c8b265b` |
| `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` at `b46b9dfe4d2328aeae6f2f244d7ba0a023eee402` | `8933b7d85d382f4bde8a8077db4359514833141a0d70c62efbfaf5785ce56109` |
| `<USER_HOME>/.ai-context/rules/blueprint_protocol.md` | `REMOTE_ATTESTED_EXTERNAL_STATE / NOT DIRECTLY REPRODUCIBLE FROM REPOSITORY`; retain the prior externally attested hash |

Do not retain the old Windows-worktree hashes. Rows reusing a source must reuse the same value and basis.

In §A and §H, state that:

- `c3c7b812...` was `PARTIAL / REWORK REQUIRED`;
- `2a556719...` was a valid blocked R3 review input but completed `0/10` repairs;
- R4 is the current isolated repair;
- UBF remains M0 and M1 is unauthorized;
- the three §D bodies were accepted and remain byte-for-byte preserved.

## 11. Repair Supplement completion and integrity

Replace stale §G–§I so they describe R4:

- §G: Handoff Parent `2a556719...`, Execution Parent `<CLAIM_COMMIT>`, isolated clean-worktree mode, original worktree untouched, selected outcome allowlist;
- §H: three embedded bodies are complete only because byte comparison passes; M0 cannot enter M1 until R4 receives ACCEPT and the separate handoff sequence completes;
- §I: exact nine normal files or exact four fallback files; unresolved R4 execution issues or `NONE`; `M0 transition: AWAITING REMOTE ARCH REVIEW`.

Normal nine-file list:

```text
M .ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md
M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md
M .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
M .ai-context/docs/UBF-M0-REWORK-02-Execution-Report.md
M .ai-context/docs/UBF-M0-REWORK-03-Execution-Report.md
A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-04_Luna_Execution_Blueprint.md
A .ai-context/docs/UBF-M0-REWORK-04-Execution-Report.md
M .ai-context/docs/experience/14_模型执行力评估.md
M .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Supplement may say `COMPLETE / PENDING REMOTE ARCH REVIEW` only when every R4 content and scope gate passes. End with exactly one newline.

## 12. Restore R2 blueprint and replace R2 report

Apply R3 §8 exactly to `<R2_BLUEPRINT>`. Required final value after LF normalization:

```text
Line count: 479
SHA-256: c9d8274e4c4247394fa7dfa97bc85af07fb4db09a397ab5989568d4f819e92ec
```

No other R2 blueprint change is allowed.

Replace `<R2_REPORT>` in full using R3 §11 exactly. It remains R2 historical evidence corrected by R3/R4 and must include:

- sanitized repository identity only;
- all eight R2 issues as individual actual-result rows;
- three exact retained-source integrity rows;
- R3-01 through R3-06 as individually opened issues;
- `Outcome at Reviewed Delivery: PARTIAL / REMOTE ARCH REVIEWED / REWORK REQUIRED`;
- no claim that R2 itself contained R3/R4 work;
- no concrete provider, owner, account, namespace or endpoint.

## 13. Repair Control and current-stage metadata

Apply the 19-rule UTF-8 R4 subsection and the seven-rule Model Execution Capability Evidence Contract from incorporated R3 §13 exactly. Do not alter M1–M8 semantics.

Update the Control header to:

```text
状态: M0 REWORK-04 IN EXECUTION / PENDING REMOTE ARCH REVIEW
Current UBF Stage: M0 — Migration Control & Truth Lock
Current Review Result: 2a5567193c688bbd0e30f323699a68aab1ffeb34 = BLOCKED_FOR_REVIEW / VALID REVIEW INPUT / REWORK REQUIRED
Current Repository Observation / Handoff Parent: 2a5567193c688bbd0e30f323699a68aab1ffeb34
CookBook Project Graph: Phase 3A EXECUTED / REWORK REQUIRED / PAUSED; Phase 3B NOT AUTHORIZED TO START
Process Revision: R4 — Remote Review Visibility & Issue Disposition Contract
```

Update `## 10. 当前启动判定` so it states:

- current UBF stage is M0;
- R3 was valid fallback evidence, not M0 completion;
- R4 is an isolated repair, not M1;
- R4 push requires remote architecture ACCEPT;
- after ACCEPT, a separate M0 End/Accept + M0→M1 Handoff persistence blueprint is required, followed by its execution and review, then a separate M1 Preview/Start;
- REWORK creates another narrow repair;
- Phase 3B and production code remain out of scope.

There must be no mojibake, Unicode replacement character, malformed arrow or malformed section symbol.

## 14. Update model execution capability ledger

Only two mutations are allowed in `<MODEL_LEDGER>`.

### 14.1 Backfill the existing R3 row

Locate the unique row with batch:

```text
UBF-M0-REWORK-03 M0 Governance Evidence and Status Repair
```

Replace only its commit cell and ARCH comment cell.

Commit cell:

```text
`2a5567193c688bbd0e30f323699a68aab1ffeb34`
```

ARCH comment cell:

```text
REMOTE ARCH 复核：BLOCKED_FOR_REVIEW 四文件 fallback、两提交链、allowlist 与 TURN=REVIEW 均有效；原十项治理修复完成 0/10，需在隔离干净 worktree 继续。R3 报告缺少逐 Issue 证据、三份固定 hash 和完整 dirty-path 记录；单批证据，不构成模型能力或路由结论。
```

Do not change any other R3 cell.

### 14.2 Append one R4 row

Append exactly one row to `## 评估台账`.

Fixed identity fields:

```text
批次: UBF-M0-REWORK-04 Isolated M0 Governance Repair and Evidence Closure
角色: CODE
实际模型: GPT-5.6 Luna
commit: 待 ARCH 依据最终远程 commit hash 回填
```

Task-complexity cell must include:

```text
治理文档精确 REWORK；隔离 detached clean worktree；9 文件正常 allowlist；REMOTE_READ_ONLY_ARCH 双提交事务；Package=FULL；CookBook Legacy Granularity=L7；无生产代码
```

Truthfully record before commit:

- selected outcome;
- number closed out of the ten original repair issues;
- R3 report/history/ledger correction result for R4-01~04;
- claim push and TURN return;
- exact staged file count;
- validation summary;
- new Issue IDs or `NONE`;
- no architecture capability conclusion.

ARCH comment cell:

```text
待 REMOTE ARCH 独立复核；本行只记录单批执行事实，不构成模型能力或路由结论。
```

If a more precise build/reasoning label is exposed, append it in parentheses after `GPT-5.6 Luna`. Never put the model name into `<STATE>`.

## 15. Replace R3 report with corrected historical evidence

Replace `<R3_REPORT>` in full. Required header:

```text
# UBF-M0-REWORK-03 Execution Report

Document Role: Repository-carried Mechanical Execution and Corrected Remote Review Evidence
Task ID: UBF-M0-REWORK-03
Blueprint Revision Executed: R1
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Package Profile: FULL
CookBook Legacy Granularity: L7
Execution Model: GPT-5.6 Luna
Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
Execution Parent / Turn Claim Commit: 838136d645b7ac73c200f08305d052d6b93cad33
Reviewed Delivery Commit: 2a5567193c688bbd0e30f323699a68aab1ffeb34
Expected Return TURN: REVIEW
Report Correction Task: UBF-M0-REWORK-04
Outcome at Reviewed Delivery: BLOCKED_FOR_REVIEW / REMOTE ARCH REVIEWED / REWORK REQUIRED
```

Required sections:

### A. Overall UBF Status

State M0, 0/10 original repairs, valid fallback, M1 prohibited and the full transition sequence.

### B. Preflight and Turn Claim

Record sanitized remote identity, exact parents, one-file claim, push and final TURN. Explicitly record these historical deviations without changing history:

- claim State did not retain R2 as immediately previous until final delivery;
- claim State used `CLAIM_COMMIT_PENDING`, not required `PENDING CLAIM COMMIT`.

### C. Architecture Disposition

One row for every accepted R2 issue, every ten repair issue, `R3-EXEC-01`, and `R4-01` through `R4-04`.

### D. Execution Result at `2a556719...`

One row per ten original repair issues. Each row records expected repair, actual `NOT EXECUTED`, relevant path and remote validation evidence. Do not aggregate `R3-01~06`.

### E. Preserved Evidence

Include exactly these rows:

| Source | LF-normalized SHA-256 | Lines | Byte comparison |
|---|---|---:|---|
| `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | `0a58da55219ce134095c3a15881205124174b74994e47b58168a91c1f402c827` | 479 | PASS |
| `.ai-context/docs/experience/INDEX.md` | `e81a8e26866ba2db3347e998b79bba9833b189ef39e098f9bd023e045aa17241` | 55 | PASS |
| `.ai-context/project_graph/README.md` | `2e8ee6833d5cf672bc62118c41938436fcbc66ea24efad500838294b489a4677` | 448 | PASS |

Also record R2-03/04/06 preserved.

### F. Scope, Isolation and Dirty-State Evidence

Record exact four-file fallback and no denylist/business files. Record only:

```text
Verified recoverable pre-existing target facts: Control was deleted and Supplement was modified before R3 implementation. The R3 report did not preserve the remaining exact dirty path names; they are not reconstructable from remote Git and are not invented here. R4 uses an isolated clean worktree and leaves the original worktree untouched.
```

Do not claim an exact historical dirty list.

### G. Model Ledger

Record the R3 row, final hash backfill authorization and ARCH assessment from §14.1.

### H. Issue Register and Historical Status

List `R3-EXEC-01`, `R4-01` through `R4-04`; state the R3 commit remains a valid blocked review input but not an accepted repair.

### I. Transition Gate

State R4 is the only next repair and M1 remains unauthorized.

## 16. Create R4 execution report and final State

Create `<R4_REPORT>` with header:

```text
# UBF-M0-REWORK-04 Execution Report

Document Role: Repository-carried Mechanical Execution and Remote Review Evidence
Task ID: UBF-M0-REWORK-04
Blueprint Revision: R1
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Execution Worktree Mode: ISOLATED_DETACHED_CLEAN
Package Profile: FULL
CookBook Legacy Granularity: L7
Execution Model: GPT-5.6 Luna
Handoff Parent: 2a5567193c688bbd0e30f323699a68aab1ffeb34
Execution Parent / Turn Claim Commit: <CLAIM_COMMIT>
Expected Return TURN: REVIEW
Report State: READY_FOR_COMMIT
Outcome: COMPLETE | PARTIAL | BLOCKED_FOR_REVIEW
```

Required sections:

- `A. Overall UBF Status`;
- `B. Isolation, Preflight and Turn Claim`;
- `C. Architecture Disposition`, one row per §2 issue;
- `D. Original Ten-Issue Repair Result`, one row per issue;
- `E. R3 Evidence Correction Result`, one row per R3-EXEC-01/R4-01~04;
- `F. Preserved Evidence`, including three fixed hashes;
- `G. Scope, Privacy and Validation`;
- `H. Model Execution Ledger`;
- `I. New Issue Register`, `NONE` only when none;
- `J. Outcome and Transition Gate`.

Include exactly:

```text
This report is created before its containing commit and push. It does not claim
its own commit hash or completed remote publication. The remote architecture
reviewer must verify the final commit, its parent, its file list, TURN return,
content integrity, model-ledger entries, original-worktree isolation, and
origin/master using the user-supplied commit hash.
```

Before staging, update current R4 State:

- `<OUTCOME> / PENDING REMOTE ARCH REVIEW`;
- `TURN=REVIEW`;
- abstract CODE/ARCH identities;
- full Handoff Parent and full `<CLAIM_COMMIT>` Execution Parent;
- evidence paths matching selected outcome;
- unresolved new execution Issue IDs or `NONE`;
- UBF Stage `M0 / AWAITING REMOTE ARCH REVIEW`;
- next: remote review R4; ACCEPT leads only to separate End/Accept+Handoff persistence, its review, then separate M1 Preview/Start; REWORK leads to narrow repair; M1 and Phase 3B remain unauthorized now.

## 17. Validation, outcome, final commit and push

One whitespace-only cleanup pass is authorized on the nine normal task-owned files. Do not alter characters inside Supplement §D.

Run and record:

```text
git diff --check
git status --short --untracked-files=all
git diff --name-status
git diff --stat
```

Required content gates:

- isolated worktree contains no unrelated/pre-existing changes;
- Truth Pack zero diff;
- R3 blueprint zero diff, 690 lines, fixed hash;
- Supplement §D fixed hashes/lines and byte comparisons PASS;
- R2 blueprint 479 lines and fixed hash PASS;
- R2 report has all eight individual results and corrected status;
- R3 report has all ten individual repair results, five R3/R4 review issues, three hashes and truthful historical dirty-evidence limitation;
- Control has valid UTF-8 19-rule R4 contract, seven-rule model contract and R4 current status;
- Supplement metadata, hashes, §G–§I and nine-file integrity list match R4;
- ledger changes are exactly R3 backfill plus one R4 append;
- State has current R4, immediately previous R3, and no concrete model name;
- all changed artifacts identify repository only as cookbook and contain no full endpoint;
- no secret/private-key/credential or real absolute user-home value;
- no denylisted file changed;
- R4 report has no unresolved placeholders except its explicit pre-commit self-reference statement.

### 17.1 COMPLETE

Use only if all ten original repairs, all five R3/R4 evidence corrections and all gates pass. Stage exactly nine normal files.

```text
docs(governance): close isolated UBF M0 R4 repair
```

### 17.2 PARTIAL

Use if all nine artifacts are safe and attributable but a non-security gate remains unresolved. Record stable new Issue IDs and stage exactly nine files.

```text
docs(governance): publish partial UBF M0 R4 review input
```

### 17.3 BLOCKED_FOR_REVIEW

Use only if target repairs should not enter the final commit but R4 blueprint/report, ledger and State are safely isolated. Stage exactly four fallback files and list all unstaged task-owned edits.

```text
docs(governance): publish blocked UBF M0 R4 review input
```

For the selected outcome, stage only the exact allowlist and require:

```text
git diff --cached --name-status
git diff --cached --stat
git diff --cached --check
```

Commit once with the selected message. Verify:

- direct parent is `<CLAIM_COMMIT>`;
- final file list exactly matches selected allowlist;
- claim direct parent is the Handoff Parent and claim changed only State;
- no unrelated path entered;
- original worktree was not modified by this task.

Push normally:

```text
git push origin HEAD:master
```

Verify remote `master` equals local detached HEAD. Do not amend, create a report-finalization commit, remove the isolated worktree, or start a follow-on task.

## 18. NON_PUBLISHABLE_STOP

Only these conditions may prevent a remote review-input push:

- credentials, token, private key or unredacted sensitive content exists in the proposed commit or unpushed ancestor;
- an allowlist/scope violation in an unpushed ancestor cannot be isolated by fallback;
- remote advanced, parent/remote identity differs, or push is not fast-forward;
- network, permission or remote-service failure prevents normal push;
- Git evidence is insufficient to prove proposed commit contents;
- an isolated clean worktree cannot be created without touching existing user files.

Return only:

```text
Task: UBF-M0-REWORK-04
Result: NON_PUBLISHABLE_STOP
Failure stage: <stage>
Handoff Parent: 2a5567193c688bbd0e30f323699a68aab1ffeb34
Claim commit: <hash-or-NONE>
Local HEAD: <hash>
Remote master: <hash-or-UNKNOWN>
Current TURN: <value>
Reason: <minimal redacted fact>
```

Do not start any follow-on task after push or STOP.
