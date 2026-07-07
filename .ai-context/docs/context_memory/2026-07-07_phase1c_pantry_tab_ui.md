# 阶段1c：库存 Tab UI（我家食材端到端打通）

[AI生成] 2026-07-07。用户在线，常规交互。用户指令：阶段1 UI 改造，除搜索下拉框外都继续。

## 关键发现

`IngredientPickerViewModel` **早已是 5 Tab**（最近/常规/营养/调养/自定义），左树右网格/分页/各 Tab 数据源都已实现。故"6 Tab 重构"真实缺口=**补库存 Tab**。

## 成果（:androidApp:assembleDebug 通过）

- `IngredientMainTab` 加 `PANTRY("库存")`，顺序=最近/库存/常规/营养/调养/自定义。
- ViewModel 注入 `PantryRepository`：库存数据源(listPantryIngredients)、`addToPantry`(Ingredient/id 两版)、`removeFromPantry`、`pantryIngredientIds`、切库存自动刷新；库存/最近同为无左树平铺。
- 详情弹层加"加入库存/移出库存"按钮（管理模式，`inPantry`+`onTogglePantry` 参数）。
- 库存 Tab 底部："从食材库添加"(跳常规) +"新建食材入库"(创建后 `pendingPantryOnCreate` 自动入库)。
- DI：`IngredientPickerViewModel(get(),get(),get(),get())` 加 PantryRepository。

## 改动文件

`IngredientPickerViewModel.kt` / `IngredientPickerScreen.kt` / `di/AndroidModule.kt`。

## 待办

- **真机验证**库存加入/移出交互（数据层已单测，UI 只编译验证过）。
- 未做：拆 1636 行 IngredientPickerScreen（纯重构，用户待定）；详情四区顺序打磨；搜索下拉框（用户明确排除）。
