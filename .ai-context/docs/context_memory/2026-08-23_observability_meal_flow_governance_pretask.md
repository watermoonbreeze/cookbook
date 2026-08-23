# 任务前快照：Observability 与 Meal Flow Governance

- 需求：在既有 State Lifecycle Observability 上补齐 Trace Diagnostic、Architecture Quality 增强、Meal Flow State Contract，覆盖 AI Recommend、Food Search、Inventory Select、New Dish、Edit Meal；统一 SAVE/RESTORE/MERGE。
- 执行级别：深度；常规交互；原因是跨模块架构治理、5 条流程、测试与远程交付。
- 授权：任务包明确要求 CODE -> REVIEW；已执行 `git pull --ff-only`，远程无新提交。
- 当前工作区：存在用户既有未提交删除/新增变更，必须保留；本批只修改任务相关 allowlist。
- 禁区：推荐算法、数据库/schema、Repository 行为、无关业务流程。
- 预计代码范围：`shared` Trace 模型/诊断与测试、`.ai-context/tools` Architecture Quality 检查与测试、`androidApp` Meal Flow 状态契约及测试、`.ai-context/docs/experience`、`BLUEPRINT_STATE`、会话交接。
- 验证：相关单测、shared/androidApp 构建；静态检查不能替代真机验证。
- 风险：现有状态文件顶部持有 ARCH；本批需以新批次记录明确 CODE 授权，交付后切回 REVIEW 等待 ARCH。
