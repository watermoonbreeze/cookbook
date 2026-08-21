# MEAL-UX-CONSOLIDATION-01 R2 任务快照

- 需求：按外部 ARCH R2 压缩包 `README_FIRST.md` 连续执行持久化、五项修复、测试/证据登记、最终状态交接。
- 模式：深度 / BLUEPRINT；角色顺序 `ARCH_PERSISTENCE_EXECUTOR → CODER → ARCH_HANDOFF_PERSISTENCE_EXECUTOR`。
- 权威输入：`.ai-context/docs/外部方案/在线审核/CookBook_MEAL_UX_CONSOLIDATION_01_ARCH_REVIEW_R2_REWORK.zip`；外部结论 `REWORK_REQUIRED`。
- 缺陷：`AF-HM-01/02/03`、`AF-GOV-01`、`AF-EVID-01`，不得新增、合并、改名或写 `ACCEPT/ACCEPTED`。
- 冻结范围：UEN-FINAL、DATE-CALENDAR-01 源实现；主代码 allowlist 为 `DayMealCardView.kt`、`HomeScreen.kt`、`MealRecordRepositoryTest.kt`。
- 当前握手：`BLUEPRINT_STATE.md` 当前可见 `TURN=CODE`，但存在冲突聚合区块；第一步需持久化为唯一 `REWORK_REQUIRED / REPAIR_BLUEPRINT_READY` 真相并提交推送。
- 证据要求：shared 单测、androidApp 单测、Android debug assembly；真机条目 UEN `01/16/17~21`、DATE `01~08`、HOME `01~08`，无真机时逐项 `PENDING_DEVICE_VERIFICATION` 并写精确原因。
- 风险：工作区已有用户未跟踪外部方案目录，保留不动；不执行破坏性 Git 操作；只提交本任务相关文件。
- 待验证：远端连续性、改动 allowlist、T-HM-10、三条构建门禁、状态唯一性、真机清单完整性、最终推送。
