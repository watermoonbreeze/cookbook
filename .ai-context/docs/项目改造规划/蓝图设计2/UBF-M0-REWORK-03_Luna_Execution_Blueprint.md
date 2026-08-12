# UBF-M0-REWORK-03 — M0 Governance Evidence and Status Repair

> Document Role: Luna Mechanical Execution Blueprint
> Status: `READY FOR EXECUTION`
> Task ID: `UBF-M0-REWORK-03`
> Blueprint Revision: `R1`
> Review Operation Mode: `REMOTE_READ_ONLY_ARCH`
> Package Profile: `FULL`
> CookBook Legacy Granularity: `L7`
> Handoff Parent: `c3c7b812272344935f2bb48f96a890d84081b5d3`
> Execution Parent: `CLAIM_COMMIT_RESOLVED_AT_RUNTIME`
> Expected Initial TURN: `REVIEW`
> Execution TURN: `CODE`
> Return TURN: `REVIEW`
> Turn Transfer Actor: `CODE_DELEGATED_CLAIM`
> Target Branch: `master`
> Target Repository: `cookbook`
> User-designated Execution Model: `GPT-5.6 Luna`
> Date: `2026-08-12`

## 0. How to execute

Read this entire document in a fresh context before acting. This document is the only execution authority for this batch.

This is a narrow governance-document REWORK. It does not authorize architecture redesign, UBF-M1, CookBook Phase 3B, production code, tests, build files, Project Graph lifecycle changes or user-level protocol changes.

Repository naming contract:

- identify the target only as `cookbook` or `the current cookbook repository`;
- do not record a hosting provider, account owner, organization or namespace;
- use the current worktree's configured `origin` without changing it;
- `origin/master` is a Git ref and is permitted;
- do not copy the full origin URL into committed documents or chat output.

Remote-review visibility contract:

- after a successful claim push, every safely publishable result must be committed and pushed;
- `COMPLETE`, `PARTIAL` and `BLOCKED_FOR_REVIEW` mean review input, not architecture acceptance;
- an unanticipated issue is recorded with a stable ID and returned for architecture disposition; it is not silently repaired;
- only §17 `NON_PUBLISHABLE_STOP` may leave the task without a remote review-input push.

On a successful final push, reply only:

```text
COMMIT_HASH: <40-character final remote commit hash>
```

## 1. UBF overall status and authorized next step

The following status is part of the execution contract and must be reflected consistently in `<CONTROL>`, `<STATE>`, `<R2_REPORT>` and `<R3_REPORT>`:

| Layer | Current state at Handoff Parent | Meaning | Authorized next action |
|---|---|---|---|
| Universal Blueprint Framework implementation | `M0 — Migration Control & Truth Lock` | M0 evidence exists but has not passed architecture acceptance | Execute only `UBF-M0-REWORK-03` |
| M0 remote review target | `c3c7b812272344935f2bb48f96a890d84081b5d3` | `PARTIAL / REWORK REQUIRED` | Repair the verified document, provenance, status and report defects in this blueprint |
| Accepted M0 sub-results | Embedded sources 479/55/448 byte-equal; Truth Pack §A/§J consistent; whitespace gate clean; delegated claim and TURN return valid | Preserve these results; do not rebuild or reinterpret them | Revalidate only; no semantic rewrite |
| M0 completion | `NOT ACCEPTED` | `COMPLETE / PENDING REMOTE ARCH REVIEW` is not an ACCEPT decision | After R3 push, wait for independent remote architecture review |
| M0 → M1 transition | `NOT AUTHORIZED` | M1 cannot start from a coder self-assessment | Only after R3 `ACCEPT`, architecture issues a separate M0 End/Accept + M0→M1 Handoff persistence blueprint; after that handoff is persisted and reviewed, M1 gets a separate Preview/Start |
| CookBook Project Graph Phase 3A | `EXECUTED / REWORK REQUIRED / PAUSED` | Separate lifecycle from UBF M0 | Do not change in this batch |
| CookBook Project Graph Phase 3B | `NOT AUTHORIZED TO START` | Separate lifecycle from UBF M0 | Do not start or modify |

The required sequence is:

```text
M0-REWORK-03 execution
→ remote architecture review
→ if REWORK: issue the next narrow repair blueprint
→ if ACCEPT: issue and execute a separate M0 End/Accept + M0→M1 Handoff persistence batch
→ review the persisted handoff
→ separate M1 Preview/Start
```

