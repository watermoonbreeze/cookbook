# UBF M3 H4 Fallback Work-01 — Independent Challenge 01

> Overall disposition: `REWORK`
> Attribution: `ARCHITECTURE_RESEARCH_CANDIDATE_DEFECT / NON_CAPABILITY`
> Empirical effect: none; H4 and passive evidence wait preserved

## 1. Gate dispositions

| Gate | Result | Finding | Required disposition |
|---|---|---|---|
| C-01 Non-inference from H4 | PASS | Candidate states explicitly that it is not H2/H3 evidence. | Preserve. |
| C-02 No hidden scalar or total ordering | REWORK | Total ordering is forbidden, but “project floor” retains scalar language and domain-level state invites aggregation. | Replace floor with per-decision constraints; forbid counts/ratios/worst-state summaries as authority. |
| C-03 Domain completeness and overlap | REWORK | Seven M2 labels are CookBook-derived candidates; no extension, collision or multi-domain assignment rule proves universal completeness. | Mark taxonomy open/non-exhaustive; add stable extension and overlap rules. |
| C-04 BOUNDED_DELEGATION safety | REWORK | Capability sufficiency is required but the evidence predicate, authority and UNKNOWN behavior are not recorded fields. | Add capability-evidence reference, adjudicator, applicability conditions and UNKNOWN→no delegation. |
| C-05 Delegation Preservation / over-design | PASS | Fail-closed policy permits bounded discretion and requires justification when none remains. | Preserve; keep per-decision justification. |
| C-06 Q/STOP completeness | PASS | UNRESOLVED_STOP and required Truth/owner/consequence are explicit. | Preserve. |
| C-07 Orthogonality | REWORK | Risk/FULL-LITE/GC/routing are separated, but scalar “floor” wording can re-import a Level-like axis. | Use component-wise project constraints and additional decision records only. |
| C-08 Overlay non-redefinition | PASS | Overlay cannot redefine state meanings. | Preserve; clarify it may only add/close records. |
| C-09 Empirical coexistence/reversibility | PASS | Passive lane and later evidence replacement remain possible. | Preserve. |
| C-10 Mechanical inspectability | REWORK | Register fields are prose-only and domain/state cardinality conflicts are unresolved; no unique-ID or summary-view invariants. | Freeze a machine-checkable candidate schema and invariants. |

Canonical recount is PASS=5, REWORK=5, REJECT=0. Because any REWORK blocks freeze, overall result is `REWORK`.

## 2. Primary consistency defect

The candidate says a task receives one state per domain while also assigning a state to every Residual Decision Record. One domain can contain multiple decisions with different states. Collapsing them loses authority; choosing a worst/best/count rule silently creates an aggregate scale.

Exact correction: the authoritative unit is the stable Decision Record. Domain is a non-authoritative classification label. A task closure object is a set/map of Decision Records. Domain summaries are generated views only and must display mixed states without reducing them to one authoritative state.

## 3. Minimal Reopen Set

Only reopen candidate §§2–6 and §8 for:

1. authoritative-unit correction;
2. open/non-exhaustive taxonomy plus extension/overlap rules;
3. capability-evidence predicate fields and UNKNOWN default;
4. replacement of scalar floor wording;
5. machine-checkable record schema and invariants;
6. corresponding challenge-gate text.

## 4. Preserve Set

Preserve candidate purpose and H4 non-inference; the four state names and their meanings; fail-closed default; Q/STOP semantics; FULL/LITE, GC, risk and routing orthogonality; Overlay non-redefinition; reversibility; M2 legacy identities/mappings; all corpus/analysis/protocol evidence; passive wait; zero later-stage authority.

## 5. Exact Repair acceptance

Repair passes only when:

- every authoritative object is a uniquely identified Decision Record;
- a record has exactly one primary domain and zero-or-more secondary tags, with no double-count authority;
- taxonomy is explicitly open and extension requires stable identity, semantic definition, overlap audit and challenge;
- domain summaries are generated views and cannot drive routing, package selection or acceptance;
- `BOUNDED_DELEGATION` records include capability evidence reference, adjudicator, conditions and UNKNOWN fail-closed behavior;
- project overlay can add records or strengthen individual record states but no scalar floor;
- a JSON candidate schema defines required fields, enums, uniqueness and zero-aggregation invariants;
- all preserved gates remain byte/semantically unchanged outside Reopen Set.

Repair does not start M4. After repair ACCEPT, the candidate must be re-challenged or the repair review must explicitly close every challenge item before roadmap rewrite.
