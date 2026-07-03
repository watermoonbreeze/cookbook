# 2026-06-06 任务前快照：食材分类编辑实现

- 用户最新需求：按推荐方案 A 实现食材选择页分类编辑；额外在“全部”下方增加固定分类“最近使用”，只展示最近被引用的食材，按引用时间倒序；无人值守执行。[AI生成]
- 执行级别：深度级别 / 无人值守实现模式。[AI生成]
- 定级原因：涉及 SQLDelight 查询、Repository、ViewModel、Compose UI、虚拟分类、测试与文档，影响食材选择核心流程。[AI生成]
- 计划角色：DEV_CODE/主线程实现分类 CRUD 与最近使用；DEV_REVIEW 终审风险；DEV_UT 补单元测试；DEV_TEST 跑构建验证。[AI生成]
- 已知项目状态：`food_category` 已有 `source/status/parent_id/dimension/sort_order`；`FoodCategoryRepository` 当前只读；`IngredientPicker` 左侧分类树只支持展开/选择。[AI生成]
- 预计涉及：`Cookbook.sq`、`IngredientRepository.kt`、`IngredientPickerViewModel.kt`、`IngredientPickerScreen.kt`、shared 单元测试、相关文档。[AI生成]
- 主要风险：预设分类误编辑、删除分类破坏食材关联、最近使用查询排序错误、分类下拉仍依赖展开态导致漏选。[AI生成]
- 待验证项：自建分类新增/改名/删除；预设分类不可编辑；最近使用分类显示最近被餐食引用的食材；shared 单元测试和 Android 构建通过。[AI生成]
