# UBF-M3-CALIBRATION-ANALYSIS-PREVIEW-START-01 — Luna Execution Blueprint

Package Revision: `R1`
Execution Model: `GPT-5.6 Luna`
Review Mode: `REMOTE_READ_ONLY_ARCH`
Payload: `AUTHORITATIVE_STATIC_TARGET_BUNDLE`
Handoff Parent: `99dc95ddd682945bfa6936a7ca2391ff211393ec`

## Objective

Mechanically persist the ARCH-authored M3 Calibration Analysis Preview/Start contract. Do not perform calibration analysis and do not decide any Universal Level/Profile/Selector/routing result.

## Turn contract

The exact Handoff Parent is expected to be `TURN=REVIEW`. That is the valid pre-claim state, not a blocker. This architecture package is a single-task delegated claim authority bound to package/revision/parent/target ref.

Execution sequence:

`REVIEW -> exact State-only claim -> push+remote verify -> CODE -> exact seven-path final -> push+remote verify -> REVIEW`

The original user worktree may be dirty or behind. Preserve it. Fetch the remote and execute only in an isolated detached clean checkout at the fixed Handoff Parent. Do not pull/reset/clean/stash/rebase the host worktree.

## Exact final scope

Normal final is exactly seven paths: three adds and four modifications. Use the package manifest as authoritative path/blob truth.

## Mechanical gates

- remote target ref equals `99dc95ddd682945bfa6936a7ca2391ff211393ec` before claim;
- delegation/package identity matches exactly;
- claim changes State only and matches expected blob;
- normal final staged set is exact 7 paths / 3A+4M;
- all 7 staged blobs match manifest;
- Work-03 accepted corpus files and Probe response remain Preserve;
- accepted corpus boundary remains 21 rows / 12 clusters / 15 eligible / 6 context / 12 positive / 3 negative / 6 neutral;
- no Universal Level/Profile/Selector/model-ranking/routing conclusion appears;
- State contains no concrete model name;
- `git diff --cached --check` passes;
- final returns `TURN=REVIEW`.

## STOP boundary

Hard STOP only for remote parent mismatch, invalid package/delegation identity, semantic/preimage mismatch, inability to create safe isolation, unauthorized changed set, or other governance Truth failure. Helper/script/path/tool incompatibility is soft compatibility while an authorized equivalent adapter remains.

## Do not continue

After final push and remote verification, stop. Do not start Calibration Analysis Work-01, M4, M5 or CookBook Phase 3B.
