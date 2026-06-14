# 任务前上下文快照：总结烹饪计时经验

- 时间：2026-06-14
- 用户需求：总结近期烹饪计时、铃声控制、排序与本地持久化相关经验。
- 任务类型：总结 / 经验沉淀。
- 执行深度：轻量。
- 交互模式：常规。
- 计划角色：主线程模拟 DEV_PM、DEV_DB、DEV_UI、DEV_REVIEW。
- 已知项目状态：任务7/8及后续倒计时调整已完成；新增 `CookingTimerScreen`、`cooking_timer_template` 表、v7 迁移、`CookingTimerRepository`，并通过迁移验证、shared 单测和 Android 构建。
- 预计涉及文件：`.codex/docs/experience/05_UI组件.md`、`.codex/docs/experience/03_数据库.md` 或 `.codex/docs/experience/06_问题与踩坑.md`。
- 主要风险：经验写得过细变成流水账；需沉淀稳定可复用规则，如“模板持久化 vs 运行态内存”“计时列表稳定排序”“Ringtone 生命周期可控”。
- 待验证项：写入位置合理、内容不和现有经验重复。