No step after remote review is automatic.

## 2. Remote review disposition ledger

Review target:

```text
c3c7b812272344935f2bb48f96a890d84081b5d3
```

Every prior Issue ID receives exactly one architecture disposition:

| Issue ID | Verified result at `c3c7b812` | Disposition | R3 execution boundary |
|---|---|---|---|
| `UBF-M0-R2-01` | Three embedded bodies are 479/55/448 lines and byte-equal to the claim-commit Git blobs; truncation marker removed | `ACCEPT_AS_IS` | Do not rewrite embedded bodies; revalidate only |
| `UBF-M0-R2-02` | Supplement still asserts `COMPLETE` and `NONE` while metadata and integrity ledger are stale | `REPAIR` | Repair only the Supplement metadata, assessment and integrity sections defined below |
| `UBF-M0-R2-03` | Truth Pack §A and §J both say `PARTIAL / SUPERSEDED IN PART` | `ACCEPT_AS_IS` | Do not modify Truth Pack |
| `UBF-M0-R2-04` | Final R2 diff passes whitespace validation | `ACCEPT_AS_IS` | Preserve; run final whitespace validation |
| `UBF-M0-R2-05` | R2 report exists but says Revision R1, does not give per-Issue actual evidence, and falsely says every issue closed | `REPAIR` | Replace R2 report with the remote-review correction contract in §11 |
| `UBF-M0-R2-06` | `2f4fcb790c9aae2373055b933ead6c64feea1876` is a valid one-file claim commit and `c3c7b812272344935f2bb48f96a890d84081b5d3` returns TURN to REVIEW | `ACCEPT_AS_IS` | Preserve history; perform a new R3 claim/return transaction |
| `UBF-M0-R2-07` | Control has an R4 subsection, but it is encoding-corrupted and omits required repository naming/endpoint clauses | `REPAIR` | Replace only the corrupted subsection and update current-stage metadata |
| `UBF-M0-R2-08` | §D hash basis is correct, but Supplement §C/§E still use stale unlabeled hashes and external-state wording | `REPAIR` | Normalize hash-basis labels and specified values; do not modify source files |

New defects discovered by remote review:

| Issue ID | Verified defect | Disposition | Exact result required |
|---|---|---|---|
| `UBF-M0-R3-01` | Repository copy of REWORK-02 is stale R1 and contains a concrete repository identity | `REPAIR` | Apply the exact R1→R2 patch in §8 and verify the fixed LF SHA-256 |
| `UBF-M0-R3-02` | Supplement lacks R2/R3 repair provenance, current Handoff Parent and accurate eight-file integrity list | `REPAIR` | Apply §9 and §10 exactly |
| `UBF-M0-R3-03` | R2 report contains concrete repository identities and material self-assessment claims contradicted by remote review | `REPAIR` | Replace the report using §11; retain it as corrected historical R2 evidence |
| `UBF-M0-R3-04` | `BLUEPRINT_STATE.md` says R2 `COMPLETE` with `NONE` despite architecture REWORK decision | `REPAIR` | Preserve R2 as previous `PARTIAL / REMOTE ARCH REVIEWED / REWORK REQUIRED`; create current R3 record |
| `UBF-M0-R3-05` | Current UBF stage and next transition are not explicit in repository control artifacts | `REPAIR` | Record the §1 state and post-R3 handoff gate in Control, State and R3 report |
| `UBF-M0-R3-06` | Current UBF governance batches are absent from the model execution capability ledger | `REPAIR` | Append exactly one R3 CODE row using §12 and add the future-batch evidence contract in §13; do not alter historical rows |

Preserve Set:

- Git history through `c3c7b812272344935f2bb48f96a890d84081b5d3`;
- Truth Pack in full;
- all three embedded source bodies in Supplement §D;
- all canonical GC/protocol/Project Graph/Phase state source files;
- all pre-existing model capability ledger rows;
- all unrelated dirty working-tree entries;
- accepted R2 turn-claim and TURN-return evidence;
- Phase 3A, Phase 3B and `GOV-BP-P3-01` source semantics.

## 3. Paths and allowlists

