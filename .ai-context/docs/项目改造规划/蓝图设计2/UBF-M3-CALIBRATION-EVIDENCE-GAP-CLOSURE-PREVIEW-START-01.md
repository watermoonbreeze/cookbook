# UBF M3 Calibration Evidence Gap Closure — Preview / Start 01

Document Role: Generated View / Evidence Acquisition Entry Contract
Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-PREVIEW-START-01`
Revision: `R1`
Handoff Parent: `bbd8bbbd5c97a9faef62fde50971a586322e625d`
Architecture Review Input: lifecycle repair `bbd8bbbd5c97a9faef62fde50971a586322e625d` = **ARCH ACCEPT / CONSUMED**
Current Analysis Disposition: `H4_INSUFFICIENT_EVIDENCE`
Empirical Rows Created By This Preview/Start: `0`
Evidence Acquisition Runs Started By This Preview/Start: `0`
Universal Level/Profile/Selector/Ranking/Routing Decisions: `0`
M4 / M5: `NOT STARTED`
CookBook Phase 3B: `NOT AUTHORIZED TO START`

## 1. Purpose

Analysis Work-01 correctly selected `H4_INSUFFICIENT_EVIDENCE`. This entry does not reinterpret the existing 21 rows or force a Level result. It freezes how the missing evidence may be acquired and the minimum conditions required before a later M3 re-analysis may begin.

The target is not “more rows”. The target is **independent, matched, attribution-clean evidence that can discriminate between H2 and H3 without reviving H1 by row counting**.

## 2. Frozen evidence gaps

| Gap | Current issue | Closure evidence required |
|---|---|---|
| EGC-G01 | actor/model imbalance | matched observations from at least two ARCH-normalized concrete coder identities |
| EGC-G02 | task-family / decision-axis crossover not matched | same frozen task family and package revision executed across the selected coder identities |
| EGC-G03 | assistance sensitivity | matched controlled lane must have no mid-execution ARCH hint/reviewer repair; any such episode is separated from the matched inferential stratum |
| EGC-G04 | synthetic dependence | controlled evidence must be stratified from naturalistic production evidence; synthetic success cannot substitute for production coverage |
| EGC-G05 | production `STRUCTURED_Q` coverage absent | at least one independently adjudicated naturalistic production-context episode where a structured question was the contract-correct behavior |
| EGC-G06 | production correct `HARD_STOP` coverage absent | at least one independently adjudicated naturalistic production-context episode where STOP was contract-correct |
| EGC-G07 | concrete actor identity provenance ambiguity | every new inferential episode requires runtime provenance and remote ARCH normalization before eligibility |
| EGC-G08 | pseudo-replication risk | retries/reworks sharing one underlying task remain one Root Incident / Episode Cluster for independence claims |

## 3. Two evidence lanes

### 3.1 Lane MC — Matched Controlled

ARCH must issue at least **two distinct controlled task families**. Each family must be consumable by at least **two distinct ARCH-normalized concrete coder identities** under the same frozen task truth/package revision.

Minimum matched matrix before re-analysis eligibility:

- 2 task families × 2 concrete coder identities;
- therefore at least 4 new independent root clusters;
- the same family must expose materially comparable residual decision space to each selected coder;
- outcome is not preconditioned: positive, negative or mixed evidence is all admissible after attribution;
- no row-count success-rate is allowed;
- one actor × one task family execution chain contributes at most one independent root cluster.

The package remains bound only to abstract actor `CODER`. Concrete model identity is runtime provenance and is normalized by remote ARCH after execution.

### 3.2 Lane NP — Naturalistic Production

Naturalistic evidence is **captured, not manufactured**.

Before a later re-analysis may claim the production coverage gaps are closed, the accepted corpus must contain:

1. at least one independently adjudicated production-context `STRUCTURED_Q` episode; and
2. at least one independently adjudicated production-context correct `HARD_STOP` episode.

A synthetic fixture, deliberately injected fake parent mismatch, or answer-key probe cannot satisfy EGC-G05/EGC-G06. If such natural episodes have not occurred, the gap remains open and H4 remains valid.

## 4. Assistance and provenance contract

For Lane MC inferential use:

- no mid-execution ARCH correction, answer hint, reviewer patch, or hidden handoff may occur;
- ordinary package text and pre-authorized fallback are not “assistance”;
- capacity/model substitution is allowed and is `SOFT_COMPATIBILITY / EXTERNAL_RUNTIME_CAPACITY / NON_CAPABILITY`;
- if a different concrete coder resumes a valid in-flight claim, provenance must record the segment boundary;
- raw self-reported model labels are evidence only;
- `authoritative_normalized_model` remains remote-ARCH authority;
- unknown or irreconcilable actor identity cannot satisfy the matched-identity gate.

## 5. Eligibility / attribution

A new episode can become `CALIBRATION_ELIGIBLE` only after remote ARCH independently closes:

- exact source task/package identity;
- Handoff/parent continuity;
- root-cluster identity;
- assistance/confound disclosure;
- actual decision/behavior evidence;
- defect attribution;
- concrete actor normalization;
- production-vs-controlled lane identity.

`ARCH_PAYLOAD_DEFECT`, `BLUEPRINT_DEFECT`, `SOFT_COMPATIBILITY`, `COMPATIBILITY_EXHAUSTED` and `EXTERNAL_TRUTH_CHANGE` remain non-negative capability classes. A negative capability signal still requires independently supported `EXECUTION_DEVIATION`.

## 6. Re-analysis entry gate

A future `M3-CALIBRATION-ANALYSIS-WORK-02` (name provisional) is **not authorized** until remote ARCH verifies all of the following:

1. Lane MC has a complete 2-family × 2-normalized-coder matrix = at least 4 independent matched root clusters.
2. All Lane MC cells use identical family truth/revision within each family.
3. Lane MC assistance parity is clean or confounded cells are excluded from the matched inferential stratum.
4. EGC-G05 has at least one accepted naturalistic production `STRUCTURED_Q` cluster.
5. EGC-G06 has at least one accepted naturalistic production correct-`HARD_STOP` cluster.
6. All new inferential clusters have remote-ARCH-normalized concrete actor identity.
7. Root-cluster recount shows no retry/rework pseudo-replication.
8. All raw Universal Level/Profile/Selector/routing decisions remain unresolved/zero.
9. Existing Work-01 analysis JSON and Work-01/02/03 corpus Truth are not rewritten; new evidence is additive with explicit provenance.
10. A fresh ARCH review explicitly signs the Evidence Gap Closure result as sufficient for re-analysis.

If any item is false, the correct state is **EVIDENCE GAP OPEN / H4 PRESERVED**.

## 7. What this Preview/Start authorizes

After this entry itself receives remote ARCH ACCEPT, ARCH may issue a separate `Evidence Gap Closure Work-01` package to acquire/persist specifically preselected evidence under this contract.

This entry does **not** itself:

- create any empirical corpus row;
- run a controlled probe;
- select concrete model names for future packages;
- choose a Universal Level count/name/threshold/envelope/mapping;
- finalize Task Profile, Capability Profile or Level Selector;
- rank models or recommend routing;
- start M4/M5;
- start CookBook Phase 3B.

## 8. Exit state

Expected repository state after mechanical persistence:

`UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-PREVIEW-START-01 = COMPLETE / PENDING REMOTE ARCH REVIEW`
`H4_INSUFFICIENT_EVIDENCE = PRESERVED`
`Evidence Gap Closure Work-01 = NOT STARTED / NOT AUTHORIZED`
`TURN = REVIEW`
