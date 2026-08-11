# Phase 2E R1 Verification Ownership Reconciliation

> Baseline: Phase 2E implementation `719d47ee`. Source set is the 52 rows previously marked `RETAIN_DEFERRED_BLOCKED`; V2-1..17 and migrated E-* rows are excluded by contract. The authoritative checklist path remains unchanged.
> Schema compatibility: source checklist IDs remain the stable audit IDs in this ledger; Graph Verification entities use the frozen `E-*` ID convention (`P0-1 → E-P0-01`, `F4-1 → E-F4-01`, etc.) and retain the source ID in their source anchor/reason. No Schema or Validator change was made.

## Programmatic input and final math

```text
Previous RETAIN_DEFERRED_BLOCKED: 52
Rows reconciled: 52
MAP_TO_EXISTING_WORKITEM: 42
CREATE_MISSING_WORKITEM-backed rows: 10
CLASSIFY_HISTORICAL_VERIFY: 0
CLASSIFY_NON_CURRENT_VERIFY: 0
ARCHITECTURE_CHANGE_REQUIRED: 0
RETAIN_DEFERRED_BLOCKED: 0
UNEXPLAINED: 0
Math: 52 = 42 + 10 + 0 + 0 + 0
```

The input count is checked from the prior ledger, and the row set below is checked against the authoritative checklist groups. `source_status` remains `pending` for every row; implementation completion never becomes device `pass`.

## Evidence rules

Evidence search followed: authoritative checklist → original Bug/Todo/Blueprint → existing Graph WorkItem source and intent → implementation path where needed. `Guess: NO` for every row. F1/F2/R7 are backed by one newly explicit missing bug responsibility (`BUG-AI-MEAL-002`); F5-3 by `BUG-AI-MEAL-003`; R6 restores the already named stable `K1f` WorkItem.

## Row-level disposition

