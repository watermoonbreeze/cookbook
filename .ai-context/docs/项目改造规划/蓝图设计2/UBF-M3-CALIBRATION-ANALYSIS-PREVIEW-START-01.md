# UBF M3 Calibration Analysis — Preview / Start 01

Document Role: Generated View / Analysis Entry Contract
Stage: `M3 — Empirical Calibration Corpus`
Task: `UBF-M3-CALIBRATION-ANALYSIS-PREVIEW-START-01`
Handoff Parent: `99dc95ddd682945bfa6936a7ca2391ff211393ec`
Architecture Input: `WORK-03 = ARCH ACCEPT`
Analysis Status: `PREVIEW/START ONLY / NO UNIVERSAL LEVEL DECISION`
M4 / M5: `NOT STARTED`
CookBook Phase 3B: `NOT AUTHORIZED TO START`

## 1. Purpose

This entry freezes how the accepted M3 corpus may be analyzed. It does **not** analyze the corpus into a Universal Level ladder and does not authorize CODE to choose a level count, names, thresholds, envelopes, mappings, Task Profile, Capability Profile, Level Selector, model ranking, or routing rule.

The empirical question remains falsifiable: **does the accepted evidence support a useful predominantly one-dimensional execution-closure ladder at all?** Valid later outcomes include `SUPPORTED`, `PARTIALLY_SUPPORTED / MULTI-DIMENSIONAL_RESIDUAL`, `INSUFFICIENT_EVIDENCE`, and `NOT_SUPPORTED`.

## 2. Accepted evidence boundary at entry

The accepted corpus boundary is frozen as:

```text
TOTAL_ROWS 21
UNIQUE_SAMPLE_IDS 21
UNIQUE_ROOT_CLUSTERS 12
CALIBRATION_ELIGIBLE 15
CONTEXT_ONLY 6
EXCLUDED 0
POSITIVE 12
NEGATIVE 3
NEUTRAL 6
NEGATIVE_NON_EXECUTION_DEVIATION 0
RAW_UNIVERSAL_LEVEL_DECISION 0
```

The six Work-03 synthetic probe rows share one root cluster. They may provide six decision-domain coverage observations, but **must contribute at most one independent cluster unit** to any success-rate or confidence calculation.

## 3. Unit of analysis and weighting

Later analysis must operate in this order:

1. **Root cluster is the primary independence unit.** Rows inside one cluster are repeated/linked observations, not independent trials.
2. `CALIBRATION_ELIGIBLE` rows may enter inferential analysis; `CONTEXT_ONLY` rows may describe coverage/history only and must not numerically strengthen a capability claim.
3. No actor/model may gain weight merely by having more retries, more files, more defects, or more rows in one incident.
4. Synthetic controlled evidence and production evidence must be stratified before any combined interpretation.
5. Assistance/reviewer/context-carryover confounds must be reported before actor or closure-boundary inference.
6. Sparse actor/model strata cannot establish a population-level model ranking.

No default numeric weighting formula is frozen here. A later ARCH analysis package must either freeze one transparently or use qualitative/ordinal evidence with explicit uncertainty; CODE may not invent weights.

## 4. Analysis axes — evidence dimensions, not Universal Levels

Every later analysis must keep at least these axes separable before testing whether they collapse into one ladder:

- scope/truth discipline;
- STOP/Q/escalation judgment;
- fallback/compatibility judgment;
- residual implementation/mechanical choice;
- reasonable-divergence handling;
- architecture-challenge behavior;
- task complexity/risk/novelty shape;
- package closure/profile/mechanism;
- assistance/context carry-over;
- actor/model observation.

A one-dimensional ladder is supported only if materially different axes show a coherent monotonic closure relationship. Evidence that different actors/tasks cross over by axis is evidence **against** a simple one-dimensional ladder.

## 5. Candidate hypothesis family

Later ARCH analysis must evaluate all four hypotheses, not just the preferred one:

- `H1_ONE_DIMENSIONAL_CLOSURE_LADDER_SUPPORTED` — a predominantly monotonic ladder explains the observed residual-decision failures/successes without material cross-axis contradictions.
- `H2_ONE_DIMENSIONAL_CORE_WITH_SECONDARY_AXES` — a core closure ladder exists, but one or more orthogonal dimensions materially affect execution and must remain explicit.
- `H3_MULTI_DIMENSIONAL_ONLY` — evidence does not support collapsing relevant execution capability into one ordered ladder without losing important predictive distinctions.
- `H4_INSUFFICIENT_EVIDENCE` — current corpus cannot distinguish the above reliably.

The null-like safety default is **not** H1. If evidence is insufficient, the analysis must say so.

## 6. Required falsification / challenge tests

