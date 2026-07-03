# 2026-06-03 修复12：权限门禁、编辑回填、食材 NPE、公共存储

## 任务编排

- 模式：深度 BugFix + 数据/存储路径核验。[AI生成]
- 智能体分派：尝试分派 explorer 分析编辑页/图片链路，但当前会话 agent thread 达到上限，改由主线程按 DEV_SA、DEV_DB、DEV_ANDROID_STORAGE 顺序执行。[AI生成]
- 执行前快照：`2026-06-03_pretask_fix12_edit_ingredient_storage.md`。[AI生成]

## 已完成

- 食历 `DayMealCardView` 内菜品 block 增加点击回调，点击进入菜品详情。[AI生成]
- `NewDishViewModel.start()` 去掉同入口跳过加载逻辑，每次进入编辑/导入都重新按 id 加载数据库，降低复用旧空状态风险。[AI生成]
- 食材查询改为 `COALESCE` 显式列查询，兼容旧库 NULL 字段，避免 SQLDelight 非空映射 NPE。[AI生成]
- `PresetDataSeeder.seedIfNeeded()` 开始时执行旧数据 NULL 文本字段修正，覆盖已升级到 v3 的旧库。[AI生成]
- 新增 `CookbookStorage`，统一 `/sdcard/cookbook` 权限检查、公共目录创建和旧 app 专属外部目录迁移。[AI生成]
- `Application` 不再启动时 seed，避免授权前提前创建数据库。[AI生成]
- `MainActivity` 增加权限门禁：授权前不加载主题偏好、不加载 HomeScreen、不触发数据库；授权后先迁移 `Android/data/<package>/files/cookbook` 到 `/sdcard/cookbook`，再创建 `db/img` 目录并加载 App。[AI生成]
- 数据库和图片保存取消 app 专属目录 fallback，统一从 `/sdcard/cookbook/db`、`/sdcard/cookbook/img` 获取数据。[AI生成]

## 权限规则

- Android 11+：使用 `MANAGE_EXTERNAL_STORAGE`，跳转系统“所有文件访问权限”设置页。[AI生成]
- Android 9 及以下：请求 `READ_EXTERNAL_STORAGE`、`WRITE_EXTERNAL_STORAGE` 运行时权限。[AI生成]
- Android 10：按平台兼容路径处理；授权门禁仍会先准备公共目录，失败则停留在权限页。[AI生成]

## 验证

- `./gradlew :shared:generateCommonMainCookbookDatabaseInterface`：通过。[AI生成]
- `./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin`：通过。[AI生成]
- `./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest`：通过；测试任务当前为 `NO-SOURCE`。[AI生成]

## 注意事项

- 后续任何 DB/图片路径逻辑必须使用 `CookbookStorage`，不要再自行 fallback 到 app 专属外部目录。[AI生成]
- `MANAGE_EXTERNAL_STORAGE` 是 Android 11+ 的特殊权限，用户必须在系统设置页开启后才会进入 App 主界面。[AI生成]
