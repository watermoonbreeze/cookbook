# Query and projection inventory

## Home

`meal_record` / `selectUpcomingMealRecords(today, limit)` → `MealRecordRepository.observeTodayPlusFuture` 分组日期、today `plan=false`、future `plan=true`、组装 `DayMealCardData` → `HomeViewModel.uiState: StateFlow<HomeUiState>` → `HomeScreen` 直接按 `plans` 找 today/future 并传 `DayMealCardView`。

Evidence: `MealRecordRepository.kt:78-101,421-442`; `HomeViewModel.kt:36-40,210-218`; `HomeScreen.kt:59-74,206-272`。

## Timeline

`selectTimelineDatesAsc` → `MealRecordRepository.observeTimelineDates: Flow<List<LocalDate>>` → `TimelineViewModel` 保存 `allDates/mealDates` 并建立分页窗口 → `loadTimelineCardsByDates` 批量查记录并组装 `DayMealCardData` → `TimelineUiState.pages` → `FoodTimelineScreen` 以 `DayMealCardView` 渲染；日历 dialog 只消费日期集合。

Evidence: `MealRecordRepository.kt:132-149`; `TimelineViewModel.kt:58-224`; `FoodTimelineScreen.kt:149,185`。

## Manual AddMeal

`selectMealRecordsByDate` / edit queries → `loadDayMealsForEdit` → `AddMealViewModel` 映射为 `MealBlockUiState`，日期和 existing guard 保存在 VM → `AddDayFoodScreen` 展示/编辑 → `DayMealDraft` → `saveDayMeals` 原子整日替换；预览卡在保存前由草稿组装 `DayMealCardData` 并复用 `DayMealCardView`。

Evidence: `MealRecordRepository.kt:333-369`; `AddMealViewModel.kt:222,418-428,514-574`; `AddDayFoodScreen.kt:546`。

## AI AddMeal

AI/规则输入 → `StreamingMealSession`/`AiMealInputViewModel` 合并 `DayMealJson` → `MultiDayRecorder.previewAll` → `DayAutoGenerator.preview` → `AutoGenPreview`（仍内存）→ UI `PreviewPhase` → `confirmSave` → `MultiDayRecorder.commitPreview` → `DayAutoGenerator.commit` → `saveDayMeals` → `AutoGenResult`/`DONE`。

Evidence: `AiMealInputViewModel.kt:693-785,955-1050`; `MultiDayRecorder.kt:119-166`; `DayAutoGenerator.kt:41-219`。

## Shared UI projections

- Home、Timeline、手工 AddMeal 预览、WeekPlan、Search 的餐卡入口均使用 `DayMealCardView`；代码入参是 `DayMealCardData`，未发现各页面另造同名 persisted model。
- `MealDateCalendarDialog` 是无存储组件；它接收调用方给出的 `initialDate`、`selectionMode`、日期集合/回调，不自行读取 Repository。
