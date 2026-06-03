# 2026-05-30 修复10：首页空计划、菜品编辑、V2.1 主题

## 用户需求
- 首页计划为空时仍展示计划标签，内容显示“暂无计划”。
- 菜品详情点击编辑必须进入编辑已有菜品，不能变成新建。
- 菜品新增/编辑页标签和烹饪方式 chip 使用内容自适应宽度，添加按钮紧跟在后面。
- 备份现有主题色，并按 `/Users/sxd/Downloads/菜单APP/菜谱菜单App 全局视觉设计规范 V2.1（CodeX 开发专用）.md` 生成新主题。

## 本次实现
- `HomeScreen`：计划标题固定展示；`ui.plans` 为空时显示 `EmptyState("暂无计划")`。
- `NewDishViewModel`：新增 `start(editingDishId, importDishId)`，统一页面入口，先重置旧表单状态，再按编辑/导入/新建加载。
- `NewDishScreen`：用单个 `LaunchedEffect(editingDishId, importDishId)` 调用 `vm.start()`，避免编辑和导入副作用互相覆盖。
- `NewDishScreen`：标签和烹饪方式改为定高 `AssistChip`，不设置宽度，达到 wrap_content 效果。
- 主题备份：`.codex/docs/theme_backup/2026-05-30_v2_1_before/`。
- 主题更新：`Color.kt`、`ExtendedColors.kt`、`Theme.kt` 按 V2.1 暖杏规范替换亮色与暗色，并将页面/模块标题字重改为 `SemiBold`。

## 验证
- 已执行：`./gradlew :shared:generateCommonMainCookbookDatabaseInterface :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest`
- 结果：`BUILD SUCCESSFUL`。
- 单元测试任务当前为 `NO-SOURCE`。

## 后续关注
- 主题色实际观感需要用户在真机/模拟器上确认；如果仍偏脏，优先微调 `surfaceVariant`、`primaryContainer`、`outline`。
- 编辑页问题如果仍复现，下一步检查 `DishDetailScreen.onEdit` 的调用点和 `MainScaffold` 的 `NavHost` back stack scoped ViewModel。
