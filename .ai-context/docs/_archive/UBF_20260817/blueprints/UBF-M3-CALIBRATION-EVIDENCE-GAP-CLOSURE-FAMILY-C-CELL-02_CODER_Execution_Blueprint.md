# UBF-M3 Family-C Cell-02 — CODER Execution Blueprint

Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-FAMILY-C-CELL-02 / R1`
Handoff Parent: `4ebe04088bdc4dfbe0495b2478ecffefe449a038`
Actor authority: abstract `CODER`
Protocol: `BAP-01`

## 1. Frozen acquisition contract

- Reuse `.ai-context/docs/项目改造规划/蓝图设计2/UBF-M3-EGC-MC-FAMILY-C-R1.json` byte-identically.
- The operator selects a concrete actor different from the ARCH-sealed Cell-01 actor before execution.
- Cell-01 Reveal, concrete identity, response, semantic result and capability result are not supplied to this CODER.
- Only the Cell-02 cryptographic Commitment enters repository history. Blind input and Reveal remain repo-external.
- CODE does not adjudicate semantic correctness, actor normalization/distinctness, capability signal, corpus eligibility or matched credit.

## 2. Transaction contract

1. Fully extract and read the execution package before classifying any STOP.
2. Treat parent `TURN=REVIEW` as the expected entry state; the package authorizes one exact State-only claim.
3. Preserve any dirty/diverged original host. Fetch only and execute in a clean temporary clone or detached worktree at the fixed parent; do not pull, merge, stash, reset, clean or rebase.
4. Push and remotely verify the claim before response capture.
5. Materialize the exact static payload, complete the blind response independently, generate the Reveal outside the repository, verify the Commitment/Reveal pair, then stage only the final allowlist.
6. Push the final without history rewrite and return the full hash plus Reveal custody receipt.

## 3. Repo-external custody contract

The generating CODER owns this ordered handoff:

`GENERATED_BY_CURRENT_CODER -> HELD_OUTSIDE_REPOSITORY -> RETURN_PATH_AND_HASH_TO_OPERATOR -> RETAIN_UNTIL_OPERATOR_RECEIPT_CONFIRMED -> ARCH_PRIVATE_AFTER_HANDOFF`

`ARCH_PRIVATE` never means the generating CODER may refuse delivery to the operator. The file must not be regenerated after its Commitment exists because regeneration changes the nonce and invalidates the pair. If the file is genuinely unavailable, report `LOST_OR_UNAVAILABLE` with exact facts and do not guess or recreate it.

## 4. Prior friction incorporated

Two recovered pre-mutation events are classified `EXECUTION_PROTOCOL_MISREAD / NON_CAPABILITY`:

- repo-external Reveal custody was confused with a prohibition on operator handoff;
- expected `TURN=REVIEW` plus dirty/ahead/behind host state was confused with a hard stop, before the package was fully read.

This R1 closes those ambiguities with a package-inspection gate, machine-readable readiness table, isolated-clone bootstrap and receipt-bearing Reveal return contract.

## 5. Final gates

- exact final scope `8 paths = 3A + 5M`;
- seven static artifacts match architecture-authored target blobs;
- one schema-constrained Cell-02 Commitment exists and contains no private response/provenance fields;
- Family-C Truth, BAP-01 and all sealed peer artifacts are preserved;
- final `TURN=REVIEW`;
- new empirical rows `0`, matched credit `DEFERRED`, H4 preserved;
- naturalistic capture, re-analysis, M4/M5 and Phase 3B remain unauthorized.
