# Phase 2D Source Coverage

> Source: `.ai-context/docs/feature/真机待验证清单_202608082330.md`
> Execution baseline: `4d551bd3`
> Coverage target: every actionable checklist row has one disposition.

## Coverage totals

| Metric | Count |
|---|---:|
| TOTAL_SOURCE_ROWS | 98 |
| VERIFICATION_ROWS | 98 |
| MIGRATE_VERIFY | 43 |
| UPDATE_EXISTING_VERIFY | 2 |
| KEEP_EXISTING_VERIFY | 0 |
| DEFER_VERIFY_UNMAPPED | 53 |
| UNEXPLAINED | 0 |

`98 = 43 + 2 + 0 + 53`; all source rows are accounted for.

## Migrated / updated rows

| Source IDs | WorkItem | Feature | Kind | Source status | Target status | Required | Disposition |
|---|---|---|---|---|---|---|---|
| E-L1-01..12 | L1 | F-TOOLS | device | pending | pending | true | MIGRATE_VERIFY |
| E-K1I-01..02 | K1i | F-AI-MEAL | device | pending | pending | true | UPDATE_EXISTING_VERIFY |
| E-K1A-01 | K1a | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-CFG-01..06 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-B4-01..06 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-B5-01..10 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |
| E-B6-01..05, E-B6-VOICE-01, E-B6-TRUNC-01, E-B6-DOT-01 | K1g | F-AI-MEAL | device | pending | pending | true | MIGRATE_VERIFY |

## Deferred rows

The following source rows are real verification rows but have no unique WorkItem evidence in the frozen graph. They are explicitly deferred, not silently omitted:

`V2-1..V2-17`, `P0-1..P0-8`, `D1..D15`, `F1-1..F1-4`, `F2-1..F2-4`, `F3-1..F3-3`, `F4-1..F4-3`, `F5-1..F5-3`, `R1..R8`, `LEG-1..LEG-4`.

Disposition for every listed row: `DEFER_VERIFY_UNMAPPED`; reason: `WORKITEM_UNMAPPED`.
