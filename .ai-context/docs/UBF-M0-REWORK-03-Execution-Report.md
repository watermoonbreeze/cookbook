# UBF-M0-REWORK-03 Execution Report

Document Role: Repository-carried Mechanical Execution and Corrected Remote Review Evidence
Task ID: UBF-M0-REWORK-03
Blueprint Revision Executed: R1
Review Operation Mode: REMOTE_READ_ONLY_ARCH
Package Profile: FULL
CookBook Legacy Granularity: L7
Execution Model: GPT-5.6 Luna
Handoff Parent: c3c7b812272344935f2bb48f96a890d84081b5d3
Execution Parent / Turn Claim Commit: 838136d645b7ac73c200f08305d052d6b93cad33
Reviewed Delivery Commit: 2a5567193c688bbd0e30f323699a68aab1ffeb34
Expected Return TURN: REVIEW
Report Correction Task: UBF-M0-REWORK-05
Outcome at Reviewed Delivery: BLOCKED_FOR_REVIEW / REMOTE ARCH REVIEWED / REWORK REQUIRED

## A. Overall UBF Status

UBF remained at M0. R3 produced a valid four-file fallback but completed 0/10 original repairs. M1 and Phase 3B remained prohibited. The only valid transition sequence is repair → remote ACCEPT → separate M0 End/Accept plus M0→M1 Handoff persistence → review of that handoff → separate M1 Preview/Start.

## B. Preflight and Turn Claim

- Remote: origin; Repository: cookbook; Endpoint: CONFIGURED / VALUE NOT RECORDED
- Handoff Parent: `c3c7b812272344935f2bb48f96a890d84081b5d3`; claim: `838136d645b7ac73c200f08305d052d6b93cad33`; delivery: `2a5567193c688bbd0e30f323699a68aab1ffeb34`.
- Claim changed only State; fallback changed exactly four authorized files; final TURN was REVIEW.
- Historical deviation: claim State did not retain R2 as immediately previous until final delivery.
- Historical deviation: claim State used `CLAIM_COMMIT_PENDING`, not required `PENDING CLAIM COMMIT`.

## C. Architecture Disposition

| Issue group | Disposition at R3/R4 |
|---|---|
| R2-01, R2-03, R2-04, R2-06 | ACCEPT_AS_IS |
| R2-02, R2-05, R2-07, R2-08 | REPAIR |
| R3-01, R3-02, R3-03, R3-04, R3-05, R3-06 | REPAIR |
| R3-EXEC-01 | REPAIR through isolated worktree |
| R4-01, R4-02, R4-03, R4-04 | REPAIR historical evidence in later report |

## D. Execution Result at `2a5567193c688bbd0e30f323699a68aab1ffeb34`

| Issue | Expected repair | Actual at reviewed delivery | Remote evidence |
|---|---|---|---|
| UBF-M0-R2-02 | Supplement truth/status | NOT EXECUTED | Supplement absent from four-file diff |
| UBF-M0-R2-05 | Corrected R2 report | NOT EXECUTED | R2 report absent from four-file diff |
| UBF-M0-R2-07 | UTF-8 Control R4 contract | NOT EXECUTED | Control absent from four-file diff |
| UBF-M0-R2-08 | Supplement hash basis | NOT EXECUTED | Supplement absent from four-file diff |
| UBF-M0-R3-01 | Restore official R2 blueprint | NOT EXECUTED | R2 blueprint absent from four-file diff |
| UBF-M0-R3-02 | Supplement provenance/integrity | NOT EXECUTED | Supplement absent from four-file diff |
| UBF-M0-R3-03 | Correct R2 historical report | NOT EXECUTED | R2 report absent from four-file diff |
| UBF-M0-R3-04 | Truthful State repair history | NOT EXECUTED | Fallback State only records blocked transaction |
| UBF-M0-R3-05 | Current UBF stage/transition gate | NOT EXECUTED | Control/Supplement absent from diff |
| UBF-M0-R3-06 | Final model-ledger evidence | NOT EXECUTED | R3 row required later ARCH backfill |

## E. Preserved Evidence

| Source | LF-normalized SHA-256 | Lines | Byte comparison |
|---|---|---:|---|
| `.ai-context/docs/experience/12_多模型协作与实施蓝图规范.md` | `0a58da55219ce134095c3a15881205124174b74994e47b58168a91c1f402c827` | 479 | PASS |
| `.ai-context/docs/experience/INDEX.md` | `e81a8e26866ba2db3347e998b79bba9833b189ef39e098f9bd023e045aa17241` | 55 | PASS |
| `.ai-context/project_graph/README.md` | `2e8ee6833d5cf672bc62118c41938436fcbc66ea24efad500838294b489a4677` | 448 | PASS |

R2-03, R2-04 and R2-06 were also preserved.

## F. Scope, Isolation and Dirty-State Evidence

The final R3 allowlist was exactly its four fallback files; no denylist or production file entered the commit.

Verified recoverable pre-existing target facts: Control was deleted and Supplement was modified before R3 implementation. The R3 report did not preserve the remaining exact dirty path names; they are not reconstructable from remote Git and are not invented here. R4 and R5 use isolated clean worktrees and leave the original worktree untouched.

## G. Model Ledger

The R3 row is backfilled with reviewed delivery `2a5567193c688bbd0e30f323699a68aab1ffeb34` and the remote ARCH assessment authorized by R5.

## H. Issue Register and Historical Status

| Issue | Historical status |
|---|---|
| UBF-M0-R3-EXEC-01 | Valid environment issue for R3; later closed by isolated worktree. |
| UBF-M0-R4-01 | R3 claim State historical deviations required explicit recording. |
| UBF-M0-R4-02 | R3 report aggregated repair issues and lacked per-Issue evidence. |
| UBF-M0-R4-03 | R3 report omitted the three fixed retained-source hashes. |
| UBF-M0-R4-04 | R3 report did not preserve all dirty paths; unrecoverable names must not be invented. |

R3 remains valid blocked review input, not an accepted repair.

## I. Transition Gate

R5 is the current deterministic repair. M1 remains unauthorized pending R5 remote review and the separate accepted-handoff sequence.
