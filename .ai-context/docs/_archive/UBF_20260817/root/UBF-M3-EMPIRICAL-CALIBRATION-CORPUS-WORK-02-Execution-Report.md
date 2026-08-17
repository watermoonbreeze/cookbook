# UBF-M3-EMPIRICAL-CALIBRATION-CORPUS-WORK-02 Execution Report

Document Role: Repository-carried Mechanical Execution Evidence
Payload: `AUTHORITATIVE_STATIC_TARGET_BUNDLE`
Review: `REMOTE_READ_ONLY_ARCH`
Execution Model: `GPT-5.6 Luna`
Handoff Parent: `1be1afa1185570e67d7d23e965f6f42ea38724df`
Outcome: `COMPLETE / PENDING REMOTE ARCH REVIEW`

## Architecture input

Remote ARCH accepted Work-01 at `1be1afa1185570e67d7d23e965f6f42ea38724df` with claim `e427375912532bf47b3571a1cc5a602db0a40b61`. Work-01 exact seven target blobs matched its authoritative R1 manifest, and its 9-row recount/attribution/non-inference gates were accepted. Work-02 semantics were fully pre-adjudicated by ARCH before CODE execution.

## Mechanical persistence result

- exact six Work-02 observation rows persisted with the same ordered 25-field schema;
- Work-02: 6 IDs / 6 clusters / 4 eligible / 2 context / 0 excluded;
- Work-02 signals: 2 positive / 2 negative / 2 neutral;
- both negatives are `EXECUTION_DEVIATION`; forbidden negative count remains zero;
- combined Work-01+02: 15 IDs / 11 clusters / 9 eligible / 6 context / 0 excluded;
- combined signals: 6 positive / 3 negative / 6 neutral;
- production-feature and scope-escape coverage added;
- assistance/co-author/reviewer confounds explicitly retained;
- GPT-5 rows remain context-only because independent capability adjudication is incomplete;
- every raw Work-02 and preserved Work-01 Universal Calibration disposition remains `UNRESOLVED`;
- no Universal Level/Profile/Selector decision created;
- M4/M5 and CookBook Phase 3B remain not started/not authorized.

## Frozen Work-02 recount

```text
TOTAL 6
UNIQUE_SAMPLE_IDS 6
UNIQUE_EPISODE_CLUSTERS 6
CALIBRATION_ELIGIBLE 4
CONTEXT_ONLY 2
EXCLUDED 0
POSITIVE 2
NEGATIVE 2
NEUTRAL 2
NEGATIVE_NON_EXECUTION_DEVIATION 0
RAW_LEVEL_DECISION 0
SOLE_WEAK_TRUTH 0
LINKED_MISSING_CLUSTER 0
SOURCE_REFERENCE_FAILURE 0
MISSING_SCHEMA_RECORD 0
```

## Combined recount

```text
TOTAL 15
UNIQUE_SAMPLE_IDS 15
UNIQUE_EPISODE_CLUSTERS 11
CALIBRATION_ELIGIBLE 9
CONTEXT_ONLY 6
EXCLUDED 0
POSITIVE 6
NEGATIVE 3
NEUTRAL 6
DUPLICATE_SAMPLE_IDS 0
NEGATIVE_NON_EXECUTION_DEVIATION 0
RAW_LEVEL_DECISION 0
```

## Evidence and acceptance boundary

This report is architecture-authored before its containing final commit and therefore does not claim its own final hash. Remote ARCH must independently verify real parent→claim→final continuity, exact seven-path scope, target blobs, preserved Work-01 blobs, 6-row and combined recount, source-reference/adjudication logic, State denyset, whitespace and remote ref.

Work-02 acceptance would mean only that this second evidence slice is accepted. It does not authorize calibration analysis, Work-03, M4, M5 or CookBook Phase 3B.

## Gate

```text
M3 PREVIEW/START ACCEPT/CONSUMED
M3 CORPUS WORK-01 ACCEPT/CONSUMED
M3 CORPUS WORK-02 COMPLETE/PENDING REMOTE ARCH REVIEW
M3 CALIBRATION ANALYSIS NOT STARTED/NOT AUTHORIZED
M4/M5 NOT STARTED
PHASE 3B NOT AUTHORIZED TO START
TURN REVIEW
```
