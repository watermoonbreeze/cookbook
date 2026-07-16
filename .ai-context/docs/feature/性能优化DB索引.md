# 工程性能优化 · DB 索引（数据规模化第一步）

> 2026-07-16 无人值守落地。用户提"数据规模化性能优化"（待办 B 节），拆为①DB索引(本项)②异步seed(单独评估)③显示优化。

## 一、目标
预设数据（食材/菜品/营养）增大 → 高频查询/JOIN 全表扫拖慢。给**非主键前导的高频过滤/JOIN/反查列**补索引，只提速不改行为。

## 二、盘点结论（读 Cookbook.sq 实测）
既有索引已较全：dish(name/preference/updated_at)、meal_record(date/created_at)、ingredient(pinyin)、food_category(parent)、care_rule(category_id,advice_level)、各 sort_order 复合索引；`ingredient.name`/`crowd_ingredient`/`pantry.ingredient_id` 有 UNIQUE、`ingredient_nutrition/detail.ingredient_id` 是 PK——均已索引。

**真正缺的 = 复合主键/唯一索引"非前导列"的高频查询**（前导列查询已被覆盖，另一列过滤则全扫）：
| 新索引 | 表(现有约束前导列) | 受益热查询 |
|---|---|---|
| `idx_ingredient_category_category(category_id)` | ingredient_category PK 前导 ingredient_id | 按分类列食材(IngredientPicker 浏览) `WHERE ic.category_id=?/IN` |
| `idx_meal_record_dish_dish(dish_id)` | meal_record_dish PK 前导 meal_record_id | selectDishesByRecent(JOIN d.id=mrd.dish_id)、cookStats(做过几次 by dish_id) |
| `idx_dish_ingredient_ingredient(ingredient_id)` | dish_ingredient uq 前导 dish_id | 删食材清关联 `WHERE ingredient_id=?`、食材→菜反查 |

**Google/DBA 审查建议1同批补的 3 个同构多对多反查列**（PK 前导另一列，反查全表扫）：
| 新索引 | 表(PK 前导) | 受益 |
|---|---|---|
| `idx_dish_tag_rel_tag(tag_id)` | dish_tag_rel PK 前导 dish_id | 按标签筛选菜品(标签反查菜) |
| `idx_dish_cooking_method_rel_method(cooking_method_id)` | 同上 | 按烹饪方式反查菜 |
| `idx_favorite_combo_dish_dish(dish_id)` | favorite_combo_dish PK 前导 combo_id | 按菜删收藏组合关联 |

共 **6 个索引**（3 首批 + 3 审查同批补）。未加：care_rule 按 category 已有复合索引；member_care/day_absentee 小表；crowd_ingredient 遗留(被 care_rule 取代)。

## 三、实现
- `24.sqm`：6 条 `CREATE INDEX IF NOT EXISTS`（幂等、纯加索引不改表结构不动数据=最安全迁移；6 列均老列无"列不存在"风险；名全新无冲突）。DB version 23→24（红线：由 .sqm 数推导）。
- `Cookbook.sq`：同步加同名 6 索引（新装走 Schema.create 需含）。
- **不需** SEED_LOGIC_VERSION bump（纯 schema，非 seed 逻辑变更）。

## 四、状态
- ✅ `:shared:testDebugUnitTest` + `:androidApp:assembleDebug` 通过（schema 已重新生成）。
- ✅ 迁移推演：v23→24 对老列幂等加索引，无副作用。
- ✅ Google/DBA 审查门禁：**无阻断**。3 索引选型/迁移安全/一致性全部正确；采纳建议1（同批补 3 个同构反查列索引 → 共6）；建议2(写放大可忽略)/建议3(不必加 status 复合索引，软删占比低选择度差)为确认类无需改。
- **真机待验**：老库升级到 v24 初始化正常（红线：迁移改动真机验"初始化数据"）；大数据量下查询提速（可 adb pull 库 + EXPLAIN QUERY PLAN 验证走索引）。

## 五、剩余（性能优化后续项，未做）
- **异步 seed**：首装/内容变更时 seed 全量写耗时上升→后台化 + 首屏 loading（风险高：动 seed 流程 + 需真机验启动，单独评估）。
- **显示/逻辑优化**：消除 N+1（如 countChildren）、大列表虚拟化、combine+mapLatest 批量查（部分已做）。
- **P2 SQLDelight 写入 Diff**：全量清空再插入 → 差异更新（工程待办）。
