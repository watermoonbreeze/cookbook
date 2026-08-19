# Phase 3 Architecture Accept Record

> Immutable architecture review record. This is not Project Truth, lifecycle state, or a generated view.

- Baseline: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Architecture Decision: `ACCEPT`
- Phase 3 Mission: Views + Activation
- Graph Mode at Architecture Acceptance: `draft`

## Architecture Decisions

1. One Truth, Multiple Views.
2. Project Graph remains Project Truth.
3. Renderer is one-way: Graph → Views.
4. Renderer must never mutate Graph.
5. Formal renderer CLI direction remains `pg render`.
6. Renderer contract will require deterministic behavior.
7. Drift-check mode must be zero-write.
8. Renderer failure model is fail-closed.
9. `PROJECT.md` remains the stable entry/pointer layer.
10. `AI_INDEX` is a generated current-state view, not replacement Project Truth.
11. `SESSION` is a thin handoff target, not independent truth.
12. Legacy views require classify → migrate/hybrid/retire, not dual-write.
13. Activation Readiness and draft→active are separate phases.
14. draft→active occurs only in the final isolated activation batch.
15. Phase 3 is decomposed 3A→3J, with explicit STOP/Handoff between accepted subphases.

## Not Frozen by This Record

Current WorkItem status, current Verification status, CurrentWork future transitions,
derived counts, final generated-file list, renderer file layout, functional-path disposition,
activation date, and exact Phase 3A implementation details remain open to their owning phases.

