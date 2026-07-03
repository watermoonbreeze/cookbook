# 2026-05-24 Codex 自动任务编排强制执行

## 背景

- 用户指出前一轮任务没有按自动任务编排流程真实分派智能体。
- 根因：`~/.codex/AGENTS.md` 只引用了流程文档路径，但没有强制开发任务入口执行流程，也没有定义 DEV 角色到 Codex 子代理的映射。

## 已更新

- `~/.codex/AGENTS.md`
  - 自动任务编排改为开发类任务强制入口。
  - 增加任务启动门禁：类型、深度、模式、DEV 角色与 Codex 子代理映射。
  - 增加轻量/标准/深度任务的角色分派要求。
  - 增加 Codex `explorer` / `worker` 与 DEV 角色的映射。
  - 增加工具不可用时的降级披露规则。

- `~/.codex/memories/workflow_auto_orchestration.md`
  - 新增“Codex 子代理落地规则”。
  - 明确 DEV_SA/DEV_PM/DEV_ARCH/DEV_CAA/DEV_TEST/DEV_SEC/DEV_REVIEW 使用 `explorer`。
  - 明确 DEV_CODE/DEV_UI/DEV_DB/DEV_UT 使用 `worker`。
  - 阶段0必须输出智能体分派表。
  - 标准/深度任务不得跳过真实子代理分派。

- `AGENTS.md`
  - 项目级补充强制规则：Cookbook 开发任务必须执行 Codex 自动任务编排流程。

## 后续执行要求

- [2026-05-27 AI修改] 任何用户任务都必须先走自动任务编排阶段0，并在动手前告知用户采用的模式/级别、原因和智能体分派策略；轻量任务也必须声明“主线程直接处理”的降级/简化原因。
- 新开发任务开始前先读 `~/.codex/memories/workflow_auto_orchestration.md`。
- 常规模式下，阶段2方案完成后必须等用户确认。
- 标准/深度任务如果当前会话提供 `multi_agent_v1`，必须真实分派子代理。
- 如果工具不可用，必须明确告知用户流程降级。
