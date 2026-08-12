# UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01 R4 — STATIC_TARGET_BUNDLE

ROLE_CONTRACT: You are CODE. You may execute only the package-authorized claim/final/abort artifacts. You must not classify clauses, rewrite the 64 records, resolve routing identity, design Universal Levels, edit user-level canonical files, expand allowlists, or start M2/Phase 3B. On Hard STOP, do not mutate. After a safely pushed final/abort, return TURN to REVIEW, return the full commit hash, and STOP.

Package ID: `UBF-M1-W01-R4-STB`

Handoff Parent: `795d2b9c807fe3954f1ac5f4cda60392c7ff9cc9`

Review mode: `REMOTE_READ_ONLY_ARCH`

Mechanism class: `STATIC_TARGET_BUNDLE`

Payload mode: `AUTHORITATIVE_STATIC_TARGET_BUNDLE / ADAPTER_INDEPENDENT_EVIDENCE`

Execution model: `GPT-5.6 Luna`

Normal final scope: exact 8 files

M2: `NOT STARTED`

CookBook Phase 3B: `NOT AUTHORIZED TO START`

## 1. Authoritative Payload

Execution Truth is `MANIFEST.json` plus `artifacts/claim`, `artifacts/final`, and `artifacts/abort`. R3 Python/bootstrap are not present and are not execution Truth. The R3 architecture result is preserved byte-for-byte inside the final artifact.

The optional Python adapter may copy and verify targets. Its failure is soft compatibility if a preauthorized native adapter is usable.

## 2. Pre-claim Hard Gates

Before mutation:

1. verify package with `python adapters/static_bundle_adapter.py package-check`; if Python cannot start, use native SHA-256/Git blob verification from `MANIFEST.json`;
2. verify raw hashes of the three external canonical files exactly as listed in the manifest;
3. record host HEAD and `git status --porcelain=v1 -z --untracked-files=all`; host dirty/HEAD mismatch is not a blocker and must not be changed;
4. `git fetch origin master` and verify `origin/master` equals the Handoff Parent;
5. create a clean isolated detached worktree/temp clone from the Handoff Parent;
6. verify the four `COMMIT_TREE_BOUND` preimage Git blobs and the initial semantic anchors;
7. verify expected initial `TURN=REVIEW`.

Hard STOP: remote parent changed; canonical continuity failed; TURN/delegation invalid; package/artifact identity failed; no safe isolation; preimage blob failed; unclosed semantic choice; push/remote verification impossible.

Soft compatibility: Python/version/console encoding/path separator/shell/temp-path/optional verifier failure. Follow only §6 fallback order.

## 3. Delegated State-only Claim

This Blueprint is the single-task, single-parent, single-use authorization for `REVIEW → CODE`.

In the isolated environment, copy the exact `artifacts/claim` tree over repository paths. Verify:

- only `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` changed;
- its SHA-256 and Git blob equal manifest claim expectations;
- tracked + untracked + index changed set contains no other path.

Commit exactly:

`docs(governance): claim UBF M1 semantic decomposition work-01`

Push `HEAD:master`; fetch; verify `origin/master == claim commit`. The verified claim is Execution Parent. Do not continue if remote verification fails.

## 4. Exact Normal Final Allowlist

Copy the complete `artifacts/final` tree over repository paths. Exactly these paths must differ from Execution Parent:

1. `A .ai-context/docs/UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01-Execution-Blueprint.md`
2. `A .ai-context/docs/UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01-Execution-Report.md`
3. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-SEMANTIC-DECOMPOSITION-WORK-01.md`
4. `A .ai-context/docs/项目改造规划/蓝图设计2/Universal-Blueprint-Execution-Architecture-v2.md`
5. `M .ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-PREVIEW-START.md`
6. `M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
7. `M .ai-context/docs/experience/14_模型执行力评估.md`
8. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No ninth path is allowed.

## 5. Normal Final Gates

Before commit, independently verify:

- every target SHA-256 and Git blob equals manifest;
- `git diff --check` PASS;
- machine changed-set uses NUL-delimited Git output; Git paths compare with `/` even on Windows;
- denyset is zero-diff;
- State contains no concrete model name and returns `TURN=REVIEW`;
- Architecture result contains exactly 64 stable records and required maps/matrices;
- root/codex MODEL_ROUTING relation remains `UNRESOLVED / DRIFT NOT ESTABLISHED`;
- no Universal Level count or Legacy/GC mapping was introduced;
- M2 remains NOT STARTED and Phase 3B remains NOT AUTHORIZED TO START;
- Execution Architecture v2 is present and points to Authoritative Payload, optional adapters, compatibility fallback and independent evidence.

Commit exactly:

`docs(governance): persist UBF M1 semantic decomposition work-01`

Push `HEAD:master`; fetch; verify `origin/master == final commit`; verify final parent is claim. Then return the full final hash and STOP.

## 6. Compatibility Fallback Graph

Use the first compatible adapter; all must produce identical blobs:

1. `OPTIONAL_PYTHON_ADAPTER`: `python adapters/static_bundle_adapter.py claim|final --repo <isolated>`.
2. `NATIVE_POWERSHELL_COPY`: recursively copy exact artifact tree with `Copy-Item -Force`, then use native Git/SHA verification.
3. `NATIVE_BASH_COPY`: recursively copy exact artifact tree with `cp -R`, then use native Git/SHA verification.
4. `ALTERNATE_NATIVE_FILE_API`: use the execution environment's exact-byte file copy API, then native Git verification.

If all normal adapters are exhausted after claim but exact abort artifacts can still be copied safely, apply `artifacts/abort`, verify exact two-file abort allowlist, commit `docs(governance): record UBF M1 work-01 compatibility block`, push/verify, return that hash and STOP. Abort is not completion.

If abort cannot be safely written/pushed or remote has changed, Hard STOP without remote overwrite.

## 7. Preserve / Deny Set

Zero-diff required for user-level canonical files, project GC registry, M0 Truth Pack/Supplement/Final/Handoff, all historical reports/blueprints outside the eight targets, `.ai-context/PROJECT.md`, `.ai-context/project_graph/**`, production code/tests/build/config/runtime files.

Do not repair State compaction, TURN enum, project fallback, routing sibling identity, Project Graph, or Phase 3A. These are M1 findings only.

## 8. Evidence Return

Normal/abort remote evidence must establish parent/claim/final chain, exact changed sets, target blobs, denyset, canonical hashes, semantic gates and remote equality. Do not rely only on adapter PASS text.

Architecture/payload/compatibility defects are not Luna capability negatives. Only a proven deviation from this complete executable package may be recorded as execution deviation.

