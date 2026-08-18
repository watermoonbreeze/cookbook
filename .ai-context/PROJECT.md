# Cookbook 项目上下文入口

> 用途：Claude、Codex、DeepSeek 或其他模型首次接手本仓库时的最小必读导航。
> 定位：CookBook 项目的稳定导航入口。短生命周期事实由各 canonical source 持有，本文件不维护其副本。

## 项目事实

- 产品：面向慢性病家庭的饮食规划 App，核心链路为记录餐食、食历复用、饮食推荐和健康提示。
- 技术：Kotlin Multiplatform；`shared` 承载 Domain/Data/SQLDelight，`androidApp` 承载 Compose UI；当前只交付 Android。
- 文档规则：项目通用规范、状态、方案、经验和交接只写本 `.ai-context/`；`.claude/`、`.codex/` 只保留工具专属配置。

## 首读顺序与真相优先级

1. 项目入口规则：仓库根 `AGENTS.md`（Codex）或 `CLAUDE.md`（Claude）。
2. 项目视图与历史资料：`docs/projectReview/00_导读与索引.md`，再按其阅读路径下钻；Project Truth 仍以 Project Graph 为准。
3. Project Truth 入口：`.ai-context/project_graph/README.md`、`.ai-context/project_graph/project.yaml`；Feature、WorkItem、Plan、Verification、Relation、CurrentWork 以 Project Graph 为准。
4. 当前进行中状态：`docs/context_memory/SESSION_交接.md`；它是 Handoff Context，不覆盖 Project Graph 或已接受决策。
5. 代码定位：`docs/功能路径索引.md`。
6. 任务范围：`docs/feature/待办索引.md` 与相应专项文档；工程与踩坑：`docs/experience/INDEX.md`。
7. 具体功能按需读 `docs/feature/`；架构、流程、数据、AI 和诊断按需读 `docs/projectReview/`。

Phase 2 Frozen Truth Hierarchy：Runtime Truth（Code / DB / schema / runtime config）> Project Truth（Project Graph）> Decision Truth（Accepted Plan / ADR / Formal Blueprint）> Execution Extension（BLUEPRINT_STATE）> Handoff Context（SESSION）。任何“待实现”不等于已经存在于代码。

## 协作模式

- **协作模式: BLUEPRINT**（常驻声明，跨机器协作时无需每次口令触发）。ARCH / CODE / REVIEW / TURN 以 `docs/context_memory/BLUEPRINT_STATE.md` 当前值为准。
- 规则正文：用户级真相源 `~/.ai-context/rules/blueprint_protocol.md`；CookBook canonical GC / fallback：`docs/experience/12_多模型协作与实施蓝图规范.md`。
- 握手状态唯一文件：`docs/context_memory/BLUEPRINT_STATE.md`。开工前先 `git pull` 读取；`TURN` 不是自己则停手、只报告持球方。
- 具体模型执行记录唯一事实源：`docs/experience/14_模型执行力评估.md`。

## 稳定导航指针

- 当前批次 / TURN / 当前执行状态：唯一读取 `docs/context_memory/BLUEPRINT_STATE.md`。
- 任何编码模型实施前必须读 `docs/experience/12_多模型协作与实施蓝图规范.md`（蓝图协议 + GC 条款）。
- 规则与反查：`docs/projectReview/21_AI与网络请求策略（专属）.md`、`08_决策记录.md` D-15/D-16、`05_诊断地图.md`。
- 真机验证只认 `docs/真机验证/真机待验证清单_<yyyyMMddHHmm>.md` 中时间最新的一份。

## 文档分层

| 位置 | 内容与使用方式 |
|---|---|
| `docs/projectReview/` | 架构、流程、UI、数据、决策、诊断等项目视图与历史资料；Project Truth 仍以 Project Graph 为准。 |
| `docs/context_memory/SESSION_交接.md` | 唯一当前会话接续入口；其他日期文件均为历史快照。 |
| `docs/feature/` | 当前功能方案、待办、验收与唯一真机清单。 |
| `docs/experience/` | 可复用工程经验与踩坑；由 `INDEX.md` 导航。 |
| `docs/feature/_archive/` | 历史资料，只用于追溯，不能覆盖当前方案或状态。 |

## 代码近旁资料

- `data-pipeline/` 的 README、脚本说明、候选与映射 review 是数据生产工具的**代码近旁资料**，不作为项目通用首读入口；涉及预设/营养数据生产时，从 `docs/projectReview/22_预设与参考资料治理（专属）.md` 进入，再按需读取。
- 根目录 `docs/` 的历史需求/规划已迁入 `docs/feature/_archive/legacy_root_docs/`；不得再创建新的根 `docs/` 项目知识副本。
