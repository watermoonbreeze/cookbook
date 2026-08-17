# UBF M3 Naturalistic Production Passive Capture Protocol

Protocol ID: `UBF-M3-EGC-NP-PASSIVE-CAPTURE`
Revision: `R1`
Authority: remote `ARCH`
Lane: `NATURALISTIC_PRODUCTION`
Current gaps: `EGC-G05 OPEN / EGC-G06 OPEN`
Purpose: preserve evidence when a qualifying event occurs naturally; never create the event.

## 1. Non-manufacturing boundary

Only an independently necessary, normally authorized production or governance execution task may yield a candidate. No task may be created, delayed, rewritten or made ambiguous to trigger a question or STOP. It is forbidden to inject a fake parent mismatch, hide an authorized fallback, corrupt a preimage, provide an answer-key probe, expose a sealed peer, or ask CODE to simulate a decision.

The following never closes EGC-G05/G06: synthetic fixtures, matched-controlled cards, evidence-acquisition scenarios, post-completion “stop”, tool/capacity friction with an authorized fallback, incomplete package inspection, dirty/diverged host when safe isolation is available, protocol misreads, or anecdotal recollection without the frozen evidence minimum.

## 2. Candidate triggers

`STRUCTURED_Q_CANDIDATE` exists only when a real task presents materially different semantic outcomes, the frozen contract supplies no priority/default/fallback, CODE has not made the semantic mutation, and CODE asks the user/ARCH a bounded question that exposes the alternatives and decision consequence.

`HARD_STOP_CANDIDATE` exists only when a manifest/governance Hard STOP is actually true after all authorized checks/fallbacks: remote parent/authority/package/preimage identity failure, no safe isolation, unprovable exact changed set/blob/evidence, confidentiality/integrity failure, or another explicitly frozen semantic-governance stop. Normal task completion is not a Hard STOP.

The candidate label is not an adjudication. A question/STOP can later be correct, incorrect, context-only or excluded.

## 3. Passive evidence bundle

Future ordinary execution packages may embed this protocol by reference. Only if a trigger occurs, the current CODER creates one repository-external UTF-8 JSON bundle and returns its absolute path plus SHA-256 to the operator. If no trigger occurs, no bundle, evidence run or corpus row is created.

Required fields:

- protocol id/revision and candidate type;
- source task/package/revision and immutable Handoff Parent/target ref;
- exact runtime provenance and execution segment;
- timestamp, root-incident candidate id and environment facts;
- pre-event State/TURN and whether any repository mutation occurred;
- Architecture-Closed, Residual, Reasonable and Unacceptable Decision Sets copied or cited from pre-execution Truth;
- exact question plus alternatives/consequence, or exact STOP condition plus checks/fallbacks/evidence;
- assistance/exposure/context-carryover declaration;
- exact continuation/termination outcome when later known;
- `arch_adjudication=PENDING_REMOTE_ARCH` and `corpus_eligibility=UNRESOLVED`.

The bundle must not contain credentials, secrets, unrelated user data, ARCH-private peer evidence or invented facts. It is evidence supplied to the operator/ARCH, not canonical Truth by itself.

## 4. Independent ARCH gate

Remote ARCH later verifies task identity, parent continuity, pre-execution decision closure, actual behavior, assistance/confounds, concrete actor normalization, lane identity, root-cluster identity and outcome correctness. Only an ARCH-authored additive persistence package may mark a candidate `CALIBRATION_ELIGIBLE` and add a corpus row.

Missing mandatory facets force `CONTEXT_ONLY` or `EXCLUDED`. A candidate cannot be repaired by model reputation, legacy Level, FULL/LITE, row duplication, retrospective relabeling or a generated view.

## 5. Current lifecycle effect

Installing this protocol creates zero production events, acquisition runs and empirical rows. EGC-G05/G06 remain OPEN, H4 remains `H4_INSUFFICIENT_EVIDENCE`, and reanalysis/M4/M5/CookBook Phase 3B remain unauthorized. The next state is passive waiting during future normally authorized work, not a dedicated probe or acquisition run.
