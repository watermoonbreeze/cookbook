# 食材详情弹层与图片点击修复完成

- 时间：2026-06-21
- 类型：BugFix / UI 交互

## 修复内容

- 食材详情底部弹层上方空白区域支持点击关闭。
- 食材 item 中的图片点击不再触发图片预览，而是跟点击卡片其他区域一样打开食材详情。
- 食材详情弹层内增加图片展示，点击详情内图片仍可打开图片预览。

## 涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/IngredientCard.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`

## 验证

- 已执行 `./gradlew :androidApp:assembleDebug`
- 结果：BUILD SUCCESSFUL
