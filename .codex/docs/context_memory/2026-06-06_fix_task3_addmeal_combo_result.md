# 2026-06-06 任务3修复结果

## 已修复

- 添加餐食页从“添加到餐次”弹框跳转新建菜品后，保存返回时不再重新打开菜品选择弹框。[AI生成]
- 新建菜品返回后仍会按 `createdDishId` 回填到当前餐食模块，并清理 `pickingBlockId` 选择上下文。[AI生成]
- 再次打开菜品选择弹框时，强制刷新菜品库，确保新建菜品能从数据库最新列表读取。[AI生成]
- 添加餐食页打开菜品选择器时，当前餐次已有菜品作为 `initialSelected` 传入，不再通过 `excludeDishIds` 过滤，因此已添加菜品会以勾选态展示。[AI生成]
- 保存组合默认名称改为 `日期 餐次 时间 (N道菜)`，例如 `2026-06-06 早餐 07:30 (3道菜)`。[AI生成]

## 变更文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/addmeal/AddDayFoodScreen.kt`。[AI生成]
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/DishPickerScreen.kt`。[AI生成]

## 验证

- `./gradlew :androidApp:assembleDebug`：成功。[AI生成]
