# 2026-06-05 基础数据 JSON 化迁移任务前上下文

- 用户确认：基础数据改成 JSON 格式，方便后期扩展。
- 约束：只改变基础数据承载方式，不调整业务功能；保留补齐式 seed，兼容旧库和后续扩展。
- 方案：在 `shared/src/commonMain/resources/seed/` 增加 JSON 文件，包含分类、食材、慢病规则；`PresetDataSeeder` 读取 JSON 并写入数据库。
- 验证：复用并扩展 `PresetDataSeederTest`，运行 shared 单元测试、迁移校验、assembleDebug。
