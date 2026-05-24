# MVP 第二夜：DishPicker 闭环与工程规范

> 时间：2026-05-24
> 状态：Android debug 构建通过

## 本轮完成

1. 新增工程统一规范：
   - `.codex/docs/experience/09_工程统一规范.md`
   - 已在 `.codex/docs/experience/INDEX.md` 登记
   - 已在 `AGENTS.md` 增加“工程一致性要求”

2. 实现 `DishPickerScreen`：
   - 路径：`androidApp/.../ui/picker/DishPickerScreen.kt`
   - 支持全屏 Compose Dialog
   - 支持多选/单选
   - 支持搜索、最近常吃 Chip、热度横滑、列表选择
   - 新增 `DishPickerViewModel`

3. 打通 AddDayFood 菜品选择闭环：
   - `AddDayFoodScreen` 点击“添加菜品”打开 DishPicker 多选
   - `AddMealViewModel.addDishes()` 合并去重
   - 保存仍走既有 `MealRecordRepository.save()`

4. 打通 NewDish 导入：
   - “导入”按钮打开 DishPicker 单选
   - `NewDishViewModel.importFromDishId()` 读取完整菜品并复用既有 `importFrom()`
   - 自动追加 `#复制` 标签的逻辑保留

5. 修复菜品详情路由缺口：
   - 新增 `DishDetailScreen` / `DishDetailViewModel`
   - `Routes.DISH_DETAIL` 已挂入 `MainScaffold`
   - 详情页展示热度、标签、食材、烹饪方式、说明，并提供编辑入口

6. 打通 DishesScreen 长按“基于此另存”：
   - 新增 `Routes.copyDish(dishId)`
   - 长按菜单进入 `NewDishScreen(importDishId=...)`

## 验证

执行：

```bash
./gradlew :androidApp:assembleDebug
```

结果：`BUILD SUCCESSFUL`

## 仍未完成

- Home 顶部搜索框仍只是样式，尚未调起 DishPicker。
- FoodTimeline 联动滚动、Mine 健康档案/备份子页仍待后续实现。

## 追加进度：NewDish 食材选择

- `NewDishScreen` 的“添加食材”已接入 `IngredientPickerScreen` 多选。
- `IngredientPickerScreen` 新增 `excludeIngredientIds` 参数，已选食材不再重复展示。
- `IngredientPickerViewModel.confirmSelected()` 改为返回跨分类/搜索保留的选中食材，避免只返回当前可见列表。
- 再次执行 `./gradlew :androidApp:assembleDebug`，结果：`BUILD SUCCESSFUL`。

## 追加进度：IngredientPicker 新建食材

- `IngredientPickerScreen` 底部“添加食材”已接入 AlertDialog 小弹窗。
- 弹窗支持食材名称、别名输入；创建失败时显示错误。
- `IngredientPickerViewModel.createUserIngredient()` 调用 `IngredientRepository.createUserIngredient()`，成功后自动选中新食材。
- 再次执行 `./gradlew :androidApp:assembleDebug`，结果：`BUILD SUCCESSFUL`。

## 下一步建议

优先处理 Home 顶部搜索框调起 DishPicker，或继续补 FoodTimeline 联动滚动 / Mine 健康档案与备份子页。
