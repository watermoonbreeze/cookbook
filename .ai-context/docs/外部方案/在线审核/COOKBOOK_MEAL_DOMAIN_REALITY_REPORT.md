# COOKBOOK_MEAL_DOMAIN_REALITY_REPORT

任务：`COOKBOOK_MEAL_DOMAIN_REALITY_VERIFICATION`

调查基线：任务包声明 Base `f3a6fec9d8e67d0cf66351b1ba1949c0bcb0bf67`，当前实现 `41eb1a390138ef6a48c4c74ed80288728415249a`。

本报告只记录当前代码、测试、数据库结构和现有 UI 链路。`FACT` 表示已由仓库证据确认；`HYPOTHESIS` 表示基于事实的架构判断或后续建议。

## 1. Existing Domain Map

### 1.1 当前实际使用的餐食模型（FACT）

- 持久化事实是 `meal_record`：`date`、`meal_type_id`、`meal_time`、`note`、`created_at`、`status`。一条记录代表某天某餐次的餐食记录，见 `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq:258-266`。
- 餐食与菜品通过 `meal_record_dish` 多对多关联，带 `sort_order`、`status`、`eaten_ratio`，见同文件 `271-280`。
- Kotlin 业务读取模型是 `domain.model.MealRecord`，其 `dishes` 是 `DishMini` 列表；写入命令是非持久化的 `DayMealDraft`，见 `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/model/MealRecord.kt:25-34` 和 `data/repository/MealRecordContracts.kt:6-20`。
- 菜品真实模型是 `domain.model.Dish`，包含名称、做法、食材、步骤、来源、餐次标签等；餐食卡片使用轻量 `DishMini`，见 `domain/model/Dish.kt:11-31,70-98`。
- `MealType` 是数据库字典模型，不是独立的 Meal 聚合；其 id/code/name/defaultTime 等来自 `meal_type`，见 `MealRecord.kt:11-17` 与 `Cookbook.sq:234-241`。

### 1.2 新增的 Canonical Meal 契约（FACT）

- `domain.meal.Meal`、`MealId`、`MealOccurrence`、`MealMetadata` 和 `MealSource` 已存在，见 `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/domain/meal/MealDomain.kt:6-46`。
- 该模型声明 `MealLifecycle = DRAFT/PLANNED/RECORDED/ARCHIVED`，并通过 `MealLifecycleContract` 限制迁移，见 `domain/meal/MealLifecycle.kt:3-34`。
- `MealProjection` 只读投影、`MealSuggestion` AI 建议边界、`LegacyMealAdapter` 适配接口已存在，见 `domain/meal/MealBoundaries.kt:3-25`。
- 当前证据只证明这些契约类及其单元测试存在；未发现 `Meal` 被 `MealRecordRepository`、SQLDelight schema 或 Android 餐食 UI 作为实际持久化/读写类型使用（FACT）。

### 1.3 Planning / Recipe / Suggestion / History（FACT）

- 未发现 `meal_plan` 或 `MealPlan` 数据库实体。周期计划使用内存模型 `PeriodPlan/DayPlan/PlannedMeal/PlannedDish`，见 `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/model/WeekPlan.kt:15-70`。
- 周计划 UI 从 `MealRecordRepository.observeTimelineWindow` 读取日期卡片；计划保存最终仍写 `meal_record`，见 `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/weekplan/WeekPlanViewModel.kt:53-70` 和 `domain/autogen/DayAutoGenerator.kt:194-203`。
- 未发现名为 `Recipe` 的持久化模型或 Repository；当前“菜谱”语义由 `Dish` 加食材/步骤承载（FACT）。
- AI 产生的是 `MealSuggestion`、`DayMealJson`、预览对象等临时结果；AI 建议本身不直接成为 Meal Truth，现有边界契约也明确如此，见 `domain/meal/MealBoundaries.kt:16-20`。
- 历史不是独立表或独立 Repository；历史/食历从 `meal_record` 的日期查询投影生成，见 `Cookbook.sq:1444-1511` 与 `MealRecordRepository.kt:140-168`。

