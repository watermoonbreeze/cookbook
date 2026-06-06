# 2026-06-05 单元测试体系建设结果

- 用户要求：按正常流程添加单元测试，自动测试所有单元，发现问题及时修。
- 新增测试依赖：`sqldelight-sqlite-driver`，用于 shared/androidUnitTest 内存 SQLite。
- 新增测试工具：`RepositoryTestDatabase`，每个测试创建独立 SQLDelight 内存库。
- 新增测试：`DishRepositoryTest` 2 个，覆盖菜品保存编辑读取、软删除后查询不可见。
- 新增测试：`MealRecordRepositoryTest` 2 个，覆盖整日清空、同日重复编辑不重复增加喜爱值。
- 新增测试：`IngredientRepositoryTest` 2 个，覆盖食材软删除后搜索不可见、创建后按分类读取。
- 测试结果：`./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest` 通过；shared 实际执行 6 个测试，0 failures，0 errors；androidApp 暂无测试源码仍为 NO-SOURCE。
- 完整验证：`./gradlew :shared:verifyCommonMainCookbookDatabaseMigration :shared:verifySqlDelightMigration :androidApp:assembleDebug` 通过。
- 规范更新：单元测试纳入基础开发流程；功能完成后必须补充/更新对应单元测试并运行相关测试任务。
