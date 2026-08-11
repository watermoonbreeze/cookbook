# Phase 2D Conflicts

> Execution baseline: `4d551bd3`. No schema, validator, YAML parser, or production code was changed.

## Non-blocking conflicts

### Q-2D-001 · Historical checklist rows lack unique WorkItem mapping

- Source: `.ai-context/docs/feature/真机待验证清单_202608082330.md`
- Rows: `V2-1..V2-17`, `P0-1..P0-8`, `D1..D15`, `F1-1..F5-3`, `R1..R8`, `LEG-1..LEG-4`
- Disposition: `DEFER_VERIFY_UNMAPPED`
- Reason code: `WORKITEM_UNMAPPED`
- Evidence: these rows have local checklist IDs but no stable WorkItem declaration or unambiguous section-to-WorkItem ownership in the frozen Project Graph.
- Decision: retain source text in the checklist; do not fuzzy-map to K1g/K1c/I* and do not create placeholder WorkItems.
- Follow-up: Phase 2E or a separately reviewed WorkItem mapping batch.

## Retained orphan

`E-K1G-01` is an existing aggregate Verification absent from the current authoritative checklist. It is retained as `KEEP_EXISTING_VERIFY` because Phase 2D forbids automatic deletion of orphan existing entities. It is not used as a substitute for the 43 migrated source rows.

## Conflict status

No blocking conflict prevents the Phase 2D data bootstrap. Deferred rows remain open and prevent closure of any newly associated WorkItem beyond `verifying`.

