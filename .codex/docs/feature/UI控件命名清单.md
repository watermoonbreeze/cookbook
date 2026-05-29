# UI控件命名清单

更新时间：2026-05-29

用途：后续沟通 UI 修改时，优先使用“沟通名称”。实现位置用于 Codex 定位代码，不要求用户记住源码类名。

## 通用控件

| 沟通名称 | 当前实现 | 使用位置 | 说明 |
|---|---|---|---|
| 菜品Block | `DishMiniCard`：`androidApp/.../ui/component/DishMiniCard.kt` | 首页热门/最近、菜品页喜爱横滑、餐食模块内菜品横滑、菜品选择弹框 | 小图 + 菜名，点击进入菜品详情，不直接预览图片。 |
| 菜品Item | `DishRow`：`androidApp/.../ui/component/DishRow.kt` | 菜品页列表、菜品选择弹框、搜索页菜品结果 | 左图、中间菜名/标签/信息、右侧喜爱值；可扩展多选或长按菜单。 |
| 餐食卡片 | `DayMealCardView`：`androidApp/.../ui/component/DayMealCardView.kt` | 首页计划、食历列表、搜索页餐食结果 | 一天的餐食组合模块，由多个餐次行组成；当前用色块 + 投影卡片。 |
| 餐次行 | `MealSectionRow`：`DayMealCardView.kt` 内部私有控件 | 餐食卡片内部 | 餐次名称 + 菜品Block横滑列表。 |
| 通用搜索栏 | `AppSearchField`：`androidApp/.../ui/component/AppSearchField.kt` | 菜品页、搜索页、食材选择页 | 48dp 高度、色块背景、无边框感的搜索输入。 |
| 图片选择控件 | `ImagePickerButton`：`androidApp/.../ui/component/ImagePickerButton.kt` | 新建/编辑菜品、添加/编辑食材弹框 | 最多 3 张图片，保存到 `image_path`，多个路径用 `|` 分隔。 |
| 图片展示控件 | `StoredImage`：`androidApp/.../ui/component/ImagePickerButton.kt` | 菜品Block、菜品Item、食材Item、详情页 | 优先展示 `image_path` 第一张；无图时展示 emoji/文字色块。 |
| 标签Chip | `TagChip`：`androidApp/.../ui/component/DishRow.kt` | 菜品Item、新建/编辑菜品标签、烹饪方式展示 | 小色块标签。 |
| 空状态 | `EmptyState`：`androidApp/.../ui/component/EmptyState.kt` | 首页、菜品页、搜索页等 | 无数据提示。 |
| 菜品选择器 | `DishPickerScreen`：`androidApp/.../ui/picker/DishPickerScreen.kt` | 添加餐食、新建菜品导入 | 全屏/弹框式菜品选择业务控件。 |
| 字段标题 | `FormFieldLabel`：`androidApp/.../ui/component/UiText.kt` | 添加餐食、菜品详情、新建/编辑菜品 | 表单或详情字段的小标题。 |
| 分区标题 | `SectionHeader`：`androidApp/.../ui/component/UiText.kt` | 首页、搜索页、菜品选择器 | 模块标题，可带右侧操作。 |
| 空行提示 | `EmptyLineText`：`androidApp/.../ui/component/UiText.kt` | 搜索页等 | 列表分组内无结果的轻量提示。 |
| 食材卡 | `IngredientCard`：`androidApp/.../ui/component/IngredientCard.kt` | 搜索页食材结果、食材选择器右侧食材格子 | 食材图片/emoji + 名称，可显示选中态、建议角标和自建食材菜单。 |

## 首页Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 首页Screen | `HomeScreen` | 个性化 | 首页顶栏、热门、最近、计划入口。 |
| 热门模块 | `SectionHeader` + LazyRow | 个性化组合 | 内容使用通用“菜品Block”。 |
| 最近模块 | `SectionHeader` + LazyRow | 个性化组合 | 内容使用通用“菜品Block”。 |
| 首页计划模块 | `DayMealCardView` | 通用 | 也叫“餐食卡片”，和食历共用。 |

## 食历Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 食历Screen | `FoodTimelineScreen` | 个性化 | 食历头部、分页列表、日期弹框入口。 |
| 餐食日期选择器 | `TimelineCalendarDialog` / `CalendarDayCell` | 个性化 | 日历中有餐食日期显示小圆点。 |
| 食历餐食卡片 | `DayMealCardView` | 通用 | 与首页计划、搜索餐食结果共用。 |