```text
<SUPPLEMENT> = .ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md
<CONTROL> = .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md
<R2_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
<R2_REPORT> = .ai-context/docs/UBF-M0-REWORK-02-Execution-Report.md
<R3_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-03_Luna_Execution_Blueprint.md
<R3_REPORT> = .ai-context/docs/UBF-M0-REWORK-03-Execution-Report.md
<MODEL_LEDGER> = .ai-context/docs/experience/14_模型执行力评估.md
<STATE> = .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Turn-claim commit allowlist, exactly one file:

```text
M  <STATE>
```

Normal `COMPLETE` or publishable `PARTIAL` delivery allowlist, exactly eight files:

```text
M  <SUPPLEMENT>
M  <CONTROL>
M  <R2_BLUEPRINT>
M  <R2_REPORT>
A  <R3_BLUEPRINT>
A  <R3_REPORT>
M  <MODEL_LEDGER>
M  <STATE>
```

`BLOCKED_FOR_REVIEW` fallback allowlist, exactly four files:

```text
A  <R3_BLUEPRINT>
A  <R3_REPORT>
M  <MODEL_LEDGER>
M  <STATE>
```

In fallback, task-owned edits to other paths remain unstaged and are listed in `<R3_REPORT>`.

## 4. Denylist

Do not modify or stage:

- `.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`;
- user-level `GLOBAL.md`, `MODEL_ROUTING.md` or `rules/blueprint_protocol.md`;
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`;
- `.ai-context/docs/experience/INDEX.md`;
- `.ai-context/project_graph/README.md`;
- `project.yaml`, Phase 3 migration/control/audit/acceptance/handoff files or generated views;
- any production code, test, build, dependency or configuration file;
- any file outside the selected outcome allowlist.

Do not run `pull`, `merge`, `rebase`, `reset`, `clean`, `stash`, amend, force push or history rewrite.

## 5. Preflight

Before writing, run and record the redacted/sanitized results in `<R3_REPORT>`:

```text
git rev-parse --show-toplevel
git rev-parse HEAD
git branch --show-current
git remote get-url origin
git ls-remote origin refs/heads/master
git status --short --untracked-files=all
git diff --cached --name-status
git show HEAD:<STATE>
git show --format=fuller --stat --name-status c3c7b812272344935f2bb48f96a890d84081b5d3
git rev-list --parents -n 3 c3c7b812272344935f2bb48f96a890d84081b5d3
git diff-tree --check 2f4fcb790c9aae2373055b933ead6c64feea1876 c3c7b812272344935f2bb48f96a890d84081b5d3
```

Use `git remote get-url origin` only for local validation. The report records exactly:

```text
Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED
```

All must be true:

- branch is `master`;
- local HEAD and remote `master` both equal the Handoff Parent;
- the final remote path component, ignoring case and optional `.git`, is `cookbook`;
- index is empty;
- all six existing normal-delivery targets have no pre-existing unstaged changes;
- `<R3_BLUEPRINT>` and `<R3_REPORT>` do not already exist or appear untracked;
- `<STATE>` says `TURN=REVIEW`, `CODE=Coder@当前机`, `ARCH=架构师@主力机`;
- R2 state at the Handoff Parent says `COMPLETE / PENDING REMOTE ARCH REVIEW` and `NONE`, which is the verified defect to repair, not a preflight reason to STOP;
- `c3c7b812272344935f2bb48f96a890d84081b5d3` has parent `2f4fcb790c9aae2373055b933ead6c64feea1876`; that parent has parent `b46b9dfe4d2328aeae6f2f244d7ba0a023eee402`;
- `2f4fcb790c9aae2373055b933ead6c64feea1876` changes only `<STATE>`;
- `c3c7b812272344935f2bb48f96a890d84081b5d3` changes exactly the six R2 delivery files verified by remote review;
- no preflight staged file exists.

Unrelated dirty/untracked entries are allowed. Record path names only, do not read, modify or stage them.

Use `NON_PUBLISHABLE_STOP` if branch, remote, parent, TURN, index ownership or file attribution cannot be proved.

## 6. Delegated turn claim

Update only `<STATE>`:

