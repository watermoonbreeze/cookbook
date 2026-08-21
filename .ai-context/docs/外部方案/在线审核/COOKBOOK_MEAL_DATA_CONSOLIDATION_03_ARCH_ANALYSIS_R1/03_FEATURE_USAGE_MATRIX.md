# FEATURE USAGE MATRIX

| Feature | 读取入口 | 主要模型 | 当前状态 | 关键依赖/备注 |
|---|---|---|---|---|
| Home | `observeUpcomingMealDayContents`；另有 `observeTimelineWindow` 今日营养链 | `MealDayContent` → `DayMealCardData` | 已使用稳定中性读取 | `HomeViewModel.kt:214-218` 显式投影；`todayCards` 走窗口观察链 |
| Timeline | `loadMealDayContentsByDates` | VM 状态为 `DayMealCardData` | 已使用稳定中性读取 | `TimelineViewModel.kt:213` 后显式投影；日期集合由真实记录日期驱动 |
| Search | `searchMealCards` → `loadTimelineCardsByDates` | `DayMealCardData` | 仍依赖 Card 结果 | `SearchViewModel.kt:74,97`；迁移必须保留分页、日期去重和展示字段 |
| WeekPlan | `observeTimelineWindow` | `DayMealCardData` | 依赖 Card 投影 | `WeekPlanViewModel.kt:54-66`；需要连续自然日窗口和空天语义 |
| Nutrition | Home 的 `todayCards` / `DayMealCardData` 聚合 | `DayMealCardData` | 依赖投影后的卡片内容 | `DietReport.kt:61-80`；不得把营养状态写回 Meal Projection |
| 共享卡片 UI | 上游各 Feature | `DayMealCardData` | 直接展示契约 | `DayMealCardView.kt:40`；仍需 Card 兼容层 |

## 类型结论

- 当前仓库中 `MealDayContent` 的直接 Feature 读取方是 Home、Timeline；`AddDayFoodScreen` 仅用于构造预览数据，不是 Repository 读取方。
- `DayMealCardData` 的消费者明显多于两个旧 API 的直接调用方，因此“统一读取 API”不能等价为“立刻统一 Feature 状态模型”。
- `DayMealCardData` 仍是跨 Feature 的展示/聚合契约；短期只能把 `MealDayContent` 作为读取层稳定边界，保留投影层。
