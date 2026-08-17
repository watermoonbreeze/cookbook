# UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-B-CELL-02 — CODER Execution Blueprint

Revision: `R1`
Handoff Parent: `673cc9f1a0eb163058edf9fb7f467c429999cebf`
Execution Actor: `CODER`
Concrete Model Binding: `NONE`
Protocol: `BAP-01`
Family SHA-256: `b3d053f2940d0d960f6ea9d4bd370c5a2c124256adfab55f48ce554e603da163`

Execute exactly one blind Family-B Cell-02 acquisition. The Family Truth must remain byte-identical to Cell-01. The operator selects a concrete coder different from the sealed peer actor, but this package never exposes or binds either concrete identity.

Canonical repository receives only the Cell-02 Commitment. Raw actions, rationales, nonce and concrete provenance stay outside the repository and are returned only to operator/ARCH. The coder must not adjudicate semantics, actor distinctness, capability, eligibility or matched credit.

Normal final scope is exact 8 paths = 3 adds + 5 modifies. Completion returns `TURN=REVIEW` and remains pending remote ARCH reveal/pair adjudication. It authorizes no Family-C, naturalistic capture, re-analysis, M4/M5 or Phase 3B work.

The original host worktree may be dirty or behind and must remain untouched. Fetch remote objects, establish an isolated detached clean environment at the fixed parent, execute the exact State-only claim, push and remotely verify it before CODE is effective, then apply only the architecture-authored static patch and runtime Commitment.
