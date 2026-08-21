# SQL and storage inventory

## Schema

来源：`source_snapshot/shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq`。

| Table/query | Evidence | Current fact |
|---|---|---|
| `meal_type` | `Cookbook.sq:234-255` | 餐次表，含 `id/code/name/default_time/is_fixed/status` |
| `meal_record` | `Cookbook.sq:258-269` | 主记录，含 `date TEXT`、`meal_type_id`、`meal_time`、`note`、`created_at`、`status`；无 `plan` 字段 |
| `meal_record_dish` | `Cookbook.sq:271-285` | 关联表，含 `meal_record_id`、`dish_id`、`sort_order`、`eaten_ratio`、`status` |
| `selectMealRecordsByDate` | `Cookbook.sq:1444-1446` | 按日期读取有效记录，排序由 SQL 定义 |
| `selectTimelineDatesAsc` | `Cookbook.sq:1448-1452` | `DISTINCT mr.date`，只返回有效记录日期，升序 |
| `selectMealRecordsByDates` | `Cookbook.sq:1454-1461` | 批量按日期读取有效记录 |
| `selectMealRecordsBetween` | `Cookbook.sq:1463-1478` | 连续日期窗口读取 |
| `selectUpcomingMealRecords` | `Cookbook.sq:1480-1484` | 从 today 起读取有效记录，供 Home projection |
| `selectDistinctDates` | `Cookbook.sq:1486-1501` | 有记录日期分页查询 |
| `selectMinAndMaxDate` | `Cookbook.sq:1503-1507` | 日期范围查询 |
| `deleteMealRecordsByDate` / `deleteMealRecordDishesByDate` | `Cookbook.sq:1509-1517` | 按日软删：`status=0`，先关联后主记录 |
| `insertMealRecordDish` | `Cookbook.sq:1519-1522` | 写入餐次-菜品关联和排序 |
| `selectEatenRatiosByDate` | `Cookbook.sq:1564-1569` | 按 `(meal_type_id,dish_id)` 快照食用比例 |
| `updateEatenRatioForMeal` | `Cookbook.sq:1558-1562` | 就地更新整餐食用比例，不替换主记录 |

## plan/actual 区分

- 数据库 schema 中未发现 `MealPlan` 表、`plan_state` 字段或 `is_plan_state` 存储字段。
- `MealRecordRepository.buildDayMealCard` 在 `MealRecordRepository.kt:421-442` 接收 `plan: Boolean`，返回 `DayMealCardData(isPlanState = isPlan)`；`observeTodayPlusFuture` 对 today 传 `false`、未来日期传 `true`（`78-100`）。
- `loadDayMealCard` 使用 `date > today` 派生 `plan`（`206-209`）。这证明当前 plan/actual 是投影时按日期推导的 UI 语义，不能据此推断数据库保存了计划态。

## 日期键、餐次、菜品

- 日期键是 `meal_record.date` 字符串，Kotlin 侧通过 `DateTime.formatDate/parseDate` 转换；餐次通过 `meal_record.meal_type_id -> meal_type.id`；菜品通过 `meal_record_dish.dish_id -> dish.id`。
- `MealRecordRepository.save`（`224-249`）单餐插入主记录、关联菜品和偏好计数，事务包裹。
- `saveDayMeals`（`258-313`）整日原子替换：读取旧菜和 eaten ratio → 软删当天记录/关联 → 重插 drafts → 回填 eaten ratio；是否抬偏好由 `bumpPreference` 控制。
- 删除日记录（`MealRecordRepository.kt:360-368`）调用两条按日软删 SQL；撤销使用 `snapshotDay` + `saveDayMeals(..., bumpPreference=false)`。
- AI `DayAutoGenerator.commit` 最终仍调用 `saveDayMeals`，没有第二套 meal_record 存储真相。
