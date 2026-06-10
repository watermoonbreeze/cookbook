# 2026-06-06 食材最近使用修复结果

## 问题原因

- 原查询按 `meal_record.created_at` 排序，实际表示记录创建/编辑时间，不等于真实用餐时间。[AI生成]
- 原查询没有排除未来计划餐食，导致未来计划里的食材也可能出现在“最近使用”靠前位置。[AI生成]

## 修复

- `selectRecentlyUsedIngredients` 增加 `today` 参数，只统计 `mr.date <= today`。[AI生成]
- 排序改为 `MAX(mr.date) DESC, MAX(mr.meal_time) DESC, MAX(mr.created_at) DESC`。[AI生成]
- `IngredientRepository.listRecentlyUsed()` 传入 `DateTime.today()`。[AI生成]
- 补充单元测试：未来计划不进入最近使用；同创建时间下按实际用餐日期/时间排序。[AI生成]

## 验证

- `./gradlew :shared:generateSqlDelightInterface`：成功。[AI生成]
- `./gradlew :shared:testDebugUnitTest`：成功。[AI生成]
- `./gradlew :androidApp:assembleDebug`：成功。[AI生成]
