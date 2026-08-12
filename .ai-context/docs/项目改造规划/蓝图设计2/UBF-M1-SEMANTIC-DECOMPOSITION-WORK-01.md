# Universal Blueprint Framework — M1 Semantic Decomposition Work-01

Document Role: Architecture-authored M1 semantic decomposition / generated analysis view
Stage: `M1 — Current-State Semantic Decomposition`
Handoff Parent: `795d2b9c807fe3954f1ac5f4cda60392c7ff9cc9`
Status: `ARCHITECTURE PAYLOAD FROZEN / PENDING CODE PERSISTENCE AND REMOTE ARCH REVIEW`
CookBook Phase 3B: `NOT AUTHORIZED TO START`

## 1. Scope and evidence boundary

This work classifies the **UBF-relevant governance clauses** only. It does not rewrite user-level canonical files, Project Graph, production code, tests, or legacy GC metadata. Android/Maven/project-documentation rules in GLOBAL that do not govern Blueprint collaboration/routing/review are explicitly excluded by scope.

### Current user-level direct inputs

| Source | Current role | Raw SHA-256 | Lines | M0 continuity |
|---|---|---|---:|---|
| `<USER_HOME>/.ai-context/rules/blueprint_protocol.md` | C-mode canonical | `c2c8332eb12d545ca89fca4c80a15dba7e2acf5faf7703a8cfe6815a0b5f0eb3` | 148 | same raw hash as M0 |
| `<USER_HOME>/.ai-context/GLOBAL.md` | global collaboration canonical | `73cf5c049585542b0f82ea216eab55ee4864399b71bfb6131efaeec254e540d0` | 129 | same raw hash as M0 |
| `<USER_HOME>/.ai-context/MODEL_ROUTING.md` | shared routing canonical named by current GLOBAL | `86b3dec955420552cbe7bcf5bc147478af06b67a6a95c0bf09cd80639bf636be` | 53 | **not the same source identity M0 catalogued**; see C-13 |

Important correction: M0 recorded `<USER_HOME>/.ai-context/codex/MODEL_ROUTING.md` at `9f33e674...`; this Work-01 received root `<USER_HOME>/.ai-context/MODEL_ROUTING.md` because current GLOBAL names that shared file first. Without the current codex-specific file, it is **not valid** to call `86b3...` a hash drift of the same object. The correct finding is a source-identity/collection gap; it is non-blocking for shared UBF semantics and becomes a continuity gate before any later routing mutation.

### Repository inputs at Handoff Parent

| Source | Role in M1 | SHA-256 / identity |
|---|---|---|
| `docs/experience/12_多模型协作与实施蓝图规范.md` | CookBook fallback + canonical GC registry | `0a58da55219ce134095c3a15881205124174b74994e47b58168a91c1f402c827` |
| `.ai-context/PROJECT.md` | project stable pointer / overlay activation | `bc044075d4aa18ddcbeacd478118ab9ff5a62b65af75fca18f464cb4bbd610c4` |
| `BLUEPRINT_STATE.md` | current handshake lifecycle Truth | fixed Handoff commit/tree object; repository filesystem SHA is not architecture-reconstructed |
| `14_模型执行力评估.md` | concrete model evidence canonical | fixed Handoff commit/tree object; semantic anchor verified before mutation |
| M0 Truth Pack + Supplement | acceptance snapshot / historical evidence | retained, no mutation |
| UBF Implementation Control | migration target/control, not user-level canonical | current R7 at parent |

### Architecture package correction recorded during persistence

`ARCH-PAYLOAD-W01-02`: Work-01 R2 used repository preimage SHA values copied from an architecture-side reconstructed fake Git fixture. Luna correctly stopped because the real fixed Handoff tree did not match that non-authoritative fixture hash. R3 changes repository preimage evidence to `COMMIT_TREE_BOUND`: exact remote Handoff commit + isolated clean HEAD + tracked-object presence + transform-specific semantic anchors. External user-level canonical files continue to use direct RAW-byte SHA-256 continuity gates. This defect is architecture-attributed and must not count as a negative coder-capability sample.

## 2. Inventory schema

Each row is one normalized governance proposition. `Decision` is an M1 migration decision, not an immediate mutation authorization. `REDEFINE/MOVE/SPLIT/DEPRECATE-CANDIDATE` means “candidate for later authorized UBF mutation,” not “change now.”

## 3. Current-clause inventory

