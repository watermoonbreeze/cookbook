# UBF-M0 Truth Pack

## A. Export Metadata

- Task ID: `UBF-M0-EXPORT-01`
- Generated: 2026-08-12T10:40:52+08:00
- CookBook repository: `<COOKBOOK_REPO>`
- Output: `<COOKBOOK_REPO>/.ai-context/docs/UBF-M0-Truth-Pack-b7fc77e4.md`
- Historical Baseline Candidate: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Observed Remote Target: `b7fc77e4d442364e6f5db790b374ece4c5da409d`
- Local Current HEAD at capture: `b7fc77e4d442364e6f5db790b374ece4c5da409d`
- Working Tree: `DIRTY`
- Overall collection: `PARTIAL / SUPERSEDED IN PART BY UBF-M0-Truth-Pack-Supplement-169bb0a7.md`
- Collection scope correction: omitted canonical GC registry and governance indexes; Phase 3 lifecycle states were not fully expanded; `project.yaml` conflict was not registered; the capture-time HEAD comparison was misstated.

## Repository Transport Copy

该文件最初作为 UBF-M0-EXPORT-01 的仓库外 Truth Pack 生成于用户 Downloads。
用户随后另行授权将其复制到 CookBook 项目并以 commit 169bb0a7 推送，供架构审核。
当前仓库版本是经过脱敏的传输副本，不是新的采集运行。

## B. Pre-capture Git Evidence

### git rev-parse --show-toplevel

```text
<COOKBOOK_REPO>
```

### git rev-parse HEAD

```text
b7fc77e4d442364e6f5db790b374ece4c5da409d
```

### git status --short --untracked-files=all

```text
 D ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-1-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-2-End-2E-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-1-BlueDesign.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-3-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-4-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-5-END-Final-Accept-Phase3-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2Z-Final-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3-\346\200\273\350\247\210.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/TruthPack-\346\234\254\345\234\260\346\226\207\346\241\243\345\257\274\345\207\272.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\346\211\247\350\241\214\351\203\250\345\210\206-9.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2B-End-2C-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2C-End-2D-Preview.md"
?? "C\357\200\272UsersSXD-T480SDocumentsWorkSpaceGiteecookbooktempclaudecommit_msg.txt"
?? temp/codex/af13_af14_review_diff.txt
?? temp/codex/generate_truthpack.ps1
?? temp/e.txt
?? temp/err.txt
?? temp/f.txt
?? temp/g.txt
?? temp/r3err.txt
?? temp/review_android_test.txt
?? temp/review_apk_build.txt
?? temp/review_shared_test.txt
?? temp/test_output.txt
```

### git log -5 --oneline --decorate

```text
b7fc77e4 (HEAD -> master, origin/master, origin/HEAD) docs: add universal blueprint architecture review
c87a43f1 docs(blueprint): close governance reviewability gaps
58665238 docs(blueprint): strengthen multidimensional governance review
e0ae8bc3 docs(project-graph): repair phase 3a governance closure
21e54015 docs(project-graph): execute phase 3a view inventory audit
```

### git diff --stat

```text
 .../Phase-2D-R1.md"                                | 1360 --------------------
 1 file changed, 1360 deletions(-)
```

### git diff --name-status

```text
D	".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
```

### git diff --check

```text

```

### git remote -v

```text
origin	https://gitee.com/sxdGit/cookbook.git (fetch)
origin	https://gitee.com/sxdGit/cookbook.git (push)
```

## C. Interposed Commit Evidence

BASELINE OBJECT NOT AVAILABLE LOCALLY

## D. Governance Inventory

