# 2026-06-03 SQLite version 2 降级到 1 修复

## 问题
- App 打开时报错：`android.database.sqlite.SQLiteException: Can't downgrade database from version 2 to 1`

## 根因
- 项目已有 `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/1.sqm`，表示 v1 -> v2 迁移。
- 但 `shared/build.gradle.kts` 的 SQLDelight 配置没有显式设置目标数据库版本。
- 当前构建仍按 version 1 创建 `AndroidSqliteDriver`，手机里已有 version 2 数据库，因此触发 SQLite 降级保护。

## 修复
- 在 `shared/build.gradle.kts` 的 `sqldelight.databases.create("CookbookDatabase")` 中添加：
  - `version = 2`
- 不清库、不删除迁移、不影响已有用户数据。

## 验证
- `./gradlew :shared:generateCommonMainCookbookDatabaseInterface :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin`：通过。
- `./gradlew :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest`：通过。
- 当前 test task 为 `NO-SOURCE`。

## 经验
- 新增 `.sqm` 迁移时，必须同步提升 SQLDelight database `version`。
- schema version 不能低于任何已安装包曾经创建/升级过的数据库版本。