| ID | Source locator | Source authority | Normalized semantic statement | Kind | Scope | Related concept | Conflict | Decision | Evidence / boundary |
|---|---|---|---|---|---|---|---|---|---|
| BP-01 | USER blueprint_protocol §定位 | user-level C-mode canonical | C-mode activates only by explicit request or project BLUEPRINT declaration; cross-machine handshake is delegated to GLOBAL. | Contract | universal candidate | FULL/LITE, coder | — | PRESERVE | Activation and authority split are stable; no UBF Level meaning. |
| BP-02 | USER blueprint_protocol §1 | user-level C-mode canonical | Implementation Blueprint is a closed execution contract, not an explanatory design note. | Contract | universal candidate | coder | — | PRESERVE | Core UBF identity. |
| BP-03 | USER blueprint_protocol §1 | user-level C-mode canonical | Decision closure means behavior-affecting choices have unique answers before CODE executes. | Contract | universal candidate | Level, coder | C-01 | SPLIT | Preserve closure principle; detach future Level measurement from “all choices closed”. |
| BP-04 | USER blueprint_protocol §1 | user-level C-mode canonical | Mechanical implementation forbids CODE from selecting architecture/strategy alternatives. | Contract | universal candidate | coder | — | PRESERVE | Compatible with architecture-authored deterministic execution. |
| BP-05 | USER blueprint_protocol §1/§3 | user-level C-mode canonical | A blueprint gap is a STOP/Q condition, not permission to improvise. | Contract | universal candidate | coder, review | — | PRESERVE | Core safety/discipline contract. |
| BP-06 | USER blueprint_protocol §1 hard principle | user-level C-mode canonical | Unlisted entry points, async tasks, global state, fallbacks, public APIs, dependencies and persistence writes are forbidden; fallback reuses main validation. | Contract | universal candidate | coder, GC | — | PRESERVE | Closure invariant independent of Level numbering. |
| BP-07 | USER blueprint_protocol §1 semantic categories | user-level C-mode canonical | Governance distinguishes Stable Identity, Contract/Semantic, Lifecycle State, Acceptance Snapshot and Generated View. | Contract | universal candidate | review, truth | — | PRESERVE | Required semantic foundation for UBF. |
| BP-08 | USER blueprint_protocol §1 Frozen wording | user-level C-mode canonical | FROZEN/FINAL/IMMUTABLE must name semantic category; snapshot/current value is not permanent Truth. | Contract | universal candidate | review, truth | — | PRESERVE | Prevents broad-freeze defects. |
| BP-09 | USER blueprint_protocol §1 Mutation Declaration | user-level C-mode canonical | Governance batches declare mutability across the five semantic categories. | Contract | universal candidate | review | — | PRESERVE | Mechanical expression of semantic boundaries. |
| BP-10 | USER blueprint_protocol §2 FULL | user-level C-mode canonical | BLUEPRINT-FULL currently packages the complete evidence/design artifact set when routing high-risk triggers hit. | Contract | universal candidate | FULL/LITE, risk | C-03 | SPLIT | Preserve FULL as package profile; selection cannot become Universal Level semantics. |
| BP-11 | USER blueprint_protocol §2 LITE | user-level C-mode canonical | BLUEPRINT-LITE carries four core artifacts and must not weaken constraints. | Contract | universal candidate | FULL/LITE | C-03 | SPLIT | Preserve lightweight carrier; future selector may require more than risk-only trigger. |
| BP-12 | USER blueprint_protocol §2 rule syntax/While | user-level C-mode canonical | Executable rules use ID/Owner/While/When/Input/Do/Must not/Evidence and enumerate mutually exclusive pre-states. | Contract | universal candidate | GC, coder | — | PRESERVE | High-value deterministic expression independent of legacy Level. |
| BP-13 | USER blueprint_protocol §2 scale×granularity | user-level C-mode canonical | Artifact scale and blueprint detail/closure are orthogonal axes. | Contract | universal candidate | FULL/LITE, Level | — | PRESERVE | Matches UBF target orthogonality, while Level semantics will change. |
| BP-14 | USER blueprint_protocol §2.1 coverage formula | user-level C-mode canonical | Current Lk is declared from 100% coverage of cumulative GC sets. | Contract | universal candidate | Level, GC | C-02/C-04 | REDEFINE | Directly conflicts with target: GC validates closure but does not define Universal Level. |
| BP-15 | USER blueprint_protocol §2.1 GC redline | user-level C-mode canonical | Each GC must be an existence proposition, not a subjective degree statement. | Contract | universal candidate | GC | — | PRESERVE | Useful mechanical evidence contract. |
| BP-16 | USER blueprint_protocol §2.1 monotonic sets | user-level C-mode canonical | Legacy level monotonicity is encoded as cumulative GC set inclusion S1⊂…⊂SN. | Contract | historical/legacy mechanism | Level, GC | C-04 | REDEFINE | Future Level monotonicity must be about closure envelopes/coder discretion, not GC membership. |
| BP-17 | USER blueprint_protocol §2.1 L1-L7 themes | user-level C-mode canonical | Legacy L1-L7 theme names are declared as cross-project level semantics. | Contract | historical/legacy mechanism | Level | C-02 | MOVE | Retain as Legacy Level vocabulary/evidence; do not inherit as Universal Level semantics. |
| BP-18 | USER blueprint_protocol §2.1 post-L7 extension | user-level C-mode canonical | Projects may open L8+ and feed theme names back into the user-level table. | Contract | historical/legacy mechanism | Level, promotion | C-05 | DEPRECATE-CANDIDATE | Universal promotion cannot be “new GC expression ⇒ new numeric Level”. |
| BP-19 | USER blueprint_protocol §2.2 declaration | user-level C-mode canonical | Legacy GRANULARITY=Lk is copied into State, Blueprint header and GC checklist. | Contract | historical/legacy mechanism | Level, GC | C-06 | REDEFINE | Keep explicit delegation profile declaration, replace legacy Lk semantics after calibration. |
| BP-20 | USER blueprint_protocol §2.2 baseline floor | user-level C-mode canonical | Project baseline may not be lowered; new projects start from legacy L3 floor. | Contract | historical/legacy mechanism | Level | C-07 | DEPRECATE-CANDIDATE | A fixed legacy numeric floor cannot survive before Universal Level calibration. |
| BP-21 | USER blueprint_protocol §3 | user-level C-mode canonical | CODE implements/tests/evidence only and STOPs with structured Q when behavior is not uniquely determined. | Contract | universal candidate | coder | — | PRESERVE | Core role boundary. |
| BP-22 | USER blueprint_protocol §4 review/block | user-level C-mode canonical | Review outcomes are pass/block/missing-evidence; blockers carry violated ID, location, evidence, exact repair/test and no-scope-expansion. | Contract | universal candidate | review | — | PRESERVE | Independent of Universal Level numbering. |
| BP-23 | USER blueprint_protocol §4 recurrence/expansion/new-level | user-level C-mode canonical | Defect feedback currently increments GC recurrence, expands a legacy level, or opens a new level. | Contract | historical/legacy mechanism | GC, promotion, Level | C-05 | SPLIT | Preserve recurrence/automation feedback; redefine Level promotion as a separate UBF mechanism. |
| BP-24 | USER blueprint_protocol §4 independent challenge | user-level C-mode canonical | Freeze requires an auditable independent challenge ledger. | Contract | universal candidate | review | — | PRESERVE | Keep as challenge mode, not a Level. |
| BP-25 | USER blueprint_protocol §4 canonical challenge | user-level C-mode canonical | Challenge derives coverage from canonical requirements, not Blueprint self-checks alone. | Contract | universal candidate | review, evidence | — | PRESERVE | Evidence independence contract. |
| BP-26 | USER blueprint_protocol §4 Improvement Review | user-level C-mode canonical | Verified defects require multi-dimension blueprint-improvement review and over-design control. | Contract | universal candidate | promotion, review | — | PRESERVE | Keep feedback architecture; decouple legacy Level expansion branch. |
| BP-27 | USER blueprint_protocol §4 Governance Batch Identity | user-level C-mode canonical | Design Baseline, Execution Parent, Initial Delivery and Review Target are distinct; allowlist is judged from Execution Parent. | Contract | universal candidate | review, lifecycle | — | PRESERVE | Critical audit identity. |
| BP-28 | USER blueprint_protocol §4 external canonical evidence | user-level C-mode canonical | User-level canonical mutation records hashes, reviewer access, method, fallback mapping and continuity gate. | Contract | universal candidate | truth, review | C-13 | PRESERVE | This M1 batch uses the same evidence discipline without mutating user-level files. |
| BP-29 | USER blueprint_protocol §4 sibling scan/CV | user-level C-mode canonical | Governance mutation uses sibling scan plus CV-1..CV-4 evidence/requirement cross-validation. | Contract | universal candidate | review, evidence | — | PRESERVE | Cross-dimensional validation remains useful. |
| BP-30 | USER blueprint_protocol §4 REWORK/error attribution | user-level C-mode canonical | REWORK is narrow with Reopen/Preserve sets; every failure is attributed to Execution Error or Blueprint Defect. | Contract | universal candidate | review, promotion | — | PRESERVE | Core learning and repair boundary. |
| BP-31 | USER blueprint_protocol §4 Ownership/Evidence Resolution | user-level C-mode canonical | Impact/path similarity is not ownership; responsibility needs authoritative evidence and unresolved multiple owners block. | Contract | universal candidate | truth, coder | — | PRESERVE | General ownership discipline. |
| BP-32 | USER blueprint_protocol §4 Registry/Stable Entry | user-level C-mode canonical | Discover existing canonical registries before creating new ones; stable entry keeps pointers rather than volatile state copies. | Contract | universal candidate | truth, lifecycle | C-10 | PRESERVE | Supports future State split and avoids second Truth. |
| GL-01 | USER GLOBAL §标准/深度任务协作模式 | user-level global canonical | A/B/C are distinct collaboration modes; C is explicit/project-triggered and never auto-selected from risk alone. | Contract | universal candidate | FULL/LITE, routing | — | PRESERVE | Activation identity remains separate from risk. |
| GL-02 | USER GLOBAL §标准/深度任务协作模式 | user-level global canonical | High risk automatically triggers B; C still requires explicit BLUEPRINT activation and named ARCH/CODE roles. | Contract | universal candidate | routing, review | — | PRESERVE | Prevents risk from becoming collaboration-mode identity. |
| GL-03 | USER GLOBAL C档规模分级 | user-level global canonical | Within C, current FULL/LITE choice is tied to MODEL_ROUTING high-risk triggers. | Contract | universal candidate | FULL/LITE, risk | C-03 | REDEFINE | Keep high-risk ⇒ FULL floor, but future selector may also elevate package for closure/evidence needs. |
| GL-04 | USER GLOBAL C档退出 | user-level global canonical | C batch exits on ACCEPTED or explicit user exit; mode does not silently continue across unrelated tasks. | Lifecycle | universal candidate | lifecycle | — | PRESERVE | Stage/batch lifecycle rule. |
| GL-05 | USER GLOBAL §跨机器蓝图协作 | user-level global canonical | Cross-machine transport uses project Git; task artifacts/state live in project .ai-context, not user-level storage. | Contract | universal candidate | review, evidence | — | PRESERVE | Matches remote read-only architecture workflow. |
| GL-06 | USER GLOBAL §跨机器蓝图协作 | user-level global canonical | BLUEPRINT_STATE is the unique handshake Truth Source. | Contract | universal candidate | lifecycle, truth | — | PRESERVE | Unique handshake ownership is valuable. |
| GL-07 | USER GLOBAL §跨机器蓝图协作 | user-level global canonical | BLUEPRINT_STATE is specified as ≤30 lines with a compact fixed field set. | Contract | universal candidate | lifecycle | C-10 | SPLIT | Preserve compact current handshake; move historical batch evidence out rather than silently tolerating 144-line state. |
| GL-08 | USER GLOBAL §跨机器蓝图协作 | user-level global canonical | TURN enum is currently ARCH/CODE/USER/NONE. | Contract | universal candidate | lifecycle, review | C-08 | REDEFINE | Current accepted UBF workflow uses REVIEW; canonical enum must eventually represent review explicitly or define REVIEW as role/state mapping. |
| GL-09 | USER GLOBAL §跨机器蓝图协作 | user-level global canonical | If TURN is not your role, default action is STOP with no mutation. | Contract | universal candidate | lifecycle, coder | C-09 | SPLIT | Preserve default STOP; add explicit one-task REMOTE_READ_ONLY_ARCH delegated REVIEW→CODE claim exception with state-only commit/push/verify. |
| GL-10 | USER GLOBAL §跨机器蓝图协作 ROLE_CONTRACT | user-level global canonical | Blueprint first screen states role permissions/stops; unknown local model identity receives strict CODE constraints. | Contract | universal candidate | coder | — | PRESERVE | Strong execution safety rule. |
| GL-11 | USER GLOBAL §跨机器蓝图协作 Q flow | user-level global canonical | Unfrozen point causes CODE STOP/Q and handback to ARCH. | Lifecycle | universal candidate | coder, review | — | PRESERVE | Role handback semantics remain. |
| GL-12 | USER GLOBAL §跨机器蓝图协作 delivery/review | user-level global canonical | CODE supplies blueprint→file→test→commit mapping; review is written into the same Blueprint and turns to CODE or ACCEPTED. | Contract | universal candidate | review | C-11 | SPLIT | Preserve traceability; split local writable review from REMOTE_READ_ONLY_ARCH review where ARCH cannot directly mutate reviewed commit. |
| GL-13 | USER GLOBAL §自带验收框架 | user-level global canonical | Explicit acceptance frameworks are mandatory task definition and must be executed item by item before advancing. | Contract | universal candidate | review, evidence | — | PRESERVE | Supports closure evidence. |
| MR-01 | USER MODEL_ROUTING §三档定义 | user-level shared routing canonical | 旗舰/主力/快速 are abstract routing tiers based on task/risk/output authority. | Contract | universal candidate | coder capability, routing | C-14 | PRESERVE | Keep as routing axis; explicitly not Universal Level. |
| MR-02 | USER MODEL_ROUTING §研究支撑角色 | user-level shared routing canonical | Research engineer is an evidence-production role, not a fourth decision tier. | Contract | universal candidate | routing, evidence | C-15 | PRESERVE | Useful role separation; not a closure level. |
| MR-03 | USER MODEL_ROUTING §高风险升级条件 | user-level shared routing canonical | Security, concurrency, public API, data migration, release config, 3+ modules, unresolved alternatives/root cause trigger flagship involvement. | Contract | universal candidate | risk, FULL/LITE | — | PRESERVE | Risk/novelty axis remains separate from Level. |
| MR-04 | USER MODEL_ROUTING §路由 | user-level shared routing canonical | Default main-tier implementation; high risk gets flagship contract/final review; research may reduce uncertainty. | Contract | universal candidate | coder capability, review | C-14 | PRESERVE | Routing chooses actors, not Blueprint closure semantics. |
| MR-05 | USER MODEL_ROUTING §路由 | user-level shared routing canonical | Fast-tier output cannot independently establish business/architecture Truth. | Contract | universal candidate | coder capability | — | PRESERVE | Authority boundary can inform capability profile constraints. |
| MR-06 | USER MODEL_ROUTING §线上事件路由 | user-level shared routing canonical | Incident response routing defines main/research/flagship responsibilities under production risk. | Contract | routing-only adjacent domain | routing | — | MOVE | Keep in MODEL_ROUTING; do not absorb into UBF Level definition. |
| MR-07 | USER MODEL_ROUTING §文档与子智能体 | user-level shared routing canonical | Named document/curation roles map to routing tiers and review requirements. | Contract | routing-only adjacent domain | routing | — | MOVE | Tool/agent routing remains outside Universal Level semantics. |
| PJ-01 | PROJECT fallback12 header | project fallback / GC canonical | Project file declares user blueprint_protocol as higher canonical and itself as fallback/GC carrier. | Identity | project overlay | GC, truth | — | PRESERVE | Authority relationship is explicit. |
| PJ-02 | PROJECT fallback12 BL-01..BL-12 | project historical/overlay canonical | CookBook BL taxonomy records concrete recurring failure patterns and project evidence. | Contract | project overlay | GC, review | — | PRESERVE | Keep project evidence; only abstract reusable semantics may propagate upward. |
| PJ-03 | PROJECT fallback12 §4 A-I package | project fallback / historical contract | Project fallback still states all standard/deep tasks require A-I artifact set. | Contract | project overlay | FULL/LITE | C-12 | REDEFINE | Stale against current user canonical LITE four-artifact contract; later fallback synchronization required, not M1 mutation. |
| PJ-04 | PROJECT fallback12 §12 GC-01..GC-48 | project GC canonical registry | GC-01..GC-48 identities, recurrence counts and project provenance are canonical legacy assets. | Identity | project overlay/historical evidence | GC, Level | — | PRESERVE | Preserve IDs/evidence; per-GC Universal mapping is explicitly M2. |
| PJ-05 | PROJECT fallback12 legacy baseline | project historical contract/snapshot | CookBook current legacy baseline is L7 under the old GC mechanism. | Acceptance Snapshot | historical evidence | Level | C-02/C-04 | MOVE | Retain as Legacy L7 evidence only; never infer Universal Level. |
| PR-01 | PROJECT.md §首读顺序/Truth hierarchy | project stable canonical pointer | Project Graph owns project Truth; accepted plans/blueprints are decision Truth; State is execution extension; Session is handoff context. | Contract | project overlay | truth, review | — | PRESERVE | Project-specific Truth hierarchy must survive UBF migration. |
| PR-02 | PROJECT.md §协作模式 | project stable canonical pointer | CookBook permanently activates BLUEPRINT mode and points to user protocol + project fallback + State. | Contract | project overlay | routing, lifecycle | — | PRESERVE | Project activation overlay, not universal default. |
| PR-03 | PROJECT.md §协作模式 | project stable canonical pointer | Concrete model evidence is owned by 14_模型执行力评估; State keeps abstract roles. | Contract | project overlay | coder capability | — | PRESERVE | Supports capability evidence without role identity leakage. |
| ST-01 | BLUEPRINT_STATE current batch | project lifecycle Truth | At parent 795d2b9, Preview/Start delivery is COMPLETE/PENDING REMOTE ARCH REVIEW with TURN=REVIEW. | Lifecycle | project lifecycle | review | C-17 | REDEFINE | This batch consumes the already-issued ARCH ACCEPT and advances to Work-01; no retroactive Truth rewrite. |
| ML-01 | 14_模型执行力评估 §定位/方法 | project empirical evidence canonical | Concrete model results are evidence for later routing; fewer than three comparable batches cannot establish boundary conclusions. | Contract | project overlay/evidence | coder capability, routing | — | PRESERVE | Compatible with target conditional Capability Profile. |
| M0-01 | M0 Truth Pack/Supplement external source records | acceptance snapshot | M0 preserved user-level hashes/line counts but repository transport intentionally omitted full user-level contents. | Acceptance Snapshot | historical evidence | truth | C-13 | PRESERVE | Keep as M0 snapshot; current direct inputs supersede only for present observation, not history. |
| M0-02 | M0 Truth Pack MODEL_ROUTING record vs current GLOBAL | acceptance snapshot + current canonical | M0 catalogued <USER_HOME>/.ai-context/codex/MODEL_ROUTING.md; current GLOBAL points first to root <USER_HOME>/.ai-context/MODEL_ROUTING.md, which user directly supplied. | Contract | historical evidence / source identity | routing | C-13 | SPLIT | Do not call this proven content drift of one file; record source-identity mismatch. Per-model routing files must be rediscovered before any later routing mutation. |