- create current batch `UBF-M0-REWORK-03 — M0 Governance Evidence and Status Repair`;
- preserve R2 as the immediately previous batch;
- change the previous R2 status to `PARTIAL / REMOTE ARCH REVIEWED / REWORK REQUIRED`;
- change previous R2 unresolved issues from `NONE` to `UBF-M0-R2-02, UBF-M0-R2-05, UBF-M0-R2-07, UBF-M0-R2-08, UBF-M0-R3-01 ~ UBF-M0-R3-06`;
- current R3 state: `AUTHORIZED / IN PROGRESS`;
- `TURN=CODE`;
- `CODE=Coder@当前机` and `ARCH=架构师@主力机`;
- Review mode: `REMOTE_READ_ONLY_ARCH`;
- Handoff Parent: full `c3c7b812272344935f2bb48f96a890d84081b5d3`;
- Execution Parent: `PENDING CLAIM COMMIT`;
- scope: all Issue IDs in §2;
- UBF stage: `M0 / REWORK BEFORE ACCEPTANCE`;
- next: execute only R3, then return to remote review; M1 and Phase 3B remain unauthorized.

Do not write a concrete model name into `<STATE>`.

Stage only `<STATE>`, require one `M` entry and `git diff --cached --check` PASS.

Commit message:

```text
chore(governance): claim UBF-M0-REWORK-03
```

The claim commit must be a direct child of the Handoff Parent and change only `<STATE>`. Push it normally, verify `origin/master` equals it, and record its full hash as `<CLAIM_COMMIT>`. Continue only after remote verification.

## 7. Preserve this R3 blueprint

Save the exact user-provided contents of this document at `<R3_BLUEPRINT>`. Do not summarize, rewrite or alter its metadata.

## 8. Restore the official REWORK-02 R2 blueprint

Modify `<R2_BLUEPRINT>` only with the following exact transformations:

1. `Blueprint Revision: R1` → `Blueprint Revision: R2`.
2. Replace the existing concrete repository-identity value on the `Target Repository` line with `cookbook`; do not reproduce the old value elsewhere.
3. After the five bullets ending with `NON_PUBLISHABLE_STOP`, insert the repository naming contract from §0 of this R3 blueprint, preserving the R2 wording where it says `this and all generated artifacts in this batch`.
4. In R2 §4, state that `git remote get-url origin` is used only locally, and the report uses the sanitized identity line.
5. Replace the R2 preflight provider/owner requirement with the final-path-component `cookbook` test from §5.
6. Add R2 Control clauses 13–19 exactly as represented by §10 items 13–19 below.
7. In the R2 report template, change `Blueprint Revision: R1` to `Blueprint Revision: R2` and require the sanitized remote identity line.
8. Make no other R2 blueprint change.

After LF normalization, the official restored file must have:

```text
Line count: 479
SHA-256: c9d8274e4c4247394fa7dfa97bc85af07fb4db09a397ab5989568d4f819e92ec
```

If the prescribed transformations do not produce this exact result, do not improvise. Register `UBF-M0-R3-EXEC-01` and select `PARTIAL` or `BLOCKED_FOR_REVIEW` according to whether the file is safe to publish.

## 9. Repair Supplement provenance and current status

Do not alter Supplement §D or any text inside its three four-tilde bodies.

Replace the header metadata with this field set, resolving `<CLAIM_COMMIT>` and `<OUTCOME>` at runtime:

```text
Document Role: Supplemental Evidence and Errata
Status: <OUTCOME> / PENDING REMOTE ARCH REVIEW
Original Capture HEAD: b7fc77e4d442364e6f5db790b374ece4c5da409d
Prior Repair Task: UBF-M0-REWORK-02
Current Repair Task: UBF-M0-REWORK-03
R2 Handoff Parent: b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
R3 Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
R3 Execution Parent: <CLAIM_COMMIT>
```

In §A:

- identify `c3c7b812272344935f2bb48f96a890d84081b5d3` as `PARTIAL / REWORK REQUIRED`, not accepted delivery;
- state the R3 allowlist as the eight normal files or actual outcome list;
- keep the canonical source denylist and no-source-mutation rule;
- use repository-relative paths and placeholders only;
- add that §D source bodies were accepted by remote review and are preserved byte-for-byte.

In §C:

- rename `SHA-256` to `Repository LF-normalized SHA-256`;
- use exactly these values and retain line counts:

