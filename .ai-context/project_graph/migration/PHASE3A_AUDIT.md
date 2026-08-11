# Phase 3A Audit — Baseline + View Inventory / Classification

## 1. Baseline and Protocol

- Exact baseline: `598daf4e5083d62038adfe39b1635993a7d90fa4`.
- Isolated execution worktree: clean, detached at the exact baseline.
- User-level `blueprint_protocol.md`: available and read; project fallback `12_多模型协作与实施蓝图规范.md` also read.
- Current Graph mode: `draft`; no activation performed.
- Current Graph observation: `feature=F-AI-MEAL`, `work_item=K1i`, `phase=verifying`, `blocker=`.
  `K1i` exists and belongs to `F-AI-MEAL`; governance work is not Product CurrentWork.

## 2. Fresh Validation Evidence

| Check | Actual result |
|---|---|
| Validator command | `python -m unittest test_validator -v` |
| Validator | 61 tests, 61 passed, 0 failed, 0 errors |
| `pg check` | OK; features=13, work_items=109, plans=4, verifications=98, relations=10; mode=draft; graph_version=1 |
| Independent recount | features=13, work_items=109, plans=4, verifications=98, relations=10 |
| Registry / feature files | 13 / 13 |
| Duplicate issues | 0 |
| Dangling issues | 0 |
| All graph issues | 0 |

Previous Phase 2 acceptance snapshot was `13 / 109 / 4 / 98 / 10`; fresh observation matches it.
This is an audit snapshot, not permanently frozen truth.

## 3. Canonical Registry Discovery

| Candidate | Existing role / overlap | Final disposition |
|---|---|---|
| `PHASE3_ARCHITECTURE_ACCEPT.md` | No current Phase 3 architecture accept record | New immutable architecture record |
| `PHASE3A_BLUEPRINT.md` | No current Phase 3A execution contract | New decision/execution record |
| `PHASE3A_AUDIT.md` | `PHASE2E_VIEW_DRIFT.md` owns Phase 2E drift | New batch audit evidence only |

`PHASE3A_AUDIT.md` is not the Canonical View Drift Registry, Project Truth, or Lifecycle State Ledger.
`PHASE2E_VIEW_DRIFT.md` is read-only in this batch and remains unchanged.

## 4. Repository View Classification Matrix

| Path / logical view | Exists | Current role | Project state? | Runtime narrative? | Execution state? | Truth owner | Current authority | Known drift | Target class | Future owner | Evidence |
|---|---:|---|---:|---:|---:|---|---|---|---|---|---|
| `docs/projectReview/07_项目现状.md` | YES | Human project-status snapshot | YES | YES | YES | Graph / Runtime by section | NO / PARTIAL | Can lag Graph | HYBRID_COVERAGE_AUDIT_REQUIRED | Phase 3H | Phase2E row 1 |
| `docs/功能路径索引.md` | YES | Navigation and code pointers | YES | YES | NO | Graph mapping + repository | NO / PARTIAL | Separately maintained | HYBRID_COVERAGE_AUDIT_REQUIRED | Phase 3F | Phase2E row 2 |
| `docs/feature/待办索引.md` | YES | Legacy backlog index | YES | NO | YES | Graph for lifecycle | NO / PARTIAL | Completion markers can differ | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase2E row 3 |
| `docs/feature/待办_Bug修复.md` | YES | Historical bug backlog | YES | YES | YES | Runtime / Graph by semantic field | NO / PARTIAL | Not a Graph registry | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase2E row 4 |
| `docs/feature/待办_功能算法.md` | YES | Human feature/planning backlog | YES | YES | YES | Graph for accepted plans | NO / PARTIAL | May lag accepted plans | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase2E row 5 |
| `docs/feature/待办_UI交互.md` | YES | Human UI backlog | YES | YES | YES | Runtime / Graph by semantic field | NO / PARTIAL | Human taxonomy has no deterministic kind mapping | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase2E row 6 |
| `docs/feature/待办_数据健康.md` | YES | Human health backlog | YES | YES | YES | Runtime / Graph by semantic field | NO / PARTIAL | Not authoritative for verification closure | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase2E row 7 |
| `docs/feature/待办_工程合规.md` | YES | Compliance checklist | YES | YES | YES | Accepted governance records / Graph | NO / PARTIAL | May retain stale markers | DEFERRED_WITH_EXACT_OWNER | Phase 3H | Phase2E row 8 |
| `docs/feature/待办_战略会商.md` | YES | Historical/product planning source | YES | YES | YES | Graph relation/ownership | NO / PARTIAL | L3 taxonomy is not Graph kind | DEFERRED_WITH_EXACT_OWNER | Phase 3E | Phase2E row 9 |
| `.ai-context/PROJECT.md` | YES | Stable entry and pointers | NO | NO | NO | This file only for navigation | YES for navigation | No current TURN/batch/lifecycle duplicate | STABLE_ENTRY_POINTER | Phase 3G | Direct read |
| `docs/context_memory/SESSION_交接.md` | YES | Transitional handoff context | YES | YES | YES | Graph / BLUEPRINT_STATE / decision records by section | NO | Contains copies and handoff narrative | THIN_HANDOFF_CANDIDATE | Phase 3G | Direct read; unchanged |
| `docs/context_memory/BLUEPRINT_STATE.md` | YES | CODE/ARCH/REVIEW/TURN handshake | NO | NO | YES | BLUEPRINT_STATE | YES | Must not be rendered back to Graph | EXECUTION_STATE_CANONICAL | Phase 3J | Direct read; updated only at handshake |
| AI_INDEX | ABSENT | No file exists | N/A | N/A | N/A | Future renderer output | N/A | Must not be created in 3A | ABSENT_TARGET | Phase 3D | Repository inventory |
| Plan View | ABSENT | No generated view | N/A | N/A | N/A | Graph | N/A | Future projection only | ABSENT_TARGET | Phase 3D | Repository inventory |
| Verification View | ABSENT | No generated view | N/A | N/A | N/A | Graph | N/A | Future projection only | ABSENT_TARGET | Phase 3D | Repository inventory |
| Current Work View | ABSENT | No generated view | N/A | N/A | N/A | Graph `current` | N/A | Future projection only | ABSENT_TARGET | Phase 3D | Repository inventory |
| Handoff View | ABSENT | No generated view | N/A | N/A | N/A | SESSION / Graph by section | N/A | Future thin view | ABSENT_TARGET | Phase 3G | Repository inventory |

