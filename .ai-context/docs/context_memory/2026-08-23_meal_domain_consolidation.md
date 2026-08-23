# Meal Domain Consolidation 执行快照

- 任务：`COOKBOOK_MEAL_DOMAIN_CONSOLIDATION_EXECUTION`
- 角色：CODER / IMPLEMENTER；执行后交 ARCH REVIEW
- 级别：深度；范围为 shared Domain、边界 Contract、测试与 Evidence
- 冻结约束：Meal 为 Aggregate Root；MealId 为唯一 Identity；Occurrence 与 Meal 分离；Lifecycle 属于 Domain；Projection 非事实源；AI 仅 Suggestion/Context；Migration 使用 Adapter
- 禁止：schema、旧 Meal 模型、Repository 核心行为、推荐算法、用户流程与第二套 Truth
- 当前状态：Execution Package 已读取；Blueprint 正文未随包提供，仅按用户冻结约束与执行包实施
- 预期验证：shared 单元测试、架构/边界测试、schema diff 为空、限定文件 diff

