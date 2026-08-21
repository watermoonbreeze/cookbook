# Call-site inventory

| Callee | Caller | File | Transform before UI | Notes |
|---|---|---|---|---|
| `observeTodayPlusFuture` | `HomeViewModel.uiState` | `androidApp/.../home/HomeViewModel.kt:210-218` | Repository 返回 `List<DayMealCardData>`；VM 直接包装 `HomeUiState` | today + 后续最多 2 个有记录日期 |
| `observeTodayPlusFuture` | repository tests | `shared/.../data/repository/MealRecordRepositoryTest.kt` | 断言日期/完整餐次行为 | 快照中包含完整测试 |
| `observeTimelineDates` | `TimelineViewModel.observeTimelineDates` | `androidApp/.../timeline/TimelineViewModel.kt:173-196` | `List<LocalDate>` → `allDates`、`mealDates`、分页窗口 | 只来自实际有记录日期 |
| `loadTimelineCardsByDates` | `TimelineViewModel.loadVisibleCards` | `androidApp/.../timeline/TimelineViewModel.kt:201-224` | 先按窗口切日期，再返回 `DayMealCardData` 并按日期排序 | UI 状态更新 `pages` |
| `loadDayMealsForEdit` | `AddMealViewModel` | `androidApp/.../addmeal/AddMealViewModel.kt:222,514,567,574` | `List<MealRecordEditData>` → occupied/编辑块/存在日期 | existing-meal guard 来源 |
| `saveDayMeals` | `AddMealViewModel.save` | `androidApp/.../addmeal/AddMealViewModel.kt:418-428` | `MealBlockUiState` → `DayMealDraft` → 原子整日替换 | 移动时另删来源日 |
| `deleteDayMeals` | `AddMealViewModel` | `androidApp/.../addmeal/AddMealViewModel.kt:423` | 保存移动后的目标日后删除来源日 | 代码事实，未推断产品语义 |
| `saveDayMeals` | `HomeViewModel.deleteDayUndoable` | `androidApp/.../home/HomeViewModel.kt:236-256` | `snapshotDay` → 删除 → 撤销时原样恢复，`bumpPreference=false` | 首页删整日撤销 |
| `saveDayMeals` | `TimelineViewModel.deleteDayUndoable` | `androidApp/.../timeline/TimelineViewModel.kt:142-164` | 同上 | 食历删整日撤销 |
| `saveDayMeals` | `WeekPlanViewModel` | `androidApp/.../weekplan/WeekPlanViewModel.kt:94-100` | 计划卡删除撤销 | WeekPlan VM/UI 已纳入快照 |
| `saveDayMeals` | `DayAutoGenerator.commit` | `shared/.../domain/autogen/DayAutoGenerator.kt:195-207` | AI/自动生成预览按天转 drafts 后提交 | AI 成为 persisted truth 的写入口 |
| `preview` | `AiMealInputViewModel.updatePreview` | `androidApp/.../ai/AiMealInputViewModel.kt:693-785` | 流式 `DayMealJson` 合并 → `AutoGenPreview` → state | preview 仍未落库 |
| `commit` | `AiMealInputViewModel.confirmSave` | `androidApp/.../ai/AiMealInputViewModel.kt:955-998` | 冻结 `AutoGenPreview` → `SAVING` → `AutoGenResult`/`DONE` | 具体持久化在 `MultiDayRecorder`/`DayAutoGenerator` |
| `reset` | `AiMealInputSheet`、`UnifiedAddMealScreen` | `androidApp/.../ai/AiMealInputSheet.kt:1083`；`androidApp/.../addmeal/UnifiedAddMealScreen.kt:99,119` | 清空 AI 会话回 INPUT | 不触碰数据库 |
| `cancelGeneration` | `UnifiedAddMealScreen`、`AiMealInputSheet` | `androidApp/.../addmeal/UnifiedAddMealScreen.kt:54`；`androidApp/.../ai/AiMealInputSheet.kt:149` | 取消 job 并清理会话辅助状态 | 关闭输入流程 |
| `DayMealCardView` | Home/Timeline/AddMeal/WeekPlan/Search | 对应 `HomeScreen.kt:238,260`、`FoodTimelineScreen.kt:149`、`AddDayFoodScreen.kt:546` | 均直接传 `DayMealCardData` | 共享 domain 投影类型 |
| `MealDateCalendarDialog` | AddMeal/Timeline | `AddDayFoodScreen.kt:418`；`FoodTimelineScreen.kt:185` | 调用方提供 `initialDate`、模式和日期集合 | 组件本身不查询数据库 |

## 其他明确命中

- `observeTimelineCards`、`observeTimelineWindow` 在 `MealRecordRepository` 定义；当前目标 Timeline VM 主要使用日期流 + `loadTimelineCardsByDates`，不能把未命中调用误写成 UI 主链。
- `MealPlan`、`DayMealData`、`isPlanState` 作为独立持久化概念未发现；`isPlanState` 仅是 `DayMealCardData` 投影字段及其 UI 消费。