Before any Universal Level proposal, later analysis must challenge at least:

1. **Pseudo-replication test** — does the conclusion change when weighting by root cluster rather than row count?
2. **Synthetic dependence test** — does the conclusion survive when the synthetic Work-03 cluster is removed?
3. **Actor imbalance test** — is the claimed pattern driven almost entirely by one actor/model?
4. **Assistance sensitivity test** — do assisted/review-guided production observations alter the conclusion?
5. **Task-family crossover test** — do actors/closure patterns reverse across task families or decision axes?
6. **Negative-attribution purity test** — are all negative capability signals still independently attributable to `EXECUTION_DEVIATION`?
7. **Coverage-gap test** — would missing production structured-Q/correct-STOP evidence plausibly reverse the conclusion?
8. **Legacy contamination test** — is any inference accidentally using legacy L1–L7 labels or GC lifecycle as target truth?

Failure of any test must reduce confidence, split dimensions, or force `INSUFFICIENT_EVIDENCE`; it may not be hand-waved away.

## 7. Prohibited inferences

The following remain forbidden at Preview/Start:

- selecting Universal Level count, names, thresholds, envelopes or mappings;
- converting legacy L1–L7 or GC identities into Universal Level truth;
- producing model rankings or routing recommendations;
- treating `FULL/LITE` as a level label;
- treating risk/complexity as a scalar level proxy;
- counting Work-03's six rows as six independent successes;
- upgrading `CONTEXT_ONLY` into inferential weight;
- inferring causality across materially confounded actor/task/mechanism strata;
- modifying user canonical, root/provider routing, Project Graph, production code/tests/data/build/config;
- starting M4, M5 or CookBook Phase 3B.

## 8. Entry gate for future Analysis Work-01

A later `M3-CALIBRATION-ANALYSIS-WORK-01` may start only after remote ARCH accepts this Preview/Start package. That later package must be architecture-authored and must freeze:

- exact evidence inputs and their accepted blobs/identities;
- cluster-aware analysis procedure;
- confound strata and sensitivity checks;
- candidate hypothesis evaluation format;
- explicit uncertainty / evidence insufficiency rules;
- machine-checkable zero counters for forbidden Level/routing leakage unless the architecture package itself explicitly authorizes a later conclusion phase.

CODE's role in this Preview/Start is mechanical persistence only.

## 9. Current lifecycle

```text
M3 PREVIEW/START ACCEPT/CONSUMED
M3 CORPUS WORK-01 ACCEPT/CONSUMED
M3 CORPUS WORK-02 ACCEPT/CONSUMED
M3 CONTROLLED PROBE-01 ACCEPT/CONSUMED
M3 CORPUS WORK-03 ACCEPT/CONSUMED
M3 CALIBRATION ANALYSIS PREVIEW/START COMPLETE/PENDING REMOTE ARCH REVIEW
M3 CALIBRATION ANALYSIS WORK-01 NOT STARTED/NOT AUTHORIZED
M4/M5 NOT STARTED
PHASE 3B NOT AUTHORIZED TO START
TURN REVIEW
```


## 10. Remote ARCH acceptance and Analysis Work-01 authorization

Remote ARCH accepts this Preview/Start at reviewed delivery `5d6eda046be0b2a09f52059e438cb51f7db38e40`. The method contract is therefore **ACCEPT / CONSUMED** and authorizes only the separately architecture-authored `UBF-M3-CALIBRATION-ANALYSIS-WORK-01 / R1` static payload.

Work-01 is permitted to persist an evidence disposition after executing the frozen cluster-aware procedure and eight falsification tests. It is **not** authorized to define Universal Level count/names/thresholds/envelopes/mappings, finalize Task/Capability Profiles or Level Selector, rank models, recommend routing, start M4/M5, or start CookBook Phase 3B.

The ARCH-authored Work-01 result is `H4_INSUFFICIENT_EVIDENCE`; this is an evidence sufficiency disposition, not a Universal Level decision.


## 11. Analysis Work-01 reviewed delivery and repair boundary

Remote ARCH reviewed Analysis Work-01 delivery `b87726abc575a0c17cd1b76f663f242edbddc041` and accepts CODE execution fidelity. The frozen evidence disposition remains `H4_INSUFFICIENT_EVIDENCE`, with H1 not established and H2/H3 not distinguishable on current evidence.

A lifecycle-view propagation omission in the architecture-authored Work-01 package is classified `ARCH_PAYLOAD_DEFECT / NON_CAPABILITY`. This repair may update only generated/current lifecycle views and acceptance bookkeeping. It may not change analysis data, evidence labels, falsification verdicts, hypothesis disposition, or any forbidden-decision counter.
