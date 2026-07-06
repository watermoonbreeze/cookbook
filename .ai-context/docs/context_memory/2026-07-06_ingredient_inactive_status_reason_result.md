# 结论：食材失效状态（status/reason）与引用不断裂

时间：2026-07-06。[AI生成] 提交：f549afb。DB 版本 v11（10.sqm）。

## 需求

食材失效用 `status`(0失效/1有效) + `reason`(失效原因) 表达；失效食材**不影响已引用它的菜品**（灰显保留），
覆盖两个来源：后台下架的预设食材、用户删除的自定义食材。已确认：失效食材在列表隐藏、仅菜品引用处灰显；自定义可恢复、预设跟后台。

## 引用断裂的两处根因（都已修）

1. `selectIngredientsOfDish`（菜品读食材）原带 `i.status=1` 过滤 → 失效食材在菜品里消失。**已去掉过滤并读出 status/reason**。
2. `deleteUserIngredient` 原先还软删 `dish_ingredient` 关联（deleteDishIngredientsByIngredient）→ 引用也没了。**已改为只置 status=0+reason，不动关联**。

## 关键实现

- DB：ingredient 加 `reason`；`deleteUserIngredient(reason,id)`；新增 restoreUserIngredient / hardDeleteUserIngredient / selectInactiveUserIngredients / updatePresetIngredientStatus / hardDeleteDishIngredientsByIngredient。
- 预设失效跟后台：seedFoundationIngredients 用 `updatePresetIngredientStatus(seed.status,seed.reason,id)`，SeedIngredient 加 status/reason（默认1/""，兼容旧 JSON）。JSON 内容不变时指纹守卫会跳过重跑——存量预设 status 由迁移默认=1；后台真下架时改 JSON→指纹变→重跑生效。
- 硬删除：`dish_ingredient.ingredient_id` 无 ON DELETE CASCADE，故 hardDelete 先 `hardDeleteDishIngredientsByIngredient` 物理删关联再删食材。
- 模型：Ingredient 加 status/reason（默认1/""，多数查询不读、保持有效语义）；DishRepository 组装填充。
- UI：菜品详情失效食材灰显+原因；食材页自定义 tab「已失效」回收站（恢复/彻底删除，彻底删除二次确认）；失效食材仍从食材列表/选择器隐藏（保持 status=1 过滤）。

## 验证

- IngredientRepositoryTest 13 个全过：新增「软删后菜品保留引用带 status=0 + 列表隐藏 + 进回收站」「恢复回列表」「彻底删除清食材与关联」。
- :androidApp:assembleDebug BUILD SUCCESSFUL。

## 后台下架预设食材的用法（给后续/爬虫管线）

在 ingredients.json 里给某食材加 `"status":0,"reason":"..."`，用户点「更新基础数据」或下次内容变更即失效；改回 `status:1` 即恢复。见 `食材数据规范.md`。
