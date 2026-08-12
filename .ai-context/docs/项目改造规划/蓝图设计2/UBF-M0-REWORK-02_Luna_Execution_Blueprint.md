# UBF-M0-REWORK-02 — Remote-visible Evidence Repair

> Document Role: Luna Mechanical Execution Blueprint
> Status: `READY FOR EXECUTION`
> Task ID: `UBF-M0-REWORK-02`
> Blueprint Revision: `R2`
> Review Operation Mode: `REMOTE_READ_ONLY_ARCH`
> Handoff Parent: `b46b9dfe4d2328aeae6f2f244d7ba0a023eee402`
> Execution Parent: `CLAIM_COMMIT_RESOLVED_AT_RUNTIME`
> Expected Initial TURN: `REVIEW`
> Execution TURN: `CODE`
> Return TURN: `REVIEW`
> Turn Transfer Actor: `CODE_DELEGATED_CLAIM`
> Target Branch: `master`
> Target Repository: `cookbook`
> Date: `2026-08-12`

## 0. How to execute

Read this entire document in a fresh context before acting. This document is the only execution authority for this batch.

This batch implements the `Remote Review Visibility & Issue Disposition Contract`:

- after a successful turn-claim push, every safely publishable outcome must be committed and pushed;
- `COMPLETE`, `PARTIAL`, and `BLOCKED_FOR_REVIEW` are review-input states, not architecture acceptance;
- unanticipated problems must be recorded in the repository-carried execution report, not silently repaired;
- the remote architecture reviewer decides `ACCEPT_AS_IS / REPAIR / DEFER / REJECT` in the next task document;
- only a `NON_PUBLISHABLE_STOP` condition may leave the result without a review-input push.

Repository naming contract for this and all generated artifacts in this batch:

- identify the target only as `cookbook` or `the current cookbook repository`;
- do not assert or record a hosting provider, account owner, organization or namespace identity;
- treat the currently configured `origin` as the operational endpoint without changing it;
- `origin/master` is permitted as a Git ref name and does not constitute a repository-identity claim;
- do not copy the full origin URL into committed documents or chat output.

On a successful push, reply only:

```text
COMMIT_HASH: <40-character final remote commit hash>
```

Do not start UBF-M1 or CookBook Phase 3B.

## 1. Review decision being executed

Remote review target:

```text
b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
```

Verified disposition ledger:

| Issue ID | Evidence at `b46b9dfe` | Disposition | Exact result required |
|---|---|---|---|
| `UBF-M0-R2-01` | Supplement line 211 contains a literal tool-output truncation marker; embedded GC source has 334 lines while the committed source has 479 | `REPAIR` | Rebuild all three embedded project files directly from committed Git blobs; GC body must contain 479/479 lines and no truncation marker |
| `UBF-M0-R2-02` | Supplement claims `COMPLETE`, complete original contents and `Unresolved Q/STOP: NONE` despite R2-01 | `REPAIR` | Make status and completion statements truthfully follow validation output |
| `UBF-M0-R2-03` | Truth Pack metadata says `PARTIAL`, but §J still says `Collection: COMPLETE` and `Unresolved STOP/Q: NONE` | `REPAIR` | Remove the internal contradiction while preserving original capture evidence |
| `UBF-M0-R2-04` | `git diff-tree --check b46b9dfe^ b46b9dfe` reports Supplement line 953 `new blank line at EOF` | `REPAIR` | Final cumulative diff must pass `git diff --check` |
| `UBF-M0-R2-05` | `b46b9dfe` changes only Truth Pack and Supplement; repository-carried execution report is absent | `REPAIR` | Add the fixed R2 execution report defined below; do not fabricate a retroactive R1 report |
| `UBF-M0-R2-06` | `b46b9dfe` is a direct child of `3e08ab9b`; no turn-claim commit exists and `BLUEPRINT_STATE.md` remains on `GOV-BP-P3-01`, `TURN=REVIEW` | `REPAIR` | Use a separate claim commit, then return `CODE → REVIEW` in the R2 delivery/review-input commit |
| `UBF-M0-R2-07` | Repository UBF control still contains the old single-path handoff loop | `REPAIR` | Add the R4 remote-review visibility and issue-disposition contract; make it explicitly supersede conflicting old §§6–7 rules |
| `UBF-M0-R2-08` | Supplement mixes Windows worktree hashes with remotely reproducible Git-blob evidence without naming the hash basis | `REPAIR` | For the three embedded files and project state evidence, record `Repository LF-normalized SHA-256`; label user-level protocol hash as remote-attested external state |

