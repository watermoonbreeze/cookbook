# 2026-05-24 修复4无人值守完成记录

## 任务状态

- 模式：无人值守 + 深度级。
- 完成时间：2026-05-24 23:15:59 CST。
- 终审结论：通过，可交付。
- 验证命令：`./gradlew :androidApp:compileDebugKotlin :androidApp:assembleDebug`。
- 验证结果：`BUILD SUCCESSFUL`。

## 已完成

- 菜品详情喜爱度显示改为只显示数量，不再显示 `/1000`。
- `DishesScreen` / `DishPickerScreen` 用户可见“热度”改为“喜爱”。
- `DishRow` 改为左侧图片、中间菜名+标签上下结构、普通模式右侧星级、多选模式右侧 Checkbox。
- `DishRepository` 统一补齐列表 `DishMini` 的标签和烹饪方式，并为 Flow 链路补 `flowOn(Dispatchers.Default)`。
- 食历改为连续自然日窗口：
  - 默认 `today-6..today`；
  - 日期正序；
  - 首次定位当天；
  - 顶部加载历史 7 天；
  - 底部加载未来计划 7 天；
  - 前插历史后通过 `prependCount` 保持滚动锚点。
- 添加餐食默认日期规则：
  - 无记录或最大餐食日期小于今天：默认今天；
  - 最大餐食日期大于等于今天：默认最大日期 + 1；
  - 编辑入口 `editDate` 优先。
- 新增/编辑菜品与菜品详情不再展示“主料”标识。
- 新添加食材 `isMain=false`，保留字段但不暴露主料语义。
- 用户自建食材删除：
  - UI 增加二次确认；
  - Repository 事务内先删除 `dish_ingredient` 关联，再删除 `ingredient source='user'`；
  - 删除失败时通过 `operationError` 弹窗提示。

## 重要决策

- `dish.preference INTEGER` 是上一轮用户明确要求的既有改动，本轮不回退。
- `dish_ingredient.is_main` 字段暂不删除，后续做原料/调味料分类时再统一设计迁移。
- 食历方向固定为：列表上方历史、下方计划；手指上滑看未来计划，下拉看更早历史。

## 残余风险

- 旧规划文档中仍有“热度”“主料”等历史描述，后续可专门做文档语义清理。
- `NewDishViewModel.toggleMain()` 已无 UI 入口，后续确认彻底不做主料后可清理。
- 删除用户食材显式清理了 `dish_ingredient`，其他关联依赖 schema 的 `ON DELETE CASCADE`。

