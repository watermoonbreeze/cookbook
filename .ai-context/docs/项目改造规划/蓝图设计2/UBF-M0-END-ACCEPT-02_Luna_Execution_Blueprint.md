# UBF-M0-END-ACCEPT-02 Luna Execution Blueprint

Document Role: Architecture-authored Mechanical REWORK Package
Task ID: UBF-M0-END-ACCEPT-02
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
Handoff Parent: `d6c8d5f693ace96a525d9dc797042467660bf6ef`
Expected Return TURN: REVIEW

## 0. Remote Architecture Decision Already Made

Remote ARCH reviewed `UBF-M0-END-ACCEPT-01` delivery:

- reviewed delivery: `d6c8d5f693ace96a525d9dc797042467660bf6ef`;
- Git transaction: PASS;
- exact 7-file normal allowlist: PASS;
- deterministic payload execution fidelity: PASS, remote 7/7 target files matched the architecture-authored transform;
- R5 acceptance persistence: PASS;
- M0 Final Accept content: PASS;
- M0→M1 Handoff content: PASS;
- Control/State M0 and M1/Phase 3B gates: PASS;
- only blocker: `ARCH-PAYLOAD-01`.

`ARCH-PAYLOAD-01` is narrowly defined:

> The final model execution ledger row for `UBF-M0-END-ACCEPT-01` still said `待执行`, while Git, Execution Report and State already said the batch was complete. The stale value was authored inside the architecture deterministic payload. It is **not** a GPT-5.6 Luna execution deviation.

Remote disposition is therefore:

`REWORK — ARCH-PAYLOAD-01 ONLY`

This task repairs that one truth inconsistency and records the current repair execution evidence. It does not reopen M0 acceptance content.

## 1. Frozen Semantics and Lifecycle Boundary

The following facts are already accepted and are frozen in this batch:

1. R5 `3489523db6508ba742ee835022d7e2a9a64f2c4f` remains ARCH `ACCEPT`.
2. M0 remains `ACCEPT / CLOSED`.
3. `UBF-M0-FINAL-ACCEPT.md` is correct and must not change.
4. `UBF-M0-to-M1-Handoff.md` is correct and must not change.
5. the implementation control content produced by END-ACCEPT-01 is correct and must not change.
6. M1 remains `NOT STARTED / NOT YET AUTHORIZED` until remote ARCH accepts **this repair delivery**.
7. CookBook Phase 3B remains `NOT AUTHORIZED TO START`.

The mutable lifecycle/evidence state in this batch is limited to:

- correcting the END-ACCEPT-01 model-ledger row from pre-execution placeholder to actual execution/review truth;
- adding one current END-ACCEPT-02 CODE evidence row;
- updating `BLUEPRINT_STATE.md` to represent this repair transaction and return `TURN=REVIEW`.

## 2. Preserve / Deny Set — Zero Diff Required

The following must remain byte-identical to Handoff Parent:

- `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-FINAL-ACCEPT.md`
- `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-to-M1-Handoff.md`
- `.ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
- `.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`
- `.ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md`
- all R2/R3/R4/R5 execution reports and blueprints
- `UBF-M0-END-ACCEPT-01-Execution-Report.md`
- `UBF-M0-END-ACCEPT-01_Luna_Execution_Blueprint.md`
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`
- `.ai-context/docs/experience/INDEX.md`
- `.ai-context/project_graph/**`
- all production code, tests, build/configuration files
- all user-level files outside the repository

No amend, reset, rebase, force push, or historical rewrite is allowed.

## 3. Exact Normal Delivery Allowlist — 4 Files

The final commit may contain **exactly** these paths:

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-END-ACCEPT-02_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M0-END-ACCEPT-02-Execution-Report.md`
3. `M .ai-context/docs/experience/14_模型执行力评估.md`
4. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No fifth path is allowed.

## 4. Fallback Delivery After Claim — Exact 3 Files

If claim has already been pushed but the normal deterministic final transform cannot complete, the only fallback scope is:

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-END-ACCEPT-02_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M0-END-ACCEPT-02-Execution-Report.md`
3. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

Fallback must return `TURN=REVIEW`, leave `ARCH-PAYLOAD-01` unresolved, and keep M1/Phase 3B blocked. Do not modify the model ledger in fallback.

## 5. Fixed Parent Preimages

The deterministic script independently checks the accepted remote parent versions before any mutation:

- `14_模型执行力评估.md` SHA-256: `4bd0915538988879181055999029d7428ffbb1cdada719fbcf3c618787b1703f`
- `BLUEPRINT_STATE.md` SHA-256: `cd63cfc1065c9138e97d4ee2cdd4de8f0362e5f544b1912d291711a6c430e7c8`

A mismatch is `NON_PUBLISHABLE_STOP` before TURN claim.

## 6. Package Integrity

The package contains:

- this blueprint;
- `m0_end_accept_02_apply.py`;
- `MANIFEST.sha256`.

Before repository work run:

