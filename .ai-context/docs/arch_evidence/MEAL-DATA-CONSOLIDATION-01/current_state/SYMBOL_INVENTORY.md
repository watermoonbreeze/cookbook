# Symbol inventory

事实基线：`7c9a0707ae717b7d5ae3e30221b84e6ea5595bac`。行号以 `source_snapshot/` 对应原始文件为准。

| Symbol | Kind | File | Role observed in code | Persisted? | Notes |
|---|---|---|---|---|---|
| `MealRecord` | data class | `shared/.../domain/model/MealRecord.kt:25-35` | 单餐记录领域模型：date、mealTypeId、time、note、dishes | 是，映射 `meal_record` | 无 plan 字段 |
| `MealType` | data class | `shared/.../domain/model/MealRecord.kt:11-21` | 餐次定义 | 是，映射 `meal_type` | `code/name/defaultTime/isFixed` |
| `DayMealCardData` | data class | `shared/.../domain/model/MealRecord.kt:39-47` | Home/Timeline/卡片统一消费模型 | 否，查询投影 | 含 `isToday`、`isPlanState` |
| `MealSection` | data class | `shared/.../domain/model/MealRecord.kt:51-59` | 一日卡片内餐次分组 | 否，查询投影 | `mealRecordId` 可空 |
| `DayMealDraft` | data class | `shared/.../data/repository/MealRecordRepository.kt:562-569` | 整日保存/撤销快照输入 | 否，写入时转 persisted | 不在独立 model 文件 |
| `MealRecordEditData` | data class | `shared/.../data/repository/MealRecordRepository.kt:572-578` | 编辑页读取模型 | 否 | 由真实记录查询组装 |
| `MealRecordRepository` | class | `shared/.../data/repository/MealRecordRepository.kt:30-577` | meal_record 查询、投影、保存、删除 | 间接 | `isPlanState` 在此按日期派生 |
| `HomeUiState` | data class | `androidApp/.../home/HomeViewModel.kt:36-40` | 首页订阅结果 | 否 | `plans` 类型为 `List<DayMealCardData>` |
| `TimelineUiState` | data class | `androidApp/.../timeline/TimelineViewModel.kt:18-32` | 食历日期/窗口/卡片状态 | 否 | `mealDates` 是 `Set<LocalDate>` |
| `MealBlockUiState` | data class | `androidApp/.../addmeal/AddMealViewModel.kt:31-40` | 手工编辑页的餐次草稿 | 否 | 由 VM 收集后转 `DayMealDraft` |
| `AiMealPhase` | enum | `androidApp/.../ai/AiMealInputViewModel.kt:47-58` | AI 会话阶段 | 否 | INPUT/GENERATING/PARTIAL_READY/PREVIEW_READY/SAVING/DONE/ERROR |
| `AiMealInputUiState` | data class | `androidApp/.../ai/AiMealInputViewModel.kt:102-158` | AI 输入、流式进度、预览和保存状态 | 否 | `autoGenPreview` 仍为内存态 |
| `AutoGenPreview` | data class | `shared/.../domain/autogen/AutoGenModels.kt:124-134` | 自动生成预览结果 | 否 | 供 commit 输入 |
| `AutoGenResult` | data class | `shared/.../domain/autogen/AutoGenModels.kt:137-145` | 自动生成提交结果 | 否 | 计数型结果 |
| `MultiDayRecorder` | class | `shared/.../ai/meallog/MultiDayRecorder.kt:60-214` | AI 多日输入的 preview/commit 门面 | 间接 | commit 调 `DayAutoGenerator` |
| `DayAutoGenerator` | class | `shared/.../domain/autogen/DayAutoGenerator.kt:17-288` | 餐次级 preview/commit | 是，经 `saveDayMeals` | 处理 mergeMode 与 eaten ratio |
| `DayMealCardView` | composable | `androidApp/.../component/DayMealCardView.kt:39-...` | 消费 `DayMealCardData` | 否 | `data.isPlanState` 影响视觉与操作 |
| `MealDateCalendarDialog` | composable | `androidApp/.../component/MealDateCalendarDialog.kt:37-...` | 消费日期集合/选择模式 | 否 | 日期集合由调用方传入 |
