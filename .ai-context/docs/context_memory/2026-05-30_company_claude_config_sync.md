# 2026-05-30 公司 Claude 配置同步

## 来源
- `/Users/sxd/Documents/国安广传/work-record/国安广传/临时资料/.claude/`

## 同步到 Claude
- 更新 `~/.claude/workflow_auto_orchestration.md`：同步公司版微任务、方案风险评审、返工分层等规则。
- 更新 `~/.claude/CLAUDE.md`：保留本机语言、同步规则和非开发智能体，补充强制前置步骤、任务前上下文快照、同步记录。
- 更新 `~/.claude/commands/`：同步公司版 `myinit`、`zongjie`、`fansi`、`create-android-project`。
- 更新 `~/.claude/agents/DEV_*.md`、`~/.claude/memory/*.md`、`~/.claude/skills/maven_upgrade/`。

## 同步到 Codex
- 更新 `~/.codex/AGENTS.md`：新增微任务、方案风险评审、任务前快照和公司配置同步记录。
- 更新 `~/.codex/memories/workflow_auto_orchestration.md`：在保留 Codex 子代理映射和任务前快照门禁的基础上，合入公司版微任务/方案风险评审/返工分层规则。
- 更新 `~/.codex/commands/myinit.md`：补充入口文件补充模式、自维护规则、任务前快照要求。
- 更新 `~/.codex/commands/zongjie.md`：补充结构化反思流程和 `introspection/` 目录规则。
- 更新 `~/.codex/agents/DEV_*.md`、`~/.codex/memories/*.md`、`~/.codex/skills/maven_upgrade/`。

## 跳过项
- 跳过 `adb_transfer` 技能：依赖 Windows 绝对路径 `D:\Company\AI\skill-script\adb_transfer.py`，当前 macOS 不可直接使用。
- 未同步运行态/敏感目录：`auth`、history、sessions、cache、sqlite、telemetry、projects、file-history、paste-cache。

## 备份
- Claude：`~/.claude/backups/config_sync_2026-05-30/`
- Codex：`~/.codex/backups/config_sync_2026-05-30/`

## 重新载入
- 已在本次会话中重新读取并复查 `~/.codex/AGENTS.md`、`~/.codex/memories/workflow_auto_orchestration.md`、`~/.claude/CLAUDE.md`、`~/.claude/workflow_auto_orchestration.md`。
- 后续任务按新规则执行：阶段0声明 → 任务前快照 → 分析/实现 → 验证 → 任务后上下文/经验总结。

## 最终一致性复查
- `~/.claude/agents` 与 `~/.codex/agents`：DEV 角色文件一致；Codex/Claude 各自运行时按自身入口读取。
- `~/.claude/skills` 与 `~/.codex/skills`：业务技能一致为 `maven_upgrade`；Codex 额外 `.system` 是系统内置技能，正常保留。
- `adb_transfer`：Claude 与 Codex 两边均未安装，符合“Windows 绝对路径不可用则跳过”的策略。
- 命令文件：两边存在语义差异，属于正常差异。Claude 命令使用 `CLAUDE.md`/`.claude/`，Codex 命令使用 `AGENTS.md`/`.codex/` 并保留双模式同步规则。
- 模式入口：两边均具备强制入口门禁、任务前上下文快照、微任务快速通道、方案风险评审、无人值守方案确认规则。
