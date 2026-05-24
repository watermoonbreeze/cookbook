# 2026-05-24 餐食编辑刷新与图片选择记录

- 修复日期选择偏移：`DatePicker` 初始值改为 UTC 日期毫秒，避免本地时区导致默认显示前一天。
- 添加餐食页支持按日期加载已有餐食；保存时整日覆盖当天旧餐食，再写入当前模块列表。
- `MealRecordRepository` 新增 `loadDayMealsForEdit`、`observeTimelineCards`，食历改为监听数据库变化。
- 首页热门/最近菜品支持点击进入详情；计划卡片和食历卡片增加编辑入口，跳转到同一添加/编辑餐食页。
- 首页改为 `LazyColumn`，食历使用监听式 `LazyColumn`，数据库组装放到 `Dispatchers.Default`，降低切换卡顿。
- 新增 `ImagePickerButton`：支持相册多选和系统相机拍照，最多 3 张，使用 `FileProvider` 写入 app cache。
- 新建食材弹框增加分类必选和最多 3 张图片；新建/编辑菜品描述下方增加最多 3 张图片。
- 图片当前以 URI 字符串通过 `|` 拼接保存在现有 `image_path` 字段，后续可升级为私有目录持久复制和独立图片表。
- 验证命令：`./gradlew :androidApp:compileDebugKotlin`、`./gradlew :androidApp:assembleDebug`，均通过。
