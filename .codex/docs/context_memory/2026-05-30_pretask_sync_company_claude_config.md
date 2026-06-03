# 2026-05-30 任务前快照：同步公司 Claude 配置

## 用户最新需求
- 读取公司使用的 Claude 配置目录：`/Users/sxd/Documents/国安广传/work-record/国安广传/临时资料/.claude/`
- 分析其中哪些配置可以通用使用。
- 同步到本机 Claude 与 Codex 配置中。
- 完成后重新载入配置，确保以后按这些配置执行。

## 执行模式
- 标准级配置迁移/规则整合。
- 原因：涉及跨全局配置目录同步，需要筛选规则、避免迁移敏感运行态文件，并保持 Claude/Codex 双模式独立。

## 计划分派
- DEV_SA：分析公司 `.claude/` 目录结构和可迁移规则。
- DEV_SEC：检查是否包含 auth、history、sessions、cache、token、cookie、密钥等不可迁移内容。
- DEV_CODE：主线程执行配置合并到 `~/.claude/` 与 `~/.codex/`。
- DEV_TEST：复查目标配置文件是否写入成功、路径是否合理。

## 当前已知状态
- 本项目已有双模式规则：Codex 使用 `~/.codex/AGENTS.md` / 项目 `AGENTS.md`，Claude 使用 `~/.claude/CLAUDE.md` / 项目 `CLAUDE.md`。
- 现有规则要求不得让 Claude 运行时读取 Codex 文件，也不得迁移运行时状态和凭据。

## 预计涉及路径
- 来源：`/Users/sxd/Documents/国安广传/work-record/国安广传/临时资料/.claude/`
- Claude 目标：`~/.claude/`
- Codex 目标：`~/.codex/`

## 主要风险
- 公司配置可能包含项目专用规则，不应无差别加入全局。
- 可能包含敏感文件或运行态目录，必须跳过。
- Claude 与 Codex 入口语义不同，Codex 侧需转换为 `AGENTS.md`、`.codex/commands`、`.codex/agents` 等格式。

## 待验证项
- 目标配置是否存在备份或合并记录。
- 新增规则是否没有覆盖原有个人规则。
- 是否明确记录跳过项和同步日期。
