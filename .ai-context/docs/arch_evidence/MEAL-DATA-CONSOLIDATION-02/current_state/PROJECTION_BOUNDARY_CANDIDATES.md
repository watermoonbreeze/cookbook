# Projection boundary candidates

本文件只记录由源码事实暴露出的候选边界，不作目标架构决定。

| 候选 | 事实依据 | 尚不能决定的部分 |
|---|---|---|
| 保持 `MealDayContent → DayMealCardProjector` | Home/Timeline 已显式走内容再投影；Temporal Role 有单一来源 | 是否应把 Search/WeekPlan 也统一到同一 service |
| `MealProjectionService` 候选 | Repository 暴露多组相似的 content/card 读取入口，Search 复用 Timeline card loader | service 是否应拥有日期窗口、营养派生或仅负责投影 |
| `HomeProjection` / `TimelineProjection` / `PlanProjection` 候选 | Home 有营养/年度/今日派生；Timeline 有分页日期状态；WeekPlan 有周营养线 | 这些是否属于 projection 边界还是 VM 编排，源码无法裁决 |
| Card model 拆分候选 | `DayMealCardData` 同时携带 Temporal Role、餐内容、展示所需日期语义，多个 feature 直接持有 | 是否“过多 Feature 语义”需要 ARCH 定义不变量和目标消费者 |

禁止由本发现阶段据此创建新 Projection、拆 Repository 或设计最终架构。

