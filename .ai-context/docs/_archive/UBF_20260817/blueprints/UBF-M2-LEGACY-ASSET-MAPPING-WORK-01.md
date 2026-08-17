# Universal Blueprint Framework — M2 Legacy Asset Mapping Work-01

Document Role: Architecture-authored M2 mapping artifact / generated analysis view

Stage: `M2 — Legacy Asset Mapping`

Handoff Parent: `c72a19b257550de7bb75dc9361b9f939fc220cb9`

Status: `ARCHITECTURE PAYLOAD FROZEN / PENDING CODE PERSISTENCE AND REMOTE ARCH REVIEW`

CookBook Phase 3B: `NOT AUTHORIZED TO START`

## 1. Scope and frozen interpretation

This artifact maps CookBook's 48 canonical legacy GC assets exactly once. It preserves their identity, history, recurrence and project provenance without treating the historical L1～L7 grouping as future Universal Level semantics.

Frozen rules:

- `Legacy Level` is the current project GC-coverage grouping only.
- `Universal Mapping` is not inferred from GC number, order, count, Legacy Level, risk, package size or actor tier.
- every row therefore keeps `Universal Mapping = UNRESOLVED` until later empirical calibration has an authorized Universal ladder;
- `Applicable Task Profile Candidate` is a provisional workload-shape label, not a final Task Profile schema and not a Universal Level;
- every GC remains an active legacy closure/evidence rule unless a later separately authorized migration explicitly supersedes it;
- `PRESERVE_IDENTITY + REDEFINE_LEVEL_RELATION` preserves the rule while rejecting the old implication that GC coverage defines Universal Level.

## 2. Record schema and controlled vocabulary

Each record contains the entry-contract fields: Stable Identity, Origin, Current Authority, Decision Category, Applicable Task Profile candidate, Closure Effect, Preserved Coder Discretion, Evidence Type, Legacy Level, Universal Mapping, Lifecycle Status and evidence/contradiction references.

| Field | Frozen meaning |
|---|---|
| Stable Identity | Canonical `GC-nn`; never renumbered by this migration. |
| Origin | Registry-carried defect/lesson that caused the rule to exist or expand. |
| Current Authority | CookBook canonical GC registry §12.3 at the fixed parent. |
| Decision Category | `PRESERVE_IDENTITY + REDEFINE_LEVEL_RELATION` for all 48 records. |
| Task Profile Candidate | Provisional task-shape family; candidate only. |
| Closure Effect | The decision/evidence surface the rule closes. |
| Preserved Coder Discretion | Choices still safely retained after the rule applies; `NONE` means no implementation-time semantic choice in that surface. |
| Evidence Type | Minimum independently inspectable evidence form. |
| Legacy Level | Exact current L1～L7 registry grouping. |
| Universal Mapping | `UNRESOLVED` for all records in Work-01. |
| Lifecycle | `ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION`. |

Task-profile candidate vocabulary:

| Candidate | Provisional meaning |
|---|---|
| `TP-DECISION-SCOPE` | behavior branches, invariants, scope and mutation boundaries |
| `TP-EVIDENCE-TRACEABILITY` | test, command, recount, challenge and review evidence |
| `TP-TRUTH-AUTHORITY` | writer ownership, protocol parity, registry and generated-view truth |
| `TP-LIFECYCLE-CONCURRENCY` | resource ownership, async identity and lifecycle state |
| `TP-COLLECTION-IDENTITY` | index spaces, collection projections and identity mappings |
| `TP-USER-SIDE-EFFECT` | user-visible automatic effects and high-frequency side effects |
| `TP-MECHANICAL-DELIVERY` | exact steps, literals, delivery ledger and frozen-value revision |

## 3. Complete mapping inventory

Common values omitted from repeated cells below:

- Current Authority: `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md §12.3 / <Stable Identity>`.
- Decision Category: `PRESERVE_IDENTITY + REDEFINE_LEVEL_RELATION`.
- Universal Mapping: `UNRESOLVED`.
- Lifecycle: `ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION`.