## 添加餐食Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 添加餐食Screen | `AddDayFoodScreen` | 个性化 | 添加/编辑一天餐食。 |
| 普通日期选择器 | `DatePickerDialog` 调用点 | 个性化 | 添加餐食页选择日期。 |
| 餐次编辑卡片 | `MealBlockCard` | 可抽取 | 单个餐次编辑模块。 |
| 餐次下拉框 | `MealTypeDropdown` | 可抽取 | 早餐/中餐/晚餐/加餐选择。 |
| 用餐时间控件 | `TimePickerDialog` 调用点 | 可抽取 | 点击时间修改具体用餐时间。 |
| 添加菜品按钮 | `MealBlockCard` 内 TextButton | 可抽取 | 跳转菜品库选择菜品。 |
| 保存餐食按钮 | `AddDayFoodScreen` 顶栏 Button | 个性化 | 保存当天所有餐次。 |

## 菜品Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 菜品Screen | `DishesScreen` | 个性化 | 菜品顶栏、喜爱横滑、Tab、列表。 |
| 菜品页搜索栏 | `AppSearchField` | 通用 | 通用搜索栏实例。 |
| 加号添加按钮 | `IconButton(Icons.Outlined.Add)` | 可抽取 | 进入新建菜品。 |
| 菜品筛选Tab | `TabRow` | 个性化 | 最近/喜爱/拼音/全部。 |
| 菜品喜爱横滑 | `DishMiniCard` 列表 | 通用组合 | 内容为“菜品Block”。 |
| 菜品列表Item | `DishRow` | 通用 | 列表主控件。 |

## 搜索Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 搜索Screen | `SearchScreen` | 个性化 | 全局搜索菜品/食材/餐食。 |
| 搜索页搜索栏 | `AppSearchField` | 通用 | 通用搜索栏实例。 |
| 搜索菜品结果 | `DishRow` 横滑 | 通用组合 | 使用“菜品Item”。 |
| 搜索食材结果 | `IngredientCard` | 通用 | 食材图文结果卡片。 |
| 搜索餐食结果 | `DayMealCardView` | 通用 | 使用“餐食卡片”。 |

## 食材选择Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 食材选择弹框 | `IngredientPickerScreen` | 个性化 | 分类树 + 食材网格 + 搜索 + 新增/编辑食材。 |
| 食材搜索栏 | `AppSearchField` | 通用 | 通用搜索栏实例。 |
| 食材分类树 | `CategoryTreeNode` | 个性化 | 左侧分类展开。 |
| 食材格子Item | `IngredientCard` | 通用 | 展示食材图片/emoji、名称、可长按提示。 |
| 添加/编辑食材弹框 | `IngredientEditDialog` | 可抽取 | 名称、别名、分类、图片。 |

## 菜品详情/编辑Screen

| 沟通名称 | 当前实现 | 是否通用 | 说明 |
|---|---|---|---|
| 菜品详情Screen | `DishDetailScreen` | 个性化 | 展示菜品图片、标签、食材、做法、编辑入口。 |
| 菜品编辑Screen | `NewDishScreen` | 个性化 | 新建/编辑共用页面。 |
| 烹饪方式选择器 | `CookingMethodDialog` | 可抽取 | 标签样式入口，弹框下拉选择或手动输入。 |
| 菜品图片选择控件 | `ImagePickerButton` | 通用 | 新建/编辑菜品底部图片。 |
| 菜品食材清单 | `NewDishScreen` 内食材列表 | 可抽取 | 当前不展示“主料”标识。 |

## 后续抽取建议

- 优先保持 `DishMiniCard`、`DishRow`、`DayMealCardView`、`AppSearchField`、`StoredImage`、`ImagePickerButton`、`IngredientCard`、`FormFieldLabel`、`SectionHeader` 为稳定通用控件。
- `MealBlockCard`、`MealTypeDropdown`、`CookingMethodDialog` 已具备抽取价值，但当前仍与页面状态耦合，后续大改时再迁到 `ui/component/`。
- 后续沟通中建议使用“菜品Block / 菜品Item / 餐食卡片 / 餐次编辑卡片 / 通用搜索栏 / 食材卡 / 字段标题 / 分区标题”等名称。
