# 食材界面 BUG 修复完成记录

时间：2026-06-22。[AI生成]

## 修复内容

- 营养 Tab 不再显示“健康饮食”聚合根，左侧树直接上提其内部标签。
- 调养 Tab 不再显示“人群分类”聚合根，左侧树直接上提病种/人群节点。
- 编辑表单中“别名”改为“二级名称”，详情展示也同步为“二级名称”。
- 食材卡展示统一为 `食材名称(二级名称)`。
- 编辑保存后立即替换 ViewModel 当前列表和已选列表中的食材对象，详情弹层可实时看到新名称。
- 食材卡不再传入长按编辑/删除入口；管理模式下详情底部固定显示编辑按钮，用户自建食材额外显示删除按钮。

## 主要文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/IngredientCard.kt`

## 验证

- `./gradlew :androidApp:assembleDebug` 通过。

## 流程说明

- 本次任务按标准 BugFix 流程处理。
- 多智能体工具存在，但当前工具约束要求只有用户明确要求子代理时才能使用，因此本次由主线程模拟 DEV 角色完成。