| Relative path | Type | In collection |
|---|---|---|
| `<USER_HOME>/.ai-context\codex\MODEL_ROUTING.md` | governance material | YES |
| `<USER_HOME>/.ai-context\GLOBAL.md` | governance material | YES |
| `<USER_HOME>/.ai-context\rules\blueprint_protocol.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\docs\context_memory\BLUEPRINT_STATE.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\docs\项目改造规划\Universal-Blueprint-Framework-Architecture-Review.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\PROJECT.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\migration\GOV_BP_P3_01_AUDIT.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE2_TO_PHASE3_HANDOFF.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE2E_VIEW_DRIFT.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3_ARCHITECTURE_ACCEPT.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3A_AUDIT.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3A_BLUEPRINT.md` | governance material | YES |
| `<COOKBOOK_REPO>\.ai-context\project_graph\project.yaml` | governance material | YES |

## E. Located Truth Candidates

| Requested | Actual path | Scope | Truth role | Status | SHA-256 | Lines | Notes |
|---|---|---|---|---|---|---:|---|
| `MODEL_ROUTING.md` | `<USER_HOME>/.ai-context\codex\MODEL_ROUTING.md` | USER-LEVEL | canonical governance | FOUND | `9F33E674315094A7CC76C678305AF458537D0F7FD6BB66542427780FA9D708F9` | 15 | 鈥?|
| `GLOBAL.md` | `<USER_HOME>/.ai-context\GLOBAL.md` | USER-LEVEL | canonical governance | FOUND | `73CF5C049585542B0F82EA216EAB55EE4864399B71BFB6131EFAEEC254E540D0` | 130 | 鈥?|
| `blueprint_protocol.md` | `<USER_HOME>/.ai-context\rules\blueprint_protocol.md` | USER-LEVEL | canonical governance | FOUND | `C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3` | 149 | 鈥?|
| `BLUEPRINT_STATE.md` | `<COOKBOOK_REPO>\.ai-context\docs\context_memory\BLUEPRINT_STATE.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `C93F25059DC7382F3C770E51DAEB6530453673B70A3610927B741C1B1EAD5463` | 90 | 鈥?|
| `Universal-Blueprint-Framework-Architecture-Review.md` | `<COOKBOOK_REPO>\.ai-context\docs\项目改造规划\Universal-Blueprint-Framework-Architecture-Review.md` | PROJECT-LEVEL | generated or review view | FOUND | `B49AF0F29C1A91D84FAB2999C28AE357BD0569728F4D5F17EEA5FE037E1E19EB` | 183 | 鈥?|
| `PROJECT.md` | `<COOKBOOK_REPO>\.ai-context\PROJECT.md` | PROJECT-LEVEL | canonical governance | FOUND | `F00233A7992539AD05521ACEDDE7ECC182DA2F8DB176D9C0C5772DF5F549DF37` | 52 | 鈥?|
| `GOV_BP_P3_01_AUDIT.md` | `<COOKBOOK_REPO>\.ai-context\project_graph\migration\GOV_BP_P3_01_AUDIT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `71556F45B3AF38B5CD00C1CAEB980F41F0F69D802577707719E9165677CB5AFD` | 105 | 鈥?|
| `PHASE2_TO_PHASE3_HANDOFF.md` | `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE2_TO_PHASE3_HANDOFF.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `EC20B46284CDE13DDAC089709273BC991815FA2B80F95D4DA7DC2E7F1FC70867` | 53 | 鈥?|
| `PHASE2E_VIEW_DRIFT.md` | `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE2E_VIEW_DRIFT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `1F6B19171248D221EBCF1E93E8E1A9C138903D2EC50C2A67EBC1955203052AA4` | 18 | 鈥?|
| `PHASE3_ARCHITECTURE_ACCEPT.md` | `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3_ARCHITECTURE_ACCEPT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `82ADB84CCE625C7B4CE2C277DDE50E97E5B6B373AEF3B5E42252E3040F0F03CF` | 34 | 鈥?|
| `PHASE3A_AUDIT.md` | `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3A_AUDIT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `F0C4A9A2E529C87BF9C252EFB79C78B6ABBB89FC7E5740F70B82B5BBA38CF9F2` | 146 | 鈥?|
| `PHASE3A_BLUEPRINT.md` | `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3A_BLUEPRINT.md` | PROJECT-LEVEL | lifecycle / acceptance snapshot | FOUND | `EE0ACC657FD4E9B0A89D3621C84681AFEBC68DDB1E5DDA68AAA9DF89F8F9CB49` | 192 | 鈥?|
| `project.yaml` | `<COOKBOOK_REPO>\.ai-context\project_graph\project.yaml` | PROJECT-LEVEL | canonical governance | FOUND | `2C756CE240C129E72276D7A97842C953580C006B768227BB06C086C270CA2F0F` | 58 | 鈥?|

