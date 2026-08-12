# Universal Blueprint Framework — M0 → M1 Handoff

Document Role: Generated View / Stage Handoff
Parent Stage: `M0 — ACCEPT / CLOSED`
Child Stage: `M1 — Current-State Semantic Decomposition`
Handoff Status: `PERSISTED / PENDING REMOTE ARCH REVIEW`
M0 Accepted Review Target: `3489523db6508ba742ee835022d7e2a9a64f2c4f`
Persistence Task: `UBF-M0-END-ACCEPT-01`

## 1. Handoff Meaning

This document transfers the accepted M0 evidence boundary into the next-stage entry gate.

It does not become a second Truth source, does not modify the accepted M0 evidence, and does not start M1 by itself.

## 2. Inherited Truth / Evidence Inputs

M1 must treat the following as inherited inputs, subject to their documented roles:

- `universal_blueprint_framework_implementation_control.md`;
- `UBF-M0-Truth-Pack-b7fc77e4.md`;
- `UBF-M0-Truth-Pack-Supplement-169bb0a7.md`;
- `UBF-M0-FINAL-ACCEPT.md`;
- accepted R2/R3/R4/R5 historical reports and blueprints as historical execution evidence;
- `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` as the project fallback governance copy;
- `.ai-context/docs/experience/14_模型执行力评估.md` as the concrete model-execution evidence owner;
- repository/project sources catalogued by the M0 Truth Pack.

M1 must not silently promote a generated view or an acceptance snapshot into canonical Truth.

## 3. Preserve Set Entering M1

M1 Preview/Start must preserve:

- M0 `ACCEPT / CLOSED`;
- reviewed R5 delivery `3489523db6508ba742ee835022d7e2a9a64f2c4f`;
- the Stable Identity / Contract / Lifecycle State / Acceptance Snapshot / Generated View distinction;
- CookBook Legacy L7 and GC-01～GC-48 historical identities/evidence;
- the prohibition on defining Universal Level by GC count, risk score, document length, or historical L1～L7 topic order;
- the rule that project overlays may set a safe floor but may not redefine Universal Level semantics;
- historical R2/R3/R4 statuses exactly as reviewed;
- M0 Truth Pack/Supplement as accepted M0 evidence snapshots unless a later task explicitly opens an erratum path.

## 4. M1 Objective

M1 is **Current-State Semantic Decomposition**.

Its future authorized work is to decompose current governance into semantic objects rather than directly rewriting policy text.

Expected M1 outputs, when separately authorized, are:

- current-clause inventory;
- per-clause `Identity / Contract / Lifecycle / Snapshot / View` classification;
- current-state map for Level, GC, FULL/LITE, coder role, review and promotion;
- contradiction/gap/preserve matrix against the target architecture;
- CookBook-specific rules that must remain project-overlay material.

Every candidate change must ultimately be classifiable as:

```text
PRESERVE / REDEFINE / MOVE / SPLIT / DEPRECATE-CANDIDATE
```

with authority and evidence.

## 5. Carried Observations — Not Pre-Decided M1 Conclusions

The M0 evidence records Project Graph/Phase 3A lifecycle declarations that are not fully aligned. M1 may classify the authority/lifecycle meaning of those records if relevant to semantic decomposition, but this handoff does not pre-decide the resolution and does not authorize Project Graph mutation.

No Universal Level count is pre-frozen by M0.

No CookBook Legacy L7 → Universal Level mapping is pre-decided.

## 6. Entry Preconditions for M1 Preview/Start

A separate M1 Preview/Start batch is permitted only after all are true:

1. this `UBF-M0-END-ACCEPT-01` persistence delivery has been pushed;
2. remote architecture review verifies its exact parent/claim/final chain and allowlist;
3. remote architecture review returns `ACCEPT`;
4. the next M1 blueprint names that accepted persistence commit as its exact handoff parent;
5. the next batch performs its own TURN claim.

Until then:

```text
M1: NOT STARTED / NOT YET AUTHORIZED
CookBook Phase 3B: NOT AUTHORIZED TO START
```

## 7. Explicit Non-Scope

This handoff does not authorize:

- M1 semantic decomposition work in the current batch;
- M2 or later UBF stages;
- CookBook Phase 3B;
- production code/test/build/configuration changes;
- Project Graph state repair;
- user-level protocol mutation;
- model-routing conclusions from the current Luna sample.
