# 2026-06-07 任务6食材最近使用口径修正结果

## 原实现

- “最近使用”通过 `meal_record -> meal_record_dish -> dish_ingredient -> ingredient` 查询。[AI生成]
- 只有食材所在菜品被加入餐食后才会出现；编辑菜品刚添加食材不会显示。[AI生成]

## 新实现

- `ingredient` 表新增 `last_referenced_at INTEGER NOT NULL DEFAULT 0`。[AI生成]
- 数据库版本从 v5 升到 v6，并新增 `5.sqm` 迁移。[AI生成]
- 保存菜品时，只要食材写入 `dish_ingredient`，同步更新 `ingredient.last_referenced_at`。[AI生成]
- “最近使用”查询改为 `last_referenced_at > 0 ORDER BY last_referenced_at DESC`。[AI生成]

## 验证

- `./gradlew :shared:generateSqlDelightInterface`：成功。[AI生成]
- `./gradlew :shared:testDebugUnitTest`：成功。[AI生成]
- `./gradlew :shared:verifySqlDelightMigration`：成功。[AI生成]
- `./gradlew :androidApp:assembleDebug`：成功。[AI生成]

## 备注

- 历史库升级后，旧食材默认 `last_referenced_at=0`，不会全部出现在最近使用。[AI生成]
- 后续如果希望历史已有菜品食材也进入最近，可再做一次历史回填迁移，但当前按“新增/编辑菜品后进入最近”处理。[AI生成]
