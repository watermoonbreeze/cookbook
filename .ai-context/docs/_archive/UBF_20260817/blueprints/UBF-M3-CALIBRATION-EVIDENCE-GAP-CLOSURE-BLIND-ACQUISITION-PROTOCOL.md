# UBF M3 Calibration Evidence Gap Closure — Blind Acquisition Protocol

Document Role: Architecture Truth / Controlled Evidence Acquisition Protocol
Protocol Revision: `BAP-01`
Repair Task: `UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-ACQUISITION-PROTOCOL-REPAIR-01 / R1`
Repair Handoff Parent: `d43c73fe12cfe3abd3a5b5efa7b5492b0487beca`
Analysis Disposition: `H4_INSUFFICIENT_EVIDENCE` — PRESERVED
Execution Actor: `CODER`
Concrete Model Binding: `NONE`

## 1. Defect being repaired

`UBF-M3-CALIBRATION-EVIDENCE-GAP-CLOSURE-WORK-01` correctly captured one Family-A raw response, and remote ARCH adjudicates its four scenario decisions as **4/4 SEMANTIC PASS**.

However, two facts prevent that episode from satisfying the matched-controlled inferential matrix:

1. its Runtime-Provenance contains `UNKNOWN_SELF_REPORT / UNAVAILABLE`, so a concrete coder identity cannot be independently normalized;
2. its raw Response was persisted to canonical repository history before a second same-family coder observation existed.

Because later coders can inspect canonical history, a sequential same-Family-A Cell-02 could be exposed to Cell-01's choices. A prose instruction to "not look" cannot independently prove absence of prior exposure.

Classification:

- Work-01 CODE execution fidelity: **ACCEPT**.
- Work-01 semantic behavior: **4/4 PASS / NON-NEGATIVE**.
- Work-01 matched-inference eligibility: **NOT ELIGIBLE**.
- Family-A future matched reuse: **BURNED_BY_CANONICAL_RESPONSE_EXPOSURE**.
- concrete actor identity: **UNRESOLVED**.
- architecture defect: `BLUEPRINT_DEFECT / NON_CAPABILITY`.
- coder negative capability signal: **NONE**.

No empirical corpus row is created by this repair.

## 2. Blind capture principle

For every future matched-controlled family that may contribute to the 2-family × 2-coder matrix:

**Raw response choices, rationales, and raw concrete-model provenance MUST NOT enter canonical repository history until every required matched cell for that family has been captured.**

The canonical repository may contain only:

- immutable Family Truth Capsule identity/digest;
- cell status and lifecycle metadata;
- a cryptographic commitment to the hidden reveal bundle;
- abstract actor role `CODER`;
- non-inferential acquisition metadata.

It must not contain:

- selected actions;
- rationales;
- answer key / semantic adjudication;
- concrete model label;
- raw Runtime-Provenance;
- reveal nonce.

## 3. Blind Reveal Bundle

Each blind cell produces a repo-external JSON file:

`<TASK>-Blind-Reveal-Bundle.json`

The reveal bundle contains:

- exact Family Truth identity/revision/SHA-256;
- cell ID;
- selected actions and rationales;
- runtime provenance, including truthful self-report and operator/launcher label when available;
- a fresh cryptographically random 32-byte nonce encoded as lowercase hex;
- `canonical_reveal_payload` object;
- no ARCH correctness/capability/eligibility decision.

The reveal bundle is **not staged, committed, pushed, or placed anywhere under the repository root**.

The CODER returns the reveal bundle to the operator together with the final commit hash. The operator may upload it to the ARCH conversation, but MUST NOT pass it to another coder executing a matched cell from the same family.

## 4. Canonical commitment

The repo contains only a commitment JSON.

Define the canonical reveal payload bytes as:

1. JSON UTF-8;
2. `ensure_ascii=false`;
3. keys sorted recursively;
4. separators exactly `(',', ':')`;
5. no trailing newline.

Define:

`commitment_input = nonce_hex UTF-8 + b"\n" + canonical_reveal_payload_bytes`

