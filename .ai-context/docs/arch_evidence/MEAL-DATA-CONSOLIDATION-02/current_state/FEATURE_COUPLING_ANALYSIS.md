# Feature coupling analysis

## Home projection dependency map

- 展示：`observeUpcomingMealDayContents` → Projector → `HomeUiState.plans` → `HomeScreen`。
- 今日营养与实际吃量：`observeTimelineWindow(today,today)` → `todayCards` → `TodayNutrition`、`todayMeals`。
- 色系墙/年度统计：同一 `DayMealCardData.meals` 派生主料名和日期评级。
- 结论：Home 同时绑定 Card Projection 的展示语义与营养派生语义。

## Timeline projection dependency map

- `observeTimelineDates` 仅提供有记录日期；VM 建立分页窗口。
- `loadMealDayContentsByDates` 返回内容，VM 再调用 Projector 写入 `TimelineUiState.pages`。
- Screen 直接消费 `pages` 并复用 `DayMealCardView`。
- 结论：Timeline 依赖 `MealDayContent` 到 Card 的转换，但拥有独立日期窗口状态。

## WeekPlan semantic analysis

- WeekPlan 通过 `observeTimelineWindow(ws,we)` 获取整周每天的卡片，包含空天。
- VM 未读取或修改 `isPlanState`；Screen 只使用 `day.isToday` 做当前日强调。
- 删除/撤销仍调用 `deleteDayMeals` / `saveDayMeals`，没有 Plan 表或 Plan repository。
- 结论：未来日期在当前实现中是时间投影意义，不足以证明存在持久化 MealPlan；不能将 future meal 自动解释为独立计划实体。

## Search coupling analysis

- `SearchViewModel` 的餐食搜索调用 `mealRepo.searchMealCards`，结果类型是 `List<DayMealCardData>`。
- Repository 先用搜索 SQL 得到日期，再通过 `loadTimelineCardsByDates` 组装完整卡片。
- Search Screen 仅展示日期和卡片餐内容，未发现直接消费 `MealDayContent`。
- 结论：Search 与 DayMealCardData 有直接耦合，但与 Temporal Role 的业务决策耦合较弱。

## Nutrition dependency analysis

- `NutritionTableViewModel` 只依赖 `NutritionRepository` 与 `IngredientNutritionRow`，不依赖 Meal Projection。
- Home 的 `todayNutrition`/`nutritionWall` 依赖餐卡中的 dishes、主料名和 eatenRatio。
- WeekPlan 的 `nutritionLine` 从 `days.meals.dishes.mainIngredientNames` 聚合。
- `DietReportAggregator` 入参是 `List<DayMealCardData>`，并据此生成 per-day nutrition。
- 结论：F-NUTRITION 的“营养表”独立；餐食营养报告/首页/周计划营养派生依赖 Card Projection 的 meal 内容，但未发现其依赖 `isPlanState`。