```bash
python <PACKAGE_PATH>/m0_end_accept_02_apply.py package-check
```

Every entry must report `OK`. Do not rewrite the script or blueprint.

## 7. Remote Preflight and Isolated Worktree

Use the currently configured `origin`. Do not copy the full remote URL into reports or chat output.

```bash
git fetch origin
git rev-parse origin/master
```

It must equal exactly:

```text
d6c8d5f693ace96a525d9dc797042467660bf6ef
```

Otherwise `NON_PUBLISHABLE_STOP`: do not claim TURN, reset, rebase, or touch the user's existing worktree.

Create an isolated detached clean worktree at the fixed Handoff Parent. The original worktree/index/untracked files must remain untouched.

## 8. TURN Claim — Commit 1

From the isolated worktree at the exact Handoff Parent:

```bash
python <PACKAGE_PATH>/m0_end_accept_02_apply.py claim --repo .
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
git commit -m "docs(governance): claim UBF M0 evidence truth repair"
git rev-parse HEAD
git push origin HEAD:master
```

The claim commit must be a direct child of the fixed Handoff Parent and change only State. Save its full hash as `<CLAIM_COMMIT>`.

## 9. Deterministic Final Transform

At clean `<CLAIM_COMMIT>` run:

```bash
python <PACKAGE_PATH>/m0_end_accept_02_apply.py final --repo . --claim <CLAIM_COMMIT>
```

The architecture script must produce changes in exactly four files. The semantic operations are:

1. add this exact Blueprint;
2. add the deterministic Execution Report;
3. replace the END-ACCEPT-01 ledger row with actual remote truth:
   - final commit = `d6c8d5f693ace96a525d9dc797042467660bf6ef`;
   - CODE execution fidelity = PASS;
   - exact 7-file delivery = PASS;
   - deterministic remote 7/7 byte identity = PASS;
   - M0/Handoff/State gates = PASS;
   - remote delivery disposition = REWORK only because of `ARCH-PAYLOAD-01`;
   - root cause attribution = architecture-authored payload, **not Luna execution failure**;
4. append exactly one END-ACCEPT-02 current CODE evidence row whose execution result is already `COMPLETE / PENDING REMOTE ARCH REVIEW`, not `待执行`;
5. update State to current END-ACCEPT-02, `TURN=REVIEW`, with the previous END-ACCEPT-01 review truth recorded and M1/Phase 3B still blocked.

The current END-ACCEPT-02 ledger row must leave its own final commit/ARCH review fields pending for the next authorized write batch. This avoids self-referential final-commit claims.

## 10. Semantic Acceptance Gate Added After ARCH-PAYLOAD-01

This repair explicitly separates two checks:

- **Payload identity gate**: CODE output equals the architecture deterministic transform.
- **Architecture semantic truth gate**: generated cross-document lifecycle/evidence states are mutually consistent.

For this batch both must pass before final commit.

Required script assertions include:

- END-ACCEPT-01 ledger row contains `d6c8d5f...` and no longer contains `待执行`;
- END-ACCEPT-01 ARCH assessment explicitly assigns `ARCH-PAYLOAD-01` to architecture payload, not CODE deviation;
- END-ACCEPT-02 ledger row contains `COMPLETE / PENDING REMOTE ARCH REVIEW`;
- State contains no concrete model names;
- State says M0 `ACCEPT / CLOSED`;
- State says M1 is `NOT STARTED` and Phase 3B remains `NOT AUTHORIZED TO START`;
- exact normal changed-file set = 4;
- all Preserve/Deny paths have zero diff.

## 11. Final Validation and Commit — Commit 2

Run:

```bash
git diff --check
git diff --name-status
git status --short
```

Then stage exactly the four normal paths:

```bash
git add -- \
  .ai-context/docs/项目改造规划/蓝图设计2/UBF-M0-END-ACCEPT-02_Luna_Execution_Blueprint.md \
  .ai-context/docs/UBF-M0-END-ACCEPT-02-Execution-Report.md \
  .ai-context/docs/experience/14_模型执行力评估.md \
  .ai-context/docs/context_memory/BLUEPRINT_STATE.md

git diff --cached --check
git diff --cached --name-status
```

Commit and push:

```bash
git commit -m "docs(governance): close UBF M0 evidence truth repair"
git rev-parse HEAD
git push origin HEAD:master
```

Do not amend either commit.

## 12. Completion Contract

Normal completion requires:

- two-commit chain from fixed Handoff Parent;
- claim direct child and State-only;
- final direct child of claim;
- exact four-file final allowlist;
- deterministic payload identity checks PASS;
- semantic truth checks PASS;
- `ARCH-PAYLOAD-01` closed in ledger truth;
- no attribution of the architecture payload defect to Luna;
- State returns `TURN=REVIEW`;
- M1 remains not started;
- CookBook Phase 3B remains unauthorized.

CODE must **not** claim remote ARCH ACCEPT for END-ACCEPT-02 and must not start M1.

After successful push, return only the full final commit hash to the user.