## 4. Current-state concept map

| Concept | Current owner / current semantics | Target UBF interpretation | M1 disposition |
|---|---|---|---|
| Universal/Legacy Level | User protocol currently defines Lk by cumulative GC coverage and L1-L7 theme order; CookBook carries Legacy L7 | Universal Level must measure how much decision space ARCH has closed vs coder may safely retain | **REDEFINE**; preserve Legacy identities as history only |
| GC | Project fallback owns GC-01..48 and user protocol defines existence-proposition mechanics | GC proves required closure/evidence is actually present; it does not define Level | **PRESERVE registry identity + REDEFINE relationship to Level**; per-GC mapping is M2 |
| FULL/LITE | User protocol/GLOBAL define package size; current trigger is high-risk routing | Package profile expresses/proves needed closure; does not lower closure or become Level | **SPLIT/PRESERVE carrier; REDEFINE selector inputs** |
| Coder role | User protocol/GLOBAL require mechanical execution and Q/STOP on ambiguity; MODEL_ROUTING chooses abstract actor tier | Coder Capability Profile describes conditional remaining-decision capacity; role contract remains strict for delegated work | **PRESERVE role boundary; GAP for capability schema** |
| Review | User protocol requires challenge/evidence; GLOBAL assumes writable same-blueprint review; UBF remote mode uses read-only ARCH + later persistence | Review mode must distinguish direct writable review from remote read-only review while retaining evidence independence | **SPLIT** |
| Promotion / learning | Current GC recurrence can expand GC or open a new legacy numeric level | UBF must separately evolve GC registry, package templates, capability evidence and only freeze Level after empirical calibration | **REDEFINE** |
| Routing tiers | MODEL_ROUTING owns flagship/main/fast + research support and high-risk triggers | Routing is an actor/task-risk axis, not Universal Level | **PRESERVE and keep orthogonal** |
| TURN/handshake | GLOBAL owns generic handshake; Project State is current Truth; accepted UBF uses REVIEW and delegated remote claim | Default STOP remains; explicit audited one-task delegation is required for remote read-only ARCH | **SPLIT/REDEFINE canonical contract later** |

