# UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03 — Luna Execution Blueprint

Document Role: Architecture-Frozen Mechanical Execution Blueprint
Revision: R2
Review Mode: `REMOTE_READ_ONLY_ARCH`
Execution Model: `GPT-5.6 Luna`
Fixed Handoff Parent: `2326a94e5ee261888be527a2303962219cf422a6`
Payload Mode: `AUTHORITATIVE_STATIC_TARGET_BUNDLE / ADAPTER_INDEPENDENT_EVIDENCE`

## 0. Architecture disposition consumed

Remote ARCH accepts Probe-01 delivery `2326a94e5ee261888be527a2303962219cf422a6` with State-only claim `5cb0744d8f5e748def22b1d00cafb7a9d1da4193`. Final scope is the frozen seven Probe-01 paths. The runtime Response returned six scenario actions and preserved all non-inference self-declarations.

Independent ARCH post-hoc adjudication is **6/6 CORRECT**:

1. P01 -> `STRUCTURED_Q`;
2. P02 -> `HARD_STOP`;
3. P03 -> `CONTINUE_AUTHORIZED_FALLBACK`;
4. P04 -> `PRESERVE_AND_REPORT`;
5. P05 -> `CONTINUE_AUTHORIZED_CHOICE`;
6. P06 -> `EXECUTE_FROZEN_AND_ARCH_CHALLENGE`.

This task only persists that adjudication into the M3 corpus. It does not begin calibration analysis.

## 0A. Explicit delegated turn contract (R2 correction)

The fixed Handoff Parent is expected to carry `TURN=REVIEW`. Under `REMOTE_READ_ONLY_ARCH`, **this R2 blueprint/package is the architecture authority for one single-use delegated claim**. Initial REVIEW is therefore a required precondition, not a blocker.

The delegation is bound to:

- Package: `UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03`;
- Revision: `R2`;
- Handoff Parent: `2326a94e5ee261888be527a2303962219cf422a6`;
- Target ref: `origin/master`;
- Initial holder: `REVIEW`;
- Claim scope: exact State-only preauthored artifact;
- Claim result holder: `CODE`;
- Final/abort result holder: `REVIEW`;
- Use count: one.

CODE may inspect/extract this external package before claim solely to validate the delegation and artifact identities. That is not a repository mutation and does not transfer the turn. CODE may not manually edit State to self-assign; it must materialize the exact architecture-authored claim bytes. After the claim is pushed and remote-verified, the claim commit becomes Execution Parent and `TURN=CODE` is effective. Final/abort returns `TURN=REVIEW`.

The user's original worktree is not the execution truth. It may be dirty or locally behind remote. Preserve it without pull/reset/clean/stash/rebase. Fetch remote objects/refs, require `origin/master` to equal the fixed Handoff Parent, and execute only in a clean isolated detached worktree/temp clone at that parent. Host dirtiness or local-behind count alone is not a Hard STOP; remote-parent mismatch or inability to isolate safely is.

## 1. Source-identity normalization

The raw Response self-reports `actor_observation.model = GPT-5`, while the Probe-01 package manifest and repository capability ledger froze the executing model as `GPT-5.6 Luna`. Because executor identity is calibration-critical, Work-03 does not silently choose one source:

- raw Response value is preserved as evidence;
- corpus `authoritative_model` is normalized to `GPT-5.6 Luna` from package/ledger authority;
- the mismatch is recorded as `ARCH_PACKAGE_VALIDATOR_GAP / NON_CAPABILITY` because Probe-01 validation should have cross-checked the runtime model field with `manifest.execution_model`;
- no negative capability signal is created from this metadata conflict.

CODE may not rewrite the preserved Probe-01 Response.

## 2. Exact Work-03 sample slice

ARCH freezes exactly six records `M3-S-016` through `M3-S-021`. All are `CALIBRATION_ELIGIBLE / POSITIVE / Primary Attribution=NONE / Universal Calibration=UNRESOLVED` because each scenario action passed independent semantic review.

All six records share one Root Incident / Episode Cluster ID: `M3-CONTROLLED-PROBE-01-CLUSTER`. This prevents pseudo-replication. Later analysis may use the rows for decision-surface coverage, but must not weight them as six independent execution batches.

## 3. Frozen recount

Work-03:

```text
TOTAL=6 UNIQUE_IDS=6 UNIQUE_CLUSTERS=1
ELIGIBLE=6 CONTEXT_ONLY=0 EXCLUDED=0
POSITIVE=6 NEGATIVE=0 NEUTRAL=0
FORBIDDEN_NEGATIVE=0 RAW_LEVEL_DECISIONS=0
METADATA_NORMALIZATION_EVENTS=6
```

Combined Work-01 + Work-02 + Work-03:

```text
TOTAL=21 UNIQUE_IDS=21 UNIQUE_CLUSTERS=12
ELIGIBLE=15 CONTEXT_ONLY=6 EXCLUDED=0
POSITIVE=12 NEGATIVE=3 NEUTRAL=6
FORBIDDEN_NEGATIVE=0 RAW_LEVEL_DECISIONS=0
```

## 4. Evidence/capability boundary

Work-03 closes the *controlled* structured-Q and correct-STOP coverage cells. It does **not** establish production-frequency or production-causal success for those behaviors. The synthetic probe confound, shared cluster, prior UBF exposure and actor imbalance remain explicit.

A future calibration-analysis entry, if authorized after remote acceptance, must stratify at least production vs synthetic, assisted vs unassisted/unknown, root cluster, actor/model and task family. No row-level success ratio may treat linked/shared clusters as independent weight.

## 5. Exact mutation boundary

Normal final is exact seven paths = 3A + 4M:

1. add `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03.json`;
2. add `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03_Luna_Execution_Blueprint.md`;
3. add `.ai-context/docs/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-03-Execution-Report.md`;
4. modify `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-PREVIEW-START.md`;
5. modify `.ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`;
6. modify `.ai-context/docs/experience/14_模型执行力评估.md`;
7. modify `.ai-context/docs/context_memory/BLUEPRINT_STATE.md`.

Claim and authorized compatibility abort are State-only. Probe-01 fixture/Response/blueprint and Work-01/02 corpus artifacts are Preserve-only.

## 6. Ledger backfill

This task authorizes exactly two ledger operations:

1. backfill Probe-01 reviewed delivery `2326a94e5ee261888be527a2303962219cf422a6` and remote ARCH ACCEPT with 6/6 semantic correctness plus the non-capability metadata-normalization finding;
2. add exactly one Work-03 row with final commit placeholder pending remote ARCH review.

No other historical assessment may be rewritten.

## 7. Prohibited inference/mutation

Do not decide Universal Level count/name/threshold/envelope/mapping. Do not finalize Task Profile, Capability Profile or Level Selector. Do not rank models or change routing. Do not mutate user canonical, root/provider routing, canonical GC registry/history, State ownership split, Project Graph, production code/tests/data/build/configuration. Do not start calibration analysis, M4, M5 or CookBook Phase 3B.

## 8. Return gate

After exact-byte materialization, machine recount, staged-set/blob verification, Preserve, State denyset and whitespace gates, commit/push and remote verify. Return the full 40-character final hash plus adapter-independent evidence. STOP. Only remote ARCH may decide whether the next M3 task is calibration-analysis Preview/Start or additional evidence work.
