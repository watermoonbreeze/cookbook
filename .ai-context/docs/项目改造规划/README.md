# 项目改造规划 · 现状速览（2026-08-17）

> 本目录只剩 3 样东西：本文件（唯一入口）、`改造总览.md`（Project Graph 原始设计，仍有效）、`UBF退休与模型执行力评估重设计.md`（当前治理现状）。其余全部历史迭代文档与 UBF 治理支线的原始产出（100+ 文件）已搬进 `_archive/` 与 `../_archive/UBF_20260817/`，不删除、可反查，只是不再占默认阅读路径的 token。

## 这个目录管两件事

1. **Project Graph**——用结构化 Feature/WorkItem/Plan/Verification 取代人工维护的"项目现状"文档，见 `改造总览.md`（设计）+ `.ai-context/project_graph/README.md`（实际进度）。
2. **蓝图治理协议**——ARCH 怎么给 CODE 出蓝图、复核出问题该怎么处置，见用户级 `~/.ai-context/rules/blueprint_protocol.md`（核心协议）+ 项目内 `docs/experience/12_多模型协作与实施蓝图规范.md`（GC 登记表）+ `docs/experience/14_模型执行力评估.md`（模型能力画像）。

这两件事互相独立，别混在一起读。

## 一句话现状

| 项 | 状态 |
|---|---|
| Project Graph Phase 1（核心实体/Schema） | **FINAL ACCEPT / FROZEN** |
| Project Graph Phase 2（Bootstrap，104 条数据迁移） | **FINAL ACCEPT / FROZEN** |
| Project Graph Phase 3（生成视图 + 激活） | **AUTHORIZED / NOT STARTED**，卡在 Phase 3A 的 `GOV-BP-P3-01` 治理升级审计，待 ARCH 裁决（见下一步） |
| 蓝图治理协议 | **当前生效版**：`BP`/`EXEC`/`NON-BP` 前置归因层（2026-08-17 定稿）；ARCH 角色已从远程 ChatGPT 网页版转移到本机 |
| UBF（Universal Blueprint Framework）支线 | **已正式退休**（2026-08-11~08-15，142 个提交，0 行产品代码，0 条 Universal Level 结论；核心问题：其"密码学承诺-揭示"协议无脚本支撑，是未经验证的文本断言） |

## 下一步（唯一真相源：`BLUEPRINT_STATE.md` 当前批次）

1. 裁决 `GOV-BP-P3-01`（ACCEPT/REWORK），解锁 Project Graph Phase 3
2. 清真机验证积压（L1 的 `E-L1-01~12`、K1i 的 `E-K1I-01/02`，以及更早 AI 记一餐 ~30 项）
3. 新真实 CODE 批次（当前模型 DeepSeek V4 Flash）按 `UBF退休与模型执行力评估重设计.md` §3~§4.5 的循环执行，每批复核强制走归因四步

## 要深入读，去这几个文件（不要读 `_archive/`）

| 想知道什么 | 读哪个 |
|---|---|
| Project Graph 的实体模型、Phase 1~6 总体规划 | `改造总览.md` |
| Project Graph 实际做到哪、Schema、Validator 用法 | `.ai-context/project_graph/README.md` |
| UBF 为什么被退休、证据、新治理循环怎么设计的 | `UBF退休与模型执行力评估重设计.md` |
| 蓝图分级/GC 条款是什么、归因判据 AT-01~AT-10 | `docs/experience/12_多模型协作与实施蓝图规范.md` §12~§14 |
| 具体哪个模型在哪类任务上表现如何、当前生效的加严项 | `docs/experience/14_模型执行力评估.md`「模型能力画像」表 |
| 机械核对工具怎么用 | `.ai-context/tools/blueprint_check.py --help` |

## `_archive/` 里是什么（仅当需要考古时才打开）

- `_archive/`（本目录下）：Project Graph Phase 1/2 的迭代设计与 Rework 文档（`第一阶段*.md`、`第二阶段*.md`、`Phase-2D-R1.md`）+ 早期的 `Universal-Blueprint-Framework-Architecture-Review.md`（UBF 启动前的理论评估，讽刺地准确预言了后来发生的膨胀）。最终状态已由 `.ai-context/project_graph/migration/PHASE*_ACCEPT.md` 承接，不需要再读迭代过程。
- `../_archive/UBF_20260817/`：UBF 支线的全部原始产出（`blueprints/` 73 个逐批执行蓝图、`root/` 40+ 个 Execution-Report、`experience_json/` 40+ 个"承诺哈希"JSON、`execution_results/` 2 个远程执行结果）。这些文件不影响任何当前判断，纯粹是治理失控过程的原始证据。
- `docs/context_memory/_archive/BLUEPRINT_STATE_UBF历史_20260817.md`：`BLUEPRINT_STATE.md` 里搬出的 50+ 个 UBF 批次握手记录。
- `docs/experience/_archive/14_模型执行力评估_UBF历史_20260817.md`：评估台账里搬出的 Phase 3A/GOV-BP-P3-01/UBF 治理行。
