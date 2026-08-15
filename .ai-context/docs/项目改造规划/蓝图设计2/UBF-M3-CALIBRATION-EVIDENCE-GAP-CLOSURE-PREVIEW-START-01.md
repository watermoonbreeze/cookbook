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
Current Acquisition Status: `NP GAP REASSESSMENT ARCH ACCEPT / LANE MC COMPLETE / NP GAPS OPEN / PASSIVE WAIT ACTIVE`

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


## 9. Preview/Start remote ACCEPT and Work-01 authorization

Remote ARCH accepts this Preview/Start at `423d7382d56765e17ea9395e2b167454d5e1450f`.

The only acquired evidence now authorized is `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-WORK-01 / R1`:

- lane: `MATCHED_CONTROLLED`;
- Family Truth: `UBF-M3-EGC-MC-FAMILY-A/R1`;
- Family Truth SHA-256: `af32e947d7c21b4dce0ad9e012b0de28d865a0183f4aa53d13bc006ce45bf33b`;
- cell: `MC-A-CELL-01`;
- one concrete coder execution = at most one independent root cluster;
- four scenario responses are within-cluster observations, not four independent clusters;
- no answer key is included in the execution package;
- raw evidence only; ARCH correctness/actor normalization/attribution/eligibility remains pending;
- new empirical corpus rows = 0.

For matched equality, the stable comparison identity is `family_truth_id + family_truth_revision + family_truth_sha256`. Future Git transaction wrappers may have different Handoff Parents/revisions solely to persist later cells; they must reuse this exact Family Truth Capsule unchanged.

Cell-02 is not authorized by Work-01 itself. After Work-01 remote adjudication, ARCH may issue a second wrapper that requires a concrete coder identity distinct from the normalized Cell-01 actor while preserving the exact Family-A Truth Capsule.

All naturalistic production gates EGC-G05/EGC-G06 remain open until real production episodes occur.


## 10. Work-01 post-adjudication repair of the matched-acquisition protocol

Remote ARCH adjudicates Work-01 `d43c73fe12cfe3abd3a5b5efa7b5492b0487beca`:

- execution fidelity: ACCEPT;
- Family-A four scenario decisions: 4/4 semantic PASS;
- capability direction: NON-NEGATIVE;
- concrete actor normalization: UNRESOLVED from `UNKNOWN_SELF_REPORT / UNAVAILABLE`;
- matched-inference eligibility: NO;
- new empirical corpus rows: 0.

The earlier sequential acquisition design is insufficient for matched inference because Cell-01's raw Response entered canonical history before Cell-02. A later coder could inspect those choices, and "do not read it" is not an independently verifiable assistance-control mechanism.

This is an architecture-side `BLUEPRINT_DEFECT / NON_CAPABILITY`. Family-A is therefore `BURNED_BY_CANONICAL_RESPONSE_EXPOSURE` for future matched reuse.

The repaired protocol is `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-BLIND-ACQUISITION-PROTOCOL.md` (`BAP-01`). Future qualifying matched families B/C must use blind reveal bundles held outside the repo and canonical cryptographic commitments only until their same-family pairs are complete.

The original 2-family × 2-normalized-coder gate is preserved, not weakened. Family-A contributes zero qualifying matrix cells. Re-analysis remains unauthorized.


## 11. BAP-01 delivery review and State identity-hygiene repair

Remote ARCH reviews BAP-01 repair `15b3470703b3df0f1f7dcae8a815b3f660463f0c`:

- CODE execution fidelity: ACCEPT;
- blind commitment/reveal protocol: preserved;
- new evidence runs: 0;
- new empirical rows: 0;
- H4: preserved.

The architecture payload nevertheless repeated a concrete runtime/model label in a historical `BLUEPRINT_STATE.md` row. State is an abstract-role lifecycle owner and must not duplicate concrete model identity.

Classification: `ARCH_PAYLOAD_DEFECT / NON_CAPABILITY`.

The narrow repair removes that label from State and records review truth. It does not alter BAP-01, Work-01 evidence, Family-A burn, the qualifying matrix or any calibration result.

Blind Family-B Cell-01 remains not authorized until this repair receives remote ARCH ACCEPT.


## 12. Blind Family-B Cell-01 authorization
Parent `7c4c060dc5f6e86bcd9517da353cc8924e93818c` accepted. Family SHA `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`. Reveal only to ARCH. Cell-02 is not authorized.