## 5. Contradiction / ambiguity matrix

| ID | Current facts | Target/observed fact | Classification | M1 disposition | Blocks this Work-01? |
|---|---|---|---|---|---|
| C-01 | “decision closure” can read as every behavior choice uniquely fixed | UBF first principle intentionally retains coder-safe decision space at lower closure levels | semantic overload | SPLIT general closure safety from Level-specific delegated discretion | NO |
| C-02 | user protocol presents historical L1-L7 themes as cross-project universal names | Control forbids deriving Universal Level from historical topic order | direct semantic conflict | MOVE L1-L7 to Legacy vocabulary; future Universal semantics remain unresolved | NO |
| C-03 | FULL/LITE choice is currently risk-triggered | target package profile is orthogonal to Level and may need task/closure/evidence inputs | selector incompleteness | preserve risk floor; REDEFINE selector later | NO |
| C-04 | current Lk is mathematically GC-coverage based | Control explicitly says GC does not define Universal Level | direct semantic conflict | REDEFINE Level; preserve GC as evidence mechanism | NO |
| C-05 | new expression form can create new numeric level | target requires empirical closure-envelope calibration before freezing levels | promotion conflict | SPLIT GC evolution from Level evolution | NO |
| C-06 | GRANULARITY=Lk is copied into State/header/checklist | future Lk meaning will change | migration coupling | REDEFINE declaration semantics only after level calibration | NO |
| C-07 | fixed project baseline L7 / new-project L3 floor | future Universal ladder/count not yet known | premature numeric policy | DEPRECATE-CANDIDATE | NO |
| C-08 | GLOBAL TURN enum omits REVIEW | accepted CookBook/UBF workflow uses TURN=REVIEW as real handshake state | canonical vs accepted runtime-governance conflict | REDEFINE enum/role mapping in later canonical mutation | NO; batch-level contract is explicit |
| C-09 | GLOBAL says TURN not yours ⇒ STOP | remote read-only ARCH cannot mutate; accepted R2 transaction used explicit delegated REVIEW→CODE state-only claim | missing authorized exception | SPLIT default STOP + explicit delegated single-task claim contract | NO; this Work-01 embeds explicit user/ARCH authorization |
| C-10 | GLOBAL says BLUEPRINT_STATE ≤30 lines | current repository State at parent is 144 lines and carries history | ownership/scale conflict | SPLIT compact handshake from historical evidence in later mutation; do not silently rewrite now | NO |
| C-11 | GLOBAL assumes review result written into same blueprint and review can mutate TURN | REMOTE_READ_ONLY_ARCH review is intentionally read-only and persists verdict in a later authorized write batch | review-mode conflict | SPLIT direct writable vs remote read-only review protocols | NO |
| C-12 | project fallback §4 says standard/deep blueprint package always A-I | higher user canonical currently defines LITE as four core artifacts | fallback semantic drift | later synchronize fallback; user canonical wins now | NO |
| C-13 | M0 catalogued `codex/MODEL_ROUTING.md`; current GLOBAL points first to root shared `MODEL_ROUTING.md` | user supplied root shared file; hashes are different objects unless path identity is proven | source identity / collection gap | record both; rediscover all routing siblings before any mutation | NO for shared UBF semantics |
| C-14 | routing tiers can superficially resemble capability/Level tiers | Control requires Universal Level and Coder Capability Profile to be distinct axes | naming/axis ambiguity | explicitly preserve routing tiers as orthogonal | NO |
| C-15 | research engineer sits “between” main and flagship | file explicitly says it is not a fourth decision tier | possible false hierarchy | PRESERVE non-tier evidence role | NO |
| C-17 | parent State/Preview still say Preview pending remote review | ARCH has already accepted `795d2b9...` in this controlling session | lifecycle stale value | this batch persists ACCEPT/consumption and moves to Work-01 | NO |

