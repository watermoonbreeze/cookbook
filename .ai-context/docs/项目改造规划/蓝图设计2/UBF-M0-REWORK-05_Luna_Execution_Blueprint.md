# UBF-M0-REWORK-05 — Deterministic M0 Governance Repair

> Document Role: Luna Mechanical Execution Blueprint
> Status: `READY FOR EXECUTION`
> Task ID: `UBF-M0-REWORK-05`
> Blueprint Revision: `R1`
> Execution Mode: `EVALUATION / INDEPENDENT`
> Review Operation Mode: `REMOTE_READ_ONLY_ARCH`
> Execution Worktree Mode: `ISOLATED_DETACHED_CLEAN`
> Payload Mode: `DETERMINISTIC_ARCH_AUTHORED_TRANSFORM`
> Package Profile: `FULL`
> CookBook Legacy Granularity: `L7`
> Handoff Parent: `d7423f30b3892f021a50d162b832d168d2cfad22`
> Execution Parent: `CLAIM_COMMIT_RESOLVED_AT_RUNTIME`
> Expected Initial TURN: `REVIEW`
> Execution TURN: `CODE`
> Return TURN: `REVIEW`
> Target Branch: `master`
> Target Repository: `cookbook`
> Blueprint Designer / Reviewer: `Architecture model`
> User-designated Execution Model: `GPT-5.6 Luna`
> Mid-execution Architecture Assistance Allowed: `NO`
> Date: `2026-08-12`

## 0. Authority and execution contract

Read this document completely in a fresh context before acting. This document and the two immutable files packaged with it form one self-contained execution package. No earlier chat, worktree, blueprint interpretation or historical source reconstruction is required or authorized.

This is a narrow UBF M0 governance repair. It does not authorize architecture redesign, UBF M1, CookBook Phase 3B, production code, tests, builds, dependencies, Project Graph mutations, cleanup of the user's original worktree or history rewriting.

R5 converts the R4 failure into a higher-closure execution form:

- architecture has already decided every target-content transformation;
- `r5_apply.py` accepts only the exact reviewed R4 preimages and produces the target files deterministically;
- Luna's remaining responsibilities are package integrity, isolated-worktree creation, the two Git transactions, exact script invocation, mechanical validation, staging and push;
- Luna must not manually reinterpret or repair generated content;
- if a fixed assertion fails, record the exact assertion as an execution issue and use the fallback path; do not improvise.

This remains `EVALUATION / INDEPENDENT`. Do not ask for or use architecture assistance during execution. A payload assertion failure is evidence and must be reported, not silently bypassed.

On successful final push, reply only:

```text
COMMIT_HASH: <40-character final remote commit hash>
```

## 1. R4 review disposition being executed

Reviewed delivery:

```text
d7423f30b3892f021a50d162b832d168d2cfad22
```

Architecture result:

```text
BLOCKED_FOR_REVIEW / REMOTE INPUT VALID / CONTENT REWORK REQUIRED
```

Accepted R4 mechanics:

- isolated detached clean worktree;
- State-only claim commit;
- four-file fallback final commit;
- direct-parent chain, allowlist, whitespace and final `TURN=REVIEW`;
- remote `master` publication.

R5 repair issues:

| Issue ID | Verified fact | Disposition | R5 exact boundary |
|---|---|---|---|
| `UBF-M0-R2-02` | Supplement truth/status stale | `REPAIR` | Payload replaces non-§D governance sections |
| `UBF-M0-R2-05` | R2 report inaccurate | `REPAIR` | Payload replaces full R2 report |
| `UBF-M0-R2-07` | Control R4 text corrupted/incomplete | `REPAIR` | Payload installs UTF-8 19-rule and 7-rule contracts |
| `UBF-M0-R2-08` | Supplement hash basis stale | `REPAIR` | Payload installs fixed LF-normalized evidence |
| `UBF-M0-R3-01` | Repository R2 blueprint is stale R1 | `REPAIR` | Payload copies packaged 479-line R2 authority exactly |
| `UBF-M0-R3-02` | Supplement provenance/integrity stale | `REPAIR` | Payload installs R5 metadata and integrity |
| `UBF-M0-R3-03` | R2 report contains contradicted assessment/identity | `REPAIR` | Full deterministic replacement |
| `UBF-M0-R3-04` | State/history not truthfully closed | `REPAIR` | Payload builds R5/R4/R3/R2 ordered state |
| `UBF-M0-R3-05` | Control/Supplement current-stage gate stale | `REPAIR` | Payload installs M0/R5 transition gate |
| `UBF-M0-R3-06` | R3 ledger/review evidence incomplete | `REPAIR` | Exact R3 backfill, R4 backfill and R5 append |
| `UBF-M0-R3-EXEC-01` | R3 original worktree was polluted | `ACCEPT_AS_CLOSED_MECHANICALLY` | Continue isolated worktree; original remains untouched |
| `UBF-M0-R4-01` | R3 claim historical deviations absent from corrected report | `REPAIR` | Payload replaces R3 report |
| `UBF-M0-R4-02` | R3 report aggregated issues | `REPAIR` | Payload writes ten individual rows |
| `UBF-M0-R4-03` | R3 report omitted fixed hashes | `REPAIR` | Payload writes three fixed rows |
| `UBF-M0-R4-04` | R3 dirty paths incompletely preserved | `REPAIR` | Payload records only recoverable facts and non-invention boundary |
| `UBF-M0-R5-01` | R4 blocker attribution unsupported | `REPAIR` | Correct R4 report; payload does not reuse blocker |
| `UBF-M0-R5-02` | R3 ledger not backfilled although R4 report claimed it | `REPAIR` | Exact R3 row mutation |
| `UBF-M0-R5-03` | R4-01~04 were falsely marked PASS | `REPAIR` | Correct R4 and R3 reports |
| `UBF-M0-R5-04` | R4 report internally inconsistent | `REPAIR` | Full R4 report replacement |
| `UBF-M0-R5-05` | R4 `New Issue Register: NONE` was false | `REPAIR` | Correct R4 issue register |

