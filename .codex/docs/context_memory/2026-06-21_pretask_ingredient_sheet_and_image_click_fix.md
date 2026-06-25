# 任务前快照：食材详情弹层与图片点击修复

- 时间：2026-06-21
- 用户需求：修复食材详情弹层点击外部空白不能关闭；食材 item 有图片和名称时，点击图片不应打开图片预览，应与点击 item 一样打开详情，图片预览放到详情中处理。
- 任务类型：BugFix
- 执行深度：轻量
- 角色分派：主线程模拟 DEV_UI、DEV_CODE、DEV_TEST

## 已知项目状态

- 食材首页与菜品选择食材已统一使用 `IngredientPickerScreen`。
- 食材详情已改为底部 65% 高度弹层。
- 当前弹层使用 Compose `Dialog`。
- 食材卡片来自 `ui/component/IngredientCard`。

## 预计涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/IngredientCard.kt` 或实际定义所在文件

## 风险与验证

- 风险：修改卡片图片点击后不能影响其他依赖 `IngredientCard` 的页面预期。
- 验证：执行 `./gradlew :androidApp:assembleDebug`。
