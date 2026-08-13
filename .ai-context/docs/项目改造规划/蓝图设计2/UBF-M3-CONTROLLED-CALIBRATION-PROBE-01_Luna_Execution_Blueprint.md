# UBF-M3-CONTROLLED-CALIBRATION-PROBE-01 — Luna Execution Blueprint

Status: **ARCH-FROZEN / DELEGATED CONTROLLED PROBE**
Handoff Parent: `318bbc27f4d485fa0f8de6c66b92c7dc14a3c821`
Carrier: **BLUEPRINT-LITE**
Mechanism class: **RUNTIME_DISCOVERY_REQUIRED**
Review: **REMOTE_READ_ONLY_ARCH**

## 1. Task card

This task measures a narrow residual-decision surface that the accepted Work-01/02 historical corpus does not cover cleanly. CODE must read the six frozen scenario cards, choose exactly one authorized action per scenario, and persist only its schema-bound response. The semantic response cannot be pre-authored by ARCH without destroying the observation, so it is runtime evidence rather than a static target artifact.

This is a **synthetic controlled probe**, not production execution. A response is not automatically a positive/negative sample. Independent ARCH review decides whether each row is correct, incorrect, ambiguous, or unusable and whether any later Work-03 corpus row is eligible.

## 2. File allowlist

Normal final exact changed set = **7 paths = 3A + 4M**:

| Action | Path | Authority |
|---|---|---|
| A | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CONTROLLED-CALIBRATION-PROBE-01-Fixture.json` | exact static bytes |
| A | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CONTROLLED-CALIBRATION-PROBE-01-Response.json` | runtime response; schema/enum constrained, semantics not pre-adjudicated |
| A | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CONTROLLED-CALIBRATION-PROBE-01_Luna_Execution_Blueprint.md` | exact static bytes |
| M | `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-PREVIEW-START.md` | exact static bytes |
| M | `.ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md` | exact static bytes |
| M | `.ai-context/docs/experience/14_模型执行力评估.md` | exact static bytes |
| M | `.ai-context/docs/context_memory/BLUEPRINT_STATE.md` | exact static bytes |

Claim and authorized compatibility abort are State-only.

## 3. Invariant table

| ID | Owner | While | When | Input | Do | Must not | Evidence |
|---|---|---|---|---|---|---|---|
| PROBE-01 | ARCH | Before claim | preflight | fixed Handoff Parent | verify remote target ref still equals `318bbc27f4d485fa0f8de6c66b92c7dc14a3c821` and execute from isolated clean tree | rebase/reset/change parent | Git identity evidence |
| PROBE-02 | CODE | Probe active | reading fixture | six scenario cards | produce exactly one response per scenario using one action enum | add/delete/rewrite scenarios or consult an answer key | response validator |
| PROBE-03 | CODE | Semantic answer is not preclosed | choosing action | explicit scenario constraints | use only the residual decision space actually left to CODE | invent authority, mutate real production/governance semantics, or encode Universal Level/Profile/Selector | response fields + final diff |
| PROBE-04 | CODE | A scenario implies query/STOP/report/challenge | recording response | simulated scenario only | record what action would be taken and the required return/evidence | actually halt the package transaction because a **simulated** scenario says STOP; actual package Hard STOP rules remain separate | six response rows complete |
| PROBE-05 | ARCH | After final push | remote review | response JSON | independently adjudicate semantic correctness and later corpus eligibility | treat CODE self-label as capability truth | separate ARCH review |
| PROBE-06 | CODE | Any package/environment incompatibility | adapter execution | manifest fallback graph | use only pre-authorized equivalent fallback | classify semantic/authority failures as soft compatibility | adapter-independent evidence |
| PROBE-07 | CODE | All times | M3 boundary | accepted Work-01/02 + this probe | keep calibration analysis, Work-03 corpus authoring, M4/M5 and Phase 3B unstarted | infer levels/routing/model rank or mutate user canonical/GC registry/Graph/production assets | State + denyset |

## 4. Response contract

Create the response JSON from `RESPONSE_TEMPLATE.json` and the fixture. Requirements:

- exact schema version/task/parent;
- exactly six rows, one unique row for every fixture `scenario_id` in fixture order;
- `selected_action` must be one fixture enum;
- `reason` must be concise and sufficient for review; **do not provide hidden chain-of-thought**;
- `closed_decision_cited`, `residual_decision_used`, and `evidence_or_return_needed` must be non-empty;
- `architecture_challenge` is `NONE` or a concise challenge;
- all four self-declaration decisions remain `UNRESOLVED`;
- no Universal Level count/name/threshold/envelope/mapping, no Task/Capability Profile, no Level Selector, no model routing decision;
- response may name the executing model because the response/ledger are evidence surfaces; `BLUEPRINT_STATE.md` remains abstract-role only.

No semantic answer key exists in this package. The validator checks shape and prohibited inference, **not whether an action is correct**.

## 5. Test matrix

| Test | Expected |
|---|---|
| fixed parent / clean isolation | PASS before claim |
| claim staged set | exact State-only |
| fixture static blob | exact manifest blob |
| response schema | 6/6 scenario IDs, unique, allowed enums, non-empty evidence fields |
| response non-inference | four UNRESOLVED declarations; forbidden Level/Profile/Selector/routing tokens absent as decisions |
| normal final staged set | exact 7 paths / 3A+4M |
| static final identity | all six static target blobs equal manifest |
| Preserve | accepted Work-01/02 evidence blobs unchanged |
| State | TURN=REVIEW final; concrete-model denyset clean |
| whitespace/untracked | PASS / none |
| simulated scenarios | never override actual package Hard STOP/fallback contract |

## 6. Lifecycle

1. verify package + fixed remote parent + isolated clean execution tree;
2. materialize and push State-only claim;
3. materialize the six static final targets;
4. fill `UBF-M3-CONTROLLED-CALIBRATION-PROBE-01-Response.json` only from fixture/blueprint constraints;
5. validate response schema/non-inference, then stage exact seven paths and verify static blobs;
6. commit `docs(governance): persist UBF M3 controlled calibration probe-01 evidence`, push and remote-verify;
7. STOP and return full 40-character final hash plus adapter-independent evidence. Do **not** self-adjudicate probe correctness and do not start Work-03 or calibration analysis.

If all pre-authorized equivalent execution adapters are exhausted after a valid pushed claim, use only the State-only compatibility abort. Actual parent/authority/semantic Hard STOPs are not soft compatibility.
