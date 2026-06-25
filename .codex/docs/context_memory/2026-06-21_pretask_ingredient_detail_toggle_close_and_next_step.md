# 任务前快照：食材详情选择后关闭与重构下一步

- 时间：2026-06-21
- 用户需求：
  - 食材详情弹层中点击“选择”或“取消选择”后，弹层需要消失。
  - 继续按照之前食材体系重构方案推进下一步。
- 任务类型：BugFix + Feature 续做
- 执行深度：标准
- 角色分派：当前没有真实 `multi_agent_v1.spawn_agent`，主线程模拟 DEV_SA、DEV_ARCH、DEV_UI、DEV_CODE、DEV_TEST、DEV_REVIEW。

## 已知状态

- 食材页与菜品选择食材已经共用 `IngredientPickerScreen`。
- 食材详情已经是底部 65% 弹层。
- 底部“已选 X 项”已经支持跟随弹框。
- 文档基准为 `.codex/docs/feature/食材体系重构总方案.md`。

## 本轮计划

- 先修复详情弹层选择/取消选择后的关闭行为。
- 再读取当前数据库/Repository/seed 结构，选择食材体系重构下一步的最小闭环，优先推进新增/编辑食材按新维度承载所需的数据结构或 UI 入口。

## 风险与验证

- 风险：食材体系下一步可能涉及数据库迁移，需先确认现有 schema。
- 验证：至少执行 `./gradlew :androidApp:assembleDebug`；如改 shared/schema，则补跑 shared 相关测试。