| Path | LF-normalized SHA-256 | Lines |
|---|---|---:|
| `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | `0a58da55219ce134095c3a15881205124174b74994e47b58168a91c1f402c827` | 479 |
| `.ai-context/docs/experience/INDEX.md` | `e81a8e26866ba2db3347e998b79bba9833b189ef39e098f9bd023e045aa17241` | 55 |
| `.ai-context/project_graph/README.md` | `2e8ee6833d5cf672bc62118c41938436fcbc66ea24efad500838294b489a4677` | 448 |

In §E:

- rename the hash column to `Repository LF-normalized SHA-256 / Evidence Basis`;
- use the b46 evidence snapshot values below, without changing the source files:

| Source | Required value/basis |
|---|---|
| `.ai-context/project_graph/project.yaml` | `2c756ce240c129e72276d7a97842c953580c006b768227bb06c086c270ca2f0f` |
| `.ai-context/project_graph/migration/PHASE3A_AUDIT.md` | `c78dfbcb08b35ffbc26e165d139cd3929c9aae895bcc70e3acddb40fcb215a52` |
| `.ai-context/project_graph/migration/PHASE3A_BLUEPRINT.md` | `7269fab1a893212fce068367454051ae26c0d643452fac41b19b926c1c8b265b` |
| `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` at `b46b9dfe4d2328aeae6f2f244d7ba0a023eee402` | `8933b7d85d382f4bde8a8077db4359514833141a0d70c62efbfaf5785ce56109` |
| `<USER_HOME>/.ai-context/rules/blueprint_protocol.md` | `REMOTE_ATTESTED_EXTERNAL_STATE / NOT DIRECTLY REPRODUCIBLE FROM REPOSITORY`; retain the prior externally attested hash |

Rows that reuse one of these sources must reuse the same value and basis. Do not retain the old Windows-worktree hashes in §C/§E.

## 10. Repair Supplement completion and integrity

Replace stale §G–§I execution claims so they describe R3 rather than the original two-file repair:

- §G: Handoff Parent is `c3c7b812272344935f2bb48f96a890d84081b5d3`; Execution Parent is `<CLAIM_COMMIT>`; list the actual pre-existing dirty paths and preserve them; identify the selected outcome allowlist;
- §H: preserve the statement that the three embedded bodies are complete only because byte comparison passes; state that UBF overall remains in M0 and cannot enter M1 until R3 receives architecture ACCEPT;
- §I: list the exact eight normal-delivery files or the exact four fallback files; list unresolved R3 execution Issue IDs or `NONE`; state `M0 transition: AWAITING REMOTE ARCH REVIEW`;
- end the file with exactly one newline after the final nonblank line.

The normal eight-file list must be:

```text
M .ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md
M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md
M .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
M .ai-context/docs/UBF-M0-REWORK-02-Execution-Report.md
A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-03_Luna_Execution_Blueprint.md
A .ai-context/docs/UBF-M0-REWORK-03-Execution-Report.md
M .ai-context/docs/experience/14_模型执行力评估.md
M .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Supplement may use `COMPLETE / PENDING REMOTE ARCH REVIEW` only if every R3 content and scope gate passes. Otherwise use the selected `PARTIAL` or `BLOCKED_FOR_REVIEW` state and list Issue IDs.

## 11. Replace the R2 report with corrected remote-review evidence

Replace `<R2_REPORT>` in full. It must remain an R2 historical execution report and must not claim that R3 work existed in the R2 commit.

Required header:

```text
# UBF-M0-REWORK-02 Execution Report

Document Role: Repository-carried Mechanical Execution and Corrected Remote Review Evidence
Task ID: UBF-M0-REWORK-02
Blueprint Revision Executed: R1
Authoritative Blueprint Revision Intended: R2
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Handoff Parent: b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
Execution Parent / Turn Claim Commit: 2f4fcb790c9aae2373055b933ead6c64feea1876
Reviewed Delivery Commit: c3c7b812272344935f2bb48f96a890d84081b5d3
Expected Return TURN: REVIEW
Report Correction Task: UBF-M0-REWORK-03
Outcome at Reviewed Delivery: PARTIAL / REMOTE ARCH REVIEWED / REWORK REQUIRED
```

Required sections and exact facts:

### A. Preflight and Turn Claim

- sanitized remote line only;
- branch `master`;
- Handoff Parent, claim hash, claim parent and one-file claim list;
- claim remote verification and final `TURN=REVIEW` evidence;
- no provider, owner, account, namespace or full endpoint.

