# 2026-05-27 暖杏主题落地记录

## 背景

- 用户反馈上一版色系有脏感，提供新规范：`/Users/sxd/Downloads/菜单APP/菜谱菜单App 视觉设计规范文档（适配CodeX）.md`。
- 本轮按“莫兰迪低饱和暖杏风”重做 Android Compose UI 主题层。

## 已落地

- `Color.kt`：Material3 亮色切到 `#E2B999/#EED3BC/#F9F5F0`，暗色按暖杏语义生成低饱和暖棕灰体系。
- `ExtendedColors.kt`：成功/警告/危险色改为规范中的 `#7B9A86/#D19065/#B95F4F` 及对应暗色版本。
- `Theme.kt`：字号按页面标题 20、卡片标题 18、重点正文 16、正文 14、标签 12；圆角按卡片 16、输入框/按钮 12、标签 8、角标 4。
- 关键页面顶栏使用 `background + onBackground`，与页面根背景一体化。
- 内容卡片使用白底 `surface`，计划/分类/弱分组使用浅底 `secondaryContainer/surfaceVariant`。

## 关键文件

- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/theme/Color.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/theme/ExtendedColors.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/theme/Theme.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/component/DayMealCardView.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/nav/MainScaffold.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/search/SearchScreen.kt`
- `androidApp/src/main/java/com/sxdbsm/cookbook/android/ui/picker/*PickerScreen.kt`

## 验证

- `./gradlew :androidApp:compileDebugKotlin`：通过。
- `./gradlew :androidApp:assembleDebug`：通过。
- `./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest`：通过，当前均为 `NO-SOURCE`。

## 后续注意

- 后续新增 UI 禁止硬编码新色值，必须从 `MaterialTheme.colorScheme` 或 `ExtendedColorsHolder` 取色。
- 禁止毛玻璃、渐变、强阴影、浮雕和粗描边。
- 后续新增 UI 禁止直接复用上一版主题语义；主操作用暖杏主色，热销/推荐/价格类强调用 `tertiary`。