## 2. User Flow Map

### 2.1 手动新增餐（FACT）

1. `AddMealViewModel` 维护按日期的多个餐次块；用户选择餐次、时间、备注和 `DishMini`，状态由 UI 草稿持有，见 `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddMealViewModel.kt:254-328`。
2. `save()` 过滤可保存餐次块，转换为 `DayMealDraft`，见同文件 `416-429`。
3. ViewModel 调用 `MealRecordRepository.saveDayMeals()`；Repository 在事务内按日期删除旧记录，再插入每个餐次及其菜品关联，见 `MealRecordRepository.kt:312-367`。
4. 保存成功后 ViewModel 设置 `done=true`，UI 通过全局 Snackbar 并返回目标页面，见 `AddMealViewModel.kt:449-470` 和 `AddDayFoodScreen.kt:195-197`。

### 2.2 编辑 / 移动（FACT）

- 编辑页通过 `loadDayMealsForEdit(date)` 读取当天所有 `MealRecordEditData`；该读取仍来自 `meal_record` 与关联菜品，见 `MealRecordRepository.kt:385-399`。
- 同日编辑采用整日删重插；实现会快照并回填 `eaten_ratio`，并只对新增菜品增加偏好计数，见 `MealRecordRepository.kt:327-363`。
- 修改到空日期时，UI 保留草稿；保存新日期后再删除旧日期，见 `AddMealViewModel.kt:439-447`。已有餐食的目标日期会被拒绝并提示，见 `AddMealViewModel.kt:226-237`。

### 2.3 保存失败（FACT）

- ViewModel 在保存前设置 `saving=true`，Repository 或后续删除异常时进入 catch，记录失败 trace/event，并将 `saving=false`、错误消息写回状态；代码位于 `AddMealViewModel.kt:433-479`。
- `saveDayMeals()` 的写入本身位于 `db.transaction`；因此单日删除旧记录和插入新记录是一个数据库事务（FACT），但移动流程的新日期保存与旧日期删除是两个 Repository 调用（FACT）。
- 现有测试覆盖空列表清空当天、同日编辑、移动/历史读取等 Repository 行为，见 `shared/src/androidUnitTest/kotlin/com/sxdbsm/cookbook/data/repository/MealRecordRepositoryTest.kt:251-284,289-338`。

### 2.4 AI 生成与保存（FACT）

1. AI/规则解析产生多天 `DayMealJson`；流式会话按 segment 聚合，失败段可保留已成功前缀，见 `StreamingMealSessionTest.kt:157-179,267-294`。
2. `MultiDayRecorder.previewAll()` 通过 `DayAutoGenerator.preview()` 生成零写库预览；确认后 `commitPreview()` 调用 `DayAutoGenerator.commit()`，见 `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/ai/meallog/MultiDayRecorder.kt:115-159`。
3. `DayAutoGenerator.commit()` 创建或复用食材/菜品，构造 `DayMealDraft`，按 `MergeMode` 合并已有餐食，然后调用 `mealRepo.saveDayMeals()`；见 `domain/autogen/DayAutoGenerator.kt:125-203,284-317`。
4. 保存后再读取当天记录并回填 AI 解析出的 `eaten_ratio`，见 `DayAutoGenerator.kt:205-213`。
5. AI UI 的 `DONE` 状态才触发离开入口；预览、生成中和错误状态的选择器锁定规则由 `UnifiedAddMealStateTest` 覆盖，见 `androidApp/src/test/java/com/sxdbsm/cookbook/android/ui/addmeal/UnifiedAddMealStateTest.kt:44-75`。

### 2.5 历史查看（FACT）

