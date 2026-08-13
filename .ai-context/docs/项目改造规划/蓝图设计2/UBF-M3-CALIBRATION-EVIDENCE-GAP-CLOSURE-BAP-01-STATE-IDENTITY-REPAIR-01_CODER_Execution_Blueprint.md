# UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-BAP-01-STATE-IDENTITY-REPAIR-01 — CODER Execution Blueprint

Document Role: Narrow Architecture Payload Repair Blueprint
Revision: `R1`
Handoff Parent: `15b3470703b3df0f1f7dcae8a815b3f660463f0c`
Execution Actor: `CODER`
Concrete Model Binding: `NONE`

## Objective

Mechanically repair one architecture-authored State model-identity hygiene defect and persist the remote review disposition.

## Frozen review facts

- BAP-01 delivery `15b3470703b3df0f1f7dcae8a815b3f660463f0c` CODE execution fidelity = ACCEPT.
- Claim `eba909fad46a613e121f75f1ee66f443509cebcd` = State-only.
- BAP-01 semantics/evidence boundary are preserved.
- Defect: one historical State row repeats a concrete runtime/model label.
- Attribution: `ARCH_PAYLOAD_DEFECT / NON_CAPABILITY`.
- No coder/model negative sample.

## Exact repair

The concrete runtime/model label is removed from State history and replaced by an abstract statement that concrete identity remains in the experience/runtime-provenance layer.

Do not alter the underlying provenance file or BAP protocol.

## Transaction

Parent REVIEW -> exact State-only claim -> remote verify -> CODE -> exact 8-path final -> REVIEW.

## Final scope

Exact final scope: **8 paths = 3A + 5M**.

- 7 static exact-blob targets;
- 1 dynamic Runtime-Provenance JSON.

## Non-scope

No BAP redesign. No evidence acquisition. No Family-B cell. No corpus row. No analysis change. No Level/Profile/Selector/routing. No M4/M5/Phase 3B.
