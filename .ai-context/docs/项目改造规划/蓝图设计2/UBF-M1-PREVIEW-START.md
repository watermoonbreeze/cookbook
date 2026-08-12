# Universal Blueprint Framework — M1 Preview / Start

Document Role: Generated View / Stage Entry Contract
Stage: `M1 — Current-State Semantic Decomposition`
Entry Task: `UBF-M1-PREVIEW-START-01`
Exact Handoff Parent: `eb1bdc846b3f746dde80e8a1fec234f6434b411f`
Entry Status: `PERSISTED / PENDING REMOTE ARCH REVIEW`
M0 Status: `ACCEPT / CLOSED`
M0→M1 Handoff: `ARCH ACCEPTED / CONSUMED BY THIS ENTRY`
CookBook Phase 3B: `NOT AUTHORIZED TO START`

## 1. Entry Meaning

This document persists the M1 stage-entry contract after remote ARCH accepted the M0 End/Accept + handoff persistence chain.

It does **not** contain the M1 semantic decomposition itself and is not a substitute for canonical Truth. It defines what the next architecture-authored M1 work may analyze and what it must not pre-decide.

## 2. M1 Objective

M1 decomposes current governance into semantic objects before any policy rewrite.

Future architecture-authored M1 work must produce:

- a current-clause inventory;
- per-clause `Identity / Contract / Lifecycle / Snapshot / View` classification;
- a current-state map covering Level, GC, FULL/LITE, coder role, review and promotion;
- a contradiction / gap / preserve matrix against the target architecture;
- a list of CookBook-specific rules that belong to the project overlay.

## 3. Required Decision Vocabulary

Every candidate change produced by M1 must be labeled exactly one of:

- `PRESERVE`
- `REDEFINE`
- `MOVE`
- `SPLIT`
- `DEPRECATE-CANDIDATE`

Each classification must name its authority/evidence basis. `UNKNOWN` or an explicit conflict record is required when authority cannot yet be resolved; silence is not resolution.

## 4. Inherited Evidence Boundary

M1 inherits, without silently promoting generated views into canonical Truth:

- `universal_blueprint_framework_implementation_control.md`;
- `UBF-M0-Truth-Pack-b7fc77e4.md`;
- `UBF-M0-Truth-Pack-Supplement-169bb0a7.md`;
- `UBF-M0-FINAL-ACCEPT.md`;
- `UBF-M0-to-M1-Handoff.md`;
- accepted R2/R3/R4/R5 historical evidence;
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md`;
- `.ai-context/docs/experience/14_模型执行力评估.md`;
- repository/project sources catalogued by the M0 Truth Pack.

## 5. Frozen Non-Conclusions

M1 entry does not pre-decide:

- a Universal Level count;
- a CookBook Legacy L7 → Universal Level mapping;
- Universal Level semantics from GC count, risk, document size, or historical topic order;
- a model-routing conclusion from Luna samples;
- Project Graph lifecycle repairs;
- resolution of historical Phase 3A declaration conflicts.

CookBook Legacy L7 and GC-01～GC-48 remain historical identities/evidence until a later authorized mapping stage changes their metadata or status.

## 6. Architecture / CODE Split for M1

The semantic decomposition is an architecture judgment task.

The architecture model must read the inherited Truth/evidence, make classifications, resolve or expose authority conflicts, and produce the complete deterministic target payload.

Luna/CODE may later mechanically persist an architecture-authored M1 payload and run prescribed validation. Luna must not independently invent classifications, Universal Level mappings, canonical authority decisions, or project-overlay boundaries.

## 7. Schema for the First M1 Semantic-Decomposition Work Batch

The next separately authorized architecture work should emit clause-level records containing at least:

- stable clause/inventory ID;
- source path and source locator;
- source-declared role / authority;
- normalized semantic statement;
- semantic kind: `Identity / Contract / Lifecycle / Snapshot / View`;
- scope: universal candidate / project overlay / historical evidence / generated view;
- related Level / GC / FULL-LITE / coder / review / promotion concept, if any;
- contradiction or ambiguity references;
- evidence basis;
- decision category: `PRESERVE / REDEFINE / MOVE / SPLIT / DEPRECATE-CANDIDATE`;
- unresolved questions / STOP condition.

This schema is an entry constraint, not an inventory. This document classifies zero current clauses.

## 8. Entry Review Gate

Remote ARCH must verify this Preview/Start delivery before semantic decomposition is persisted.

Until that review returns ACCEPT:

```text
M1 PREVIEW/START: PERSISTED / PENDING REMOTE ARCH REVIEW
M1 SEMANTIC DECOMPOSITION: NOT YET EXECUTED
M2+: NOT STARTED
CookBook Phase 3B: NOT AUTHORIZED TO START
```

An ACCEPT authorizes a separate architecture-authored M1 semantic-decomposition work batch. It does not authorize M2 or CookBook Phase 3B.