- `TimelineViewModel` 监听真实存在的餐食日期，按 7 天窗口加载；UI 通过 `MealRecordRepository` 组装日期卡片，见 `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/timeline/TimelineViewModel.kt:59-91`。
- 日期搜索按 `meal_record.date` 或关联 `dish.name` 返回日期，再组装整天餐食，见 `Cookbook.sq:1499-1508`。
- 菜品被软删除后，历史关联查询仍不要求 `dish.status=1`，以保留历史展示，见 `Cookbook.sq:1549-1556`。

## 3. Data Relationship

```text
meal_type (字典)
   │ 1
   └──< meal_record (日期 + 餐次 + 时间 + 备注)
              │ 1
              └──< meal_record_dish >── 1 dish
                                           ├──< dish_ingredient >── ingredient
                                           ├──< dish_step
                                           └──< dish_meal_slot >── meal_type
```

- `meal_record` 是当前 Planning、Recording、History 共用的事实表（FACT）。区别主要由日期和调用场景表达：未来日期可作为计划展示，过去/当天可作为记录或历史展示；schema 没有独立状态列区分计划与实际，只有通用 `status`（FACT）。
- `Dish`/`Ingredient`/营养表是被餐食引用的菜品与食材体系；营养由 `NutritionRepository` 根据菜品食材关联计算/估算，不存在 Meal 专属营养实体（FACT）。
- UI 草稿、AI 预览和 `AiMealInputUiState` 属于内存/Compose/ViewModel 状态；没有发现餐食草稿的数据库缓存表（FACT）。
- Android 图片存在 `LruCache`，但它是图片渲染缓存，不是 Meal/Record 数据缓存，见 `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/StoredImage.kt:134-155,193`（FACT）。

## 4. Domain Conflict

### C1：Canonical Meal 与实际 MealRecord 并存（FACT）

`domain.meal.Meal` 使用字符串身份、Occurrence 和 Domain lifecycle；实际写入使用自增 `meal_record.id`、`meal_record_dish` 和按日整替换。两套模型没有发现 Adapter、Repository 接口或数据库映射的生产调用（FACT）。这造成“规范模型存在，但真实流程仍绕过它”的语义分裂。

### C2：Planning 与 Recording 共用同一事实表（FACT）

未来日期的周计划和当天/过去的实际餐食都落到 `meal_record`；没有 `MealPlan` 实体、计划版本或计划到记录的转换关系（FACT）。因此 `MealLifecycle.PLANNED` 与数据库实际语义不能直接一一对应（HYPOTHESIS：未来引入明确计划生命周期时可能产生冲突）。

### C3：Recipe 语义由 Dish 承载（FACT）

`Dish` 同时包含可复用菜品、食材、步骤、图片、来源和偏好；餐食只引用 `DishMini`。未发现独立 `Recipe` 实体（FACT）。若架构文档把 Recipe 当作独立聚合，当前代码没有对应事实支撑（HYPOTHESIS）。

### C4：生命周期与保存原子性边界不一致（FACT）

单日替换在一个事务内；移动日期则先保存目标日、再删除来源日，跨两个 Repository 操作。Canonical lifecycle 只定义状态迁移，没有覆盖该跨日期操作（FACT）。这可能解释部分“保存失败/状态恢复”问题的复杂性，但具体线上失败原因需运行日志或复现证据确认（HYPOTHESIS）。

### C5：AI 预览、确认结果与手动草稿有两套状态壳（FACT）

AI 使用 `AutoGenPreview/AutoGenResult` 与流式 session 状态，手动新增使用 `AddMealUiState`/`DayMealDraft`；最终写入入口仍汇聚到 `saveDayMeals()`（FACT）。因此问题更可能位于流程状态合并、确认/恢复或跨日写入边界，而不是存在两个数据库记录系统（HYPOTHESIS）。

## 5. Root Cause

