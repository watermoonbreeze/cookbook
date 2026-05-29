# 2026-05-26 修复6：搜索、返回键、底栏加号

## 用户需求
- 食历只在首次打开时自动定位，用户滑动后切回不再回到当天。
- 菜品列表 item 右侧星级下方显示喜爱值，值为 0 不显示。
- 首页搜索图标进入独立搜索页。
- 搜索页同时搜索菜品、食材、餐食，结果按三组展示。
- 添加餐食按钮融入底部导航栏正中，样式保持圆形主按钮。
- 物理返回键：有路由先返回；路由结束且非首页回首页；首页再按一次退出。

## 关键改动
- `FoodTimelineScreen`
  - 移除页面层 `state.pages/todayIndex` 兜底自动定位，只响应 VM 的 `scrollRequestVersion`。
- `DishRow`
  - 普通模式右侧 `StarRating` 下方显示 `dish.preference`，仅大于 0 显示。
- `SearchScreen` / `SearchViewModel`
  - 新增全局搜索页和 ViewModel。
  - 搜索输入防抖 280ms。
  - 菜品复用 `DishRow` 横滑展示。
  - 食材使用轻量卡片横滑展示。
  - 餐食复用 `DayMealCardView` 纵向展示，并支持触底加载更多。
- `MealRecordRepository`
  - 新增 `searchMealCards()`，按日期文本或关联菜品名搜索餐食日期后组装整天卡片。
- `Cookbook.sq`
  - 新增 `searchMealDates`。
- `MainScaffold`
  - 注册 `Routes.SEARCH`。
  - `BackHandler` 统一物理返回键策略。
  - 中间加号改为 `NavigationBarItem`，不再使用 `floatingActionButton`。

## 验证
- `./gradlew :androidApp:compileDebugKotlin`：通过。
- `./gradlew :androidApp:assembleDebug`：通过。
- `./gradlew :shared:testDebugUnitTest`：通过，当前无测试源。

## 注意
- 当前餐食日期搜索支持 `2026-05-26` 这类数据库日期文本，不解析“今天”“5月26”自然语言。
- 如果后续要求食材点击进入详情，需要先补食材详情页或编辑入口。
