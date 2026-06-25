# 操作步骤标签移除与食材重构启动任务前快照

- 时间：2026-06-21
- 用户需求：
  1. 菜品操作步骤中不再显示固定“第1步/第2步”标签，添加步骤时只保留输入框和添加图片。
  2. 开始食材界面开发，按新的 `食材体系重构总方案.md` 推进。
- 任务类型/深度：Feature，实现任务，深度分阶段。
- 本轮范围：
  - 小功能：移除新建/编辑菜品和详情页操作步骤的第 x 步固定标识。
  - 食材第一步：调整主分类为 `最近 / 常规 / 营养 / 调养 / 自定义`；菜品选择食材与食材页统一启用同一套主分类入口；最近使用只在最近 Tab；管理分类只在自定义 Tab。
- 暂不做：完整 `日常食材维度.md` seed 重建、完整新增食材表单、多级树深度重写、调养规则新表。
- 预计涉及文件：
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishScreen.kt`
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/dishdetail/DishDetailScreen.kt`
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
  - `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- 风险：食材选择弹窗和食材管理页共用组件，必须避免选择模式无法完成、分类为空或管理按钮错误显示。
- 验证：运行 `./gradlew :androidApp:assembleDebug`。
