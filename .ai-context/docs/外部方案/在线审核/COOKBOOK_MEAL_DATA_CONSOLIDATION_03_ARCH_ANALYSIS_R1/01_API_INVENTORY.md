# API INVENTORY

## 稳定读取契约

| API | 当前返回 | 直接调用方 | 生命周期/职责 |
|---|---|---|---|
| `loadMealDayContentsByDates(dates)` | `List<MealDayContent>` | `TimelineViewModel.refreshLoadedPages` | 一次性按指定日期读取中性日内容；调用方显式通过 `MealDayCardProjector` 投影 |
| `observeUpcomingMealDayContents(referenceDate, futureDayLimit)` | `Flow<List<MealDayContent>>` | `HomeViewModel.observePlan` | 监听数据库日期变化，返回今天及未来日期的中性内容；时间角色不在 Repository 内固化 |

证据：`MealRecordRepository.kt:101-123, 172-177`；调用方分别位于 `TimelineViewModel.kt:213`、`HomeViewModel.kt:214-218`。

## 兼容/旧边界

| API | 当前返回 | 直接调用方 | 事实 |
|---|---|---|---|
| `loadTimelineCardsByDates(dates)` | `List<DayMealCardData>` | 无 Feature 直接调用；被 `searchMealCards` 内部调用 | 先读取 `MealDayContent`，再用当天日期投影为卡片 |
| `observeDayMealCard(date)` | `Flow<DayMealCardData>` | 当前无调用方 | 监听单日记录后直接投影；保留旧的 Card 返回语义 |

证据：`MealRecordRepository.kt:69-77, 163-177, 184-191`；全仓 Kotlin 符号检索未发现上述两个 API 的其他调用。

## 相关但不应混淆的 API

- `observeTodayPlusFuture`、`observeTimelineCards`、`loadDayMealCard`、`observeTimelineWindow` 是现存其他读取路径；它们不等同于本包列出的四个演进目标。
- `searchMealCards` 仍以 `DayMealCardData` 为结果契约，Search 的迁移不能只删除 `loadTimelineCardsByDates`。
- `MealDayCardProjector` 是当前唯一的 `MealDayContent → DayMealCardData` 投影边界，位于 `shared/.../domain/projection/MealDayCardProjector.kt`。
