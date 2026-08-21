# MEAL-DATA-CONSOLIDATION-03 · ARCH 分析证据包

- 输入包：`COOKBOOK_MEAL_DATA_CONSOLIDATION_03_ARCH_ANALYSIS_R1.zip`
- 阶段：`ARCH ANALYSIS / EVIDENCE ONLY`
- 状态：`EVIDENCE_READY / PENDING ARCH REVIEW`
- TURN：`REVIEW`
- Holder：`ARCH`
- 代码改动：无
- 数据库/API迁移：无
- 用户行为变化：无

本目录只记录当前代码事实、兼容边界和候选演进方案，不构成最终架构裁决。

## 证据范围

- 读取契约：`shared/.../MealRecordRepository.kt`、`MealDayModels.kt`、`MealDayCardProjector.kt`
- Feature 调用方：Home、Timeline、Search、WeekPlan、Nutrition 及共享卡片组件
- 验证方式：源码符号检索、调用链走查、模型/投影边界走查
- 本批未运行构建：没有产品代码变更；后续 CODE 批次仍需按最终 ARCH 裁决补测试与构建

## 文档

1. [API inventory](01_API_INVENTORY.md)
2. [Compatibility map](02_COMPATIBILITY_MAP.md)
3. [Feature usage matrix](03_FEATURE_USAGE_MATRIX.md)
4. [Migration risk](04_MIGRATION_RISK.md)
5. [Recommendation](05_RECOMMENDATION.md)
6. [Multi-role review](06_MULTI_ROLE_REVIEW.md)
