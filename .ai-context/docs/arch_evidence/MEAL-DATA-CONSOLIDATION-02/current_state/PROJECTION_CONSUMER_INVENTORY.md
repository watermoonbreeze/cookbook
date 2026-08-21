# Projection consumer inventory

| Feature | State/model | Input path | Direct `DayMealCardData` use | Fact boundary |
|---|---|---|---|---|
| Home | `HomeUiState.plans` | `observeUpcomingMealDayContents` → `MealDayCardProjector` → VM | `plans`、`todayCards`、`nutritionWall`、`todayMeals`、`todayNutrition` | Home 同时消费日期/卡片展示和营养派生 |
| Timeline | `TimelineUiState.pages` | `observeTimelineDates` → `loadMealDayContentsByDates` → Projector | `pages`；Screen 传入 `DayMealCardView` | 日期窗口与卡片内容是两段链路 |
| WeekPlan | `WeekPlanUiState.days` | `observeTimelineWindow` → `DayMealCardData` | `days`、`isToday` 展示、整周营养线 | 未来日期显示不等于独立 Plan persistence |
| Search | `SearchUiState.meals` | `searchMealCards` → `loadTimelineCardsByDates` | 搜索结果直接是卡片 | 搜索按餐日期/菜名查询，不消费 `MealDayContent` |
| Nutrition | `NutritionTableUiState.rows` | `NutritionRepository.allIngredientNutrition` | 表格本身不消费卡片 | Home/WeekPlan/DietReport 另有餐卡营养派生链 |

共享事实：`MealDayContent` 由 Repository 从 meal records + meal types + dishes 组装；`DayMealCardProjector` 唯一计算 `TemporalRole`，`isToday`/`isPlanState` 是派生 getter。

