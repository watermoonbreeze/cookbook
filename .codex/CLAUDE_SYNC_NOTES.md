# Codex/Claude 双模式配置说明（2026-07-03 更新）

- 公共规范、经验手册、功能文档、上下文记忆、公共 hook 统一存放在 `.ai-context/`，两个工具的入口（`CLAUDE.md` / `AGENTS.md`）都引用它；详见 `.ai-context/README.md`。
- `.codex/` 只保留 Codex 专属内容：`settings.json`、`agents/`、`hooks/pre-tool-use.js`（指向公共 hook 的薄包装）。
- `.claude/` 只保留 Claude Code 专属内容，结构与 `.codex/` 对等。
- 旧的"两边各自维护等价副本、互不读取"模式已废弃；历史副本已于 2026-07-03 合并进 `.ai-context/`（以 codex 侧较新版本为准）。
