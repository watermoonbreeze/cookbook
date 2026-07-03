# 食材 TODO 改造第一步任务前快照

- 时间：2026-06-21
- 用户需求：按 `.ai-context/docs/feature/食材知识库与按食材找菜TODO.md` 逐步开始改造食材。
- 本轮范围：先实现 P0 第一批，控制改动面。
  - 食材页顶部主分类 Tab：常规 / 营养 / 慢病 / 自定义。
  - 左侧手风琴按当前 Tab 展示对应子分类。
  - 管理模式下点击食材先显示右侧详情，不再直接进入编辑；编辑入口放详情中。
- 暂不做：多分类编辑、筛选面板、按食材找菜、DB 新表。
- 执行深度：深度任务分阶段执行；本轮作为可独立验证的小步实现。
- 计划角色：主线程模拟 DEV_PM、DEV_SA、DEV_ARCH、DEV_UI、DEV_CODE、DEV_TEST、DEV_REVIEW；不真实 spawn 子代理。
- 已知项目状态：任务10已新增 `IngredientsScreen`，复用 `IngredientPickerScreen(asDialog=false, selectionMode=false)`；食材分类由 `IngredientPickerViewModel` 管理，当前左侧树未按主维度拆分。
- 预计涉及文件：
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- 风险：现有选择器和食材管理页共用同一组件，必须避免影响菜品编辑时的食材多选弹窗。
- 验证：至少运行 `./gradlew :androidApp:assembleDebug`。
