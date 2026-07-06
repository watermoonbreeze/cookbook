# 任务前快照：食材失效状态（status/reason）与引用不断裂

时间：2026-07-06。[AI生成] 级别：深度（含 DB 迁移）。模式：常规授权实施。

## 需求（用户已确认方案与两处决策）

- 食材失效用 `status`（0失效/1有效，已存在）+ 新增 `reason`（失效原因）表达。
- 两个来源：① 后台下架的预设食材（数据包带 status=0+reason 导入）；② 用户删除的自定义食材。
- 核心：失效食材**不影响已引用它的菜品**——菜品引用处灰显保留、可看原因，不断裂。

## 已拍板决策

- Q1 失效食材在食材页/选择器列表：**隐藏**（保持现有 status=1 过滤），仅菜品引用处灰显。
- Q2 恢复能力：**自定义可恢复**（status 置回 1），预设跟后台（重新上架即恢复，界面只读）。

## 关键现状（分析结论）

- `ingredient` 已有 `status INTEGER NOT NULL DEFAULT 1`；无 `reason`。
- `deleteUserIngredient`：UPDATE status=0，仅 source='user'（预设删不掉）。
- 食材列表查询（search 536、按分类 552、care 652 等）全过滤 status=1 → 失效食材从列表消失（保持）。
- **菜品读食材查询 `Cookbook.sq:856` 带 `i.status=1`** → 失效食材在菜品里消失（引用断裂）——这是要改的关键点。

## 实施要点

1. DB：ingredient 加 `reason TEXT NOT NULL DEFAULT ''`；`10.sqm` 迁移，schema v11。
2. SQL：菜品读食材放开 i.status 过滤并读出 status+reason；deleteUserIngredient 补 reason；新增 listInactiveUserIngredients/restoreUserIngredient；seed 支持写 status/reason。
3. Domain：Ingredient 加 status+reason；DishIngredient 承载失效态；mapIngredientRow 更新。
4. UI：菜品详情/编辑失效食材灰显+原因；食材页自定义 tab 加"已失效"回收站入口（恢复/彻底删除；预设只读）。
5. 测试 + build-cli 验证 + 分阶段 [unattended] 提交。

## 风险

- 放开菜品读食材的 status 过滤后，需确认搜索/统计等其他依赖该查询的地方不被误伤。
- Ingredient model 加字段影响 mapIngredientRow 所有调用点，逐一核对。
