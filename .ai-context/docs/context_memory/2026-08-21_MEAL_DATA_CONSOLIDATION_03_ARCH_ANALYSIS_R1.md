# MEAL-DATA-CONSOLIDATION-03 ARCH 分析快照

- 输入：`.ai-context/docs/外部方案/在线审核/COOKBOOK_MEAL_DATA_CONSOLIDATION_03_ARCH_ANALYSIS_R1.zip`
- 阶段：`ARCH ANALYSIS / EVIDENCE ONLY`
- 状态：`EVIDENCE_READY / PENDING ARCH REVIEW`
- TURN：`REVIEW`；Holder：`ARCH`
- 代码、Repository、数据库、API 迁移、用户行为：均未修改

## 关键事实

- `MealDayContent` 已是 Home、Timeline 的稳定读取边界。
- `MealDayCardProjector` 是 `MealDayContent → DayMealCardData` 的唯一投影边界。
- Search 通过 `searchMealCards` 间接复用 `loadTimelineCardsByDates`。
- `DayMealCardData` 仍被 Search、WeekPlan、Nutrition、共享卡片 UI 使用。
- 旧 `observeDayMealCard` 当前无仓内调用方，但删除前仍需确认仓外消费者。
- `observeTimelineWindow` 的关联表 revision token 是生命周期回归重点，不能因 API 收敛丢失。

## ARCH 输入

推荐候选 A：保留兼容 API，后续在确认调用方和等价测试后再加 Deprecated Boundary；不在本批迁移或删除。

证据目录：`.ai-context/docs/外部方案/在线审核/COOKBOOK_MEAL_DATA_CONSOLIDATION_03_ARCH_ANALYSIS_R1/`
