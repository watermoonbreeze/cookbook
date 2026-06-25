# 食材界面 BUG 修复任务前快照

时间：2026-06-22。[AI生成]

## 用户最新需求

1. 营养和调养标签下不再显示“健康饮食”“人群分类”这类聚合根节点，需要把内部标签上提一层。
2. 食材编辑后列表展示名称没有实时刷新；编辑里的“别名”要改成“二级名称”，展示格式为 `食材名称(二级名称)`。
3. 因为已有食材详情，长按编辑和删除要迁移到详情中，在详情下方固定显示编辑、删除按钮。

## 执行模式

- 任务类型：BugFix。
- 执行深度：标准。
- 交互模式：常规。
- 子代理：工具存在，但本会话工具约束要求只有用户明确要求子代理时才能 spawn；本次降级为主线程模拟 DEV_SA/DEV_ARCH/DEV_CODE/DEV_UI/DEV_TEST/DEV_REVIEW。

## 预计涉及模块

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- 可能涉及 `shared` 的展示命名文案或模型注释，但优先只改 Android UI。

## 主要风险

- 营养/调养根节点上提不能破坏子节点展开和筛选。
- 编辑保存后需要同步列表、选中项、详情态，避免 Sheet 中仍显示旧名称。
- 移除长按入口后，食材页管理模式仍必须能编辑/删除；菜品选择模式不能误显示删除。

## 待验证项

- `./gradlew :androidApp:assembleDebug`
- 如 shared 未修改，可不跑 shared 单测；若涉及 shared，再补跑 `:shared:testDebugUnitTest`。
