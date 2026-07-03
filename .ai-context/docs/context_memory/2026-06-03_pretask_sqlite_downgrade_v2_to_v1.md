# 2026-06-03 任务前快照：SQLite version 2 降级到 1

## 用户最新需求
- App 打开时报错：`android.database.sqlite.SQLiteException: Can't downgrade database from version 2 to 1`
- 需要修复启动崩溃。

## 执行模式
- 标准级 BugFix / DB 版本修复。
- 原因：涉及 SQLDelight 数据库版本、迁移文件和 Android 启动流程，属于数据层高风险问题。

## 计划分派
- DEV_DB：定位 SQLDelight schema version、`.sqm` 迁移和当前数据库版本不一致原因。
- DEV_CODE：按 SQLDelight 规则修复版本或迁移配置。
- DEV_TEST：运行 SQLDelight interface 生成、shared 编译、Android 编译/打包验证。

## 当前已知状态
- 之前新增过 `dish_cooking_method_rel` 表，并补过 `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/1.sqm` 迁移。
- 当前报错表示设备已有数据库版本为 2，但当前 app 打包出来的数据库版本是 1，Android SQLite 不允许降级。

## 预计涉及文件/模块
- `shared/build.gradle.kts` 或 SQLDelight 配置文件。
- `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/Cookbook.sq`
- `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/*.sqm`

## 风险
- 不能通过清库解决，因为用户数据会丢失。
- 不能随意删除迁移文件，否则老版本升级会再次崩溃。
- 需要保证 schema version 不低于已发布/已安装版本。

## 待验证项
- SQLDelight 当前配置的 schema version。
- `.sqm` 迁移编号是否和目标版本匹配。
- 编译生成和 Android assemble 是否通过。
