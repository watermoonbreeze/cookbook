# 2026-06-05 基础数据 JSON 化迁移正式阶段0快照

- 用户需求：把基础数据改成 JSON 格式，后续只维护 JSON 文件即可增加基础数据。
- 任务类型：重构 / 基础数据迁移。
- 任务级别：标准级别。
- 判定原因：涉及 Seeder、commonMain resources、资源读取器、JSON 解析、单元测试；不改数据库结构和 UI/业务功能。
- 计划角色：DEV_SA(explorer) 分析现有 Seeder/资源/测试；DEV_REVIEW(explorer) 评审方案风险；主线程承担 DEV_CODE/DEV_DB/DEV_UT 实现与测试。
- 预计文件：PresetDataSeeder.kt、SeedResourceLoader expect/actual、shared/src/commonMain/resources/seed/*.json、PresetDataSeederTest、Gradle resources 配置如需要。
- 主要风险：KMP commonMain resources 在 Android unit test 中读取路径；JSON 结构变更兼容；老库补齐式 seed 不重复插入；基础数据只增加不改变业务功能。
- 待验证：shared 单元测试、seed 数据数量/关键分类、迁移校验、assembleDebug。
