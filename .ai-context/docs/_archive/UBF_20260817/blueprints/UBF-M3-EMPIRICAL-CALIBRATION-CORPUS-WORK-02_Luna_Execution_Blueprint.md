# UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02 — Luna Execution Blueprint

Document Role: Architecture-Frozen Mechanical Execution Blueprint
Revision: R1
Review Mode: `REMOTE_READ_ONLY_ARCH`
Execution Model: `GPT-5.6 Luna`
Fixed Handoff Parent: `1be1afa1185570e67d7d23e965f6f42ea38724df`
Payload Mode: `AUTHORITATIVE_STATIC_TARGET_BUNDLE / ADAPTER_INDEPENDENT_EVIDENCE`

## 0. Architecture disposition consumed

Remote ARCH accepts Work-01 delivery `1be1afa1185570e67d7d23e965f6f42ea38724df`. Its claim is `e427375912532bf47b3571a1cc5a602db0a40b61`; final scope is the frozen seven Work-01 paths and the remote target blobs match the R1 authoritative manifest. Work-01 corpus truth is therefore accepted as evidence: 9 rows, 5 clusters, 5 eligible / 4 context, 4 positive / 1 negative / 4 neutral, raw Universal Calibration 9/9 `UNRESOLVED`.

This task consumes that acceptance and only broadens corpus coverage. It does not begin calibration analysis.

## 1. Exact Work-02 sample slice

ARCH freezes exactly six new records `M3-S-010` through `M3-S-015`:

- `M3-S-010` — DeepSeek V4 Pro, B4+B5+B6 first round: `CALIBRATION_ELIGIBLE / REWORK / EXECUTION_DEVIATION / NEGATIVE`. Negative cause is the explicit omission of mandatory new tests plus inaccurate completion marking. The AF-B456-05 specification gap is separately recorded as `BLUEPRINT_DEFECT` and is not the negative cause.
- `M3-S-011` — DeepSeek V4 Flash K1a: `CALIBRATION_ELIGIBLE / success / NONE / POSITIVE` under disclosed co-author/quality-review assistance.
- `M3-S-012` — DeepSeek V4 Flash L1: `CALIBRATION_ELIGIBLE / success / NONE / POSITIVE` under disclosed co-author/quality/copy review assistance.
- `M3-S-013` — DeepSeek V4 Flash K1i: `CALIBRATION_ELIGIBLE / scope escape / EXECUTION_DEVIATION / NEGATIVE`; code was functionally accepted, but unauthorized `CloudAiConsent.kt` mutation plus inaccurate allowlist-compliance self-report are independently established.
- `M3-S-014` — GPT-5 Phase 3A initial inventory: `CONTEXT_ONLY / PENDING_REVIEW / UNRESOLVED / NEUTRAL` because independent capability adjudication is not frozen in the source ledger.
- `M3-S-015` — GPT-5 GOV-BP-P3-01 initial delivery: `CONTEXT_ONLY / PENDING_REVIEW / UNRESOLVED / NEUTRAL` for the same evidence-authority reason.

CODE may not add the historical DeepSeek V4 Pro second-round row whose commit identity is incomplete in the ledger. CODE may not promote either GPT-5 row to eligible.

## 2. Assistance/confound rule

Legacy production commits that show `Co-Authored-By` or recorded quality/copy review are not treated as pure unassisted model trials. The exact contribution boundary is not reconstructed. They remain eligible only as **assisted-condition execution observations**, and later analysis must stratify them. This package does not subtract review help, invent a latent model-only score, or compare them directly with unassisted UBF rows.

## 3. Frozen recount

Work-02 slice:

```text
TOTAL=6 UNIQUE_IDS=6 UNIQUE_CLUSTERS=6
ELIGIBLE=4 CONTEXT_ONLY=2 EXCLUDED=0
POSITIVE=2 NEGATIVE=2 NEUTRAL=2
FORBIDDEN_NEGATIVE=0 RAW_LEVEL_DECISIONS=0
```

Combined Work-01 + Work-02:

```text
TOTAL=15 UNIQUE_IDS=15 UNIQUE_CLUSTERS=11
ELIGIBLE=9 CONTEXT_ONLY=6 EXCLUDED=0
POSITIVE=6 NEGATIVE=3 NEUTRAL=6
ACTORS/MODELS=GPT-5.6 Luna 9; DeepSeek V4 Pro 1; DeepSeek V4 Flash (1M context) 3; GPT-5 2
ELIGIBLE ACTORS/MODELS=GPT-5.6 Luna 5; DeepSeek V4 Pro 1; DeepSeek V4 Flash (1M context) 3
```

## 4. Coverage decision

Work-02 is required before calibration analysis because Work-01 was single-actor and governance/static-payload dominated. Work-02 adds production feature, actor/model diversity and scope-escape coverage. It still leaves structured-Q and correct-STOP absent, GPT-5 without eligible evidence, and assisted-review confounds in the legacy production rows.

Therefore Work-02 final state is **COMPLETE / PENDING REMOTE ARCH REVIEW**, not "corpus complete" and not "analysis ready". Only ARCH may decide after review whether a Work-03/challenge slice is required or a separate calibration-analysis entry can be designed.

## 5. Exact mutation boundary

Normal final is exact seven paths = 3A + 4M:

1. add `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02.json`;
2. add `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02_Luna_Execution_Blueprint.md`;
3. add `.ai-context/docs/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02-Execution-Report.md`;
4. modify `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-PREVIEW-START.md`;
5. modify `.ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`;
6. modify `.ai-context/docs/experience/14_模型执行力评估.md`;
7. modify `.ai-context/docs/context_memory/BLUEPRINT_STATE.md`.

Claim and authorized compatibility abort are State-only. Work-01 corpus/blueprint/report are Preserve-only and must retain their accepted blobs.

## 6. Ledger backfill

This task explicitly authorizes one historical backfill: Work-01 ledger row receives reviewed delivery `1be1afa1185570e67d7d23e965f6f42ea38724df` and `REMOTE ARCH ACCEPT (2026-08-13)` with the verified recount/blob facts. Then CODE adds exactly one Work-02 row with final commit placeholder according to the capability-evidence contract. No other historical ledger assessment may be rewritten.

## 7. Prohibited inference/mutation

Do not decide Universal Level count/name/threshold/envelope/mapping. Do not finalize Task Profile, Capability Profile or Level Selector. Do not mutate user canonical, root/provider routing, GC registry/history, State ownership split, Project Graph, production code/tests/data/build/configuration. Do not start M4, M5 or CookBook Phase 3B.

## 8. Return gate

After exact-byte final materialization, machine recount, staged-set/blob verification, Preserve, State denyset and whitespace gates, commit/push and remote verify. Return full 40-character final hash plus adapter-independent evidence. STOP. Do not start Work-03 or calibration analysis.
