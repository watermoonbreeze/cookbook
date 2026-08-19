# Phase 2D Conflicts

> Execution baseline: `4d551bd3`. No schema, validator, YAML parser, or production code was changed.

## Non-blocking conflicts

### Q-2D-001 · Deferred checklist rows lack unique WorkItem mapping

- Source: `.ai-context/docs/feature/真机待验证清单_202608082330.md`
- Rows: 69 explicit IDs listed in `PHASE2D_SOURCE_COVERAGE.md` (17 pass, 52 pending)
- Disposition: `DEFER_VERIFY_UNMAPPED`
- Reason code: `WORKITEM_UNMAPPED`
- Evidence: these rows have local checklist IDs but no stable WorkItem declaration or unambiguous section-to-WorkItem ownership in the frozen Project Graph.
- Decision: retain source text in the checklist; do not fuzzy-map to K1g/K1c/I* and do not create placeholder WorkItems.
- Follow-up: Phase 2E or a separately reviewed WorkItem mapping batch.

### Q-2D-R1-001 · CFG ownership

- IDs: `E-CFG-01..06`
- Status: `CFG_WORKITEM_MAPPING / RESOLVED`
- Evidence: the formal K1a blueprint records `STEP-K1A-CFG-1~7` as completed in the prior AI→rule fallback batch, maps the corresponding acceptance to `T-CFG-01~03`, and records CFG as the prior K1a batch (`AI记一餐_K1a营养展示统一化与未配置报错_实施蓝图.md` §9).
- Decision: retain all six Verification IDs in `F-AI-MEAL`, change `work_item` from `K1g` to `K1a`.
- Guess used: no.

## Retained orphan

`E-K1G-01` is an existing aggregate Verification absent from the current authoritative checklist. It is retained as `KEEP_EXISTING_VERIFY` because Phase 2D forbids automatic deletion of orphan existing entities. It is not used as a substitute for the 43 migrated source rows.

## Conflict status

No blocking conflict prevents the Phase 2D data bootstrap. Deferred rows remain open and prevent closure of any newly associated WorkItem beyond `verifying`.

## Legacy classification

```text
EXISTING_VERIFY_NOT_IN_CURRENT_CHECKLIST
verification: E-K1G-01
classification: LEGACY_AGGREGATE
follow_up: Phase 2E
```
