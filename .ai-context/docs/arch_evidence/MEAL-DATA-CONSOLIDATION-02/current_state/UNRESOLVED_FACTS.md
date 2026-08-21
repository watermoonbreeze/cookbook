# Unresolved facts

1. 未发现独立 MealPlan persistence；但 WeekPlan 的产品术语与 future temporal role 的最终关系仍需 ARCH 明确。
2. `DayMealCardData` 是否应仅表示 UI card，还是允许承载营养报告所需 `eatenRatio`/主料名，源码无法裁决。
3. Home `todayCards`、`nutritionWall`、`yearAverages` 与 WeekPlan/DietReport 的营养派生是否应共享新的领域服务，当前没有目标契约。
4. Timeline 的 `observeTimelineDates`（有记录日）与 `observeTimelineWindow`（窗口内含空日）是否应统一成一个 projection API，属于架构决策。
5. Search 目前按日期重新装配完整卡片；是否需要轻量 summary 结果模型，源码没有性能或交互验收依据。
6. `NutritionTableViewModel` 与餐食营养报告同属 Nutrition 语义，但当前数据入口不同；不能仅按包名推断应合并。
7. 当前证据未包含运行时性能、真实数据规模、分页上限和 UI 交互验收，因此不推导拆分收益或必要性。

