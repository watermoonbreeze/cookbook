# Universal Blueprint Framework — M3 Preview / Start

Document Role: Generated View / Stage Entry Contract
Stage: `M3 — Empirical Calibration Corpus`
Entry Task: `UBF-M3-PREVIEW-START-01`
Exact Handoff Parent: `0cb6d95057485bebb088523a6fd44a7e5ef1c2a4`
Entry Status: `ACCEPT / REMOTE ARCH REVIEWED / CONSUMED BY UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01`
M0 / M1 / M2: `ACCEPT / CLOSED`
M2→M3 Handoff: `ARCH ACCEPTED / CONSUMED BY THIS ENTRY`
M3 Corpus: `WORK-01 COMPLETE / PENDING REMOTE ARCH REVIEW`
M4 / M5: `NOT STARTED`
CookBook Phase 3B: `NOT AUTHORIZED TO START`
Empirical Sample Rows Created By This Entry: `0`

## 1. Entry meaning

This document freezes the architecture contract that a later M3 corpus-work package must satisfy. It is a Generated View, not canonical Truth, and contains no empirical sample row. CODE does not choose the schema semantics, labels, samples, attribution or calibration conclusions.

M3 is an empirical **test** of whether a useful one-dimensional closure ladder exists. The answer may be “not supported by current evidence”; this entry must not bias the result toward a predetermined level count, name, threshold, envelope or mapping.

## 2. Corpus record schema — frozen for future M3 work

A future empirical record represents one bounded delegated execution episode and must carry all of the following fields. Field names are M3 observation schema only; they do not define final Task Profile, Capability Profile or Universal Level canonical objects.

1. `Sample ID` — M3-local stable identity, unique and immutable after acceptance.
2. `Root Incident / Episode Cluster ID` — links retries/reworks sharing one underlying task to prevent pseudo-replication.
3. `Source Task / Blueprint Identity` — exact task ID/revision and source role.
4. `Handoff / Execution Parent` and `Reviewed Delivery Identity` — exact Git identities when repository-backed.
5. `Review Disposition` — independent ARCH disposition and review evidence reference.
6. `Observed Task Shape` — architecture-authored descriptive dimensions; M2 TP candidates may be cited only as provisional observations.
7. `Observed Risk / Novelty Shape` — evidence context only; never a Universal Level proxy.
8. `Actor / Model Observation` — concrete execution identity belongs in evidence/corpus, never `BLUEPRINT_STATE` and never auto-promotes routing.
9. `Package Profile / Mechanism / Environment` — FULL/LITE, mechanism class, adapter/tool/OS where material.
10. `Assistance / Hidden Help` — prior hints, extra architecture intervention, retry guidance, context carry-over and other help.
11. `Architecture-Closed Decision Set` — decisions explicitly removed from coder discretion by authoritative payload/contract.
12. `Residual Coder Decision Set` — decisions intentionally left to the coder at task start.
13. `Reasonable Divergence Set` — alternate coder choices acceptable under the frozen contract.
14. `Unacceptable Divergence Set` — choices that would violate scope/truth/contract even if output appears functional.
15. `Observed Decision / Behavior` — what the coder actually chose or did, with direct evidence.
16. `Outcome Class` — success, REWORK, structured Q, correct STOP, incorrect STOP, scope escape, blocked/external or `UNRESOLVED` as independently adjudicated.
17. `Primary Defect Attribution` — `NONE`, `EXECUTION_DEVIATION`, `SOFT_COMPATIBILITY`, `COMPATIBILITY_EXHAUSTED`, `BLUEPRINT_DEFECT`, `ARCH_PAYLOAD_DEFECT`, `EXTERNAL_TRUTH_CHANGE` or `UNRESOLVED`.
18. `Secondary Attribution Events` — optional list; cannot override the independently adjudicated primary cause.
19. `Capability Signal Disposition` — `POSITIVE`, `NEGATIVE`, `NEUTRAL`, `EXCLUDED` or `UNRESOLVED`, with rationale.
20. `Evidence References` — direct repository/canonical/runtime/review evidence sufficient to reproduce labels.
21. `Evidence Independence` — identifies which evidence is independent review versus execution self-report/generated view.
22. `Confound Flags` — explicit values for the controls in §7.
23. `Coverage Tags` — only the dimensions in §8; no Universal Level label.
24. `Adjudication Status` — `ARCH_FROZEN`, `CHALLENGED`, `ACCEPTED`, `REWORK` or `UNRESOLVED` with challenge reference.
25. `Universal Calibration Disposition` — for raw corpus work this remains `UNRESOLVED`; candidate envelope/Level analysis is a later architecture step, not a CODE field.