No Issue ID may be reinterpreted. The payload contents are the architecture disposition.

## 2. Execution-package manifest

The user supplies one ZIP. Extract it into a new temporary directory outside every Git worktree. It must contain exactly these three regular files at its root:

| File | Role | LF-normalized SHA-256 | Lines |
|---|---|---|---:|
| `UBF-M0-REWORK-05_Luna_Execution_Blueprint.md` | sole authority document | verify identity/header; repository reviewer compares exact bytes | fixed by package |
| `r5_apply.py` | deterministic claim/content transformer | `ec574e97ce079b812a6ca31a6bce4028e12d7e55fd45aacc4a6314e6f66d8b17` | 863 |
| `UBF-M0-REWORK-02_Luna_Execution_Blueprint.md` | exact R2 authority payload | `c9d8274e4c4247394fa7dfa97bc85af07fb4db09a397ab5989568d4f819e92ec` | 479 |

Package rules:

- do not edit any extracted file;
- do not place extracted payload files inside the repository;
- do not commit the ZIP or `r5_apply.py`;
- the script copies only the R5 blueprint and authoritative R2 blueprint into authorized repository paths;
- reject symlinks, additional package files, hash mismatch, line-count mismatch or filename mismatch;
- temporary absolute paths are local-only and must not enter reports or chat output.

## 3. Repository paths and exact allowlists