Preserve Set:

- Git history through `b46b9dfe`;
- original capture command outputs in the Truth Pack;
- all canonical GC, protocol, Project Graph, Phase 3 and business-code source files;
- all unrelated dirty working-tree entries;
- the two governance files that were correctly embedded in `b46b9dfe`: `experience/INDEX.md` and `project_graph/README.md`, except for deterministic fence/hash metadata reconstruction;
- Phase 3A, Phase 3B and `GOV-BP-P3-01` evidence semantics already recorded from source files.

## 2. Paths and allowlists

Resolve the repository root first. Use only repository-relative paths in committed documents.

Fixed paths:

```text
<TRUTH_PACK> = .ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md
<SUPPLEMENT> = .ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md
<CONTROL> = .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md
<BLUEPRINT_COPY> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
<REPORT> = .ai-context/docs/UBF-M0-REWORK-02-Execution-Report.md
<STATE> = .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Turn-claim commit allowlist, exactly one file:

```text
M  <STATE>
```

Normal `COMPLETE` or publishable `PARTIAL` delivery allowlist, exactly six files:

```text
M  <TRUTH_PACK>
M  <SUPPLEMENT>
M  <CONTROL>
A  <BLUEPRINT_COPY>
A  <REPORT>
M  <STATE>
```

`BLOCKED_FOR_REVIEW` fallback allowlist, exactly three files:

```text
A  <BLUEPRINT_COPY>
A  <REPORT>
M  <STATE>
```

In the fallback path, target-document edits may remain unstaged locally but must be listed in the report. Do not delete or hide them and do not include them in the fallback commit.

## 3. Denylist

Do not modify or stage:

- user-level `GLOBAL.md`, `MODEL_ROUTING.md` or `rules/blueprint_protocol.md`;
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`;
- `.ai-context/docs/experience/INDEX.md`;
- `.ai-context/project_graph/README.md`;
- `project.yaml` or any Project Graph migration/control/audit/acceptance/handoff file;
- any GC registry, Phase 3 state source, generated view, business code, test, build or configuration file;
- any file outside the active outcome allowlist.

Do not run `pull`, `merge`, `rebase`, `reset`, `clean`, `stash`, amend, force push or history rewrite.

## 4. Preflight

Before writing, run the following commands. Capture exact output for the report except for `git remote get-url origin`: use its result only for local validation and record the sanitized result specified below.

```text
git rev-parse --show-toplevel
git rev-parse HEAD
git branch --show-current
git remote get-url origin
git ls-remote origin refs/heads/master
git status --short --untracked-files=all
git diff --cached --name-status
git show HEAD:<STATE>
git show --format=fuller --stat --name-status b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
git rev-list --parents -n 2 b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
git diff-tree --check b46b9dfe4d2328aeae6f2f244d7ba0a023eee402^ b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
```

All must be true:

- branch is `master`;
- local HEAD and remote `master` both equal the Handoff Parent;
- `origin` is configured and its final repository path component, ignoring an optional `.git` suffix and case, is `cookbook`; do not require or assert any provider, owner, organization or namespace;
- the report records only `Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED` and does not contain the full origin URL;
- index is empty;
- `<TRUTH_PACK>`, `<SUPPLEMENT>`, `<CONTROL>` and `<STATE>` have no pre-existing unstaged changes;
- `<BLUEPRINT_COPY>` and `<REPORT>` do not exist and are not untracked;
- `<STATE>` says `TURN=REVIEW`, `CODE=Coder@当前机`, `ARCH=架构师@主力机`;
- `b46b9dfe` has parent `3e08ab9b7f07d7ba54a0981e74a78193fa315e05` and changes exactly the Truth Pack plus Supplement;
- no preflight staged file exists.

Unrelated pre-existing unstaged/untracked files are not an automatic STOP. Record them exactly, do not read unrelated content, do not modify them and never stage them.