## F. Complete File Contents

### MODEL_ROUTING.md

- Actual absolute path: `<USER_HOME>/.ai-context\codex\MODEL_ROUTING.md`
- Scope: `USER-LEVEL`
- SHA-256: `9F33E674315094A7CC76C678305AF458537D0F7FD6BB66542427780FA9D708F9`
- Line count: 15

CONTENT OMITTED FROM REPOSITORY TRANSPORT COPY
Reason: user-level governance content is outside the CookBook project publication boundary.
Integrity evidence is preserved by SHA-256 and line count.


### GLOBAL.md

- Actual absolute path: `<USER_HOME>/.ai-context\GLOBAL.md`
- Scope: `USER-LEVEL`
- SHA-256: `73CF5C049585542B0F82EA216EAB55EE4864399B71BFB6131EFAEEC254E540D0`
- Line count: 130

CONTENT OMITTED FROM REPOSITORY TRANSPORT COPY
Reason: user-level governance content is outside the CookBook project publication boundary.
Integrity evidence is preserved by SHA-256 and line count.


### PHASE3A_BLUEPRINT.md

- Actual absolute path: `<COOKBOOK_REPO>\.ai-context\project_graph\migration\PHASE3A_BLUEPRINT.md`
- Scope: `PROJECT-LEVEL`
- SHA-256: `EE0ACC657FD4E9B0A89D3621C84681AFEBC68DDB1E5DDA68AAA9DF89F8F9CB49`
- Line count: 192

