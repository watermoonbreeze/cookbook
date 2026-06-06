# 2026-06-05 任务前快照：首页主题弹框与 MVP 审计

- 用户最新需求：完成上一任务后继续；首页点击主题切换按钮直接弹出主题切换弹框，不跳转“我的”；随后检查 MVP 是否还有未完成项，列成文档并逐一完成；无人值守模式。[AI生成]
- 执行级别：深度级别。[AI生成]
- 定级原因：任务包含 UI 交互修改、MVP 全量审计、文档整理、后续多项实现和回归验证，影响范围跨 Android UI、Navigation、docs、tests。[AI生成]
- 计划角色：DEV_SA 分析代码流程；DEV_PM 梳理 MVP 边界和验收；DEV_ARCH/DEV_REVIEW 做方案和风险；DEV_UI/DEV_CODE/DEV_UT 落地实现和测试；DEV_TEST 做验证。[AI生成]
- 已知状态：基础数据 JSON 化与食材 emoji 任务已通过 shared 单元测试、SQLDelight 迁移校验和 Android assemble。[AI生成]
- 预计涉及：`MainScaffold`、Home/Theme 相关 UI、`.codex/docs/feature/MVP实施方案.md`、`docs/MVP开发规划.md`、功能文档、测试文件。[AI生成]
- 主要风险：MVP 文档与当前实现可能不完全一致；“逐一完成”可能扩展为多个独立功能，需要按风险分批；主题弹框需保持现有主题偏好持久化逻辑。[AI生成]
- 待验证项：主题按钮点击直接弹框；主题保存后即时生效；MVP 未完成项文档完整；新增实现配套单元测试或构建验证。[AI生成]