If parent, branch, remote, TURN, index ownership or file attribution cannot be proved, use `NON_PUBLISHABLE_STOP`. Do not repair the repository.

## 5. Delegated turn claim

After preflight passes, update only `<STATE>`:

- create a new current batch `UBF-M0-REWORK-02 — Remote-visible Evidence Repair`;
- preserve the former current batch as the immediately previous batch; do not rewrite older history;
- state: `AUTHORIZED / IN PROGRESS`;
- `TURN=CODE`;
- `CODE=Coder@当前机`;
- `ARCH=架构师@主力机`;
- review mode: `REMOTE_READ_ONLY_ARCH`;
- Handoff Parent: full `b46b9dfe4d2328aeae6f2f244d7ba0a023eee402`;
- Execution Parent: `PENDING CLAIM COMMIT`;
- scope: the eight Issue IDs in §1;
- next step: execute only this blueprint and return to `REVIEW`.

Stage only `<STATE>`. Validate the staged name-status is exactly one modification and `git diff --cached --check` passes.

Commit message:

```text
chore(governance): claim UBF-M0-REWORK-02
```

The claim commit parent must be the Handoff Parent and its only changed file must be `<STATE>`. Record its full hash as `<CLAIM_COMMIT>`, push normally to `origin/master`, and verify remote `master` equals `<CLAIM_COMMIT>`.

Only after remote verification may execution continue. `<CLAIM_COMMIT>` is the runtime Execution Parent.

## 6. Preserve this blueprint in the repository

Save the exact user-provided contents of this document at `<BLUEPRINT_COPY>` without summarizing, rewriting or changing its metadata. This makes the executed contract remotely auditable.

## 7. Repair the Truth Pack

Modify only these statements in `<TRUTH_PACK>`:

1. Keep metadata `Overall collection` as:

```text
PARTIAL / SUPERSEDED IN PART BY UBF-M0-Truth-Pack-Supplement-169bb0a7.md
```

2. In §J replace the contradictory lines with:

```text
- Collection: `PARTIAL / SUPERSEDED IN PART BY UBF-M0-Truth-Pack-Supplement-169bb0a7.md`
- Original capture gaps: canonical GC registry and governance indexes were absent from the original export; explicit phase-state expansion and conflict registration were also absent.
- Current repository resolution evidence: see `UBF-M0-Truth-Pack-Supplement-169bb0a7.md` and `UBF-M0-REWORK-02-Execution-Report.md`.
- SELF_SHA256_REPORTED_EXTERNALLY
```

3. Do not alter original command-output blocks, historical hashes, captured dirty-worktree evidence or project-level source contents.

## 8. Deterministically rebuild Supplement §D

The source snapshot for all three bodies is the runtime Execution Parent `<CLAIM_COMMIT>`. Because the claim commit changes only `<STATE>`, these blobs must be identical to `b46b9dfe`.

Source files and required LF-normalized evidence:

| Source path | Required lines | Required LF-normalized SHA-256 |
|---|---:|---|
| `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | 479 | `0a58da55219ce134095c3a15881205124174b74994e47b58168a91c1f402c827` |
| `.ai-context/docs/experience/INDEX.md` | 55 | `e81a8e26866ba2db3347e998b79bba9833b189ef39e098f9bd023e045aa17241` |
| `.ai-context/project_graph/README.md` | 448 | `2e8ee6833d5cf672bc62118c41938436fcbc66ea24efad500838294b489a4677` |

Mandatory reconstruction method:

1. Use a deterministic temporary script outside the repository or under the system temp directory.
2. The script must read each body directly from `git show <CLAIM_COMMIT>:<source-path>` or `git cat-file blob <CLAIM_COMMIT>:<source-path>` into an in-process byte/string buffer.
3. Do not display the full body in the terminal, do not copy terminal/tool output, and do not let model context become the data transport.
4. Decode as UTF-8, normalize `CRLF` and lone `CR` to `LF`, and preserve every source line in order.
5. Replace the entire content of Supplement `## D. Complete Project-level File Contents` through immediately before `## E. Phase and Protocol State Evidence`.
6. Wrap each full source body in an outer four-tilde fence so embedded triple-backtick fences remain literal:

