# 食材展示统一与详情底部弹层任务前快照

- 时间：2026-06-21
- 用户需求：
  1. 首页食材 Screen 与菜品中添加食材界面除选择相关按钮/底栏外，其余展示应一致。
  2. 食材详情不在右侧展示，改为从底部弹出，约占界面 65% 高度，内容可上下滚动。
  3. 详情顶部左侧“取消”，右侧“选择”；从菜品选择食材打开时显示选择，从首页食材页打开时只显示取消。
  4. 底部已选择食材栏只有选择后才显示；未选择时隐藏。
- 任务类型/深度：BugFix/UI 架构调整，标准。
- 计划角色：主线程模拟 DEV_PM、DEV_ARCH、DEV_UI、DEV_CODE、DEV_TEST、DEV_REVIEW。
- 预计涉及文件：
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- 设计决策：
  - 移除食材页右侧详情面板，食材列表在管理/选择模式下共用完整宽度。
  - 点击食材统一打开底部详情 Sheet。
  - 菜品选择模式下详情 Sheet 右侧显示“选择”，点击后加入已选并关闭详情。
  - 首页食材页模式下详情 Sheet 只展示详情和取消。
  - 底部选择栏只在选择模式且已有已选食材时显示。
- 验证：运行 `./gradlew :androidApp:assembleDebug`。