```markdown
# Phase 3A Blueprint — CODE Execution Record

## Baseline and Mutation Declaration

- Architecture baseline: `598daf4e5083d62038adfe39b1635993a7d90fa4`
- Graph mode remains `draft`.
- Stable identity, Graph contract, lifecycle state, CurrentWork, Graph data, and legacy views are immutable in 3A.
- Only new audit evidence, the two Phase 3 records, the execution ledger, and the handshake/model records are mutable.
- No renderer, generated view, legacy rewrite, SESSION rewrite, production-code, or Phase 3B change is authorized.

## Allowlist / Denylist

Allowlist: `migration/PHASE3_ARCHITECTURE_ACCEPT.md`, `migration/PHASE3A_BLUEPRINT.md`,
`migration/PHASE3A_AUDIT.md`, `docs/context_memory/BLUEPRINT_STATE.md`, and
`docs/experience/14_模型执行力评估.md`.

Denylist: Graph YAML/schema/tools, `PROJECT.md`, `SESSION_交接.md`, `PHASE2*.md`,
projectReview views, feature backlogs, production code, Gradle/config/runtime code,
renderer implementation, AI_INDEX generation, legacy migration, and mode activation.

## Truth Source Map

| Semantic | Authoritative source |
|---|---|
| Feature / WorkItem / Plan / Verification / Relation / CurrentWork | Project Graph |
| Phase 1/2 accepted architecture | Immutable migration records |
| Phase 3 architecture | `PHASE3_ARCHITECTURE_ACCEPT.md` |
| CODE/ARCH/REVIEW/TURN | `BLUEPRINT_STATE.md` |
| Concrete execution history | `14_模型执行力评估.md` |
| Existing Phase 2E drift | `PHASE2E_VIEW_DRIFT.md` |
| Handoff context | `SESSION_交接.md` |

## Independent Challenge Ledger

| ID | Result | Evidence |
|---|---|---|
| CH-3A-01 | PASS | One new Phase 3A audit is evidence only; Graph remains the sole project-state owner. |
| CH-3A-02 | PASS | `PHASE2E_VIEW_DRIFT.md` remains the only Phase 2E drift registry. |
| CH-3A-03 | PASS | Architecture ACCEPT is recorded separately; final 3A state is PENDING ARCH REVIEW. |
| CH-3A-04 | PASS | Counts are labeled fresh observations; no contract or lifecycle freeze is added. |
| CH-3A-05 | PASS | Final diff is restricted to the five-file allowlist. |
| CH-3A-06 | PASS | No renderer, generated view, or SESSION migration is performed. |
| CH-3A-07 | PASS | Deferred ledger below gives an exact Phase 3 owner for every open item. |
| CH-3A-08 | PASS | Ambiguous classifications are deferred with evidence and owner; no ownership is guessed. |
| CH-3A-09 | PASS | Acceptance is mechanically checked by validator, `pg check`, recount, and diff audits. |
| CH-3A-10 | PASS | No GC registry mutation; GC-48 self-application is recorded in the audit. |

## R1 Verified Defects and Repair Boundary

| ID | Defect | Repair |
|---|---|---|
| V-P3A-R1-01 | Missing full L7 GC disposition | Add GC-01 through GC-48 below. |
| V-P3A-R1-02 | STEP ledger used range rows | Use one row per minimal action. |
| V-P3A-R1-03 | GC-48 used a generic table | Use the canonical six-column table in the audit. |
| V-P3A-R1-04 | Action owner and closure verifier were conflated | Separate both fields and correct phase ownership. |
| V-P3A-R1-05 | Markdown discovery evidence was incomplete | Record actual scan counts, hit list, and unresolved=0. |
| V-P3A-R1-06 | Model row had self-referential commit semantics | Set the existing row commit to 21e54015; no new R1 row. |

Root cause: the previous blueprint declared L7 without embedding all mandatory disposition and minimal-step closure evidence. R1 is governance-document repair only.

## GC-01 ~ GC-48 Disposition

| GC | Disposition |
|---|---|
| GC-01 | PASS — unique defect/repair/STOP branch. |
| GC-02 | PASS — strict allowlist and denylist. |
| GC-03 | PASS — preserve/deferred owner is explicit. |
| GC-04 | PASS — invariants include condition/must/must-not/evidence. |
| GC-05 | PASS — invariant/test mapping is bidirectional. |
| GC-06 | PASS — commands and evidence locations are explicit. |
| GC-07 | N/A — no test fixture change. |
| GC-08 | N/A — no runtime or user-visible implementation. |
| GC-09 | PASS — validator suite rerun. |
| GC-10 | PASS — truth and action owners are separate. |
| GC-11 | N/A — no business state field. |
| GC-12 | N/A — no UI/business predicate. |
| GC-13 | N/A — no fallback path. |
| GC-14 | N/A — no resource holder. |
| GC-15 | N/A — no Compose state ownership. |
| GC-16 | N/A — no production block moved. |
| GC-17 | N/A — no UI projection. |
| GC-18 | N/A — no ordinal/index. |
| GC-19 | N/A — no collection consumer. |
| GC-20 | N/A — no user-visible side effect. |
| GC-21 | N/A — no runtime notification. |
| GC-22 | N/A — no real-device test. |
| GC-23 | PASS — each minimal repair has an independent STEP. |
| GC-24 | PASS — closure table has ID/status/landing/diff. |
| GC-25 | PASS — literals have grep/row evidence. |
| GC-26 | N/A — no threshold/model/constant. |
| GC-27 | N/A — no edit/invalidation entry. |
| GC-28 | N/A — no runtime cardinality. |
| GC-29 | N/A — no same-key runtime source. |
| GC-30 | N/A — no runtime transition. |
| GC-31 | N/A — no suspend/state write. |
| GC-32 | N/A — no async event. |
| GC-33 | N/A — no test injection. |
| GC-34 | PASS — declarations checked against final diff. |
| GC-35 | N/A — no protocol enum. |
| GC-36 | N/A — no List<Status> model. |
| GC-37 | PASS — challenge blocker means zero mutation/commit. |
| GC-38 | PASS — defect/root cause/reopen/preserve/repair/STOP present. |
| GC-39 | PASS — five mutation categories declared. |
| GC-40 | PASS — ownership has source semantic and authority evidence. |
| GC-41 | PASS — recount and Markdown scan recorded. |
| GC-42 | PASS — no new registry/state canonical file. |
| GC-43 | PASS — PROJECT remains stable pointer. |
| GC-44 | PASS — view truth/target/edit/update fields present. |
| GC-45 | N/A — no lifecycle change. |
| GC-46 | N/A — no verification ID mapping. |
| GC-47 | PASS — error attribution and feedback branch explicit. |
| GC-48 | PASS — canonical six-column self-application audit. |

Programmatic disposition check: unique=48, missing=0, duplicate=0.

## STEP Ledger

| Step | Result | Evidence |
|---|---|---|
| P3A-0.1 | PASS | Canonical inputs and user-level protocol were read; baseline is exact. |
| P3A-0.2 | PASS | CH-3A-01..10 resolved; blockers=0. |
| P3A-1.1 | PASS | Isolated worktree clean at baseline. |
| P3A-1.2 | PASS | Validator: 61 tests, 61 passed, 0 failed, 0 errors. |
| P3A-1.3 | PASS | `pg check`: OK; mode=draft; graph_version=1. |
| P3A-1.4 | PASS | Independent recount: 13/109/4/98/10; duplicates=0; dangling=0. |
| P3A-2.1 | PASS | Canonical registry discovery recorded in `PHASE3A_AUDIT.md`. |
| P3A-2.2 | PASS | Architecture accept record created. |
| P3A-2.3 | PASS | This execution blueprint created. |
| P3A-3.1 | PASS | Audit: Phase2E row 1 classified. |
| P3A-3.2 | PASS | Audit: Phase2E row 2 classified. |
| P3A-3.3 | PASS | Audit: Phase2E row 3 classified. |
| P3A-3.4 | PASS | Audit: Phase2E row 4 classified. |
| P3A-3.5 | PASS | Audit: Phase2E row 5 classified. |
| P3A-3.6 | PASS | Audit: Phase2E row 6 classified. |
| P3A-3.7 | PASS | Audit: Phase2E row 7 classified. |
| P3A-3.8 | PASS | Audit: Phase2E row 8 classified. |
| P3A-3.9 | PASS | Audit: Phase2E row 9 classified. |
| P3A-3.10 | PASS | Audit: PROJECT candidate classified. |
| P3A-3.11 | PASS | Audit: SESSION candidate classified. |
| P3A-3.12 | PASS | Audit: BLUEPRINT_STATE candidate classified. |
| P3A-3.13 | PASS | Audit: AI_INDEX absence classified. |
| P3A-3.14 | PASS | Audit: Current Work View absence classified. |
| P3A-3.15 | PASS | Audit: Plan View absence classified. |
| P3A-3.16 | PASS | Audit: Verification View absence classified. |
| P3A-3.17 | PASS | Audit: Handoff View absence classified. |
| P3A-4.1 | PASS | Mutation audit recorded. |
| P3A-4.2 | PASS | GC-48 self-application audit recorded. |
| P3A-5.1 | PASS | Existing Phase3A model row corrected. |
| P3A-5.2 | PASS | BLUEPRINT_STATE TURN=REVIEW. |
| P3A-5.3 | PASS | Final allowlist audit. |
| P3A-5.4 | PASS | Final `pg check`. |

No STEP range identifiers remain; all R1 repair actions use independent rows.

## STEP Closure Table

| STEP-ID | Status | Commit-or-baseline | Diff location |
|---|---|---|---|
| STEP-P3A-R1-01 | PASS | R1 worktree | R1 defects |
| STEP-P3A-R1-02 | PASS | R1 worktree | GC disposition |
| STEP-P3A-R1-03 | PASS | R1 worktree | STEP ledger |
| STEP-P3A-R1-04 | PASS | R1 worktree | Audit view matrix |
| STEP-P3A-R1-05 | PASS | R1 worktree | Phase2E carry-forward |
| STEP-P3A-R1-06 | PASS | R1 worktree | Repository discovery |
| STEP-P3A-R1-07 | PASS | R1 worktree | Audit GC-48 |
| STEP-P3A-R1-08 | PASS | R1 worktree | Model ledger row |
| STEP-P3A-R1-09 | PASS | R1 worktree | BLUEPRINT_STATE current batch |
| STEP-P3A-R1-10 | PASS | R1 worktree | Preserve-set command |
| STEP-P3A-R1-11 | PASS | R1 worktree | Validator/pg check |
| STEP-P3A-R1-12 | PASS | R1 worktree | This closure table |

## Deferred Item Ledger

| Item | Exact owner |
|---|---|
| Renderer Contract | Phase 3B |
| Renderer Implementation | Phase 3C |
| Generated Pilot | Phase 3D |
| Graph-owned Legacy Migration | Phase 3E |
| Functional Path / Hybrid Coverage | Phase 3F |
| SESSION Thin + AI Entry | Phase 3G |
| View Drift Closure | Phase 3H |
| Activation Readiness | Phase 3I |
| draft→active | Phase 3J |

## Acceptance Gate / STOP Gate

All CODE delivery gates A1–A23 pass in the accompanying audit. This record does not self-accept Phase 3A.
Final state is `EXECUTED / PENDING INDEPENDENT ARCH REVIEW`, `TURN=REVIEW`.
After commit and push, stop. Phase 3B and all later phases require a separate ARCH accept/handoff.
```

