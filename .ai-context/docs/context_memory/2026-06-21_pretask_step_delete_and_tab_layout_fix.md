# 操作步骤删除按钮与食材主分类布局修复任务前快照

- 时间：2026-06-21
- 用户需求：
  1. 操作步骤 item 里的关闭图标放到右下方或采用更优布局，避免顶部空占位。
  2. 食材一级主分类 item 自适应布局，避免“自定义”被挤成两行；当前可一行展示，后续可滚动。
- 任务类型/深度：BugFix/UI 调整，轻量。
- 计划角色：主线程模拟 DEV_UI、DEV_CODE、DEV_TEST。
- 预计涉及文件：
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishScreen.kt`
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- 方案：
  - 操作步骤删除按钮移到 item 右下角，释放顶部空间。
  - 食材主分类使用 `ScrollableTabRow` + 单行文本 + 最小宽度，避免文字换行。
- 验证：运行 `./gradlew :androidApp:assembleDebug`。