### 3.1 Legacy L1 — decision and scope closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-01 | BL-01: ambiguous cancel/retry/fallback branches | TP-DECISION-SCOPE | makes every behavior branch one condition→one action→one forbidden action | local syntax only; no branch semantics | branch table + ambiguity grep | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-01; M1 C-01/C-02/C-04 |
| GC-02 | BL-07: repair drift into UI/DI/protocol/dependencies | TP-DECISION-SCOPE | closes writable scope and explicit deny scope | implementation inside listed operations only | file×operation allowlist + changed-set audit | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-02; M1 P-05 |
| GC-03 | BL-01 variant: deferred items lost across batches | TP-DECISION-SCOPE | gives each inherited deferred item an explicit destination or rejection | wording of justified rejection; no silent deferral | deferred-item disposition ledger | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-03; recurrence=1 |
| GC-04 | BL-01 / missing test evidence for INV-B4-04 | TP-DECISION-SCOPE | closes invariant condition/result/forbidden-result/evidence | none in invariant semantics | five-column invariant row | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-04; M1 BP-12/BP-15 |
| GC-29 | B3.1 AF-B3-06: naked overwrite of same aggregate key | TP-DECISION-SCOPE | freezes merge-versus-replace semantics for multi-source aggregation | implementation algorithm inside frozen merge policy | aggregation policy + data-flow evidence | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-29 |
| GC-30 | B3.1 AF-B3-01/04/05 and AF-B456-04: partial side-effect chains | TP-DECISION-SCOPE | closes the complete companion-action chain per state branch | internal ordering only when behaviorally equivalent | per-branch companion-action checklist | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-30; recurrence=1; related GC-21 |
| GC-37 | AF-B456-05 / GOV-BP-P3-01: self-certified challenge | TP-EVIDENCE-TRACEABILITY | requires independent canonical-requirement coverage challenge before freeze | challenge presentation, not requirement selection | independent coverage ledger | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-37; M1 BP-24/BP-25 |
| GC-38 | Project Graph Phase 2E R1/R2/R3: narrow rework boundary | TP-DECISION-SCOPE | closes defect, cause, reopen, preserve, repair, regression and stop sets | repair mechanics inside Exact Repair | REWORK contract + zero-diff Preserve audit | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-38; M1 BP-30 |
| GC-39 | Phase 2 Final: over-broad freeze semantics | TP-DECISION-SCOPE | declares mutability separately for five governance semantic categories | none outside declared mutable categories | Mutation Declaration | L1 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-39; M1 BP-07/BP-09 |

### 3.2 Legacy L2 — evidence closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-05 | AF-B456-06: declared tests absent | TP-EVIDENCE-TRACEABILITY | closes both INV→T and T→INV coverage | choice of test organization; not missing mappings | bidirectional INV↔T matrix + orphan recount | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-05; M1 P-07 |
| GC-06 | AF-B456-06: zero-detail test claims | TP-EVIDENCE-TRACEABILITY | binds release gates to exact commands and current output | equivalent adapter only when command contract permits | command transcript + per-module counts | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-06 |
| GC-07 | BL-05: fake directly produced business terminal state | TP-EVIDENCE-TRACEABILITY | separates external-cause fixtures from production behavior and forbids sleep-based async proof | fixture implementation that respects boundary | fixture-responsibility table + async test | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-07 |
| GC-08 | AF-B456-08: device checks omitted from delivery | TP-EVIDENCE-TRACEABILITY | makes device evidence range part of delivery identity | scheduling of user-run check; not omission | device-check file/range ledger | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-08 |
| GC-09 | BL-06 / AF-B456-01: regression suite silently invalidated | TP-EVIDENCE-TRACEABILITY | freezes required regression suites and one-to-one replacement authority | additional tests beyond floor | regression baseline + assertion replacement map | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-09 |
| GC-34 | BL-11: repeated comment/KDoc drift | TP-EVIDENCE-TRACEABILITY | makes implementation-to-comment consistency inspectable | whether a truthful mismatch is blocking, but never omission | comment/KDoc diff audit ledger | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-34; recurrence=3; automation candidate |
| GC-40 | Over-No-Guess / Fuzzy Ownership | TP-TRUTH-AUTHORITY | blocks non-mechanical ownership or mapping without semantic responsibility evidence | selection only when authoritative disambiguation already proves it | source semantics + responsibility evidence | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-40; M1 BP-31 |
| GC-41 | Phase 2D manual counts lacked closure | TP-EVIDENCE-TRACEABILITY | makes identity-set and coverage counts reproducible | implementation language of recount | programmatic recount command + actual output | L2 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-41 |