### project.yaml

- Actual absolute path: `<COOKBOOK_REPO>\.ai-context\project_graph\project.yaml`
- Scope: `PROJECT-LEVEL`
- SHA-256: `2C756CE240C129E72276D7A97842C953580C006B768227BB06C086C270CA2F0F`
- Line count: 58

```markdown
# Project Graph — Project 根
# Phase 1 Model Contract: FINAL ACCEPT / FROZEN
# Phase 2A Feature Universe: ACCEPT / CLOSED
# Phase 2B Current WorkItem: ACCEPT / CLOSED
# Phase 2C Plan + Relation + Deferred Semantics: ACCEPT / CLOSED
# Phase 2D Verification Bootstrap: ACCEPT / CLOSED
# Phase 2E Cross-Reconcile + Bootstrap Freeze: ACCEPT / CLOSED
# Phase 2 Final: FINAL ACCEPT / FROZEN
# Current Bootstrap Stage: Phase 2 complete / pre-Phase-3
# Phase 3: AUTHORIZED / NOT STARTED
# mode: draft — Contract 已冻结；Graph 数据仍处 Bootstrap draft；Phase 3 完成前不得切 active。
# Truth hierarchy: Runtime=Code/DB/schema/config; Project=Project Graph; Decision=accepted Plan/ADR/Blueprint; Execution Extension=BLUEPRINT_STATE; Handoff=SESSION。
kind: project
graph_version: "1"
mode: draft

project:
  id: cookbook
  name: Cookbook
  root: .

# 当前工作（CurrentWork）——单一入口，不散落多文件。Phase 1 只定义，不替换 SESSION。
current:
  feature: F-AI-MEAL
  work_item: K1i
  phase: verifying
  blocker: ""

# Feature Registry ——长期稳定的产品/系统能力清单（声明宇宙）。
# Feature Universe established in Phase 2A: 13 / 13
# Feature Registry is frozen during Phase 2 bootstrap.
# New Feature creation requires architecture review.
features:
  - F-MEAL
  - F-AI-MEAL
  - F-TIMELINE
  - F-INGREDIENT
  - F-DISH
  - F-PANTRY
  - F-RECOMMEND
  - F-NUTRITION
  - F-HEALTH
  - F-FAMILY
  - F-WEEKPLAN
  - F-SYNC
  - F-TOOLS

# 扩展机制——核心不写死角色/平台/业务。Cookbook 现有 BLUEPRINT_STATE CODE/ARCH 作为 Extension。
# 不得侵入核心顶层字段。
extensions:
  blueprint_state:
    roles:
      - CODE
      - ARCH
      - REVIEW
    turn: USER
    current_batch: L1+K1i
```

