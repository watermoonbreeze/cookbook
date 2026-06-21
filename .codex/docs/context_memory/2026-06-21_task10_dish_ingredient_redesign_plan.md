# 任务10 菜品/食材重构阶段方案

- 时间：2026-06-21 12:07 CST
- 类型：Feature，深度任务，常规交互。
- 子代理：当前工具要求只有用户明确要求并行代理时才能 spawn，因此本轮由主线程模拟 DEV_SA/DEV_ARCH/DEV_DB/DEV_UI/DEV_REVIEW。

## 关键现状

- 当前底部 Tab 是：首页、食历、菜品、我的；`Routes.TIMELINE` 是底部页。
- 当前应用是单 `MainActivity` + Compose `NavHost`，食历由 `FoodTimelineScreen` 作为 route 加载。
- 食材能力集中在 `IngredientPickerScreen/ViewModel`：分类、搜索、最近使用、添加/编辑/删除食材、分类管理都已具备，但当前形态是全屏 Dialog 选择器。
- 菜品 `Dish` 模型只有标签、食材、特殊说明、描述和图片；没有操作步骤。
- 数据库当前 SQLDelight version=7，最新迁移为 `6.sqm` 烹饪计时模板。

## 推荐方案

- 底部导航改为：首页、菜品、食材、我的；移除食历底部 Tab。
- 首页“计划-全部”进入食历独立页面。推荐先做 Compose 独立 route 隐藏底栏并复用 `FoodTimelineScreen`；若必须是真 Android Activity，则新增 `TimelineActivity` 并复用主题/初始化包装。
- 食材独立成一级页面：抽取 `IngredientPickerScreen` 内部主体为可复用 `IngredientManagerContent`，新增 `IngredientsScreen` 作为 Tab；选择器仍使用同一内容但保留多选确认栏。
- 菜品操作步骤新增 shared 模型：
  - `DishStep(id, sortOrder, text, imagePath, thumbnailPath)`
  - `Dish.steps: List<DishStep>`
- 数据库新增表 `dish_step`，字段建议：`id, dish_id, sort_order, text, image_path, thumbnail_path, created_at, updated_at, status`。
- Repository `saveDish` 事务同步步骤：编辑时软删除旧步骤，再按 sortOrder 写入新步骤；读取详情/编辑时按 sortOrder 返回。
- `NewDishScreen` 在食材清单下方、特殊说明上方增加“操作步骤”编辑区，支持动态添加步骤、编辑文字、每步选择多图、删除步骤。
- `DishDetailScreen` 在食材下方展示操作步骤，步骤内展示文字和图片。

## 风险评审

- 真 Activity 方案会重复处理 Koin、主题、沉浸式系统栏、初始化状态和跨页面导航，和当前单 Activity 架构不一致；除非用户明确坚持，否则推荐独立 route。
- 操作步骤图片如果使用新媒体表会更规范，但改动更大；MVP 推荐复用现有 `ImagePickerButton` 与 `|` 编码路径，后续再拆 `dish_step_image` 表。
- 新增 DB 表必须同步更新 `Cookbook.sq`、新增 `7.sqm`、把 version 升到 8，并运行 shared 测试与 Android assemble。
- 食材页抽组件时要避免破坏菜品编辑中的选择器行为，选择模式和管理模式要隔离确认栏/选中状态。

## 待用户确认

- “食历单独成为一个 Activity”是否必须是真 Android Activity？推荐解释为“独立页面/路由，隐藏底栏，内容与食历一致”。
- 操作步骤图片是否接受复用当前菜品图片选择/保存机制，即每步最多若干图，路径用 `|` 编码存入 `dish_step`。
