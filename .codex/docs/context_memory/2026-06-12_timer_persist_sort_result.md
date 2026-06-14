# 倒计时排序与持久化结果

- 时间：2026-06-12
- 用户需求：运行中的倒计时不要自动排到前面，避免列表跳动；创建的倒计时需要保存到本地数据库，便于下次烹饪直接复用。
- 数据库：新增 `cooking_timer_template` 表，只保存计时模板配置，不保存运行/暂停/响铃等临时状态。
- 迁移：新增 `shared/src/commonMain/sqldelight/com/sxdbsm/cookbook/db/6.sqm`，数据库版本从 v6 升到 v7。
- shared：新增 `CookingTimerTemplate` 领域模型和 `CookingTimerRepository`，支持模板列表、保存/编辑、软删除。
- Android：`CookingTimerScreen` 进入页面时加载数据库模板；保存编辑时写库；删除模板时软删除；运行态仍在页面内存中。
- 排序：计时列表改为按 `sort_order` 稳定展示，不再按运行/暂停/完成状态重排，避免倒计时过程中列表跳动。
- 文档：同步更新 `.codex/docs/feature/数据库设计方案.md`。
- 测试：新增 `CookingTimerRepositoryTest` 覆盖保存、编辑、软删除和固定排序读取。
- 验证：`./gradlew :shared:verifyCommonMainCookbookDatabaseMigration :shared:verifySqlDelightMigration :shared:testDebugUnitTest :androidApp:assembleDebug` 成功。
- 边界：当前仅持久化模板；后台继续计时、锁屏通知、计时运行状态恢复仍属于后续扩展。
