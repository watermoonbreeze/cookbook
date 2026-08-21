# MULTI ROLE REVIEW

以下观点均基于本包源码证据，作为 ARCH 裁决输入；不替代最终 ARCH 决定。

| 角色 | 观点 | 主要风险/建议 |
|---|---|---|
| Algorithm Engineer | `MealDayContent` 与日期角色解耦是正确边界；投影必须是唯一角色计算点 | 防止营养/统计聚合误把 `isPlanState` 当数据库事实；保留 reference date 可重复计算 |
| Google Quality Engineer | 当前 API 迁移最容易漏的是分页、空天、Flow 触发和关联表刷新 | 先补 API 等价测试与 revision-token 回归，再允许 Deprecated/删除 |
| Apple Engineer | 中性共享模型更适合跨平台；平台层应消费稳定领域读取结果 | 不把 Android 卡片展示字段倒灌到 shared 读取契约；保持 iOS 可复用的无平台语义 |
| Apple UX Engineer | 用户可见的日期角色、今天/未来排序和空状态不可因 API 重组改变 | 迁移验收应逐项比较日期顺序、空天占位、今天标识和刷新时机 |
| Google UI Engineer | `DayMealCardData` 仍是现有 Compose 组件的直接输入 | 不让各页面临时自行构造 Card；统一复用 `MealDayCardProjector` |
| UI Engineer | Search、WeekPlan、Nutrition 的需求并不等同于 Home/Timeline | 不能用“读取 API 统一”替代 Feature 状态统一；每个调用方需独立确认 |
| ARCH | 证据支持“Content 读取层 + Card 投影层”双层边界，尚不支持删除兼容 API | 采用候选 A；下一批再按调用方和等价测试分批迁移 |

## 综合裁决输入

推荐 `A 保留兼容 API + Deprecated Boundary`，状态为 `PENDING ARCH REVIEW`。本批没有代码改动，也没有执行迁移。
