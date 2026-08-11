# 2026-08-11 · Project Graph 阶段推进：Phase 2B 收口 → Phase 2C 实施 → Phase 2C END → 2D 交接

> 会话快照（context_memory）。主交接入口见 `SESSION_交接.md`。本文件记录本次会话的完整动作与决策，供反查。

## 一、执行了什么（按蓝图顺序，三个 Commit）

按 `.ai-context/docs/项目改造规划/` 下三份蓝图逐批执行，每批独立 Commit + push + 门禁：

| Commit | 蓝图 | 内容 |
|---|---|---|
| `e3389fba` | `第二阶段-2B-End-2C-Preview.md` PART A | **Phase 2B END**：重算 2B 派生统计（104/51/53）、修正 `PHASE2B_INVENTORY.md`（旧 101/39+62 标 SUPERSEDED）、新建 `PHASE2B_ACCEPT.md` + `PHASE2B_TO_2C_HANDOFF.md`（P2C-H01~10）、README/project.yaml 阶段状态更新。 |
| `ced5f13f` | `第二阶段-2B-End-2C-Preview.md` PART B | **Phase 2C PREVIEW/START**：Plan 迁移（PLAN-AI-NDJSON→completed；新建 PLAN-K1I/K1A/L1 均 completed）、Relations（K15↔I7、J22↔L2 related_to；L3 affects 6 Feature；FEAT-AI-MEAL-001 affects F-DISH/F-MEAL）、FEAT-AI-MEAL-001（backlog）/FEAT-RECOMMEND-001（parked）落位、冻结 Plan lifecycle / Observed vs Verification / BLUEPRINT_STATE Extension 边界 / FEAT-* 约定，新建 `PHASE2C_PLAN_INVENTORY / DECISIONS / CONFLICTS.md`。 |
| `12984df3` | `第二阶段-2C-End-2D-Preview.md` | **Phase 2C END → 2D HANDOFF**：4 处 Governance Cleanup（README Plan 示例抽象化、project.yaml Phase 2A 过时指令、README 底部"Phase 2 未开始"、Plan status 链式→状态集合）、新建 `PHASE2C_ACCEPT.md` + `PHASE2C_TO_2D_HANDOFF.md`（冻结 Stable Verification ID 不可变、统计执行时重算、VERIFY_UNMAPPED no-guess、build/test=Observed、required 默认 true、Closure 契约、PoC Verification 先 reconcile）、`PHASE2C_CONFLICTS.md` 交接注记。 |

## 二、关键统计与决策

- **Phase 2B 派生统计（From Graph）**：Total WorkItems = 104（Stable 51 + Generated 53）；By Feature/Kind/Status 明细见 `PHASE2B_ACCEPT.md`。Generated 口径 = ID 以 `BUG-/TODO-/TECH-/REFACTOR-/COMP-/RESEARCH-/MAINT-` 前缀开头（§7 冻结），FEAT-* 属 2C 不计入。
- **Phase 2C 统计**：WorkItems 106、Plans 4（全 completed）、Verifications 3、Relations 10（affects 8 / related_to 2）。
- **4 个 Plan 全 completed，但 K1g/K1i/K1a/L1 全保持 verifying**（真机验证 pending）——正是「Plan completed ≠ WorkItem done」的落地实例。
- **K1i 独立 PLAN-K1I**（未复用 PLAN-AI-NDJSON）；**K15↔I7 / J22↔L2 用 related_to**（不 supersedes，无 replacement evidence）；**L3 Primary=F-TOOLS + affects 6 Feature**，FEATURE_SPLIT_CANDIDATE 留 Phase 2E。
- **FEAT-\<FEATURE\>-NNN 约定冻结**：FEAT-AI-MEAL-001（AI 对话生成菜品/餐食，backlog）、FEAT-RECOMMEND-001（放开 AI 推荐限制，parked 用户暂缓）。

## 三、当前仓库状态

```text
Phase 1  — Model Contract      : FINAL ACCEPT / FROZEN   （83623a3）
Phase 2A — Feature Universe    : ACCEPT / CLOSED          （b54246c1）
Phase 2B — Current WorkItem    : ACCEPT / CLOSED          （e2127176）
Phase 2C — Plan+Relation+Deferred : ACCEPT / CLOSED       （ced5f13f）
Phase 2D — Verification Bootstrap : AUTHORIZED / NOT STARTED
Graph Mode                     : draft
```

Graph 数据：features=13 · work_items=106 · plans=4 · verifications=3 · relations=10。全部 Commit 已 push，本地与 origin/master 同步。

## 四、⏭ 下一步（Phase 2D，尚未开始）

- 必须等待外部架构审核 `12984df3`（Phase 2C END 交接 Commit）。
- 审核通过后执行 Phase 2D Verification Bootstrap（蓝图 `第二阶段-2D-*.md` 尚未生成）。**强制入口**：`.ai-context/project_graph/migration/PHASE2C_TO_2D_HANDOFF.md`。
- Phase 2D 边界（Handoff 已冻结）：最新唯一真机验证清单 → Stable Verification entities；Verification→WorkItem mapping；status reconciliation；required closure。禁止：CurrentWork reconcile（2E）、L3 split（2E）、Observed Store（4）、Graph active（3）。
- **真机验证仍被用户确认暂时无法进行**（2026-08-08 起）——Phase 2D 迁移的是「清单→实体」，不要求真机实跑。

## 五、会话规则/经验要点

- 蓝图执行模式：先建立基线（git status/HEAD/log）→ 读必读清单 → 按蓝图逐节落地 → 跑 Project Graph 测试 + `pg check` → Commit（每批独立）→ push → STOP 等外部审核。禁止连续自动执行下一 Phase。
- **派生统计直接写临时脚本复用 `project_graph.py` loader 计算，不人工推算**（临时脚本放 `temp/claude/`，不提交）。
- **Contract README 教学示例用抽象 ID（PLAN-X/WORK-X），别用真实业务 ID**——真实实例会随阶段推进变成错误示例（`PLAN-AI-NDJSON work_items:[K1g,K1i]` 就曾是旧 PoC 示例，与 2C 冻结的 K1i 独立 Plan 冲突，已清理）。
- 本轮边界全部守住：Schema / Validator / 生产代码 / CurrentWork / Feature YAML 0 改动；mode=draft；61 tests PASS；pg check PASS / 0 issue。
