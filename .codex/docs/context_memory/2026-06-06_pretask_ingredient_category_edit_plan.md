# 2026-06-06 任务前快照：食材分类编辑方案

- 用户最新需求：食材选择页面想增加分类编辑功能，需要规划设计并给出可行方案对比。[AI生成]
- 执行级别：标准级别 / 方案设计模式。[AI生成]
- 定级原因：涉及食材选择器、分类树、预设/自建分类权限、软删除、seed 数据兼容和后续多维分类扩展，但当前只做方案不改代码。[AI生成]
- 计划角色：DEV_PM 梳理产品边界与验收标准；DEV_ARCH 评估实现方案、风险和推荐路径；主线程汇总输出。[AI生成]
- 已知状态：食材分类来自 `food_category`，支持 `dimension/parent_id/crowd_type_id/status`；食材选择器已有左侧分类树和右侧食材网格；项目要求删除走 `status=0` 软删除。[AI生成]
- 预计涉及：`IngredientPickerScreen`、`IngredientPickerViewModel`、`FoodCategoryRepository`、`Cookbook.sq`、基础 seed JSON、分类相关文档。[AI生成]
- 主要风险：预设分类是否允许编辑、分类被食材引用时删除策略、慢病人群分类是否可编辑、JSON seed 与用户修改冲突。[AI生成]
- 待验证项：分类编辑入口是否清晰；用户自建分类能增删改；预设分类默认保护；删除不破坏食材关联和人群筛选。[AI生成]
