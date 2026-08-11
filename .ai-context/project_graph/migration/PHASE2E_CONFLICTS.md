# Phase 2E Conflict Ledger — R2

> Phase 2E R1 supersedes the generic 52-row blocked conclusion from the first reconciliation. Row-level ownership evidence is recorded in `PHASE2E_VERIFICATION_RECONCILIATION.md`.

## Resolved in R1

```text
Q-2E-R1-001
source: Phase 2E initial generic RETAIN_DEFERRED_BLOCKED set
status: RESOLVED
resolution: 52 rows individually mapped or backed by strictly proven missing WorkItems
evidence: PHASE2E_VERIFICATION_RECONCILIATION.md
```

```text
Q-2E-R1-002
source: F4-1..F4-3 ownership ambiguity
status: RESOLVED
resolution: all three map to BUG-AI-MEAL-001
evidence: Phase 2E-2-R1 §14-17; K2语音 Bug intent
```

```text
Q-2E-R1-003
source: F1/F2/R7 parser ownership gap
status: RESOLVED
resolution: BUG-AI-MEAL-002 created under frozen BUG-AI-MEAL-NNN convention
evidence: FIX-1/FIX-2 checklist rows + parser defect family evidence
```

```text
Q-2E-R1-004
source: F5-3 quantity-language ownership gap
status: RESOLVED
resolution: BUG-AI-MEAL-003 created under frozen BUG-AI-MEAL-NNN convention
evidence: FIX-5 checklist row + distinct quantity semantic gap
```

```text
Q-2E-R1-005
source: R6 K1f missing from Graph shard
status: RESOLVED
resolution: existing stable K1f restored to F-INGREDIENT; R6 mapped to it
evidence: 待办_功能算法.md#K1f and 待办总览.md#K1f
```

## Final status

```text
ARCHITECTURE_CHANGE_REQUIRED: 0
RETAIN_DEFERRED_BLOCKED: 0
UNKNOWN / FORGOTTEN: 0
Blocking Conflicts: 0
```

## Resolved in R2

```text
Q-2E-R2-001
type: OWNERSHIP_CONFLICT
status: RESOLVED
resolution: P0-2/P0-6/D11/F3-1/F3-2/F3-3 no longer use K1d as fuzzy Schema ownership; all map to K1g with parser/preview evidence.
```

```text
Q-2E-R2-002
type: WORKITEM_STATUS_CONFLICT
status: RESOLVED
resolution: implemented K1f, BUG-AI-MEAL-002 and BUG-AI-MEAL-003 with required pending device acceptance are `verifying`, not `backlog` or `done`.
```

```text
Q-2E-R2-003
type: SOURCE_GRAPH_ID_CONTRACT_CONFLICT
status: RESOLVED
resolution: Source IDs remain checklist identities; E-* values remain Graph VerifyIds; normalization is recorded one-to-one in the verification ledger without Schema changes.
```

## Resolved in R3

```text
Q-2E-R3-001
type: WORKITEM_STATUS_CONFLICT
work_item: BUG-AI-MEAL-001
previous_status: backlog
status: RESOLVED
resolution: implementation evidence is present at d6465b61ced1461a9f016902eda68b82d7fd8206; required E-F4-01/E-F4-02/E-F4-03 remain pending, so the frozen contract requires status verifying.
blocking_conflicts_remaining: 0
```
