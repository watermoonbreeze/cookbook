# Phase 2D Source Coverage

> Source: `.ai-context/docs/feature/真机待验证清单_202608082330.md`
> Execution baseline: `4d551bd3`
> Coverage target: every actionable checklist row has one disposition.

## Coverage totals

| Metric | Count |
|---|---:|
| TOTAL_SOURCE_ROWS | 114 |
| VERIFICATION_ROWS | 114 |
| MIGRATE_VERIFY | 43 |
| UPDATE_EXISTING_VERIFY | 2 |
| KEEP_EXISTING_VERIFY | 0 |
| DEFER_VERIFY_UNMAPPED | 69 |
| UNEXPLAINED | 0 |

`114 = 43 + 2 + 0 + 69`; all source rows are accounted for.

## Source status statistics

| Source status | Total | Deferred subset |
|---|---:|---:|
| pass | 17 | 17 |
| pending | 97 | 52 |
| fail | 0 | 0 |
| blocked | 0 | 0 |
| not_required | 0 | 0 |

## Migrated / updated rows

| Source IDs | WorkItem | Feature | Kind | Source status | Target status | Required | Disposition |
|---|---|---|---|---|---|---|---|
| E-L1-01..12 | L1 | F-TOOLS | device | pending | pending | true | MIGRATE_VERIFY |
| E-K1I-01..02 | K1i | F-AI-MEAL | device | pending | pending | true | UPDATE_EXISTING_VERIFY |
| E-K1A-01 | K1a | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-CFG-01..06 | K1a | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-B4-01..06 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-B5-01..10 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-B6-01..05, E-B6-VOICE-01, E-B6-TRUNC-01, E-B6-DOT-01 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |

## Deferred rows

The following 69 source rows are real verification rows but have no unique WorkItem evidence in the frozen graph. They are explicitly deferred, not silently omitted. `V2-1..V2-17` retain source status `pass`; all remaining deferred rows retain source status `pending`.

`V2-1`, `V2-2`, `V2-3`, `V2-4`, `V2-5`, `V2-6`, `V2-7`, `V2-8`, `V2-9`, `V2-10`, `V2-11`, `V2-12`, `V2-13`, `V2-14`, `V2-15`, `V2-16`, `V2-17`

`P0-1`, `P0-2`, `P0-3`, `P0-4`, `P0-5`, `P0-6`, `P0-7`, `P0-8`

`D1`, `D2`, `D3`, `D4`, `D5`, `D6`, `D7`, `D8`, `D9`, `D10`, `D11`, `D12`, `D13`, `D14`, `D15`

`F1-1`, `F1-2`, `F1-3`, `F1-4`, `F2-1`, `F2-2`, `F2-3`, `F2-4`, `F3-1`, `F3-2`, `F3-3`, `F4-1`, `F4-2`, `F4-3`, `F5-1`, `F5-2`, `F5-3`

`R1`, `R2`, `R3`, `R4`, `R5`, `R6`, `R7`, `R8`, `LEG-1`, `LEG-2`, `LEG-3`, `LEG-4`.

Disposition for every listed row: `DEFER_VERIFY_UNMAPPED`; reason: `WORKITEM_UNMAPPED`.