No future Work-01 may silently omit a field by replacing it with prose elsewhere.

## 3. Sample eligibility

Every candidate episode is classified by ARCH as exactly one of:

- `CALIBRATION_ELIGIBLE` — bounded delegation, reconstructable decision closure/residual boundary, independent reviewed outcome, minimum evidence complete and confounds explicitly recorded;
- `CONTEXT_ONLY` — useful architecture history but insufficient for capability/calibration inference;
- `EXCLUDED` — duplicate/pseudo-replicated, authority/source identity unresolved, critical evidence absent, or not a delegated execution episode.

Eligibility rules:

- a GC row, file, checklist item or generated view is never itself an empirical sample;
- one execution episode cannot be duplicated merely because it contains multiple defects or files;
- linked claim/final/rework revisions of one underlying incident share a cluster identity; independence must be demonstrated before separate calibration weighting;
- success and failure may both be eligible; Q/STOP is not automatically failure and may be positive evidence when the contract required escalation;
- architecture-authored payload/Blueprint defects and compatibility events may remain eligible as `CONTEXT_ONLY` architecture evidence but cannot become coder-negative capability evidence;
- execution self-report without independent outcome evidence cannot be `CALIBRATION_ELIGIBLE`;
- unresolved source authority or irreversible attribution ambiguity forces `CONTEXT_ONLY` or `EXCLUDED`, never a guessed label.

## 4. Evidence minimum

Every `CALIBRATION_ELIGIBLE` sample requires all of these evidence facets:

- authoritative blueprint/task identity and immutable revision/source role;
- exact parent and reviewed delivery identity when repository-backed, or equivalent immutable external source identity;
- independent ARCH review disposition, not only CODE self-report;
- reconstructable Architecture-Closed and Residual Decision sets from pre-execution material;
- direct evidence of observed behavior/outcome and exact relevant scope;
- evidence sufficient to adjudicate defect attribution and capability-signal disposition;
- explicit assistance/hidden-help and environment/mechanism facts needed for confound review;
- evidence links that distinguish canonical Truth, acceptance snapshots, generated views and self-report.

A missing mandatory facet is not repaired by model reputation, routing tier, legacy Level, GC count, FULL/LITE profile or anecdotal recollection.

## 5. Labeling and adjudication authority

- ARCH authors and adjudicates all semantic labels, eligibility, defect attribution, capability-signal disposition, confound treatment and coverage interpretation.
- CODE may mechanically materialize architecture-frozen records and run deterministic recount/validation only.
- Independent challenge uses current user canonical, canonical GC registry, UBEA-v2 and the frozen M3 contract; design rationale is not pass evidence.
- A challenge disagreement stays `UNRESOLVED` until ARCH explicitly adjudicates it; CODE cannot break ties.
- `14_模型执行力评估.md` is an evidence source/ledger, not sole labeling authority and not routing Truth.
- Root/provider routing is orthogonal Actor/Capability Routing and remains Preserve-only in M3 entry/corpus work.

## 6. Defect attribution and compatibility classification

The UBEA-v2 causal distinction is mandatory:

- `EXECUTION_DEVIATION` may support a negative coder capability signal only when the authoritative payload closed the relevant decision, direct evidence shows CODE diverged, and material confounds do not better explain the result.
- `ARCH_PAYLOAD_DEFECT` and `BLUEPRINT_DEFECT` are architecture defects; they are never coder-negative samples.
- `SOFT_COMPATIBILITY` is an adapter/environment difference with an authorized equivalent path remaining; it is never a semantic/capability failure.
- `COMPATIBILITY_EXHAUSTED` means all authorized equivalent adapters were exhausted; it is not a coder capability negative by itself.
- `EXTERNAL_TRUTH_CHANGE` is an environment/repository/authority change and is not a coder capability negative.
- `NONE` indicates no defect; `UNRESOLVED` is required when causal evidence cannot close the label.

A “correct STOP” under a Hard STOP condition is positive contract-following evidence, not execution failure.

## 7. Confound controls

Each eligible record must explicitly capture and later analysis must control or stratify at least:

- actor/model/build/version where known;
- task family and observed complexity/risk/novelty shape;
- package profile and mechanism class;
- adapter/toolchain/OS/runtime constraints that materially affected execution;
- assistance, hidden help, architecture follow-up, retry guidance and context carry-over;
- blueprint/package revision and whether the episode belongs to a linked repair chain;
- prior exposure/learning/order effects for repeated actors on the same incident;
- evidence completeness and independence;
- reviewer/adjudication change when it could alter labels;
- source availability or external truth changes.

Comparisons that cross material confounds must be stratified or explicitly marked non-causal. Single incidents cannot establish a general actor/model or Level conclusion.

## 8. Coverage dimensions

Future corpus work must report coverage, without treating any dimension as a Universal Level proxy, across:

- multiple observed task families/decision domains;
- varied complexity, risk and novelty shapes;
- successful delivery, REWORK, structured Q, correct STOP, incorrect STOP, scope escape and blocked/external outcomes where evidence exists;
- `NONE`, `EXECUTION_DEVIATION`, architecture-defect, compatibility and external-truth attribution classes;
- assisted versus unassisted/unknown assistance states;
- different actor/model observations where available;
- FULL/LITE and applicable mechanism classes where available;
- direct versus weaker evidence-independence states;
- independent root incidents rather than only repeated revisions of one incident.

A missing coverage cell is reported as a gap, not filled by duplicating or relabeling a sample. M3 entry freezes no numeric Universal-Level or capability threshold from current history.

## 9. Programmatic recount contract

Every future corpus delivery must produce machine-readable recount evidence for at least:

- total rows and unique Sample IDs;
- unique root incident/episode cluster IDs and duplicate keys;
- `CALIBRATION_ELIGIBLE` / `CONTEXT_ONLY` / `EXCLUDED` counts;
- missing mandatory-field count;
- per-outcome and per-primary-attribution counts;
- capability-signal disposition counts;
- assistance/confound completeness counts;
- task-shape, actor/model, package-profile and mechanism coverage counts;
- count of `NEGATIVE` capability signals whose Primary Attribution is not `EXECUTION_DEVIATION` — **must equal 0**;
- count of raw corpus rows with a decided Universal Level/count/name/threshold/envelope/mapping — **must equal 0**;
- count of rows using Acceptance Snapshot/generated view/self-report as sole calibration Truth — **must equal 0**;
- linked-incident records lacking cluster identity — **must equal 0**;
- source/evidence reference failures — **must equal 0**.

Recount code is a verifier of architecture-frozen data, not authority to invent or repair labels.

## 10. M3 corpus acceptance gate

A future corpus Work package is eligible for remote ARCH acceptance only when all gates pass:

1. **Source/authority** — every calibration sample has stable source identity and independent review authority.
2. **Schema/completeness** — all records satisfy the frozen schema; missing/duplicate violations are zero.
3. **Attribution** — forbidden negative-signal/defect combinations are zero and unresolved causality stays explicit.
4. **Confound discipline** — material confounds and incident clustering are present; pseudo-replication is not treated as independent evidence.
5. **Coverage transparency** — required dimensions are recounted and gaps are visible rather than silently filled.
6. **Non-inference** — raw corpus contains no invented Universal Level count/name/threshold/envelope/mapping and no final Task/Capability Profile or Selector decision.
7. **Independent challenge** — canonical-requirement challenge and Self-Application pass against the frozen payload.
8. **Programmatic recount** — all mandatory zero-error counters are zero and row/coverage totals are reproducible.
9. **Preserve/non-scope** — user canonical, GC registry, routing, Project Graph, State ownership split, production assets and M4/M5/Phase 3B gates remain untouched.

Corpus acceptance is evidence acceptance only. It does not itself establish that a one-dimensional Universal Level exists. Any candidate closure-envelope/monotonicity analysis is a separate architecture decision after accepted corpus evidence.

## 11. Preserve and non-scope

Preserve M0/M1/M2 accepted outputs, all 48 GC identities/history/provenance, the 64-record M1 decomposition, user canonical, canonical GC registry, root/provider routing, Project Graph, State ownership split, production code/tests/data/build/configuration and unrelated documentation.

This entry does not authorize empirical rows, current-case retroactive labeling, Universal Level count/names/thresholds/envelopes/mappings, final Task Profile, final Capability Profile, Level Selector, canonical mutation, routing mutation, Project Graph repair, M4/M5 or CookBook Phase 3B.

## 12. Entry acceptance and next gate

This entry creates zero samples. Remote ARCH must first accept the `UBF-M3-PREVIEW-START-01` persistence delivery. Only then may ARCH issue a separate, architecture-frozen M3 corpus Work-01. CODE must not infer that this entry acceptance authorizes corpus construction on its own.

## 13. Remote ARCH acceptance and Work-01 consumption

Remote ARCH accepts the Preview/Start delivery `c07e4d582a485739144a38ed06267473596cadee` with chain `0cb6d950... -> 9cba9eda... -> c07e4d58...`, claim State-only, final exact seven paths, entry sample rows `0`, State denyset, Preserve, attribution gates and non-inference all intact.

That acceptance authorizes only the separately architecture-authored `UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01` payload. Work-01 creates exactly nine pre-adjudicated empirical records and leaves every raw `Universal Calibration Disposition` as `UNRESOLVED`. Work-01 is `COMPLETE / PENDING REMOTE ARCH REVIEW`; M3 calibration analysis has not started. M4/M5 and CookBook Phase 3B remain prohibited.

## 14. Remote ARCH acceptance of Work-01 and Work-02 coverage slice

Remote ARCH accepts `UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01` reviewed delivery `1be1afa1185570e67d7d23e965f6f42ea38724df` with claim `e427375912532bf47b3571a1cc5a602db0a40b61`. Exact final scope is seven paths and all seven repository target blobs match the Work-01 R1 authoritative manifest. Accepted Work-01 evidence remains 9 rows / 5 clusters / 5 eligible / 4 context / 4 positive / 1 negative / 4 neutral, with all raw Universal Calibration dispositions `UNRESOLVED`.

The accepted slice is insufficient to begin calibration analysis because it observes only one actor/model and is dominated by governance/static-payload tasks. ARCH therefore authorizes only `UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02`, a six-row coverage expansion. Work-02 adds production-feature observations, DeepSeek V4 Pro/Flash, scope escape, and explicitly represented review/co-author confounds. Two GPT-5 historical deliveries remain `CONTEXT_ONLY` because independent capability adjudication is incomplete.

Work-02 does not close the corpus. After Work-02 the combined corpus is frozen at 15 rows / 11 clusters / 9 eligible / 6 context, with 6 positive / 3 negative / 6 neutral and zero forbidden negative combinations. Structured Q and correct STOP remain uncovered. Calibration analysis, Level/Profile/Selector decisions, M4/M5 and CookBook Phase 3B remain unauthorized until a later ARCH decision.
