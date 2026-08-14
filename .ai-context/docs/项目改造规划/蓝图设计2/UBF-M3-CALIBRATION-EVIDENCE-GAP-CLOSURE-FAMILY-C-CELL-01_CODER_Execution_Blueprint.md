# UBF-M3 Blind Family-C Cell-01 — CODER Execution Blueprint

## Identity

- Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-C-CELL-01`
- Revision: `R1`
- Handoff Parent: `6e4214c26ea42467cdf9616d4783ee17fc68ae00`
- Actor role: abstract `CODER`
- Review mode: `REMOTE_READ_ONLY_ARCH`
- Protocol: `BAP-01`
- Family Truth: `UBF-M3-EGC-MC-FAMILY-C/R1`
- Truth SHA-256: `c98fd56ad559657107c8cfc21ebd6d80de58241c95bcf008db93690991ab406b`

## Purpose

Capture one blind matched-controlled Family-C execution while keeping raw response, rationale, nonce and concrete provenance outside canonical repository history.

## Exact transaction

1. Enter only at exact remote parent with State `TURN=REVIEW`.
2. Preserve a dirty/behind host and execute in an isolated clean checkout.
3. Push and remotely verify the exact State-only claim before CODE begins.
4. Read the frozen Family-C Truth and complete every scenario independently without hints.
5. Keep Blind Response Input and Reveal outside the repository.
6. Generate the canonical Commitment plus repo-external Reveal using the supplied adapter.
7. Apply/stage exact `9 paths = 4A + 5M`; verify 8/8 static blobs, Commitment/Reveal integrity, Preserve, confidentiality and lifecycle coherence.
8. Commit/push, return State to REVIEW, then provide only final hash, Reveal and Commitment SHA-256 to the operator.

## Prohibited

Do not request an answer key or prior same-family response; do not infer correctness/capability/eligibility; do not stage Reveal/Input; do not start Cell-02, naturalistic capture, re-analysis, M4/M5 or Phase 3B.
