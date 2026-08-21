# MEAL-DATA-CONSOLIDATION-03 · API 边界与调用关系

## 稳定契约

```text
MealRecordRepository
  -> MealDayContent              (Stable Read Contract)
  -> MealDayCardProjector
  -> DayMealCardData             (Shared Read Projection)
  -> Home / Timeline / WeekPlan / Search / Nutrition 派生
```

- `MealDayContent` 只表达某日中性餐食事实，不携带 TODAY/FUTURE、页面加载或编辑状态。
- `MealDayCardProjector` 是 `MealDayContent -> DayMealCardData` 的唯一时间角色投影边界。
- `DayMealCardData` 是共享读取投影，不是 Feature 状态容器。

## 兼容 API

以下入口暂保留，直到仓内外消费者完成确认并有等价测试支撑：

| API | 当前角色 | 处置 |
|---|---|---|
| `observeDayMealCard` | Compatibility | 不删除；新消费者优先稳定契约 |
| `observeTodayPlusFuture` | Compatibility | 不删除；时间语义仍委托 projector |
| `observeTimelineCards` | Compatibility | 不删除 |
| `loadTimelineCardsByDates` | Compatibility | 与稳定内容读取投影保持等价 |
| `searchMealCards` | Compatibility | 暂不改变 Search 行为 |
| `loadDayMealCard` | Compatibility | 不删除 |
| `observeTimelineWindow` | Shared Read Projection | 保留 revision token 生命周期语义 |

## 本批门禁

- 不删除 API，不修改 SQLDelight schema，不重构 Repository。
- `MealRecordRepositoryTest.compatibilityCardLoaderEqualsStableContentProjection` 覆盖旧/新读取结果等价。
- `MealRecordRepositoryTest.timelineWindowReemitsWhenRelatedRevisionTokenChanges` 覆盖关联表 revision token 的 Flow 生命周期。
- 后续若引入 `@Deprecated` 边界，必须先补消费者清单、迁移窗口与等价测试；本批不提前删除或迁移。

