# 保存菜品后最近食材未显示修复结果

- 时间：2026-06-10
- 任务类型：BugFix。
- 用户现象：使用食材并保存菜品成功后，再打开食材选择器的“最近使用”没有看到该食材。
- 日志结论：`temp/info.log` 显示 `new_dish_save success=true`，且再次进入编辑页后 `ingredientCount` 已增加，说明菜品与食材关联保存成功。
- 根因：新建/编辑菜品页打开 `IngredientPickerScreen` 时传入当前菜品已有食材作为 `excludeIngredientIds`，选择器会把这些食材从“最近使用”查询结果中过滤掉。
- 修复：`NewDishScreen` 打开食材选择器时传 `emptySet()`，不再过滤当前菜品已有食材；重复添加仍由 `NewDishViewModel.addIngredient()` 的去重逻辑兜底。
- 影响范围：仅 Android 新建/编辑菜品页的食材选择器展示策略；不改 shared、SQLDelight、保存菜品和最近食材数据层。
- 验证：`./gradlew :shared:testDebugUnitTest` 成功。
- 验证：`./gradlew :androidApp:assembleDebug` 成功。
- 流程偏差：多智能体工具存在，但当前工具规则要求用户明确授权才可 spawn，本次由主线程模拟 DEV 角色执行。
