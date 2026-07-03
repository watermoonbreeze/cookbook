# 任务前上下文快照：倒计时排序与持久化

- 时间：2026-06-12
- 用户需求：倒计时调整2：运行中的闹钟不要自动排到前面，避免列表跳动；创建的倒计时需要保存到本地数据库，便于下次烹饪直接复用。
- 任务类型：Feature / 数据持久化。
- 执行深度：标准。
- 交互模式：常规。
- 计划角色：主线程模拟 DEV_DB、DEV_CODE、DEV_UI、DEV_TEST、DEV_REVIEW。
- 已知项目状态：`CookingTimerScreen` 目前使用页面内存状态，新增计时器未持久化；当前数据库版本已有迁移文件到 `5.sqm`，`shared/build.gradle.kts` 版本为 6。
- 预计涉及文件：`Cookbook.sq`、新增迁移 `.sqm`、`shared/build.gradle.kts`、shared Repository/DI、Android DI、`CookingTimerScreen`、单元测试、数据库设计文档。
- 主要风险：数据库版本和迁移号不一致导致升级失败；运行中排序跳动需改为固定创建顺序；页面状态和数据库刷新互相覆盖；响铃状态不应持久化。
- 待验证项：新增模板可保存/编辑后重进页面保留；运行中不改变行顺序；迁移验证、shared 单测、Android 构建通过。