## 13. Family-B Cell-01 Pre-Pair Seal

Cell-01 delivery `bd96410bd20e3a41848ca61a98eb41875e7c8829` has passed transaction and Commitment/Reveal integrity review.

To preserve BAP-01 blindness, semantic adjudication, concrete actor identity and capability attribution remain ARCH-private/sealed until the same-family peer cell is captured. Canonical matched credit therefore remains deferred.

This seal repairs only the malformed capability-ledger row and persists non-revealing hash/status evidence. It creates 0 new empirical rows and 0 evidence runs.

After this seal is remotely accepted, Cell-02 may be issued. The operator must select a concrete coder different from the sealed Cell-01 actor, and Cell-01 Reveal must not be provided to the Cell-02 coder.

## 14. Blind Family-B Cell-02

The Cell-01 Pre-Pair Seal is remote ARCH ACCEPT at `673cc9f1a0eb163058edf9fb7f467c429999cebf` and is consumed only by this blind Cell-02 acquisition.

Cell-02 reuses exact Family-B Truth revision R1 and SHA-256 `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`. The package remains abstract-CODER-bound. Concrete identity is supplied only in the repo-external Reveal so ARCH can later verify the operator's distinct-actor selection without exposing the sealed peer identity to this coder.

Canonical scope stores a cryptographic Commitment only. Pair acquisition completion still leaves semantic adjudication, actor normalization/distinctness, assistance/blindness, capability signal, corpus eligibility and matched credit pending remote ARCH review. This cell adds 0 empirical rows and preserves H4.

No later acquisition or re-analysis is authorized by Cell-02 itself.

## 15. Family-B Cell-02 identity-collision seal

Cell-02 delivery `72e296a80eb71eb9a864c528e3c1ae3ba791ce4a` passed transaction and Commitment/Reveal integrity review. Private actor normalization nevertheless failed the required distinctness gate with `FAIL_SAME_NORMALIZED_ACTOR`.

Both actor identities, raw Reveals, responses, scenario outcomes and capability results remain sealed. Canonical matched credit is `0 / DEFERRED`, and Cell-02 is `INELIGIBLE_IDENTITY_COLLISION` for matched inference.

The event is classified `OPERATOR_SELECTION_ATTESTATION_INCONSISTENCY / ACQUISITION_IDENTITY_CONFOUND / NON_CAPABILITY` with coder-negative signal `NONE`. This seal adds no evidence run or empirical row and does not change H4.

The Family-B Truth is not burned because raw responses remain outside canonical history. After this seal receives remote ARCH ACCEPT, ARCH may issue replacement `MC-B-CELL-03` for a concrete actor distinct from the sealed peer. Cell-03 and all later lanes remain unauthorized until separately issued.

## 16. Blind Family-B replacement Cell-03

The identity-collision seal is remote ARCH ACCEPT at `c8741c97e8a31c16ac42636600b8c019a8f53292` and is consumed only by replacement `MC-B-CELL-03`.

Cell-03 uses exact Family-B Truth R1 and SHA-256 `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`. Package authority remains abstract `CODER`; operator-selected concrete provenance is supplied only in the repo-external Reveal.

No peer/collision Reveal, raw response, actor identity or outcome is disclosed to this CODER. Canonical history stores only the Cell-03 Commitment. Pair eligibility, distinctness, semantic outcomes, capability signal and matched credit remain pending remote ARCH review.

This replacement adds no empirical row and preserves H4. No later acquisition or re-analysis is authorized by Cell-03 itself.

## 17. Family-B qualifying pair seal

Remote ARCH verifies both qualifying Commitment/Reveal pairs, blindness/exposure control and distinct normalized actors. Family-B is sealed as `QUALIFYING_MATCHED_PAIR` with 2/2 cells.

Concrete actors, responses, scenario outcomes and capability results remain ARCH-private. The excluded Cell-02 collision remains `NON_CAPABILITY` with no coder-negative signal.

Overall matched-family completion is 1/2: Family-B complete, Family-C 0/2. The seal creates no acquisition run or empirical row and leaves H4 unchanged. Only Family-C Cell-01 may be separately authorized after seal ACCEPT.

## 18. Blind Family-C Cell-01

Remote ARCH accepts Family-B Pair Seal `6e4214c26ea42467cdf9616d4783ee17fc68ae00` and consumes it only to authorize blind `MC-C-CELL-01`.

