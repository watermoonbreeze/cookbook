# 2026-05-24 界面切换卡顿优化记录

- 修复 `DishesViewModel.uiState` getter 每次 Compose 读取都重新创建 `combine/stateIn` 的问题，改为稳定 `val uiState`。
- `DishRepository.searchDishes/getDishById/saveDish/deleteDish` 切到 `Dispatchers.Default`，避免 ViewModel 主线程执行 SQLDelight `executeAsList`。
- `IngredientRepository` 的搜索、分类查询、创建/编辑/删除等同步 SQL 调用也切到后台线程。
- `StoredImage` 图片解码改为 `Dispatchers.IO`，并使用 `inSampleSize=4` 解码缩略图，降低列表切换和返回页面时的图片解码压力。
- `DishPickerViewModel.refresh` 增加重复查询保护；`DishPickerScreen` 仅在可新增菜品的场景 `ON_RESUME` 强制刷新，保证新建菜品返回后能显示，同时避免普通重组反复查询。
- 验证命令：`./gradlew :androidApp:assembleDebug`，结果通过。
