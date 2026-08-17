# UBF-M0-END-ACCEPT-01 Luna Execution Blueprint

Document Role: Architecture-authored Mechanical Execution Package
Task ID: UBF-M0-END-ACCEPT-01
Blueprint Revision: R1
Date: 2026-08-12
Repository: cookbook
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Execution Mode: EVALUATION / INDEPENDENT
Execution Model: GPT-5.6 Luna
Execution Worktree Mode: ISOLATED_DETACHED_CLEAN
Package Profile: FULL
CookBook Legacy Granularity: L7
Payload Mode: DETERMINISTIC_ARCH_AUTHORED_TRANSFORM
Handoff Parent: `3489523db6508ba742ee835022d7e2a9a64f2c4f`
Expected Return TURN: REVIEW

## 0. Architecture Decision Already Made

This batch does **not** ask CODE to decide whether M0 passes.

The remote architecture review has already decided:

- Reviewed R5 delivery: `3489523db6508ba742ee835022d7e2a9a64f2c4f`
- Architecture disposition: `ACCEPT`
- R5 transaction chain verified: `d7423f30b3892f021a50d162b832d168d2cfad22 -> d3935ae7620312e85d85429b48ac30c62ef80f00 -> 3489523db6508ba742ee835022d7e2a9a64f2c4f`
- R5 exact normal delivery scope: 10 governance files
- Original ten governance repair issues: `10/10 CLOSED`
- Deterministic architecture-authored payload identity/content checks: `PASS`
- R2/R3/R4 report truth corrections: `PASS`
- R3/R4 model-ledger backfill: `PASS`
- R5 State and M0 gate: `PASS`
- R5 introduced issue register: `NONE`

This task only persists that already-made decision and creates the M0→M1 handoff.

It is **not M1 execution** and it does **not** authorize CookBook Phase 3B.

## 1. First-Principles Boundary

The batch must distinguish:

1. `M0 architecture acceptance` — already decided by ARCH and may now be persisted.
2. `This persistence delivery` — still requires its own remote review.
3. `M0→M1 handoff` — may be created as a generated handoff view, but remains `PENDING REMOTE ARCH REVIEW`.
4. `M1 Preview/Start` — a separate future batch only after this persistence delivery receives remote ARCH `ACCEPT`.

Therefore the final repository state of this batch must be:

- M0: `ACCEPT / CLOSED`
- M0→M1 Handoff: `PERSISTED / PENDING REMOTE ARCH REVIEW`
- M1: `NOT STARTED / NOT YET AUTHORIZED`
- CookBook Phase 3B: `NOT AUTHORIZED TO START`
- TURN: `REVIEW`

## 2. Preserve Set

The following accepted/historical material is read-only in this batch:

