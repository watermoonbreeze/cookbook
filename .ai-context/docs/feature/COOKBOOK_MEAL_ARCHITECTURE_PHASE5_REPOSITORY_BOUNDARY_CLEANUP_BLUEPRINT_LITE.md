# Phase 5 Repository Boundary Cleanup — Reality Verification + BLUEPRINT-LITE

状态：**BLUEPRINT_READY / TURN=CODE**
冻结日期：2026-08-26
事实基线：`852a4f3b`
前置验收：Phase 4 `6a538de3` = **ARCH_ACCEPTED**；真机仍为 `PENDING_DEVICE_VERIFICATION`，不阻断本批代码与单测。

## 1. Reality Verification

### 1.1 已核实事实

- `MealRecordRepository` 目前同时提供持久化、查询、兼容卡片投影与餐食写入 API，尚未收敛成纯 `load/save/query` 外观。
- `saveDayMeals()` 的单日 SQL transaction 内混合了：整日替换、菜品关联顺序、旧 `eaten_ratio` 保留/统一餐次继承、按基线日期计算 preference 增量。这里既有存储原子性，也有既有业务兼容语义，不能一次重写。
- `HomeViewModel`、`TimelineViewModel`、`WeekPlanViewModel` 重复编排“快照成功才删除；撤销时 `bumpPreference=false` 还原”，并直接调用 Repository mutation。
- `HomeViewModel` 还直接调用单菜/整餐 `eaten_ratio` mutation。
- `DayAutoGenerator` 的主保存已走 `MealRecordUseCase.saveDay()`，但冲突/merge 查询与保存后的 `eaten_ratio` 回填仍直接依赖 `MealRecordRepository`。
- `MultiDayRecorder` 仍以 Repository 做已有餐食查询并构造 `DayAutoGenerator`；`AiPlanViewModel`、`AddMealViewModel` 等剩余直接 Repository 使用以查询或迁移兼容为主。
- `SyncRepository` 的 `mealRepo.save()` 位于数据同步落库边界；本批不把同步导入伪装成用户业务 UseCase，也不改变其合并语义。
- 当前 schema 仍是兼容 `meal_record` 存储；不存在独立 `meal_plan` 表。`MealPlan != MealRecord`、`MealRecord` 保存后 canonical 回读已由 B1–B3/Phase 4 固定。

### 1.2 分类与本批裁决

| 责任 | 当前事实 | P5-A 裁决 |
|---|---|---|
| SQL transaction、记录/关联表写入、查询组装 | Storage | 留在 Repository |
| `snapshot -> delete -> undo restore(bumpPreference=false)` | UseCase orchestration，且 UI 重复 | 本批提取到 `MealRecordUseCase` |
| 单菜/整餐食用比例写入 | 用户业务 mutation | UI/AutoGenerator 改经 `MealRecordUseCase`；Repository SQL API暂保留 |
| AI 自动记餐 merge 查询、保存后比例回填 | Domain/UseCase orchestration | `DayAutoGenerator` 只依赖 `MealRecordUseCase` |
| preference 基线、ratio 继承、整日替换 | 混合且回归风险高 | 只补回归证据，不在 P5-A 搬迁 |
| 兼容 Card/read API、Sync 导入 | 兼容/独立边界 | P5-A 保留；P5-B 前重新 Reality Review |

## 2. P5-A 目标

本批只收敛 **MealRecord mutation 边界**：UI 和 `DayAutoGenerator` 不再直接决定 Repository 写参数与撤销策略；`MealRecordUseCase` 成为这些 mutation 的唯一业务入口。Repository 的 SQL、transaction、查询结果和兼容 API 不改。

建议冻结 API 语义：

- `deleteDayWithUndo(date)`：先取非空快照，成功删除后返回不透明 restore token；快照为空时零写。
- `restoreDeletedDay(token)`：恢复原日期/餐次/时间/备注/菜品顺序，固定不增加 preference。
- `updateDishEatenRatio(...)`、`updateMealEatenRatio(...)`：经 UseCase 调用现有 Repository mutation，保持 `[0,1]` clamp 语义。
- `queryDayForEdit()` 继续作为迁移期 query seam，供 `DayAutoGenerator` merge/回填定位；不得向 UI 新增持久化模型泄漏。

## 3. 精确 allowlist

### 3.1 生产代码

1. `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/usecase/mealrecording/MealRecordUseCase.kt`
2. `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/autogen/DayAutoGenerator.kt`
3. `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/MultiDayRecorder.kt`（仅适配 `DayAutoGenerator` 构造；查询兼容不扩张）
4. `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/home/HomeViewModel.kt`
5. `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/timeline/TimelineViewModel.kt`
6. `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/weekplan/WeekPlanViewModel.kt`
7. `androidApp/src/main/java/com/sxdbsm/cookbook/android/di/AndroidModule.kt`

