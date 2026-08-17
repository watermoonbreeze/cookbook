# UBF-M3 Family-B Cell-02 Identity-Collision Seal-01 — CODER Execution Blueprint

## Identity

- Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-B-CELL-02-IDENTITY-COLLISION-SEAL-01`
- Revision: `R1`
- Handoff Parent: `72e296a80eb71eb9a864c528e3c1ae3ba791ce4a`
- Execution actor: abstract `CODER`
- Review mode: `REMOTE_READ_ONLY_ARCH`
- Protocol: `BAP-01`

## Purpose

Persist a non-revealing architecture review seal after Cell-02 transaction/reveal integrity passed but the required cross-cell normalized-actor distinctness gate failed. This is a governance persistence task, not a new evidence acquisition or semantic adjudication.

## Frozen disposition

- Commitment/Reveal integrity: `PASS`.
- Actor distinctness: `FAIL_SAME_NORMALIZED_ACTOR`.
- Matched inference: `INELIGIBLE_IDENTITY_COLLISION`.
- Canonical matched credit: `0 / DEFERRED`.
- Defect attribution: `OPERATOR_SELECTION_ATTESTATION_INCONSISTENCY / ACQUISITION_IDENTITY_CONFOUND / NON_CAPABILITY`.
- Coder-negative signal: `NONE`.
- Family-B status: `NEEDS_DISTINCT_REPLACEMENT_CELL`.
- Replacement identity: `MC-B-CELL-03`, not authorized until this seal receives remote ARCH ACCEPT.

## Sealed boundary

The execution package and canonical target MUST NOT disclose or infer either concrete actor, raw Reveal, raw response, nonce, rationale, per-scenario outcome, semantic score or capability result. Hashes and non-revealing disposition codes are the only canonical review evidence.

## Exact transaction

1. Start only when `origin/master` exactly equals the Handoff Parent and parent State is `TURN=REVIEW`.
2. Preserve the original dirty/behind worktree and execute in an isolated detached clean worktree or temporary clone.
3. Materialize and push the exact State-only claim; CODE authority becomes effective only after remote verification.
4. Apply the architecture-authored static bundle and create truthful runtime provenance.
5. Stage exact 9 final paths: 4 adds + 5 modifies.
6. Verify static target blobs, runtime schema, exact allowlist, confidentiality denyset, Family Truth/BAP-01 Preserve, zero new run/row, H4 and lifecycle coherence.
7. Commit, push, verify remote, return State to `TURN=REVIEW`, and stop.

## Preserve / prohibited

- Preserve the byte-identical Family-B Truth and BAP-01.
- Preserve all accepted analysis/corpus/evidence and production assets.
- Do not read or request repo-external Reveal bundles.
- Do not adjudicate private semantics or capability.
- Do not start Cell-03, Family-C, naturalistic capture, re-analysis, M4/M5 or CookBook Phase 3B.
