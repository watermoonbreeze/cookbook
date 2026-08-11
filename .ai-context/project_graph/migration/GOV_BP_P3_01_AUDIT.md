# GOV-BP-P3-01 R1 Audit

> 角色：CODE 交付审计；状态：EXECUTED / PENDING INDEPENDENT ARCH REVIEW；本文件是持久化证据，不是治理 registry、state ledger 或 Project Truth。

## A. Baseline Identity

| 项目 | 值 |
|---|---|
| Design Baseline | `21e54015ec5ce0fb02d0f47911a6442400a8c44b` |
| Interposed Phase 3A R1 | `e0ae8bc3f925ae6974c41f2aa9d844e2c95219ff` |
| Initial Governance Delivery | `586652388cde269b614728d8160e7963bd88452c` |
| R1 Execution Parent | `586652388cde269b614728d8160e7963bd88452c` |
| Review Target | 本批最终 commit（push 后由 ARCH 读取 exact hash） |
| Comparison base | allowlist diff 相对 R1 Execution Parent；不以 Design Baseline 判定越界 |

## B. Global → Project Semantic Parity Matrix

| Canonical requirement | Global location | Project location | Result |
|---|---|---|---|
| Baseline Identity | `blueprint_protocol.md` Governance Batch Identity | `12_...规范.md` §14.7；本文件 A；STATE 基线行 | PASS |
| Persisted Evidence Landing | `blueprint_protocol.md` Governance Batch Identity | 本文件 C/D/E/F | PASS |
| External Canonical Evidence | `blueprint_protocol.md` External canonical clause | `12_...规范.md` §14.7；本文件 E | PASS |
| Blueprint Escape | protocol §4 + §14.6 | `12_...规范.md` §14.6；本蓝图 §3 | PASS |
| Granularity | protocol §2.1/§2.2 | `12_...规范.md` §12/GC registry；STATE L7 | PASS |
| Scale | protocol Improvement Review | `12_...规范.md` §14.6；本蓝图 §5 | PASS |
| CV-1~CV-4 | protocol Cross-Validation Contract | `12_...规范.md` §14.6；本文件 C/D | PASS |
| Propagation | protocol Improvement Review / Sibling Scan | `12_...规范.md` §14.8；本文件 F | PASS |
| Over-design | protocol Architecture Action | `12_...规范.md` §14.6；本蓝图 §19 | PASS |
| Self-Application | protocol Canonical Sibling Entry Scan | `12_...规范.md` §14.8；本文件 F | PASS |

## C. Requirement → Evidence

| Requirement | Evidence source | Reproduction / inspection | Landing | Result |
|---|---|---|---|---|
| R1-01~02 baseline split | A + STATE | `git rev-parse HEAD^` and A | A | PASS |
| R1-03 GC-02 | 12 registry + §14.7 | inspect allowlist/denylist and batch base | B/F | PASS |
| R1-04 GC-06 | protocol + §14.7 | inspect command/evidence landing contract | B/C | PASS |
| R1-05 GC-37 / §6 | registry + §14.6 | B row and canonical coverage scan | B | PASS |
| R1-06 GC-47 / §10 / §14.6 | registry + §14.6 | inspect fixed review fields | B/F | PASS |
| R1-07~08 GC-48 recurrence and sibling scan | registry + §14.8 | F six-column scan | F | PASS |
| R1-09 parity | B | read all 10 rows | B | PASS |
| R1-10~11 bidirectional matrices | C/D | inspect each row and source mapping | C/D | PASS |
| R1-12~14 external evidence | E | SHA/method/access/continuity fields | E | PASS |
| R1-15~16 history and model ledger | 12 history + 14 ledger | inspect dated history and initial commit | F / 14 | PASS |
| R1-17~19 GC and level | 12 registry | recount IDs, search GC-49/L8 | G | PASS |
| R1-20~21 graph/Phase 3A preserve | `git diff` | denylist diff audit | G | PASS |
| R1-22 TURN | STATE | inspect current batch row | STATE | PASS |

## D. Evidence → Requirement

