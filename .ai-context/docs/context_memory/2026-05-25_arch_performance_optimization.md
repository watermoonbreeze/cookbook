# 2026-05-25 阶段性架构审核与性能优化

## 背景
- 用户要求无人值守进行项目整体审核和性能优化，重点解决首页/食历切换、添加菜品后返回、添加餐食保存返回卡顿。
- 按自动任务编排执行：DEV_SA、DEV_PM、DEV_ARCH、DEV_REVIEW 参与分析/方案/预审。

## 本轮决策
- 采用低风险优化：不做 DB schema 迁移、不引入新依赖、不大改导航和页面结构。
- 食历窗口最大天数裁剪暂缓，因为会改变已加载日期保留体验和滚动锚点行为。
- 图片优化先用 `StoredImage` 内部 LRU 缩略图缓存，预览单独读取清晰图。

## 关键改动
- `Cookbook.sq` 新增 `selectTagsByDishIds`、`selectDishesOfMealRecords` 两个只读批量查询。
- `DishRepository` 批量组装 `DishMini`，首页/菜品页/选择器不再每个菜品单独查标签和烹饪方式。
- `MealRecordRepository` 按整个食历窗口批量组装餐食卡片菜品，减少首页计划、食历窗口、编辑回填的 N+1 查询。
- `saveDish()`、`save()`、`saveDayMeals()` 使用 `db.transaction`，避免半写入和多次中间 Flow 刷新。
- `DishesViewModel`、`DishPickerViewModel`、`IngredientPickerViewModel` 搜索防抖并取消旧任务。
- `DishPickerViewModel.refresh(force=true)` 保持立即执行，保证添加菜品返回菜品库能立刻刷新。
- `AGENTS.md` 修正 shared 测试命令为 `./gradlew :shared:testDebugUnitTest`。

## 验证
- `./gradlew :androidApp:compileDebugKotlin`：通过。
- `./gradlew :androidApp:assembleDebug`：通过。
- `./gradlew :shared:testDebugUnitTest`：通过，当前无测试源。
- `./gradlew :shared:allTests`：失败，原因是当前工程没有该 Gradle 任务。

## 后续待办
- 如仍卡顿，再单独评估食历窗口 49/63 天裁剪，并重点验证 `prependCount` 锚点。
- 发布前需要为 `dish.preference REAL -> INTEGER` 这类既有 schema 变更补正式迁移策略。
- 可考虑补充 Repository 单元测试，覆盖事务失败回滚和批量组装排序/标签。
