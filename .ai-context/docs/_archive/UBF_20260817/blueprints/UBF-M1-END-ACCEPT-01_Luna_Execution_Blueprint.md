# UBF-M1-END-ACCEPT-01 Luna Execution Blueprint

Document Role: Architecture-authored Mechanical Execution Task
Mechanism: STATIC_TARGET_BUNDLE
Review Mode: REMOTE_READ_ONLY_ARCH
Delegation: DELEGATED_SINGLE_TASK_CLAIM
Execution Model: GPT-5.6 Luna
Handoff Parent: `1723a4f9c050d4da47740d04164fa27d73ea9f2b`
Expected Return TURN: REVIEW

## 0. Frozen architecture decision

Remote ARCH accepts M1 Work-01 at `1723a4f9c050d4da47740d04164fa27d73ea9f2b`. CODE does not adjudicate acceptance.

The review verified the original 64-record decomposition and all repair transactions, including R4-REWORK-02 chain `aa45a286... -> e176a722... -> 1723a4f9...`, exact scopes/blobs, State concrete-model denyset, whitespace, Preserve blobs, remote ref and lifecycle gates. Open issue register: `NONE`.

This batch only persists that decision and creates the M1→M2 handoff. It does not execute or start M2.

## 1. Required final lifecycle

```text
M0: ACCEPT / CLOSED
M1: ACCEPT / CLOSED
M1→M2 Handoff: PERSISTED / PENDING REMOTE ARCH REVIEW
M2: NOT STARTED / NOT AUTHORIZED
CookBook Phase 3B: NOT AUTHORIZED TO START
TURN: REVIEW
```

## 2. Exact normal final allowlist

1. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-END-ACCEPT-01_Luna_Execution_Blueprint.md`
2. `A .ai-context/docs/UBF-M1-END-ACCEPT-01-Execution-Report.md`
3. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-FINAL-ACCEPT.md`
4. `A .ai-context/docs/项目改造规划/蓝图设计2/UBF-M1-to-M2-Handoff.md`
5. `M .ai-context/docs/项目改造规划/蓝图设计2/universal_blueprint_framework_implementation_control.md`
6. `M .ai-context/docs/experience/14_模型执行力评估.md`
7. `M .ai-context/docs/context_memory/BLUEPRINT_STATE.md`

No eighth path is allowed.

## 3. Preserve Set

The 64-record artifact, Work-01 artifact, all R4/rework evidence, canonical sources, GC history, Project Graph, production code/tests/data/build/configuration and unrelated documentation remain byte-identical.

State must contain none of `GPT-5.6`, `Luna`, `DeepSeek`, `Claude`, `Gemini`. Concrete model identity belongs only in the model evidence ledger and execution report.

## 4. Transaction

From an isolated clean environment at the exact Handoff Parent:

1. materialize only the claim State target;
2. verify manifest blob, exact one-path staged set, State denyset and whitespace;
3. commit `docs(governance): claim UBF M1 accept handoff turn`;
4. push and remote-verify; this commit becomes Execution Parent;
5. materialize all seven exact final targets without editing;
6. verify exact seven-path scope relative to claim and parent, seven target blobs, State denyset, whitespace, no untracked residue, Preserve blobs and lifecycle truth;
7. commit `docs(governance): persist UBF M1 accept and M2 handoff`;
8. push, remote-verify, return evidence and STOP.

If every authorized adapter is exhausted after claim, materialize only abort State, commit `docs(governance): record UBF M1 accept handoff compatibility block`, push, verify and STOP.

## 5. Compatibility / Hard STOP

Path separators, console encoding, Python/PowerShell/Bash differences, optional adapter startup and nonessential tool absence are soft compatibility; use an equivalent preauthorized native adapter.

Hard STOP on parent/delegation/package/preimage/isolation failure, allowlist escape, blob mismatch, Preserve mutation, State denyset/whitespace failure, invalid parent chain, unverified remote push/ref or an unclosed semantic choice.

## 6. Evidence and stop boundary

Return exact parent/claim/final-or-abort identities and parents, remote refs after each push, NUL-safe tracked+untracked sets, target/preimage/Preserve blobs, whitespace and State denyset results, lifecycle assertions and STOP confirmation. Adapter PASS text alone is insufficient.

Do not create M2 Preview/Start, mapping rows, Universal Level decisions, canonical mutations, State restructuring, routing mutation, Project Graph mutation or production changes. Do not start CookBook Phase 3B.
