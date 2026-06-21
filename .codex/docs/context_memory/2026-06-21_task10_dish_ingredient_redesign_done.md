# 任务10 菜品/食材重构完成摘要

- 时间：2026-06-21
- 结果：已按确认方案实现，未新增真实 Android Activity，使用独立 Compose route 承载食历页面。

## 完成内容

- 底部导航调整为：首页、菜品、食材、我的；保留中间添加餐食按钮。
- 首页“计划-全部”改为进入 `timeline_full` 独立食历页面，页面隐藏底栏并显示返回按钮。
- 新增 `IngredientsScreen`，复用食材选择器的分类、搜索、新增、编辑、删除和分类管理能力，以管理模式作为一级 Tab 展示。
- `IngredientPickerScreen` 增加 `asDialog/selectionMode` 参数：菜品编辑继续使用选择弹窗，食材 Tab 使用非弹窗管理模式。
- 菜品新增操作步骤：
  - shared 新增 `DishStep`，`Dish.steps`。
  - SQLDelight 新增 `dish_step` 表，数据库 version 8，迁移文件 `7.sqm`。
  - `DishRepository.saveDish/getDishById/observeDishById` 支持步骤保存和读取。
  - 新建/编辑菜品页在食材清单下方、特殊说明上方增加操作步骤编辑区。
  - 菜品详情页展示步骤文字和过程图片。

## 验证

- `./gradlew :shared:testDebugUnitTest` 通过。
- `./gradlew :androidApp:assembleDebug` 通过。

## 后续注意

- 当前步骤图片复用既有 `ImagePickerButton` 和 `|` 路径编码；如果后续步骤图片需要独立排序、说明或删除追踪，可再拆 `dish_step_image` 表。
- 数据库设计文档已同步新增 `dish_step`，但该文档 SQL 示例区域仍有历史字段滞后，不在本任务中整体重写。
