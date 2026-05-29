# 2026-05-28 修复7：菜品展示、导航与烹饪方式

## 已完成

- 菜品 Item 右侧喜爱值不再显示裸数字：
  - 普通有值显示 `⭐ 喜爱度 N`。
  - 当前列表喜爱值前 3 名显示 `🔥 热度 N`。
- 首页“热门/最近”的更多入口改用底部 Tab 切换逻辑进入菜品页。
- 底部 Tab 统一走 `navigateRootTab()`，点击首页可直接回到首页，不再必须先按返回。
- 菜品 block（横滑小卡）和菜品 Item 缩略图禁用图片预览，点击统一进入菜品详情。
- 菜品详情补充图片区域：
  - 顶部展示首图。
  - 图片区域多图时展示剩余图片，单图时保留可点击预览入口。
- 菜品编辑页烹饪方式支持下拉选择或手动输入。

## 技术决策

- 当前数据库 `dish` 只有单个 `cooking_method_id`，本轮不做多选关系表迁移。
- “下拉多选和可输入”按当前结构落地为“下拉选择一个或输入一个新方式”；用户输入的新方式会写入 `cooking_method` 字典并保存其 id。
- 若后续需要一菜多烹饪方式，需要新增关系表并迁移旧字段。

## 关键文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/MainScaffold.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/DishRow.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/DishMiniCard.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/StoredImage.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/dishdetail/DishDetailScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/newdish/NewDishViewModel.kt`
- `shared/src/commonMain/kotlin/com/sxdbsm/cookbook/data/repository/DishRepository.kt`

## 验证

- `./gradlew :androidApp:compileDebugKotlin`：通过。
- `./gradlew :androidApp:assembleDebug`：通过。
- `./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest`：通过，当前均为 `NO-SOURCE`。