## 6. Gap matrix

| Gap | Missing current construct | Required later stage | Current treatment |
|---|---|---|---|
| G-01 | empirically calibrated Universal Level semantics/count | M3/M4 | keep UNRESOLVED; do not invent |
| G-02 | Task Profile schema | M4 | identify axis only |
| G-03 | conditional Coder Capability Profile schema | M4 with M3 evidence | preserve model ledger as evidence source |
| G-04 | Level Selector algorithm | M4 | no selector frozen in M1 |
| G-05 | GC metadata for Closure Effect / Preserved Coder Discretion / applicable profile | M2 | per-GC mapping deferred |
| G-06 | empirical corpus covering success/REWORK/Q/blueprint defect and hidden help | M3 | no capability conclusion in M1 |
| G-07 | canonical remote-read-only delegated TURN claim rule | M6 canonical mutation candidate | batch-level explicit authorization only |
| G-08 | canonical compact State/history ownership split | M5/M6 design + mutation | record conflict; do not rewrite history in M1 |
| G-09 | complete discovery of root + per-model routing sibling files before routing mutation | M6 pre-mutation discovery | source-identity continuity gate |

## 7. Preserve matrix — mandatory semantics that survive UBF redesign

| Preserve ID | Semantic | Why it survives | Owner after migration (candidate) |
|---|---|---|---|
| P-01 | Blueprint is a closed delegation contract, not prose guidance | first principle | user-level protocol |
| P-02 | scope/allowlist/unauthorized mutation prohibition | prevents coder discretion from escaping delegation | user-level protocol + project overlay |
| P-03 | five semantic categories + precise freeze wording | prevents Truth/lifecycle conflation | user-level protocol |
| P-04 | explicit Mutation Declaration | mechanical self-application | user-level protocol/GC |
| P-05 | FULL/LITE constraints do not weaken closure | scale/package orthogonality | package profile |
| P-06 | GC existence-proposition discipline | keeps evidence mechanically checkable | GC mechanism |
| P-07 | CODE Q/STOP on unresolved behavior | prevents hidden architecture invention | role contract |
| P-08 | independent canonical-requirement challenge | prevents Blueprint self-certification | challenge mode |
| P-09 | batch identity + evidence landing + external hash continuity | auditability | review/evidence contract |
| P-10 | REWORK Reopen/Preserve + error attribution + improvement feedback | bounded repair and learning | review/promotion system |
| P-11 | Project Graph truth hierarchy and CookBook BLUEPRINT activation | project-specific governance | CookBook overlay |
| P-12 | GC-01..48 IDs, recurrence/provenance and BL-01..12 cases | migration evidence must not be erased | CookBook legacy registry |
| P-13 | concrete model evidence is separate from abstract roles/routing | avoids identity leakage and n=1 conclusions | project evidence ledger + routing |
| P-14 | default TURN mismatch STOP | safe baseline handshake | GLOBAL; with explicit delegated exception |

