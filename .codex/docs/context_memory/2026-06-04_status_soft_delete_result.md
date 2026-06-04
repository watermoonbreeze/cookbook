# 2026-06-04 全表 status 软删除改造结果

- 用户要求：检查所有数据表是否都有 status 字段；status 0=无效，1=有效，默认 1；后续删除操作改为软删除。
- 审计：`Cookbook.sq` 当前 20 张表原本都没有 status，删除语句均为真删除。
- 实现：20 张表全部新增 `status INTEGER NOT NULL DEFAULT 1`。
- 实现：新增 v3 -> v4 迁移 `3.sqm`，对 20 张表执行 `ALTER TABLE ... ADD COLUMN status INTEGER NOT NULL DEFAULT 1`，并追加 `UPDATE ... SET status = COALESCE(status, 1)` 防御异常旧库 NULL。
- 实现：`shared/build.gradle.kts` SQLDelight version 从 3 升到 4。
- 实现：常规 COUNT/SELECT/JOIN/SEARCH 查询默认过滤 `status=1`；关联表 join 同时检查关联表和主表 status。
- 实现：当前 `Cookbook.sq` 已无 `DELETE FROM`；删除语句改为 `UPDATE ... SET status=0`。
- 实现：关联表重新添加用 SQLite 3.18 兼容的 `INSERT OR REPLACE` 恢复 `status=1`；用户偏好/健康档案等 upsert 也设置 `status=1`。
- 经验沉淀：新增 NOT NULL 字段必须 schema DEFAULT、迁移 DEFAULT、迁移 COALESCE、启动清洗纳入字段；新增业务过滤字段必须同步所有查询和删除语义。
- 文档：更新 `.codex/docs/feature/数据库设计方案.md` 的全表 status 软删除规则。
- 验证：`./gradlew :shared:generateCommonMainCookbookDatabaseInterface :shared:compileDebugKotlinAndroid :androidApp:compileDebugKotlin` 通过。
- 验证：`./gradlew :shared:verifyCommonMainCookbookDatabaseMigration :shared:verifySqlDelightMigration :androidApp:assembleDebug :shared:testDebugUnitTest :androidApp:testDebugUnitTest` 通过。
