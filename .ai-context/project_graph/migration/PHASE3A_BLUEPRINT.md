# Phase 3A Blueprint — CODE Execution Record

## Baseline and Mutation Declaration

- Architecture baseline: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Graph mode remains `draft`.
- Stable identity, Graph contract, lifecycle state, CurrentWork, Graph data, and legacy views are immutable in 3A.
- Only new audit evidence, the two Phase 3 records, the execution ledger, and the handshake/model records are mutable.
- No renderer, generated view, legacy rewrite, SESSION rewrite, production-code, or Phase 3B change is authorized.

## Allowlist / Denylist

Allowlist: `migration/PHASE3_ARCHITECTURE_ACCEPT.md`, `migration/PHASE3A_BLUEPRINT.md`,
`migration/PHASE3A_AUDIT.md`, `docs/context_memory/BLUEPRINT_STATE.md`, and
`docs/experience/14_模型执行力评估.md`.

Denylist: Graph YAML/schema/tools, `PROJECT.md`, `SESSION_交接.md`, `PHASE2*.md`,
projectReview views, feature backlogs, production code, Gradle/config/runtime code,
renderer implementation, AI_INDEX generation, legacy migration, and mode activation.

## Truth Source Map

| Semantic | Authoritative source |
|---|---|
| Feature / WorkItem / Plan / Verification / Relation / CurrentWork | Project Graph |
| Phase 1/2 accepted architecture | Immutable migration records |
| Phase 3 architecture | `PHASE3_ARCHITECTURE_ACCEPT.md` |
| CODE/ARCH/REVIEW/TURN | `BLUEPRINT_STATE.md` |
| Concrete execution history | `14_模型执行力评估.md` |
| Existing Phase 2E drift | `PHASE2E_VIEW_DRIFT.md` |
| Handoff context | `SESSION_交接.md` |

## Independent Challenge Ledger

| ID | Result | Evidence |
|---|---|---|
| CH-3A-01 | PASS | One new Phase 3A audit is evidence only; Graph remains the sole project-state owner. |
| CH-3A-02 | PASS | `PHASE2E_VIEW_DRIFT.md` remains the only Phase 2E drift registry. |
| CH-3A-03 | PASS | Architecture ACCEPT is recorded separately; final 3A state is PENDING ARCH REVIEW. |
| CH-3A-04 | PASS | Counts are labeled fresh observations; no contract or lifecycle freeze is added. |
| CH-3A-05 | PASS | Final diff is restricted to the five-file allowlist. |
| CH-3A-06 | PASS | No renderer, generated view, or SESSION migration is performed. |
| CH-3A-07 | PASS | Deferred ledger below gives an exact Phase 3 owner for every open item. |
| CH-3A-08 | PASS | Ambiguous classifications are deferred with evidence and owner; no ownership is guessed. |
| CH-3A-09 | PASS | Acceptance is mechanically checked by validator, `pg check`, recount, and diff audits. |
| CH-3A-10 | PASS | No GC registry mutation; GC-48 self-application is recorded in the audit. |

## STEP Ledger

| Step | Result | Evidence |
|---|---|---|
| P3A-0.1 | PASS | Canonical inputs and user-level protocol were read; baseline is exact. |
| P3A-0.2 | PASS | CH-3A-01..10 resolved; blockers=0. |
| P3A-1.1 | PASS | Isolated worktree clean at baseline. |
| P3A-1.2 | PASS | Validator: 61 tests, 61 passed, 0 failed, 0 errors. |
| P3A-1.3 | PASS | `pg check`: OK; mode=draft; graph_version=1. |
| P3A-1.4 | PASS | Independent recount: 13/109/4/98/10; duplicates=0; dangling=0. |
| P3A-2.1 | PASS | Canonical registry discovery recorded in `PHASE3A_AUDIT.md`. |
| P3A-2.2 | PASS | Architecture accept record created. |
| P3A-2.3 | PASS | This execution blueprint created. |
| P3A-3.1..3.6 | PASS | Nine Phase 2E rows and mandatory candidates classified in the audit. |
| P3A-4.1..4.2 | PASS | Mutation audit and GC-48 self-application audit recorded. |
| P3A-5.1..5.4 | PASS | Model ledger, TURN=REVIEW, final allowlist audit, and `pg check` completed. |

## Deferred Item Ledger

| Item | Exact owner |
|---|---|
| Renderer Contract | Phase 3B |
| Renderer Implementation | Phase 3C |
| Generated Pilot | Phase 3D |
| Graph-owned Legacy Migration | Phase 3E |
| Functional Path / Hybrid Coverage | Phase 3F |
| SESSION Thin + AI Entry | Phase 3G |
| View Drift Closure | Phase 3H |
| Activation Readiness | Phase 3I |
| draft→active | Phase 3J |

## Acceptance Gate / STOP Gate

All CODE delivery gates A1–A23 pass in the accompanying audit. This record does not self-accept Phase 3A.
Final state is `EXECUTED / PENDING INDEPENDENT ARCH REVIEW`, `TURN=REVIEW`.
After commit and push, stop. Phase 3B and all later phases require a separate ARCH accept/handoff.