- `.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`
- `.ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md`
- all `UBF-M0-REWORK-02/03/04/05` execution reports
- all prior R2/R3/R4/R5 execution blueprints
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`
- `.ai-context/docs/experience/INDEX.md`
- `.ai-context/project_graph/**`
- all production code, tests, build/configuration files
- all user-level files outside the repository

No historical commit is amended, rebased, reset, force-pushed, or rewritten.

## 3. Normal Delivery Allowlist — Exact 7 Files

The normal final commit may contain **exactly** these paths:

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-END-ACCEPT-01_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M0-END-ACCEPT-01-Execution-Report.md`
3. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-FINAL-ACCEPT.md`
4. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-to-M1-Handoff.md`
5. `M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
6. `M .ai-context/docs/experience/14_模型执行力评估.md`
7. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No eighth path is allowed.

## 4. Fallback Delivery Allowlist — Exact 4 Files

If the claim commit has already been pushed and the deterministic final transform cannot safely complete, publish a reviewable fallback with exactly:

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-END-ACCEPT-01_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M0-END-ACCEPT-01-Execution-Report.md`
3. `M .ai-context/docs/experience/14_模型执行力评估.md`
4. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

Fallback outcome must be `BLOCKED_FOR_REVIEW / PENDING REMOTE ARCH REVIEW`, return `TURN=REVIEW`, preserve M1/Phase 3B gates, and must **not** create the final-accept or handoff documents or modify the implementation control.

## 5. Package Integrity

The architecture package contains:

- this blueprint;
- `m0_end_accept_apply.py`;
- `MANIFEST.sha256`.

Before repository work:

```bash
python <PACKAGE_PATH>/m0_end_accept_apply.py package-check
```

All entries must be `OK`. If package integrity fails, `NON_PUBLISHABLE_STOP`.

Do not rewrite the script or blueprint.

## 6. Preflight and Isolated Worktree

Use the currently configured `origin`. Do not record or print its full URL in task documents/reports/chat output.

Required preflight:

```bash
git fetch origin
git rev-parse origin/master
```

`origin/master` must equal exactly:

```text
3489523db6508ba742ee835022d7e2a9a64f2c4f
```

If not equal, `NON_PUBLISHABLE_STOP`; do not claim TURN and do not checkout/reset/rebase the user's existing worktree.

Create a new isolated detached clean worktree at the exact Handoff Parent. The original worktree/index/untracked files must remain untouched.

The architecture script itself verifies the three mutable preimages against the accepted R5 commit.

## 7. Turn Claim — Commit 1

From the isolated worktree at `3489523db6508ba742ee835022d7e2a9a64f2c4f` run:

```bash
python <PACKAGE_PATH>/m0_end_accept_apply.py claim --repo .
git diff --check
git diff --name-status
```

The only changed path must be:

```text
M .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

Then:

```bash
git add -- .ai-context/docs/context_memory/BLUEPRINT_STATE.md
git diff --cached --check
git diff --cached --name-status
git commit -m "docs(governance): claim UBF M0 accept handoff turn"
git rev-parse HEAD
git push origin HEAD:master
```

The claim commit must:

- be a direct child of `3489523db6508ba742ee835022d7e2a9a64f2c4f`;
- change only `BLUEPRINT_STATE.md`;
- set the current batch to `UBF-M0-END-ACCEPT-01`;
- set `TURN=CODE`;
- record R5 architecture disposition as `ACCEPT`;
- keep M1 and Phase 3B unauthorized.

Save the full claim hash as `<CLAIM_COMMIT>`.

## 8. Deterministic Final Transform

After the claim push, with the isolated worktree clean at `<CLAIM_COMMIT>`:

```bash
python <PACKAGE_PATH>/m0_end_accept_apply.py final --repo . --claim <CLAIM_COMMIT>
```

The script must complete all seven fixed writes in memory first, validate them, and only then write files.

It must:

1. persist the R5 remote ARCH `ACCEPT`;
2. backfill the R5 ledger row with final commit `3489523db6508ba742ee835022d7e2a9a64f2c4f` and the architecture assessment;
3. append exactly one current-task CODE evidence row for GPT-5.6 Luna;
4. create `UBF-M0-FINAL-ACCEPT.md`;
5. create `UBF-M0-to-M1-Handoff.md`;
6. update implementation control to M0 `ACCEPT / CLOSED` and handoff pending review;
7. update State to M0 closed, handoff persisted/pending review, `TURN=REVIEW`;
8. create the current execution report;
9. add this exact blueprint to the repository;
10. keep concrete model names out of `BLUEPRINT_STATE.md`.

## 9. Required Final Validations

Run:

```bash
git diff --check
git diff --name-status
git status --short
```

Exact normal changed-file set must equal the seven-file allowlist in §3.

Also verify:

```bash
grep -n "3489523db6508ba742ee835022d7e2a9a64f2c4f"   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-FINAL-ACCEPT.md   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-to-M1-Handoff.md   .ai-context/docs/experience/14_模型执行力评估.md

grep -n "M0.*ACCEPT"   .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md   .ai-context/docs/context_memory/BLUEPRINT_STATE.md

grep -n "M1.*NOT STARTED\|M1.*NOT YET AUTHORIZED\|M1.*unauthorized"   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-to-M1-Handoff.md   .ai-context/docs/context_memory/BLUEPRINT_STATE.md

grep -n "Phase 3B.*NOT AUTHORIZED"   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-to-M1-Handoff.md   .ai-context/docs/context_memory/BLUEPRINT_STATE.md
```

The State file must contain none of:

```text
GPT-5.6
Luna
DeepSeek
Claude
Gemini
```

The Preserve Set must have zero diff.

## 10. Final Commit and Push

Stage only the seven allowed files.

```bash
git add --   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-END-ACCEPT-01_Luna_Execution_Blueprint.md   .ai-context/docs/UBF-M0-END-ACCEPT-01-Execution-Report.md   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-FINAL-ACCEPT.md   .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-to-M1-Handoff.md   .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md   .ai-context/docs/experience/14_模型执行力评估.md   .ai-context/docs/context_memory/BLUEPRINT_STATE.md

git diff --cached --check
git diff --cached --name-status
```

Commit:

```bash
git commit -m "docs(governance): persist UBF M0 accept and M1 handoff"
git rev-parse HEAD
git push origin HEAD:master
```

Do not amend either commit.

## 11. Fallback After Claim

Use fallback only if:

- claim was already pushed;
- repository/parent relation remains provable;
- failure is safe to publish;
- normal transform cannot satisfy its exact validation.

Run:

```bash
python <PACKAGE_PATH>/m0_end_accept_apply.py fallback   --repo .   --claim <CLAIM_COMMIT>   --reason-code <TRANSFORM_FAILURE|VALIDATION_FAILURE>
```

Validate the exact four-file fallback set, stage only those four files, and commit:

```text
docs(governance): publish blocked UBF M0 handoff persistence
```

Push normally and return the full commit hash.

Do not use fallback for credential/sensitive-ancestry exposure, preimage/package-integrity failure, unprovable Git contents, parent/remote mismatch, non-fast-forward, network/permission failure, or package-integrity failure. Those are `NON_PUBLISHABLE_STOP`.

## 12. Completion Gate

A successful normal delivery may state only:

```text
M0: ACCEPT / CLOSED
M0→M1 Handoff: PERSISTED / PENDING REMOTE ARCH REVIEW
M1: NOT STARTED / NOT YET AUTHORIZED
CookBook Phase 3B: NOT AUTHORIZED TO START
TURN: REVIEW
```

It must **not** state:

- M1 started;
- M1 accepted;
- CookBook Phase 3B authorized;
- this persistence delivery self-accepted.

After push, return the **full final commit hash** to the user. The remote architecture reviewer will independently verify the chain, allowlist, payload result, acceptance/handoff truth, ledger, State and gates.