### 3.3 Legacy L3 — truth-source closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-10 | BL-03 / AF-B456-01: overlapping state writers | TP-TRUTH-AUTHORITY | assigns one writer and complete readers/forbidden overwrites per field | internal implementation behind unique writer | per-field truth-source table | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-10; M1 C-10 |
| GC-11 | AF-B456-01/02: partial migration of overlapping state fields | TP-TRUTH-AUTHORITY | closes every old-field writer and terminal derive/replace/coexist disposition | migration mechanics within selected terminal form | repository writer grep + disposition map | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-11 |
| GC-12 | AF-B456-01: UI enabled and submit validation read different truth | TP-TRUTH-AUTHORITY | forces UI and business preconditions onto one field/derivation | rendering details | UI↔business predicate parity table | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-12 |
| GC-13 | BL-03: fallback invented separate validation semantics | TP-TRUTH-AUTHORITY | converts fallback to main internal type and reuses named validation entry | conversion implementation when semantics stay identical | named-entry data-flow proof + tests | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-13 |
| GC-27 | AF-B3-03→AF-B456-01: edit invalidation bypass recurred | TP-TRUTH-AUTHORITY | closes all edit entry points through canonical invalidation function | internal call placement only | entry-point routing audit | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-27; recurrence=1; mandatory review |
| GC-35 | AF-ARCH-01: sender event not consumed and internal warning leaked | TP-TRUTH-AUTHORITY | aligns protocol event enumeration with every receiver disposition | internal handler structure inside frozen dispositions | sender↔receiver enumeration matrix | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-35 |
| GC-42 | ae372d2: duplicate project registry | TP-TRUTH-AUTHORITY | discovers canonical registry before reuse/merge/replacement | none where authority overlaps unresolved | Canonical Registry Discovery table | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-42; M1 BP-32 |
| GC-43 | PROJECT.md copied volatile TURN/batch state | TP-TRUTH-AUTHORITY | limits stable entries to truth-role pointers instead of volatile value copies | stable explanatory text that does not duplicate current values | Truth Role / Pointer table + sibling scan | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-43; M1 BP-32 |
| GC-44 | Project Graph Final / Phase 3 generated views | TP-TRUTH-AUTHORITY | distinguishes authoritative source, derived view, edit policy and regeneration authority | view formatting only | source/view authority contract | L3 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-44; M1 BP-07/BP-08 |

### 3.4 Legacy L4 — ownership and lifecycle closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-14 | BL-02 / AF-B456-03: system resource not released | TP-LIFECYCLE-CONCURRENCY | assigns creator, holder, caller and release trigger for resource owners | internal release mechanism within frozen lifecycle | object lifecycle table + resource test | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-14 |
| GC-15 | AF-B456-03: value-passed mutable owner could not write back | TP-LIFECYCLE-CONCURRENCY | freezes mutable transfer as state, callback or hoist | naming/implementation within chosen form | ownership-transfer declaration | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-15 |
| GC-16 | AF-B456-03: historical paired cleanup lost during code move | TP-LIFECYCLE-CONCURRENCY | preserves paired logic and historical fixes across relocation | placement if every preserved item has exact landing | move inventory + before/after landing map | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-16 |
| GC-28 | BL-09 / AF-ARCH-02: singleton parser failed when cardinality became N | TP-LIFECYCLE-CONCURRENCY | forces cardinality review for construct-once mutable accumulators | partition implementation after cardinality answer is frozen | cardinality decision + lifecycle/data-flow evidence | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-28 |
| GC-31 | AF-B3-R2-01 / AF-B3-R3-01: stale async continuation wrote state | TP-LIFECYCLE-CONCURRENCY | enumerates suspension points and revalidates session/generation before writes | predicate implementation when identity semantics frozen | suspension-point ledger + stale-session tests | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-31; recurrence=1; mandatory review |
| GC-33 | B3 final S5: mutable global test replacement hook | TP-LIFECYCLE-CONCURRENCY | forbids post-construction global mutation and fixes constructor injection boundary | fixture values passed through approved constructor | injection-surface audit | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-33 |
| GC-45 | Project Graph work marked done before required verification | TP-LIFECYCLE-CONCURRENCY | separates implementation state, required verification, current and expected status | none in lifecycle result; evidence acquisition method may vary | lifecycle×verification evidence table | L4 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-45 |

