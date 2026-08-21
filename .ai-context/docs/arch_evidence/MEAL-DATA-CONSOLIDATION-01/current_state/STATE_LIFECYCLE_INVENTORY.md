# State lifecycle inventory

| State | Current code fact | Evidence |
|---|---|---|
| persisted state | 有效餐食在 `meal_record` + `meal_record_dish`；数据库 `status=1` 才参与主要查询 | `Cookbook.sq:258-285,1444-1501` |
| manual draft | `MealBlockUiState`、`DayMealDraft`、`MealRecordEditData` 在 VM/Repository 内存中流转 | `AddMealViewModel.kt:31-40,418-428`；`MealRecordRepository.kt:333-369,562-578` |
| navigation-derived state | `AddMealViewModel` 当前日期/来源日期、`AiMealInputUiState.targetDate`、统一入口 `MealRange/MealInputMethod` | `AddMealViewModel.kt`；`AiMealInputViewModel.kt:102-158`；`UnifiedAddMealState.kt:4-38` |
| AI input | `INPUT` 阶段维护 quick/period 文本、输入模式、日期和 existing dates | `AiMealInputViewModel.kt:102-158,259-321` |
| AI generating | `submit()` 取消旧 generation，进入 `GENERATING`，流式 session 更新 segment/progress | `AiMealInputViewModel.kt:422-505` |
| AI partial/preview | 合法解析结果触发 `preview()`；阶段为 `PARTIAL_READY` 或 `PREVIEW_READY`，`autoGenPreview` 仅在 `StateFlow` | `AiMealInputViewModel.kt:693-785` |
| saving | `confirmSave()` 只接受 partial/preview，冻结 preview，进入 `SAVING`，调用 session port commit | `AiMealInputViewModel.kt:955-998` |
| done | commit 成功后 state 进入 `DONE` 并持有 `autoGenResult`；UI `AiMealBody` 在 DONE 触发 `onSaved` | `AiMealInputViewModel.kt:985-998`；`AiMealInputSheet.kt:217-222` |
| reset/cancel | reset 清理 generation、preview、warnings 等并回 INPUT；cancel 取消 job 并清理流式辅助状态 | `AiMealInputViewModel.kt:1004-1024` |
| existing-meal guard | 手工编辑和 AI 预览均通过 `loadDayMealsForEdit(date).isNotEmpty()` / `hasExisting` 判定已有记录 | `AddMealViewModel.kt:222,574`；`DayAutoGenerator.kt:53` |

## 边界结论

当前代码可直接证明的“AI 数据成为 persisted truth”时点是 `confirmSave -> sessionPort.commit -> MultiDayRecorder.commitPreview -> DayAutoGenerator.commit -> MealRecordRepository.saveDayMeals` 完成写库；输入、生成、preview 都不是数据库真相。`DONE` 是 UI/VM 状态，不等价于独立数据库状态字段。
