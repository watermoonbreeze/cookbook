# 食材维度重构方案分析任务前快照

- 时间：2026-06-21
- 用户需求：读取 `tmp/claude/日常食材维度.md` 中与豆包讨论的食材维度划分，结合当前 Cookbook 应用整理可落地方案；分析食材页调整：顶部主分类 Tab、左侧子分类手风琴、多维筛选、食材可归属多个类目、右侧详情展示做法/分类/营养素/慢病适宜性等。用户明确要求先分析方案，不动代码。
- 任务类型/深度：Research + Feature 方案，标准级。
- 计划角色：主线程模拟 DEV_SA、DEV_ARCH、DEV_DB、DEV_UI、DEV_REVIEW；不真实 spawn 子代理，因用户未明确要求并行代理。
- 已知项目状态：食材当前由 `IngredientPickerScreen/ViewModel` 承载分类、搜索、最近使用、添加/编辑/删除和分类管理；任务10已把食材作为一级 Tab，并复用选择器的管理模式。
- 预计涉及模块：食材分类维度文档、`.ai-context/docs/feature/数据库设计方案.md`、`IngredientRepository`、`FoodCategoryRepository`、`IngredientPickerScreen/ViewModel`、食材相关 SQLDelight 表。
- 主要风险：维度过多导致 UI 复杂；营养素/慢病数据来源不足；现有 `food_category.dimension` 可支持多维但筛选模型需要扩展；右侧详情会牵涉数据表和编辑流程升级。
- 待验证项：用户文档真实路径；当前数据库是否已有 nutrition/crowd/general 维度；现有食材分类和人群建议是否足够支撑 MVP 筛选。
