# COOKBOOK_MEAL_DOMAIN_CONSOLIDATION_EXECUTION Evidence

> 2026-08-26 Sol 架构裁决：`ARCH_REJECTED / SUPERSEDED`。本 Evidence 仅保留历史可追溯性；不得据此将旧单一 `Meal` 模型接入生产。现行真相为 MealPlanning / MealRecording / FoodKnowledge 双边界，旧包留待 Phase 6 Legacy Retirement Evaluation。

## Scope

- Execution Package：`COOKBOOK_MEAL_DOMAIN_CONSOLIDATION_EXECUTION_PACKAGE.zip`
- Role：CODER / IMPLEMENTER
- Previous accepted commit：`f3a6fec9d8e67d0cf66351b1ba1949c0bcb0bf67`
- Implementation commit：`3ba8ea45`
- Contract：Meal 为 Aggregate Root；MealId 为唯一 Identity；Occurrence 分离；Lifecycle 属于 Domain；Projection 非事实源；AI 仅 Suggestion/Context；Migration 使用 Adapter。

## Code Evidence

- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/meal/MealDomain.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/meal/MealLifecycle.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/meal/MealBoundaries.kt`
- `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/domain/meal/MealDomainContractTest.kt`
- 未修改数据库 schema、旧 Meal 模型、Repository 核心行为、推荐算法或用户流程。

## Test Evidence

- `scripts\build-cli.bat :shared:testDebugUnitTest --tests com.sxdbsm.cookbook.domain.meal.MealDomainContractTest`：PASS
- `scripts\build-cli.bat :shared:testDebugUnitTest`：PASS
- `python .ai-context\tools\architecture_quality_check.py --root .`：`ARCHITECTURE_QUALITY: PASS`
- schema diff：空

## Boundary Evidence

- Identity：`MealId` 是 Meal 与 MealOccurrence 的唯一关联身份。
- Lifecycle：`MealLifecycleContract` 集中定义允许迁移，非法迁移抛出异常。
- Projection：`MealProjection` 由 Meal 只读投影生成，不提供事实写入口。
- AI：`MealSuggestion` 只承载 suggestion/context，未提供转为 Meal Truth 的接口。
- Migration：`LegacyMealAdapter` 仅定义适配边界，未连接或改变 Repository。

## Handoff

- 状态：`CODE_COMPLETE`
- TURN：`REVIEW`
- ARCH 复核前不进入下一 Phase。
