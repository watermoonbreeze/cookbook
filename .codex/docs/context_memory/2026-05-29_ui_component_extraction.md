# 2026-05-29 UI 控件抽取上下文

- 任务编排：标准级无人值守；只读评估智能体 Carson 因网络断开失败，主线程基于本地扫描继续执行。
- 新增通用控件：
  - `AppSearchField`：48dp 色块搜索栏，已用于菜品页、搜索页、食材选择器。
  - `UiText.kt`：`FormFieldLabel`、`SectionHeader`、`EmptyLineText`，替换页面内重复 `FieldLabel` / `SectionTitle` / `SearchEmptyLine`。
  - `IngredientCard`：统一搜索页食材结果和食材选择器食材格子，支持选中态、自建食材长按菜单、人群建议角标。
- 样式统一：
  - `DayMealCardView` 和添加餐食的 `MealBlockCard` 使用 `ElevatedCard` 色块 + 投影，减少边框。
  - 菜品页、搜索页、食材选择页搜索框统一为 `AppSearchField`。
- 导航修复：
  - `MainScaffold` 为 `NEW_DISH` 显式声明 `dishId` / `importDishId` navArgument，详情页点击编辑能进入编辑模式。
- 文档：
  - `.codex/docs/feature/UI控件命名清单.md` 已新增并更新为当前抽取状态。
- 验证：
  - `:shared:generateCommonMainCookbookDatabaseInterface`
  - `:shared:compileDebugKotlinAndroid`
  - `:androidApp:compileDebugKotlin`
  - `:androidApp:assembleDebug`
  - `:shared:testDebugUnitTest`
  - `:androidApp:testDebugUnitTest`
  全部通过；当前测试任务为 `NO-SOURCE`。
