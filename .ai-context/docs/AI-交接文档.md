# 跨模型接手指南

> 更新：2026-08-05。适用于 Claude、Codex、DeepSeek 及其他接手本仓库的模型。

## 先读什么

1. 仓库根 `AGENTS.md`（Codex）或 `CLAUDE.md`（Claude）：工具入口和强制规则。
2. `.ai-context/PROJECT.md`：项目事实、真相优先级和当前任务入口。
3. `.ai-context/docs/projectReview/00_导读与索引.md`：项目全貌与按任务阅读路径。
4. `.ai-context/docs/context_memory/SESSION_交接.md`：唯一当前会话状态和下一步。
5. `.ai-context/docs/功能路径索引.md`：确定功能对应的代码路径。

之后按需进入 `projectReview/`、`feature/`、`experience/`。不要把日期快照、`_archive/`、旧待办统计或代码近旁说明当作当前状态。

## 项目速览

- Cookbook 是面向慢性病家庭的饮食规划 App：记一餐、食历复用、饮食推荐、营养与健康提示。
- KMP 架构：`shared` 为 Domain/Data/SQLDelight，`androidApp` 为 Compose UI；当前只交付 Android。
- MVP 核心已完成，当前处于功能扩展与打磨阶段。
- 当前主任务是 AI 记一餐的周期记 + NDJSON 流式解析，实施基线见 `feature/AI记一餐_周期记_NDJSON流式开发规范.md`。

## 工作规则

- 公共项目知识只写 `.ai-context/`；`.claude/` 与 `.codex/` 仅保留各自工具配置。
- 修改架构、流程、页面、算法、数据、AI/网络或公共能力时，同任务更新项目地图、诊断地图、ADR 和功能路径索引。
- 会话接续只读/更新 `context_memory/SESSION_交接.md`；历史只追加到 `SESSION_交接_历史.md`。
- 真机验证只使用时间戳最新的 `feature/真机待验证清单_<yyyyMMddHHmm>.md`。
- 代码、数据与文档冲突时，优先级见 `.ai-context/PROJECT.md`；未经确认的方案不得描述为已实现。

## 资料分级

| 类型 | 位置 | 用法 |
|---|---|---|
| 当前项目地图 | `docs/projectReview/` | 全局理解与反查。 |
| 当前任务状态 | `docs/context_memory/SESSION_交接.md` | 接续工作。 |
| 当前方案与待办 | `docs/feature/` | 实施和验收。 |
| 经验/红线 | `docs/experience/` | 避坑与工程规范。 |
| 历史资料 | `docs/feature/_archive/`、日期快照 | 仅追溯，不作当前依据。 |
| 代码近旁资料 | `data-pipeline/` 等 | 仅在对应工具/数据任务中按需读取。 |
