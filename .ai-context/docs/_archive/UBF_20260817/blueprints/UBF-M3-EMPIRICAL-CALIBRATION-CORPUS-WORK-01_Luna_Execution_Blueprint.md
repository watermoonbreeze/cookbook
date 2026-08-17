# UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01 — Luna Execution Blueprint

Document Role: Repository-carried Mechanical Execution Contract
Task ID: `UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01`
Blueprint Revision: `R1`
Architecture Role: `REMOTE_READ_ONLY_ARCH`
Execution Mode: `EVALUATION / INDEPENDENT`
Payload Mode: `AUTHORITATIVE_STATIC_TARGET_BUNDLE / ADAPTER_INDEPENDENT_EVIDENCE`
Fixed Handoff Parent: `c07e4d582a485739144a38ed06267473596cadee`
Expected Return TURN: `REVIEW`
Normal Final Scope: `EXACT 7 FILES`

## 1. Architecture decision already closed

Remote ARCH accepts `UBF-M3-PREVIEW-START-01` at `c07e4d582a485739144a38ed06267473596cadee`. This Work-01 is the first authorized M3 empirical-corpus persistence batch. CODE does not select cases, label eligibility, adjudicate attribution, assign capability signals, repair sources, infer Universal Levels, or expand coverage. The exact nine architecture-frozen records in `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01.json` are the payload Truth.

Frozen recount: `TOTAL=9`, `UNIQUE_SAMPLE_IDS=9`, `UNIQUE_CLUSTERS=5`, `CALIBRATION_ELIGIBLE=5`, `CONTEXT_ONLY=4`, `EXCLUDED=0`, `POSITIVE=4`, `NEGATIVE=1`, `NEUTRAL=4`.

The only NEGATIVE row is `M3-S-002`, whose Primary Defect Attribution is independently frozen as `EXECUTION_DEVIATION`. `ARCH_PAYLOAD_DEFECT` and `COMPATIBILITY_EXHAUSTED` rows remain NEUTRAL/CONTEXT_ONLY. This is an attribution gate, not a ranking conclusion.

## 2. Exact transaction

Claim phase changes only `.ai-context/docs/context_memory/BLUEPRINT_STATE.md`. Final phase changes exactly:

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01.json`
2. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01_Luna_Execution_Blueprint.md`
3. `A .ai-context/docs/UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-01-Execution-Report.md`
4. `M .ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-PREVIEW-START.md`
5. `M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
6. `M .ai-context/docs/experience/14_模型执行力评估.md`
7. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No other path is authorized. Compatibility abort after a valid pushed claim is State-only.

## 3. CODE authority boundary

CODE MAY:
- materialize exact package bytes;
- select only a preauthorized equivalent execution adapter;
- stage/commit/push and collect Git evidence;
- run deterministic schema/recount/whitespace/denyset/Preserve validation.

CODE MUST NOT:
- add/remove/relabel a corpus record or change any 25-field value;
- reinterpret M3-S-002 or create another negative signal;
- convert CONTEXT_ONLY to CALIBRATION_ELIGIBLE;
- decide independence/weight of linked repair-chain episodes;
- fill coverage gaps by duplication, guessing, or importing ledger-only anecdotes;
- decide Universal Level count/name/threshold/envelope/mapping;
- finalize Task Profile, Capability Profile or Level Selector;
- mutate user canonical, GC registry, routing, State ownership split, Project Graph, production assets;
- start M4, M5 or CookBook Phase 3B.

## 4. Mandatory final semantic gates

The machine-readable corpus must verify:

```text
TOTAL_ROWS=9
UNIQUE_SAMPLE_IDS=9
DUPLICATE_SAMPLE_IDS=0
UNIQUE_EPISODE_CLUSTERS=5
ELIGIBLE=5 CONTEXT_ONLY=4 EXCLUDED=0
POSITIVE=4 NEGATIVE=1 NEUTRAL=4
NEGATIVE_NON_EXECUTION_DEVIATION=0
RAW_ROWS_WITH_DECIDED_UNIVERSAL_CALIBRATION=0
SOLE_WEAK_TRUTH_ROWS=0
LINKED_INCIDENT_MISSING_CLUSTER_ID=0
SOURCE_EVIDENCE_REFERENCE_FAILURES=0
MISSING_SCHEMA_RECORDS=0
```

All nine rows must expose exactly the frozen 25 observation fields. All `Universal Calibration Disposition` values must be `UNRESOLVED`. Coverage gaps must remain explicit.

## 5. Lifecycle after normal final

```text
M0/M1/M2 ACCEPT/CLOSED
M3 PREVIEW/START ACCEPT/CONSUMED BY WORK-01
M3 CORPUS WORK-01 COMPLETE/PENDING REMOTE ARCH REVIEW
EMPIRICAL CORPUS ROWS 9
UNIVERSAL LEVEL ANALYSIS NOT STARTED/NOT AUTHORIZED
M4/M5 NOT STARTED
COOKBOOK PHASE 3B NOT AUTHORIZED TO START
TURN REVIEW
```

Remote acceptance of this corpus is evidence acceptance only. It does not authorize CODE to continue into calibration analysis or a further M3 batch without a separate architecture-authored task.
