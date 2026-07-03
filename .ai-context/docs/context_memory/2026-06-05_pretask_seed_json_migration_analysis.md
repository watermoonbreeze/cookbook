# Seed JSON 迁移只读分析任务前快照

- 时间：2026-06-05
- 用户需求：以 DEV_SA 系统分析师角色，只读分析将基础 seed 数据从 `PresetDataSeeder.kt` 硬编码迁移到 `shared/src/commonMain/resources/seed/*.json` 的注意事项。
- 模式/级别：轻量分析；原因是仅做只读代码、Gradle resources、测试结构分析，不修改业务代码。
- 计划角色：DEV_SA；Codex 子代理类型按流程映射为 explorer。本次工具环境无 `multi_agent_v1`，由主线程按 DEV_SA 规范执行。
- 已知项目状态：KMP 项目，`:shared` 负责跨平台 Domain/Data/seed；SQLDelight 为数据库来源；要求保持补齐式 seed、兼容旧库、不改业务功能。
- 预计涉及模块：`shared` commonMain seed 逻辑、resources 配置、SQLDelight 数据库访问、shared 测试结构。
- 主要风险：KMP common resources 读取方式、iOS framework 打包资源可见性、旧库字段/ID/唯一键兼容、补齐式 seed 幂等性、测试环境资源加载差异。
- 待验证项：`PresetDataSeeder.kt` 当前插入策略、Gradle 是否已有 resources 配置、commonTest/androidUnitTest 是否覆盖 seed、现有资源目录是否存在。