| ID | source_status | source meaning | candidate / evidence searched | final disposition | target / graph action | conflict |
|---|---|---|---|---|---|---|
| P0-1 | pending | absolute date must beat selected date | checklist P0-1; K1c source + intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| P0-2 | pending | invalid date/portion must be rejected | checklist P0-2; K1d source + Schema intent | MAP_TO_EXISTING_WORKITEM | K1d; add Verification | none |
| P0-3 | pending | existing meal merge requires confirmation | checklist P0-3; K15 source + controlled-confirm intent | MAP_TO_EXISTING_WORKITEM | K15; add Verification | none |
| P0-4 | pending | missing date uses selected anchor | checklist P0-4; K1c source + date intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| P0-5 | pending | multi-day AI parse | checklist P0-5; K1g source + cycle segmentation intent | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| P0-6 | pending | nullable optional schema fields | checklist P0-6; K1d source + compatibility intent | MAP_TO_EXISTING_WORKITEM | K1d; add Verification | none |
| P0-7 | pending | Chinese date/time rule fallback | checklist P0-7; K1c source + date intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| P0-8 | pending | cloud AI logs must not expose payload/key | checklist P0-8; L1 compliance source/intent | MAP_TO_EXISTING_WORKITEM | L1; add Verification | none |
| D1 | pending | selected-date anchor | checklist D1; K1c source + date intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| D2 | pending | weekday mapping within selected week | checklist D2; K1c source + weekdayHint intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| D3 | pending | parentheses become ingredients | checklist D3; I4 Bug source + intent | MAP_TO_EXISTING_WORKITEM | I4; add Verification | none |
| D4 | pending | save refreshes timeline | checklist D4; K1g source + meal flow | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| D5 | pending | multi-day output not truncated | checklist D5; I6 Bug source + token intent | MAP_TO_EXISTING_WORKITEM | I6; add Verification | none |
| D6 | pending | reverse weekday input maps target week | checklist D6; K1c source + weekdayHint intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| D7 | pending | multi-day save enters week plan | checklist D7; K1g source + cycle flow | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| D8 | pending | single-day save refreshes current date | checklist D8; K1g source + meal flow | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| D9 | pending | compound dish + parentheses | checklist D9; I4 Bug source + intent | MAP_TO_EXISTING_WORKITEM | I4; add Verification | none |
| D10 | pending | AI failure exposes rule fallback reason | checklist D10; I7 Bug source + controlled fallback intent | MAP_TO_EXISTING_WORKITEM | I7; add Verification | none |
| D11 | pending | AI/rule results share schema/date constraints | checklist D11; K1d source + compatibility intent | MAP_TO_EXISTING_WORKITEM | K1d; add Verification | none |
| D12 | pending | AI-created ingredient source and lookup | checklist D12; K1g code paths include IngredientAutoGenerator | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| D13 | pending | input copy/paste contract | checklist D13; K1g input path and source | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| D14 | pending | multi-day input template contract | checklist D14; K1g source + cycle input intent | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| D15 | pending | repeated confirmation is idempotent | checklist D15; K1g save path | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| F1-1 | pending | strip action phrase and split dishes | checklist FIX-1; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F1-2 | pending | parsed dishes show nutrition | checklist FIX-1; K1a blueprint/intent | MAP_TO_EXISTING_WORKITEM | K1a; add Verification | none |
| F1-3 | pending | parsed dishes save to meal | checklist FIX-1; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F1-4 | pending | action phrase is not ingredient | checklist FIX-1; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F2-1 | pending | strip “准备吃” | checklist FIX-2; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F2-2 | pending | strip “想吃” | checklist FIX-2; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F2-3 | pending | strip “打算吃” | checklist FIX-2; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F2-4 | pending | strip “要喝” | checklist FIX-2; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| F3-1 | pending | AI mode reaches valid preview | checklist FIX-3; K1d Schema source/intent | MAP_TO_EXISTING_WORKITEM | K1d; add Verification | none |
| F3-2 | pending | rule mode reaches equivalent preview | checklist FIX-3; K1d Schema source/intent | MAP_TO_EXISTING_WORKITEM | K1d; add Verification | none |
| F3-3 | pending | AI/rule result consistency | checklist FIX-3; K1d Schema source/intent | MAP_TO_EXISTING_WORKITEM | K1d; add Verification | none |
| F4-1 | pending | long press enters listen/waveform | checklist FIX-4; Bug K2语音; BUG-AI-MEAL-001 intent | MAP_TO_EXISTING_WORKITEM | BUG-AI-MEAL-001; add Verification | none |
| F4-2 | pending | release fills transcription | checklist FIX-4; Bug K2语音; BUG-AI-MEAL-001 intent | MAP_TO_EXISTING_WORKITEM | BUG-AI-MEAL-001; add Verification | none |
| F4-3 | pending | first use requests microphone permission | checklist FIX-4; Bug K2语音; BUG-AI-MEAL-001 intent | MAP_TO_EXISTING_WORKITEM | BUG-AI-MEAL-001; add Verification | none |
| F5-1 | pending | repeated AI dish does not duplicate | checklist FIX-5; K1g MultiDayRecorder/IngredientAutoGenerator path | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| F5-2 | pending | AI ingredient names are valid | checklist FIX-5; K1g IngredientAutoGenerator path | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| F5-3 | pending | vague quantity is not fixed half portion | checklist FIX-5; new parser quantity bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-003; add Verification | none |
| R1 | pending | preview must not write | checklist R1; K1g preview/confirm flow | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| R2 | pending | AI ingredient nutrition ref | checklist R2; K1a nutrition/estimate intent | MAP_TO_EXISTING_WORKITEM | K1a; add Verification | none |
| R3 | pending | pending-review ingredient tab | checklist R3; TODO-AI-MEAL-004 AI nutrition completion intent | MAP_TO_EXISTING_WORKITEM | TODO-AI-MEAL-004; add Verification | none |
| R4 | pending | weekday date regression | checklist R4; K1c source + intent | MAP_TO_EXISTING_WORKITEM | K1c; add Verification | none |
| R5 | pending | salt controlled default dose | checklist R5; J14 source + seasoning completion intent | MAP_TO_EXISTING_WORKITEM | J14; add Verification | none |
| R6 | pending | alias reuses ingredient | checklist R6; existing stable K1f source + alias intent | MAP_TO_EXISTING_WORKITEM | K1f; add Verification | none |
| R7 | pending | “宫保鸡丁” not split | checklist R7; new parser bug responsibility | CREATE_MISSING_WORKITEM | BUG-AI-MEAL-002; add Verification | none |
| R8 | pending | re-input clears stale state | checklist R8; K15 controlled session/fallback intent | MAP_TO_EXISTING_WORKITEM | K15; add Verification | none |
| LEG-1 | pending | nutrition visibility toggle | checklist LEG-1; K1a nutrition display intent | MAP_TO_EXISTING_WORKITEM | K1a; add Verification | none |
| LEG-2 | pending | AI new-dish marker | checklist LEG-2; K1g AI meal creation path | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| LEG-3 | pending | paste button | checklist LEG-3; K1g input path | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |
| LEG-4 | pending | multi-day preview | checklist LEG-4; K1g cycle segmentation intent | MAP_TO_EXISTING_WORKITEM | K1g; add Verification | none |

