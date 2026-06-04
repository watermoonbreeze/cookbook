# 2026-06-04 菜品编辑空白专项结果

- 任务模式：专项缺陷排查模式；原因是编辑菜品空白属于高优先级单链路问题，需要先确认路由 id、仓库读取、SQLDelight 映射和 ViewModel 状态。
- 用户补充：编辑菜品时增加 Toast，显示当前编辑菜品名称和 id，便于判断为空时到底带了哪个 id。
- 实现：`NewDishScreen` 编辑模式加载完成后 Toast 提示：成功为“正在编辑：菜品名 ID=xxx”；失败为“编辑菜品 ID=xxx 加载失败：...”；空白为“编辑菜品 ID=xxx 当前为空”。
- 实现：`Cookbook.sq` 新增 `selectDishForEditById`，对 dish 主表编辑/详情字段使用 `COALESCE`，规避旧库 NULL 文本字段打断加载。
- 实现：`selectIngredientsOfDish` 对食材关联文本字段和单位名称使用 `COALESCE`，避免某个旧食材导致整个菜品详情/编辑加载失败。
- 实现：`DishRepository.observeDishById()` 和 `getDishById()` 切换到安全查询；详情页和编辑页共用稳定读取链路。
- 验证：`./gradlew :shared:generateCommonMainCookbookDatabaseInterface :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin` 通过。
- 验证：`./gradlew :shared:verifyCommonMainCookbookDatabaseMigration :shared:verifySqlDelightMigration :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest` 通过；测试任务为 NO-SOURCE。
- 真机判断：如果 Toast 显示正确菜名和 id 但表单仍空，需要继续排查 Compose 表单渲染；如果显示“当前为空”或“加载失败”，重点看该 id 对应数据库行/关联旧数据。
