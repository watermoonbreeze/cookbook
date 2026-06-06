# 食材分类编辑方案评估任务前快照

- 日期：2026-06-06
- 用户需求：评估在食材选择页面增加分类编辑功能，当前只做规划设计和方案对比，不实现。
- 任务模式/级别：标准调研/架构方案任务；原因是涉及 UI、Repository、数据库分类结构，需读取现有链路并给出多方案。
- 计划分派角色：DEV_SA 负责只读结构分析；DEV_ARCH 负责方案对比、风险和推荐方案。
- 流程偏差：当前无 multi_agent_v1 子代理工具可用，降级为主线程按 DEV_ARCH 角色执行。
- 已知项目状态：KMP 项目，shared 承接 Domain/Data，androidApp 承接 Compose UI；SQLDelight schema 为数据库唯一来源。
- 预计涉及模块：shared 分类模型/Repository/SQLDelight；androidApp IngredientPicker 相关页面与 ViewModel。
- 主要风险：分类来源 preset/user 权限边界、分类重命名对已有关联数据影响、Android/iOS 双端 API 一致性、删除分类后的食材归属处理。
- 待验证项：现有 food_category/ingredient_category 表结构、FoodCategoryRepository 接口能力、IngredientPicker UI 状态流和分类筛选数据来源。
