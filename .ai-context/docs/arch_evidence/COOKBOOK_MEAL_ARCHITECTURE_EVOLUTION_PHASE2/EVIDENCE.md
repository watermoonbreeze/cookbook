# COOKBOOK Meal Architecture Evolution — Phase 2 Evidence

## Scope

- Implementation commits: `de1b5d14`；验收修复 `b94ec622`（AF-B2-01/02）
- Current phase: Phase 2 — UseCase Migration
- Authorized flows: create, save, query
- Phase 3 Projection Migration: not executed
- Phase 4 AI Alignment: not executed

## Implementation evidence

1. `MealRecordUseCase` receives `MealRecordDraft` from the UI-facing flow.
2. Save results are read back from persisted `meal_record`/`meal_type` facts and adapted as canonical `domain.mealrecording.MealRecord`；不以草稿备注伪造餐次名或创建时间。
3. AddMeal 的餐食日期读取经 `MealProjectionRepository`；餐次字典、编辑回读与既有写入兼容 API 保持边界内。
4. Existing edit-page `MealRecordEditData` is retained as a migration-period read projection; no legacy model was deleted.
5. `MealRecordUseCaseTest` 覆盖 create/saveDay 的持久化回读、餐次名与备注分离、创建时间、混日期拒绝；`MealProjectionRepositoryTest` 覆盖日期流与兼容读取等价。

## Verification

| Evidence | Result |
|---|---|
| `:shared:testDebugUnitTest --tests ...MealRecordUseCaseTest` | PASS |
| `:shared:testDebugUnitTest` | PASS, 117 tests, 0 failures |
| `:androidApp:testDebugUnitTest` | PASS, 15 tests, 0 failures |
| `:androidApp:assembleDebug` | PASS |
| Database schema diff | None introduced |
| Real-device verification | Not executed; remains outside this shared/domain batch |
| `:shared:testDebugUnitTest --rerun-tasks`（MealRecordUseCaseTest 3/3、MealProjectionRepositoryTest 1/1） | PASS |
| `:shared:compileDebugKotlinAndroid --rerun-tasks` | PASS |
| `:androidApp:assembleDebug --rerun-tasks` | PASS |

## Boundary check

- `MealPlan != MealRecord`: preserved.
- `AI Suggestion != Domain Truth`: unchanged.
- `Dish != MealRecord`: preserved; only dish IDs cross the save command.
- `Legacy Storage != Domain Model`: preserved through the existing repository and adapter boundary.

## Blueprint conflict

No conflict found between the Phase 2 package and the current code for the authorized create/save/query migration. The package baseline commits predate the current Phase 1 implementation, so the current checked-out Phase 1 code was used as the implementation baseline. Sol 定向复审已关闭 AF-B2-01/02；本批不执行 Phase 4。