### 3.5 Legacy L5 — index-space and collection-projection closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-17 | BL-08 / AF-B456-05: scalar count inferred item states | TP-COLLECTION-IDENTITY | represents item state as ordered `List<Status>` from data layer | UI rendering of supplied statuses | type/data-flow proof + per-item tests | L5 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-17 |
| GC-18 | AF-B456-05: business ordinal compared with display index | TP-COLLECTION-IDENTITY | names each integer index space/domain and freezes conversions | local variable form within declared spaces | type table + conversion expression | L5 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-18 |
| GC-19 | AF-B456-05: filtered collection consumed without explicit remap | TP-COLLECTION-IDENTITY | makes filter/sort/group projection and sole UI collection explicit | implementation of equivalent collection operators | collection projection data-flow | L5 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-19 |
| GC-36 | BL-12: three-value enum could not represent pending | TP-COLLECTION-IDENTITY | proves carrier value-space covers all distinguishable real states | carrier implementation after value-space is frozen | state-space cardinality table + tests | L5 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-36 |
| GC-46 | F4-1→E-F4-01: source identity confused with normalized identity | TP-COLLECTION-IDENTITY | separates source/target identities and proves deterministic 1:1 mapping without duplicate/collision | normalization implementation within rule | identity-layer map + 1:1/duplicate/collision audits | L5 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-46; M1 C-13 boundary analogous, not same object |

### 3.6 Legacy L6 — user-visible side-effect closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-20 | AF-B456-04: automatic truncation was silent | TP-USER-SIDE-EFFECT | freezes trigger, user carrier, exact copy, dedupe/reset and transparency tier for automatic effects | visual presentation inside frozen carrier/copy | automatic-side-effect table + UX evidence | L6 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-20 |
| GC-21 | AF-B456-04: invariant promised notice but script had no landing | TP-USER-SIDE-EFFECT | binds every visible-result invariant to an exact implementation step/location | internal wiring within named host | INV→STEP landing map | L6 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-21; related GC-30 |
| GC-22 | AF-B456-04 / B6: visible effect lacked verification | TP-USER-SIDE-EFFECT | requires at least one test or device item per visible effect | additional evidence beyond minimum | effect↔test/device mapping | L6 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-22 |
| GC-32 | BL-10: high-frequency delta triggered unbounded expensive work | TP-USER-SIDE-EFFECT | freezes trigger timing and throttle/dedupe behavior with invariant/test | equivalent implementation satisfying timing contract | timing policy + invariant + load/behavior test | L6 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-32; recurrence=1; mandatory review |

### 3.7 Legacy L7 — mechanically cancellable delivery closure

