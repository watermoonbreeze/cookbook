# DAY_MEAL_CARD_DATA usage map

## 创建点

- `MealDayCardProjector.project(MealDayContent, referenceDate)` 创建 `DayMealCardData`。
- Repository 入口：`observeDayMealCard`、`observeTodayPlusFuture`、`observeTimelineCards`、`loadTimelineCardsByDates`、`searchMealCards`、`observeTimelineWindow`、`loadDayMealCard`。
- `MealDayContent` 的组装集中在 `MealRecordRepository.buildMealDayContent(s)`。

## 字段事实

| 字段 | 写入/派生 | 消费者 |
|---|---|---|
| `date` | `MealDayContent.date` 透传 | 五功能日期分组/定位 |
| `temporalRole` | Projector 按 `date` 与 reference date 计算 | `DayMealCardView`、WeekPlan Screen |
| `meals` | Repository 从 meal records 组装 | Card UI、Home/WeekPlan/DietReport 营养派生 |
| `isToday` | `temporalRole == TODAY` getter | Home、DayMealCardView、WeekPlan Screen |
| `isPlanState` | `temporalRole == FUTURE` getter | `DayMealCardView`；未见 WeekPlan VM 依赖 |

## 关键转换

`Database Truth → MealDayContent → DayMealCardProjector → DayMealCardData → Feature UiState / shared card UI`

Search 不绕过 Projection：它按关键字查日期，随后调用 `loadTimelineCardsByDates` 重新生成卡片；NutritionTable 则完全不走该链。

