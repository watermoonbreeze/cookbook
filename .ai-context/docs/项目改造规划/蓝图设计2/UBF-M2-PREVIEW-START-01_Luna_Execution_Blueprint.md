# UBF-M2-PREVIEW-START-01 Luna Execution Blueprint

Document Role: Architecture-authored Mechanical Entry Task
Mechanism: STATIC_TARGET_BUNDLE
Review Mode: REMOTE_READ_ONLY_ARCH
Delegation: DELEGATED_SINGLE_TASK_CLAIM
Execution Model: GPT-5.6 Luna
Handoff Parent: `2054899ad93d9c2bc1353914c31a1ef3b96c15ac`
Expected Return TURN: REVIEW

## 0. Frozen architecture decision

Remote ARCH accepts the M1 End/Accept + M1→M2 Handoff persistence at the Handoff Parent. Verified: `1723a4f9... -> 5650c5c5... -> 2054899a...`, claim exact one path, final exact seven paths, 7/7 target blobs, State concrete-model denyset, four Preserve blobs, clean whitespace and consistent M1/Handoff lifecycle. Open issue register: `NONE`.

This task only persists M2 Preview/Start and consumes the accepted handoff. It does not perform Legacy Asset Mapping.

## 1. Required final lifecycle

```text
M0/M1: ACCEPT / CLOSED
M1→M2 Handoff: ARCH ACCEPTED / CONSUMED
M2 Preview/Start: PERSISTED / PENDING REMOTE ARCH REVIEW
M2 Mapping: NOT EXECUTED / NOT YET AUTHORIZED
M3: NOT STARTED
CookBook Phase 3B: NOT AUTHORIZED TO START
TURN: REVIEW
```

## 2. Exact normal final allowlist

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M2-PREVIEW-START-01_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M2-PREVIEW-START-01-Execution-Report.md`
3. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M2-PREVIEW-START.md`
4. `M .ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-to-M2-Handoff.md`
5. `M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
6. `M .ai-context/docs/experience/14_模型执行力评估.md`
7. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No eighth path is allowed.

## 3. Entry contract

The future mapping task, if separately authorized after entry acceptance, must non-destructively create one record for each GC-01～GC-48 with at least:

- stable identity, Origin, Current Authority;
- Decision Category and Applicable Task Profile candidate;
- Closure Effect and Preserved Coder Discretion;
- Evidence Type, Legacy Level and Lifecycle Status;
- Universal Level Mapping, allowed and usually required to remain `UNRESOLVED`;
- source/evidence references and contradiction links.

Entry does not create any such record.

Frozen rules:

- Legacy L1～L7 is a GC coverage mechanism, not Universal Level.
- Universal Level concerns coder remaining decision space / architecture closure.
- GC count, old Level or checklist position cannot infer Universal mapping.
- FULL/LITE and Universal Level are orthogonal.
- routing tiers and research role are Actor/Capability Routing, not Level.
- project overlay cannot redefine Universal semantics.
- Source Identity Discovery precedes routing-source comparison or mutation.
- snapshots/generated views cannot silently become canonical Truth.

## 4. Preserve Set

Preserve M1 Final Accept, the 64-record decomposition, Work-01/R4 repair evidence, GC-01～GC-48 identities/provenance, canonical sources, Project Graph, application code/tests/data/build/configuration and unrelated docs.

State must contain none of `GPT-5.6`, `Luna`, `DeepSeek`, `Claude`, `Gemini`.

## 5. Transaction

From an isolated clean environment at the exact Handoff Parent:

1. materialize claim State only; verify exact scope/blob/State denyset/whitespace;
2. commit `docs(governance): claim UBF M2 preview start turn`, push and remote-verify;
3. materialize all seven final targets without editing;
4. verify exact seven-path scope relative to claim and parent, target blobs, State denyset, whitespace, no untracked residue, Preserve blobs and mapping non-start;
5. commit `docs(governance): persist UBF M2 preview start`, push, remote-verify and STOP.

Compatibility abort after claim uses abort State only and commit `docs(governance): record UBF M2 preview compatibility block`.

## 6. Compatibility, evidence and STOP

Adapter/path/shell/encoding/tool differences are soft compatibility with automatic equivalent fallback. Hard STOP on parent/delegation/package/preimage/isolation failure, allowlist escape, blob mismatch, Preserve mutation, denyset/whitespace failure, mapping creation, invalid chain, unverified remote ref or unclosed semantic choice.

Return exact parent/claim/final-or-abort identities and parents, remote refs, NUL-safe tracked+untracked sets, target/preimage/Preserve blobs, State denyset, whitespace, lifecycle/mapping non-start assertions and STOP confirmation.

Do not create GC mapping rows, choose Universal Levels, start M3, mutate canonical/fallback/State ownership/routing/Project Graph/production code, or start CookBook Phase 3B.