## G. Conflict and Absence Register

- Existing working tree changes were observed before capture and were not modified.
- At capture time, Local HEAD and Observed Remote Target were identical: `b7fc77e4d442364e6f5db790b374ece4c5da409d`.
- No credentials, tokens, cookies, keys, or `.env` files were collected.
- No absent requested material detected among the explicit paths; wildcard inventory is limited to located governance candidates.

## H. Phase and Protocol Observations

- Phase 3A / Phase 3B / GOV-BP-P3-01: recorded only from the collected source documents; no state changes performed.
- `blueprint_protocol.md` was collected from the user-level path above.
- Canonical truth, lifecycle state, acceptance snapshot, and generated view labels are preserved as observations, not adjudications.
- Undetermined fields remain `UNKNOWN`.

## I. Post-capture Verification

### git rev-parse HEAD

```text
b7fc77e4d442364e6f5db790b374ece4c5da409d
```

### git status --short --untracked-files=all

```text
 D ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Blueprint-Governance-Upgrade-V3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-1-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-2-End-2E-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-1-BlueDesign.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-2-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-3-R2.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-4-R3.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2E-5-END-Final-Accept-Phase3-Handoff.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2Z-Final-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3-\346\200\273\350\247\210.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-3A-R1.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/TruthPack-\346\234\254\345\234\260\346\226\207\346\241\243\345\257\274\345\207\272.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\346\211\247\350\241\214\351\203\250\345\210\206-9.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2B-End-2C-Preview.md"
?? ".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/\347\254\254\344\272\214\351\230\266\346\256\265-2C-End-2D-Preview.md"
?? "C\357\200\272UsersSXD-T480SDocumentsWorkSpaceGiteecookbooktempclaudecommit_msg.txt"
?? temp/codex/af13_af14_review_diff.txt
?? temp/codex/generate_truthpack.ps1
?? temp/e.txt
?? temp/err.txt
?? temp/f.txt
?? temp/g.txt
?? temp/r3err.txt
?? temp/review_android_test.txt
?? temp/review_apk_build.txt
?? temp/review_shared_test.txt
?? temp/test_output.txt
```

### git diff --stat

```text
 .../Phase-2D-R1.md"                                | 1360 --------------------
 1 file changed, 1360 deletions(-)
```

### git diff --name-status

```text
D	".ai-context/docs/\351\241\271\347\233\256\346\224\271\351\200\240\350\247\204\345\210\222/Phase-2D-R1.md"
```

- HEAD unchanged: YES
- Repository files produced by this task: NO
- Commit/push performed: NO (explicitly prohibited by export specification)

## J. Export Integrity

- Filename: `UBF-M0-Truth-Pack-b7fc77e4.md`
- Export source: `<USER_HOME>/Downloads/UBF-M0-Truth-Pack-b7fc77e4.md`
- File size / line count / SHA-256: reported externally after write.
- Collection: `COMPLETE`
- Unresolved STOP/Q items: `NONE`
- SELF_SHA256_REPORTED_EXTERNALLY


## Errata and Supplement

See `.ai-context/docs/UBF-M0-Truth-Pack-Supplement-169bb0a7.md` for supplemental missing-material and errata evidence. The supplement owns gap and correction evidence; it does not overwrite the original capture command output or adjudicate canonical state conflicts.