### B. Architecture Disposition

List all R2 issues with the dispositions from §2 of this blueprint.

### C. Verified Result at `c3c7b812272344935f2bb48f96a890d84081b5d3`

Record one row per R2 issue:

| Issue | Actual result |
|---|---|
| R2-01 | PASS — 479/55/448 and byte-equal; no truncation marker |
| R2-02 | FAIL — Supplement completion and unresolved-issue statements were not truthful |
| R2-03 | PASS — Truth Pack §A/§J consistent |
| R2-04 | PASS — whitespace check clean |
| R2-05 | FAIL — report existed but did not contain per-Issue actual evidence and used wrong revision |
| R2-06 | PASS — separate claim and final return to REVIEW verified |
| R2-07 | FAIL — R4 text encoding-corrupted and contract incomplete |
| R2-08 | FAIL — §C/§E hash basis and external-state labels remained stale |

### D. Embedded Source Integrity

Retain the three exact path/hash/line/byte-compare PASS rows already verified.

### E. Scope and Privacy

Record that the delivery changed the expected six R2 files and no business/denylist files; use only sanitized remote identity.

### F. Issues Opened by Remote Review

List `UBF-M0-R3-01` through `UBF-M0-R3-06` with the verified facts in §2. Do not say `NONE`.

### G. Historical Status

State exactly:

```text
The R2 delivery is preserved in Git history as a PARTIAL remote review input.
This corrected report does not rewrite that commit and does not convert it into
an accepted result. Repair authority is limited to UBF-M0-REWORK-03. UBF-M1
remains unauthorized until M0 receives remote architecture ACCEPT, a separate
End/Accept plus M0-to-M1 handoff batch is persisted, and that handoff is reviewed.
```

## 12. Update the model execution capability ledger

Modify only `<MODEL_LEDGER>` by appending one row to `## 评估台账`. Do not edit or reformat existing rows and do not write a model conclusion.

Use exactly these fixed identity fields:

```text
批次: UBF-M0-REWORK-03 M0 Governance Evidence and Status Repair
角色: CODE
实际模型: GPT-5.6 Luna
commit: 待 ARCH 依据最终远程 commit hash 回填
```

The task-complexity cell must include:

```text
治理文档窄范围 REWORK；8 文件正常 allowlist；REMOTE_READ_ONLY_ARCH 双提交事务；Package=FULL；CookBook Legacy Granularity=L7；无生产代码
```

The one-delivery-result cell must be filled truthfully before commit with:

- selected outcome;
- number of known issues closed out of the ten repair-required rows in §2 (`R2-02/05/07/08` plus `R3-01~06`);
- claim push and TURN return result;
- exact staged file count;
- validation PASS/FAIL summary;
- new Issue IDs or `NONE`;
- no architecture capability conclusion.

The ARCH comment cell must be:

```text
待 REMOTE ARCH 独立复核；本行只记录单批执行事实，不构成模型能力或路由结论。
```

If the runtime environment exposes a more precise model build or reasoning-effort label, append it in parentheses after `GPT-5.6 Luna`; never replace the user-designated base name. Do not put the model name into `<STATE>`.

## 13. Repair Control and make the current stage explicit

Update `<CONTROL>` header to:

```text
状态: M0 REWORK-03 IN EXECUTION / PENDING REMOTE ARCH REVIEW
Current UBF Stage: M0 — Migration Control & Truth Lock
Current Review Result: c3c7b812272344935f2bb48f96a890d84081b5d3 = PARTIAL / REWORK REQUIRED
Current Repository Observation / Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
CookBook Project Graph: Phase 3A EXECUTED / REWORK REQUIRED / PAUSED; Phase 3B NOT AUTHORIZED TO START
Process Revision: R4 — Remote Review Visibility & Issue Disposition Contract
```

Replace the corrupted R4 subsection, from its `### 7.1 R4` heading through immediately before `## 8`, with a valid UTF-8 subsection containing these 19 numbered rules:

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

End the subsection with:

```text
This R4 subsection supersedes conflicting failure-report and local-only STOP wording in older §§6–7 for all future REMOTE_READ_ONLY_ARCH writable batches.
```

There must be no replacement character, mojibake sequence or malformed arrow/section symbol in the replacement.

Update `## 10. 当前启动判定` so it states:

