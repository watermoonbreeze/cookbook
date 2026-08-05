# Cookbook 项目上下文入口

> 用途：Claude、Codex、DeepSeek 或其他模型首次接手本仓库时的最小必读导航。
> 状态：2026-08-05；当前事实以本文件列出的优先级为准。

## 项目事实

- 产品：面向慢性病家庭的饮食规划 App，核心链路为记录餐食、食历复用、饮食推荐和健康提示。
- 技术：Kotlin Multiplatform；`shared` 承载 Domain/Data/SQLDelight，`androidApp` 承载 Compose UI；当前只交付 Android。
- 阶段：MVP 核心已完成，处于功能扩展与打磨；AI 记一餐 V2 已验证，周期记 + NDJSON 流式改造已有架构规范，待按批实施。
- 文档规则：项目通用规范、状态、方案、经验和交接只写本 `.ai-context/`；`.claude/`、`.codex/` 只保留工具专属配置。

## 首读顺序与真相优先级

1. 项目入口规则：仓库根 `AGENTS.md`（Codex）或 `CLAUDE.md`（Claude）。
2. 全局全貌：`docs/projectReview/00_导读与索引.md`，再按其阅读路径下钻。
3. 当前进行中状态：`docs/context_memory/SESSION_交接.md`；它优先于任何历史快照、旧交接或待办摘要。
4. 代码定位：`docs/功能路径索引.md`。
5. 任务范围：`docs/feature/待办索引.md` 与相应专项文档；工程与踩坑：`docs/experience/INDEX.md`。
6. 具体功能按需读 `docs/feature/`；架构、流程、数据、AI 和诊断按需读 `docs/projectReview/`。

冲突时的优先级：**当前代码与数据库 schema > 当前 `SESSION_交接.md` > 项目地图/ADR > 专项方案与待办 > experience > `_archive` 历史资料**。任何“待实现”不等于已经存在于代码。

## 当前关键任务

- AI 记一餐的实施唯一基线：`docs/feature/AI记一餐_周期记_NDJSON流式开发规范.md`。
- 对应产品方案：`docs/feature/AI记一餐_周期记_NDJSON流式改造落地方案.md`。
- B3 会话唯一实施蓝图：`docs/feature/AI记一餐_周期记_NDJSON流式_B3会话实施蓝图.md`；任何编码模型实施前还必须读 `docs/experience/12_多模型协作与实施蓝图规范.md`。
- 规则与反查：`docs/projectReview/21_AI与网络请求策略（专属）.md`、`08_决策记录.md` D-15/D-16、`05_诊断地图.md`。
- 真机验证只认 `docs/feature/真机待验证清单_<yyyyMMddHHmm>.md` 中时间最新的一份；当前为 `真机待验证清单_202608051156.md`。

## 文档分层

| 位置 | 内容与使用方式 |
|---|---|
| `docs/projectReview/` | 当前项目地图、架构/流程/UI/数据/决策/诊断；全局任务首读。 |
| `docs/context_memory/SESSION_交接.md` | 唯一当前会话接续入口；其他日期文件均为历史快照。 |
| `docs/feature/` | 当前功能方案、待办、验收与唯一真机清单。 |
| `docs/experience/` | 可复用工程经验与踩坑；由 `INDEX.md` 导航。 |
| `docs/feature/_archive/` | 历史资料，只用于追溯，不能覆盖当前方案或状态。 |

## 代码近旁资料

- `data-pipeline/` 的 README、脚本说明、候选与映射 review 是数据生产工具的**代码近旁资料**，不作为项目通用首读入口；涉及预设/营养数据生产时，从 `docs/projectReview/22_预设与参考资料治理（专属）.md` 进入，再按需读取。
- 根目录 `docs/` 的历史需求/规划已迁入 `docs/feature/_archive/legacy_root_docs/`；不得再创建新的根 `docs/` 项目知识副本。
