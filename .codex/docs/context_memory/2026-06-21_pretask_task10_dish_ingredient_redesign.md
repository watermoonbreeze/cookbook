# 任务前上下文快照：任务10 菜品与食材功能重构

- 时间：2026-06-21
- 用户需求：重新设计菜品功能；底部导航调整为“首页 / 菜品 / 食材 / 我的”；首页计划“全部”进入食历独立 Activity/独立加载；食材从选择器中独立为与菜品同级模块；我的保持不变；菜品新增操作步骤，步骤支持动态添加文本或图片。
- 任务类型：Feature / 架构与数据重构。
- 执行深度：深度。
- 交互模式：常规。
- 角色分派：主线程模拟 DEV_SA、DEV_PM、DEV_ARCH、DEV_REVIEW、DEV_DB、DEV_UI、DEV_CODE、DEV_TEST；因当前工具规则未获用户明确授权，不真实 spawn 子代理。
- 已知项目状态：Android 当前单 Activity + Compose NavHost；底部 Tab 为首页/食历/菜品/我的；食材目前主要在 `IngredientPickerScreen` 中使用；菜品数据已有 dish、dish_ingredient、image_path/thumbnail_path 等字段。
- 预计涉及文件：`Destinations`、`MainScaffold`、`HomeScreen`、`FoodTimelineScreen`、新增/调整 Ingredient 独立页、`NewDishScreen`、`DishDetailScreen`、`DishRepository`、`Cookbook.sq`、迁移文件、数据库设计与功能文档、测试。
- 主要风险：从“食历 Tab”改为独立页面会影响现有入口；“单独 Activity”与当前单 Activity 架构冲突，需要确认是否真新 Android Activity 还是 Compose 独立页面；菜品步骤支持图片会引入 schema、图片选择复用、保存/编辑/详情展示的一致性问题。
- 待验证项：路由结构、底栏行为、食材页是否复用现有选择器能力、菜品步骤的保存/加载/编辑、迁移验证、shared 单测和 Android 构建。
