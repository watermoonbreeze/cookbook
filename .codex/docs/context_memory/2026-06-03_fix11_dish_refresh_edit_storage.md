# 2026-06-03 修复11：菜品刷新、编辑回填、数据库与图片外置

## 任务编排

- 模式：深度 BugFix + 数据/存储迁移。[AI生成]
- 参与：DEV_SA 智能体做 UI/ViewModel 读码分析；DEV_DB 因智能体数量上限由主线程按数据库角色执行。[AI生成]
- 执行前已保存快照：`2026-06-03_pretask_fix11_dish_refresh_edit_storage.md`。[AI生成]

## 已完成

- 菜品页空关键词列表改为观察 `dishRepo.observeAllDishes()`，添加菜品后返回可自动刷新。[AI生成]
- 菜品页增加 `ON_RESUME` 刷新和 Tab 列表下拉刷新提示。[AI生成]
- 编辑菜品增加 `loading/errorMessage` 状态，加载失败不再静默显示空表单，保存按钮会被禁用。[AI生成]
- `dish`、`ingredient` 新增 `thumbnail_path` 字段；SQLDelight 目标版本升级到 v3，并新增 `2.sqm` 迁移。[AI生成]
- 图片保存规则调整：`image_path` 存原图，`thumbnail_path` 存缩略图，多图均用 `|` 分隔并按索引对应。[AI生成]
- 图片路径优先 `/sdcard/cookbook/img/`，数据库优先 `/sdcard/cookbook/db/cookbook.db`；系统限制时回退 app 专属外部目录。[AI生成]
- 数据库外置时会从旧内部库复制主库及 `-wal`、`-shm` 文件，目标库存在时不覆盖。[AI生成]

## 验证

- `./gradlew :shared:generateCommonMainCookbookDatabaseInterface`：通过。[AI生成]
- `./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin`：通过。[AI生成]
- `./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest`：通过；两个测试任务当前为 `NO-SOURCE`。[AI生成]
- 清理 `StoredImage` 预览分支的冗余空判断后，重新执行 `./gradlew :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin`：通过，未再出现该警告。[AI生成]

## 注意事项

- Android 10+ 对公共根目录写入有限制，`/sdcard/cookbook/...` 是优先目标，不保证所有设备都可写；代码已做 app 专属外部目录 fallback。[AI生成]
- 后续改图片逻辑时必须同时维护 `image_path` 和 `thumbnail_path`，删除/排序都按索引同步处理。[AI生成]
- 后续新增 SQLDelight schema 变更时，必须同步 `.sqm` 迁移和 `shared/build.gradle.kts` 的 `version`。[AI生成]
