# UBF M3 H4 Fallback — Non-Ordinal Closure-Core Candidate 01

> Identity: architecture-research candidate, not canonical Universal Core
> Empirical disposition: `H4_INSUFFICIENT_EVIDENCE`
> Stage effect: M3 research only; M4–M8 remain not started

## 1. Why this candidate exists

Current evidence does not establish a useful one-dimensional Universal Level ladder and does not distinguish H2 from H3. The accepted roadmap therefore permits a reversible architecture-research candidate while naturalistic evidence remains under passive observation. This document does not infer multidimensional truth from H4; it defines a fail-closed design option for independent challenge.

## 2. Candidate identity

The candidate object is a `Closure Vector`, not a Level. A task receives one independent state per applicable decision domain. Two tasks are not ranked by counting closed domains, comparing state strings or calculating a scalar score.

Candidate domains reuse M2's provisional labels only:

1. `D-DECISION-SCOPE`
2. `D-EVIDENCE-TRACEABILITY`
3. `D-TRUTH-AUTHORITY`
4. `D-LIFECYCLE-CONCURRENCY`
5. `D-COLLECTION-IDENTITY`
6. `D-USER-SIDE-EFFECT`
7. `D-MECHANICAL-DELIVERY`

These are research-domain labels, not a finalized Task Profile schema. Future challenge may split, merge, rename or reject them without rewriting M2 legacy identities.

## 3. Per-domain states

| State | Meaning | Required proof | Coder authority |
|---|---|---|---|
| `NOT_APPLICABLE` | domain is outside the task after explicit applicability review | exclusion reason + affected-surface inventory | none in excluded domain |
| `ARCH_CLOSED` | architecture supplies the unique behavior-affecting answer | exact rule/target/forbidden set + validation | mechanical implementation only |
| `BOUNDED_DELEGATION` | coder may choose among explicitly safe alternatives | allowed set + equivalence boundary + evidence + Q/STOP trigger | choose only inside the frozen set |
| `UNRESOLVED_STOP` | safe authority cannot yet be assigned | unresolved question + consequence + owner + required Truth | no mutation in that domain |

The states are not numeric grades. `NOT_APPLICABLE` is not below or above another state. `UNRESOLVED_STOP` is a safety barrier, not a low Level. Cross-domain state aggregation into a total order is forbidden.

## 4. Residual Decision Register

Every applicable domain must expose:

- stable decision ID;
- domain;
- authoritative owner;
- current state;
- exact closed decision or bounded allowed set;
- forbidden choices;
- preconditions and invalidating conditions;
- evidence/validation;
- Q/STOP trigger;
- lifecycle and mutation authority.

Missing fields resolve to `UNRESOLVED_STOP`, never inferred delegation.

## 5. Fail-closed delegation rule

`BOUNDED_DELEGATION` is permitted only when all are true:

1. alternatives and their equivalence boundary are enumerated;
2. no alternative can change user-visible, persistence, protocol, security, authority or irreversible semantics outside that boundary;
3. required evidence is executable or independently reviewable;
4. actor capability evidence is sufficient for this decision type under the stated conditions;
5. failure has a precise Q/STOP route and cannot silently fall back.

If any condition is false or unknown, the state is `ARCH_CLOSED` when ARCH can decide from Truth, otherwise `UNRESOLVED_STOP`. This is a design-policy safeguard, not a capability ranking.

## 6. Orthogonal objects

- Risk/novelty constrains whether delegation is safe; it is not a domain state or Level.
- FULL/LITE selects the carrier needed to express and prove the vector; it cannot weaken closure.
- GC validates required closure/evidence and preserves stable identity; GC count never computes the vector.
- Actor routing remains owned by routing policy; the vector does not rank models.
- Project Overlay may set a stricter floor or close additional project decisions, but may not redefine state meanings.

## 7. Reversibility

The candidate is versioned and replaceable. M2 legacy mappings stay `UNRESOLVED`. Later accepted evidence may restore a level-based path, support a core-plus-residual structure, or reject this candidate. Migration must preserve Stable Identity, historical snapshots and acceptance evidence.

## 8. Independent challenge contract

The next challenge must issue PASS/REWORK/REJECT for each gate:

1. non-inference from H4;
2. no hidden scalar or total ordering;
3. domain completeness and overlap;
4. safety of `BOUNDED_DELEGATION`;
5. Delegation Preservation and over-design control;
6. Q/STOP completeness;
7. FULL/LITE, GC, risk and actor-routing orthogonality;
8. Overlay non-redefinition;
9. empirical-lane coexistence and reversibility;
10. mechanical inspectability of the Residual Decision Register.

Any REWORK/REJECT must provide a minimal Reopen Set, Preserve Set and exact repair. Challenge acceptance alone does not start M4. A separate roadmap rewrite, M3 disposition and explicit stage handoff are still required.

## 9. Frozen non-results

This candidate establishes zero Universal Level count/name/threshold/envelope/mapping decisions, zero final Task/Capability Profile decisions, zero production Selector decisions, zero model rankings/routing decisions, and zero new empirical events/runs/rows/reanalysis.
