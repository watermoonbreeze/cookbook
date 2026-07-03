# 方案结论：食材调养体系梳理与展示优化（待用户确认）

时间：2026-07-03。[AI生成] 状态：方案已交付，等确认后实施。

## 关键事实（DEV_SA 摸底）

1. 调养(CARE) tab 点分类走 `ingredient_category` 关联，但该表中**没有任何食材↔调养分类的行**（ingredients.json 的 categories 无 care_*）→ 调养 tab 点任何分类**恒为空**。真实调养数据在 `ingredient_care_rule`（仅 15 条种子）。
2. 调养分类层级不统一：仅痛风/糖尿病挂了绿/黄/红剂量子分类，其余 11 病种为叶子；care rule 的 category 有的指剂量子类、有的指病种大类。`advice_level` 字段与绿/黄/红分类节点语义重复。
3. 详情不刷新根因：`loadIngredientDetail` 仅由 `LaunchedEffect(selectedIngredient?.id)` 触发，编辑保存 id 不变 → detail payload 不重载。
4. IngredientCard 名称 `name(alias)` 单行 maxLines=1 截断。
5. 关联菜品的 `DishMini.cookingMethodNames` 数据已具备，按烹饪方式分组是纯 UI 工作。
6. ingredient_detail 覆盖 6/87；库中无数值型营养指标字段，营养/嘌呤/GI 只有分类标签。

## 推荐方案要点

- **模型统一**：care rule 的 category 只指病种层（大类+便秘/腹泻/甲亢/甲减等子型），剂量由 advice_level 表达；删除 6 个绿/黄/红分类节点（DB 迁移 + seed + 启动清洗，旧数据剂量子类→病种大类，冲突取最严）。
- **CARE tab 改走 care rule 查询**，结果按 绿灯/黄灯/红灯 三段分组展示。
- 编辑器调养分类下拉过滤为病种层节点（随模型统一自然解决）。
- 详情刷新：LaunchedEffect key 增加 `ui.lastSavedIngredientId` 触发重载。
- 名称两行：第一行 name，第二行 (alias) 小 2 号 onSurfaceVariant，恒占位保证网格对齐。
- 关联菜品按烹饪方式分组平铺，多方式菜品每组都出现，无方式归"其他"。
- 数据补充分层：L1 标签层（以日常食材维度.md 为底，按膳食指南/食物成分表核对，补全 87 食材标签 + care rule 扩到 13 病种、detail 文本补全，可扩食材至 ~150）；L2 数值层（GI 值/嘌呤 mg 等数值字段）暂缓，待有摄入计算场景再做。

## 实施批次建议

批次A（速赢，纯 UI/VM）：详情刷新 + 两行名称 + 菜品按烹饪方式分组。
批次B（模型，含 DB 迁移）：调养模型统一 + CARE tab 查询重构 + 编辑器下拉。
批次C（数据工程）：L1 标签层数据补充。

## 已定案

- 自定义食材固定只在自定义 tab 显示——现状代码已满足（filterForTabSource），仅 RECENT tab 会混入自定义食材（合理，待用户确认）。
