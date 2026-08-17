# UBF-M3-CALIBRATION-ANALYSIS-WORK-01 — Luna Execution Blueprint

Package Revision: `R1`
Execution Model: `GPT-5.6 Luna`
Review Mode: `REMOTE_READ_ONLY_ARCH`
Payload: `AUTHORITATIVE_STATIC_TARGET_BUNDLE`
Handoff Parent: `5d6eda046be0b2a09f52059e438cb51f7db38e40`

## Objective

Mechanically persist the ARCH-authored cluster-aware Analysis Work-01 result. **Do not re-analyze, relabel, reinterpret, or choose a different hypothesis.** The authoritative analysis disposition is `H4_INSUFFICIENT_EVIDENCE`. This is an evidence disposition only, not a Universal Level decision.

## Turn contract

The exact Handoff Parent is expected to be `TURN=REVIEW`. That is the valid pre-claim state. This architecture package is a single-task delegated claim authority bound to package/revision/parent/target ref.

`REVIEW -> exact State-only claim -> push+remote verify -> CODE -> exact eight-path final -> push+remote verify -> REVIEW`

The user's original worktree may be dirty or behind. Preserve it. Fetch remote and execute only in an isolated detached clean checkout at the fixed parent. Never pull/reset/clean/stash/rebase the host worktree.

## Exact final scope

Normal final is exactly **8 paths = 3 adds + 5 modifications**. All target bytes are architecture-authored static Truth.

## Mechanical gates

- remote target ref equals `5d6eda046be0b2a09f52059e438cb51f7db38e40` before claim;
- exact package/delegation identity matches;
- claim changes State only and matches expected blob;
- final staged set is exact 8 paths / 3A+5M;
- all 8 staged blobs match manifest;
- Work-01/02/03 corpus JSON files remain exact Preserve;
- analysis JSON recount = 9 eligible clusters / 6 positive-only / 2 negative-only / 1 mixed / 1 synthetic;
- actor cluster recount = Luna 5 / V4 Flash 3 / V4 Pro 1;
- all 8 falsification tests are present exactly once;
- disposition = `H4_INSUFFICIENT_EVIDENCE`;
- all forbidden decision counters = 0;
- State contains no concrete model name;
- `git diff --cached --check` passes;
- final returns `TURN=REVIEW`.

## Hard STOP boundary

Hard STOP only for remote parent mismatch, invalid package/delegation identity, semantic/preimage mismatch, unsafe isolation, unauthorized changed set, or other governance Truth failure. Tool/path/helper incompatibility is soft compatibility while an authorized exact-byte fallback remains.

## Do not continue

After final push + remote verification, STOP. Do not create a Universal Level proposal, Work-02 analysis, M4, M5 or CookBook Phase 3B.