```text
<SUPPLEMENT> = .ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md
<CONTROL> = .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md
<R2_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
<R2_REPORT> = .ai-context/docs/UBF-M0-REWORK-02-Execution-Report.md
<R3_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-03_Luna_Execution_Blueprint.md
<R3_REPORT> = .ai-context/docs/UBF-M0-REWORK-03-Execution-Report.md
<R4_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-04_Luna_Execution_Blueprint.md
<R4_REPORT> = .ai-context/docs/UBF-M0-REWORK-04-Execution-Report.md
<R5_BLUEPRINT> = .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-05_Luna_Execution_Blueprint.md
<R5_REPORT> = .ai-context/docs/UBF-M0-REWORK-05-Execution-Report.md
<MODEL_LEDGER> = .ai-context/docs/experience/14_模型执行力评估.md
<STATE> = .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Claim commit allowlist, exactly one file:

```text
M  <STATE>
```

Normal final allowlist, exactly ten files:

```text
M  <SUPPLEMENT>
M  <CONTROL>
M  <R2_BLUEPRINT>
M  <R2_REPORT>
M  <R3_REPORT>
M  <R4_REPORT>
A  <R5_BLUEPRINT>
A  <R5_REPORT>
M  <MODEL_LEDGER>
M  <STATE>
```

Fallback final allowlist, exactly four files:

```text
A  <R5_BLUEPRINT>
A  <R5_REPORT>
M  <MODEL_LEDGER>
M  <STATE>
```

The two prior blueprints are preserve-only and must have zero diff.

## 4. Preserve set and denylist

Preserve without modification:

- Git history through the Handoff Parent;
- the user's original worktree, index, dirty/untracked files and contents;
- R3 blueprint: 690 LF-normalized lines, SHA-256 `815c0344698baad56797a863eabd4d8202e6f981fa28df34017a0fa21a5596ef`;
- R4 blueprint: 771 LF-normalized lines, SHA-256 `860bf759e237146b456c7cf7d5959492f597d18c4aa102a419f7db9e205118a7`;
- Truth Pack and all three Supplement §D embedded bodies;
- canonical GC/protocol/Project Graph/Phase state sources;
- every model-ledger row except exact R3/R4 backfills and one R5 append;
- all production code, tests, builds, dependencies and configuration.

Do not run `pull`, `merge`, `rebase`, `reset`, `clean`, `stash`, amend, force push or history rewrite. Do not remove any worktree in this task.

Repository naming in committed files and chat is only `cookbook` or `the current cookbook repository`. Use configured `origin` without changing it. Never record provider, owner, organization, namespace or full origin URL.

## 5. Isolated clean-worktree bootstrap

Run from an existing cookbook working copy without reading or changing dirty-file contents:

1. Resolve repository root and locally validate configured `origin`; do not output its value.
2. Fetch `refs/heads/master` without merging.
3. Require `origin/master` exactly equals the Handoff Parent.
4. Require the remote path's final component, ignoring case and optional `.git`, equals `cookbook`.
5. Create a new sibling detached worktree from the exact Handoff Parent.
6. Use preferred name `cookbook-ubf-m0-rework-05-clean`; if occupied, choose first unused suffix `-01` through `-20`. Never delete, prune, overwrite or reuse an existing directory/worktree.
7. Run every remaining repository command only inside the isolated worktree.

Expected start:

```text
HEAD = d7423f30b3892f021a50d162b832d168d2cfad22
origin/master = d7423f30b3892f021a50d162b832d168d2cfad22
branch = detached HEAD
index = empty
worktree = clean
```

Record only `Execution Worktree Mode: ISOLATED_DETACHED_CLEAN`; never record an absolute path.

## 6. Preflight

Run and locally inspect:

```text
git rev-parse HEAD
git branch --show-current
git ls-remote origin refs/heads/master
git status --short --untracked-files=all
git diff --cached --name-status
git show HEAD:.ai-context/docs/context_memory/BLUEPRINT_STATE.md
git show -s --format='%H %P %s' d7423f30b3892f021a50d162b832d168d2cfad22
git diff-tree --check 6c62a91dfc9dab1806725ec595cd7297e947a732 d7423f30b3892f021a50d162b832d168d2cfad22
```

Require:

- exact start state from §5;
- Handoff Parent direct parent `6c62a91dfc9dab1806725ec595cd7297e947a732`;
- Handoff Parent changes exactly the R4 four-file fallback;
- State says `TURN=REVIEW` and contains no concrete model name;
- all ten normal targets that already exist are present and clean;
- R5 blueprint/report do not exist;
- no staged or unstaged file exists.

## 7. Deterministic claim transaction

From the extracted package, run the script's claim phase with the isolated repository root:

```text
python3 <PACKAGE_DIR>/r5_apply.py --repo <ISOLATED_REPO> --prepare-claim
```

Require stdout:

```text
R5_CLAIM_STATE_PREPARED
```

Do not edit the produced State. Verify:

- only State is modified;
- current task is R5;
- status is `AUTHORIZED / IN PROGRESS`;
- `TURN=CODE`;
- Execution Parent is exactly `PENDING CLAIM COMMIT`;
- R4 is immediately previous, followed by R3 and R2;
- no concrete model name exists;
- `git diff --cached --check` passes after staging only State.

Commit:

```text
chore(governance): claim UBF-M0-REWORK-05
```

Push normally with `git push origin HEAD:master`, verify remote `master` equals it, and record the full hash as `<CLAIM_COMMIT>`. It must directly descend from the Handoff Parent and change only State.

## 8. Deterministic content transformation

With a clean isolated worktree at `<CLAIM_COMMIT>`, run exactly:

```text
python3 <PACKAGE_DIR>/r5_apply.py \
  --repo <ISOLATED_REPO> \
  --claim <CLAIM_COMMIT> \
  --blueprint <PACKAGE_DIR>/UBF-M0-REWORK-05_Luna_Execution_Blueprint.md \
  --r2-payload <PACKAGE_DIR>/UBF-M0-REWORK-02_Luna_Execution_Blueprint.md
