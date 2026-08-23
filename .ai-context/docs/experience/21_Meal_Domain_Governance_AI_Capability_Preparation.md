# Meal Domain Governance 与 AI Capability Preparation

## 本批结论（2026-08-23）

- 五类 Meal Flow（AI 推荐、食物搜索、库存选择、新建菜品、编辑餐食）共用 `SAVE_STATE → RESTORE_STATE → MERGE_RESULT`，并额外校验顺序；库存选择不因此伪造导航事实。
- `TraceDiagnostic` 输出 `FLOW_PASS`、`STATE_RESTORE_FAILURE`、`MERGE_FAILURE` 等可机器判断的分类；仍只消费结构化事件和代码标识。
- 架构质量门禁覆盖五类流程、三段状态合同、诊断失败分类、双 AI Recommend 入口、推荐能力准备模型及真机清单存在性。
- `RecommendationTrace`、`RecommendationReason`、`RecommendationFeedback` 只提供能力准备数据结构，不修改推荐算法、数据库或业务流程。

## 关键路径

- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/MealFlowStateContract.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/TraceDiagnostic.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/platform/RecommendationCapability.kt`
- `.ai-context/tools/architecture_quality_check.py`

## 验收证据

- `python .ai-context/tools/architecture_quality_check.py`：应为 `ARCHITECTURE_QUALITY: PASS`。
- `python -m unittest .ai-context/tools/test_architecture_quality_check.py`：静态门禁回归通过。
- `scripts\\build-cli.bat :shared:testDebugUnitTest`：覆盖状态合同、诊断分类和能力准备模型。
- 本批无 UI 真机步骤；静态证据不替代既有真机清单中的待验证项。

## 红线

- 不把 Navigation 成功当作状态恢复成功。
- 不在能力准备模型中持久化用户输入、菜名或推荐全文。
- 不以静态门禁或单测将真机验证项标记为通过。
