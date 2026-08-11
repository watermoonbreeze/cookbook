# Phase 2 → Phase 3 Handoff

> Phase 2 已 FINAL ACCEPT / FROZEN。本文件是 Phase 3 新会话的唯一治理交接入口；它授权后续设计，不代表 Phase 3 已开始实施。

## Handoff status

```text
From: Phase 2 FINAL ACCEPT / FROZEN
To: Phase 3
Phase 3 Status: AUTHORIZED / NOT STARTED
Phase 3 Mission: Views + Activation
Graph Mode: draft
Phase 2 Review Commit: fd3ded5e080fe772d820815366269fb536e463df
```

## Phase 3 must read first

1. `migration/PHASE2_FINAL_ACCEPT.md`
2. `migration/PHASE2E_ACCEPT.md`
3. `.ai-context/project_graph/README.md`
4. `.ai-context/project_graph/project.yaml`
5. `.ai-context/docs/context_memory/SESSION_交接.md`
6. `.ai-context/project_graph/migration/PHASE2E_VIEW_DRIFT.md`

## Authorized scope

Phase 3 may design and, after a new architecture blueprint is accepted, implement:

- Graph-derived AI_INDEX and human/AI views for project status, features, Todo, Bug, Plan, Verification and Current Work/Handoff。
- A thin SESSION handoff view derived from Project Graph truth。
- Renderer/lifecycle tooling and the activation readiness path。
- The explicit governance decision for `mode: draft → active`。

## Invariants to preserve

- Phase 1 Frozen Core Contract and all Phase 2 Project Truth entities/IDs。
- Feature Registry = 13 and all stable Feature/WorkItem/Plan/Verification identities。
- Verification closure and WorkItem status semantics。
- CurrentWork `F-AI-MEAL / K1i / verifying`。
- `E-K1G-01 = ACCEPTED_LEGACY / not_required` and L3 `F-TOOLS / RESOLVED_NO_REGISTRY_CHANGE`。
- One Truth, Multiple Views: generated views must not become independent truth sources。

## Explicit stop conditions

- Do not begin Phase 3 implementation in this handoff commit。
- Do not set Graph mode to `active`。
- Do not modify schema, validator contract, production code, Feature YAML, WorkItem/Verification/Plan/Relation semantics or legacy views。
- Do not create `PHASE3_*.md` or renderer code here。

## Next authorized action

Open a fresh Phase 3 control conversation, read the files above, issue a Phase 3 architecture blueprint, and wait for its acceptance before implementation.