```

Require stdout:

```text
R5_PAYLOAD_APPLIED
CLAIM_COMMIT=<CLAIM_COMMIT>
EXPECTED_FINAL_FILE_COUNT=10
```

The script is the complete content authority. It performs:

- exact R4-reviewed preimage-hash checks;
- fixed retained-source hash/line checks from the claim commit;
- exact R2 blueprint restoration from the packaged 479-line authority;
- Supplement reconstruction while preserving §D byte-for-byte;
- Control UTF-8 R4/model contracts and current M0/R5 gate;
- full corrected R2, R3 and R4 reports;
- exact R3 and R4 model-ledger backfills plus one R5 row;
- truthful R5 report and final State;
- exact ten-path post-write assertion.

Do not manually edit any generated file. Do not run formatters.

## 9. Mechanical validation

After the script succeeds, run:

```text
git status --short --untracked-files=all
git diff --name-status
git diff --stat
git diff --check
git diff -- .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-03_Luna_Execution_Blueprint.md
git diff -- .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-REWORK-04_Luna_Execution_Blueprint.md
```

Required:

- exact ten-file normal allowlist and no other path;
- `git diff --check` PASS;
- R3 and R4 blueprints zero diff;
- Truth Pack and every denylisted source zero diff;
- R2 blueprint 479 LF-normalized lines and fixed SHA-256 `c9d8274e4c4247394fa7dfa97bc85af07fb4db09a397ab5989568d4f819e92ec`;
- R3/R4 blueprint fixed line/hash values from §4;
- Supplement §D unchanged and retained-source 479/55/448 hash checks PASS;
- Control has no `鈥`, `鈫`, `�`, malformed arrow or malformed section symbol;
- R2 report contains eight individual result rows;
- R3 report contains ten individual repair rows, five R3/R4 evidence issues, three hashes, two claim deviations and the historical dirty-path recoverability boundary;
- R4 report records 0/10, rejected blocker attribution, R4-01~04 failures and R5-01~05;
- ledger mutations are exactly R3 backfill, R4 backfill and one R5 append;
- State current order is R5 → R4 → R3 → R2, `TURN=REVIEW`, no concrete model name;
- R5 report outcome is COMPLETE pending review and does not claim its own final commit/push;
- no provider/owner/namespace/full endpoint, secret, credential, private key or real absolute user-home value was added.

## 10. Outcome and final transaction

### 10.1 COMPLETE

Use only if the payload and every §9 gate pass. Stage exactly the ten normal files and run:

```text
git diff --cached --name-status
git diff --cached --stat
git diff --cached --check
```

Commit once:

```text
docs(governance): apply deterministic UBF M0 R5 repair
```

Verify:

- final commit direct parent is `<CLAIM_COMMIT>`;
- claim direct parent is the fixed Handoff Parent;
- final file list is exactly ten normal files;
- claim file list is State only;
- original worktree remains untouched.

Push normally:

```text
git push origin HEAD:master
```

Verify remote `master` equals local detached HEAD. Do not amend, add a report-finalization commit, remove the worktree or start another task.

### 10.2 ASSERTION_FAILURE fallback

If package, preimage, transform or content assertion fails after the claim push:

- stop immediately at the first failure;
- do not manually modify generated targets or bypass the assertion;
- do not stage any normal target;
- restore task-generated normal-target working changes to the claim commit only if the script wrote them, leaving the original worktree untouched;
- save the exact package R5 blueprint;
- create an R5 report containing the fixed preflight/claim facts, the exact failed assertion, zero falsely closed issues, the list of any unstaged task-owned paths and `Outcome: BLOCKED_FOR_REVIEW`;
- append one truthful R5 ledger row with the failure stage and issue `UBF-M0-R5-EXEC-01`;
- set State to `BLOCKED_FOR_REVIEW / PENDING REMOTE ARCH REVIEW`, `TURN=REVIEW`, unresolved `UBF-M0-R5-EXEC-01`;
- stage exactly the four fallback files and commit `docs(governance): publish blocked UBF M0 R5 review input`;
- push normally and return only its full hash.

No assertion failure may be reported as `COMPLETE` or `PARTIAL`.

## 11. NON_PUBLISHABLE_STOP

Only these prevent a remote review-input push:

- credential, private key or sensitive value exists in proposed commit or unpushed ancestry;
- unisolatable scope violation exists in unpushed ancestry;
- remote advanced, parent differs or push is non-fast-forward;
- network, permission or remote-service failure prevents normal push;
- Git evidence cannot prove proposed commit contents;
- no clean isolated worktree can be created without touching user files.

Return only:

```text
Task: UBF-M0-REWORK-05
Result: NON_PUBLISHABLE_STOP
Failure stage: <stage>
Handoff Parent: d7423f30b3892f021a50d162b832d168d2cfad22
Claim commit: <hash-or-NONE>
Local HEAD: <hash>
Remote master: <hash-or-UNKNOWN>
Current TURN: <value>
Reason: <minimal redacted fact>
```

Do not start M1, CookBook Phase 3B or any follow-on task after push or STOP.