```text
~~~~markdown
<exact LF-normalized source body>
~~~~
```

7. Record the table's `Repository LF-normalized SHA-256` and `Line Count` using the exact values above.
8. Remove the literal truncation marker and the corrupted synthetic line at prior Supplement line 211.

The reconstructed section must not contain any tool-output truncation signature, ellipsis-based truncation marker or copied tool metadata.

After writing, use the same deterministic script to extract the text between each four-tilde pair, normalize to LF, and compare its bytes with the corresponding committed blob normalized to LF. All three comparisons must be byte-equal. A heading count or visual inspection is not sufficient.

## 9. Repair the remaining Supplement metadata

Update `<SUPPLEMENT>` mechanically:

- header `Status` follows the final outcome: `COMPLETE`, `PARTIAL`, or `BLOCKED_FOR_REVIEW`;
- add `Repair Task: UBF-M0-REWORK-02`;
- add `Handoff Parent: b46b9dfe4d2328aeae6f2f244d7ba0a023eee402`;
- set `Execution Parent: <CLAIM_COMMIT>`;
- update allowlist/denylist text to match this blueprint;
- identify `b46b9dfe` as the prior partial remote review input, not as accepted delivery;
- in §H, claim complete contents only after all three deterministic comparisons pass;
- in §I list the six normal delivery files, or the actual outcome-specific file list;
- set unresolved issues from the execution report; use `NONE` only when all eight §1 issues are closed;
- remove the extra blank line at EOF; end with exactly one newline after the final nonblank line.

For project source evidence, use a column named `Repository LF-normalized SHA-256` and the reproducible values in this blueprint. For `<USER_HOME>/.ai-context/rules/blueprint_protocol.md`, keep its prior externally attested hash but label it `REMOTE_ATTESTED_EXTERNAL_STATE / NOT DIRECTLY REPRODUCIBLE FROM REPOSITORY`.

For state-source rows use these repository LF-normalized values from `b46b9dfe`:

| Source | SHA-256 |
|---|---|
| `.ai-context/project_graph/project.yaml` | `2c756ce240c129e72276d7a97842c953580c006b768227bb06c086c270ca2f0f` |
| `.ai-context/project_graph/migration/PHASE3A_AUDIT.md` | `c78dfbcb08b35ffbc26e165d139cd3929c9aae895bcc70e3acddb40fcb215a52` |
| `.ai-context/project_graph/migration/PHASE3A_BLUEPRINT.md` | `7269fab1a893212fce068367454051ae26c0d643452fac41b19b926c1c8b265b` |
| `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` at `b46b9dfe` | `8933b7d85d382f4bde8a8077db4359514833141a0d70c62efbfaf5785ce56109` |

Do not change any source file to make a hash match.

## 10. Update the repository UBF control

Modify `<CONTROL>` without rewriting historical M0 evidence.

Header changes:

- status becomes `M0 REWORK-02 IN EXECUTION / PENDING REMOTE ARCH REVIEW`;
- current Repository Observation / Handoff Parent becomes full `b46b9dfe4d2328aeae6f2f244d7ba0a023eee402`;
- add `Process Revision: R4 — Remote Review Visibility & Issue Disposition Contract`.

Immediately after existing §7, add a subsection titled:

```text
### 7.1 R4 — Remote Review Visibility & Issue Disposition Contract
```

The subsection must state exactly these contracts in equivalent concise wording:

1. Review role and repository write capability are separate dimensions.
2. `WRITE_CAPABLE_ARCH`: architecture side performs and pushes `REVIEW → CODE` before task release.
3. `REMOTE_READ_ONLY_ARCH`: execution blueprint grants a one-task delegated claim; Coder first pushes a claim commit containing only `BLUEPRINT_STATE.md`.
4. In remote-read-only mode, remote visibility is the exit gate. Every safely publishable `COMPLETE`, `PARTIAL`, `Q`, validation failure or implementation blocker is recorded in the fixed repository report and pushed.
5. Outcomes are `COMPLETE / PENDING REMOTE ARCH REVIEW`, `PARTIAL / PENDING REMOTE ARCH REVIEW`, or `BLOCKED_FOR_REVIEW / PENDING REMOTE ARCH REVIEW`.
6. Every remote-read-only blueprint defines a normal allowlist, publishable-PARTIAL rules, a report+state fallback allowlist, commit messages, Return TURN and `NON_PUBLISHABLE_STOP` conditions.
7. Each issue receives a stable Issue ID, classification, expected/actual, path/line, redacted evidence, action=`NONE — AWAITING ARCH DISPOSITION`, and delivery impact.
8. Coder does not decide whether an unanticipated issue should be repaired.
9. The next architecture task document gives every Issue ID exactly one disposition: `ACCEPT_AS_IS / REPAIR / DEFER / REJECT`, with an exact execution boundary.
10. Only credentials/sensitive ancestry, unisolatable scope/attribution, parent/remote/non-fast-forward mismatch, network/permission failure, or unprovable Git contents may use `NON_PUBLISHABLE_STOP`.
11. No outcome authorizes amend, reset, rebase, force push, history rewrite or expansion of allowlist.
12. After any safely pushed outcome, Luna returns only the full final commit hash and the user forwards only that hash.
13. All future task documents and repository-carried reports identify the target repository only as `cookbook` or `the current cookbook repository`.
14. No future task document hard-codes a hosting provider, account owner, organization or namespace.
15. Coder uses the current worktree's configured `origin`; a task must not require changing it merely to match an architecture-side mirror.
16. Remote preflight validates only that `origin` exists and its final repository path component, ignoring case and an optional `.git` suffix, is `cookbook`.
17. The full origin URL is used only for local mechanical validation and is not copied into task documents, committed reports or chat output.
18. Repository-carried reports use `Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED`.
19. `origin/master` is a permitted Git ref name and is not a hosting or ownership identity claim.

Add a precedence sentence: this R4 subsection supersedes conflicting failure-report and local-only STOP wording in the older §§6–7 for all future `REMOTE_READ_ONLY_ARCH` writable batches.

## 11. Create the R2 execution report

Create `<REPORT>` with:

```text
# UBF-M0-REWORK-02 Execution Report

Document Role: Repository-carried Mechanical Execution and Remote Review Evidence
Task ID: UBF-M0-REWORK-02
Blueprint Revision: R2
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Handoff Parent: b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
Execution Parent / Turn Claim Commit: <CLAIM_COMMIT>
Expected Return TURN: REVIEW
Report State: READY_FOR_COMMIT
Outcome: COMPLETE | PARTIAL | BLOCKED_FOR_REVIEW
```

Required sections:

- `A. Preflight and Turn Claim`: branch, sanitized remote identity (`Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED`), local/remote parent, initial TURN, exact claim hash, claim parent, claim file list and remote verification;
- `B. Prior Review Disposition`: all eight Issue IDs from §1 and their disposition;
- `C. Execution Result`: actual result for each Issue ID and exact validation evidence;
- `D. Embedded Source Integrity`: three source paths, expected/actual line counts, expected/actual LF-normalized SHA-256, byte-compare result;
- `E. Scope and Privacy`: pre-existing dirty entries, actual task-modified paths, staged paths, allowlist result, secrets/path/user-level-fulltext checks;
- `F. Issue Register`: every unresolved issue using the R4 issue schema; `NONE` only if none exist;
- `G. Outcome and Commit Contract`: chosen outcome, chosen allowlist, planned commit message, expected Return TURN, and the statement below.

Include exactly:

```text
This report is created before its containing commit and push. It does not claim
its own commit hash or completed remote publication. The remote architecture
reviewer must verify the final commit, its parent, its file list, TURN return,
content integrity, and origin/master using the user-supplied commit hash.
```

Do not create `UBF-M0-REWORK-01-Execution-Report.md`; that historical report did not exist in `b46b9dfe` and must remain recorded as absent.

## 12. Validation and outcome selection

Before validation, a single mechanical whitespace normalization pass is explicitly authorized on the six task-owned normal-delivery files: remove trailing spaces/tabs and remove blank lines after the final nonblank line, leaving exactly one terminal newline. Do not alter any character inside the three deterministically embedded source bodies. Record whether the pass changed any file.

Run and record at minimum:

```text
git diff --check
git status --short --untracked-files=all
git diff --name-status
git diff --stat
```