Cell-01 introduces frozen Family-C Truth R1 with SHA-256 `c98fd56ad559657107c8cfc21ebd6d80de58241c95bcf008db93690991ab406b`. The package remains abstract-CODER-bound. Concrete provenance, scenario actions, rationales and nonce are held only in the repo-external Reveal.

Canonical history contains the Family-C Truth and Cell-01 Commitment only. This acquisition adds no empirical corpus row and preserves H4. Family-C matched credit remains deferred pending remote ARCH reveal review and a separate non-revealing Pre-Pair Seal.

Cell-02, naturalistic capture, re-analysis, M4/M5 and Phase 3B remain unauthorized.

## 19. Family-C Cell-01 Commitment LF repair

Remote ARCH reviews Cell-01 delivery `13d63ee407fd4ac60e25f370091294073f1372d5`: the two-commit chain, claim scope, final 9-path scope, 8/8 static blobs, Preserve set, blind schema and no-leakage checks pass. The sole failure is the runtime Commitment blob using CRLF on 22/22 lines, so default `git diff --check` fails the frozen final gate.

This repair changes only canonical line endings for that Commitment to LF. Parsed JSON values, `response_commitment_sha256=e528239a2557729ff861a18c410a84f4a7bf0d1a8799a5f8afbaa8be4a66cdb9` and `reveal_payload_sha256=8f87d9e0d54811164a2651355e49388c930fba53c3178cca497d2798096531a8` remain exact. No Reveal is needed, read or published.

The repair adds no acquisition run or empirical row and preserves Family-C credit as deferred plus H4. Cell-02, naturalistic capture, re-analysis, M4/M5 and Phase 3B remain unauthorized until their separate gates.

## 20. Family-C Cell-01 Pre-Pair Seal

Remote ARCH accepts LF repair `442096fe81697360049d9b5df8e6986587873809` and privately verifies the original Cell-01 Commitment/Reveal pair, exposure controls, semantic disposition and concrete actor normalization.

The canonical seal publishes only commitment/reveal hashes plus non-revealing integrity, sealed-adjudication and sealed-normalization states. Raw Reveal, response, nonce, concrete actor, scenario outcome and capability result remain ARCH-private and are not supplied to CODER.

Cell-01 is one sealed Family-C pair candidate; canonical matched credit remains deferred until the peer is acquired and the pair is privately reviewed. This seal creates no acquisition run or empirical row and preserves H4.

After this seal receives remote ARCH ACCEPT, only blind Family-C Cell-02 may be separately issued. The operator must select a concrete coder different from the ARCH-sealed Cell-01 actor, and Cell-01 Reveal must never be disclosed to that coder.

## 21. Blind Family-C Cell-02

Remote ARCH accepts the Cell-01 Pre-Pair Seal at `4ebe04088bdc4dfbe0495b2478ecffefe449a038` and consumes it only to authorize blind `MC-C-CELL-02`.

Cell-02 reuses the byte-identical Family-C Truth (`sha256=c98fd56ad559657107c8cfc21ebd6d80de58241c95bcf008db93690991ab406b`) under BAP-01. Package authority remains abstract `CODER`; the operator selects a concrete actor different from the ARCH-sealed Cell-01 actor without exposing the peer identity, Reveal or result.

Canonical history stores only the Cell-02 Commitment. Raw actions, rationales, nonce and concrete provenance remain repo-external in operator/ARCH custody. The generating CODER must return the outside-repo path and hash and retain the file until receipt is confirmed; ARCH-private-after-handoff does not prohibit operator delivery.

Cell-02 completes pair capture only, not pair qualification. Actor normalization/distinctness, semantic outcomes, assistance controls, capability signals, corpus eligibility and matched credit remain pending private ARCH review. New empirical rows remain zero and H4 remains preserved. Naturalistic capture, re-analysis, M4/M5 and Phase 3B remain separately gated.

## 22. Family-C qualifying Pair Seal

Remote ARCH accepts Cell-02 transaction `88c1f352fae5a3b397b427d9bf8e978b285bb546` and privately verifies both Family-C Commitment/Reveal pairs, blindness/exposure controls and distinct normalized actors.

Family-C is sealed as `QUALIFYING_MATCHED_PAIR` with 2/2 cells. Concrete actors, responses, scenario outcomes and capability results remain ARCH-private. The canonical seal publishes only cryptographic hashes and non-revealing integrity/distinctness/eligibility status.

