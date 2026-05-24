# Codex/Claude 双模式独立配置说明

- `.codex` 是 Codex 项目配置目录，`.claude` 是 Claude Code 项目配置目录；两边互不作为运行时依赖。
- 当已有 Claude Code 配置需要适配给 Codex 时，应转换为 Codex 自身规则和路径，例如 `CLAUDE.md` 转为 `AGENTS.md`、`.claude/docs/` 转为 `.codex/docs/`。
- Codex 工作时读取 `AGENTS.md` 和 `.codex/`；Claude Code 工作时读取 `CLAUDE.md` 和 `.claude/`。
- 两边可以维护等价内容，但不通过让一侧读取另一侧配置来实现兼容。
