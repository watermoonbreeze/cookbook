# 食材自定义分类与编辑规则调整任务前快照

时间：2026-06-25。[AI生成]

## 用户最新需求

- 预设食材编辑时只能修改二级名称和图片，其他字段不允许修改。
- 自定义食材可以编辑所有字段。
- 编辑页分类归属不再完整展开显示，改为逗号展示已选具体分类，并通过“选择分类”弹出分类选择器。
- 分类选择器标题为“分类选择”，右侧有 `+` 可单独创建分类；默认显示自定义分类一级菜单，风琴式展开子级菜单。
- 食材界面选择一级/二级/子级分类时，都要立即筛选该分类及其所有子分类下食材。
- 分类内容默认分页 30 个，下拉/加载更多继续展示。

## 执行模式

- 任务类型：Feature/BugFix。
- 执行深度：标准。
- 交互模式：常规。
- 子代理：未明确要求真实分派，主线程模拟 DEV 角色。

## 预计涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/IngredientRepository.kt`
- `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq` 如分页需要新增查询。

## 风险

- 当前分类查询可能只查直接分类，需要补子分类聚合。
- 自定义食材和预设食材编辑权限不同，保存时要避免误覆盖预设分类/详情/单位。
- Compose 分类选择器要避免和已有分类管理弹框状态冲突。

## 验证

- 至少运行 `./gradlew :androidApp:assembleDebug`。
- 如改 shared 查询或 Repository，运行 `./gradlew :shared:testDebugUnitTest`。