- **FACT：真实数据源单一但语义复用。** `meal_record` 同时服务未来计划、当前记录和历史；代码靠日期、调用入口和 UI 文案区分，而数据库没有显式 Planning/Recording 类型。
- **FACT：Canonical Meal 尚未成为生产读写模型。** 其契约测试验证了身份、生命周期、投影和建议边界，但实际餐食保存/历史读取仍使用 `MealRecordRepository` 与 SQLDelight 旧模型。
- **FACT：编辑是“整日替换”而非实体级更新。** `saveDayMeals()` 删除当天旧记录后重插，并额外回填食用比例；这使编辑、移动、撤销、偏好计数和失败恢复共享一条复杂路径。
- **FACT：AI 保存不是独立持久化链路。** AI 在生成层有 preview/commit 和 merge，但 commit 最终复用 `saveDayMeals()`；AI 保存失败应沿该共享链路和前置状态恢复链路排查。
- **HYPOTHESIS：若已知问题表现为保存后页面状态丢失或 AI 返回后餐次块不见，优先怀疑 UI 状态恢复/merge 时机，而非先假设需要新增 Meal 聚合。** 现有代码明确记录 `state.restore`、`state.merge.result`，但本报告没有运行时日志，不能把该推测升级为事实。

## 6. Architecture Recommendation

结论：**HYPOTHESIS：候选 B（MealPlan + MealRecord）最贴近当前事实，但应以现有 `Dish` 作为菜谱事实来源，而不是立即新增 Recipe 模型。**

理由：

1. 现有业务已经同时存在“未来日期安排”和“实际/历史记录”的使用场景，但数据库目前只有 `meal_record`；因此 Planning 与 Recording 的语义分离是当前最明显的缺口（FACT → HYPOTHESIS）。
2. `MealRecordRepository` 已集中承载保存、编辑、历史查询、日期窗口和撤销恢复；它是事实入口，任何后续边界调整都应先以其真实行为和测试为约束（FACT）。
3. 独立 Recipe 的必要性证据不足，因为 `Dish` 已包含可复用菜品所需的食材、步骤和展示字段，并被所有餐食引用（FACT）。直接选择 C 并新增 Recipe 会超出本次事实调查能支持的范围（HYPOTHESIS）。
4. 候选 A 的单一 Meal 生命周期无法解释当前计划/记录/历史共用同一表及跨日期移动的事实，故匹配度较低（HYPOTHESIS）。

本节只是供 ARCH REVIEW 的建议，不是实施方案；本次未修改代码、数据库或 Repository。

## 7. Blueprint Changes

- FACT：本任务包要求先做 Reality Verification；本报告确认当前实现中已经存在 `domain.meal` 契约，但它尚未连接真实 MealRecord 数据链路。
- HYPOTHESIS：任何后续 Blueprint 都应先明确 `meal_record` 的计划/实际语义、日期移动的原子性边界、AI commit 与手动 save 的共同入口，以及 Canonical Meal 是否采用 Adapter 过渡。
- 本次不修改既有 Blueprint，不创建新的架构方案，不新增 Domain Model。

## 8. Confidence

### FACT（高置信）

- SQLDelight 表、字段、关系和查询：`Cookbook.sq`。
- 生产保存/编辑/删除/历史读取：`MealRecordRepository`、`AddMealViewModel`、`TimelineViewModel`、`WeekPlanViewModel`。
- AI preview/commit 最终转为 `DayMealDraft` 并调用 `saveDayMeals()`：`MultiDayRecorder`、`DayAutoGenerator`。
- Canonical Meal 契约及其单测存在，但未发现生产接入：`domain/meal/*` 与 `MealDomainContractTest`。
- 已有测试覆盖整日保存、编辑、空列表清除、食用比例保留、流式部分失败和状态恢复契约。

### HYPOTHESIS（需 ARCH/运行验证）

- 候选 B 是最贴近现状的后续架构方向。
- 某一具体“保存失败”或“AI 保存失败”现象的唯一根因是状态恢复、跨日期双调用或整日替换；当前静态证据不足以单独定案。
- 是否需要独立 Recipe、是否需要把 Canonical Meal 接入生产，必须在 ARCH REVIEW 后决定，不能由本报告直接实施。

状态：`CODE_COMPLETE`

TURN=`REVIEW`
