# UBF M3 Family-C Cell-01 Commitment LF Repair — CODER Execution Blueprint

## Authority

- Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-C-CELL-01-COMMITMENT-LF-REPAIR-01 / R1`
- Handoff Parent: `13d63ee407fd4ac60e25f370091294073f1372d5`
- Actor: abstract `CODER`; no concrete model binding
- Execution: dirty-host-safe isolated clean worktree or temporary clone
- History rewrite, force-push, pull/merge/stash/reset/clean/rebase: forbidden

## Frozen repair

The existing Family-C Cell-01 Commitment is semantically valid but canonicalized with CRLF, causing 22 default `git diff --check` failures. Normalize that file to LF only. Preserve every parsed JSON key/value and both embedded cryptographic hashes exactly.

No Reveal is required or authorized for this repair. Do not read, copy, stage, disclose or regenerate any Blind Reveal/Response Input or ARCH-private material.

## Transaction

1. Resolve remote `origin/master` exactly to the Handoff Parent.
2. In isolation, materialize the exact State-only claim, commit/push it, and remotely verify CODE ownership.
3. Materialize the exact final payload from this package.
4. Verify final exact `8 paths = 2A + 6M`, exact target blobs, LF-only Commitment, clean diff, Preserve set and `TURN=REVIEW`.
5. Commit/push final without rewriting history and return only the full final hash.

## Semantic gates

- acquisition runs added: `0`
- empirical rows added: `0`
- Family-C matched credit: `DEFERRED`
- H4: `H4_INSUFFICIENT_EVIDENCE`
- Family-C Cell-02: `NOT AUTHORIZED`
- naturalistic capture / re-analysis / M4 / M5 / Phase 3B: `NOT AUTHORIZED`