## 8. CookBook-specific overlay boundary

The following are **project overlay / historical evidence**, not candidates for direct inclusion in Universal Level semantics:

- Project Graph as CookBook Project Truth and its lifecycle history.
- CookBook permanent `协作模式: BLUEPRINT` declaration.
- BL-01..BL-12 concrete failure taxonomy and Cookbook-specific examples.
- GC-01..GC-48 identities, recurrence counters, provenance, current Legacy L7 assignment.
- project-specific validation commands, Kotlin/KMP/UI/state examples and Phase 3 lifecycle records.
- concrete model execution rows in `14_模型执行力评估.md`.
- Phase 3A/3B pause/authorization facts.

Reusable abstractions already promoted into the user-level protocol remain universal candidates; the concrete CookBook incidents that motivated them remain project evidence.

## 9. Explicitly excluded-by-scope user GLOBAL areas

- Android/Maven technical guidance.
- project map/navigation conventions not specific to Blueprint semantics.
- AI comments, AUTO_DEV, long-task recovery mechanics except where already represented by evidence contracts.
- shared runtime/plugin/config synchronization details.
- session-memory convenience rules.

Exclusion means “not needed to answer M1 UBF semantics,” not deprecation.

## 10. M1 completion test for this work

Work-01 is architecture-complete when all of the following persist exactly:

- UBF-relevant current clause inventory with stable IDs and five-kind classification;
- Level/GC/FULL-LITE/coder/review/promotion current-state map;
- contradiction/ambiguity matrix with no silent authority resolution;
- gap matrix;
- preserve matrix;
- CookBook overlay boundary;
- explicit deferral of per-GC metadata/mapping to M2;
- explicit non-conclusions below.

## 11. Frozen non-conclusions

This Work-01 does **not** decide:

- Universal Level count or final names;
- any Legacy L1-L7 → Universal Level mapping;
- any GC-01..48 → Universal Level mapping;
- Task Profile fields, Capability Profile scoring, or Level Selector algorithm;
- a concrete-model routing upgrade/downgrade from Luna samples;
- a user-level canonical mutation;
- a project fallback synchronization patch;
- a BLUEPRINT_STATE compaction migration;
- Project Graph/Phase 3A repair;
- CookBook Phase 3B start.

## 12. Next stage if this delivery is remotely ACCEPTED

No M2 work starts directly. The next authorized governance batch is **M1 End/Accept + M1→M2 Handoff persistence**. Only after that handoff is remotely accepted may M2 Legacy Asset Mapping begin.