| Evidence | Mapped requirements | Source / command | Result |
|---|---|---|---|
| A baseline table | R1-01, R1-02, R1-16 | `git rev-parse HEAD^`; `git show --format=` | PASS |
| B parity matrix | R1-05, R1-06, R1-09 | global/project marker scan | PASS |
| C/D bidirectional tables | R1-10, R1-11 | table review against B | PASS |
| E external table | R1-12, R1-13, R1-14 | `Get-FileHash` before/after; method/access fields | PASS |
| F self-application table | R1-07, R1-08, R1-15 | sibling keyword/entry/gate/template/lifecycle scan | PASS |
| G command results | R1-17~21 | commands recorded below | PASS |

## E. External Canonical Evidence

| Field | Value |
|---|---|
| Path | `~/.ai-context/rules/blueprint_protocol.md` |
| Truth Role | Global Blueprint Truth; project file is fallback, not co-owner |
| Execution Machine | `当前机` |
| Reviewer Direct Access | `NO`（审计工件可提交；ARCH 需在可访问该用户目录的机器重算） |
| SHA-256 Before | `C4F3A116265DE97B105CE988AA65B50957C80FA2661B4811EE752F53D46537F5` |
| SHA-256 After | `C2C8332EB12D545CA89FCA4C80A15DBA7E2ACF5FAF7703A8CFE6815A0B5F0EB3` |
| Mutation Method | `apply_patch`；变更前后 `Get-FileHash -Algorithm SHA256` |
| Changed Semantic Clauses | 增加 Governance Batch Identity and Evidence Landing；增加 Canonical Sibling Entry Scan |
| Project Fallback Mapping | `12_...规范.md` §14.7/§14.8；GC-02/GC-06/GC-48 保持项目语义一致 |
| Local Parity Command | `rg -n "Baseline Identity|Persisted Evidence|External Canonical|Canonical Sibling|CV-1|Propagation|Over-design" <global> <project-12>` |
| Persisted Evidence Landing | 本文件 E；STATE 只保留摘要与指针 |
| Next-Batch Continuity Gate | 下一批重算该路径 SHA-256，必须等于本行 SHA-after；否则 STOP，并标记 `REMOTE_ATTESTED_EXTERNAL_STATE` |

## F. GC-48 Six-Column Self-Application

| New / Modified Rule or Record | Affected Governance Files | Self-Check Target | Compliance Result | Violation | Disposition |
|---|---|---|---|---|---|
| Batch identity / evidence landing | global protocol; 12; STATE; current audit | baseline, parent, review target, evidence landing | PASS | none | keep split truth roles |
| External canonical mutation contract | global protocol; 12; current audit | SHA, access label, continuity gate | PASS | none | remote-attested boundary recorded |
| Canonical sibling scan | global protocol; 12; current audit | keyword, entry, gate, template, lifecycle siblings | PASS | none | no stale parallel clause found |
| GC-02 registry | 12; current Blueprint | allowlist/denylist/base | PASS | none | strengthen existing GC-02; no GC-49 |
| GC-06 evidence | 12; current Blueprint | command + persisted landing | PASS | none | strengthen existing GC-06 |
| GC-37 challenge | 12; current Blueprint | canonical requirement coverage | PASS | none | preserve and propagate |
| GC-47 review loop | 12; current Blueprint | attribution through evidence | PASS | none | preserve and propagate |
| GC-48 self-application | 12; current Blueprint; future Phase 3B~3J | six-column coverage and denylist | PASS | none | future propagation only; no denylist mutation |
| Stable project entry / state | PROJECT.md; BLUEPRINT_STATE; 14 | pointer ownership and no duplicate model facts | PASS | none | preserve existing owners |

## G. Programmatic Checks and Boundary

Commands executed for this batch:

```text
git rev-parse HEAD                  -> 586652388cde269b614728d8160e7963bd88452c
git rev-parse HEAD^                 -> e0ae8bc3f925ae6974c41f2aa9d844e2c95219ff
python -m unittest test_validator -v  -> 61 tests, 61 passed, 0 failed, 0 errors
python .ai-context/project_graph/tools/project_graph.py check -> PG OK; mode=draft; counts 13/109/4/98/10
GC registry recount                -> 48 unique; missing=0; duplicate=0; GC-49 absent; L7; L8 absent
denylist diff vs 5866523           -> 0 (Phase3A / Graph / PROJECT / SESSION / legacy / production)
```

This audit records CODE facts only. It does not grant ARCH acceptance and does not authorize Phase 3B.
