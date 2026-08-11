# Phase 2D Inventory

> Execution baseline: `4d551bd3` (2026-08-11). Phase 2D keeps Graph `mode: draft`.

## Source

- Authoritative checklist: `.ai-context/docs/feature/真机待验证清单_202608082330.md`
- Actionable source rows: 98
- Current-source stable IDs: 45 `E-*` rows (including existing `E-K1I-01/02`)
- Historical rows without unique WorkItem evidence: 53 rows

## Existing Graph Verification inventory

| ID | Feature shard | WorkItem | Kind | Status | Required | Disposition |
|---|---|---|---|---|---|---|
| E-K1G-01 | F-AI-MEAL | K1g | device | pending | true | KEEP_EXISTING_VERIFY (orphan aggregate retained) |
| E-K1I-01 | F-AI-MEAL | K1i | device | pending | true | UPDATE_EXISTING_VERIFY |
| E-K1I-02 | F-AI-MEAL | K1i | device | pending | true | UPDATE_EXISTING_VERIFY |

## Current checklist reconciliation

| Source groups | Count | WorkItem | Disposition | Target shard |
|---|---:|---|---|---|
| E-L1-01..12 | 12 | L1 | MIGRATE_VERIFY | F-TOOLS |
| E-K1I-01..02 | 2 | K1i | UPDATE_EXISTING_VERIFY | F-AI-MEAL |
| E-K1A-01 | 1 | K1a | MIGRATE_VERIFY | F-AI-MEAL |
| E-CFG-01..06 | 6 | K1g | MIGRATE_VERIFY | F-AI-MEAL |
| E-B4-01..06 | 6 | K1g | MIGRATE_VERIFY | F-AI-MEAL |
| E-B5-01..10 | 10 | K1g | MIGRATE_VERIFY | F-AI-MEAL |
| E-B6-01..05, E-B6-VOICE-01, E-B6-TRUNC-01, E-B6-DOT-01 | 8 | K1g | MIGRATE_VERIFY | F-AI-MEAL |
| V2-1..17, P0-1..8, D1..D15, F1-1..F5-3, R1..R8, LEG-1..4 | 53 | — | DEFER_VERIFY_UNMAPPED | — |

## Closure snapshot

All newly migrated device verifications are `pending` because the source checklist has no recorded result. K1g, K1i, K1a and L1 remain `verifying`; no WorkItem is changed to `done`.
