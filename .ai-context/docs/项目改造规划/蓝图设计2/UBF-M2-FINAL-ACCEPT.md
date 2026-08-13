# UBF-M2 Final Accept

Document Role: Acceptance Snapshot

Stage: `M2 — Legacy Asset Mapping`

Architecture Decision: **ACCEPT / CLOSED**

Decision Date: 2026-08-13

Reviewed Delivery: `84cd8508e213e3664ec898cd2b9a783570b28de5`

Persistence Task: `UBF-M2-END-ACCEPT-01`

## 1. Decision

Remote ARCH independently accepts M2 Work-01 at the reviewed delivery. This decision predates persistence and is not delegated to CODE.

## 2. Verified evidence

- chain `c72a19b257550de7bb75dc9361b9f939fc220cb9 -> 416e36199cb47eacba0b10de0170af5082b42c83 -> 84cd8508e213e3664ec898cd2b9a783570b28de5`;
- claim exact one State path and final exact seven paths;
- claim and 7/7 final target Git blobs equal the architecture manifest;
- mapping total=48, unique=48, missing=0, duplicate=0;
- Legacy assignment L1=9, L2=8, L3=9, L4=7, L5=5, L6=4, L7=6;
- Universal Mapping `UNRESOLVED=48` and preserved lifecycle=48;
- State concrete-model denyset, whitespace, Preserve blobs, remote ref and lifecycle gates clean;
- open issue register: `NONE`.

## 3. Accepted outputs

M2 accepts the exact GC-01～GC-48 mapping inventory with stable identity, Origin, Current Authority, Decision Category, Applicable Task Profile candidate, Closure Effect, Preserved Coder Discretion, Evidence Type, Legacy Level, Universal Mapping, Lifecycle Status and evidence/contradiction references.

It also accepts these architecture conclusions:

- existing L1～L7 is retained only as Legacy GC-coverage vocabulary/evidence;
- GC identity/history/provenance remains active and preserved;
- GC coverage is a closure/evidence mechanism, not a Universal Level definition;
- every row's `PRESERVE_IDENTITY + REDEFINE_LEVEL_RELATION` decision is frozen for migration;
- 48/48 `UNRESOLVED` is honest completion of M2, not permission to infer mappings;
- Task Profile labels are candidate workload-shape families, not a final schema or Universal Level.

## 4. Preserved unresolved decisions

M2 does not decide:

- Universal Level count, names, thresholds, closure envelopes or selector;
- any Legacy L1～L7 or GC-01～GC-48 → Universal Level mapping;
- final Task Profile or Capability Profile schema;
- canonical protocol mutation, TURN/delegation rewrite, State handshake/history split or routing sibling identity;
- Project Graph repair or CookBook Phase 3B.

These are later-stage inputs, not M2 defects.

## 5. Transition gate

M2 is semantically `ACCEPT / CLOSED`. M3 is not started by this decision.

Required order: persist this decision and M2→M3 handoff; remotely accept that persistence; issue a separate M3 Preview/Start; remotely accept entry; only then authorize empirical corpus work.

Until this persistence is accepted:

```text
M2→M3 Handoff: PERSISTED / PENDING REMOTE ARCH REVIEW
M3: NOT STARTED / NOT AUTHORIZED
CookBook Phase 3B: NOT AUTHORIZED TO START
```