Overall qualifying matched-family completion becomes 2/2: Family-B 2/2 and Family-C 2/2. The Lane MC minimum matrix is therefore complete. This seal creates no acquisition run or empirical row and preserves H4 pending the remaining evidence gates.

Naturalistic production gaps EGC-G05 and EGC-G06 remain open. Their required `STRUCTURED_Q` and correct-`HARD_STOP` episodes must be captured from real production context, not manufactured. Re-analysis, M4/M5 and Phase 3B remain unauthorized until all §6 gates receive a fresh ARCH review.

## 23. Naturalistic-production gap reassessment and passive capture contract

Remote ARCH accepts Family-C Pair Seal `f4744068092a8af89e44f0d1920b14a4050e3887` and independently reassesses repository history against §§2–6.

No existing candidate closes Lane NP. The accepted structured-Q/correct-STOP probe rows and Family-A/B/C scenarios are controlled/synthetic; M0 R3 is context-only blocked/external rather than correct Hard STOP; M0 R4 is incorrect STOP; the Family-B R1 correct governance STOP occurred inside a matched-controlled evidence-acquisition wrapper; recovered custody/isolation stops are protocol misreads; early clarification notes lack the full immutable task/parent/actor/residual-boundary/independent-adjudication evidence minimum. Normal completion STOP is not Hard STOP.

Therefore EGC-G05 and EGC-G06 are each `OPEN_WITH_AUTHORIZED_NON_MANUFACTURING_CAPTURE_PROTOCOL`, not satisfied. The protocol may preserve a candidate only when it occurs naturally during future independently necessary work. It may not create, induce or simulate an event. This persistence creates zero runs, rows and manufactured events, preserves H4, and does not authorize re-analysis, M4/M5 or Phase 3B.

## 24. Passive wait entry

Remote ARCH accepts the reassessment delivery `64f071261559d0239837d210ab2f10c518849687` after verifying the exact claim/final chain, `10 paths=5A+5M`, all static blobs, runtime provenance, preimages, Preserve boundary and semantic gates.

The passive non-manufacturing capture contract is now active as a wait condition, not as an acquisition assignment. No dedicated task, ambiguity, fake parent/preimage condition, hidden fallback or probe may be created to obtain G05/G06 evidence.

This entry creates zero production events, zero acquisition runs and zero empirical rows. EGC-G05/G06 remain open with accepted clusters 0/0; H4 remains insufficient. Re-analysis, M4/M5 and Phase 3B remain unauthorized. A naturally occurring candidate requires fresh ARCH eligibility and correctness adjudication before any additive corpus persistence.


## H4 fallback roadmap architecture-research amendment — UBF-M3-H4-ARCHITECTURE-RESEARCH-FALLBACK-ROADMAP-01

Remote ARCH accepts `476910270016a325e767143c3361e20cdeee77b6` as the passive-wait persistence baseline. No naturalistic candidate has appeared after that entry; EGC-G05/G06 remain open with accepted clusters 0/0.

The implementation-control M3 Exit rule already requires failure or non-support to return to architecture research instead of inventing Levels. This amendment activates that research path without changing the empirical disposition:

1. `H4_INSUFFICIENT_EVIDENCE` remains the evidence result; this amendment is not an H3 or H2 finding.
2. The passive evidence lane remains active and non-manufacturing. No dedicated Q/STOP probe, fake parent, seeded ambiguity or hidden fallback may be created.
3. A parallel architecture-research lane may study a reversible non-ordinal closure-core candidate expressed by decision domains and explicit closure/delegation states. It may not call those states Universal Levels or infer an ordering from H4.
4. This batch finalizes zero Level/Profile/Selector/mapping decisions. It performs no reanalysis and creates zero events, runs and rows.
5. M4/M5 remain not started. Only an independently accepted fallback Work-01 plus an explicit roadmap amendment and stage handoff can make a revised M4 entry reachable.
6. If later natural evidence satisfies G05/G06, the empirical lane may resume under its existing fresh-adjudication and reanalysis gates; neither lane silently overrides the other.

The only next design activity after ACCEPT is a separate ARCH-authored `UBF-M3-H4-ARCHITECTURE-RESEARCH-FALLBACK-WORK-01` package. CODE has no standing authority to design the fallback semantics.
