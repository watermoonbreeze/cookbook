# RECOMMENDATION

## ARCH 输入结论

### 候选 A：保留兼容 API + Deprecated Boundary（推荐，当前批次可接受）

- 保留 `loadTimelineCardsByDates`、`observeDayMealCard` 符号及行为。
- 将新开发/迁移入口限定为 `MealDayContent` 读取 API。
- 通过 `MealDayCardProjector` 生成 `DayMealCardData`；不让 Repository 新增 Feature 状态。
- 下一批先做调用方确认、等价测试和 deprecation 文档，再决定何时标记 `@Deprecated`。

理由：当前新链已在 Home/Timeline 落地，而 Search/WeekPlan/Nutrition/共享 UI 仍消费 Card；A 能缩小兼容风险，不改变产品行为。

### 候选 B：Feature 逐步迁移到 `MealDayContent`

可作为 A 之后的实施路线，但不能在本批直接执行。适用顺序建议：

1. 先迁移 Search 的内部构造，保持对外 `searchMealCards` 返回 Card。
2. 再评估 WeekPlan/Nutrition 是否需要中性内容，避免为页面展示重复投影。
3. 最后处理无调用方旧 API，并以全仓/仓外消费者确认作为删除门槛。

### 候选 C：Repository 全部统一成单一 Card 或单一 Content

不推荐。单一 Card 会把时间角色重新压入读取层；单一 Content 则会迫使多个 UI/聚合方重复投影，且无法自然覆盖带 revision token 的窗口观察链。

## ARCH 待裁决项

- 是否允许下一批给旧 API 加 `@Deprecated`；
- “无仓内调用方”是否足以满足删除门槛，还是必须取得仓外消费者确认；
- `searchMealCards` 是否继续作为 Card 结果 API，还是新增 Content 版本并延后 UI 迁移。

## 明确禁止

本证据包不授权 Repository 重构、数据库/schema 修改、API 删除/迁移、用户行为调整或新增领域实体。
