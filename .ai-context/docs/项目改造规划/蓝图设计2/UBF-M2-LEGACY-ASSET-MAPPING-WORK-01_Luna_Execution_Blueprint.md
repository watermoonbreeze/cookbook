# UBF-M2-LEGACY-ASSET-MAPPING-WORK-01 Luna Execution Blueprint

Document Role: Architecture-authored Mechanical Mapping Task

Mechanism: `STATIC_TARGET_BUNDLE`

Review Mode: `REMOTE_READ_ONLY_ARCH`

Delegation: `DELEGATED_SINGLE_TASK_CLAIM`

Execution Model: `GPT-5.6 Luna`

Handoff Parent: `c72a19b257550de7bb75dc9361b9f939fc220cb9`

Expected Return TURN: `REVIEW`

## 0. Frozen architecture decision

Remote ARCH accepts `UBF-M2-PREVIEW-START-01` at the Handoff Parent. Verified chain: `2054899ad93d9c2bc1353914c31a1ef3b96c15ac -> 15d97682f2f8a276d494d98871d6c692cf8ab6c5 -> c72a19b257550de7bb75dc9361b9f939fc220cb9`; claim exact one State path; final exact seven paths; 7/7 target blobs; State denyset; four Preserve blobs; whitespace, lifecycle and mapping-non-start gates all PASS. Open issue register: `NONE`.

Architecture has independently authored all 48 mapping records. CODE must only materialize exact target bytes and produce Git evidence. CODE must not classify a GC, change a candidate value, resolve a Universal Mapping or reopen M1 decomposition.

## 1. Required final lifecycle

```text
M0/M1: ACCEPT / CLOSED
M1→M2 Handoff: ARCH ACCEPTED / CONSUMED
M2 Preview/Start: ACCEPT / CONSUMED BY WORK-01
M2 Mapping Work-01: COMPLETE / PENDING REMOTE ARCH REVIEW
M2 Stage: IN PROGRESS / NOT CLOSED
M3: NOT STARTED / NOT AUTHORIZED
CookBook Phase 3B: NOT AUTHORIZED TO START
TURN: REVIEW
```

## 2. Exact normal final allowlist

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M2-LEGACY-ASSET-MAPPING-WORK-01_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M2-LEGACY-ASSET-MAPPING-WORK-01-Execution-Report.md`
3. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M2-LEGACY-ASSET-MAPPING-WORK-01.md`
4. `M .ai-context/docs/项目改造规划/蓝图设计2/UBF-M2-PREVIEW-START.md`
5. `M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
6. `M .ai-context/docs/experience/14_模型执行力评估.md`
7. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No eighth path is allowed.

## 3. Frozen mapping result

The target mapping artifact contains exactly one record for every `GC-01`～`GC-48` and carries the required fields through common-value declarations plus per-row values.

Frozen aggregate facts:

- stable identities: 48 unique; missing 0; duplicate 0;
- Legacy grouping: L1=9, L2=8, L3=9, L4=7, L5=5, L6=4, L7=6;
- Decision Category: 48 × `PRESERVE_IDENTITY + REDEFINE_LEVEL_RELATION`;
- Universal Mapping: 48 × `UNRESOLVED`;
- Lifecycle: 48 × `ACTIVE_LEGACY / PRESERVED_FOR_CALIBRATION`;
- Universal Level names, numeric ladder and direct Legacy→Universal inference: 0;
- M1 64-record decomposition reopen: `NO`.

`Applicable Task Profile Candidate` labels are provisional workload-shape labels only. They do not define the future Task Profile schema, Capability Profile, actor routing or Universal Level.

## 4. Claim and transaction

From an isolated clean environment created from the exact Handoff Parent:

1. verify fetched target ref equals the Handoff Parent;
2. materialize the package's exact claim State only;
3. verify claim scope, claim State Git blob, State concrete-model denyset, whitespace and clean residue;
4. commit exactly `docs(governance): claim UBF M2 mapping work turn`, push and remote-verify;
5. treat the verified claim commit as Execution Parent;
6. materialize the package's exact seven final target artifacts without editing;
7. verify final scope relative to claim, parent scope, target blobs, State denyset, whitespace, tracked+untracked residue, Preserve blobs, 48-record recount and lifecycle truth;
8. commit exactly `docs(governance): persist UBF M2 legacy asset mapping`, push, remote-verify and STOP.

If all preauthorized adapters are exhausted after a published claim, materialize only the exact abort State, commit `docs(governance): record UBF M2 mapping compatibility block`, push, verify and STOP. Compatibility abort is not task completion.

## 5. Adapter contract and fallback

Authoritative Payload is Truth. Python, PowerShell, Bash, native Git and file-copy methods are execution adapters only.

Preauthorized fallback order:

```text
OPTIONAL_HELPER
→ NATIVE_EXACT_FILE_COPY
→ ALTERNATE_SHELL_EXACT_FILE_COPY
→ AUTHORIZED_ABORT_TARGET (only after claim and only if all adapters are exhausted)
```

OS path separators, console encoding, shell differences, optional-tool absence and helper startup failures are soft compatibility. They require automatic fallback when an equivalent adapter remains available. They are not reasons to edit target content or return to ARCH.

Hard STOP on parent/delegation/package/preimage/isolation failure, allowlist escape, payload/blob mismatch, Preserve mutation, State denyset failure, whitespace failure, invalid chain, unverified remote ref, mapping recount failure, cross-document lifecycle conflict or any newly required semantic choice.

## 6. Adapter-independent evidence

Return exact:

- Handoff Parent, claim and final-or-abort 40-character identities and their parents;
- remote target ref after fetch, claim push and final/abort push;
- claim and final tracked+untracked changed sets using NUL-safe Git semantics;
- all claim/final target Git blobs and modes compared with manifest;
- parent-bound Preserve blobs and zero-diff checks;
- State concrete-model denyset result;
- `git diff --check` for parent→claim and claim→final;
- record recount: 48 total, 48 unique, missing 0, duplicate 0, UNRESOLVED 48 and exact Legacy counts;
- lifecycle assertions and explicit CODE STOP confirmation.

Adapter logs are supporting evidence only. Remote Git objects and independent recounts decide acceptance.

## 7. Preserve Set and non-scope

Preserve the canonical GC registry, GC recurrence/provenance, M1 64 records, M1 Final Accept, M1→M2 Handoff, UBEA-v2, user-level canonical files, project stable entry, Project Graph, production code, tests, data, build and configuration.

Do not resolve any Universal Mapping, invent Universal Level count/name/threshold, finalize Task/Capability Profiles, edit canonical GC rows, mutate routing, split State responsibility, start M3, close M2, or start CookBook Phase 3B.

## 8. STOP ownership

Any package semantic defect is attributed to `BLUEPRINT_DEFECT / ARCH_PAYLOAD_DEFECT`, not to CODE capability. CODE is accountable only for faithful materialization, evidence and transaction discipline. After verified final or authorized abort push, CODE must STOP and return only the evidence summary; remote ARCH adjudicates the delivery.