Content gates:

- truncation-marker scan returns zero matches in Truth Pack, Supplement, Control, Blueprint copy and Report;
- secret/credential/private-key scan returns zero suspected values;
- real Windows/macOS/Linux home-path scan returns zero real absolute user paths;
- user-level `GLOBAL.md` and `blueprint_protocol.md` full text is absent;
- all Markdown outer four-tilde fences are paired;
- three embedded source bodies pass deterministic normalized byte comparison;
- required line counts are 479, 55 and 448;
- Truth Pack §A and §J both say `PARTIAL / SUPERSEDED IN PART ...`;
- Supplement completion claim equals actual validation;
- `<CONTROL>` contains the R4 heading, three outcomes, four dispositions and `NON_PUBLISHABLE_STOP`;
- `<REPORT>` contains no unresolved template placeholders other than the explicitly self-referential commit/push statement;
- no denylisted file changed.

Select outcome:

### 12.1 COMPLETE

Use only if every content and scope gate passes. Stage the six normal allowlist files.

Commit message:

```text
docs(governance): repair UBF M0 remote review evidence
```

### 12.2 PARTIAL

Use when artifacts are safe to publish and within the normal allowlist, but one or more non-security quality/validation gates remain unresolved. Do not perform an unplanned repair. Record stable Issue IDs, stage the six normal allowlist files and use:

```text
docs(governance): publish partial UBF M0 review input
```

### 12.3 BLOCKED_FOR_REVIEW

Use when target artifacts should not be committed but the Blueprint copy, Report and State can be safely isolated. Stage only the three fallback files. List every unstaged task-owned edit and reason in the report. Use:

```text
docs(governance): publish blocked UBF M0 review input
```

## 13. Return TURN, stage, commit and push

Before staging, update only the current R2 record in `<STATE>`:

- state: `<OUTCOME> / PENDING REMOTE ARCH REVIEW`;
- `TURN=REVIEW`;
- preserve `CODE` and `ARCH` identities;
- Handoff Parent: full `b46b9dfe...`;
- Execution Parent: full `<CLAIM_COMMIT>`;
- evidence: `<BLUEPRINT_COPY>` and `<REPORT>`, plus Truth Pack/Supplement/Control for normal delivery;
- unresolved issues: Issue IDs or `NONE`;
- next step: remote architecture reviewer decides each Issue ID; M1 remains unauthorized.

Stage only the chosen outcome allowlist. Run:

```text
git diff --cached --name-status
git diff --cached --stat
git diff --cached --check
```

The staged file list must exactly match the chosen outcome. `git diff --cached --check` must pass for all outcomes. If it fails, do not silently repair unless the exact repair is already authorized above; record the failure and use `PARTIAL` or `BLOCKED_FOR_REVIEW` as appropriate.

Create one delivery/review-input commit with the outcome-specific message. Verify:

- its parent is `<CLAIM_COMMIT>`;
- its file list equals the chosen allowlist;
- no unrelated dirty item is included;
- the claim commit remains its direct parent and changed only `<STATE>`.

Push normally:

```text
git push origin HEAD:master
```

Then verify `git ls-remote origin refs/heads/master` equals local HEAD. Do not amend after commit and do not create a separate report-finalization commit.

## 14. NON_PUBLISHABLE_STOP

Only these conditions may prevent a remote review-input push:

- credentials, token, private key or unredacted sensitive content exists in the proposed commit or any unpushed ancestor;
- an allowlist/scope violation is already in an unpushed ancestor and cannot be isolated by the fallback path;
- remote advanced, parent/branch/remote identity differs, or push would not be fast-forward;
- network, permission or remote-service failure prevents normal push;
- Git state or evidence is insufficient to prove the exact proposed commit contents.

Return only:

```text
Task: UBF-M0-REWORK-02
Result: NON_PUBLISHABLE_STOP
Failure stage: <stage>
Handoff Parent: b46b9dfe4d2328aeae6f2f244d7ba0a023eee402
Claim commit: <hash-or-NONE>
Local HEAD: <hash>
Remote master: <hash-or-UNKNOWN>
Current TURN: <value>
Reason: <minimal redacted fact>
```

Do not start any follow-on task after push or STOP.