`response_commitment_sha256 = SHA256(commitment_input)`

The canonical Commitment JSON records:

- protocol revision;
- family identity/revision/digest;
- cell ID;
- `actor_role=CODER`;
- response commitment SHA-256;
- reveal payload SHA-256;
- reveal bundle status=`HELD_OUTSIDE_REPOSITORY`;
- concrete actor=`SEALED_PENDING_ARCH_REVEAL`;
- semantic/capability/eligibility=`PENDING_REMOTE_ARCH_REVEAL_AND_ADJUDICATION`.

The commitment cannot reveal selected actions, rationales, nonce, or concrete model.

## 5. ARCH reveal verification

On receipt of a reveal bundle, ARCH must:

1. recompute canonical reveal payload bytes;
2. recompute payload SHA-256;
3. recompute `SHA256(nonce_hex + "\n" + canonical_payload_bytes)`;
4. compare both hashes to the canonical repository Commitment JSON;
5. reject any mismatch as evidence-integrity failure;
6. keep semantic adjudication outside canonical repo until all same-family matched cells required by the protocol are captured.

Only after the family pair is complete may ARCH persist response/adjudication evidence.

## 6. Matched identity gate

Packages remain bound to abstract actor `CODER`. They never bind a concrete commercial model name.

For a cell to satisfy the matched identity gate, the reveal must permit ARCH to normalize a concrete coder identity from truthful runtime/operator evidence.

- If identity is unresolved, the cell is `MATCHED_INFERENCE_INELIGIBLE / NON_NEGATIVE`.
- If two cells normalize to the same concrete coder identity, they do not form the required cross-coder pair.
- The user/operator may deliberately choose a different available concrete coder for the next cell, but the package itself remains model-agnostic.
- Capacity substitution remains `SOFT_COMPATIBILITY / EXTERNAL_RUNTIME_CAPACITY / NON_CAPABILITY`.

## 7. Future matched families

Family-A is not reusable for qualifying matched inference because its first raw Response is already visible in canonical history.

The qualifying 2 × 2 matrix therefore restarts with **new, not-yet-published Family Truths**:

- Family-B Cell-01 blind capture;
- Family-B Cell-02 blind capture with a different ARCH-normalized concrete coder;
- Family-C Cell-01 blind capture;
- Family-C Cell-02 blind capture with a different ARCH-normalized concrete coder.

A Family Truth becomes visible to its first coder when that family begins. During the pair, neither cell's raw response is canonical.

The two family cells must share the exact same Family Truth identity/revision/digest. Outer Git wrapper parents may differ for lifecycle transport.

## 8. Assistance / exposure gate

A matched cell is inferentially usable only if:

- no prior same-family raw response/adjudication was available in canonical history;
- operator did not give the coder another cell's reveal;
- no mid-execution ARCH/reviewer answer hint occurred;
- package text and pre-authorized fallback are the only normal assistance;
- reveal provenance discloses any interruption/model substitution.

If exposure cannot be ruled out, classify the cell `CONTEXT_ONLY / NON_NEGATIVE` unless independently attributable execution deviation exists.

## 9. Evidence Gap Closure matrix after this repair

Current qualifying matched matrix:

- Family-A: `0 qualifying cells` (burned for future matched reuse).
- Family-B: `0/2`.
- Family-C: `0/2`.

Naturalistic Production lane:

- production `STRUCTURED_Q`: OPEN.
- production correct `HARD_STOP`: OPEN.

Re-analysis remains unauthorized.

## 10. Authority after repair ACCEPT

This repair itself authorizes no new evidence run.

After remote ARCH ACCEPT, ARCH may issue **one blind Family-B Cell-01 acquisition package** under BAP-01.

That package must:

- bind abstract `CODER`;
- capture raw response outside repo;
- commit only a commitment;
- return a Blind-Reveal-Bundle to the operator;
- create 0 empirical corpus rows;
- preserve H4;
- not start Family-B Cell-02 automatically.

M4/M5 and CookBook Phase 3B remain prohibited.
