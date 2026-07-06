# 阶段1b：库存（我家食材）pantry 数据层 · 无人值守

[AI生成] 2026-07-06 无人值守。承接阶段1，按数据模型明细④建 pantry 表。

## 成果（build+test 通过，Schema.version→12）

- `Cookbook.sq`：新增 `pantry` 表（ingredient_id 唯一，quantity/unit_id/added_at/expire_at/note/status）+ 命名查询（upsert/list/ids/remove/count）。
- `11.sqm`：v11→v12 正式迁移（按刚立的迁移规矩，version 自动升 12）。
- `PantryRepository`：加入/移出/在手列表(Flow+一次性)/在手 id 集合/计数；已注册 Koin。
- `PantryRepositoryTest`：加入+列表、重复加入不重、移出后可再加入，3 用例通过。

## 关键实现决策

- upsert 用 `INSERT OR REPLACE`（非 `ON CONFLICT DO UPDATE`），兼容 Android 低版本 SQLite。
- 库存 Tab 查询 JOIN ingredient 且列与 `selectAllIngredients` 对齐，直接复用 ingredient 行映射。
- 移出=status=0 软失效（保留复购历史），重复加入=替换刷新为在手。

## 下一步（阶段1 剩余 / 待用户回来确认）

- 全局搜索（跨库、字符包含、结果跳转分类）——含 UI，风险较高。
- 库存/常规/营养/调养 6 Tab 的 UI（拆 IngredientPickerScreen 1636 行）——UI 大改，建议用户在线时做。
- 属于**待确认队列**：UI 层大改动无人值守暂缓，先把数据层备齐。