- M0 is authorized and remains the current UBF stage;
- `c3c7b812272344935f2bb48f96a890d84081b5d3` was remotely reviewed as `PARTIAL / REWORK REQUIRED`;
- this R3 is a repair step, not continuation to M1;
- R3 push must receive remote architecture `ACCEPT` before M0 completion;
- after ACCEPT, architecture issues a separate M0 End/Accept + M0→M1 Handoff persistence blueprint; only after its execution and review may a separate M1 Preview/Start be issued;
- a REWORK result produces another narrow repair blueprint;
- Phase 3B and production code remain outside this UBF task.

Immediately before `## 8`, after the repaired R4 subsection, add:

```text
### 7.2 Model Execution Capability Evidence Contract
```

It must state:

1. Every writable CODE blueprint declares the actual execution model and includes `.ai-context/docs/experience/14_模型执行力评估.md` in its normal delivery allowlist.
2. The current Coder appends exactly one task row with Task ID, CODE role, actual model, task family/complexity, package profile, blueprint granularity, outcome, rework/STOP facts and validation summary.
3. Concrete model names belong only in the capability ledger and execution report; `BLUEPRINT_STATE.md` keeps abstract role+machine identifiers.
4. Because the final commit does not exist when its row is written, the Coder records `待 ARCH 依据最终远程 commit hash 回填` and must not amend or add a follow-up commit merely to self-fill it.
5. Remote architecture review verifies the final hash and produces the ARCH assessment; the next writable architecture task explicitly authorizes backfilling the previous row's commit and ARCH comment.
6. Historical rows are append-preserved; a current task may edit a prior row only with an explicit architecture backfill instruction naming that row and verified commit.
7. A single batch is evidence, not a model-routing conclusion; the ledger's minimum-sample rule remains authoritative.

Do not rewrite historical M0 sections or redefine M1–M8.

## 14. Create the R3 execution report

Create `<R3_REPORT>` with this header:

```text
# UBF-M0-REWORK-03 Execution Report

Document Role: Repository-carried Mechanical Execution and Remote Review Evidence
Task ID: UBF-M0-REWORK-03
Blueprint Revision: R1
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Package Profile: FULL
CookBook Legacy Granularity: L7
Execution Model: GPT-5.6 Luna
Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
Execution Parent / Turn Claim Commit: <CLAIM_COMMIT>
Expected Return TURN: REVIEW
Report State: READY_FOR_COMMIT
Outcome: COMPLETE | PARTIAL | BLOCKED_FOR_REVIEW
```

Required sections:

- `A. Overall UBF Status`: reproduce §1 accurately and state this is M0 repair, not M1;
- `B. Preflight and Turn Claim`: branch, sanitized remote identity, parents, initial TURN, claim hash/file list/push verification;
- `C. Architecture Disposition`: every R2 and R3 Issue ID from §2 with disposition;
- `D. Execution Result`: per repair-required Issue ID expected, actual, path/line and validation evidence;
- `E. Preserved Evidence`: R2-01/03/04/06 revalidation, including three source hashes/lines/byte compares;
- `F. Scope and Privacy`: dirty paths, actual modified/staged paths, allowlist/denylist, secrets and user-path scans;
- `G. Model Execution Ledger`: exact model string, task family, FULL/L7, row location and truthful pre-commit result;
- `H. Issue Register`: every newly discovered unresolved issue in the R4 schema; `NONE` only when none exist;
- `I. Outcome and Transition Gate`: selected outcome/allowlist/commit message/Return TURN and next-step gate.

Include exactly:

```text
This report is created before its containing commit and push. It does not claim
its own commit hash or completed remote publication. The remote architecture
reviewer must verify the final commit, its parent, its file list, TURN return,
content integrity, model-ledger entry, and origin/master using the user-supplied
commit hash.
```

## 15. Validation

One mechanical whitespace pass is authorized on the eight normal task-owned files: remove trailing spaces/tabs and blank lines after the final nonblank line, leaving exactly one final newline. Do not alter characters inside Supplement §D embedded bodies.

Run and record:

```text
git diff --check
git status --short --untracked-files=all
git diff --name-status
git diff --stat
```

Content gates:

- Truth Pack has zero diff;
- Supplement §D bodies match the claim-commit blobs after LF normalization with 479/55/448 lines and the three fixed hashes;
- restored R2 blueprint is exactly 479 lines and has SHA-256 `c9d8274e4c4247394fa7dfa97bc85af07fb4db09a397ab5989568d4f819e92ec` after LF normalization;
- no concrete provider/owner/namespace or full origin URL appears in either blueprint, either report, Supplement or new Control subsection;
- Control R4 contains all 19 rules, three outcomes, four dispositions, repository naming contract and precedence sentence;
- no mojibake or Unicode replacement character appears in changed files;
- R2 report outcome is `PARTIAL / REMOTE ARCH REVIEWED / REWORK REQUIRED` and contains all eight actual results;
- Supplement header, §A, §C, §E, §G–§I match this blueprint;
- Supplement and R3 report both state current UBF stage M0 and the R3 ACCEPT → separate End/Accept+Handoff persistence batch → handoff review → separate M1 Preview/Start sequence;
- State previous R2 record is REWORK and current R3 record is truthful;
- model ledger has exactly one appended R3 row, actual model `GPT-5.6 Luna`, FULL/L7 task metadata, truthful result and no new boundary conclusion;
- State contains no concrete model name;
- secret/credential/private-key scan has zero suspected values;
- real absolute user-home path scan has zero values;
- user-level full text is absent;
- no denylisted file changed;
- R3 report has no unresolved placeholders except its explicit self-referential commit/push statement.

If a known exact repair fails, do not invent a broader repair. Register `UBF-M0-R3-EXEC-NN` and select the applicable publishable outcome.

## 16. Outcome, return TURN, commit and push

### 16.1 COMPLETE

Use only if every content and scope gate passes. Stage exactly eight normal files.

```text
docs(governance): repair UBF M0 status and evidence
```

### 16.2 PARTIAL

Use when all eight artifacts are safe and attributable but a non-security gate remains unresolved. Record Issue IDs, stage exactly eight normal files.

```text
docs(governance): publish partial UBF M0 R3 review input
```

### 16.3 BLOCKED_FOR_REVIEW

Use when target edits should not enter the delivery commit but R3 Blueprint, Report, Model Ledger and State can be safely isolated. Stage exactly four fallback files and list every unstaged task-owned edit.

```text
docs(governance): publish blocked UBF M0 R3 review input
```

Before staging, update the current R3 record in `<STATE>`:

- state `<OUTCOME> / PENDING REMOTE ARCH REVIEW`;
- `TURN=REVIEW`;
- preserve abstract `CODE` and `ARCH` identities;
- full Handoff Parent and `<CLAIM_COMMIT>` Execution Parent;
- evidence paths matching the selected outcome;
- unresolved Issue IDs or `NONE`;
- UBF stage `M0 / AWAITING REMOTE ARCH REVIEW`;
- next step: architecture reviews R3; if ACCEPT, issue a separate M0 End/Accept + M0→M1 Handoff persistence blueprint, execute and review that batch, then separately Preview/Start M1; if REWORK, issue a narrow repair; M1 and Phase 3B remain unauthorized now.

Stage only the selected allowlist. Run and require:

```text
git diff --cached --name-status
git diff --cached --stat
git diff --cached --check
```

Commit once with the selected message. Verify the direct parent is `<CLAIM_COMMIT>`, the file list exactly matches the selected allowlist, the claim remains the direct parent and changed only State, and no unrelated path entered.

Push normally:

```text
git push origin HEAD:master
```

Verify remote `master` equals local HEAD. Do not amend and do not create a report-finalization commit.

## 17. NON_PUBLISHABLE_STOP

Only these conditions may prevent a remote review-input push:

- credentials, token, private key or unredacted sensitive content exists in the proposed commit or an unpushed ancestor;
- an allowlist/scope violation in an unpushed ancestor cannot be isolated by fallback;
- remote advanced, branch/parent/remote identity differs, or push is not fast-forward;
- network, permission or remote-service failure prevents normal push;
- Git evidence is insufficient to prove the proposed commit contents.

Return only:

```text
Task: UBF-M0-REWORK-03
Result: NON_PUBLISHABLE_STOP
Failure stage: <stage>
Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
Claim commit: <hash-or-NONE>
Local HEAD: <hash>
Remote master: <hash-or-UNKNOWN>
Current TURN: <value>
Reason: <minimal redacted fact>
```

Do not start any follow-on task after push or STOP.
