# UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-B-CELL-01-PRE-PAIR-SEAL-01 — CODER Execution Blueprint

Revision: `R1`
Handoff Parent: `bd96410bd20e3a41848ca61a98eb41875e7c8829`
Execution Actor: `CODER`
Concrete Model Binding: `NONE`
Review Mode: `REMOTE_READ_ONLY_ARCH`

## Objective

Mechanically persist the ARCH-authored non-revealing Cell-01 Pre-Pair Seal and repair one malformed capability-ledger row.

## Frozen private-review boundary

ARCH has already verified the Cell-01 Commitment/Reveal pair. The repository MUST NOT learn:

- raw Reveal contents;
- scenario choices or rationales;
- semantic adjudication result;
- concrete Cell-01 actor identity;
- capability result.

Those stay sealed until Family-B Cell-02 is captured and the pair is complete.

## Canonical facts allowed

- reviewed delivery `bd96410bd20e3a41848ca61a98eb41875e7c8829`;
- claim `e00cabe703aec65efbc60b18679e4b69fd6b2b56`;
- Family-B/R1 digest `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`;
- commitment hash `4dc7307c6c3fc3529a4f77400d183cb84f3e7a2f39e3aadf89a2c4a6cf170227`;
- reveal payload hash `8ac02e8747bb457ffbb344c11b99e5b75f9050751b6fdc8385dd4056337aa15f`;
- Commitment/Reveal integrity PASS;
- private adjudication/actor normalization completed but SEALED;
- canonical matched credit DEFERRED;
- Cell-02 may be issued only after this seal is remote ARCH ACCEPT;
- Cell-02 concrete actor must differ from the sealed Cell-01 actor;
- Cell-01 Reveal must not be disclosed to Cell-02 CODER.

## Transaction

Parent REVIEW is the expected entry. This package itself authorizes exact State-only claim.

Original worktree may be dirty/behind and must be preserved. Do not pull/stash/reset/clean/rebase unrelated user changes. Fetch and execute in isolated detached clean worktree or temporary clone.

## Final scope

Exact **9 paths = 4A + 5M**:

- 8 static exact targets;
- 1 Runtime-Provenance JSON for this mechanical seal execution.

## Non-scope

No Cell-02 acquisition. No reveal persistence. No Cell-01 semantic/actor publication. No new evidence run/row. No re-analysis. No Level/Profile/Selector/routing. No M4/M5/Phase3B.
