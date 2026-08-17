# UBF M3 H4 Fallback — Non-Ordinal Closure-Core Candidate 01 / Exact Repair

> Identity: architecture-research candidate, not canonical Universal Core
> Empirical disposition: `H4_INSUFFICIENT_EVIDENCE`
> Stage effect: M3 research only; M4–M8 remain not started

## 1. Purpose and non-inference

Current evidence neither establishes a useful one-dimensional Universal Level ladder nor distinguishes H2 from H3. This reversible design-policy candidate is therefore not an H2/H3 empirical conclusion and cannot define a Universal Level, final Profile, production Selector, model ranking or routing rule.

## 2. Authoritative object

The authoritative unit is a stable `Decision Record`. A task closure object is a set/map of uniquely identified Decision Records. Each record has exactly one state. A domain is only a classification label; it never owns or collapses record state.

One domain may contain multiple records with mixed states. Domain summaries are Generated Views only. They must list the underlying record IDs and states without reducing them to a single status, worst/best state, count, percentage, score, rank, Level or routing input.

## 3. Open candidate taxonomy

The seven M2-derived seed domains are provisional and non-exhaustive:

1. `D-DECISION-SCOPE`
2. `D-EVIDENCE-TRACEABILITY`
3. `D-TRUTH-AUTHORITY`
4. `D-LIFECYCLE-CONCURRENCY`
5. `D-COLLECTION-IDENTITY`
6. `D-USER-SIDE-EFFECT`
7. `D-MECHANICAL-DELIVERY`

Every Decision Record has exactly one `primary_domain` and zero-or-more unique `secondary_domain_tags`. Secondary tags support discovery only and never create duplicate authority, coverage credit or acceptance credit.

A new `D-EXT-*` domain requires stable ID, semantic definition, inclusion/exclusion examples, overlap audit against every active domain, source/rationale, lifecycle state and independent challenge. Extension does not imply a new Level or taxonomy completeness.

## 4. Decision Record states

| State | Meaning | Required proof | Coder authority |
|---|---|---|---|
| `NOT_APPLICABLE` | decision is outside the task after explicit applicability review | exclusion reason + affected-surface inventory | none |
| `ARCH_CLOSED` | architecture supplies the unique behavior-affecting answer | exact rule/target/forbidden set + validation | only separately recorded safe implementation choices |
| `BOUNDED_DELEGATION` | coder may choose within an explicit safe set | allowed set + equivalence boundary + evidence + capability predicate + Q/STOP | only choices inside that record's allowed set |
| `UNRESOLVED_STOP` | safe authority cannot yet be assigned | unresolved Truth + consequence + owner + acquisition route | no mutation in that decision |

These names and meanings are preserved. They are non-numeric and have no cross-record or cross-domain total order.

## 5. Machine-checkable Residual Decision Register

Every record follows `UBF-M3-H4-FALLBACK-DECISION-RECORD-CANDIDATE-SCHEMA-01.json` and contains stable decision ID, primary domain, secondary tags, owner, state, applicability, decision contract, allowed and forbidden choices, preconditions, invalidating conditions, evidence references, Q/STOP contract, lifecycle, mutation authority and capability evidence.

Required invariants beyond JSON Schema syntax:

1. `decision_id` is unique in the task closure object.
2. every `primary_domain` and secondary tag resolves to a seed domain or a complete extension registry entry.
3. primary domain cannot repeat as a secondary tag; secondary tags are unique.
4. exactly one state exists per Decision Record; no authoritative domain state exists.
5. `BOUNDED_DELEGATION` requires capability status `SUFFICIENT`, nonempty adjudicator, evidence references and applicability conditions.
6. capability `UNKNOWN`, missing, stale or inapplicable always forbids `BOUNDED_DELEGATION`; ARCH must close the decision from Truth or use `UNRESOLVED_STOP`.
7. domain summaries, record counts/ratios and any `level/score/rank/floor/aggregate_state/coverage_ratio` field are non-authoritative and forbidden from selector, routing, package-profile or acceptance decisions.
8. every record has an explicit Q/STOP consequence and mutation authority.

## 6. Fail-closed bounded delegation

`BOUNDED_DELEGATION` is allowed only when alternatives and equivalence boundary are enumerated; no choice escapes user-visible/persistence/protocol/security/authority/irreversibility boundaries; validation is executable or independently reviewable; conditional actor capability evidence is adjudicated SUFFICIENT; and failure has a precise Q/STOP route without silent fallback.

If any predicate is false or UNKNOWN, delegation is forbidden. ARCH uses `ARCH_CLOSED` only when authoritative Truth supports a unique decision; otherwise the record is `UNRESOLVED_STOP`. Full closure across all records requires an explicit over-design/Delegation Preservation justification.

## 7. Orthogonal objects and Project Overlay

Risk/novelty constrains individual delegation decisions but is not a state or Level. FULL/LITE carries and proves the Decision Records without weakening them. GC validates named records/evidence and never computes state. Actor routing remains separately authoritative.

Project Overlay may add project-specific Decision Records or strengthen an individual existing record from bounded delegation to architecture closure when project Truth requires it. It may not define a scalar floor, aggregate domain state, Level, score or altered state meaning.

## 8. Reversibility and empirical coexistence

M2 legacy identities and mappings remain preserved/UNRESOLVED. Passive G05/G06 evidence observation remains active. Later accepted evidence may restore a level-based path, support another structure or reject this candidate. Migration preserves Stable Identity, lifecycle, snapshots and acceptance evidence.

## 9. Challenge closure

Exact repair addresses authoritative-unit consistency, taxonomy completeness/overlap, capability predicate, scalar-floor removal and mechanical schema. Remote ARCH must re-run all ten challenge gates. Repair delivery alone does not start M4 or authorize roadmap rewrite.

## 10. Frozen non-results

Universal Level/Profile/Selector/mapping/routing finalizations=0; new events/runs/rows/reanalysis=0; M4–M8 and CookBook Phase 3B remain not started/not authorized.