### 3.2 测试与收口证据

8. `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/data/repository/MealRecordUseCaseTest.kt`
9. `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/domain/autogen/DayAutoGeneratorMealBoundaryTest.kt`
10. `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/data/repository/MealRecordRepositoryTest.kt`（只允许补既有语义回归断言）
11. `androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/home/HomeMealMutationBoundaryTest.kt`（可新建）
12. `androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/timeline/TimelineMealMutationBoundaryTest.kt`（可新建）
13. `androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/weekplan/WeekPlanMealMutationBoundaryTest.kt`（可新建）
14. `.ai-context/docs/arch_evidence/COOKBOOK_MEAL_ARCHITECTURE_EVOLUTION_PHASE5/EVIDENCE.md`（完成时新建）
15. `.ai-context/docs/context_memory/BLUEPRINT_STATE.md`（只更新本批状态/commit/evidence）
16. 唯一最新 `.ai-context/docs/真机验证/真机待验证清单_<yyyyMMddHHmm>.md`（代码+单测通过后按项目规则原位更新并改名，不复制第二份）

allowlist 外文件默认禁止修改；确有编译必需项时先登记 `Q-P5-A-xx`，由 ARCH 扩围后再动。

## 4. 禁止项

- 禁止修改 `.sq`、schema、migration、seed 或数据格式。
- 禁止重写/拆分 `MealRecordRepository`，禁止删除或重命名兼容 API。
- 禁止改变 `saveDayMeals()` 的 transaction、整日替换、preference、`eaten_ratio` 保留/继承语义。
- 禁止修改 AI 算法、prompt、preview/确认/冲突文案与计划生成策略。
- 禁止把 `MealPlan` 与 `MealRecord` 合并，禁止新增独立计划表。
- 禁止改变 Sync 合并、备份恢复、undo 产品行为；禁止执行 Phase 6 Legacy Retirement。
- 禁止声称跨日期、跨菜品创建与餐食保存具备整体原子性。
- 禁止无关清理、格式化、依赖升级、日志/可观测性扩展。

## 5. 不变量

- **INV-P5-A-01**：三处 UI 的 MealRecord mutation 不再直接调用 `MealRecordRepository`；读取继续走既有 Projection/兼容 query。
- **INV-P5-A-02**：`DayAutoGenerator` 不再 import/持有 `MealRecordRepository`；merge、canonical 保存、比例回填均经 `MealRecordUseCase`。
- **INV-P5-A-03**：快照失败或为空时不得删除；删除成功后才可向 UI 暴露 undo。
- **INV-P5-A-04**：undo restore 固定 `bumpPreference=false`，恢复日期、餐次、时间、备注和菜品顺序；不得承诺当前快照未保存的字段。
- **INV-P5-A-05**：单菜与整餐 ratio 继续 clamp 到 `[0,1]`，不得改 sort/status/preference。
- **INV-P5-A-06**：正常新增/编辑/移动/撤销的 preference 增量口径与 P5 前一致。
- **INV-P5-A-07**：保存后的 Domain `MealRecord` 必须由 Repository 回读，不得从 draft 猜 id、mealName、createdAt。
- **INV-P5-A-08**：Phase 4 的计划冲突精确重检、确认前零写、`PLANNED` 生命周期与连点 guard 不退化。
- **INV-P5-A-09**：每个 `saveDay` 只有现有单日 transaction；多日循环不得虚称原子。
- **INV-P5-A-10**：UI 成功/失败提示与异常可观测行为保持等价；不得把失败吞成成功。
- **INV-P5-A-11**：`SyncRepository` 直接 storage 写、剩余兼容 query 是显式暂留项，不因本批被偷偷删除或扩张。
- **INV-P5-A-12**：schema、AI 输出、非 AI 记录流程与 Projection 结果无行为变化。

## 6. 实施步骤

1. 在 `MealRecordUseCase` 引入不透明删除恢复 token，并封装 delete-with-undo、restore、两类 ratio mutation；Repository API 不动。
2. 先补 shared UseCase 单测，锁定空快照零写、成功删除/恢复、preference 不增与 ratio clamp。
3. 将 `DayAutoGenerator` 的 query/ratio mutation 改经 UseCase，移除其 Repository 依赖；仅机械调整 `MultiDayRecorder` 构造。
4. 将 Home/Timeline/WeekPlan mutation 改经 UseCase；WeekPlan 的窗口读取改用已存在的 `MealProjectionRepository`，不得新造 projection。
5. 更新 Koin 构造注入；补三处 UI 边界测试或等价的真实内存 DB 行为证据。
6. 跑完整门禁、生成 Evidence、登记真机项；状态只能先到 `CODE_COMPLETE / TURN=REVIEW`，不得由 CODER 自写 `ARCH_ACCEPTED`。

