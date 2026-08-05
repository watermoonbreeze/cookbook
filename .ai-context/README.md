# .ai-context — Claude Code 与 Codex 的公共 AI 上下文目录

> 创建时间：2026-07-03。[AI生成] 由双模式配置抽取而来。

本目录是 **Claude Code 和 Codex 共同引用的唯一公共配置/文档源**。两个工具的入口文件（`CLAUDE.md` / `AGENTS.md`）都指向这里；往本目录放内容，两边都能看到。

## 目录结构

```
.ai-context/
├── README.md                  # 本文件：公共目录说明与维护规则
├── PROJECT.md                 # 跨模型项目首读、事实优先级与当前任务入口
├── rules/
│   └── 通用规则.md            # 双端通用强制规则（任务编排、快照、注释、测试等）
├── hooks/
│   └── pre-tool-use.js        # 公共安全 hook（危险命令拦截），两侧 settings.json 均指向此文件
└── docs/
    ├── context_memory/        # 上下文记忆（双端共写共读，任务快照/阶段结论）
    ├── experience/            # 经验手册（INDEX.md 为索引，09 为工程统一规范）
    ├── feature/               # 功能/方案文档（MVP、数据库、食材体系、端侧AI等）
    ├── projectReview/         # 项目地图（00 为全局首读入口）
    ├── theme_backup/          # 主题改版前的代码备份
    └── unattended_decisions.md # 无人值守决策日志
```

## 分工原则

| 内容 | 位置 |
|---|---|
| 项目规范、经验、功能文档、上下文记忆、通用规则、公共 hook | `.ai-context/`（本目录，唯一来源） |
| Claude Code 专属：settings.json、agents/、hook 薄包装 | `.claude/` |
| Codex 专属：settings.json、agents/、hook 薄包装、CLAUDE_SYNC_NOTES | `.codex/` |
| Claude 临时文件 | `temp/claude/` |
| Codex 临时文件 | `temp/codex/` |

## 维护规则

1. **公共内容只写这里，不再双份维护**。历史上 `.claude/docs` 与 `.codex/docs` 各存一份的做法已废弃（2026-07-03 合并，以 codex 侧较新版本为准）。
2. `.claude/` 与 `.codex/` 只保留各自工具**必须**放在原生目录才能被识别的内容（如 agents 定义、settings.json）；这类内容如需双端等价，按各自格式分别维护并在文件头注明对应关系。
3. hook 逻辑改动只改 `.ai-context/hooks/pre-tool-use.js`；两侧 `hooks/pre-tool-use.js` 是薄包装，不要往里加逻辑。
4. 新模型首读 `.ai-context/PROJECT.md`；再按其中顺序读取项目地图、当前交接与路径索引。历史资料只作追溯，不能覆盖当前状态。
5. 上下文记忆写入 `.ai-context/docs/context_memory/`，命名 `YYYY-MM-DD_主题.md`，单文件 ≤100 行，只存结论；`SESSION_交接.md` 是唯一当前接续入口。
6. 数据库、主题、架构边界或公共组件变更时，同步更新 `docs/feature/` 对应文档。
