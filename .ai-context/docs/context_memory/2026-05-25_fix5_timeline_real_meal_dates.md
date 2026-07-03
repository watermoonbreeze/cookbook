# 2026-05-25 修复5：食历真实餐食日期分页

## 用户需求
- 食历只按 `meal_record` 中真实存在餐食记录的日期展示，不能显示空日期。
- 顺序按时间正序。
- 默认定位：今天有餐食则定位今天；否则定位未来最近餐食日期；否则定位最大餐食日期。
- 历史和计划分页每次加载 7 个有餐食日期。
- 顶部日期范围只显示全量最早/最新餐食日期，控件放在标题栏右侧。
- 点击日期范围打开日历，默认当前月，有餐食日期显示小圆点。

## 关键改动
- `Cookbook.sq`
  - 新增 `selectTimelineDatesAsc`：按正序读取 `meal_record` 的 distinct date。
  - 新增 `selectMealRecordsByDates`：按日期集合批量读取 meal_record。
- `MealRecordRepository`
  - 新增 `observeTimelineDates()`。
  - 新增 `loadTimelineCardsByDates()`，只为传入日期组装卡片。
- `TimelineViewModel`
  - 改为 `allDates + loadedStartIndex/loadedEndIndex` 的真实日期索引分页。
  - `rangeMin/rangeMax` 来自全量最早/最晚餐食日期。
  - `mealDates` 提供给日历小圆点。
  - `jumpToDate()` 支持日历点选后加载并定位。
- `FoodTimelineScreen`
  - 日期范围移入 `TopAppBar.actions`。
  - 新增自定义 `TimelineCalendarDialog`，支持月份切换和餐食小圆点。

## 验证
- `./gradlew :androidApp:compileDebugKotlin`：通过。
- `./gradlew :androidApp:assembleDebug`：通过。
- `./gradlew :shared:testDebugUnitTest`：通过，当前无测试源。

## 注意
- 不要再用 `observeTimelineWindow(start, end)` 作为食历主列表数据源；它会生成连续自然日空卡片。
- 未来如果要做真正无限分页，也应基于有记录日期索引或 SQL limit/offset，而不是自然日窗口。