## 5. Phase 2E Drift Carry-Forward Matrix

| Phase2E View | Phase2E Known Drift | Phase3A Classification | Future Phase | Carry-forward Status | Evidence |
|---|---|---|---|---|---|
| `projectReview/07_项目现状.md` | Snapshot can lag Graph | Hybrid status/runtime narrative | 3H | CLASSIFIED | Phase2E row 1 |
| `docs/功能路径索引.md` | Separately maintained navigation | Hybrid mapping/narrative | 3F | DEFERRED_WITH_OWNER | Phase2E row 2 |
| `待办索引.md` | Legacy completion markers differ | Legacy human taxonomy | 3E | DEFERRED_WITH_OWNER | Phase2E row 3 |
| `待办_Bug修复.md` | Historical bug list not registry | Legacy human taxonomy | 3E | DEFERRED_WITH_OWNER | Phase2E row 4 |
| `待办_功能算法.md` | Planning status can lag Graph | Legacy human taxonomy | 3E | DEFERRED_WITH_OWNER | Phase2E row 5 |
| `待办_UI交互.md` | Human UI taxonomy | Legacy human taxonomy | 3E | DEFERRED_WITH_OWNER | Phase2E row 6 |
| `待办_数据健康.md` | Not authoritative for verification closure | Legacy human taxonomy | 3E | DEFERRED_WITH_OWNER | Phase2E row 7 |
| `待办_工程合规.md` | Stale implementation markers possible | Governance narrative | 3H | DEFERRED_WITH_OWNER | Phase2E row 8 |
| `待办_战略会商.md` | L3 is historical/product planning | Legacy relation narrative | 3E | DEFERRED_WITH_OWNER | Phase2E row 9 |

No row is marked CLOSED because 3A performs no view migration.

## 6. Deferred Item Ledger

Renderer Contract→3B; Renderer Implementation→3C; Generated Pilot→3D; Graph-owned Legacy Migration→3E;
Functional Path / Hybrid Coverage→3F; SESSION Thin + AI Entry→3G; View Drift Closure→3H;
Activation Readiness→3I; draft→active→3J. No deferred item uses `later`, `future`, or `TBD` as owner.

## 7. Mutation and GC-48 Audits

| Audit | Result | Evidence |
|---|---|---|
| Graph semantic files | 0 mutation | Final diff against baseline has no Graph YAML/features/schema/tools changes. |
| Graph mode | Unchanged `draft` | Fresh `pg check`. |
| `PROJECT.md` / `SESSION_交接.md` | 0 mutation | Final allowlist audit. |
| `PHASE2E_VIEW_DRIFT.md` | 0 mutation | Final allowlist audit. |
| Renderer/generated view/legacy rewrite | 0 | No such files in allowlist or diff. |
| GC registry mutation | NONE | GC-01..GC-48 unchanged. |
| Snapshot broad freeze | PASS | Counts explicitly labeled observations. |
| Parallel registry | PASS | Only the three declared Phase 3 records created. |
| BLUEPRINT_STATE role/model separation | PASS | Handshake uses abstract role; model name only in execution ledger. |

## 8. Test Matrix and Delivery Gate

T-P3A-01/02/03/04/05/06/07/08/09/10/11/12/13/14/15: PASS.
The 23 CODE delivery conditions A1–A23 are satisfied: exact baseline, zero challenge blockers,
validator and graph checks pass, fresh counts are recorded, 9/9 drift rows are classified,
all candidates have an owner/class, diff is allowlist-only, and no Phase 3B work exists.

Final state: `Phase 3A EXECUTED / PENDING INDEPENDENT ARCH REVIEW`; `TURN=REVIEW`; `STOP`.

