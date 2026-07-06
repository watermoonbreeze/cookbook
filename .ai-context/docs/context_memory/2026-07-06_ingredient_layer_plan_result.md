# 食材层整体方案梳理与文档重构（结论）

[AI生成] 2026-07-06。依据 chatlog「食材」段 + 现有代码/DB 实测，产出食材层「1总纲+4明细」文档并定案。仅定方案，未落代码。

## 用户三项决策（已拍板）

1. **营养/调养数据架构 = 轻方案**：复用 `food_category.dimension` + `ingredient_category`/`ingredient_care_rule`，**不建** attribute 三表（贴合「营养/调养只是常规分类的聚合，数据源唯一」）。三表降级为未来预留附录。
2. **库存 = 独立 `pantry` 表**（quantity/added_at/expire_at/status），非 ingredient 加列。
3. **文档整理 = 食材主题归拢**，其余大文档保留。

## 文档结构（`.ai-context/docs/feature/`）

- 总纲：`食材层总方案.md`（新建）— 三层架构/数据源模型/6Tab/四明细索引/分阶段路线图
- 明细①`食材品类分类体系.md`（常规品类树13大类，已有）
- 明细②`食材维度体系设计.md`（由旧`食材属性系统设计.md`改写为轻方案，旧文件已删）
- 明细③`食材界面设计.md`（6Tab/详情四区/编辑/库存/拆分，已按 pantry+维度措辞对齐）
- 明细④`食材数据模型与迁移.md`（新建）— 全表盘点/pantry新表/DB版本对齐/迁移排号/搜索/软失效
- 契约：`食材数据规范.md`（seed 字段）；旧方案在 `_archive/`

## 现有代码/DB 已具备（复用，不重建）

`ingredient` 已有 status/reason/last_referenced_at/alias(二级名)；`dish_ingredient.is_main`；`food_category.dimension`(general/nutrition/gi/purine/sodium/fat/sugar/crowd)；`ingredient_care_rule`(v10剂量统一)；`SearchIngredients` 查询。**DB 侧本方案只新增 pantry 表 + 补维度节点。**

## 关键坑（阶段0必先修）

`build.gradle` `version=8` **滞后**于已存在的 `9.sqm`（迁到 v10）；`.sqm` 注释与文件名错位一位；`ingredient.reason` 漏迁移、靠 `DatabaseDriverFactory.android.kt` 驱动 `ALTER` 临时补列。→ 先把 version 对齐真实值(10)、补正式 reason 迁移、移除驱动临时补列，之后新增 `10/11.sqm`(reason 正迁移 + pantry)。

## 路线图

阶段0 版本对齐 → 阶段1 数据层框架(pantry+维度seed+搜索) → 阶段2 拆 `IngredientPickerScreen`(1636行) → 阶段3 内容补充(不动架构) → 阶段4 AI层(预留)。

## 待办（下一步可选）

- 是否开始阶段0/阶段1 的代码落地（需再定级）。
- 三份新文档尚未 git 提交。
