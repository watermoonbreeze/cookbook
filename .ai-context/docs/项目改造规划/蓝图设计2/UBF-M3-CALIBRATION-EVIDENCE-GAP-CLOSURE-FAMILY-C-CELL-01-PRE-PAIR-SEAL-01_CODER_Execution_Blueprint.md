# UBF-M3 Family-C Cell-01 Pre-Pair Seal — CODER Execution Blueprint

## Authority

- Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-C-CELL-01-PRE-PAIR-SEAL-01 / R1`
- Handoff Parent: `442096fe81697360049d9b5df8e6986587873809`
- Actor: abstract `CODER`; no concrete model binding
- Execution: clean isolated detached worktree or temporary clone
- History rewrite and unrelated host-worktree mutation: forbidden

## Frozen action

Mechanically materialize the exact non-revealing seal and lifecycle payload supplied by the architecture package. The seal may publish only cryptographic hashes and sealed review states. It must not publish or infer Cell-01 raw response, nonce, rationale, concrete actor, scenario outcome, semantic result or capability result.

No Cell-01 Reveal or Blind Response Input is included, required or authorized for CODER access. Do not request it from the operator or ARCH.

## Transaction

1. Resolve `origin/master` exactly to the Handoff Parent.
2. Materialize, commit and push the exact State-only claim; remotely verify CODE ownership.
3. Materialize the final payload and one schema-constrained execution Runtime-Provenance file for this mechanical seal task.
4. Verify final exact `9 paths = 4A + 5M`, 8 exact static blobs, one valid Runtime-Provenance, Preserve, clean diff and `TURN=REVIEW`.
5. Commit/push without rewriting history and return only the full final hash.

## Gates

- new acquisition runs: `0`
- new empirical rows: `0`
- Family-C matched credit: `DEFERRED`
- H4: `H4_INSUFFICIENT_EVIDENCE`
- Cell-02: authorized only after this seal receives remote ARCH ACCEPT and a separate package
- future Cell-02 actor: must differ from the ARCH-sealed Cell-01 actor
- Cell-01 Reveal disclosure to Cell-02 CODER: forbidden
- naturalistic capture / re-analysis / M4 / M5 / Phase 3B: not authorized
