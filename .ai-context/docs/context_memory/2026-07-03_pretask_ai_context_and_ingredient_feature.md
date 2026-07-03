# 任务前快照：.ai-context 公共化 + 食材功能补全

时间：2026-07-03。[AI生成]

## 用户需求（4 项）

1. 抽取 .codex/.claude 公共内容为 `.ai-context/`，两边入口都引用它，后续往 `.ai-context` 放内容双方可见。
2. 重新整理 `.claude/` 目录，符合关联公共目录的格式。
3. 当前以 Claude 为主力，把 codex 侧后续新增的规则统一进公共规则（`.ai-context`），CLAUDE.md 与 AGENTS.md 分别引用。
4. 检查项目进度：chatlog.md 的「## 食材与菜品关联做法」为最后进行的功能（参考 temp/claude/日常食材维度.md），补全剩余功能，并修复项目遗留 bug。

## 执行模式

- 级别：深度；交互模式：无人值守（用户给出完整清单并要求全部处理好）。
- codex 侧最后进度：2026-06-25 `pretask_ingredient_custom_category_rules.md` 无对应 done 文件，
  「食材自定义分类与编辑规则调整」很可能做了一半。
- 最近提交：98d6200「食材界面改造2」、31ddbb8「食材界面改造1」。

## 「食材与菜品关联做法」需求点（来自 chatlog）

1. 自定义 tab 只展示用户手动添加的食材（当前预设/自建混在一起）。
2. 预设食材编辑仅允许改二级名称+图片；自定义食材可编辑全部。
3. 编辑页分类归属：逗号显示已选分类 + 「选择分类」弹出分类选择器（标题「分类选择」，右侧 + 创建，风琴式展开）。
4. 食材界面分类筛选：选一级大类→该大类全部食材分页 30；选任意层级分类→逐层聚合子分类食材。

## 预计涉及

- 配置：`.ai-context/`（新建）、CLAUDE.md、AGENTS.md、.claude/、.codex/CLAUDE_SYNC_NOTES.md
- 代码：IngredientPickerScreen/ViewModel、IngredientRepository、Cookbook.sq（如需分页查询）

## 风险

- 双模式引用改造不能破坏 codex 现有工作方式（AGENTS.md 仍是 codex 入口）。
- 预设/自定义编辑权限区分需防止误覆盖预设数据。