## 7. 测试矩阵与验收门槛

| ID | 证据 | 必须证明 |
|---|---|---|
| T-P5-A-01 | `MealRecordUseCaseTest` | 空日/快照空返回无 token 且零删除 |
| T-P5-A-02 | 同上 | 非空日 delete 后为空；restore 后日期/餐次/时间/备注/菜品顺序等价 |
| T-P5-A-03 | 同上 + Repository test | restore 不增加 preference；新增/编辑口径不变 |
| T-P5-A-04 | 同上 | 单菜/整餐 ratio 的 `<0`、合法值、`>1` clamp 等价 |
| T-P5-A-05 | `DayAutoGeneratorMealBoundaryTest` | preview 零写；commit 经 UseCase；merge 与 ratio 回填结果不变 |
| T-P5-A-06 | 三个 Android boundary test | delete/undo 每次只发起一次；失败不报成功；回调时序不变 |
| T-P5-A-07 | 既有 `MealPlanSaveUseCaseTest` | D1 冲突后新增 D2 时旧确认失效且两日零写；保存仍为 PLANNED |
| T-P5-A-08 | source audit | 三 VM 无 Repository mutation；`DayAutoGenerator` 无 Repository import/字段 |
| T-P5-A-09 | schema diff | `.sq`/migration 零 diff |

必跑命令：

```text
scripts\build-cli.bat :shared:testDebugUnitTest --rerun-tasks
scripts\build-cli.bat :androidApp:testDebugUnitTest --rerun-tasks
scripts\build-cli.bat :shared:compileDebugKotlinAndroid --rerun-tasks
scripts\build-cli.bat :androidApp:assembleDebug --rerun-tasks
scripts\build-cli.bat :androidApp:assembleRelease --rerun-tasks
```

静态零命中门槛：

```text
HomeViewModel / TimelineViewModel / WeekPlanViewModel 中
deleteDayMeals|snapshotDay|saveDayMeals|setEatenRatio|setEatenRatioForMeal = 0

DayAutoGenerator 中
MealRecordRepository|mealRepo = 0
```

任一目标测试失败、强制构建无 Gradle 成功终态、schema 有 diff、allowlist 越界或 INV 破坏，均为阻断；不得用历史 PASS 替代本批结果。

## 8. 真机统一登记项（本批不执行）

代码+单测完成即把以下条目写入唯一最新真机清单，状态保持 `PENDING`；按用户项目级决定，未做真机不阻断后续代码，但绝不能标 `PASS`。

- **DEV-P5-01 删除/撤销**：分别从首页、食历、一周计划删除同一天并撤销；预期仅删除一次、仅恢复一次，餐次/时间/备注/菜品顺序一致；失败判定为误删、重复恢复、空快照仍删除或恢复缺项。
- **DEV-P5-02 preference**：记录撤销前菜品排序/偏好，删除后撤销；预期不额外增加 preference；失败判定为撤销导致“常做”排序抬升。
- **DEV-P5-03 食用比例**：单菜与整餐调整后切页并重启；预期值持久化且营养卡同步；失败判定为越界、丢失、串餐或 preference 被改。
- **DEV-P5-04 AI 快捷记餐**：preview/确认/已有餐食 merge 确认后提交；预期行为与 Phase 4 一致且 ratio 回填正确；失败判定为确认前写入、merge 丢原餐或比例错误。
- **DEV-P5-05 AI 周期计划**：制造已有日期冲突并在确认前新增第二冲突；预期重新提示精确集合、旧确认不覆盖新冲突；失败判定为确认前写入或任一原记录被静默替换。

## 9. AF / Q 与交接

- `AF-P5-A-xx | INV | 代码/测试证据 | 最小修复 | 必补测试 | OPEN/CLOSED`
- `Q-P5-A-xx | 缺失事实 | 影响 | 只读核验方式 | 是否申请扩 allowlist`
- Terra 可直接实施本冻结批；仅纯机械的构造参数/测试夹具可委派 Luna，UseCase API、undo token 与 invariant 相关代码必须由 Terra 决策并自验。
- P5-A 验收后才允许为 P5-B（兼容 read/projection 与 Repository 内 preference/ratio policy 分类提取）重新 Reality Verification；不得顺带进入 Phase 6。
