# 阶段0：DB 版本对齐与地基清理（结论·已完成并构建通过）

[AI生成] 2026-07-06。食材层重构前置。前提：应用**重新安装**（无历史旧库），故可彻底清理。

## 证实的真相（DB 版本机制）

- SQLDelight 2.0.1 database 块**无 `version` 属性**；运行时 `Schema.version` 由 `*.sqm` 文件数推导（1~10.sqm ⇒ **11**）。
- 旧 `build.gradle` 的 `version = 8` 实为泄漏到 `Project.version`，与 DB 无关——版本误解根因。真实版本只看生成的 `CookbookDatabaseImpl.Schema.version`。

## 改动（3 处，已 build+test 通过）

1. `shared/build.gradle.kts`：删除 `version = 8`，加注释说明版本由 `.sqm` 数推导。
2. 新增 `shared/.../db/10.sqm`：`ALTER TABLE ingredient ADD COLUMN reason ...`，让迁移链重建结果与 `Schema.create` 一致；`Schema.version` 自动 10→11。
3. `DatabaseDriverFactory.android.kt`：删除 `ensureLegacyColumns` 幂等补列（reason 改由 10.sqm 承接，消除驱动侧重复 DDL）。

## 验证

- `:shared:testDebugUnitTest` ✅；`:androidApp:assembleDebug` ✅。
- 生成代码确认 `Schema.version get() = 11`，migrate() 含 reason ALTER。

## 用户三要求达成情况

1. 基础数据只写一次（版本/hash 控制）✅ 已有 `PresetDataSeeder` 内容指纹守卫（`reseedContentIfChanged`），无需改。
2. 数据源唯一不冗余 ✅ 种子数据仅 `resources/seed/*.json`；schema+查询仅 `Cookbook.sq`；删掉了驱动侧重复 DDL。
3. 地基清理干净 ✅ 见上。

## 三层架构（复述给用户，来自 食材层总方案.md）

界面层（Compose 只管展示交互）/ 数据层（①基础数据 seed 可增量扩展 ②用户数据可存/后期同步）/ AI 层（结合两者给膳食建议，预留）。核心：先搭框架规范，内容后续增量补，加内容不动架构。

## 下一步

阶段1：建 pantry 库存表（按正道 11.sqm，version 自动→12）+ 常规品类树/维度节点 seed + 全局搜索。
建议后续开启 SQLDelight `verifyMigrations` 让"schema 与迁移不一致"构建期暴露（本次未启，避免节外生枝）。
