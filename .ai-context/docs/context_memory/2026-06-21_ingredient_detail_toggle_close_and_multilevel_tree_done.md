# 食材详情选择关闭与多级分类树完成

- 时间：2026-06-21
- 类型：BugFix + Feature
- 流程说明：标准任务；当前会话没有真实 `multi_agent_v1.spawn_agent`，主线程模拟 DEV 角色执行。

## 完成内容

- 食材详情弹层中点击 `选择` 或 `取消选择` 后，现在会立即关闭弹层。
- `FoodCategoryRepository.listChildren()` 不再把子分类固定为无子节点，会继续计算每个子节点是否还有下一层。
- `IngredientPickerViewModel.toggleExpand()` 支持任意层级分类展开/收起，不再限制一级展开二级。
- 左侧分类 UI 支持任意有子节点的分类显示展开箭头。
- 左侧分类按层级缩进，深层缩进做了上限控制，避免挤压文字。
- 常规 Tab 的子级匹配允许用户挂载到正式常规分类下的自定义分类同步展示。
- 已在 `.ai-context/docs/feature/食材体系重构总方案.md` 标记阶段 2 “支持多级分类树”完成。

## 涉及文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/IngredientPickerViewModel.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/IngredientRepository.kt`
- `.ai-context/docs/feature/食材体系重构总方案.md`

## 验证

- `./gradlew :androidApp:assembleDebug`：BUILD SUCCESSFUL
- `./gradlew :shared:testDebugUnitTest`：BUILD SUCCESSFUL

## 下一步建议

- 阶段 1 还缺结构化 seed；阶段 3 还缺新增/编辑食材的完整表单。
- 建议下一轮优先做 `ingredient_detail` 与调养/营养分类关系的数据承载，再改新增/编辑食材全屏 Sheet。
