# Phase 3 Projection Migration

- 任务：COOKBOOK_MEAL_ARCHITECTURE_EVOLUTION_PHASE3_PROJECTION_MIGRATION
- 角色：CODER / IMPLEMENTER；范围仅为读取侧 Projection Migration。
- 基线：`54db71b9d92e4490cf1e79ca9ba95bc320381f9e`。
- 已知状态：`MealDayContent` 与 `MealDayCardProjector` 已存在；Home、Timeline 仍直接依赖 `MealRecordRepository` 读取。
- 本轮决策：新增 `MealProjectionRepository` 只读门面；不修改 schema、Domain truth、Legacy Model、AI、Recipe/Nutrition 或写入流程。
- 预计受影响：Home 计划/营养墙/今日卡、Timeline 日期与分页、Search 餐食历史检索、shared DI、Projection 测试、Evidence、`BLUEPRINT_STATE.md`。
- 风险：当前工作树已有用户历史文档删除和未跟踪外部执行包；提交仅暂存本 Phase 文件。
- 待验证：shared 单测、Android 单测/Debug 编译、schema diff、剩余 legacy read 清单、Evidence 与状态回写。
