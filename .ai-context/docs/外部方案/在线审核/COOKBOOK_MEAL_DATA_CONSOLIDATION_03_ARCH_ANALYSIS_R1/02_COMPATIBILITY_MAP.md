# COMPATIBILITY MAP

```text
数据库 meal_record / 关联查询
        |
        v
MealRecordRepository
  ├─ MealDayContent 读取契约
  │    ├─ Home: observeUpcomingMealDayContents
  │    └─ Timeline: loadMealDayContentsByDates
  └─ MealDayCardProjector
       └─ DayMealCardData
            ├─ DayMealCardView
            ├─ Search
            ├─ WeekPlan
            ├─ Nutrition / DietReport
            └─ Home 派生聚合
```

## API → Feature → 替代路径 → 风险

| API | Feature/内部使用 | 可替代路径 | 迁移风险 |
|---|---|---|---|
| `loadMealDayContentsByDates` | Timeline | 保持 `MealDayContent`，在 VM 投影 | 低；现有调用已按该边界实现 |
| `observeUpcomingMealDayContents` | Home | 保持中性内容，Home 投影 | 低；Home 已验证该方向，需保留日期窗口语义 |
| `loadTimelineCardsByDates` | `searchMealCards` 内部 | `loadMealDayContentsByDates(...).map(project)` | 中；Search 对 Card 字段和空结果/顺序语义有隐式依赖 |
| `observeDayMealCard` | 当前无调用方 | `observe...MealDayContents` + projector，或删除前先确认外部消费者 | 中；公共 Repository API 可能存在未被本仓库检索到的消费者 |

## 兼容边界

1. `MealDayContent` 只表达日期和餐次内容，不携带 `PAST/TODAY/FUTURE`。
2. `DayMealCardData.temporalRole` 是唯一日期角色来源；不得让 Feature 自行拼接多个布尔状态。
3. 任何 Card API 的废弃都必须先确认所有调用方已切换，并保留等价投影测试。
4. API 迁移不应改变日期集合来源、排序、空天是否返回、分页 offset 或监听触发条件。