| ID | Origin | Task Profile Candidate | Closure Effect | Preserved Coder Discretion | Evidence Type | Legacy | Universal | Lifecycle | Evidence / contradiction refs |
|---|---|---|---|---|---|---|---|---|---|
| GC-23 | AF-B456-09: prose substep silently skipped | TP-MECHANICAL-DELIVERY | gives every minimal action a stable STEP, location, action and comparable completion form | code syntax when completion form remains exact | numbered STEP ledger | L7 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-23; M1 BP-04 |
| GC-24 | AF-B456-07 / AF-B456-05: ledger claimed absent changes/tests | TP-MECHANICAL-DELIVERY | binds each STEP status to real commit and diff location; requires referenced test existence | evidence formatting only | STEP→commit/diff/test-existence audit | L7 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-24; recurrence=1; mandatory review |
| GC-25 | AF-B456-09: dead branch shared same literal | TP-MECHANICAL-DELIVERY | freezes exact target literals and grep review criterion | placement only where behavior is unchanged | literal identity + repository grep | L7 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-25 |
| GC-26 | maxTokens 2048→4096 changed without traceable evidence | TP-MECHANICAL-DELIVERY | makes every frozen-value revision evidence-bound with impact assessment | new value only when evidence already authorizes it | frozen-value revision ledger | L7 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-26 |
| GC-47 | GOV-BP-P3-01: repair-only feedback did not improve Blueprint architecture | TP-EVIDENCE-TRACEABILITY | forces fixed attribution, escape, multidimension, propagation and over-design review sections | concrete improvement proposal, subject to ARCH adjudication | fixed-section AF/rework report | L7 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-47; M1 BP-26/BP-30 |
| GC-48 | Blueprint Governance R2: new rules were not applied to their own package | TP-EVIDENCE-TRACEABILITY | applies new/changed governance rules to current truth, state, registry and directly affected future designs | disposition only where audit finds an explicit propagation gap | six-column Self-Application Audit + sibling scan | L7 | UNRESOLVED | ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION | registry GC-48; M1 BP-29 |

## 4. Programmatic completeness and non-inference gate

Required post-materialization facts:

| Gate | Expected |
|---|---:|
| record rows matching `^\| GC-[0-9]{2} \|` | 48 |
| unique stable identities | 48 |
| minimum/maximum | GC-01 / GC-48 |
| missing from GC-01..GC-48 | 0 |
| duplicates | 0 |
| rows with `UNRESOLVED` | 48 |
| rows with active preserved lifecycle | 48 |
| Legacy assignment counts | L1=9, L2=8, L3=9, L4=7, L5=5, L6=4, L7=6 |
| Universal Level names or numeric decisions introduced | 0 |

The row count proves inventory completeness only. It does not prove a future Universal ladder and must not be used as a proxy for one.

## 5. Contradiction and authority handling

| Issue | Frozen disposition |
|---|---|
| Legacy protocol defines Lk by cumulative GC coverage | Preserve as historical mechanism; relationship to future Universal Level is REDEFINED. |
| Legacy L1～L7 theme order appears universal | Treat as CookBook legacy vocabulary/evidence, not direct Universal semantics. |
| GC identities and provenance are project canonical | Preserve without renumbering or copying authority into this generated view. |
| Task Profile schema does not yet exist | Keep profile labels explicitly candidate-only; M2 Work-01 does not establish selector semantics. |
| Universal ladder/count/names are not calibrated | Keep every mapping `UNRESOLVED`; do not guess. |
| User-level canonical migration is eventually required | Record as later authorized work; do not mutate canonical files in M2 Work-01. |

## 6. Preserve Set and non-scope

Preserve without semantic or byte mutation:

- the canonical GC registry and all GC-01～GC-48 identities, provenance, recurrence counters and current Legacy Level groupings;
- M1 64-record semantic decomposition, M1 Final Accept, M1→M2 Handoff and their evidence;
- user-level canonical files, project stable entry, Project Graph, production code, tests, data, build and configuration;
- UBEA-v2 frozen execution architecture.

Not authorized here:

- Universal Level count, names, thresholds, mapping or selector;
- final Task/Capability Profile schema;
- GC expansion, renumbering, recurrence mutation or canonical registry edit;
- user-level canonical rewrite, routing-source mutation or State responsibility split;
- M3 corpus/calibration work, M4 framework freeze, M5 verification or CookBook Phase 3B.

## 7. Work-01 outcome and next gate

Architecture result: **48/48 legacy assets mapped; identity/history/provenance preserved; Universal Mapping 48/48 explicitly UNRESOLVED; no semantic gap requires reopening M1's 64-record decomposition.**

After mechanical persistence and remote ARCH acceptance, M2 Work-01 may be closed. The next stage is not authorized by this artifact; M3 requires a separate End/Accept + handoff transaction and explicit Preview/Start.