## New WorkItems

| ID | Why current | Why independent | Why existing WorkItems do not fit | Stable ID basis |
|---|---|---|---|---|
| BUG-AI-MEAL-002 | FIX-1/FIX-2 and R7 remain current pending acceptance rows | One coherent parser defect family with eight rows | I3/I4/I5 cover dish/parenthesis/connector symptoms but none owns action-phrase stripping plus the exact regression family | `BUG-<FEATURE>-NNN`, next unused AI-MEAL bug ID |
| BUG-AI-MEAL-003 | F5-3 remains current pending acceptance row | Quantity-language interpretation is independent of dish/connector parsing | Existing I3-I7/K15 intents do not own vague quantity semantics | `BUG-<FEATURE>-NNN`, next unused AI-MEAL bug ID |
| K1f | Existing stable ID is present in authoritative planning sources and implementation scope | Alias normalization is a distinct ingredient-ingestion responsibility | No existing F-INGREDIENT WorkItem owns alias-table reuse | Existing stable source ID retained |

All new WorkItems use `Guess: NO`; no Feature or Schema change was made.

## R2 Identity / Ownership Corrections

R1 rows are retained as historical evidence. The following corrections are the R2 disposition:

| Source ID | R1 disposition | R2 disposition | Primary Evidence | Final WorkItem | Graph VerifyId | Status |
|---|---|---|---|---|---|---|
| P0-2 | K1d / Schema boundary | remap to implemented AI meal parsing chain | checklist `#P0-2`; `2026-08-04_AI记一餐MVP算法审查与修复方案.md` | K1g | E-P0-02 | pending |
| P0-6 | K1d / nullable Schema | remap to implemented AI meal parsing chain | checklist `#P0-6`; `AI记一餐_周期记_NDJSON流式开发规范.md` | K1g | E-P0-06 | pending |
| D11 | K1d / AI-rule Schema | remap to implemented AI meal parsing chain | checklist `#D11`; `AI记一餐_周期记_NDJSON流式开发规范.md` | K1g | E-D-11 | pending |
| F3-1 | K1d / AI Schema | remap to implemented AI meal parsing/preview path | checklist `#FIX-3`; `AI记一餐_周期记_NDJSON流式开发规范.md` | K1g | E-F3-01 | pending |
| F3-2 | K1d / rule Schema | remap to implemented AI meal parsing/preview path | checklist `#FIX-3`; `AI记一餐_周期记_NDJSON流式开发规范.md` | K1g | E-F3-02 | pending |
| F3-3 | K1d / cross-platform compatibility | remap to implemented AI meal parsing/preview path | checklist `#FIX-3`; `AI记一餐_周期记_NDJSON流式开发规范.md` | K1g | E-F3-03 | pending |
| D12 | K1g / CodeMapping-led | retain with formal confirmation path evidence | checklist `#D12`; `AI记一餐_周期记_NDJSON流式开发规范.md` §6 | K1g | E-D-12 | pending |
| F5-1 | K1g / CodeMapping-led | retain with formal confirmation path evidence | checklist `#FIX-5`; `AI记一餐_周期记_NDJSON流式开发规范.md` §6 | K1g | E-F5-01 | pending |
| F5-2 | K1g / CodeMapping-led | retain with formal confirmation path evidence | checklist `#FIX-5`; `AI记一餐_周期记_NDJSON流式开发规范.md` §6 | K1g | E-F5-02 | pending |

### Identity audit

```text
Source IDs audited: 98 Graph Verification rows with source anchors
Graph VerifyIds audited: 98
Source -> Graph mapping: PASS (1:1)
Duplicate Source IDs: 0
Duplicate Graph VerifyIds: 0
Mapping collisions: 0
Schema changed: NO
```

### Status/provenance audit

`K1f`, `BUG-AI-MEAL-002`, and `BUG-AI-MEAL-003` are `verifying`: implementation evidence is present and their required device rows remain pending. BUG-002 provenance points to FIX-1/FIX-2 and the parser defect report; BUG-003 points to FIX-5 and the quantity-validation review. No `verifying -> done` promotion occurred.

### Observed / Acceptance Evidence

The full Project Graph test suite and `pg check` were executed for this commit and are recorded as observed acceptance evidence; they do not create Verification entities.
